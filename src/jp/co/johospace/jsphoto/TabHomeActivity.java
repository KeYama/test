package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.managed.AutoAlbumNavigationGroup;
import jp.co.johospace.jsphoto.managed.LocalNavigationGroup;
import jp.co.johospace.jsphoto.managed.NavigationGroup;
import jp.co.johospace.jsphoto.managed.OnlineNavigationGroup;
import jp.co.johospace.jsphoto.managed.TagNavigationGroup;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.util.SizeConv;
import jp.co.johospace.jsphoto.view.TabItemView;
import android.app.TabActivity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

/**
 * ホーム画面のアクティビティです
 */
public class TabHomeActivity extends TabActivity {

	/** タブホスト */
	private TabHost mTabHost;
	/** インデックス　タグ種類検索 */
	private static final int
		INDEX_TAG_METADATA = 1;

	/** DBアクセス */
	private static SQLiteDatabase mDatabase;

	/** タブインデックス_ローカル */
	private static final int TAB_LOCAL = 0;
	/** タブインデックス_オンライン */
	private static final int TAB_ONLINE = 1;
	/** タブインデックス_タグ */
	private static final int TAB_TAG = 2;
	/** タブインデックス_オートアルバム */
	private static final int TAB_AUTO = 3;
	/** タブインデックス_カメラ */
	private static final int TAB_CAMERA = 4;

	/** アプリ内カメラディレクトリ */
	public static final String CAMERA_PATH = "/jsphoto";

	private SizeConv sc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sc = new SizeConv(this);
		
		// オンライン系の自動更新状態をクリア
		AutoAlbumListActivity.autoUpdated = false;
		OnlineListActivity.autoUpdated = false;

		// SDカードの判定
		if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {

			// 存在しない場合は、別画面を起動し、処理停止
			Intent intent = new Intent(this, NoMountActivity.class);

			startActivity(intent);
			finish();
			return;
		}

		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// データベース
		mDatabase = OpenHelper.external.getDatabase();

		setCategory();

		// タブの高さを調整
		android.view.ViewGroup.LayoutParams lp = getTabWidget().getLayoutParams();
		lp.height = (int)sc.getSize(47);
		getTabWidget().setLayoutParams(lp);

		// 初回時のみイントロ画面の起動
		showIntro();

	}

	/**
	 * イントロ画面の起動
	 */
	private void showIntro(){

		//初回起動時、イントロ画面を表示する
		boolean notFirst = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_NOT_FIRST_TIME);
		if(!notFirst){
			startActivityForResult(new Intent(this, IntroActivity.class), ApplicationDefine.REQUEST_PREF_TUTORIAL_INTRO);
		}

	}

	/**
	 * 各カテゴリのレイアウトを設定します
	 */
	public void setCategory() {

		if (mTabHost != null) mTabHost.clearAllTabs();

		// ローカル
		setLayoutLocal();
		// オンラインフォルダ
		setLayoutSync();
		// タグ
		setLayoutTag();
		// オートアルバム
		setAutoAlbum();
		// カメラ
		setLayoutCamera();
		
		// カメラタブは幅を調整
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, (int)sc.getSize(37));
		params.setMargins(10, 10, 0, 10);
		mTabHost.getTabWidget().getChildAt(TAB_CAMERA).setLayoutParams(params);
		// TabActivityからカメラ起動できなかった為、リスナー設定
