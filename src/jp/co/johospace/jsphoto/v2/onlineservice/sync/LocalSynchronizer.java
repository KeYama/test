package jp.co.johospace.jsphoto.v2.onlineservice.sync;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.database.CMediaMetadataDirty;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.provider.JorlleProvider;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner;
import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.JsMediaAuth;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.db.CLocalSync;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.db.CMediaSync;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

/**
 * ローカル同期
 */
public class LocalSynchronizer extends ContextWrapper/* implements UncaughtExceptionHandler*/ {
	private static final String tag = LocalSynchronizer.class.getSimpleName();
	
	protected final JsMediaServerClient mJsMedia;
	protected final SQLiteDatabase mDb;
	protected final JsMediaAuth mAuth;
	public LocalSynchronizer(Context context) {
		super(context);
		mJsMedia = ClientManager.getJsMediaServerClient(context);
		mDb = OpenHelper.sync.getDatabase();
		mAuth = new JsMediaAuth(context);
	}
	
	protected static final int FILE_NOT_FOUND = 0;
	protected static final int UPDATE = 1;
	protected static final int DELETE = 2;
	protected static final int KEEP = 3;
	
	public synchronized long sendLocalIndexes(final boolean resendAll) throws IOException {
		final long[] next = new long[1];
		JsMediaAuth.Credential _credential = mAuth.loadCredential();
		if (_credential == null) {
			Map<String, String> given = mJsMedia.createAccount();
			mAuth.saveCredential(given.get("devid"), given.get("token"));
			_credential = mAuth.loadCredential();
		}
		
		final JsMediaAuth.Credential credential = _credential;
		
		mDb.beginTransaction();
		try {
			d("INIT");
			// メタデータの汚れを反映
			applyMetadataDirtyToLocalSync(mDb, CLocalSync.METADATA);
			
			// ステータスをクリア
			if (resendAll) {
				d("    MODE RE-SEND ALL!");
				deletePreviouse(mDb);
			} else {
				clearStatus(mDb, FILE_NOT_FOUND);
			}
			d("    done");
			
			scanMedia(new ScanHandler() {
				@Override
				public void onFound(File file) {
					d("* FOUND - %s", file.getAbsolutePath());
					final String dirpath = file.getParent();
					final String name = file.getName();
					Cursor cPrev = mDb.query(CLocalSync.$TABLE,
							new String[] {CLocalSync.METADATA, CLocalSync.PREV_METADATA, CLocalSync.METADATA_NEW},
							CLocalSync.DIRPATH + " = ? AND " + CLocalSync.NAME + " = ?",
							new String[] {dirpath, name},
							null, null, null, "1");
					try {
						Cursor cSync = mDb.query(CMediaSync.$TABLE,
								new String[] {CMediaSync.MEDIA_ID},
								CMediaSync.DIRPATH + " = ? AND " + CMediaSync.NAME + " = ?",
								new String[] {dirpath, name},
								null, null, null, "1");
						try {
							ContentValues values = new ContentValues();
							values.put(CLocalSync.DIRPATH, dirpath);
							values.put(CLocalSync.NAME, name);
							
							if (cPrev.moveToFirst() && cPrev.getInt(2) != 1) {
								Long metadata = cPrev.isNull(0) ? null : cPrev.getLong(0);
								Long prevMetadata = cPrev.isNull(1) ? null : cPrev.getLong(1);
								if (cSync.moveToFirst()) {
									// 前回存在して、いま双方向 -> 削除として送る
									d("  前回存在して、いま双方向 -> 削除として送る");
									values.put(CLocalSync.STATUS, DELETE);
								} else {
									// 前回存在して、いま双方向でない -> 変更されていれば送る（更新）
									d("  前回存在して、いま双方向でない -> 変更されていれば送る（更新）");
									if ((metadata != null && prevMetadata != null && !metadata.equals(prevMetadata))
											|| !(metadata == null && prevMetadata == null)) {
										// 変更あり
										d("     変更あり");
										values.put(CLocalSync.STATUS, UPDATE);
									} else {
										// 変更なし
										d("     変更なし");
										values.put(CLocalSync.STATUS, KEEP);
									}
								}
								values.put(CLocalSync.METADATA, metadata);
							} else {
								if (cSync.moveToFirst()) {
									// 前回存在しなくて、いま双方向 -> なにもしない
									d("  前回存在しなくて、いま双方向 -> なにもしない");
									values = null;
								} else {
									// 前回存在しなくて、いま双方向でない -> 送る（新規）
									d("  前回存在しなくて、いま双方向でない -> 送る（新規）");
									values.put(CLocalSync.STATUS, UPDATE);
									if (cPrev.isFirst()) {
										values.put(CLocalSync.METADATA,
												cPrev.isNull(0) ? null : cPrev.getLong(0));
									}
								}
							}
							
							if (values != null) {
								values.put(CLocalSync.PREV_METADATA, values.getAsLong(CLocalSync.METADATA));
								values.put(CLocalSync.METADATA_NEW, 0);
								mDb.insertWithOnConflict(CLocalSync.$TABLE, null,
										values, SQLiteDatabase.CONFLICT_REPLACE);
							}
						} finally {
							cSync.close();
						}
					} finally {
						cPrev.close();
					}
					d("    done");
				}
			});
			
			d("SEND");
			final Cursor c = mDb.query(CLocalSync.$TABLE,
					new String[] {
							CLocalSync.DIRPATH,
							CLocalSync.NAME,
							CLocalSync.STATUS
					},
					CLocalSync.STATUS + " IN (?, ?, ?)",
					new String[] {
							String.valueOf(FILE_NOT_FOUND),
							String.valueOf(UPDATE),
							String.valueOf(DELETE)
					},
					null, null, null);
			
			IOIterator<Media> medias = new IOIterator<Media>() {
				
				Media mNext = prepareNext();
				
				Media prepareNext() {
					if (c.moveToNext()) {
						int status = c.getInt(2);
						String dirpath = c.getString(0);
						String name = c.getString(1);
						File file = new File(dirpath, name);
						boolean delete =
								status == FILE_NOT_FOUND || status == DELETE;
						mNext = toMedia(credential.deviceId, file, delete);
					} else {
						mNext = null;
					}
					return mNext;
				}
				
				@Override
				public boolean hasNext() throws IOException {
					return mNext != null;
				}
				
				@Override
				public Media next() throws IOException, NoSuchElementException {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					try {
						return mNext;
					} finally {
						prepareNext();
					}
				}
				
				@Override
				public void terminate() throws IOException {
					c.close();
				}
			};
			
			mJsMedia.updateLocalMediaIndices(medias, next);
			
			mDb.delete(CLocalSync.$TABLE,
					CLocalSync.STATUS + " IN (?, ?)",
					new String[] {String.valueOf(FILE_NOT_FOUND), String.valueOf(DELETE)});
			
			mDb.setTransactionSuccessful();
			d("    done");
		} finally {
			mDb.endTransaction();
		}
		
		return next[0];
	}
	
	
	protected Media toMedia(String deviceId, File file, boolean delete) {
		Media media = new Media();
		media.deleted = delete;
		media.service = ServiceType.JORLLE_LOCAL;
		media.account = deviceId;
		media.mediaId = file.getAbsolutePath();
		media.directoryId = ServiceType.JORLLE_LOCAL;
		media.mediaUri = null;
		media.fileName = file.getName();
		media.thumbnailData = null;
		media.version = null;
		
		if (!delete) {
			media.productionDate = getProductionDate(file);
			media.updatedTimestamp = file.lastModified();
			media.metadata = ProviderAccessor.queryMetadata(
					getApplicationContext(), file.getParent(), file.getName());
			if (media.metadata != null && !media.metadata.isEmpty()) {
//				System.out.println(String.format("%s %s", media.mediaId, media.metadata));		/*$debug$*/
			}
		} else {
			media.productionDate = null;
			media.updatedTimestamp = null;
			media.metadata = null;
		}
		return media;
	}
	
