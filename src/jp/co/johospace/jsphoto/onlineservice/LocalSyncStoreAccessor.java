package jp.co.johospace.jsphoto.onlineservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.CMediaSync;
import jp.co.johospace.jsphoto.database.CMediaSyncToSend;
import jp.co.johospace.jsphoto.provider.JorlleProvider;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.StringPair;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;


/**
 * ローカルの同期ストアへのアクセサ
 */
public class LocalSyncStoreAccessor extends ContextWrapper {
	private static final String tag = LocalSyncStoreAccessor.class.getSimpleName();

	/** オンラインサービスタイプ */
	private final String mServiceType;

	/** オンラインサービスアカウント */
	private final String mServiceAccount;
	
	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 * @param serviceType 処理対象のサービスタイプ
	 * @param serviceAccount 処理対象のサービスアカウント
	 */
	public LocalSyncStoreAccessor(Context context, String serviceType, String serviceAccount) {
		super(context);
		mServiceType = serviceType;
		mServiceAccount = serviceAccount;
	}
	
	/**
	 * 渡されたディレクトリ内のメディアの同期完了状態をクリアします。
	 * @param db データベース
	 * @param dirpath 対象ディレクトリ
	 * @param status ステータス
	 */
	public void clearSyncStatus(SQLiteDatabase db, String dirpath, int status) {
		ContentValues values = new ContentValues();
		values.put(CMediaSync.SYNC_STATUS, status);
		db.update(CMediaSync.$TABLE, values,
				CMediaSync.SERVICE_TYPE + " = ?" +
						" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
						" AND " + CMediaSync.DIRPATH + " = ?",
				new String[] {mServiceType, mServiceAccount, dirpath});
	}
	
