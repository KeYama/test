package jp.co.johospace.jsphoto.fullscreen.loader;
import jp.co.johospace.jsphoto.PicasaPrefsActivity;
import android.content.Context;

/**
 * Picasa参照 イメージローダーファクトリ
 */
public class PicasaImageLoaderFactory implements ImageLoaderFactory {

	private final Context mContext;
	public PicasaImageLoaderFactory(Context context) {
		super();
		mContext = context;
	}
	
	@Override
	public ImageLoader create() {
		String account = PicasaPrefsActivity.getPicasaAccount(mContext);
		return new PicasaImageLoader(mContext, account);
	}

}
