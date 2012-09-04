package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.AuthPreference;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.util.Pair;

public class JorllePrefsOnlineActivity extends AbstractPreferenceActivity implements OnPreferenceChangeListener, CMediaIndex{
		
	/** プリファレンスキー : サービスを追加/削除 */
	private static final String PREF_SERVICE_ADD_DELETE = "service_add_delete";
	/** プリファレンスキー : サービス追加ボタンを表示*/
	private static final String PREF_SHOW_ADD_SERVICE_BUTTON = "show_service_add_button";
	/** プリファレンスキー : 写真を同期 */
	private static final String PREF_SYNC_PHOTO = "sync_photo";
	/** プリファレンスキー : Wi-Fi接続時にみ同期*/
	private static final String PREF_WIFI_ONLY_SYNC = "wifi_only_sync";
	/** プリファレンスキー : いますぐ同期*/
	private static final String PREF_SYNC_NOW = "sync_now";
	/** プリファレンスキー : 同期間隔*/
	private static final String PREF_SYNC_INTERVAL = "sync_interval";

	/** データベース */
	private SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();
	
	/** チェックボックス : サービス追加ボタンを表示 */
	private CheckBoxPreference mChkAddService;
	/** チェックボックス : 写真を同期 */
	private CheckBoxPreference mChkSyncPhoto;
	/** チェックボックス : Wi-Fi接続時のみ同期 */
	private CheckBoxPreference mChkWiFiSync;
	/** チェックボックス : 充電中のみ同期 */
	private CheckBoxPreference mChkChargingSync;
	/** リスト ： 同期間隔 */
	private ListPreference mLstInterval;
	/** スクリーン ： 今すぐ同期 */
	private PreferenceScreen mSyncNow;
	
	private SyncSetting mPreviouseState;
	
