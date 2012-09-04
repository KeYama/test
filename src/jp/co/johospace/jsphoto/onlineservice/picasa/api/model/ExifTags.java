package jp.co.johospace.jsphoto.onlineservice.picasa.api.model;

import com.google.api.client.util.Key;

/**
 * Exifタグ
 */
public class ExifTags {
	@Key("exif:fstop")
	public String fstop;
	@Key("exif:make")
	public String make;
	@Key("exif:model")
	public String model;
	@Key("exif:exposure")
	public String exposure;
	@Key("exif:flash")
	public Boolean flash;
	@Key("exif:focallength")
	public String focallength;
	@Key("exif:iso")
	public String iso;
	@Key("exif:time")
	public Long time;
	@Key("exif:imageUniqueID")
	public String imageUniqueID;
}
