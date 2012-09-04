package jp.co.johospace.jsphoto.v2.onlineservice.sync.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import jp.co.johospace.jsphoto.util.RowHandler;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.db.CMediaSync;
import android.content.ContentValues;
import android.database.Cursor;


/**
 * ローカルメディア 
 */
public class LocalMedia {
	protected final String tag = getClass().getSimpleName();

	/**
	 * コンストラクタ
	 * @param dirpath ディレクトリ
	 * @param name 名前
	 */
	public LocalMedia(String dirpath, String name) {
		super();
		this.dirpath = dirpath;
		this.name = name;
	}
	
	/** ディレクトリパス */
	public final String dirpath;
	/** 名前 */
	public final String name;
	/** 前回タイムスタンプ */
	public Long previousTimestamp;
	/** メタデータタイムスタンプ（前回） */
	public Long previousMetadataTimestamp;
	/** メタデータタイムスタンプ */
	public Long metadataTimestamp;
	/** 同期キャッシュ */
	public byte[] syncCache;
	/** 同期ステータス */
	public Integer syncStatus;
	/** 同期時刻 */
	public Long syncTime;
	
	/** メディア同期 */
	public Media sync;
	
	/** メタデータ */
	public Collection<Metadata> metadata;
	
	/**
	 * メディアコンテンツが存在するかどうかを返します。
	 * @return 存在する場合true
	 */
	public boolean contentExists() {
		return new File(dirpath, name).exists();
	}
	
	/**
	 * メディアコンテンツを読み込むためのストリームをオープンします。
	 * @return メディアコンテンツを読むストリーム。コンテンツの実体がない場合null。
	 * @throws IOException 入出力例外発生時
	 */
	public InputStream openContent() throws IOException {
		File file = new File(dirpath, name);
		if (file.exists()) {
			return new FileInputStream(file);
		} else {
			return null;
		}
	}
	
	/**
	 * ローカルメディアコンテンツのタイムスタンプを返します。
	 * @return タイムスタンプ
	 */
	public Long getContentTimestamp() {
		File file = new File(dirpath, name);
		if (file.exists()) {
			long lastModified = file.lastModified();
			return lastModified;
		} else {
			return null;
		}
	}
	
	/**
	 * ローカルメディアのタイムスタンプを返します。
	 * @return　タイムスタンプ
	 */
	public long getTimestamp() {
		Long contentTimestamp = getContentTimestamp();
		return Math.max(previousTimestamp, contentTimestamp);
	}
	
	/**
	 * ローカルメディアの変更を調べます。
	 * @return 前回同期時から変更されている場合true
	 */
	public boolean isDirty() {
		return isContentDirty() || isMetadataDirty();
	}
	
	/**
	 * ローカルメディアコンテンツの変更を調べます。
	 * @return 前回同期時から変更されている場合true
	 */
	public boolean isContentDirty() {
		Long current = getContentTimestamp();
		boolean dirty;
		if (current != null) {
			long diff = current - previousTimestamp;
			dirty = 1000L < Math.abs(diff);
		} else {
			dirty = !previousTimestamp.equals(current);
		}
		d("content's dirty previous(%d) vs current(%d) : dirty=%s", previousTimestamp, current, dirty);
		return dirty;
	}
	
	/**
	 * ローカルメディアメタデータの変更を調べます。
	 * @return 前回同期時から変更されている場合true
	 */
	public boolean isMetadataDirty() {
		boolean dirty;
		if (metadataTimestamp != null && previousMetadataTimestamp != null) {
			dirty = !metadataTimestamp.equals(previousMetadataTimestamp);
		} else {
			dirty = !(metadataTimestamp == null && previousMetadataTimestamp == null);
		}
		d("metadata's dirty %s : dirty=%s", String.valueOf(metadataTimestamp), dirty);
		return dirty;
	}
	
