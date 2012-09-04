package jp.co.johospace.jsphoto.database;

/**
 * 列定義 メディアインデックス
 */
public interface CMediaIndex {

	/** テーブル名 */
	String $TABLE = "media_indexes";
	
	/** ID **/
	String _ID = "_id";
	/** ディレクトリパス **/
	String DIRPATH = "dirpath";
	/** 名前 **/
	String NAME = "name";
	/** サムネイル **/
	String THUMBNAIL = "thumbnail";
	/** サムネイルタイムスタンプ **/
	String THUMBNAIL_TIMESTAMP = "thumbnail_timestamp";
	/** 方向 */
	String ORIENTATION = "orientation";
	/** サイズ */
	String SIZE = "image_size";
}
