package jp.co.johospace.jsphoto.onlineservice.picasa.api.model;

import com.google.api.client.util.Key;

/**
 * メディアサムネイル media:thumbnail
 */
public class MediaThumbnail {

	@Key("@url")
	public String url;
	
	@Key("@width")
	public int width;
	
	@Key("@height")
	public int height;
}
