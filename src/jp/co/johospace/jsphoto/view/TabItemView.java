package jp.co.johospace.jsphoto.view;

import java.io.File;

import jp.co.johospace.jsphoto.LocalFolderActivity;
import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.TabHomeActivity;
import jp.co.johospace.jsphoto.TagListActivity;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.managed.AutoAlbumNavigationGroup;
import jp.co.johospace.jsphoto.managed.NavigationGroup;
import jp.co.johospace.jsphoto.managed.OnlineNavigationGroup;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

public class TabItemView extends FrameLayout {
	
	/** タブのアイコン */
	public ImageView mImageView;
	/** タブのテキスト */
	public TextView mTextView;
	/** タップのリスナー */
	private String mCategory;
	/** タブホスト */
	private TabHost mTabHost;
	/** コンテキスト */
	private Context mContext;
	
//	private JsMediaServerClient mJsMedia;
	
	LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);	
	
	public TabItemView(Context context) {
		super(context);
	}
	
    /**
     * タブ一つのレイアウトを設定
     * @param context 
     * @param tabCategory タブの種別
     */
    public TabItemView(Context context, final String tabCategory, int drawable) {
		this(context);
		
		mContext = context;
		
		final Resources re = getResources();
		
		mCategory = tabCategory;
		mTabHost = ((TabHomeActivity)mContext).getTabHost();
		
		
        View childView = inflater.inflate(R.layout.tab_item, null);
        
        mImageView = (ImageView) childView.findViewById(R.id.imgTab);
        mTextView = (TextView) childView.findViewById(R.id.txtTab);
		// ローカル
		if (mCategory.equals(ApplicationDefine.TAB_LOCAL)) {
			mTextView.setText(re.getString(R.string.home_label_local));
		// オンライン
		} else if (mCategory.equals(ApplicationDefine.TAB_ONLINE)) {
			mTextView.setText(re.getString(R.string.home_label_online));
		// タグ
		} else if (mCategory.equals(ApplicationDefine.TAB_TAG)) {
			mTextView.setText(re.getString(R.string.home_label_tag));
		// オートアルバム
		} else if (mCategory.equals(ApplicationDefine.TAB_AUTO)) {
			mTextView.setText(re.getString(R.string.home_label_auto_album));
		}
        
        
        mImageView.setImageResource(drawable);
        
        // Selecterを有効にするために設定
        mImageView.setFocusable(true);
        mImageView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
                
				// ホーム画面取得
				TabHomeActivity home = (TabHomeActivity)mContext;
				
				// ローカル
				if (mCategory.equals(ApplicationDefine.TAB_LOCAL)) {
					// ローカルタブ以外から遷移
					if (mTabHost.getCurrentTab() != 0) {
						mTabHost.setCurrentTab(0);
					// ローカルタブ上で押下された場合
					} else {
						NavigationGroup navigationGroup = (NavigationGroup)home.getLocalActivityManager().getActivity(ApplicationDefine.TAB_LOCAL);
						navigationGroup.mHistory.clear();
						
						// タブが押下された場合、トップに遷移
						Intent intent = new Intent().setClass(mContext, LocalFolderActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						
						View view = navigationGroup.getLocalActivityManager()
								.startActivity(intent.getComponent().getShortClassName(), intent)
								.getDecorView();
						navigationGroup.replaceView(view);
					}
				// オンライン
				} else if (mCategory.equals(ApplicationDefine.TAB_ONLINE)) {
					// オンラインタブ以外から遷移
					if (mTabHost.getCurrentTab() != 1) {
						mTabHost.setCurrentTab(1);
					// オンラインタブ上で押下された場合
					} else {
						OnlineNavigationGroup navi = (OnlineNavigationGroup) home.getLocalActivityManager().getActivity(ApplicationDefine.TAB_ONLINE);
						navi.startLoading();
//						mJsMedia = ClientManager.getJsMediaServerClient(mContext);
//						new GetPrefsOnlineTask().execute();
					}
				// タグ
				} else if (mCategory.equals(ApplicationDefine.TAB_TAG)) {
					// タグ以外から遷移した場合
					if (mTabHost.getCurrentTab() != 2) {
						mTabHost.setCurrentTab(2);
					// タグで押下された場合
					} else {
						NavigationGroup navigationGroup = (NavigationGroup)home.getLocalActivityManager().getActivity(ApplicationDefine.TAB_TAG);
						navigationGroup.mHistory.clear();
						
						// タブが押下された場合、トップに遷移
						Intent intent = new Intent().setClass(mContext, TagListActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						
						View view = navigationGroup.getLocalActivityManager()
								.startActivity(intent.getComponent().getShortClassName(), intent)
								.getDecorView();
						navigationGroup.replaceView(view);
					}
				// オートアルバム
				} else if (mCategory.equals(ApplicationDefine.TAB_AUTO)) {
					// タブ以外で押下された場合
					if (mTabHost.getCurrentTab() != 3) {
						mTabHost.setCurrentTab(3);
					// タブ上で押下された場合
					} else {
						AutoAlbumNavigationGroup navi = (AutoAlbumNavigationGroup) home.getLocalActivityManager().getActivity(ApplicationDefine.TAB_AUTO);
						navi.startLoading();
//						mJsMedia = ClientManager.getJsMediaServerClient(mContext);
//						new GetPrefsAutoTask().execute();
					}
				
				// カメラ
				} else if (mCategory.equals(ApplicationDefine.TAB_CAMERA)) {
					
					// 保存先のフォルダを作成
					File f = new File(ApplicationDefine.PATH_JSPHOTO);
					if (!f.exists()) f.mkdirs();

					// 指定する保存先を作成
					mTmpFile = new File(ApplicationDefine.PATH_JSPHOTO, String.valueOf(System.currentTimeMillis()) + ".jpg");

					// インテントの生成
					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

					// 保存を指定
					intent.putExtra(MediaStore.EXTRA_OUTPUT, android.net.Uri.fromFile(mTmpFile));

					// カメラを起動
					((TabHomeActivity) mContext).startActivityForResult(intent, 4);
					
				}
			}
		});
        
        addView(childView);
		
	}
    /** カメラで撮った画像を保存するファイル **/
    private File mTmpFile;
    
    /**
     * カメラで撮った画像を保存するUriを返します。
     * @return
     */
    public String getEntryUri(){
    	if(mTmpFile != null){
    		return mTmpFile.getPath();
    	}else{
    		return null;
    	}
    }
	
    
