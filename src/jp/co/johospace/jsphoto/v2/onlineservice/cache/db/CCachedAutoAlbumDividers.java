package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;

/**
 * オートアルバムキャッシュ 区切り 列定義
 */
public interface CCachedAutoAlbumDividers {
	String $TABLE = "extsv_cached_auto_album_dividers";
	
	String _ID = "_id";
	String CATEGORY_NAME = "category_name";
	String ORDER_SEQ = "order_seq";
	String IS_EVENT = "is_event";
	String MONTH_DATE = "month_date";
	String TITLE = "title";
}
