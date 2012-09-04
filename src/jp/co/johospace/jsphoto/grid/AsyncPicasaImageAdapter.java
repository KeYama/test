package jp.co.johospace.jsphoto.grid;

import jp.co.johospace.jsphoto.PicasaPrefsActivity;
import jp.co.johospace.jsphoto.cache.CacheLoader;
import jp.co.johospace.jsphoto.cache.PicasaImageCacheLoader;
import android.content.Context;

/**
 * Picasa参照 非同期画像アダプタ
 */
public class AsyncPicasaImageAdapter extends AsyncImageAdapter {

	public AsyncPicasaImageAdapter(Context context) {
		super(context);
	}
	
	@Override
	protected CacheLoader createLoader() {
		String account = PicasaPrefsActivity.getPicasaAccount(mContext);
		return new PicasaImageCacheLoader(mContext, account);
	}
}
