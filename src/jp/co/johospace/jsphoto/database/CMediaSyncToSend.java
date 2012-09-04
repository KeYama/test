package jp.co.johospace.jsphoto.database;

/**
 * 列定義 メディア同期送信
 */
public interface CMediaSyncToSend {

	/** テーブル名 */
	String $TABLE = "media_sync_to_send";
	
	
	/** サービスタイプ **/
	String SERVICE_TYPE_TO_SEND = "service_type_to_send";
	/** サービスアカウント **/
	String SERVICE_ACCOUNT_TO_SEND = "service_account_to_send";
	/** メディアID **/
	String MEDIA_ID_TO_SEND = "media_id_to_send";
	/** 操作 */
	String OPERATION_TO_SEND = "operation_to_send";
	/** 操作： 挿入 */
	String OP_INSERT = "INS";
	/** 操作： 更新 */
	String OP_UPDATE = "UPD";
	/** 操作： 削除 */
	String OP_DELETE = "DEL";
}
