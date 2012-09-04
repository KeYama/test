package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class JorllePrefsCommonActivity extends PreferenceActivity implements OnPreferenceChangeListener, CMediaIndex{
	
	/** プリファレンスキー : メタ情報を送信 */
	private static final String PREF_SEND_METADATA = "send_metadata";
	private CheckBoxPreference mChkSendMetadata;

	/**
	 * 初期化/生成
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_common);
		
		mChkSendMetadata = (CheckBoxPreference) findPreference(PREF_SEND_METADATA);
		mChkSendMetadata.setChecked(MediaSyncManagerV2.isLocalSyncAllowed(this));
	}
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return false;
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
		// 非表示ファイルを表示
		if (ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY.equals(key)) {
			final boolean checked = ((CheckBoxPreference) preference).isChecked();
			PreferenceUtil.setBooleanPreferenceValue(this, key, checked);
			
		// メタ情報を送信押下
		} else if (PREF_SEND_METADATA.equals(key)) {
			if (mChkSendMetadata.isChecked()) {
				MediaSyncManagerV2.saveLocalSyncAllowed(this, true);
				MediaSyncManagerV2.startSend(this, null);
			} else {
				showDialog(DIALOG_CONFIRM_METADATA_OFF);
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	
	private static final int DIALOG_CONFIRM_METADATA_OFF = 1;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_CONFIRM_METADATA_OFF: {
			return new AlertDialog.Builder(this)
				.setMessage(R.string.pref_message_metadata_off)
				.setCancelable(false)
				.setPositiveButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mChkSendMetadata.setChecked(true);
						dialog.dismiss();
					}
				})
				.setNegativeButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mChkSendMetadata.setChecked(false);
						MediaSyncManagerV2.saveLocalSyncAllowed(JorllePrefsCommonActivity.this, false);
						MediaSyncManagerV2.cancelRepeatingSend(JorllePrefsCommonActivity.this);
					}
				})

				.create();
		}
		}
		return super.onCreateDialog(id);
	}
}
