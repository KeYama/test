package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.database.OpenHelper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class JorllePrefsAutoActivity extends PreferenceActivity implements OnPreferenceChangeListener, CMediaIndex{
		
	/** プリファレンスキー : カレンダーを選択 */
	private static final String PREF_SELECT_CALENDAR = "select_calendar";
	/** プリファレンスキー : サービス追加ボタンを表示*/
	private static final String PREF_SHOW_ADD_SERVICE_BUTTON = "show_add_service_button";

	/** データベース */
	private SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();
	
	/**
	 * 初期化/生成
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_auto);
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

		// TODO プリファレンスマネージャから取得する箇所Utilに出す
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		// プリファレンスキー取得
		final String key = preference.getKey();

		// プリファレンスキーの比較
		// カレンダーを選択画面表示
		if (PREF_SELECT_CALENDAR.equals(key)) {
//			Intent selectIntent = new Intent(JorllePrefsAutoActivity.this, TutorialAutoalbumActivity.class);
			Intent selectIntent = OnlineSchedulerSetupActivity.createFullScreenIntent(this);
			startActivity(selectIntent);
			
		// サービス追加ボタンを表示設定
		} else if (PREF_SHOW_ADD_SERVICE_BUTTON.equals(key)) {
			// TODO サービス追加ボタンを表示する処理
			Toast.makeText(getApplicationContext(), "サービス追加ボタン押下", Toast.LENGTH_SHORT).show();
			
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
}
