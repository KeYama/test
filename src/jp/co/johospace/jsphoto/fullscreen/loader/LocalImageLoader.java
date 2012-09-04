package jp.co.johospace.jsphoto.fullscreen.loader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class LocalImageLoader implements ImageLoader{
	
	//private static final int BIG_BITMAP = 1500;
	private static final int BIG_BITMAP = 2480;

	@Override
	public Bitmap loadFullImage(Object path) {
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inScaled = false;
		
		opt.inJustDecodeBounds = true;
		opt.inPreferredConfig = Bitmap.Config.RGB_565;
		
		BitmapFactory.decodeFile(path.toString(), opt);
		
		if(opt.outWidth*opt.outHeight > BIG_BITMAP*BIG_BITMAP){
			opt.inSampleSize = (opt.outWidth*opt.outHeight) / (BIG_BITMAP*BIG_BITMAP);
		}
		
		opt.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(path.toString(), opt);
	}

	@Override
	public Bitmap loadThumbnailImage(Object path, int screenWidth,
			int screenHeight) {
		BitmapFactory.Options opt = new BitmapFactory.Options();
		
		opt.inJustDecodeBounds = true;
		opt.inScaled = false;
		opt.inPreferredConfig = Bitmap.Config.RGB_565;
		
		BitmapFactory.decodeFile(path.toString(), opt);
		
		opt.inJustDecodeBounds = false;
		opt.inScaled = true;
		opt.inPurgeable = true;
		
		int sampleWidth = opt.outWidth / screenWidth;
		int sampleHeight = opt.outHeight / screenHeight;
		opt.inSampleSize = (sampleWidth > sampleHeight)? sampleWidth : sampleHeight;
		if(opt.inSampleSize == 0)opt.inSampleSize = 1;
		
		while(true){
			try{
				return BitmapFactory.decodeFile(path.toString(), opt);
			}catch(OutOfMemoryError e){
				opt.inSampleSize += 1;
			}
		}
	}

	@Override
	public void cancel(Object tag) {
	}
}
