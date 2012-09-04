package jp.co.johospace.jsphoto.v2.onlineservice.sync.db;

/**
 * 列定義 ローカル同期
 */
public interface CLocalSync {

	/** テーブル名 */
	String $TABLE = "local_sync";
	
	
	/** ID **/
	String _ID = "_id";
	
	
	/** ディレクトリパス **/
	String DIRPATH = "dirpath";
	/** 名前 **/
	String NAME = "name";
	/** メタデータタイムスタンプ **/
	String METADATA = "metadata";
	/** メタデータタイムスタンプ（前回） **/
	String PREV_METADATA = "prev_metadata";
	/** 同期状態 **/
	String STATUS = "status";
	/** メタデータ新 **/
	String METADATA_NEW = "metadata_new";
}
