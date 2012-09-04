package jp.co.johospace.jsphoto.provider;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.cache.ImageCache;
import jp.co.johospace.jsphoto.cache.ImageCache.ImageData;
import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.CMediaMetadataDirty;
import jp.co.johospace.jsphoto.database.CMediaSync;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.util.StringPair;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

/**
 * メディア管理 プロバイダ
 */
public class JorlleProvider extends ContentProvider {

	/** 列名： サムネイル */
	public static final String COLUMN_THUMB = CMediaIndex.THUMBNAIL;
	
	/** MIMEタイプ： サムネイル */
	public static final String TYPE_THUMB =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.jp.co.johospace.jsphoto.thumb";
	/** MIMEタイプ： メタデータ */
	public static final String TYPE_METADATA =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.jp.co.johospace.jsphoto." + CMediaMetadata.$TABLE;
	/** MIMEタイプ： メタデータ汚れ */
	public static final String TYPE_METADATA_DIRTY =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.jp.co.johospace.jsphoto." + CMediaMetadataDirty.$TABLE;
	
	private static final int THUMB_SYNCMEDIA = 1;
	private static final String PATH_THUMB_SYNCMEDIA = "thumb/syncmedia/*/*/*";
	private static final int METADATA = 2;
	private static final String PATH_METADATA = CMediaMetadata.$TABLE;
	private static final int REPLACE_METADATA = 3;
	private static final String PATH_REPLACE_METADATA = CMediaMetadata.$TABLE + "/*/*/*";
	private static final int METADATA_DIRTY = 4;
	private static final String PATH_METADATA_DIRTY = CMediaMetadataDirty.$TABLE;
	
	private static UriMatcher sMatcher;
	
	@Override
	public final void attachInfo(Context context, ProviderInfo info) {
		super.attachInfo(context, info);
		if (sMatcher == null) {
			UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
			matcher.addURI(info.authority, PATH_THUMB_SYNCMEDIA, THUMB_SYNCMEDIA);
			matcher.addURI(info.authority, PATH_METADATA, METADATA);
			matcher.addURI(info.authority, PATH_REPLACE_METADATA, REPLACE_METADATA);
			matcher.addURI(info.authority, PATH_METADATA_DIRTY, METADATA_DIRTY);
			sMatcher = matcher;
		}
	}

	@Override
	public String getType(Uri uri) {
		int match = sMatcher.match(uri);
		switch (match) {
		case THUMB_SYNCMEDIA:
			return TYPE_THUMB;
		case METADATA:
		case REPLACE_METADATA:
			return TYPE_METADATA;
		case METADATA_DIRTY:
			return TYPE_METADATA_DIRTY;
		}
		
		return null;
	}
	
	@Override
	public boolean onCreate() {
		upgradeDatabaseFromBeta();
		OpenHelper.external.initialize();
		OpenHelper.cache.initialize();
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int match = sMatcher.match(uri);
		switch (match) {
		case THUMB_SYNCMEDIA: {
			List<String> segments = uri.getPathSegments();
			if (segments.size() == 5) {
				return queryThumbSyncMedia(
						segments.get(2), segments.get(3), segments.get(4));
			}
			break;
		}
		
		case METADATA:
		case METADATA_DIRTY: {
			List<String> segments = uri.getPathSegments();
			if (segments.size() == 1) {
				String table = segments.get(0);
				return OpenHelper.external.getDatabase().query(
						table, projection, selection, selectionArgs, null, null, sortOrder);
			}
		}
		}
		
		return null;
	}
	
