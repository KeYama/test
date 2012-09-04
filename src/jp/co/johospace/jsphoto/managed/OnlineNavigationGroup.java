package jp.co.johospace.jsphoto.managed;

import java.io.IOException;
import java.util.List;

import jp.co.johospace.jsphoto.OnlineListActivity;
import jp.co.johospace.jsphoto.OnlineSetupActivity;
import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.AuthPreference;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * オンラインタブグループ
 */
public class OnlineNavigationGroup extends NavigationGroup {

	private JsMediaServerClient mJsMedia;
	private ProgressBar mProgress;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.online_navigation_group);
		mProgress = (ProgressBar) findViewById(R.id.progress);
		
		Intent intent = getIntent();
		if ("TAB".equals(intent.getStringExtra(ApplicationDefine.EXTRA_LAUNCHER))) {
			
			mJsMedia = ClientManager.getJsMediaServerClient(this);
			
//			Intent childIntent = new Intent(getApplicationContext(), OnlineFolderActivity.class);
////			Intent childIntent = new Intent(getApplicationContext(), OnlineSetupActivity.class);
//			childIntent.putExtra(OnlineFolderActivity.EXTRA_SERVICE_TYPE, ServiceType.TWITTER);
//			childIntent.putExtra(OnlineFolderActivity.EXTRA_SERVICE_ACCOUNT, "545617375");
////			childIntent.putExtra(OnlineFolderActivity.EXTRA_SERVICE_DIRID, ServiceType.TWITTER);
//			childIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			View view = getLocalActivityManager()
//					.startActivity(childIntent.getComponent().getClassName(), childIntent)
//					.getDecorView();
//			replaceView(view);
			
			startLoading();
		}
	}

	public void startLoading() {
		new GetPrefsTask().execute();
	}
	
	private class GetPrefsTask extends AsyncTask<Void, Void, List<AuthPreference>> {
		@Override
		protected void onPreExecute() {
			mProgress.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected List<AuthPreference> doInBackground(Void... params) {
			try {
				List<AuthPreference> prefs = mJsMedia.getAuthPreferences(false);
				if (prefs == null) {
					prefs = mJsMedia.getAuthPreferences(true);
				}
				return prefs;
			} catch (IOException e) {
//				e.printStackTrace();		/*$debug$*/	// TODO $ol
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<AuthPreference> result) {
			mProgress.setVisibility(View.GONE);
			if (result != null) {
				Intent intent = null;
				for (AuthPreference pref : result) {
					if (!ClientManager.isScheduler(pref.service)) {
						if (!pref.accounts.isEmpty()) {
							intent = new Intent(OnlineNavigationGroup.this, OnlineListActivity.class);
							break;
						}
					}			
				}
				if (intent == null) {
					intent = new Intent(OnlineNavigationGroup.this, OnlineSetupActivity.class);
					intent.putExtra(OnlineSetupActivity.EXTRA_BACK_MODE,
							OnlineSetupActivity.BACK_MODE_ONLINE_LIST_BACKKEY_FINISH);
				}
				
				mHistory.clear();
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				View view = getLocalActivityManager()
						.startActivity(intent.getComponent().getShortClassName(), intent)
						.getDecorView();
				replaceView(view);
				
			} else {
				Toast.makeText(OnlineNavigationGroup.this,
						R.string.error_failed_to_connect, Toast.LENGTH_LONG).show();
			}
		}
	}
}