//		mTabHost.getTabWidget().getChildAt(TAB_CAMERA).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//
//				// 保存先のフォルダを作成
//				File f = new File(ApplicationDefine.PATH_JSMEDIA);
//				if (!f.exists()) f.mkdirs();
//
//				// 指定する保存先を作成
//				File mTmpFile = new File(ApplicationDefine.PATH_JSMEDIA, String.valueOf(System.currentTimeMillis()) + ".jpg");
//
//				// インテントの生成
//				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//				// 保存を指定
//				intent.putExtra(MediaStore.EXTRA_OUTPUT, android.net.Uri.fromFile(mTmpFile));
//
//				// カメラを起動
//				startActivityForResult(intent,TAB_CAMERA);
//			}
//		});
		
		
		// タブが変更された後の処理
		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {

			@Override
			public void onTabChanged(String tabId) {
				
	    		// 一旦画像の表示をデフォルトに戻す
	    		for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {

	    			String strTabId = null;
	    			
	    			switch (i) {
					case TAB_LOCAL:
						strTabId = ApplicationDefine.TAB_LOCAL;
						break;
					case TAB_ONLINE:
						strTabId = ApplicationDefine.TAB_ONLINE;
						break;
					case TAB_TAG:
						strTabId = ApplicationDefine.TAB_TAG;
						break;
					case TAB_AUTO:
						strTabId = ApplicationDefine.TAB_AUTO;
						break;
					}

	    			// タブごとのヒストリーを初期化
	    			NavigationGroup navigationGroup = null;
	    			
	    			if (strTabId != null && !tabId.equals(strTabId)) {
	    				navigationGroup = (NavigationGroup)getLocalActivityManager().getActivity(strTabId);
	    			}
	    			
	    			if (navigationGroup != null) {
	    				navigationGroup.doClean(TabHomeActivity.this);
	    			}
	    			
	    		}
			}

		});
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == ApplicationDefine.REQUEST_PREF_TUTORIAL_INTRO) {
			// 初回チュートリアルで未承諾の場合は、アプリケーションを終了
			boolean notFirst = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_NOT_FIRST_TIME);
			if(!notFirst){
				finish();
			}
			
		} else if(requestCode == 4){

			// カメラアイコンをデフォルトに戻す
			TabItemView tab = (TabItemView) mTabHost.getTabWidget().getChildAt(TAB_CAMERA);
			ImageView image = tab.mImageView;
			image.setImageResource(R.drawable.cam);
			
			//撮影した画像をスキャンする
			if(resultCode == RESULT_OK){
				
				String uri = tab.getEntryUri();
				if(uri != null){
					String[] paths = {uri};
					String[] mimeTypes = {"image/*"};
					
					MediaScannerConnection.scanFile(this, paths, mimeTypes,  new OnScanCompletedListener() {
		
						@Override
						public void onScanCompleted(String s, Uri uri) {
						}
					});
				}
			}
		}
    }


	/**
	 * タブを作る
	 * @param text
	 */
	private void createTab(Intent intent, String tabCategory, int drawable, boolean isCamera) {

		mTabHost = getTabHost();

		TabSpec spec = mTabHost.newTabSpec(tabCategory);
		View childView = new TabItemView(this, tabCategory, drawable);
		spec.setIndicator(childView);

		// タブの中身
		intent.putExtra(ApplicationDefine.EXTRA_LAUNCHER, "TAB");
		spec.setContent(intent);
		mTabHost.addTab(spec);
	}


	/**
	 * カテゴリ　ローカル設定
	 */
	public void setLayoutLocal() {

		// ローカル起動用インテントセット
		Intent intent = new Intent().setClass(this, LocalNavigationGroup.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// タブ切替時の遷移先
		intent.putExtra(ApplicationDefine.TAB_LOCAL, ApplicationDefine.TAB_TOP);
		createTab(intent, ApplicationDefine.TAB_LOCAL, R.drawable.tab_local, false);

	}

	/**
	 * カテゴリ　オートアルバム設定
	 */
	public void setAutoAlbum() {

		// オートアルバム起動用インテントセット
		Intent intent = new Intent(TabHomeActivity.this, AutoAlbumNavigationGroup.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(ApplicationDefine.TAB_AUTO, ApplicationDefine.TAB_TOP);
		createTab(intent, ApplicationDefine.TAB_AUTO, R.drawable.tab_auto, false);
	}



	/**
	 * カテゴリ　オンラインフォルダ設定
	 */
	public void setLayoutSync() {

		// 同期起動用インテントセット
		Intent intent = new Intent(TabHomeActivity.this, OnlineNavigationGroup.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// タブ切替時の遷移先
        intent.putExtra(ApplicationDefine.TAB_ONLINE, ApplicationDefine.TAB_TOP);

		createTab(intent, ApplicationDefine.TAB_ONLINE, R.drawable.tab_online, false);
	}


	/**
	 * カテゴリ　タグ設定
	 */
	public void setLayoutTag() {
		
		Intent intent = new Intent(TabHomeActivity.this, TagNavigationGroup.class);

		// タブ切替時の遷移先
        intent.putExtra(ApplicationDefine.TAB_TAG, ApplicationDefine.TAB_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		createTab(intent, ApplicationDefine.TAB_TAG, R.drawable.tab_tag, false);

	}


	/**
	 * カメラ起動
	 */
	public void setLayoutCamera() {

		Intent intentCamera = new Intent();
		createTab(intentCamera, ApplicationDefine.TAB_CAMERA, R.drawable.tab_camera, true);

	}

	/*
	 * 現在表示中のActivityに処理を任せる
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return getLocalActivityManager().getCurrentActivity().onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}

}
