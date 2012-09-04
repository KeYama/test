package jp.co.johospace.jsphoto.onlineservice.picasa.api.model;

import java.util.List;

import com.google.api.client.util.Key;

public class AlbumFeed extends Feed {
	@Key("entry")
	public List<PhotoEntry> photos;
}
