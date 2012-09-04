package jp.co.johospace.jsphoto;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.managed.NavigationGroup;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.AuthPreference;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.JsMediaAuth;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.JsMediaUrl;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * オンラインスケジューラ設定画面アクティビティです
 */
public class OnlineSchedulerSetupActivity extends NavigatableActivity {
	
	private JsMediaAuth mAuth;
	private JsMediaServerClient mJsMedia;
	private static final int REQUEST_CONFIRM = 1;
	private static final int REQUEST_AUTH = 2;
	
	private static final int DIALOG_PROGRESS_REVOKE = 1;
	
	public static final String EXTRA_BACK_MODE =
			OnlineSchedulerSetupActivity.class.getSimpleName() + ".EXTRA_BACK_MODE";
	public static final int BACK_MODE_POPSTACK = 1;
	public static final int BACK_MODE_FINISH = 2;
	
	public static final String EXTRA_FORCE_REQUEST =
			OnlineSchedulerSetupActivity.class.getSimpleName() + ".EXTRA_FORCE_REQUEST";
	private List<AuthPreference> authPreferenceList;

	/**
	 * フルスクリーン起動用ののインテントを作成します
	 * @param context
	 * @return
	 */
	public static Intent createFullScreenIntent(Context context) {
		// 	スケジューラサービスの設定
		Intent intent = new Intent(context, OnlineSchedulerSetupActivity.class);
		intent.putExtra(EXTRA_BACK_MODE, BACK_MODE_FINISH);
		intent.putExtra(EXTRA_FORCE_REQUEST, true);
		
		return intent;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 初期処理
		init();

	}

	/**
	 * 初期処理
	 */
	private void init(){

		setContentView(R.layout.tutorial_autoalbum);

		mAuth  = new JsMediaAuth(this);
		mJsMedia = ClientManager.getJsMediaServerClient(this);

		if (authPreferenceList == null){
			new GetAuthPrefTask().execute(true);
		} else {
			setupList(authPreferenceList);
		}

	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
		// 初期処理
		init();
    }

	private class ServiceItem {
		AuthPreference pref;
		String name;
		int icon;
		
		ServiceItem(AuthPreference pref) {
			super();
			this.name =
					ClientManager.getServiceName(getApplicationContext(), pref.service);
			this.icon =
					ClientManager.getIconResource(getApplicationContext(), pref.service);
			this.pref = pref;
		}
	}
	
	private void setupList(List<AuthPreference> prefs) {
//		System.out.println("++++++++++++++++ refresh service list");		/*$debug$*/
		LinearLayout container =
				(LinearLayout) findViewById(R.id.laySyncapk);
		container.removeAllViews();
		
		Collections.sort(prefs, new Comparator<AuthPreference>() {
			@Override
			public int compare(AuthPreference p1, AuthPreference p2) {
				Integer o1 = ClientManager.getDisplayOrder(p1.service);
				Integer o2 = ClientManager.getDisplayOrder(p2.service);
				return o1.compareTo(o2);
			}
		});
		
		for (AuthPreference pref : prefs) {
			if (ClientManager.isScheduler(pref.service)) {
				ServiceItem item = new ServiceItem(pref);
				if (!pref.accounts.isEmpty()) {
					if (pref.expired != null && pref.expired) {
						View view = getLayoutInflater().inflate(
								R.layout.tutorial_syncapk_row, container, false);

						((ImageView)view.findViewById(R.id.ivApk)).setImageResource(item.icon);
						((TextView)view.findViewById(R.id.tvApkName)).setText(getString(R.string.online_message_reauth, item.name));
						((ImageView)view.findViewById(R.id.ivAddRevoke)).setImageResource(R.drawable.button_plus);
						
						view.findViewById(R.id.viewOver).setTag(item);
						view.findViewById(R.id.viewOver).setOnClickListener(mAuthListener);

						container.addView(view);
					} else {
						// 解除する行
						View view = getLayoutInflater().inflate(
								R.layout.tutorial_syncapk_row, container, false);

						((ImageView)view.findViewById(R.id.ivApk)).setImageResource(item.icon);
						((TextView)view.findViewById(R.id.tvApkName)).setText(getString(R.string.online_message_revoke, item.name));
						((ImageView)view.findViewById(R.id.ivAddRevoke)).setImageResource(R.drawable.button_minus);
						
						view.findViewById(R.id.viewOver).setTag(item);
						view.findViewById(R.id.viewOver).setOnClickListener(mRevokeListener);

						container.addView(view);
					}
				} else {
					View view = getLayoutInflater().inflate(
							R.layout.tutorial_syncapk_row, container, false);

					((ImageView)view.findViewById(R.id.ivApk)).setImageResource(item.icon);
					((TextView)view.findViewById(R.id.tvApkName)).setText(item.name);
					((ImageView)view.findViewById(R.id.ivAddRevoke)).setImageResource(R.drawable.button_plus);
					
					view.findViewById(R.id.viewOver).setTag(item);
					view.findViewById(R.id.viewOver).setOnClickListener(mAuthListener);

					container.addView(view);
				}
			}
		}
	}

