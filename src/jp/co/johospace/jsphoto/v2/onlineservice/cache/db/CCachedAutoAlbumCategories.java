package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;

/**
 * オートアルバムキャッシュ カテゴリ 列定義
 */
public interface CCachedAutoAlbumCategories {
	String $TABLE = "extsv_cached_auto_album_categories";
	
	String _ID = "_id";
	String ORDER_SEQ = "order_seq";
	String CATEGORY_NAME = "category_name";
	String DIVIDER_COUNT = "divider_count";
	String MEDIA_COUNT = "media_count";
}
