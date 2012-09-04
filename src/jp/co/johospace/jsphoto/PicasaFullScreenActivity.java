package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.cache.PicasaCache;
import jp.co.johospace.jsphoto.fullscreen.ImageSurfaceView;
import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoaderFactory;
import jp.co.johospace.jsphoto.fullscreen.loader.PicasaImageLoaderFactory;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Picasa参照 フルスクリーン
 */
public class PicasaFullScreenActivity extends FullScreenActivity {

	private PicasaCache mCache;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String account = pref.getString("picasaAccount", null);
		mCache = new PicasaCache(this, account);
	}
	
	@Override
	protected ImageLoaderFactory createImageLoaderFactory() {
		return new PicasaImageLoaderFactory(getApplicationContext());
	}
	
	@Override
	protected boolean isFullScreenTarget(String tag) {
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0, MENU_SETTING, Menu.NONE, R.string.menu_setting).setIcon(R.drawable.ic_setting);
		menu.add(0, MENU_INFO, Menu.NONE, R.string.image_context_info);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	}
	
	@Override
	protected void recreateSurfaceView(int pos) {
		FrameLayout frame = (FrameLayout)findViewById(R.id.flFullscreen);
		frame.removeAllViews();
		if(mSurfaceView != null)mSurfaceView.dispose();
		
		mTagPos = pos;
		
		frame.addView(
			mSurfaceView = new ImageSurfaceView(this, mFactory, mTags, mTagPos)
		);
		
		mSurfaceView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mShowTop){
					hideTopbar();
					mHandler.removeCallbacksAndMessages(null);
				}else{
					showTopbar();
					mHandler.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							hideTopbar();
						}
					}, TIME_TO_HIDE);
				}
			}
		});
	}
	
	@Override
	protected void showInfo() {
		String current = mSurfaceView != null ? mSurfaceView.getCurrentTag().toString() : null;
		if (current != null) {
			Intent intent = new Intent(this, PicasaMediaInfoActivity.class);
			intent.putExtra(PicasaMediaInfoActivity.EXTRA_ACCOUNT, mCache.getAccount());
			intent.putExtra(PicasaMediaInfoActivity.EXTRA_TAG, current);
			startActivity(intent);
		}
	}
	
	@Override
	protected Long getCurrentDate() {//TODO
//		try {
//			String tag = mSurfaceView.getCurrentTag();
//			String[] key = PicasaCache.decodeTag(tag);
//			PhotoEntry entry = mCache.loadMediaEntry(key[0], key[1]);
//			return entry.title;
//		} catch (IOException e) {
//			return "";
//		}
		return null;
	}
	
	@Override
	protected void onTapHomeIcon() {
		Intent intent = new Intent(this, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
}
