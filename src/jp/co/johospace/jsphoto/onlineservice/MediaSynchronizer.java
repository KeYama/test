package jp.co.johospace.jsphoto.onlineservice;

import java.io.IOException;
import java.util.Arrays;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.database.CMediaMetadataDirty;
import jp.co.johospace.jsphoto.database.CMediaSync;
import jp.co.johospace.jsphoto.provider.JorlleProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

/**
 * メディア同期
 */
public abstract class MediaSynchronizer extends ContextWrapper {
	protected final String tag = getClass().getSimpleName();
	
	protected MediaSynchronizer(Context context) {
		super(context);
	}
	
	/**
	 * 双方向に同期します。
	 * @param db データベース
	 * @param client オンラインメディアサービスクライアント
	 * @param localDir ローカルディレクトリ
	 * @param remoteDirectoryID リモートディレクトリの識別子
	 * @param behavior 競合時の振る舞い
	 * @throws IOException 入出力例外発生時
	 */
	public void synchronize(SQLiteDatabase db, OnlineMediaServiceClient<? extends MediaDirectory, ? extends Media> client,
			String localDir, String remoteDirectoryID, BehaviorInConflict behavior) throws IOException {
		final LocalSyncStoreAccessor localStore =
				new LocalSyncStoreAccessor(this, client.getServiceType(), client.getServiceAccount());
		db.beginTransaction();
		try {
			localStore.clearSyncStatus(db, localDir, STATUS_NOT_PROCESSED);
			localStore.clearSyncCache(db, localDir);
			applyMetadataDirty(db, localDir);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		doDownload(db, client, localDir, remoteDirectoryID, behavior);
		doUpload(db, client, localDir, remoteDirectoryID, behavior);
		
		deleteNotProcessed(db, client, localDir, remoteDirectoryID);
		
		localStore.extractDirtiesToSendAsUpdates(db, localDir,
				STATUS_PROCESSED_TOOK_LOCAL, STATUS_PROCESSED_TOOK_REMOTE);
		
//		scanMedia(localDir);
	}
	
	/**
	 * 下り方向に同期します。
	 * @param db データベース
	 * @param client オンラインメディアサービスクライアント
	 * @param localDir ローカルディレクトリ
	 * @param remoteDirectoryID リモートディレクトリの識別子
	 * @param behavior 競合時の振る舞い
	 * @throws IOException 入出力例外発生時
	 */
	public void download(SQLiteDatabase db, OnlineMediaServiceClient<? extends MediaDirectory, ? extends Media> client,
			String localDir, String remoteDirectoryID, BehaviorInConflict behavior) throws IOException {
		final LocalSyncStoreAccessor localStore =
				new LocalSyncStoreAccessor(this, client.getServiceType(), client.getServiceAccount());
		db.beginTransaction();
		try {
			localStore.clearSyncStatus(db, localDir, STATUS_NOT_PROCESSED);
			localStore.clearSyncCache(db, localDir);
			applyMetadataDirty(db, localDir);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		doDownload(db, client, localDir, remoteDirectoryID, behavior);
		
		deleteNotProcessed(db, client, localDir, remoteDirectoryID);
		
		localStore.extractDirtiesToSendAsUpdates(db, localDir,
				STATUS_PROCESSED_TOOK_LOCAL, STATUS_PROCESSED_TOOK_REMOTE);
		
//		scanMedia(localDir);
	}
	
	/**
	 * 上り方向に同期します。
	 * @param db データベース
	 * @param client オンラインメディアサービスクライアント
	 * @param localDir ローカルディレクトリ
	 * @param remoteDirectoryID リモートディレクトリの識別子
	 * @param behavior 競合時の振る舞い
	 * @throws IOException 入出力例外発生時
	 */
	public void upload(SQLiteDatabase db, OnlineMediaServiceClient<? extends MediaDirectory, ? extends Media> client,
			String localDir, String remoteDirectoryID, BehaviorInConflict behavior) throws IOException {
		final LocalSyncStoreAccessor localStore =
				new LocalSyncStoreAccessor(this, client.getServiceType(), client.getServiceAccount());
		db.beginTransaction();
		try {
			localStore.clearSyncStatus(db, localDir, STATUS_NOT_PROCESSED);
			localStore.clearSyncCache(db, localDir);
			applyMetadataDirty(db, localDir);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		doUpload(db, client, localDir, remoteDirectoryID, behavior);
		
		deleteNotProcessed(db, client, localDir, remoteDirectoryID);
		
		localStore.extractDirtiesToSendAsUpdates(db, localDir,
				STATUS_PROCESSED_TOOK_LOCAL, STATUS_PROCESSED_TOOK_REMOTE);
	}
	
	/**
	 * 未処理のものを削除します。
	 * @param db データベース
	 * @param client オンラインメディアサービスクライアント
	 * @param localDir ローカルディレクトリ
	 * @param remoteDirectoryID リモートディレクトリの識別子
	 * @throws IOException 入出力例外発生時
	 */
	protected void deleteNotProcessed(SQLiteDatabase db, OnlineMediaServiceClient<? extends MediaDirectory, ? extends Media> client,
			String localDir, String remoteDirectoryID) throws IOException {
		final LocalSyncStoreAccessor localStore =
				new LocalSyncStoreAccessor(this, client.getServiceType(), client.getServiceAccount());
		TerminatableIterator<LocalMedia> notProcesseds =
				localStore.queryLocalMediaAt(db, localDir, STATUS_NOT_PROCESSED);
		while (notProcesseds.hasNext()) {
			final LocalMedia media = notProcesseds.next();
			d("delete as not processed. %s/%s", media.dirpath, media.name);
			localStore.deleteLocalMedia(db, media.dirpath, media.name);
			if (media.syncCache != null) {
				d("    remote is also to delete. %s", media.sync.mediaID);
				client.deleteMedia(media.sync);
			}
		}
	}
	
	/**
	 * メタデータの汚れを反映します。
	 * @param db データベース
	 * @param localDir ローカルディレクトリ
	 */
	protected void applyMetadataDirty(SQLiteDatabase db, String localDir) {
		Uri uri = JorlleProvider.getUriFor(getApplicationContext(), new String[] {CMediaMetadataDirty.$TABLE});
		Cursor c = getContentResolver().query(uri,
				new String[] {CMediaMetadataDirty.NAME, CMediaMetadataDirty.UPDATED_TIME},
				CMediaMetadataDirty.DIRPATH + " = ?", new String[] {localDir}, null);
		try {
			ContentValues values = new ContentValues();
			while (c.moveToNext()) {
				String name = c.getString(0);
				long time = c.getLong(1);
				values.put(CMediaSync.LOCAL_METADATA_TIMESTAMP, time);
				db.update(CMediaSync.$TABLE, values,
						CMediaSync.DIRPATH + " = ?" +
								" AND " + CMediaSync.NAME + " = ?",
						new String[] {localDir, name});
				getContentResolver().delete(uri,
						CMediaMetadataDirty.DIRPATH + " = ?" +
								" AND " + CMediaMetadataDirty.NAME + " = ?",
						new String[] {localDir, name});
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * 下り方向に同期します。
	 * @param db データベース
	 * @param client オンラインメディアサービスクライアント
	 * @param localDir ローカルディレクトリ
	 * @param remoteDirectoryID リモートディレクトリの識別子
	 * @param behavior 競合時の振る舞い
	 * @throws IOException 入出力例外発生時
	 */
	protected abstract void doDownload(SQLiteDatabase db, OnlineMediaServiceClient<? extends MediaDirectory, ? extends Media> client,
			String localDir, String remoteDirectoryID, BehaviorInConflict behavior) throws IOException;
	
	protected void scanMedia(String localDir) {
		sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
	}

	/**
	 * 上り方向に同期します。
	 * @param db データベース
	 * @param client オンラインメディアサービスクライアント
	 * @param localDir ローカルディレクトリ
	 * @param remoteDirectoryID リモートディレクトリの識別子
	 * @param behavior 競合時の振る舞い
	 * @throws IOException 入出力例外発生時
	 */
	protected abstract void doUpload(SQLiteDatabase db, OnlineMediaServiceClient<? extends MediaDirectory, ? extends Media> client,
			String localDir, String remoteDirectoryID, BehaviorInConflict behavior) throws IOException;
	
	/** 競合時の振る舞い */
	public static enum BehaviorInConflict {
		/** 競合したままにします */
		KEEP_CONFLICT,
		/** リモートを採用します */
		TAKE_REMOTE,
		/** ローカルを採用します */
		TAKE_LOCAL,
		/** 最新を採用します */
		TAKE_LATEST,
	}
	
	/** 同期ステータス： 未処理 */
	public static final int STATUS_NOT_PROCESSED = 0;
	/** 同期ステータス： 下り方向の同期処理待ち */
	public static final int STATUS_WAITING_DOWNLOAD = 1;
	/** 同期ステータス： 上り方向の同期処理待ち */
	public static final int STATUS_WAITING_UPLOAD = 2;
	/** 同期ステータス： 競合未解決で完了 */
	public static final int STATUS_CONFLICT_UNRESOLVED = 3;
	/** 同期ステータス： リモートを採用して完了 */
	public static final int STATUS_PROCESSED_TOOK_REMOTE = 4;
	/** 同期ステータス： ローカルを採用して完了 */
	public static final int STATUS_PROCESSED_TOOK_LOCAL = 5;
	/** 同期ステータス： 同期の必要がなく完了 */
	public static final int STATUS_PROCESSED_NOT_NECESSARY = 6;
	/** 同期ステータス： 同期に失敗 */
	public static final int STATUS_FAILED = -1;
	
	protected void d(String format, Object... args) {		/*$debug$*/
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			try {		/*$debug$*/
				Log.d(tag, String.format(format, args));		/*$debug$*/
			} catch (RuntimeException e) {		/*$debug$*/
				Log.d(tag, "format:" + format + " args:" + Arrays.toString(args));		/*$debug$*/
			}		/*$debug$*/
		}		/*$debug$*/
	}		/*$debug$*/
	
	protected void d(String format, Throwable t, Object... args) {		/*$debug$*/
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			try {		/*$debug$*/
				Log.d(tag, String.format(format, args), t);		/*$debug$*/
			} catch (RuntimeException e) {		/*$debug$*/
				e.printStackTrace();		/*$debug$*/
				Log.d(tag, "format:" + format + " args:" + Arrays.toString(args), t);		/*$debug$*/
			}		/*$debug$*/
		}		/*$debug$*/
	}		/*$debug$*/

}
