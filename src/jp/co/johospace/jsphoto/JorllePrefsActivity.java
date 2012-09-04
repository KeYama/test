package jp.co.johospace.jsphoto;

import java.io.IOException;

import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.PasswordSettingDialog;
import jp.co.johospace.jsphoto.dialog.SecretCheckPasswordDialog;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncServiceV2;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.AutoAlbumCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class JorllePrefsActivity extends AbstractPreferenceActivity implements OnPreferenceChangeListener, CMediaIndex{

	/** ダイアログ */
	private static final int
		DIALOG_CLEAR_CACHE = 1;
	
	public static final String EXTAR_CACHE_CLEARED =
			JorllePrefsActivity.class.getName().concat(".EXTAR_CACHE_CLEARED");
		
	/** プリファレンスキー : シークレット画像表示 */
//	private static final String PREF_SHOW_SEACRET = "secret_folder_display";　// ApplicationDefineに統合
	/** プリファレンスキー : パスワードを設定 */
	private static final String PREF_PASSWORD_SETTING = "password_setting";
	/** プリファレンスキー : キャッシュを削除*/
	private static final String PREF_CLEAR_CACHE = "clear_cache";
	/** プリファレンスキー : 全般 */
	private static final String PREF_COMON = "show_common";
	/** プリファレンスキー : オンライン*/
	private static final String PREF_ONLINE = "show_online";
	/** プリファレンスキー : オートアルバム */
	private static final String PREF_AUTO = "show_auto_album";
	/** プリファレンスキー : ヘルプ*/
	private static final String PREF_SHOW_HELP = "show_help";

	/** データベース */
	private SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();
	
	/**
	 * 初期化/生成
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_base);
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
		// シークレット画像を表示
		if (ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY.equals(key)) {
			// シークレット画像表示押下時
			String password = PreferenceUtil.getPreferenceValue(this, ApplicationDefine.KEY_SECRET_PASSWORD, null);
			
			final CheckBoxPreference check = ((CheckBoxPreference) preference);
			final boolean checked = check.isChecked();
			
			// パスワード未設定時は、パスワードの設定ダイアログ表示
			if (password == null) {
//				final SecretPasswordDialog passwordDialog = new SecretPasswordDialog(this);
//				passwordDialog.setOnDismissListener(new OnDismissListener() {
//					
//					@Override
//					public void onDismiss(DialogInterface dialog) {
//						if (passwordDialog.mIsSetPassword) {
//							PreferenceUtil.setBooleanPreferenceValue(JorllePrefsActivity.this, key, checked);
//						} else {
//							check.setChecked(false);
//						}
//					}
//				});
//				passwordDialog.show();
				PreferenceUtil.setBooleanPreferenceValue(JorllePrefsActivity.this, key, checked);
			// パスワード設定済み時
			} else {
				
				// シークレット表示時、パスワードの確認ダイアログ表示
				if (checked) {
					final SecretCheckPasswordDialog checkDialog = new SecretCheckPasswordDialog(this);
					checkDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						
						@Override
						public void onDismiss(DialogInterface dialog) {
							if (checkDialog.mIsSetPassword) {
								PreferenceUtil.setBooleanPreferenceValue(JorllePrefsActivity.this, key, checked);
							} else {
								check.setChecked(false);
							}
						}
					});
					checkDialog.show();
					
				} else {
					PreferenceUtil.setBooleanPreferenceValue(JorllePrefsActivity.this, key, checked);
				}
			}
		// パスワードを設定
		} else if (PREF_PASSWORD_SETTING.equals(key)) {
			// ダイアログ呼び出し
			PasswordSettingDialog passwordDialog = new PasswordSettingDialog(this);
			passwordDialog.show();
			
		// キャッシュを削除
		} else if (PREF_CLEAR_CACHE.equals(key)) {
			showDialog(DIALOG_CLEAR_CACHE);
		
		// 全般
		} else if (PREF_COMON.equals(key)) {
			Intent commonIntent = new Intent(JorllePrefsActivity.this, JorllePrefsCommonActivity.class);
			startActivity(commonIntent);
		
		// オンライン
		} else if (PREF_ONLINE.equals(key)) {
			Intent onlineIntent = new Intent(JorllePrefsActivity.this, JorllePrefsOnlineActivity.class);
			startActivity(onlineIntent);
			
		// オートアルバム
		} else if (PREF_AUTO.equals(key)) {
			Intent autoIntent = new Intent(JorllePrefsActivity.this, JorllePrefsAutoActivity.class);
			startActivity(autoIntent);
			
		// ヘルプを表示
		} else if (PREF_SHOW_HELP.equals(key)) {
			Intent helpIntent = new Intent(JorllePrefsActivity.this, JorllePrefsHelpActivity.class);
			startActivity(helpIntent);
			
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	/**
	 * ダイアログ生成
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		
		// キャッシュ削除ダイアログ表示
		case DIALOG_CLEAR_CACHE: {
			return new AlertDialog.Builder(this)
			.setCancelable(true)
			.setTitle(getString(R.string.pref_advanced_clear_cache))
			.setMessage(getString(R.string.pref_advanced_txt_clear_cache))
			.setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// キャッシュの削除処理
					DeleteDatabaseTask task = new DeleteDatabaseTask();
					task.execute(null,null,null);
					dialog.dismiss();
				}
			})

			.create();
		}

		}
		return super.onCreateDialog(id);
	}
	
	private boolean mCacheCleared;
	
	protected class DeleteDatabaseTask extends AsyncTask<String, Integer, Long>{
		private ProgressDialog mProgressDialog = null;

		@Override
		protected Long doInBackground(String... params) {
			mCacheCleared = true;
			
			// ローカル
			SQLiteDatabase db = OpenHelper.cache.getDatabase();
			db.beginTransaction();
			try {
				db.delete($TABLE, null, null);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			
			/* オンライン */ {
				ExternalServiceCache cache =
						new ExternalServiceCacheImpl(JorllePrefsActivity.this);
				cache.clearMediaContentCache(null, null);
				cache.clearLargeThumbnailCache(null, null);
				cache.clearThumbnailCache(null, null);
				
				cache.clearMediaTree(null, null);
				
				JsMediaServerClient client =
						ClientManager.getJsMediaServerClient(JorllePrefsActivity.this);
				try {
					client.requestRecreateIndex();
					if (MediaSyncManagerV2.isLocalSyncAllowed(JorllePrefsActivity.this)) {
						Bundle extra = new Bundle();
						extra.putBoolean(MediaSyncServiceV2.EXTRA_RESEND_ALL, true);
						MediaSyncManagerV2.startSend(JorllePrefsActivity.this, extra);
					}
				} catch (IOException e) {
					handleException(e, false);
				}
				
				try {
					CachingAccessor.clearCachedMemories(JorllePrefsActivity.this);
					new AutoAlbumCacheImpl(JorllePrefsActivity.this, client).clearCache();
					CachingAccessor.clearCashedSearchMedias(JorllePrefsActivity.this);
				} catch (Exception e) {
					handleException(e, false);
				}
			}
			
			return null;
		}
		@Override
		protected void onPreExecute() {
			mProgressDialog = new ProgressDialog(JorllePrefsActivity.this);
			mProgressDialog.setTitle(getString(R.string.pref_advanced_clear_cache));
			mProgressDialog.setMessage(getString(R.string.pref_advanced_cache_clearning));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Long result) {
			mProgressDialog.dismiss();
			super.onPostExecute(result);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Intent data = new Intent();
		data.putExtra(EXTAR_CACHE_CLEARED, mCacheCleared);
		setResult(RESULT_OK, data);
	}
	
	/**
	 * シークレット操作タスク
	 */
	protected class SecretReleaseTask extends AsyncTask<Void, Void, Boolean> {
		
		ProgressDialog mFolderProgress;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			// プログレスダイアログを設定　表示
			mFolderProgress = new ProgressDialog(JorllePrefsActivity.this);
			
			mFolderProgress.setMessage(getString(R.string.folder_message_progress_no_secret));
			mFolderProgress.setCancelable(false);
			mFolderProgress.show();
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			// シークレットを全解除
			// TODO 現在は、ひとまずSDカードを設定
			return MediaUtil.changeMediaSecret(mDatabase, Environment.getExternalStorageDirectory().getPath(), false, true);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			mFolderProgress.dismiss();
			
			String message;
			
			// 結果メッセージ作成
			if (result) {
				message = getString(R.string.folder_message_setting_no_secret_success);
			} else {
				message = getString(R.string.folder_message_setting_no_secret_failure);
			}
			
			mFolderProgress.dismiss();
			
			// 終了メッセージ表示
			Toast.makeText(JorllePrefsActivity.this, message, Toast.LENGTH_SHORT).show();
		}
	}
}
