package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;

/**
 * キャッシュファイル 列定義
 */
public interface CCachedFiles extends CServiceIdentifier {
	String $TABLE = "extsv_cached_files";
	
	String _ID = "_id";
	String MEDIA_ID = "media_id";
	String CONTENT_TYPE = "content_type";
}