	private Cursor queryThumbSyncMedia(String serviceType, String serviceAccount, String mediaId) {
		try {
			Uri uri = JorlleSyncProvider.getUriFor(
					getContext(), JorlleSyncProvider.PATH_MEDIASYNC);
			Cursor c = getContext().getContentResolver().query(uri,
					new String[] {CMediaSync.DIRPATH, CMediaSync.NAME},
					CMediaSync.SERVICE_TYPE + " = ?" +
							" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
							" AND " + CMediaSync.MEDIA_ID + " = ?",
					new String[] {serviceType, serviceAccount, mediaId}, null);
			try {
				if (c.moveToFirst()) {
					String dirpath = c.getString(0);
					String name = c.getString(1);
					ImageData data = new ImageData();
					ImageCache.getImageCache(dirpath, name, data);
					
					return OpenHelper.cache.getDatabase().query(CMediaIndex.$TABLE,
							new String[] {CMediaIndex.THUMBNAIL},
								CMediaIndex.DIRPATH + " = ? AND " + CMediaIndex.NAME + " = ?",
							new String[] {dirpath, name},
							null, null, null);
					
				} else {
					return null;
				}
			} finally {
				c.close();
			}
			
		} catch (Exception e) {
//			Log.e(getClass().getSimpleName(), "failed to open image cache.", e);		/*$debug$*/
			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int match = sMatcher.match(uri);
		switch (match) {
		case METADATA:
		case METADATA_DIRTY: {
			List<String> segments = uri.getPathSegments();
			if (segments.size() == 1) {
				String table = segments.get(0);
				return OpenHelper.external.getDatabase().delete(table, selection, selectionArgs);
			} else {
				throw new IllegalArgumentException("path of table was not supplied.");
			}
		}
		}
		
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
//		System.out.println(String.format("bulkInsert(%s)", uri));		/*$debug$*/
		int match = sMatcher.match(uri);
		switch (match) {
		case REPLACE_METADATA: {
			List<String> segments = uri.getPathSegments();
			if (segments.size() == 4) {
				String silent = uri.getQueryParameter("silent");
				return replaceMetadata(
						segments.get(1), segments.get(2), "*".equals(segments.get(3)) ? null : segments.get(3), values, "true".equals(silent));
			}
			break;
		}
		}
		
		return super.bulkInsert(uri, values);
	}
	
	private int replaceMetadata(String dirpath, String name, String type, ContentValues[] values, boolean silent) {
		int effected = 0;
		SQLiteDatabase db = OpenHelper.external.getDatabase();
		
		Long prevTime;
		if (silent) {
			Cursor c = db.query(CMediaMetadataDirty.$TABLE,
					new String[] {CMediaMetadataDirty.UPDATED_TIME},
					CMediaMetadataDirty.DIRPATH + " = ?" +
							" AND " + CMediaMetadataDirty.NAME + " = ?",
					new String[] {dirpath, name},
					null, null, null);
			try {
				if (c.moveToFirst()) {
					prevTime = c.getLong(0);
				} else {
					prevTime = null;
				}
			} finally {
				c.close();
			}
		} else {
			prevTime = null;
		}
		
		db.beginTransaction();
		try {
			ArrayList<String> wheres = new ArrayList<String>();
			ArrayList<String> args = new ArrayList<String>();
			
			wheres.add(CMediaMetadata.DIRPATH + " = ?");
			args.add(dirpath);
			
			wheres.add(CMediaMetadata.NAME + " = ?");
			args.add(name);
			
			if (type != null) {
				wheres.add(CMediaMetadata.METADATA_TYPE + " = ?");
				args.add(type);
			}
			
			effected += db.delete(CMediaMetadata.$TABLE,
					TextUtils.join(" AND ", wheres),
					args.toArray(new String[0]));
			if (values != null) {
				for (ContentValues value : values) {
					if (type == null || type.equals(value.getAsString(CMediaMetadata.METADATA_TYPE))) {
						db.insertOrThrow(CMediaMetadata.$TABLE, null, value);
						effected++;
					}
				}
			}
			if (silent) {
				if (prevTime == null) {
					db.delete(CMediaMetadataDirty.$TABLE,
							CMediaMetadataDirty.DIRPATH + " = ?" +
									" AND " + CMediaMetadataDirty.NAME + " = ?",
							new String[] {dirpath, name});
				} else {
					ContentValues time = new ContentValues();
					time.put(CMediaMetadataDirty.UPDATED_TIME, prevTime);
					db.update(CMediaMetadataDirty.$TABLE, time,
							CMediaMetadataDirty.DIRPATH + " = ?" +
							" AND " + CMediaMetadataDirty.NAME + " = ?",
							new String[] {dirpath, name});
				}
			}
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		return effected;
	}
	
	public static Uri getUriFor(Context context, String[] pathSegments, StringPair... params) {
		String authority = context.getString(R.string.authority_provider);
		StringBuilder segments = null;
		for (String path : pathSegments) {
			if (segments == null) {
				segments = new StringBuilder();
			}
			segments.append("/");
			segments.append(URLEncoder.encode(path));
		}
		
		StringBuilder paramString = null;
		for (Pair<String, String> param : params) {
			if (paramString == null) {
				paramString = new StringBuilder();
				paramString.append("?");
			} else {
				paramString.append("&");
			}
			paramString.append(String.format("%s=%s",
					URLEncoder.encode(param.first), URLEncoder.encode(param.second)));
		}
		
		return Uri.parse(String.format("content://%s%s%s",
				authority,
				segments == null ? "" : segments.toString(),
				paramString == null ? "" : paramString.toString()));
	}
	
	private void upgradeDatabaseFromBeta() {
		// 旧DBファイル
		File oldDbDir = new File(getContext().getExternalCacheDir(), "database");
		File oldDbFile = new File(oldDbDir, "external.db");
		
		// 新DBファイル
		File newDbFile = OpenHelper.external.getDatabaseFile();
		
		if (oldDbFile.exists()
				&& !newDbFile.exists()) {
			
			// 旧DBをオープン
			SQLiteDatabase oldDb;
			try {
				oldDb = SQLiteDatabase.openDatabase(
							oldDbFile.getAbsolutePath(),
							OpenHelper.DEFAULT_FACTORY,
							SQLiteDatabase.OPEN_READWRITE);
				
			} catch (Exception e) {
				Log.e(JorlleProvider.class.getSimpleName(),
						"failed to open ancient database.", e);
				oldDbFile.delete();
				return;
			}
			
			try {
				// 新DBを作成
				SQLiteDatabase newDb;
				try {
					// バージョン1（初回正式リリース時）のスキーマで作成
					newDb = OpenHelper.createDatabase(getContext(),
							OpenHelper.external.getDatabaseFile(), "ddl/external.1.ddl", 1);
				} catch (Exception e) {
					Log.e(JorlleProvider.class.getSimpleName(),
							"failed to create new database.", e);
					oldDbFile.delete();
					newDbFile.delete();
					return;
				}
				
				newDb.beginTransaction();
				try {
					Cursor c;
					ContentValues values = new ContentValues();
					
					// メタデータ 移行
					c = oldDb.query("media_metadata",
							new String[] {
								"dirpath",
								"name",
								"metadata_type",
								"metadata",
								"update_timestamp",
							},
							null, null, null, null, null);
					try {
						while (c.moveToNext()) {
							values.clear();
							values.put("dirpath", c.getString(0));
							values.put("name", c.getString(1));
							values.put("metadata_type", c.getString(2));
							values.put("metadata", c.getString(3));
							values.put("update_timestamp", c.isNull(4) ? null : c.getLong(4));
							
							newDb.insertOrThrow("media_metadata", null, values);
						}
					} finally {
						c.close();
					}
					
					// シークレットフォルダー 移行
					c = oldDb.query("secret_folders",
							new String[] {
								"dirpath",
							},
							null, null, null, null, null);
					try {
						while (c.moveToNext()) {
							values.clear();
							values.put("dirpath", c.getString(0));
							
							newDb.insertOrThrow("secret_folders", null, values);
						}
					} finally {
						c.close();
					}
					
					// 移行でできたメタデータの汚れをクリア
					newDb.delete("media_metadata_dirty", null, null);
					
					// 以前のメタデータの汚れを移行
					c = oldDb.query("media_metadata_dirty",
							new String[] {
								"dirpath",
								"name",
								"updated_time",
							},
							null, null, null, null, null);
					try {
						while (c.moveToNext()) {
							values.clear();
							values.put("dirpath", c.getString(0));
							values.put("name", c.getString(1));
							values.put("updated_time", c.isNull(2) ? null : c.getLong(2));
							
							newDb.insertOrThrow("media_metadata_dirty", null, values);
						}
					} finally {
						c.close();
					}
					
					
					newDb.setTransactionSuccessful();
				} catch (Exception e) {
					Log.e(JorlleProvider.class.getSimpleName(),
							"couldn't save data in beta time.", e);
				} finally {
					try {
						newDb.endTransaction();
					} finally {
						newDb.close();
					}
				}
				
			} finally {
				oldDb.close();
			}
			
			oldDbFile.delete();
			
			// バックグラウンドでファイルキャッシュを全削除する
			new Thread() {
				@Override
				public void run() {
					File dir = new ExternalServiceCacheImpl(
							getContext()).getContentCacheDir();
					if (dir != null) {
						for (File child : dir.listFiles()) {
							delete(child);
						}
					}
				}
				
				void delete(File file) {
					if (file.isFile()) {
						file.delete();
					} else {
						for (File child : file.listFiles()) {
							delete(child);
						}
						file.delete();
					}
				}
			}.start();
		}
	}

}
