package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;


/**
 * オートアルバムキャッシュ 列定義
 */
public interface CCachedAutoAlbum {
	String $TABLE = "extsv_cached_auto_album";
	
	String PK = "pk";
	String LAST_UPDATED = "last_updated";
	String CURRENT_ETAG = "current_etag";
	
	String PK_VALUE = CCachedAutoAlbum.class.getSimpleName();
}
