package jp.co.johospace.jsphoto.v2.onlineservice.cache.db;

/**
 * キャッシュサムネイル 列定義
 */
public interface CCachedThumbs extends CServiceIdentifier {
	String $TABLE = "extsv_cached_thumbs";
	
	String _ID = "_id";
	String MEDIA_ID = "media_id";
	String SIZE_HINT = "size_hint";
	String THUMB_LENGTH = "thumb_length";
	String THUMBNAIL = "thumbnail";
}
