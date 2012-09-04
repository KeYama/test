package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;

/**
 * オートアルバムキャッシュ メディア 列定義
 */
public interface CCachedAutoAlbumMedias {
	String $TABLE = "extsv_cached_auto_album_medias";
	
	String _ID = "_id";
	String DIVIDER_ID = "divider_id";
	String SERVICE_TYPE = "service_type";
	String SERVICE_ACCOUNT = "service_account";
	String DIR_ID = "dir_id";
	String MEDIA_ID = "media_id";
	String MEDIA_URI = "media_uri";
	String FILE_NAME = "file_name";
	String PRODUCTION_DATE = "production_date";
	String THUMBNAIL_DATA = "thumbnail_data";
	String VERSION = "version";
	String UPDATED = "updated";
	String ORDER_SEQ = "order_seq";
	String CATEGORY_NAME = "category_name";
	String RANDOM_ORDER = "random_order";
}