	/**
	 * 行ハンドラを生成します。
	 * @param c カーソル
	 * @return 行ハンドラ
	 */
	public static RowHandler<LocalMedia> createRowHandler(final Cursor c) {
		return new RowHandler<LocalMedia>() {
			final int INDEX_SYNC_TIME = c.getColumnIndex(CMediaSync.SYNC_TIME);
			final int INDEX_SYNC_STATUS = c.getColumnIndex(CMediaSync.SYNC_STATUS);
			final int INDEX_SYNC_CACHE = c.getColumnIndex(CMediaSync.SYNC_CACHE);
			final int INDEX_LOCAL_TIMESTAMP = c.getColumnIndex(CMediaSync.LOCAL_TIMESTAMP);
			final int INDEX_LOCAL_METADATA_TIMESTAMP = c.getColumnIndex(CMediaSync.LOCAL_METADATA_TIMESTAMP);
			final int INDEX_PREV_METADATA_TIMESTAMP = c.getColumnIndex(CMediaSync.PREV_METADATA_TIMESTAMP);
			
			final RowHandler<Media> mMediaSyncRowHandler = Media.createRowHandler(c);
			
			@Override
			public void populateCurrentRow(Cursor c, LocalMedia row) {
				
				row.sync = new Media();
				mMediaSyncRowHandler.populateCurrentRow(c, row.sync);
				
				if (0 <= INDEX_SYNC_TIME) {
					if (c.isNull(INDEX_SYNC_TIME)) {
						row.syncTime = null;
					} else {
						row.syncTime = c.getLong(INDEX_SYNC_TIME);
					}
				}
				if (0 <= INDEX_SYNC_STATUS) {
					row.syncStatus = c.getInt(INDEX_SYNC_STATUS);
				}
				if (0 <= INDEX_SYNC_CACHE) {
					row.syncCache = c.getBlob(INDEX_SYNC_CACHE);
				}
				if (0 <= INDEX_LOCAL_TIMESTAMP) {
					row.previousTimestamp = c.getLong(INDEX_LOCAL_TIMESTAMP);
				}
				if (0 <= INDEX_LOCAL_METADATA_TIMESTAMP) {
					if (c.isNull(INDEX_LOCAL_METADATA_TIMESTAMP)) {
						row.metadataTimestamp = null;
					} else {
						row.metadataTimestamp = c.getLong(INDEX_LOCAL_METADATA_TIMESTAMP);
					}
				}
				if (0 <= INDEX_PREV_METADATA_TIMESTAMP) {
					if (c.isNull(INDEX_PREV_METADATA_TIMESTAMP)) {
						row.previousMetadataTimestamp = null;
					} else {
						row.previousMetadataTimestamp = c.getLong(INDEX_PREV_METADATA_TIMESTAMP);
					}
				}
				
			}
		};
	}
	
	/**
	 * キーバリューにマッピングします。
	 * @param serviceType サービスタイプ
	 * @param serviceAccount サービスアカウント
	 * @return マッピング
	 */
	public ContentValues toValues(String serviceType, String serviceAccount) {
		ContentValues values;
		if (sync != null) {
			values = sync.toValues();
		} else {
			values = new ContentValues();
		}
		
		values.put(CMediaSync.SERVICE_TYPE, serviceType);
		values.put(CMediaSync.SERVICE_ACCOUNT, serviceAccount);
		values.put(CMediaSync.DIRPATH, dirpath);
		values.put(CMediaSync.NAME, name);
		values.put(CMediaSync.SYNC_TIME, syncTime);
		values.put(CMediaSync.SYNC_STATUS, syncStatus);
		values.put(CMediaSync.SYNC_CACHE, syncCache);
		values.put(CMediaSync.LOCAL_TIMESTAMP, getContentTimestamp());
		values.put(CMediaSync.LOCAL_METADATA_TIMESTAMP, metadataTimestamp);
		values.put(CMediaSync.PREV_METADATA_TIMESTAMP, previousMetadataTimestamp);
		return values;
	}
	
	protected void d(String format, Object... args) {
//		if (JorlleApplication.debuggable()) {		/*$debug$*/
//			Log.d(tag, String.format(format, args));		/*$debug$*/
//		}		/*$debug$*/
	}
	
	protected void d(String format, Throwable t, Object... args) {
//		if (JorlleApplication.debuggable()) {		/*$debug$*/
//			Log.d(tag, String.format(format, args), t);		/*$debug$*/
//		}		/*$debug$*/
	}
}
