package jp.co.johospace.jsphoto.database;

/**
 * 列定義 メディアメタデータ汚れ
 */
public interface CMediaMetadataDirty {

	/** テーブル名 */
	String $TABLE = "media_metadata_dirty";
	
	/** ID **/
	String _ID = "_id";
	/** ディレクトリパス **/
	String DIRPATH = "dirpath";
	/** 名前 **/
	String NAME = "name";
	/** 更新タイムスタンプ **/
	String UPDATED_TIME = "updated_time";
}
