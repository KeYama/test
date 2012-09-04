package jp.co.johospace.jsphoto.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.co.johospace.jsphoto.accessor.MediaIndexesAccessor;
import jp.co.johospace.jsphoto.accessor.MediaMetaDataAccessor;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.grid.ExtUtil;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.webkit.MimeTypeMap;

/**
 * メディア関連ユーティリティ
 */
public class MediaUtil {
	private MediaUtil() {}
	private static final String tag = MediaUtil.class.getSimpleName();
	
	/**
	 * メディアスキャナにメディアファイルをスキャンさせます。
	 * <p>targetがディレクトリの場合、ディレクトリ階層を再帰的にスキャンします。
	 * @param context コンテキスト
	 * @param target スキャンターゲット
	 * @param waitCompletion スキャン完了を待つ場合true。その場合、このメソッドの呼び出しはブロックされます。
	 */
	public static void scanMedia(final Context context, File target, final boolean waitCompletion) {
		if (waitCompletion
				&& Thread.currentThread() == context.getMainLooper().getThread()) {
			// メディアスキャナはメインスレッド上で動作する。
			// メインスレッド上でスキャナを待つとデッドロックするので、fail-fastする。
			throw new IllegalArgumentException(
					"you cannot wait completion because calling #scanMedia() on main thread.");
		}
				
		// スキャンターゲットを列挙
		final List<File> targets = IOUtil.listFiles(target);
		
		// 別スレッドでスキャンする
		Thread engine = new Thread(new Runnable() {
			private final Object mConnectionLock = new Object();
			private final Object mScanLock = new Object();
			
			@Override
			public void run() {
				// メディアスキャナ接続を生成
				MediaScannerConnection scanner = new MediaScannerConnection(context,
						new MediaScannerConnection.MediaScannerConnectionClient() {
								
								@Override
								public void onMediaScannerConnected() {
//									Log.d(tag, "Connected to mediascanner.");		/*$debug$*/
									synchronized (mConnectionLock) {
										mConnectionLock.notifyAll();
									}
								}
					
								@Override
								public void onScanCompleted(String path, Uri uri) {
//									Log.d(tag, String.format("Scan completed.[%s -> %s]", path, uri));		/*$debug$*/
									if (waitCompletion) {
										synchronized (mScanLock) {
											mScanLock.notifyAll();
										}
									}
								}
				});
				
				// メディアスキャナサービスに接続要求して、接続完了を待つ
				synchronized (mConnectionLock) {
					scanner.connect();
//					Log.d(tag, "Request connection to MediaScanner.");		/*$debug$*/
//					Log.d(tag, "Waiting for connection....");		/*$debug$*/
					try {
						mConnectionLock.wait(60000L);
					} catch (InterruptedException e) {
						return;
					}
					if (!scanner.isConnected()) {
						// 1分間待って接続できない
//						throw new IllegalStateException("cannot connect to MediaScanner!");
//						Log.e(tag, "cannot connect to MediaScanner!");		/*$debug$*/
					}
				}
				
				try {
					// ターゲットのメディアを、完了を待ちながらスキャンする
					for (File target : targets) {
						if (!target.exists()) {
							continue;
						}
						
						synchronized (mScanLock) {
							scanner.scanFile(target.getAbsolutePath(), null);
//							Log.d(tag, String.format("Requested mediascanner to scan %s.", target.getAbsolutePath()));		/*$debug$*/
							if (waitCompletion) {
//								Log.d(tag, "Waiting for the completion of scan....");		/*$debug$*/
								try {
									mScanLock.wait(10000L);
								} catch (InterruptedException e) {
									continue;
								}
							}
						}
					}
				} finally {
					scanner.disconnect();
				}
			}
		});
		
		engine.start();
		if (waitCompletion) {
			try {
				engine.join();
			} catch (InterruptedException e) {
				return;
			}
		}
	}
	
