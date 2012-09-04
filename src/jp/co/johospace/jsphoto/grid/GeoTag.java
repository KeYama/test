package jp.co.johospace.jsphoto.grid;

import android.media.ExifInterface;

public class GeoTag {
	private ExifInterface mExif;
	private float[] mLatLon = new float[2];
	private boolean mHasGeoTag = false;
	
	public GeoTag(String path){
		try {
			mExif = new ExifInterface(path);
			
			if(mExif.getLatLong(mLatLon))
				mHasGeoTag = true;
		} catch (Exception e) {
			mHasGeoTag = false;
		}
	}
	
	public boolean hasGeoTag(){
		return mHasGeoTag;
	}
	
	public String toGeoLocation(){
		return "geo:" + mLatLon[0] + "," + mLatLon[1];
	}
	

}
