package jp.co.johospace.jsphoto.onlineservice;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import android.content.ContentValues;
import android.database.Cursor;

/**
 * メディアのメタデータ
 */
public class MediaMetadata {

	/** タイプ */
	public String type;
	/** 値 */
	public String value;
	/** 更新タイムスタンプ */
	public Long updateTimestamp;
	
	/**
	 * キーバリューマッピングに変換します。
	 * @return キーバリューマッピング
	 */
	public ContentValues toValues() {
		ContentValues values = new ContentValues();
		values.put(CMediaMetadata.METADATA_TYPE, type);
		values.put(CMediaMetadata.METADATA, value);
		return values;
	}
	
	/**
	 * 行ハンドラを生成します。
	 * @param c カーソル
	 * @return 行ハンドラ
	 */
	public static RowHandler<MediaMetadata> createRowHandler(final Cursor c) {
		return new RowHandler<MediaMetadata>() {
			final int INDEX_METADATA_TYPE = c.getColumnIndex(CMediaMetadata.METADATA_TYPE);
			final int INDEX_METADATA = c.getColumnIndex(CMediaMetadata.METADATA);
			final int INDEX_UPDATE_TIMESTAMP = c.getColumnIndex(CMediaMetadata.UPDATE_TIMESTAMP);
			@Override
			public void populateCurrentRow(Cursor c, MediaMetadata row) {
				if (0 <= INDEX_METADATA_TYPE) {
					row.type = c.getString(INDEX_METADATA_TYPE);
				}
				if (0 <= INDEX_METADATA) {
					row.value = c.getString(INDEX_METADATA);
				}
				if (0 <= INDEX_UPDATE_TIMESTAMP) {
					row.updateTimestamp = c.getLong(INDEX_UPDATE_TIMESTAMP);
				}
			}
		};
	}
}