	/**
	 * 初期化/生成
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_online);
		
		mChkAddService = (CheckBoxPreference) findPreference(PREF_SHOW_ADD_SERVICE_BUTTON);
		mChkSyncPhoto = (CheckBoxPreference) findPreference(PREF_SYNC_PHOTO);
		mChkWiFiSync = (CheckBoxPreference) findPreference(PREF_WIFI_ONLY_SYNC);
		mLstInterval = (ListPreference) findPreference(PREF_SYNC_INTERVAL);
		mSyncNow = (PreferenceScreen) findPreference(PREF_SYNC_NOW);
		
		mLstInterval.setOnPreferenceChangeListener(this);
		
		SyncSetting setting = loadSyncSetting();
		mPreviouseState = setting;
		showSyncSetting(setting);
	}
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == mLstInterval) {
			mLstInterval.setValue(newValue.toString());
			SyncSetting setting = saveSyncSetting();
			if (setting.interval < 0) {
				MediaSyncManagerV2.cancelRepeatingSyncMedia(this);
			} else {
				MediaSyncManagerV2.scheduleRepeatingSyncMedia(this, setting.interval, null, false);
			}
			showSyncSetting(setting);
		}
		return true;
	}

	/**
	 * プリファレンス項目アクションイベント
	 */
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {

		// プリファレンスキー取得
		final String key = preference.getKey();

		// プリファレンスキーの比較
		// サービスを追加/削除
		if (PREF_SERVICE_ADD_DELETE.equals(key)) {
			Intent intent = OnlineSetupActivity.createFullScreenIntent(this);
			startActivity(intent);
			
		// 写真を同期
		} else if (PREF_SYNC_PHOTO.equals(key)) {
			if (mChkSyncPhoto.isChecked()) {
				Pair<String, String> account = getSyncAccount();
				if (account == null) {
					mChkWiFiSync.setChecked(false);
					SyncSetting setting = saveSyncSetting();
					showSyncSetting(setting);
					showDialog(DIALOG_ABSENCE_OF_ACCOUNT);
				} else {
					SyncSetting setting = saveSyncSetting();
					MediaSyncManagerV2.scheduleRepeatingSyncMedia(this, setting.interval, null, false);
					showSyncSetting(setting);
				}
				
			} else {
				SyncSetting setting = saveSyncSetting();
				MediaSyncManagerV2.cancelRepeatingSyncMedia(this);
				showSyncSetting(setting);
			}
		
		// いますぐ同期
		} else if (PREF_SYNC_NOW.equals(key)) {
			MediaSyncManagerV2.startSyncMedia(this, null);
			
		// Wi-Fi接続時にのみ同期
		} else if (PREF_WIFI_ONLY_SYNC.equals(key)) {
			SyncSetting setting = loadSyncSetting();
			setting.onlyOnWifi = mChkWiFiSync.isChecked();
			MediaSyncManagerV2.saveSyncSetting(this, setting);
			showSyncSetting(setting);
			
		}
		
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	private SyncSetting loadSyncSetting() {
		Map<String, Map<String, SyncSetting>> prefs = MediaSyncManagerV2.loadSyncSettings(this);
		for (String service : prefs.keySet()) {
			Map<String, SyncSetting> accounts = prefs.get(service);
			for (String account : accounts.keySet()) {
				return accounts.get(account);
			}
		}
		return null;
	}
	
	private Pair<String, String> getSyncAccount() {
		JsMediaServerClient client = ClientManager.getJsMediaServerClient(this);
		List<AuthPreference> prefs;
		try {
			prefs = client.getAuthPreferences(false);
		} catch (IOException e) {
			handleException(e, true);
			return null;
		}
		
		if (prefs == null) {
			return null;
		} else {
			for (AuthPreference pref : prefs) {
				if (ClientManager.isBidirectional(pref.service)) {
					if (!pref.accounts.isEmpty()) {
						return new Pair<String, String>(
								pref.service, pref.accounts.iterator().next());
					}
				}
			}
			return null;
		}
	}
	
	private SyncSetting saveSyncSetting() {
		if (mChkSyncPhoto.isChecked()) {
			SyncSetting setting = loadSyncSetting();
			if (setting == null) {
				setting = new SyncSetting();
				Pair<String, String> account = getSyncAccount();
				if (account == null) {
					return null;
				}
				setting.service = account.first;
				setting.account = account.second;
				File syncDir = getSyncLocalPath();
				setting.localDir = syncDir.getAbsolutePath();
				syncDir.mkdirs();
			}
			
			setting.onlyOnWifi = mChkWiFiSync.isChecked();
			setting.interval = Long.valueOf(mLstInterval.getValue());
			
			MediaSyncManagerV2.saveSyncSetting(this, setting);
			
			return setting;
		} else {
			if (mPreviouseState != null) {
				MediaSyncManagerV2.deleteSyncSetting(this,
						mPreviouseState.service, mPreviouseState.account);
			}
			return null;
		}
	}
	
	private void showSyncSetting(SyncSetting setting) {
		if (setting == null) {
			
			mChkSyncPhoto.setChecked(false);
			mChkWiFiSync.setChecked(false);
			mChkWiFiSync.setEnabled(false);
			mLstInterval.setValue("21600000");
		} else {
			mChkSyncPhoto.setChecked(true);
			mChkWiFiSync.setChecked(setting.onlyOnWifi != null && setting.onlyOnWifi);
			mChkWiFiSync.setEnabled(setting.interval == null || 0 <= setting.interval);
			mLstInterval.setValue(setting.interval != null ? setting.interval.toString() : "21600000");
		}
	}
	
	private File getSyncLocalPath() {
		File path = new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
				TabHomeActivity.CAMERA_PATH);
		path.mkdirs();
		return path;
	}
	
	
	
	private static final int DIALOG_ABSENCE_OF_ACCOUNT = 1;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ABSENCE_OF_ACCOUNT: {
			return new AlertDialog.Builder(this)
				.setTitle(R.string.pref_sync_photo)
				.setMessage(R.string.pref_message_on_absence_of_sync_services)
				.setCancelable(false)
				.setPositiveButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setNegativeButton(R.string.pref_sync_button_setting, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = OnlineSetupActivity.createFullScreenIntent(JorllePrefsOnlineActivity.this);
						startActivity(intent);
					}
				})

				.create();
		}
		}
		return super.onCreateDialog(id);
	}
}
