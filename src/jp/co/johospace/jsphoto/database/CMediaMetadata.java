package jp.co.johospace.jsphoto.database;

/**
 * 列定義 メディアメタデータ
 */
public interface CMediaMetadata {

	/** テーブル名 */
	String $TABLE = "media_metadata";
	
	/** ID **/
	String _ID = "_id";
	/** ディレクトリパス **/
	String DIRPATH = "dirpath";
	/** 名前 **/
	String NAME = "name";
	
	/** メタデータタイプ **/
	String METADATA_TYPE = "metadata_type";
	/** タイプ：　タグ */
	String TYPE_TAG = "vnd.jp.co.johospace/jorlle-tag";
	/** タイプ：　お気に入り */
	String TYPE_FAVORITE = "vnd.jp.co.johospace/jorlle-favorite";
	
	/** メタデータ **/
	String METADATA = "metadata";
	/** 更新タイムスタンプ **/
	String UPDATE_TIMESTAMP = "update_timestamp";
}