	/**
	 * 渡されたディレクトリのローカルメディアを問い合わせます。
	 * @param db データベース
	 * @param dirpath ディレクトリ
	 * @return ローカルメディア
	 */
	public TerminatableIterator<LocalMedia> queryLocalMediaAt(final SQLiteDatabase db, String dirpath) {
		File dir = new File(dirpath);
		final File[] files = dir.listFiles();
		return new TerminatableIterator<LocalMedia>() {
			
			int pos;
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public LocalMedia next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				
				File file = files[pos++];
				LocalMedia media =
						new LocalMedia(file.getParent(), file.getName());
				Cursor c = queryMediaSync(db, media.dirpath, media.name);
				try {
					if (c.moveToFirst()) {
						LocalMedia.createRowHandler(c).populateCurrentRow(c, media);
					}
				} finally {
					c.close();
				}
				media.metadata = getMetadata(db, media.dirpath, media.name);
				return media;
			}
			
			@Override
			public boolean hasNext() {
				return pos < files.length;
			}
			
			@Override
			public void terminate() throws IOException {
			}
		};
	}
	
	/**
	 * 渡された同期状態のローカルメディアを問い合わせます。
	 * @param db データベース
	 * @param dirpath ディレクトリ
	 * @param status 同期状態
	 * @return ローカルメディア
	 */
	public TerminatableIterator<LocalMedia> queryLocalMediaAt(final SQLiteDatabase db, String dirpath, Integer... status) {
		final Cursor c = db.query(CMediaSync.$TABLE, null,
				CMediaSync.SERVICE_TYPE + " = ?" +
						" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
						" AND " + CMediaSync.DIRPATH + " = ?" +
						" AND " + CMediaSync.SYNC_STATUS + " IN (" + TextUtils.join(",", status) + ")",
				new String[] {mServiceType, mServiceAccount, dirpath}, null, null, null);
		
		return new TerminatableIterator<LocalMedia>() {
			
			final RowHandler<LocalMedia> mHandler = LocalMedia.createRowHandler(c);
			final int INDEX_DIRPATH = c.getColumnIndex(CMediaSync.DIRPATH);
			final int INDEX_NAME = c.getColumnIndex(CMediaSync.NAME);
			
			LocalMedia mNext;
			
			{
				prepareNext();
			}
			
			void prepareNext() {
				if (c.moveToNext()) {
					mNext = new LocalMedia(
							c.getString(INDEX_DIRPATH), c.getString(INDEX_NAME));
					mHandler.populateCurrentRow(c, mNext);
					mNext.metadata = getMetadata(db, mNext.dirpath, mNext.name);
				} else {
					mNext = null;
				}
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public LocalMedia next() {
				try {
					return mNext;
				} finally {
					prepareNext();
				}
			}
			
			@Override
			public boolean hasNext() {
				return mNext != null;
			}
			
			@Override
			public void terminate() throws IOException {
				c.close();
			}
		};
	}
	
	/**
	 * リモートのメディアIDに該当するローカルメディアを問い合わせます。
	 * @param db データベース
	 * @param mediaID メディアID
	 * @return ローカルメディア。該当がない場合null。
	 */
	public LocalMedia queryLocalMediaByMediaID(SQLiteDatabase db, String mediaID) {
		Cursor c = db.query(CMediaSync.$TABLE, null,
				CMediaSync.SERVICE_TYPE + " = ?" +
						" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
						" AND " + CMediaSync.MEDIA_ID + " = ?",
				new String[] {mServiceType, mServiceAccount, mediaID}, null, null, null);
		try {
			if (c.moveToFirst()) {
				LocalMedia media = new LocalMedia(
						c.getString(c.getColumnIndex(CMediaSync.DIRPATH)),
						c.getString(c.getColumnIndex(CMediaSync.NAME)));
				LocalMedia.createRowHandler(c).populateCurrentRow(c, media);
				media.metadata = getMetadata(db, media.dirpath, media.name);
				return media;
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * ローカルにメディアを生成します。
	 * @param db データベース
	 * @param dirpath ローカルディレクトリ
	 * @param remoteMedia リモートメディア
	 * @param content メディアコンテンツ
	 * @param syncStatus 初期同期ステータス
	 * @return 生成されたローカルメディア
	 * @throws IOException 入出力例外発生時
	 */
	public LocalMedia insertLocalMedia(SQLiteDatabase db, String dirpath,
			Media remoteMedia, InputStream content, int syncStatus) throws IOException {
		String remoteTitle = remoteMedia.getTitle();
		File file = new File(dirpath, remoteTitle);
		int seq = 0;
		while (file.exists()) {
			int lastDot = remoteTitle.lastIndexOf(".");
			String name;
			String ext;
			if (0 <= lastDot) {
				name = remoteTitle.substring(0, lastDot);
				ext = remoteTitle.substring(lastDot);
			} else {
				name = remoteTitle;
				ext = "";
			}
			file = new File(dirpath,
					String.format("%s_%d%s", name, ++seq, ext));
		}
		
		FileOutputStream out = new FileOutputStream(file);
		try {
			try {
				IOUtil.copy(content, out);
			} finally {
				content.close();
			}
		} finally {
			out.close();
		}
		
		MediaUtil.scanMedia(this, file, false);
		
		return insertLocalMedia(db, file, remoteMedia, syncStatus, true);
	}
	
	/**
	 * 既存のローカルメディアとアップロードされたリモートメディアをバインドします。
	 * @param db データベース
	 * @param local ローカルメディア
	 * @param remoteMedia バインドするリモートメディア
	 * @throws IOException 入出力例外発生時
	 */
	public void bindRemoteMedia(SQLiteDatabase db,
			LocalMedia local, Media remoteMedia) throws IOException {
		File file = new File(local.dirpath, local.name);
		LocalMedia newLocal = insertLocalMedia(db, file, remoteMedia, local.syncStatus, false);
		local.sync = newLocal.sync;
	}

	protected LocalMedia insertLocalMedia(SQLiteDatabase db, File localFile,
			Media remoteMedia, int syncStatus, boolean insertMetadata) throws IOException {
		LocalMedia media =
				new LocalMedia(localFile.getParent(), localFile.getName());
		media.sync = remoteMedia.toMediaSync();
		media.syncStatus = syncStatus;
		
		ContentValues values = media.toValues(mServiceType, mServiceAccount);
		if (values.get(CMediaSync.PRODUCTION_DATE) == null) {
			values.put(CMediaSync.PRODUCTION_DATE, localFile.lastModified());
		}
		
		d("insertLocalMedia values:%s", values);
		db.insertOrThrow(CMediaSync.$TABLE, null, values);
		
		Collection<MediaMetadata> metadata = remoteMedia.getMetadata();
		if (insertMetadata && metadata != null) {
//			for (MediaMetadata m : metadata) {
//				ContentValues metaValues = m.toValues();
//				metaValues.put(CMediaMetadata.DIRPATH, localFile.getParent());
//				metaValues.put(CMediaMetadata.NAME, localFile.getName());
//				metaValues.put(CMediaMetadata.UPDATE_TIMESTAMP, System.currentTimeMillis());
//				db.insertOrThrow(CMediaMetadata.$TABLE, null, metaValues);
//			}
			replaceMetadata(localFile.getParent(), localFile.getName(), metadata, true);
		}
		media.metadata = metadata;
		
		return media;
	}
	
	/**
	 * ローカルメディアストアを更新します。
	 * @param db データベース
	 * @param media メディア
	 * @param content メディアコンテンツ
	 * @param clearMetadataTimestamp メタデータタイムスタンプをクリアする場合true
	 * @return 更新された場合true
	 */
	public boolean updateLocalMedia(SQLiteDatabase db,
			LocalMedia media, InputStream content, boolean clearMetadataTimestamp) throws IOException {
		File file = new File(media.dirpath, media.name);
		FileOutputStream out = new FileOutputStream(file);
		try {
			try {
				IOUtil.copy(content, out);
			} finally {
				content.close();
			}
		} finally {
			out.close();
		}
		
		MediaUtil.scanMedia(this, file, false);
		
		return updateLocalMedia(db, media, clearMetadataTimestamp);
	}
	
	/**
	 * ローカルメディアストアを更新します。
	 * @param db データベース
	 * @param media メディア
	 * @param finalStage このメディアの処理がこの同期の最終段階の場合true
	 * @return 更新された場合true
	 * @throws IOException 入出力例外発生時
	 */
	public boolean updateLocalMedia(SQLiteDatabase db,
			LocalMedia media, boolean finalStage) throws IOException {
		ContentValues values = media.toValues(mServiceType, mServiceAccount);
		if (values.get(CMediaSync.PRODUCTION_DATE) == null) {
			values.remove(CMediaSync.PRODUCTION_DATE);
		}
		if (values.get(CMediaSync.LOCAL_TIMESTAMP) == null) {
			values.remove(CMediaSync.LOCAL_TIMESTAMP);
		}
		
		if (!finalStage) {
			values.remove(CMediaSync.LOCAL_TIMESTAMP);
		}
		
		String where = CMediaSync.SERVICE_TYPE + " = ?" +
				" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
				" AND " + CMediaSync.DIRPATH + " = ?" +
				" AND " + CMediaSync.NAME + " = ?";
		String[] args = {mServiceType, mServiceAccount, media.dirpath, media.name};
		int updated = db.update(CMediaSync.$TABLE, values, where, args);
		d("updateLocalMedia updated:%d values:%s, where:%s, args:%s",
				updated, values, where, args);
		
		if (0 < updated) {
//			db.delete(CMediaMetadata.$TABLE,
//					CMediaMetadata.DIRPATH + " = ?" +
//							" AND " + CMediaMetadata.NAME + " = ?",
//					new String[] {media.dirpath, media.name});
//			if (media.metadata != null) {
//				for (MediaMetadata m : media.metadata) {
//					ContentValues metaValues = m.toValues();
//					metaValues.put(CMediaMetadata.DIRPATH, media.dirpath);
//					metaValues.put(CMediaMetadata.NAME, media.name);
//					metaValues.put(CMediaMetadata.UPDATE_TIMESTAMP, System.currentTimeMillis());
//					db.insertOrThrow(CMediaMetadata.$TABLE, null, metaValues);
//				}
//			}
			ArrayList<MediaMetadata> metadata = new ArrayList<MediaMetadata>();
			if (media.metadata != null) {
				metadata.addAll(media.metadata);
			}
			replaceMetadata(media.dirpath, media.name, metadata, true);
		}
		
		if (finalStage) {
			ContentValues metaTime = new ContentValues();
			metaTime.putNull(CMediaSync.LOCAL_METADATA_TIMESTAMP);
			db.update(CMediaSync.$TABLE, metaTime,
					CMediaSync.SERVICE_TYPE + " = ?" +
							" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
							" AND " + CMediaSync.DIRPATH + " = ?" +
							" AND " + CMediaSync.NAME + " = ?",
					new String[] {mServiceType, mServiceAccount, media.dirpath, media.name});
		}
		
		return 0 < updated;
	}
	
	/**
	 * ローカルメディアを削除します。
	 * @param db データベース
	 * @param dirpath ディレクトリパス
	 * @param name 名前
	 * @return 削除された場合true
	 * @throws IOException 入出力例外発生時
	 */
	public boolean deleteLocalMedia(SQLiteDatabase db, String dirpath, String name) throws IOException {
		String where = CMediaSync.SERVICE_TYPE + " = ?" +
				" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
				" AND " + CMediaSync.DIRPATH + " = ?" +
				" AND " + CMediaSync.NAME + " = ?";
		String[] args = {mServiceType, mServiceAccount, dirpath, name};
		int deleted = db.delete(CMediaSync.$TABLE, where, args);
		
		d("deleteLocalMedia deleted:%d where:%s, args:%s", deleted, where, args);
		
		File file = new File(dirpath, name);
		boolean fileDeleted = file.delete();
		d("deleteLocalMedia file delete %s (deleted=%s)", file, fileDeleted);
		
		return 0 < deleted || fileDeleted;
	}
	
	/**
	 * 同期状態を更新します。
	 * @param db データベース
	 * @param dirpath 対象ディレクトリ（更新条件）
	 * @param currentStatus 現在のステータス（更新条件）
	 * @param newStatus 新しいステータス
	 * @param updateTime 最終同期時刻を更新する場合true
	 * @return 更新件数
	 */
	public int updateSyncStatus(SQLiteDatabase db, String dirpath, int currentStatus, int newStatus, boolean updateTime) {
		ContentValues values = new ContentValues();
		values.put(CMediaSync.SYNC_STATUS, newStatus);
		if (updateTime) {
			values.put(CMediaSync.SYNC_TIME, System.currentTimeMillis());
		}
		
		String where = CMediaSync.DIRPATH + " = ?" +
				" AND " + CMediaSync.SYNC_STATUS + " = ?";
		String[] args = {dirpath, String.valueOf(currentStatus)};
		int updated = db.update(CMediaSync.$TABLE, values, where, args);
		
		d("updateSyncStatus updated:%d values:%s, where:%s, args:%s",
				updated, values, where, args);
		return updated;
	}
	
	/**
	 * 同期キャッシュをクリアします。
	 * @param db データベース
	 * @param dirpath 対象ディレクトリ
	 */
	public void clearSyncCache(SQLiteDatabase db, String dirpath) {
		String sql =
				"UPDATE " + CMediaSync.$TABLE +
				" SET " + CMediaSync.SYNC_CACHE + " = NULL" +
				" WHERE " + CMediaSync.SERVICE_TYPE + " = ?" +
						" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
						" AND " + CMediaSync.DIRPATH + " = ?";
		db.execSQL(sql, new Object[] {getServiceType(), getServiceAccount(), dirpath});
	}
	
	/**
	 * 処理対象としているサービスタイプを返します。
	 * @return サービスタイプ
	 */
	public String getServiceType() {
		return mServiceType;
	}

	/**
	 * 処理対象としているサービスアカウントを返します。
	 * @return サービスアカウント
	 */
	public String getServiceAccount() {
		return mServiceAccount;
	}
	
	private Cursor queryMediaSync(SQLiteDatabase db, String dirpath, String name) {
		Cursor c = db.query(CMediaSync.$TABLE, null,
				CMediaSync.SERVICE_TYPE + " = ?" +
						" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
						" AND " + CMediaSync.DIRPATH + " = ?" +
						" AND " + CMediaSync.NAME + " = ?",
				new String[] {mServiceType, mServiceAccount, dirpath, name}, null, null, null);
		return c;
	}
	
	private List<MediaMetadata> getMetadata(SQLiteDatabase db, String dirpath, String name) {
//		Cursor c = db.query(CMediaMetadata.$TABLE, null,
//				CMediaMetadata.DIRPATH + " = ?" +
//						" AND " + CMediaMetadata.NAME + " = ?",
//				new String[] {dirpath, name}, null, null, null);
		Cursor c = getContentResolver().query(
				JorlleProvider.getUriFor(getApplicationContext(), new String[] {CMediaMetadata.$TABLE}),
				null,
				CMediaMetadata.DIRPATH + " = ?" +
						" AND " + CMediaMetadata.NAME + " = ?",
				new String[] {dirpath, name}, null);
		try {
			ArrayList<MediaMetadata> metadata = new ArrayList<MediaMetadata>();
			RowHandler<MediaMetadata> handler = MediaMetadata.createRowHandler(c);
			while (c.moveToNext()) {
				MediaMetadata m = new MediaMetadata();
				handler.populateCurrentRow(c, m);
				metadata.add(m);
			}
			return metadata;
		} finally {
			c.close();
		}
	}
	
	/**
	 * クラウド管理サーバへの送信対象として、変更分を抽出します。
	 * @param db データベース
	 * @param dirpath ディレクトリパス
	 * @param status 変更されたデータをあらわすステータス
	 */
	public void extractDirtiesToSendAsUpdates(SQLiteDatabase db, String dirpath, Integer... status) {
		String sql =
				"INSERT OR REPLACE INTO " + CMediaSyncToSend.$TABLE + "(" +
						CMediaSyncToSend.SERVICE_TYPE_TO_SEND + ", " +
						CMediaSyncToSend.SERVICE_ACCOUNT_TO_SEND + ", " +
						CMediaSyncToSend.MEDIA_ID_TO_SEND + ", " +
						CMediaSyncToSend.OPERATION_TO_SEND +")" +
				" SELECT " +
						CMediaSync.SERVICE_TYPE + ", " +
						CMediaSync.SERVICE_ACCOUNT + ", " +
						CMediaSync.MEDIA_ID + ", " +
						"?" +
				" FROM " + CMediaSync.$TABLE +
				" WHERE " + CMediaSync.SERVICE_TYPE + " = ?" +
						" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
						" AND " + CMediaSync.DIRPATH + " = ?" +
						" AND " + CMediaSync.SYNC_STATUS + " IN (" + TextUtils.join(",", status) + ")";
		db.execSQL(sql, new Object[] {CMediaSyncToSend.OP_UPDATE, mServiceType, mServiceAccount, dirpath});
	}
	
	/**
	 * 送信対象レコードを削除します。
	 * @param db データベース
	 * @param mediaID メディアID
	 * @return 削除された場合true
	 */
	public boolean deleteDirtiesToSend(SQLiteDatabase db, String mediaID) {
		String where = CMediaSyncToSend.SERVICE_TYPE_TO_SEND + " = ?" +
				" AND " + CMediaSyncToSend.SERVICE_ACCOUNT_TO_SEND + " = ?" +
				" AND " + CMediaSyncToSend.MEDIA_ID_TO_SEND + " = ?";
		String[] args = {mServiceType, mServiceAccount, mediaID};
		int deleted = db.delete(CMediaSyncToSend.$TABLE, where, args);
		return 0 < deleted;
	}
	
	private int replaceMetadata(String dirpath, String name, Collection<MediaMetadata> metadatas, boolean silent) {
		Map<String, List<ContentValues>> types = new HashMap<String, List<ContentValues>>();
		for (MediaMetadata metadata : metadatas) {
			List<ContentValues> list = types.get(metadata.type);
			if (list == null) {
				list = new ArrayList<ContentValues>();
				types.put(metadata.type, list);
			}
			ContentValues values = metadata.toValues();
			values.put(CMediaMetadata.DIRPATH, dirpath);
			values.put(CMediaMetadata.NAME, name);
			values.put(CMediaMetadata.UPDATE_TIMESTAMP, System.currentTimeMillis());
			list.add(values);
		}
		
		int effected = 0;
		for (String type : types.keySet()) {
			List<ContentValues> list = types.get(type);
			Uri uri = JorlleProvider.getUriFor(this,
					new String[] {CMediaMetadata.$TABLE, dirpath, name, type},
					new StringPair("silent", String.valueOf(silent)));
			effected += getContentResolver().bulkInsert(uri, list.toArray(new ContentValues[0]));
		}
		
		return effected;
	}
	
	protected void d(String format, Object... args) {		/*$debug$*/
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			for (int i = 0; i < args.length; i++) {		/*$debug$*/
				if (args[i].getClass().isArray()) {		/*$debug$*/
					args[i] = Arrays.toString((Object[]) args[i]);		/*$debug$*/
				}		/*$debug$*/
			}		/*$debug$*/
			Log.d(tag, String.format(format, args));		/*$debug$*/
		}		/*$debug$*/
	}		/*$debug$*/
	
	protected void d(String format, Throwable t, Object... args) {		/*$debug$*/
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			for (int i = 0; i < args.length; i++) {		/*$debug$*/
				if (args[i].getClass().isArray()) {		/*$debug$*/
					args[i] = Arrays.toString((Object[]) args[i]);		/*$debug$*/
				}		/*$debug$*/
			}		/*$debug$*/
			Log.d(tag, String.format(format, args), t);		/*$debug$*/
		}		/*$debug$*/
	}		/*$debug$*/
}
