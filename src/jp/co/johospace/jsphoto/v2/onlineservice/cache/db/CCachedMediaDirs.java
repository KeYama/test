package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;

/**
 * キャッシュメディアディレクトリ 列定義
 */
public interface CCachedMediaDirs extends CServiceIdentifier {
	String $TABLE = "extsv_cached_media_dirs";
	
	String _ID = "_id";
	String DIR_ID = "dir_id";
	String DIR_NAME = "dir_name";
	String VERSION = "version";
	String UPDATED = "updated";
	String MEDIA_COUNT = "media_count";
	String MEDIA_CACHED = "media_cached";
	String MEDIA_DIRTY = "media_dirty";
	String MEDIA_VERSION = "media_version";
	String LAST_UPDATED = "last_updated";
}
