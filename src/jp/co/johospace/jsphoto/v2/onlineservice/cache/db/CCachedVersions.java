package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;

/**
 * キャッシュバージョン 列定義
 */
public interface CCachedVersions extends CServiceIdentifier {
	String $TABLE = "extsv_cached_versions";
	
	String VERSION = "version";
	String LAST_UPDATED = "last_updated";
}
