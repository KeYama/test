package jp.co.johospace.jsphoto.v2.onlineservice.sync.db;

/**
 * 列定義 メディア同期バージョン
 */
public interface CMediaSyncVersions {

	/** テーブル名 */
	String $TABLE = "media_sync_versions";
	
	
	/** ID **/
	String _ID = "_id";
	/** サービスタイプ **/
	String SERVICE_TYPE = "service_type";
	/** サービスアカウント **/
	String SERVICE_ACCOUNT = "service_account";
	/** 同期済みバージョン **/
	String SYNCED_VERSION = "synced_version";
}