	private class GetAuthPrefTask extends AsyncTask<Boolean, Void, List<AuthPreference>> {
		@Override
		protected void onPreExecute() {
			startProgress();
		}

		@Override
		protected List<AuthPreference> doInBackground(Boolean... params) {
			try {
				return mJsMedia.getAuthPreferences(params[0]);
			} catch (IOException e) {
				handleException(e, true);
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<AuthPreference> result) {
			authPreferenceList = result;
			if (authPreferenceList != null) {
				setupList(authPreferenceList);
			} else {
				// TODO &ol
			}
			
			stopProgress();
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONFIRM: {
			if (resultCode == RESULT_OK) {
				Intent intent = new Intent(OnlineSchedulerSetupActivity.this, AuthExternalActivity.class);
				intent.putExtra(AuthExternalActivity.EXTRA_SERVICE,
						data.getStringExtra(SyncConfirmActivity.EXTRA_SERVICE));
				intent.putExtra(AuthExternalActivity.EXTRA_URL,
						data.getStringExtra(SyncConfirmActivity.EXTRA_AUTH_URL));
				startActivityForResult(intent, REQUEST_AUTH);
			}
		}
		break;
		
		case REQUEST_AUTH: {
			if (resultCode == RESULT_OK) {
				// 認証情報を保存
				String devid = data.getStringExtra(AuthExternalActivity.EXTRA_DEVID);
				String token = data.getStringExtra(AuthExternalActivity.EXTRA_TOKEN);
				mAuth.saveCredential(devid, token);

				NavigationGroup group = (NavigationGroup) getParent();
				group.mHistory.clear();
				
				Intent intent = new Intent(this, AutoAlbumListActivity.class);
				View view = group.getLocalActivityManager()
						.startActivity(intent.getComponent().getShortClassName(), intent)
						.getDecorView();
				group.replaceView(view);
			} else {
				Toast.makeText(this, R.string.error_failed_to_authorize, Toast.LENGTH_LONG).show();
			}
		}
		break;
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Context context = this.getParent();
		if (context == null) {
			context = this;
		}
		
		switch (id) {
		case DIALOG_PROGRESS_REVOKE:
			ProgressDialog dialog = new ProgressDialog(context);
			dialog.setMessage(getString(R.string.message_revoke_service));
			dialog.setCancelable(false);
			dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					removeDialog(DIALOG_PROGRESS_REVOKE);
				}
			});
			return dialog;
		default:
			return super.onCreateDialog(id);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			switch (getIntent().getIntExtra(EXTRA_BACK_MODE, BACK_MODE_POPSTACK)) {
			case BACK_MODE_POPSTACK: {
				back();
			}
			break;
			
			case BACK_MODE_FINISH: {
				finish();
			}
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private View.OnClickListener mAuthListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.viewOver){
				ServiceItem item = (ServiceItem) v.getTag();
				
				if (SyncConfirmActivity.needConfirm(OnlineSchedulerSetupActivity.this)) {
					Intent intent = new Intent(OnlineSchedulerSetupActivity.this, SyncConfirmActivity.class);
					intent.putExtra(SyncConfirmActivity.EXTRA_SERVICE, item.pref.service);
					intent.putExtra(SyncConfirmActivity.EXTRA_AUTH_URL, item.pref.authUrl);
					startActivityForResult(intent, REQUEST_CONFIRM);
				} else {
					Intent intent = new Intent(OnlineSchedulerSetupActivity.this, AuthExternalActivity.class);
					intent.putExtra(AuthExternalActivity.EXTRA_SERVICE, item.pref.service);
					intent.putExtra(AuthExternalActivity.EXTRA_URL, item.pref.authUrl);
					startActivityForResult(intent, REQUEST_AUTH);
				}
			}
		}
	};
	
	private View.OnClickListener mRevokeListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Context context = OnlineSchedulerSetupActivity.this.getParent();
			if (context == null) {
				context = OnlineSchedulerSetupActivity.this;
			}

			final ServiceItem item = (ServiceItem) v.getTag();
			new AlertDialog.Builder(context)
				.setTitle(R.string.title_confirm_revoke_service)
				.setCancelable(true)
				.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
					}
				})
				.setNegativeButton(R.string.button_cancel, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setPositiveButton(R.string.button_ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						
						new AsyncTask<Void, Void, Exception>() {
							protected void onPreExecute() {
								showDialog(DIALOG_PROGRESS_REVOKE);
							}
							
							@Override
							protected Exception doInBackground(Void... params) {
								try {
									mJsMedia.executeGet(new JsMediaUrl(item.pref.clearUrl));
								} catch (IOException e) {
									handleException(e, true);
									return e;
								}
								return null;
							}
							
							@Override
							protected void onPostExecute(Exception result) {
								if (result == null) {
									MediaSyncManagerV2.deleteSyncSetting(
											OnlineSchedulerSetupActivity.this,
											item.pref.service, item.pref.accounts.iterator().next());
									new GetAuthPrefTask().execute(true);
								} else {
									Toast.makeText(OnlineSchedulerSetupActivity.this, R.string.message_error_revoke_service, Toast.LENGTH_SHORT).show();
								}
								
								dismissDialog(DIALOG_PROGRESS_REVOKE);
							}
						}.execute();
					}
				})
				.create()
				.show();
		}
	};

	private void back() {
		switch (getIntent().getIntExtra(EXTRA_BACK_MODE, BACK_MODE_POPSTACK)) {
		case BACK_MODE_POPSTACK: {
			NavigationGroup group = (NavigationGroup) getParent();
			group.mHistory.clear();
			Intent intent = new Intent(this, AutoAlbumListActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			View view = group.getLocalActivityManager()
					.startActivity(intent.getComponent().getShortClassName(), intent)
					.getDecorView();
			group.replaceView(view);
		}
		break;
		case BACK_MODE_FINISH: {
			finish();
		}
		break;
		}
	}

	private void startProgress() {
		ScrollView scroll = (ScrollView) findViewById(R.id.sclView);
		LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(scroll.getLayoutParams());
		scrollParams.weight = 0;
		scrollParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
		scroll.setLayoutParams(scrollParams);
		
		findViewById(R.id.laySyncapk).setVisibility(View.GONE);
		findViewById(R.id.rlyProgBase).setVisibility(View.VISIBLE);
		
	}

	private void stopProgress() {
		findViewById(R.id.rlyProgBase).setVisibility(View.GONE);
		findViewById(R.id.laySyncapk).setVisibility(View.VISIBLE);

		ScrollView scroll = (ScrollView) findViewById(R.id.sclView);
		LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(scroll.getLayoutParams());
		scrollParams.weight = 1;
		scrollParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
		scroll.setLayoutParams(scrollParams);
		
	}
}
