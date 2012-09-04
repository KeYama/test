package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.util.AppUtil;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.util.Linkify;
import android.widget.ScrollView;
import android.widget.TextView;

public class JorllePrefsHelpActivity extends PreferenceActivity implements OnPreferenceChangeListener, CMediaIndex{

	/** ダイアログ */
	private static final int
		DIALOG_HELP = 1,
		DIALOG_TERMS_OF_SERVICE = 2,
		DIALOG_VERSION_INFO = 3;
	
	/** プリファレンスキー : 利用規約表示 */
	private static final String PREF_SHOW_TERMS_OF_SERVICE = "terms_of_service";
	/** プリファレンスキー : チュートリアル表示 */
	private static final String PREF_SHOW_TUTORIAL = "show_tutorial";
	/** プリファレンスキー : バージョン情報表示 */
	private static final String PREF_SHOW_VERSION_INFO = "show_version_info";

	/**
	 * 初期化/生成
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_help);
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
		// チュートリアル押下
		if (PREF_SHOW_TUTORIAL.equals(key)) {
			Intent intent = new Intent(JorllePrefsHelpActivity.this, IntroActivity.class);
			startActivity(intent);
			
		// 利用規約押下
		} else if (PREF_SHOW_TERMS_OF_SERVICE.equals(key)) {
			// テキストをセット
			TextView ca = new TextView(this);
			ca.setText(AppUtil.getTextFromAssetFile(getAssets(), "terms/"
					+ AppUtil.getAboutJorlleTextFileName(this)));
			ca.setTextColor(Color.rgb(190, 190, 190));
			
			Linkify.addLinks(ca, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);

			ScrollView sv = new ScrollView(this);
			sv.addView(ca);

			new AlertDialog.Builder(this)
					.setTitle(R.string.pref_terms_of_service).setView(sv)
					.setPositiveButton(android.R.string.ok, null).create()
					.show();
		// バージョン情報押下	
		} else if (PREF_SHOW_VERSION_INFO.equals(key)) {
			showDialog(DIALOG_VERSION_INFO);
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	/**
	 * ダイアログ生成
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_HELP: {
				// ヘルプ表示
				return new AlertDialog.Builder(this)
					.setCancelable(true)
					.setTitle(getString(R.string.pref_help))
					.setMessage("Help message...")
					.create();
			}
			case DIALOG_TERMS_OF_SERVICE: {
				// 利用規約表示
				// テキストをセット
				TextView ca = new TextView(this);
				ca.setText(AppUtil.getTextFromAssetFile(getAssets(), "about/"
						+ AppUtil.getAboutJorlleTextFileName(this)));

				Linkify.addLinks(ca, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);

				ScrollView sv = new ScrollView(this);
				sv.addView(ca);

				return new AlertDialog.Builder(this)
						.setTitle(R.string.pref_terms_of_service).setView(sv)
						.setPositiveButton(android.R.string.ok, null).create();
			}
			case DIALOG_VERSION_INFO: {
				// バージョン情報表示
				String versionName = null;
				try {
					versionName = getPackageManager().getPackageInfo(
							getPackageName(), 1).versionName;
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
//					e.printStackTrace();		/*$debug$*/
				}
				return new AlertDialog.Builder(this).setTitle(R.string.pref_build_version)
						.setMessage("Version: " + versionName)
						.setPositiveButton(android.R.string.ok, null).create();
			}
		}
		return super.onCreateDialog(id);
	}
}
