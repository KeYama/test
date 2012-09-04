package jp.co.johospace.jsphoto.v2.onlineservice.clients.model;

import java.util.Collection;

import jp.co.johospace.jsphoto.fullscreen.loader.TagHint;
import jp.co.johospace.jsphoto.util.RowHandler;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.db.CMediaSync;
import android.content.ContentValues;
import android.database.Cursor;

/**
 * メディア
 */
public class Media implements TagHint {

	/** サービスタイプ */
	public String service;
	/** サービスアカウント */
	public String account;
	/** メディアID */
	public String mediaId;
	/** ディレクトリID */
	public String directoryId;
	/** メディアURI */
	public String mediaUri;
	/** ファイル名 */
	public String fileName;
	/** 制作日時 */
	public Long productionDate;
	/** サムネイルデータ */
	public String thumbnailData;
	/** メタデータ */
	public Collection<Metadata> metadata;
	/** 更新日時 */
	public Long updatedTimestamp;
	/** バージョン */
	public Long version;
	/** 削除データ */
	public Boolean deleted;
	
	/**
	 * メディア同期の行ハンドラを生成します。
	 * @param c カーソル
	 * @return 行ハンドラ
	 */
	public static RowHandler<Media> createRowHandler(final Cursor c) {
		return new RowHandler<Media>() {
			final int INDEX_SERVICE_TYPE = c.getColumnIndex(CMediaSync.SERVICE_TYPE);
			final int INDEX_SERVICE_ACCOUNT = c.getColumnIndex(CMediaSync.SERVICE_ACCOUNT);
			final int INDEX_MEDIA_ID = c.getColumnIndex(CMediaSync.MEDIA_ID);
			final int INDEX_DIRECTORY_ID = c.getColumnIndex(CMediaSync.DIRECTORY_ID);
			final int INDEX_MEDIA_URI = c.getColumnIndex(CMediaSync.MEDIA_URI);
			final int INDEX_REMOTE_VERSION = c.getColumnIndex(CMediaSync.REMOTE_VERSION);
			final int INDEX_TITLE = c.getColumnIndex(CMediaSync.TITLE);
			final int INDEX_THUMBNAIL_DATA = c.getColumnIndex(CMediaSync.THUMBNAIL_DATA);
			final int INDEX_REMOTE_TIMESTAMP = c.getColumnIndex(CMediaSync.REMOTE_TIMESTAMP);
			final int INDEX_PRODUCTION_DATE = c.getColumnIndex(CMediaSync.PRODUCTION_DATE);
			
			@Override
			public void populateCurrentRow(Cursor c, Media row) {
				if (0 <= INDEX_SERVICE_TYPE) {
					row.service = c.getString(INDEX_SERVICE_TYPE);
				}
				if (0 <= INDEX_SERVICE_ACCOUNT) {
					row.account = c.getString(INDEX_SERVICE_ACCOUNT);
				}
				if (0 <= INDEX_MEDIA_ID) {
					row.mediaId = c.getString(INDEX_MEDIA_ID);
				}
				if (0 <= INDEX_DIRECTORY_ID) {
					row.directoryId = c.getString(INDEX_DIRECTORY_ID);
				}
				if (0 <= INDEX_MEDIA_URI) {
					row.mediaUri = c.getString(INDEX_MEDIA_URI);
				}
				if (0 <= INDEX_REMOTE_VERSION) {
					row.version = c.getLong(INDEX_REMOTE_VERSION);
				}
				if (0 <= INDEX_TITLE) {
					row.fileName = c.getString(INDEX_TITLE);
				}
				if (0 <= INDEX_THUMBNAIL_DATA) {
					row.thumbnailData = c.getString(INDEX_THUMBNAIL_DATA);
				}
				if (0 <= INDEX_REMOTE_TIMESTAMP) {
					row.updatedTimestamp = c.getLong(INDEX_REMOTE_VERSION);
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
		populateValues(values);
		return values;
	}
	
	public void populateValues(ContentValues values) {
		values.put(CMediaSync.SERVICE_TYPE, service);
		values.put(CMediaSync.SERVICE_ACCOUNT, account);
		values.put(CMediaSync.MEDIA_ID, mediaId);
		values.put(CMediaSync.DIRECTORY_ID, directoryId);
		values.put(CMediaSync.MEDIA_URI, mediaUri);
		values.put(CMediaSync.REMOTE_VERSION, version);
		values.put(CMediaSync.TITLE, fileName);
		values.put(CMediaSync.THUMBNAIL_DATA, thumbnailData);
		values.put(CMediaSync.REMOTE_TIMESTAMP, updatedTimestamp);
		values.put(CMediaSync.PRODUCTION_DATE, productionDate);
	}

	@Override
	public String getFileName() {
		return fileName;
	}
	
	@Override
	public String getFilePath(){
		return mediaId;
	}
	
	public Integer orderSeq;
	public Long parentId;

}
