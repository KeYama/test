package jp.co.johospace.jsphoto.grid;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.graphics.BitmapFactory;

public abstract class ExifContent {
	List<String> mKeys = new ArrayList<String>();
	List<String> mValues = new ArrayList<String>();
	
	public boolean load(String path){
		if(!loadFileContent(path)) return false;
		if(!loadSizeContent(path)) return false;
		
		return loadExifContent(path);
	}
	
	public boolean load(ArrayList<String> pathList){
		long size = 0;
		for (String path : pathList) {
			File f = new File(path);
			size = size + f.length();
		}
		registerContent("File Count", String.valueOf(pathList.size()));
		registerContent("File Size", formatFileSize(size));
		
		return true;
	}
	
	
	private boolean loadFileContent(String path){
		registerContent("File Path", path);
		File f = new File(path);
		registerContent("File Size", formatFileSize(f.length()));
		
		return true;
	}
	
	private boolean loadSizeContent(String path){
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, opt);
		registerContent("Size", opt.outWidth + " x " + opt.outHeight);
		return true;
	}
	
	private String formatFileSize(long size){
		return (size / 1024) + "KB";
	}
	
	protected abstract boolean loadExifContent(String path);
	protected abstract boolean loadExifContent(ArrayList<String> pathList);
	public int getNumContent(){
		return mKeys.size();
	}
	public String getKey(int num){
		return mKeys.get(num);
	}
	public String getValue(int num){
		return mValues.get(num);
	}
	
	protected void registerContent(String key, String value){
		mKeys.add(key);
		mValues.add(value);
	}
}
