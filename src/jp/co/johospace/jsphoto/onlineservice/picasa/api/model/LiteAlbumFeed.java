package jp.co.johospace.jsphoto.onlineservice.picasa.api.model;

import java.util.List;

import com.google.api.client.util.Key;


/**
 * 軽量アルバムフィード
 */
public class LiteAlbumFeed extends Feed {
	@Key("entry")
	public List<LitePhotoEntry> photos;
}
