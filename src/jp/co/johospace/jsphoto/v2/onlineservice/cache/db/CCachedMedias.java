package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;

/**
 * キャッシュメディア 列定義
 */
public interface CCachedMedias extends CServiceIdentifier {
	String $TABLE = "extsv_cached_medias";
	
	String _ID = "_id";
	String DIR_ID = "dir_id";
	String MEDIA_ID = "media_id";
	String MEDIA_URI = "media_uri";
	String FILE_NAME = "file_name";
	String PRODUCTION_DATE = "production_date";
	String THUMBNAIL_DATA = "thumbnail_data";
	String VERSION = "version";
	String UPDATED = "updated";
	String METADATA_CACHED = "metadata_cached";
	String ALBUM_PHOTO = "album_photo";
}
