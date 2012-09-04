package jp.co.johospace.jsphoto.v2.onlineservice.clients.model;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.util.RowHandler;
import android.content.ContentValues;
import android.database.Cursor;

/**
 * メディアメタデータ
 */
public class Metadata {

	/** メタデータタイプ */
	public String type;
	/** メタデータ */
	public String data;
	/** 更新タイムスタンプ */
	public Long updateTimestamp;
	
	/**
	 * キーバリューマッピングに変換します。
	 * @return キーバリューマッピング
	 */
	public ContentValues toValues() {
		ContentValues values = new ContentValues();
		values.put(CMediaMetadata.METADATA_TYPE, type);
		values.put(CMediaMetadata.METADATA, data);
		return values;
	}
	
	/**
	 * 行ハンドラを生成します。
	 * @param c カーソル
	 * @return 行ハンドラ
	 */
	public static RowHandler<Metadata> createRowHandler(final Cursor c) {
		return new RowHandler<Metadata>() {
			final int INDEX_METADATA_TYPE = c.getColumnIndex(CMediaMetadata.METADATA_TYPE);
			final int INDEX_METADATA = c.getColumnIndex(CMediaMetadata.METADATA);
			final int INDEX_UPDATE_TIMESTAMP = c.getColumnIndex(CMediaMetadata.UPDATE_TIMESTAMP);
			@Override
			public void populateCurrentRow(Cursor c, Metadata row) {
				if (0 <= INDEX_METADATA_TYPE) {
					row.type = c.getString(INDEX_METADATA_TYPE);
				}
				if (0 <= INDEX_METADATA) {
					row.data = c.getString(INDEX_METADATA);
				}
				if (0 <= INDEX_UPDATE_TIMESTAMP) {
					row.updateTimestamp = c.getLong(INDEX_UPDATE_TIMESTAMP);
				}
			}
		};
	}
}
