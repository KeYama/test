package jp.co.johospace.jsphoto.grid;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageShrinker {
	public String mFile;
	
	
	private static final int[][] SIZE = {
		{320, 240},
		{640, 480},
		{800, 600},
		{1024, 768},
		{1280, 960},
		{1920, 1400}
	};
	
	public static class Size{
		public int width;
		public int height;
		public int realWidth;
		public int realHeight;
	}
	
	public ImageShrinker(String file){
		mFile = file;
	}
	
	public boolean canResize(Size size){
		BitmapFactory.Options opt = new BitmapFactory.Options();
		
		opt.inJustDecodeBounds = true;
		opt.inScaled = false;
		
		BitmapFactory.decodeFile(mFile, opt);
		
		if(opt.outWidth * opt.outHeight > 2560 * 1920){
			return false;
		}
		if(size.width * size.height > 1920 * 1440){
			//TODO NDKたたかないと無理
			return false;
		}
		
		return true;
	}
	
	public List<Size> getAvailableSize(){
		BitmapFactory.Options opt = new BitmapFactory.Options();
		
		opt.inJustDecodeBounds = true;
		opt.inScaled = false;
		
		BitmapFactory.decodeFile(mFile, opt);
		
		int width = opt.outWidth;
		int height = opt.outHeight;
		List<ImageShrinker.Size> sizeList = new ArrayList<ImageShrinker.Size>();
		
		if(width > height){
			float ratio = (float)height / (float)width;
			for(int n = 0; n < SIZE.length; ++n){
				int[] size = SIZE[n];
				if(size[0] < width){
					Size s = new Size();
					s.width = size[0];
					s.height = (int)(size[0]*ratio);
					s.realWidth = width;
					s.realHeight = height;
					sizeList.add(s);
				}
			}
		}else{
			float ratio = (float)width / (float)height;
			for(int n = 0; n < SIZE.length; ++n){
				int[] size = SIZE[n];
				if(size[0] < height){
					Size s = new Size();
					s.height = size[0];
					s.width = (int)(size[0]*ratio);
					s.realWidth = width;
					s.realHeight = height;
					sizeList.add(s);
				}
			}
		}
		
		
		return sizeList;
	}
	
	public Bitmap shrink(Size size, BitmapFactory.Options opt){
		if(!canResize(size))return null;
		
		opt.inDensity = size.realWidth;
		opt.inTargetDensity = size.width;
		opt.inPreferredConfig = Bitmap.Config.RGB_565;
		
		return BitmapFactory.decodeFile(mFile, opt);
	}
	
	public String getPath(){
		return mFile;
	}
}