//	/**
//	 * オンラインの非同期処理
//	 */
//	private class GetPrefsOnlineTask extends AsyncTask<Void, Void, List<AuthPreference>> {
//		@Override
//		protected void onPreExecute() {
//			// TODO $ol プログレス表示
//			super.onPreExecute();
//		}
//		
//		@Override
//		protected List<AuthPreference> doInBackground(Void... params) {
//			try {
//				List<AuthPreference> prefs = mJsMedia.getAuthPreferences(false);
//				if (prefs == null) {
//					prefs = mJsMedia.getAuthPreferences(true);
//				}
//				return prefs;
//			} catch (IOException e) {
////				e.printStackTrace();		/*$debug$*/	// TODO $ol
//				return null;
//			}
//		}
//    
//    
//		@Override
//		protected void onPostExecute(List<AuthPreference> result) {
//			
//			// ホーム画面取得
//			TabHomeActivity home = (TabHomeActivity)mContext;
//			
//			if (result != null) {
//				
//				Intent intent = null;
//				for (AuthPreference pref : result) {
//					if (!ClientManager.isScheduler(pref.service)) {
//						if (!pref.accounts.isEmpty()) {
//							intent = new Intent(mContext, OnlineListActivity.class);
//							break;
//						}
//					}			
//				}
//				if (intent == null) {
//					intent = new Intent(mContext, OnlineSetupActivity.class);
//					intent.putExtra(OnlineSetupActivity.EXTRA_BACK_MODE,
//							OnlineSetupActivity.BACK_MODE_ONLINE_LIST_BACKKEY_FINISH);
//				}
//				
//				NavigationGroup navigationGroup = (NavigationGroup)home.getLocalActivityManager().getActivity(ApplicationDefine.TAB_ONLINE);
//				navigationGroup.mHistory.clear();
//				
//				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//				View view = navigationGroup.getLocalActivityManager()
//						.startActivity(intent.getComponent().getShortClassName(), intent)
//						.getDecorView();
//				navigationGroup.replaceView(view);
//				
//			} else {
//				Toast.makeText(getContext(),
//						R.string.error_failed_to_connect, Toast.LENGTH_LONG).show();
//			}
//		}
//	}
    
	
//	/**
//	 * オートアルバムの非同期処理
//	 */
//	private class GetPrefsAutoTask extends AsyncTask<Void, Void, List<AuthPreference>> {
//		@Override
//		protected void onPreExecute() {
//			// TODO $ol プログレス表示
//			super.onPreExecute();
//		}
//		
//		@Override
//		protected List<AuthPreference> doInBackground(Void... params) {
//			try {
//				List<AuthPreference> prefs = mJsMedia.getAuthPreferences(false);
//				if (prefs == null) {
//					prefs = mJsMedia.getAuthPreferences(true);
//				}
//				return prefs;
//			} catch (IOException e) {
////				e.printStackTrace();		/*$debug$*/	// TODO $ol
//				return null;
//			}
//		}
//		
//		@Override
//		protected void onPostExecute(List<AuthPreference> result) {
//			
//			// ホーム画面取得
//			TabHomeActivity home = (TabHomeActivity)mContext;
//			
//			if (result != null) {
//				Intent intent = null;
//
//				for (AuthPreference pref : result) {
//					if (ClientManager.isScheduler(pref.service)) {
//						if (!pref.accounts.isEmpty()) {
//							intent = new Intent(mContext, AutoAlbumListActivity.class);
//						}
//					}
//				}
//
//				if (intent == null) {
//					intent = new Intent(mContext, OnlineSchedulerSetupActivity.class);
//					JsMediaAuth auth = new JsMediaAuth(mContext);
//					if (auth.loadCredential() == null) {
//						intent.putExtra(OnlineSchedulerSetupActivity.EXTRA_BACK_MODE,
//								OnlineSchedulerSetupActivity.BACK_MODE_FINISH);
//					}
//				}
//				
//				NavigationGroup navigationGroup = (NavigationGroup)home.getLocalActivityManager().getActivity(ApplicationDefine.TAB_AUTO);
//				navigationGroup.mHistory.clear();
//				
//				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//				View view = navigationGroup.getLocalActivityManager()
//						.startActivity(intent.getComponent().getShortClassName(), intent)
//						.getDecorView();
//				navigationGroup.replaceView(view);
//				
//			} else {
//				Toast.makeText(getContext(),
//						R.string.error_failed_to_connect, Toast.LENGTH_LONG).show();
//			}
//		}
//	}
	
}
