package jp.co.johospace.jsphoto.onlineservice;

import jp.co.johospace.jsphoto.database.CMediaSync;
import android.content.ContentValues;
import android.database.Cursor;

/**
 * メディア同期
 */
public class MediaSync {

	/** メディアID */
	public String mediaID;
	/** ディレクトリID */
	public String directoryID;
	/** メディアURI */
	public String mediaURI;
	/** リモートバージョン */
	public String remoteVersion;
	/** 同期データ1 */
	public String syncData1;
	/** 同期データ2 */
	public String syncData2;
	/** 同期データ3 */
	public String syncData3;
	/** 同期データ4 */
	public String syncData4;
	/** 同期データ5 */
	public String syncData5;
	/** 制作日時 */
	public Long productionDate;
	
	/**
	 * メディア同期の行ハンドラを生成します。
	 * @param c カーソル
	 * @return 行ハンドラ
	 */
	public static RowHandler<MediaSync> createRowHandler(final Cursor c) {
		return new RowHandler<MediaSync>() {
			final int INDEX_MEDIA_ID = c.getColumnIndex(CMediaSync.MEDIA_ID);
			final int INDEX_DIRECTORY_ID = c.getColumnIndex(CMediaSync.DIRECTORY_ID);
			final int INDEX_MEDIA_URI = c.getColumnIndex(CMediaSync.MEDIA_URI);
			final int INDEX_REMOTE_VERSION = c.getColumnIndex(CMediaSync.REMOTE_VERSION);
			final int INDEX_SYNC_DATA1 = c.getColumnIndex(CMediaSync.SYNC_DATA1);
			final int INDEX_SYNC_DATA2 = c.getColumnIndex(CMediaSync.SYNC_DATA2);
			final int INDEX_SYNC_DATA3 = c.getColumnIndex(CMediaSync.SYNC_DATA3);
			final int INDEX_SYNC_DATA4 = c.getColumnIndex(CMediaSync.SYNC_DATA4);
			final int INDEX_SYNC_DATA5 = c.getColumnIndex(CMediaSync.SYNC_DATA5);
			final int INDEX_PRODUCTION_DATE = c.getColumnIndex(CMediaSync.PRODUCTION_DATE);
			
			@Override
			public void populateCurrentRow(Cursor c, MediaSync row) {
				if (0 <= INDEX_MEDIA_ID) {
					row.mediaID = c.getString(INDEX_MEDIA_ID);
				}
				if (0 <= INDEX_DIRECTORY_ID) {
					row.directoryID = c.getString(INDEX_DIRECTORY_ID);
				}
				if (0 <= INDEX_MEDIA_URI) {
					row.mediaURI = c.getString(INDEX_MEDIA_URI);
				}
				if (0 <= INDEX_REMOTE_VERSION) {
					row.remoteVersion = c.getString(INDEX_REMOTE_VERSION);
				}
				if (0 <= INDEX_SYNC_DATA1) {
					row.syncData1 = c.getString(INDEX_SYNC_DATA1);
				}
				if (0 <= INDEX_SYNC_DATA2) {
					row.syncData2 = c.getString(INDEX_SYNC_DATA2);
				}
				if (0 <= INDEX_SYNC_DATA3) {
					row.syncData3 = c.getString(INDEX_SYNC_DATA3);
				}
				if (0 <= INDEX_SYNC_DATA4) {
					row.syncData4 = c.getString(INDEX_SYNC_DATA4);
				}
				if (0 <= INDEX_SYNC_DATA5) {
					row.syncData5 = c.getString(INDEX_SYNC_DATA5);
				}
				if (0 <= INDEX_PRODUCTION_DATE) {
					if (c.isNull(INDEX_PRODUCTION_DATE)) {
						row.productionDate = null;
					} else {
						row.productionDate = c.getLong(INDEX_PRODUCTION_DATE);
					}
				}
			}
		};
	}
	
	/**
	 * キーバリューにマッピングします。
	 * @return マッピング
	 */
	public ContentValues toValues() {
		ContentValues values = new ContentValues();
		
		values.put(CMediaSync.MEDIA_ID, mediaID);
		values.put(CMediaSync.DIRECTORY_ID, directoryID);
		values.put(CMediaSync.MEDIA_URI, mediaURI);
		values.put(CMediaSync.REMOTE_VERSION, remoteVersion);
		values.put(CMediaSync.SYNC_DATA1, syncData1);
		values.put(CMediaSync.SYNC_DATA2, syncData2);
		values.put(CMediaSync.SYNC_DATA3, syncData3);
		values.put(CMediaSync.SYNC_DATA4, syncData4);
		values.put(CMediaSync.SYNC_DATA5, syncData5);
		values.put(CMediaSync.PRODUCTION_DATE, productionDate);
		
		return values;
	}
}
