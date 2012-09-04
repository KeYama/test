package jp.co.johospace.jsphoto.cache;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;

/**
 * Picasa参照 キャッシュローダー
 */
public class PicasaImageCacheLoader extends CacheLoader {

	private final PicasaCache mCache;
	public PicasaImageCacheLoader(Context context, String account) {
		super();
		mCache = new PicasaCache(context, account);
	}
	
	@Override
	protected Bitmap loadCache(RequestData data, Options opts)
			throws IOException {
		return mCache.loadMiniThumb(data.folder, data.name, opts);
	}
}
