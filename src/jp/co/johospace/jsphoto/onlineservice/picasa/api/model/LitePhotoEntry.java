package jp.co.johospace.jsphoto.onlineservice.picasa.api.model;

import java.util.List;

import com.google.api.client.util.Data;
import com.google.api.client.util.Key;

/**
 * 軽量フォトエントリ
 */
public class LitePhotoEntry {

	@Key("@gd:etag")
	public String etag;

//	@Key
//	public String id;

	@Key("gphoto:id")
	public String gphotoId;

	@Key("gphoto:albumid")
	public String gphotoAlbumId;

//	@Key("gphoto:timestamp")
//	public String timestamp;

	@Key("media:group")
	public MediaGroup mediaGroup;
	
	public static class MediaGroup {
		@Key("media:content")
		public MediaContent content;
//		@Key("media:keywords")
//		public String keywords;
		@Key("media:thumbnail")
		public List<MediaThumbnail> thumbnails;

		public static class MediaContent {

			@Key("@type")
			public String type;

			@Key("@url")
			public String url;
		}

		public static class MediaThumbnail {

			@Key("@url")
			public String url;

			@Key("@width")
			public int width;

			@Key("@height")
			public int height;
		}

	}

//	@Key
//	public String summary;
//
//	@Key
//	public String title;
//
//	@Key
//	public String updated;
	
//	@Key
//	public Category category = Category.newKind("photo");

//	@Key("exif:tags")
//	public ExifTags exifTags;
//
//	@Key("gphoto:width")
//	public int width;
//
//	@Key("gphoto:height")
//	public int height;
//
//	@Key("gphoto:size")
//	public long size;

//	@Key("link")
//	public List<Link> links;
//
//	public String getFeedLink() {
//		return Link.find(links, "http://schemas.google.com/g/2005#feed");
//	}
//
//	public String getSelfLink() {
//		return Link.find(links, "self");
//	}
//
//	public String getEditLink() {
//		return Link.find(links, "edit");
//	}

	@Override
	protected Entry clone() {
		try {
			Entry result = (Entry) super.clone();
			Data.deepCopy(this, result);
			return result;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
}