	protected long getProductionDate(File file) {
		try {
			ExifInterface exif = new ExifInterface(file.getAbsolutePath());
			String datetime = exif.getAttribute(ExifInterface.TAG_DATETIME);
			SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
			return format.parse(datetime).getTime();
		} catch (Exception e) {
			return file.lastModified();
		}
	}
	
	protected int clearStatus(SQLiteDatabase db, int status) {
		ContentValues values = new ContentValues();
		values.put(CLocalSync.STATUS, status);
		return db.update(CLocalSync.$TABLE, values, null, null);
	}
	
	protected int deletePreviouse(SQLiteDatabase db) {
		return db.delete(CLocalSync.$TABLE, null, null);
	}
	
	protected void applyMetadataDirtyToLocalSync(SQLiteDatabase db, String column) {
		Uri uri = JorlleProvider.getUriFor(getApplicationContext(), new String[] {CMediaMetadataDirty.$TABLE});
		Cursor c = getContentResolver().query(uri,
				new String[] {CMediaMetadataDirty.DIRPATH, CMediaMetadataDirty.NAME, CMediaMetadataDirty.UPDATED_TIME},
				null, null, null);
		try {
			ContentValues values = new ContentValues();
			while (c.moveToNext()) {
				values.clear();
				
				String dirpath = c.getString(0);
				String name = c.getString(1);
				long time = c.getLong(2);
				
				File file = new File(dirpath, name);
				if (file.exists()) {
					values.put(column, time);
					int updated = db.update(CLocalSync.$TABLE, values,
							CLocalSync.DIRPATH + " = ?" +
									" AND " + CLocalSync.NAME + " = ?",
							new String[] {dirpath, name});
					if (updated == 0) {
						values.put(CLocalSync.DIRPATH, dirpath);
						values.put(CLocalSync.NAME, name);
						values.put(CLocalSync.STATUS, FILE_NOT_FOUND);
						values.put(CLocalSync.METADATA_NEW, 1);
						db.insertOrThrow(CLocalSync.$TABLE, null, values);
					}
					
				} else {
					try {
						getContentResolver().delete(uri,
								CMediaMetadataDirty.DIRPATH + " = ?" +
										" AND " + CMediaMetadataDirty.NAME + " = ?",
								new String[] {dirpath, name});
					} catch (Exception e) {
						Log.e(tag, "failed to delete dirty.", e);/*$debug$*/
					}
					
				}
				
			}
		} finally {
			c.close();
		}
	}
	