	/**
	 * 渡されたパスのボリューム名を返します。
	 * @param path パス
	 * @return ボリューム名
	 */
	public static String volumeNameForPath(String path) {
		return path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath()) ? "external" : "internal";
	}
	
	/**
	 * MediaStoreに問い合わせて、渡されたパスにあるメディアのIDを取得します。
	 * @param context コンテキスト
	 * @param path メディアパス
	 * @return メディアID。MediaStoreにない場合null。
	 */
	public static Long getAudioMediaId(Context context, String path) {
		String volumeName = volumeNameForPath(path);
		Cursor c =
			context.getContentResolver().query(
					Audio.Media.getContentUri(volumeName),
					new String[] {Audio.Media._ID},
					Audio.Media.DATA + " = ?",
					new String[] {path},
					null);
		try {
			if (c.moveToNext()) {
				return c.getLong(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * MediaStoreに問い合わせて、渡されたIDに該当するメディアのパスを取得します。
	 * @param context コンテキスト
	 * @param volumeName ボリューム名
	 * @param mediaId メディアID
	 * @return メディアのパス。MediaStoreにない場合null。
	 */
	public static String getAudioMediaPath(Context context, String volumeName, long mediaId) {
		Cursor c =
			context.getContentResolver().query(
					Audio.Media.getContentUri(volumeName),
					new String[] {Audio.Media.DATA},
					Audio.Media._ID + " = ?",
					new String[] {String.valueOf(mediaId)},
					null);
		try {
			if (c.moveToNext()) {
				return c.getString(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * MediaStoreに問い合わせて、渡されたパスにあるメディアのIDを取得します。
	 * @param context コンテキスト
	 * @param path メディアパス
	 * @return メディアID。MediaStoreにない場合null。
	 */
	public static Long getVideoMediaId(Context context, String path) {
		String volumeName = volumeNameForPath(path);
		Cursor c =
			context.getContentResolver().query(
					Video.Media.getContentUri(volumeName),
					new String[] {Video.Media._ID},
					Video.Media.DATA + " = ?",
					new String[] {path},
					null);
		try {
			if (c.moveToNext()) {
				return c.getLong(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * MediaStoreに問い合わせて、渡されたIDに該当するメディアのパスを取得します。
	 * @param context コンテキスト
	 * @param volumeName ボリューム名
	 * @param mediaId メディアID
	 * @return メディアのパス。MediaStoreにない場合null。
	 */
	public static String getVideoMediaPath(Context context, String volumeName, long mediaId) {
		Cursor c =
			context.getContentResolver().query(
					Video.Media.getContentUri(volumeName),
					new String[] {Video.Media.DATA},
					Video.Media._ID + " = ?",
					new String[] {String.valueOf(mediaId)},
					null);
		try {
			if (c.moveToNext()) {
				return c.getString(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * MediaStoreに問い合わせて、渡されたパスにあるメディアのIDを取得します。
	 * @param context コンテキスト
	 * @param path メディアパス
	 * @return メディアID。MediaStoreにない場合null。
	 */
	public static Long getImageMediaId(Context context, String path) {
		String volumeName = volumeNameForPath(path);
		Cursor c =
			context.getContentResolver().query(
					Images.Media.getContentUri(volumeName),
					new String[] {Images.Media._ID},
					Images.Media.DATA + " = ?",
					new String[] {path},
					null);
		try {
			if (c.moveToNext()) {
				return c.getLong(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * MediaStoreに問い合わせて、渡されたIDに該当するメディアのパスを取得します。
	 * @param context コンテキスト
	 * @param volumeName ボリューム名
	 * @param mediaId メディアID
	 * @return メディアのパス。MediaStoreにない場合null。
	 */
	public static String getImageMediaPath(Context context, String volumeName, long mediaId) {
		Cursor c =
			context.getContentResolver().query(
					Images.Media.getContentUri(volumeName),
					new String[] {Images.Media.DATA},
					Images.Media._ID + " = ?",
					new String[] {String.valueOf(mediaId)},
					null);
		try {
			if (c.moveToNext()) {
				return c.getString(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	
	/**
	 * 指定されたフォルダに.nomediaファイルを配置し、隠しフォルダにします
	 * 
	 * @param folderPath	隠しフォルダに設定するパス
	 * @throws IOException
	 * @return	true:作成成功 false:作成失敗
	 */
	public static boolean setNomedia(String folderPath) throws IOException {
		// .nomediaファイル作成
		File nomedia = new File(folderPath, ApplicationDefine.NO_MEDIA);
		
		OutputStream os = null;
		
		boolean result = false;
		
		try {
			// フォルダに.nomediaファイルを配置
			os = new FileOutputStream(nomedia);
			os.flush();
			
			result = true;
			
		} catch (FileNotFoundException e) {
//			e.printStackTrace();		/*$debug$*/
		} finally {
			if (os != null) {
				os.close();
			}
		}
		
		return result;
	}
	
	/**
	 * フォルダが.nomediaファイルを保持しているかどうかの可否を返します
	 * 
	 * @param folder	検索対象フォルダ
	 * @return			true:存在する false:存在しない
	 */
	public static boolean isContainsNomedia(File folder) {
		
		File[] files = folder.listFiles();
		
		if (files == null) return true;
		
		File nomedia = new File(folder.getPath(), ApplicationDefine.NO_MEDIA);
		List<File> array = Arrays.asList(files);
		
		return array.contains(nomedia);
	}

	/**
	 * フォルダがシークレットかどうかの可否を返します
	 * 
	 * @param folder			検索対象フォルダ
	 * @param containsDotFile	true:ドットファイルを含める	false：ドットファイルを含めない
	 * @return					true:シークレット	false:通常フォルダ
	 */
	public static boolean checkSecret(File folder, boolean containsDotFile) {
		
		File[] files = folder.listFiles();
		boolean result = false;
		
		String path;
		
		// フォルダ内のファイルをチェック
		for (File file : files) {
			if (file.isDirectory()) continue;

			path = file.getName();

			// ドットファイルは無視する
			if (!containsDotFile) {
				if(".".equals(path.substring(0, 1))){
					continue;
				}
			}

			if(path.endsWith(ApplicationDefine.SECRET)) {
				result = true;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * フォルダ内部のメディアファイルに対してシークレット処理を行い、関連情報を書き換えます
	 * 
	 * @param db			データベース
	 * @param path			対象フォルダ
	 * @param isSetSecret	true:シークレット設定	false:シークレット解除
	 * @param containsDotFile	true:ドットファイルを含める	false：ドットファイルを含めない
	 * @param callback		コールバック用インターフェース
	 */
	public static boolean changeMediaSecret(SQLiteDatabase db, String path, boolean isSetSecret, boolean containsDotFile) {

		boolean result = false;
		
		File file = new File(path);
		File[] fileList = file.listFiles();
		
		try {
			// 指定したパス以下の階層を、すべて処理
			for (File media : fileList) {

				// ドットファイルは無視する
				if (!containsDotFile) {
					if(".".equals(media.getName().substring(0, 1))){
						continue;
					}
				}
				
				// 自身を再帰呼び出しし、内部の階層も処理する
				if (media.isDirectory()) {
					changeMediaSecret(db, media.getPath(), isSetSecret, containsDotFile);
					continue;
				}
				
				// メディアファイル以外は無視する
				String mime = MediaUtil.getMimeTypeFromPath(media.getPath());
				
				// 拡張子が「.secret」ではなく、かつメディアファイル以外のファイルだった場合、無視
				if (!media.getName().endsWith(ApplicationDefine.SECRET)) {
					if (mime == null || (!mime.startsWith("image/") && !mime.startsWith("video/"))) {
						continue;
					}
				}
				
				String mediaPath = media.getPath();
				String mediaParent = media.getParent();
				String mediaName = media.getName();
				
				String newPath;
	
				// 隠しフォルダ設定
				if (isSetSecret) {
					
					// 既にシークレットのものは、無視する
					if (mediaName.endsWith(ApplicationDefine.SECRET)) continue;
					
					newPath = mediaPath + ApplicationDefine.SECRET;
					
				// 隠しフォルダ解除
				} else {
					
					// シークレット出ないものは、無視する
					if (!mediaName.endsWith(ApplicationDefine.SECRET)) continue; 
					
					newPath = mediaParent + "/" + mediaName.replaceAll(ApplicationDefine.SECRET, "");
				}

				File newFile = new File(newPath);
				
				// ファイル名が変更できたら、インデックス、メタデータの情報を更新
				if (media.renameTo(newFile)) {
					MediaIndexesAccessor.updateMediaData(db, OpenHelper.cache.getDatabase(), mediaParent, mediaName, newFile.getName());
				}
			}
			result = true;
			
		} catch (Exception e) {
//			e.printStackTrace();		/*$debug$*/
		}
		
		return result;
	}

	
	/**
	 * 画像のシークレット状態を操作します
	 * 
	 * @param isSecret true:シークレットに設定	false:シークレット解除
	 */
	public static List<String> setSecret(SQLiteDatabase db, boolean isSecret, 
								String targetPath, boolean isMultiMode, List<String> selectedList) {
		
		List<String> pathList = null;
		
		// 単体・複数選択の場合で、対象リストを作りかえる
		if(isMultiMode && selectedList != null){
			pathList = selectedList;
		}else{
			pathList = new ArrayList<String>();
			pathList.add(targetPath);
		}
		
		String message = null;
		
		List<String> newNameList = new ArrayList<String>();
		
		for(String path: pathList){

			File src = new File(path);
			File dest = src;
			
			if (isSecret) {
				// 通常ファイルをシークレットに
				if(!ExtUtil.isSecret(src)) dest = ExtUtil.toSecret(src);
				
			} else {
				// シークレットを通常ファイルに
				if(ExtUtil.isSecret(src)) dest = ExtUtil.unSecret(src);
			}
			
			String oldName = src.getName();
			String newName = dest.getName();
			
			if(!src.equals(dest)) {
				src.renameTo(dest);
			
				// 成功したら、DBも更新
				MediaMetaDataAccessor.updateMetaDataName(db, src.getParent(), oldName, newName);
				MediaIndexesAccessor.updateIndexesName(OpenHelper.cache.getDatabase(), src.getParent(), oldName, newName);
				
				newNameList.add(dest.getAbsolutePath());
			}
		}
		
		return newNameList;
	}
	
	
	/**
	 * 指定したフォルダ内のメディアファイルと、メディア情報をすべて削除します
	 * 
	 * @param db		データベース
	 * @param path		フォルダパス
	 * @param containsDotFile	true:ドットファイルを含める	false：ドットファイルを含めない
	 * @return			true:削除成功　false:削除失敗
	 */
	public static boolean deleteAllMedia(SQLiteDatabase db, String path, boolean containsDotFile) {

		boolean result = false;
		
		File file = new File(path);
		File[] fileList = file.listFiles();
		
		int deleteCount = 0;

		try {
			// 指定したパス以下の階層を、すべて処理
			for (File media : fileList) {

				// ディレクトリならば無視
				if (media.isDirectory()) {
					continue;
				}
				
				// メディアファイル以外は無視する
				String mime = MediaUtil.getMimeTypeFromPath(media.getPath());
				
				// 拡張子が「.secret」かつメディアファイル以外のファイルだった場合、無視
				if (!media.getName().endsWith(ApplicationDefine.SECRET)) {
					if (mime == null || (!mime.startsWith("image/") && !mime.startsWith("video/"))) {
						continue;
					}
				}

				// 隠しフォルダを表示しない場合はドットファイルを無視
				if (!containsDotFile) {
					// ドットファイルは無視する
					if(".".equals(media.getName().substring(0, 1))){
						continue;
					}
				}

				// メディアを削除し、カウントアップ
				if (media.delete()) {
					deleteCount++;
				}
			}
			
			if (deleteCount > 0) {
				// DBのデータを削除
				MediaIndexesAccessor.deleteMediaData(db, OpenHelper.cache.getDatabase(), path);
			}
			
			result = true;
			
		} catch (Exception e) {
//			e.printStackTrace();		/*$debug$*/
		}
		
		return result;
	}
	
	
	/**
	 * パスの拡張子からMIMEタイプを取得します。
	 * @param path パス
	 * @return MIMEタイプ。不明の場合null。
	 */
	public static String getMimeTypeFromPath(String path) {
		int index = path.lastIndexOf('.');
		if (0 <= index) {
			String ext = path.substring(index + 1).toLowerCase();
			return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
		} else {
			return null;
		}
	}
	
	/**
	 * MIMEタイプから、そのタイプの代表的な拡張子を返します。
	 * @param mimeType MIMEタイプ
	 * @return 代表的な拡張子
	 */
	public static String getTypicalExtensionFromMimeType(String mimeType) {
		if (mimeType == null) {
			return null;
		} else {
			return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
		}
	}
	
	
	/**
	 * メディアファイルの内容から拡張子を検出します。
	 * @param mediaFile メディアファイル
	 * @return 検出した拡張子。検出できない場合null。
	 */
	public static String detectExtension(File mediaFile) {
		BitmapFactory.Options op = new BitmapFactory.Options();
		op.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mediaFile.getAbsolutePath(), op);
		if (op.outMimeType != null) {
			return getTypicalExtensionFromMimeType(op.outMimeType);
		} else {
			return null;
		}
	}
}
