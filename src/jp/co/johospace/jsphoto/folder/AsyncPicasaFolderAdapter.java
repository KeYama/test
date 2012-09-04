package jp.co.johospace.jsphoto.folder;

import java.util.List;

import jp.co.johospace.jsphoto.PicasaPrefsActivity;
import jp.co.johospace.jsphoto.LocalFolderActivity.FolderEntry;
import jp.co.johospace.jsphoto.cache.CacheLoader;
import jp.co.johospace.jsphoto.cache.PicasaImageCacheLoader;
import android.content.Context;

/**
 * Picasa参照 非同期フォルダアダプタ
 */
public class AsyncPicasaFolderAdapter extends AsyncFolderAdapter {

	public AsyncPicasaFolderAdapter(Context context, List<FolderEntry> entries) {
		super(context, entries);
	}
	
	@Override
	protected CacheLoader createLoader() {
		String account = PicasaPrefsActivity.getPicasaAccount(mContext);
		return new PicasaImageCacheLoader(mContext, account);
	}

}
