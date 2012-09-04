package jp.co.johospace.jsphoto.v2.onlineservice.sync.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import jp.co.johospace.jsphoto.database.CMediaMetadataDirty;
import jp.co.johospace.jsphoto.provider.JorlleProvider;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.MediaStoreOperation;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.RowHandler;
import jp.co.johospace.jsphoto.util.TerminatableIterator;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.ProviderAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.model.LocalMedia;
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
	 * リモートにバインドされたローカルメディアを取得します。
	 * @param db データベース
	 * @param mediaId メディアID
	 * @return リモートメディア。バインドされていない場合null。
	 */
	public LocalMedia getLocalMedia(SQLiteDatabase db, String mediaId) {
		Cursor c = queryMediaSync(db, mediaId);
		try {
			if (c.moveToFirst()) {
				String dirpath = c.getString(c.getColumnIndex(CMediaSync.DIRPATH));
				String name = c.getString(c.getColumnIndex(CMediaSync.NAME));
				LocalMedia media = new LocalMedia(dirpath, name);
				LocalMedia.createRowHandler(c).populateCurrentRow(c, media);
				media.metadata = getMetadata(db, dirpath, name);
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
		String remoteTitle = remoteMedia.fileName;
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
		
		try{
			MediaUtil.scanMedia(this, file, false);
			
			return insertLocalMedia(db, file, remoteMedia, syncStatus, true);
		}catch(IOException e){
			//補償を実行
			//失敗すると画像ファイルが増えるが、
			//厳密にすると二相コミットが必要になり現実的ではない。
			compensate(file, e);
		}catch(RuntimeException e){
			compensate(file, e);
		}
		
		return null;
	}

	private <E extends Exception> void compensate(File file, E e) throws E {
		file.delete();
		MediaStoreOperation.deleteMediaStoreEntry(this, file);
		throw e;
	}

	protected LocalMedia insertLocalMedia(SQLiteDatabase db, File localFile,
			Media remoteMedia, int syncStatus, boolean insertMetadata) throws IOException {
		LocalMedia media =
				new LocalMedia(localFile.getParent(), localFile.getName());
		media.sync = remoteMedia;
		media.syncStatus = syncStatus;
		
		ContentValues values = media.toValues(mServiceType, mServiceAccount);
		if (values.get(CMediaSync.PRODUCTION_DATE) == null) {
			values.put(CMediaSync.PRODUCTION_DATE, localFile.lastModified());
		}
		
		db.insertOrThrow(CMediaSync.$TABLE, null, values);
		
		Collection<Metadata> metadata = remoteMedia.metadata;
		if (insertMetadata && metadata != null) {
			ProviderAccessor.replaceMetadata(this, localFile.getParent(), localFile.getName(), metadata, true);
		}
		media.metadata = metadata;
		
		return media;
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
		
		return updateLocalMedia(db, media, clearMetadataTimestamp, true);
	}
	
	/**
	 * ローカルメディアストアを更新します。
	 * @param db データベース
	 * @param media メディア
	 * @param finalStage このメディアの処理がこの同期の最終段階の場合true
	 * @param updateMetadata メタデータを更新する場合true
	 * @return 更新された場合true
	 * @throws IOException 入出力例外発生時
	 */
	public boolean updateLocalMedia(SQLiteDatabase db,
			LocalMedia media, boolean finalStage, boolean updateMetadata) throws IOException {
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
		
		if (0 < updated && updateMetadata) {
			ArrayList<Metadata> metadata = new ArrayList<Metadata>();
			if (media.metadata != null) {
				metadata.addAll(media.metadata);
			}
			ProviderAccessor.replaceMetadata(this, media.dirpath, media.name, metadata, true);
		}
		
		if (finalStage) {
			ContentValues metaTime = new ContentValues();
			metaTime.put(CMediaSync.PREV_METADATA_TIMESTAMP,
					values.getAsLong(CMediaSync.LOCAL_METADATA_TIMESTAMP));
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
		
		File file = new File(dirpath, name);
		boolean fileDeleted = file.delete();
		
		MediaStoreOperation.deleteMediaStoreEntry(getApplicationContext(), file);
		
		return 0 < deleted || fileDeleted;
	}
	
	/**
	 * ローカルメディアを削除します。
	 * @param db データベース
	 * @param mediaId メディアID
	 * @return 削除された場合true
	 * @throws IOException 入出力例外発生時
	 */
	public boolean deleteLocalMedia(SQLiteDatabase db, String mediaId) throws IOException {
		String where = CMediaSync.SERVICE_TYPE + " = ?" +
				" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
				" AND " + CMediaSync.MEDIA_ID + " = ?";
		String[] args = {mServiceType, mServiceAccount, mediaId};
		Cursor c = db.query(CMediaSync.$TABLE, new String[] {CMediaSync.DIRPATH, CMediaSync.NAME},
				where, args, null, null, null, "1");
		try {
			if (c.moveToFirst()) {
				int deleted = db.delete(CMediaSync.$TABLE, where, args);
				
				File file = new File(c.getString(0), c.getString(1));
				boolean fileDeleted = file.delete();
				
				MediaStoreOperation.deleteMediaStoreEntry(getApplicationContext(), file);
				
				ProviderAccessor.deleteMetadata(this, c.getString(0), c.getString(1));
				
				return 0 < deleted || fileDeleted;
			} else {
				return false;
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * メタデータの汚れを反映します。
	 * @param db データベース
	 * @param localDir ローカルディレクトリ
	 */
	public void applyMetadataDirty(SQLiteDatabase db, String localDir) {
		Uri uri = JorlleProvider.getUriFor(getApplicationContext(), new String[] {CMediaMetadataDirty.$TABLE});
		Cursor c = getContentResolver().query(uri,
				new String[] {CMediaMetadataDirty.NAME, CMediaMetadataDirty.UPDATED_TIME},
				CMediaMetadataDirty.DIRPATH + " = ?", new String[] {localDir}, null);
		try {
			ContentValues values = new ContentValues();
			while (c.moveToNext()) {
				String name = c.getString(0);
				long time = c.getLong(1);
				
				File file = new File(localDir, name);
				if (file.exists()) {
					values.put(CMediaSync.LOCAL_METADATA_TIMESTAMP, time);
					db.update(CMediaSync.$TABLE, values,
							CMediaSync.DIRPATH + " = ?" +
									" AND " + CMediaSync.NAME + " = ?",
							new String[] {localDir, name});
				} else {
					try {
						getContentResolver().delete(uri,
								CMediaMetadataDirty.DIRPATH + " = ?" +
										" AND " + CMediaMetadataDirty.NAME + " = ?",
								new String[] {localDir, name});
					} catch (Exception e) {
						Log.e(tag, "failed to delete dirty.", e);/*$debug$*/
					}
				}
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * 同期済みバージョンを取得します。
	 * @param db データベース
	 * @return 同期済みバージョン
	 */
	public Long getSyncedVersion(SQLiteDatabase db) {
		Cursor c = db.query(CMediaSyncVersions.$TABLE,
				new String[] {CMediaSyncVersions.SYNCED_VERSION},
				CMediaSyncVersions.SERVICE_TYPE + " = ?" +
						" AND " + CMediaSyncVersions.SERVICE_ACCOUNT + " = ?",
				new String[] {mServiceType, mServiceAccount},
				null, null, null, "1");
		try {
			if (c.moveToFirst()) {
				return c.isNull(0) ? null : c.getLong(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	/**
	 * 同期済みバージョンを保存します。
	 * @param db データベース
	 * @param version バージョン
	 */
	public void saveSyncedVersion(SQLiteDatabase db, Long version) {
		ContentValues values = new ContentValues();
		values.put(CMediaSyncVersions.SERVICE_TYPE, mServiceType);
		values.put(CMediaSyncVersions.SERVICE_ACCOUNT, mServiceAccount);
		values.put(CMediaSyncVersions.SYNCED_VERSION, version);
		db.insertWithOnConflict(CMediaSyncVersions.$TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
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
				new String[] {mServiceType, mServiceAccount, dirpath, name}, null, null, null, "1");
		return c;
	}
	
	private Cursor queryMediaSync(SQLiteDatabase db, String mediaId) {
		Cursor c = db.query(CMediaSync.$TABLE, null,
				CMediaSync.SERVICE_TYPE + " = ?" +
						" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
						" AND " + CMediaSync.MEDIA_ID + " = ?",
				new String[] {mServiceType, mServiceAccount, mediaId}, null, null, null, "1");
		return c;
	}
	
	private List<Metadata> getMetadata(SQLiteDatabase db, String dirpath, String name) {
		return ProviderAccessor.queryMetadata(getApplicationContext(), dirpath, name);
	}
	
}
