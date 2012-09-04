package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;

/**
 * キャッシュメタデータ 列定義
 */
public interface CCachedMetadata extends CServiceIdentifier {
	String $TABLE = "extsv_cached_metadata";
	
	String _ID = "_id";
	String MEDIA_ID = "media_id";
	String METADATA_TYPE = "metadata_type";
	String METADATA = "metadata";
}
