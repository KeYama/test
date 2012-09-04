package jp.co.johospace.jsphoto.database;

/**
 * 列定義 メディア同期
 */
public interface CMediaSync {

	/** テーブル名 */
	String $TABLE = "media_sync";
	
	
	/** ID **/
	String _ID = "_id";
	/** サービスタイプ **/
	String SERVICE_TYPE = "service_type";
	/** サービスアカウント **/
	String SERVICE_ACCOUNT = "service_account";
	
	
	/** ディレクトリパス **/
	String DIRPATH = "dirpath";
	/** 名前 **/
	String NAME = "name";
	/** 制作日時 **/
	String PRODUCTION_DATE = "production_date";
	/** 最終同期時刻 **/
	String SYNC_TIME = "sync_time";
	/** 同期状態 **/
	String SYNC_STATUS = "sync_status";
	/** 同期キャッシュ **/
	String SYNC_CACHE = "sync_cache";
	/** ローカルタイムスタンプ */
	String LOCAL_TIMESTAMP = "local_timestamp";
	/** メタデータタイムスタンプ */
	String LOCAL_METADATA_TIMESTAMP = "local_metadata_timestamp";
	
	
	/** メディアID **/
	String MEDIA_ID = "media_id";
	/** ディレクトリID **/
	String DIRECTORY_ID = "directory_id";
	/** メディアURI **/
	String MEDIA_URI = "media_uri";
	/** リモートバージョン **/
	String REMOTE_VERSION = "remote_version";
	/** 同期データ1 **/
	String SYNC_DATA1 = "sync_data1";
	/** 同期データ2 **/
	String SYNC_DATA2 = "sync_data2";
	/** 同期データ3 **/
	String SYNC_DATA3 = "sync_data3";
	/** 同期データ4 **/
	String SYNC_DATA4 = "sync_data4";
	/** 同期データ5 **/
	String SYNC_DATA5 = "sync_data5";
}
