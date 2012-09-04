package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.onlineservice.Authorizer;
import jp.co.johospace.jsphoto.onlineservice.JsCloudiaWebAuthenticator;
import jp.co.johospace.jsphoto.onlineservice.OnlineMediaServiceClient;
import jp.co.johospace.jsphoto.onlineservice.OnlineMediaServiceClient.InteractionCallback;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.PicasaClient;
import jp.co.johospace.jsphoto.preference.GoogleAccountListPreference;
import jp.co.johospace.jsphoto.service.MediaSyncManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

/**
 * Picasa設定
 */
public class PicasaPrefsActivity extends PreferenceActivity
		implements OnPreferenceChangeListener, OnlineMediaServiceClient.AuthorizationHandler, Authorizer.AuthorizationHandler {
	
	public static final String SYNC_PICASA = "syncPicasa";
	public static final String PICASA_SYNC_REMOTE = "picasaSyncRemote";
	public static final String SYNC_PICASA_INTERVAL = "syncPicasaInterval";
	public static final String SYNC_PICASA_NOW = "syncPicasaNow";
	public static final String PICASA_ACCOUNT = "picasaAccount";
	public static final String PREF_PICASA_SYNC_LOCAL = "picasaSyncLocal";
	public static final String PREF_PICASA_SYNC = SYNC_PICASA;

	private static final int REQUEST_AUTHZ = 1;
	private static final int REQUEST_SETTING = 2;
	private static final int REQUEST_AUTHN = 3;
	
	private static final int DIALOG_PROGRESS_AUTH = 1;
	private static final int DIALOG_AUTH_FAILED = 2;
	
	private GoogleAccountListPreference mPrefAccount;
	private CheckBoxPreference mPrefSync;
	private Preference mPrefSyncNow;
	private ListPreference mPrefInterval;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_picasa);
		
		mPrefAccount = (GoogleAccountListPreference) findPreference(PICASA_ACCOUNT);
		mPrefAccount.setOnPreferenceChangeListener(this);
		mPrefSync = (CheckBoxPreference) findPreference(PREF_PICASA_SYNC);
		mPrefSync.setOnPreferenceChangeListener(this);
		mPrefSyncNow = findPreference(SYNC_PICASA_NOW);
		mPrefInterval = (ListPreference) findPreference(SYNC_PICASA_INTERVAL);
		mPrefInterval.setOnPreferenceChangeListener(this);
		
		showAccount();
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mPrefSyncNow) {
			MediaSyncManager.startSyncMedia(this, null);
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == mPrefAccount) {
			if (newValue != null && !TextUtils.isEmpty(newValue.toString())) {
				showDialog(DIALOG_PROGRESS_AUTH);
				PicasaClient client = new PicasaClient(this, newValue.toString());
				client.authorize(this, true);
				return false;
			} else {
				MediaSyncManager.cancelRepeatingSyncMedia(this);
				showAccount(null);
			}
		} else if (preference == mPrefSync) {
			if ((Boolean) newValue) {
				if (TextUtils.isEmpty(JsCloudiaWebAuthenticator.getToken(this))) {
					showDialog(DIALOG_PROGRESS_AUTH);
					new JsCloudiaWebAuthenticator(this, getCallbackURL()).authorize(this);
					return false;
				} else {
					startSetting();
					return false;
				}
			} else {
				MediaSyncManager.cancelRepeatingSyncMedia(this);
			}
		} else if (preference == mPrefInterval) {
			reschedule(Long.valueOf(newValue.toString()));
		}
		
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_AUTHZ:
			if (mAuthZCallback != null) {
				showDialog(DIALOG_PROGRESS_AUTH);
				mAuthZCallback.onInteractionResult(resultCode, data);
				mAuthZCallback = null;
			}
			break;
			
		case REQUEST_SETTING:
			if (resultCode == RESULT_OK) {
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				pref.edit()
					.putString(PREF_PICASA_SYNC_LOCAL, data.getStringExtra(PicasaSyncTargetActivity.EXTRA_LOCAL_FOLDER))
					.putString(PICASA_SYNC_REMOTE, data.getStringExtra(PicasaSyncTargetActivity.EXTRA_PICASA_ALBUM))
					.commit();
				mPrefSync.setChecked(true);
				reschedule(Long.valueOf(mPrefInterval.getValue()));
			} else {
				// ここで通知は不要
//				// 通知
//				NotificationManager manager =
//						(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//				Intent intent = new Intent(Intent.ACTION_VIEW);
//			    PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
//				Notification notification = new Notification();
//				notification.icon = R.drawable.ic_launcher;
//				notification.tickerText = getString(R.string.notification_picasa_sync);
//				notification.when = System.currentTimeMillis(); notification.setLatestEventInfo(
//					 getApplicationContext()
//					 , getString(R.string.notification_picasa_sync)
//					 , getString(R.string.notification_picasa_sync_no_connected)
//					 , pendingIntent);
//				manager.notify(1, notification);
			}
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (mAuthNCallback != null) {
			mAuthNCallback.onInteractionResult(RESULT_OK, intent);
			mAuthNCallback = null;
		}
	}
	
	private InteractionCallback mAuthZCallback;
	
	@Override
	public void startInteraction(Intent intent, InteractionCallback callback) {
		removeDialog(DIALOG_PROGRESS_AUTH);
		mAuthZCallback = callback;
		startActivityForResult(intent, REQUEST_AUTHZ);
	}

	@Override
	public void authorizationFinished(String account, boolean authorized) {
		removeDialog(DIALOG_PROGRESS_AUTH);
		if (authorized) {
			mPrefAccount.setOnPreferenceChangeListener(null);
			mPrefAccount.setValue(account);
			mPrefAccount.setOnPreferenceChangeListener(this);
			if (mPrefSync.isChecked()) {
				reschedule();
			}
		} else {
			mPrefAccount.setOnPreferenceChangeListener(null);
			mPrefAccount.setValue(null);
			mPrefAccount.setOnPreferenceChangeListener(this);
			showDialog(DIALOG_AUTH_FAILED);
		}
		showAccount();
	}
	
	private void reschedule() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		reschedule(Long.parseLong(pref.getString(SYNC_PICASA_INTERVAL, "-1")));
	}
	
	private void reschedule(long interval) {
		if (0 < interval) {
			MediaSyncManager.scheduleRepeatingSyncMedia(this, interval, null, false);
		} else {
			MediaSyncManager.cancelRepeatingSyncMedia(this);
		}
	}

	private Authorizer.InteractionCallback mAuthNCallback;
	@Override
	public void startInteraction(
			Intent intent,
			Authorizer.InteractionCallback callback) {
		removeDialog(DIALOG_PROGRESS_AUTH);
		mAuthNCallback = callback;
		startActivityForResult(intent, REQUEST_AUTHN);
	}

	@Override
	public void authorizationFinished(boolean authorized) {
		removeDialog(DIALOG_PROGRESS_AUTH);
		if (authorized) {
			startSetting();
		} else {
			showDialog(DIALOG_AUTH_FAILED);
		}
	}

	private void startSetting() {
		Intent intent = new Intent(this, PicasaSyncTargetActivity.class);
		intent.putExtra(PicasaSyncTargetActivity.EXTRA_ACCOUNT, mPrefAccount.getValue());
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		intent.putExtra(PicasaSyncTargetActivity.EXTRA_LOCAL_FOLDER,
				pref.getString(PicasaPrefsActivity.PREF_PICASA_SYNC_LOCAL,
						Environment.getExternalStoragePublicDirectory(
								Environment.DIRECTORY_DCIM).getAbsolutePath()));
		intent.putExtra(PicasaSyncTargetActivity.EXTRA_PICASA_ALBUM, pref.getString(PICASA_SYNC_REMOTE, null));
		startActivityForResult(intent, REQUEST_SETTING);
	}
	
	private String getCallbackURL() {
		return String.format("%s://%s%s",
				getString(R.string.jscloudia_authcallback_scheme),
				getString(R.string.jscloudia_authcallback_host),
				getString(R.string.jscloudia_authcallback_path));
	}
	
	public static boolean isSyncable(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return !TextUtils.isEmpty(getPicasaAccount(context))
				&& pref.getBoolean(PicasaPrefsActivity.PREF_PICASA_SYNC, false);
	}
	
	public static String getPicasaAccount(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(PICASA_ACCOUNT, null);
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_PROGRESS_AUTH:
			ProgressDialog d = new ProgressDialog(this);
			d.setCancelable(false);
			d.setTitle(getString(R.string.pref_picasa_authorizing));
			return d;
			
		case DIALOG_AUTH_FAILED:
			return new AlertDialog.Builder(this)
				.setCancelable(false)
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setMessage(R.string.pref_picasa_failed_to_auth)
				.create();
		}
		
		return null;
	}
	
	private void showAccount() {
		String account = getPicasaAccount(this);
		showAccount(account);
	}
	
	private void showAccount(String account) {
		if (TextUtils.isEmpty(account)) {
			mPrefAccount.setSummary(getString(R.string.pref_picasa_account_not_specified));
		} else {
			mPrefAccount.setSummary(account);
		}
	}
	
	public static boolean isAutoSyncEnabled(Context context) {
		String account = getPicasaAccount(context);
		if (TextUtils.isEmpty(account)) {
			return false;
		} else {
			SharedPreferences pref =
					PreferenceManager.getDefaultSharedPreferences(context);
			if (!pref.getBoolean(SYNC_PICASA, false)) {
				return false;
			} else {
				String interval = pref.getString(SYNC_PICASA_INTERVAL, "-1");
				if (0 < Long.parseLong(interval)) {
					return true;
				} else {
					return false;
				}
			}
			
		}
	}
}
