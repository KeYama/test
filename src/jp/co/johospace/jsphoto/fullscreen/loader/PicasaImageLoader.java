package jp.co.johospace.jsphoto.fullscreen.loader;

import java.io.File;
import java.io.IOException;

import jp.co.johospace.jsphoto.cache.PicasaCache;
import android.content.Context;
import android.graphics.Bitmap;

/**
 * Picasa参照 イメージローダ
 */
public class PicasaImageLoader extends LocalImageLoader {

	private final Context mContext;
	private final String mAccount;
	private final PicasaCache mCache;
	public PicasaImageLoader(Context context, String account) {
		super();
		mContext = context;
		mAccount = account;
		mCache = new PicasaCache(mContext, mAccount);
	}
	
	@Override
	public Bitmap loadFullImage(Object tag) {
		File cacheFile;
		try {
			cacheFile = mCache.loadFullSizeImage(tag.toString());
		} catch (IOException e) {
			return null;
		}
		
		return super.loadFullImage(cacheFile.getAbsolutePath());
	}

	@Override
	public Bitmap loadThumbnailImage(Object tag, int screenWidth,
			int screenHeight) {
		File cacheFile;
		try {
			cacheFile = mCache.loadLargeThumb(tag.toString());
		} catch (IOException e) {
			return null;
		}
		
		return super.loadThumbnailImage(cacheFile.getAbsolutePath(), screenWidth, screenHeight);
	}

}
