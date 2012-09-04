package jp.co.johospace.jsphoto.grid;


import java.io.IOException;
import java.util.ArrayList;

import android.media.ExifInterface;

public class ExifContent8 extends ExifContent {

	@Override
	protected boolean loadExifContent(String path) {
		try{
			ExifInterface exif = new ExifInterface(path);
			String[] tags = new String[]{
					ExifInterface.TAG_DATETIME,
					ExifInterface.TAG_FLASH,
					ExifInterface.TAG_FOCAL_LENGTH,
					ExifInterface.TAG_GPS_DATESTAMP,
					ExifInterface.TAG_GPS_PROCESSING_METHOD,
					ExifInterface.TAG_GPS_TIMESTAMP,
					ExifInterface.TAG_IMAGE_LENGTH,
					ExifInterface.TAG_IMAGE_WIDTH,
					ExifInterface.TAG_MAKE,
					ExifInterface.TAG_MODEL,
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.TAG_WHITE_BALANCE
			};
			for(String tag: tags)registerExif(exif, tag);
		}catch(IOException e){
			return false;
		}
		return true;
	}
	
	private void registerExif(ExifInterface exif, String tag){
		String value = exif.getAttribute(tag);
		if(value != null)
			registerContent(tag, value);
	}

	@Override
	protected boolean loadExifContent(ArrayList<String> pathList) {
		return load(pathList);
	}

}