	private interface ScanHandler {
		void onFound(File file);
	}
	
	private void scanMedia(ScanHandler handler) {
		scanMedia0(Environment.getExternalStorageDirectory(), handler);
	}
	
	private void scanMedia0(File root, ScanHandler handler) {
		if (!root.getName().startsWith(".")) {
			if (root.isDirectory()) {
				File[] children = root.listFiles();
				for (File child : children) {
					if (child.isFile()
							&& child.getName().equals(".nomedia")) {
						d(".nomedia を検出。配下のスキャンをしない - %s", root.getAbsolutePath());
						return;
					}
				}
				for (File child : children) {
					scanMedia0(child, handler);
				}
			} else {
				if (JorlleMediaScanner.isMedia(root.getName())) {
					handler.onFound(root);
				}
			}
		}
	}
	
	
	
	
	
	
	
	
//	private class DbHandler extends Handler {
//		
//		DbHandler(Looper looper) {
//			super(looper);
//		}
//		
//		static final int BEGIN = 1;
//		static final int COMMIT = 2;
//		static final int END = 3;
//		static final int QUIT = 4;
//		@Override
//		public void handleMessage(Message msg) {
//			switch (msg.what) {
//			case BEGIN:
//				d("BEGIN");
//				mDb.beginTransaction();
//				break;
//			case COMMIT:
//				d("COMMIT");
//				mDb.setTransactionSuccessful();
//				break;
//			case END:
//				d("END");
//				mDb.endTransaction();
//				break;
//			case QUIT:
//				d("QUIT");
//				((HandlerThread) msg.obj).quit();
//				break;
//			}
//			d("    done");
//		}
//	}
//	
//	public synchronized long sendLocalIndexes(final boolean resendAll) throws IOException {
//		final long[] next = new long[1];
//		mThrown = null;
//		JsMediaAuth.Credential _credential = mAuth.loadCredential();
//		if (_credential == null) {
//			Map<String, String> given = mJsMedia.createAccount();
//			mAuth.saveCredential(given.get("devid"), given.get("token"));
//			_credential = mAuth.loadCredential();
//		}
//		
//		final JsMediaAuth.Credential credential = _credential;
//		HandlerThread engine =
//				new HandlerThread(getClass().getSimpleName());
//		engine.setUncaughtExceptionHandler(this);
//		engine.start();
//		Handler handler = new DbHandler(engine.getLooper());
//		try {
//			handler.sendEmptyMessage(DbHandler.BEGIN);
//			try {
//				JorlleMediaScanner scanner =
//						new JorlleMediaScanner(engine.getLooper())
//							.scanNomedia(false).scanSecret(false).scanSubfolder(true);
//				try {
//					handler.post(new Runnable() {
//						@Override
//						public void run() {
//							d("INIT");
//							// メタデータの汚れを反映
//							applyMetadataDirtyToLocalSync(mDb, CLocalSync.METADATA);
//							
//							// ステータスをクリア
//							if (resendAll) {
//								d("    MODE RE-SEND ALL!");
//								deletePreviouse(mDb);
//							} else {
//								clearStatus(mDb, FILE_NOT_FOUND);
//							}
//							d("    done");
//						}
//					});
//					
//final PrintWriter w = new PrintWriter("/sdcard/jsphoto.locals");
//					// 全ローカルメディアをスキャニング
//					scanner.findMedia(new JorlleMediaScanner.OnFoundListener() {
//						
//						@Override
//						public void onFound(File file) {
//							w.println(file.getAbsolutePath());
//							if (mThrown != null) {
//								return;
//							}
//							
//							d("* FOUND - %s", file.getAbsolutePath());
//							final String dirpath = file.getParent();
//							final String name = file.getName();
//							Cursor cPrev = mDb.query(CLocalSync.$TABLE,
//									new String[] {CLocalSync.METADATA, CLocalSync.PREV_METADATA, CLocalSync.METADATA_NEW},
//									CLocalSync.DIRPATH + " = ? AND " + CLocalSync.NAME + " = ?",
//									new String[] {dirpath, name},
//									null, null, null, "1");
//							try {
//								Cursor cSync = mDb.query(CMediaSync.$TABLE,
//										new String[] {CMediaSync.MEDIA_ID},
//										CMediaSync.DIRPATH + " = ? AND " + CMediaSync.NAME + " = ?",
//										new String[] {dirpath, name},
//										null, null, null, "1");
//								try {
//									ContentValues values = new ContentValues();
//									values.put(CLocalSync.DIRPATH, dirpath);
//									values.put(CLocalSync.NAME, name);
//									
//									if (cPrev.moveToFirst() && cPrev.getInt(2) != 1) {
//										Long metadata = cPrev.isNull(0) ? null : cPrev.getLong(0);
//										Long prevMetadata = cPrev.isNull(1) ? null : cPrev.getLong(1);
//										if (cSync.moveToFirst()) {
//											// 前回存在して、いま双方向 -> 削除として送る
//											d("  前回存在して、いま双方向 -> 削除として送る");
//											values.put(CLocalSync.STATUS, DELETE);
//										} else {
//											// 前回存在して、いま双方向でない -> 変更されていれば送る（更新）
//											d("  前回存在して、いま双方向でない -> 変更されていれば送る（更新）");
//											if ((metadata != null && prevMetadata != null && !metadata.equals(prevMetadata))
//													|| !(metadata == null && prevMetadata == null)) {
//												// 変更あり
//												d("     変更あり");
//												values.put(CLocalSync.STATUS, UPDATE);
//											} else {
//												// 変更なし
//												d("     変更なし");
//												values.put(CLocalSync.STATUS, KEEP);
//											}
//										}
//										values.put(CLocalSync.METADATA, metadata);
//									} else {
//										if (cSync.moveToFirst()) {
//											// 前回存在しなくて、いま双方向 -> なにもしない
//											d("  前回存在しなくて、いま双方向 -> なにもしない");
//											values = null;
//										} else {
//											// 前回存在しなくて、いま双方向でない -> 送る（新規）
//											d("  前回存在しなくて、いま双方向でない -> 送る（新規）");
//											values.put(CLocalSync.STATUS, UPDATE);
//											if (cPrev.isFirst()) {
//												values.put(CLocalSync.METADATA,
//														cPrev.isNull(0) ? null : cPrev.getLong(0));
//											}
//										}
//									}
//									
//									if (values != null) {
//										values.put(CLocalSync.PREV_METADATA, values.getAsLong(CLocalSync.METADATA));
//										values.put(CLocalSync.METADATA_NEW, 0);
//										mDb.insertWithOnConflict(CLocalSync.$TABLE, null,
//												values, SQLiteDatabase.CONFLICT_REPLACE);
//									}
//								} finally {
//									cSync.close();
//								}
//							} finally {
//								cPrev.close();
//							}
//							d("    done");
//						}
//						
//						@Override
//						public void onStartFolder(File folder) {
//							d("START FOLDER - %s", folder);
//						}
//						@Override
//						public void onEndFolder(File folder, int size) {
//							d("END FOLDER - %s", folder);
//						}
//						@Override
//						public void onComplete() {
//							d("SCAN COMPLETE");
//						}
//					});
//					
//					scanner.join();
//					w.flush();
//					w.close();
//				} finally {
//					scanner.dispose();
//				}
//				
//				handler.post(new Runnable() {
//					@Override
//					public void run() {
//						try {
//							if (mThrown != null) {
//								return;
//							}
//							
//							d("SEND");
//							final Cursor c = mDb.query(CLocalSync.$TABLE,
//									new String[] {
//											CLocalSync.DIRPATH,
//											CLocalSync.NAME,
//											CLocalSync.STATUS
//									},
//									CLocalSync.STATUS + " IN (?, ?, ?)",
//									new String[] {
//											String.valueOf(FILE_NOT_FOUND),
//											String.valueOf(UPDATE),
//											String.valueOf(DELETE)
//									},
//									null, null, null);
//							
//							IOIterator<Media> medias = new IOIterator<Media>() {
//								
//								Media mNext = prepareNext();
//								
//								Media prepareNext() {
//									if (c.moveToNext()) {
//										int status = c.getInt(2);
//										String dirpath = c.getString(0);
//										String name = c.getString(1);
//										File file = new File(dirpath, name);
//										boolean delete =
//												status == FILE_NOT_FOUND || status == DELETE;
//										mNext = toMedia(credential.deviceId, file, delete);
//									} else {
//										mNext = null;
//									}
//									return mNext;
//								}
//								
//								@Override
//								public boolean hasNext() throws IOException {
//									return mNext != null;
//								}
//								
//								@Override
//								public Media next() throws IOException, NoSuchElementException {
//									if (!hasNext()) {
//										throw new NoSuchElementException();
//									}
//									try {
//										return mNext;
//									} finally {
//										prepareNext();
//									}
//								}
//								
//								@Override
//								public void terminate() throws IOException {
//									c.close();
//								}
//							};
//							
//							mJsMedia.updateLocalMediaIndices(medias, next);
//							
//							mDb.delete(CLocalSync.$TABLE,
//									CLocalSync.STATUS + " IN (?, ?)",
//									new String[] {String.valueOf(FILE_NOT_FOUND), String.valueOf(DELETE)});
//							
//							mDb.setTransactionSuccessful();
//							d("    done");
//						} catch (Exception e) {
//							d("FAILED", e);
//							throw new ExceptionWrapper(e);
//						}
//					}
//				});
//				
////				handler.sendEmptyMessage(DbHandler.COMMIT);
//			} finally {
//				handler.sendEmptyMessage(DbHandler.END);
//			}
//		} finally {
//			Message message = handler.obtainMessage(DbHandler.QUIT, engine);
//			handler.sendMessage(message);
//			try {
//				engine.join();
//			} catch (InterruptedException e) {
//				;
//			}
//			
//			throwUncaught();
//		}
//		
//		return next[0];
//	}
//
//	private Throwable mThrown;
//	@Override
//	public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
//		if (paramThrowable instanceof ExceptionWrapper) {
//			mThrown = paramThrowable.getCause();
//		} else {
//			mThrown = paramThrowable;
//		}
////		mThrown.printStackTrace();		/*$debug$*/
//	}
//	
//	private void throwUncaught() throws IOException {
//		if (mThrown != null) {
//			if (mThrown instanceof IOException) {
//				throw (IOException) mThrown;
//			} else if (mThrown instanceof RuntimeException) {
//				throw (RuntimeException) mThrown;
//			} else {
//				throw new RuntimeException(mThrown);
//			}
//		}
//	}
//	
//	class ExceptionWrapper extends RuntimeException {
//		private static final long serialVersionUID = 1L;
//		ExceptionWrapper(Throwable t) {
//			super(t);
//		}
//	}
	
	protected void d(String format, Object... args) {
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			try {		/*$debug$*/
				Log.d(tag, String.format(format, args));		/*$debug$*/
			} catch (RuntimeException e) {		/*$debug$*/
				Log.d(tag, "format:" + format + " args:" + Arrays.toString(args));		/*$debug$*/
			}		/*$debug$*/
		}		/*$debug$*/
	}
	
	protected void d(String format, Throwable t, Object... args) {
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			try {		/*$debug$*/
				Log.d(tag, String.format(format, args), t);		/*$debug$*/
			} catch (RuntimeException e) {		/*$debug$*/
				e.printStackTrace();		/*$debug$*/
				Log.d(tag, "format:" + format + " args:" + Arrays.toString(args), t);		/*$debug$*/
			}		/*$debug$*/
		}		/*$debug$*/
	}
}
