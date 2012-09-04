package jp.co.johospace.jsphoto.database;

/**
 * 列定義 シークレットフォルダ
 */
public interface CSecretFolder {

	/** テーブル名 */
	String $TABLE = "secret_folders";
	
	/** ID **/
	String _ID = "_id";
	/** ディレクトリパス **/
	String DIRPATH = "dirpath";
}
