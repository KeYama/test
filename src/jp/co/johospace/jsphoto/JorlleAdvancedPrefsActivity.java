package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.SecretCheckPasswordDialog;
import jp.co.johospace.jsphoto.dialog.SecretPasswordDialog;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class JorlleAdvancedPrefsActivity extends PreferenceActivity implements CMediaIndex{

	/** ダイアログ：キャッシュ削除 */
	private static final int DIALOG_CLEAR_CACHE = 1;
	/** ダイアログ：シークレット画像リセット */
	private static final int DIALOG_CLEAR_SECRET = 2;

	/** データベース */
	private SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();

	/**
	 * 初期設定
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_help);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {

		// プリファレンスキー取得
		final String key = preference.getKey();
		
		if(ApplicationDefine.PREF_IMAGE_AUTO_ROTATION.equals(key)) {
			// 画像の向きの自動回転押下時
			final boolean checked = ((CheckBoxPreference) preference).isChecked();
			PreferenceUtil.setBooleanPreferenceValue(this, key, checked);
		} else if (ApplicationDefine.PREF_INCLUDE_VIDEOS.equals(key)) {
			// 動画ファイルを含む押下時
			final boolean checked = ((CheckBoxPreference) preference).isChecked();
			PreferenceUtil.setBooleanPreferenceValue(this, key, checked);
		} else if (ApplicationDefine.PREF_CLEAR_CACHE.equals(key)) {
			// キャッシュの削除押下時
			showDialog(DIALOG_CLEAR_CACHE);
		} else if (ApplicationDefine.PREF_CLEAR_SECRET.equals(key)) {
			// シークレット画像のリセット押下時
			if (PreferenceUtil.getPreferenceValue(this, ApplicationDefine.KEY_SECRET_PASSWORD, null) == null) {
				Toast.makeText(JorlleAdvancedPrefsActivity.this, getString(R.string.pref_advanced_message_no_secret_password), Toast.LENGTH_SHORT).show();
			} else {
				showDialog(DIALOG_CLEAR_SECRET);
			}
		} else if (ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY.equals(key)) {
			// 隠しフォルダ押下時
			final boolean checked = ((CheckBoxPreference) preference).isChecked();
			PreferenceUtil.setBooleanPreferenceValue(this, key, checked);
		} else if (ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY.equals(key)) {
			
			// シークレットフォルダ表示押下時
			String password = PreferenceUtil.getPreferenceValue(this, ApplicationDefine.KEY_SECRET_PASSWORD, null);
			
			final CheckBoxPreference check = ((CheckBoxPreference) preference);
			final boolean checked = check.isChecked();
			
			// パスワード未設定時は、パスワードの設定ダイアログ表示
			if (password == null) {
				final SecretPasswordDialog passwordDialog = new SecretPasswordDialog(this);
				passwordDialog.setOnDismissListener(new OnDismissListener() {
					
					@Override
					public void onDismiss(DialogInterface dialog) {
						if (passwordDialog.mIsSetPassword) {
							PreferenceUtil.setBooleanPreferenceValue(JorlleAdvancedPrefsActivity.this, key, checked);
						} else {
							check.setChecked(false);
						}
					}
				});
				passwordDialog.show();
				
			// パスワード設定済み時
			} else {
				
				// シークレット表示時、パスワードの確認ダイアログ表示
				if (checked) {
					final SecretCheckPasswordDialog checkDialog = new SecretCheckPasswordDialog(this);
					checkDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
						
						@Override
						public void onDismiss(DialogInterface dialog) {
							if (checkDialog.mIsSetPassword) {
								PreferenceUtil.setBooleanPreferenceValue(JorlleAdvancedPrefsActivity.this, key, checked);
							} else {
								check.setChecked(false);
							}
						}
					});
					checkDialog.show();
					
				} else {
					PreferenceUtil.setBooleanPreferenceValue(JorlleAdvancedPrefsActivity.this, key, checked);
				}
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_CLEAR_CACHE:
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
		case DIALOG_CLEAR_SECRET:
			

			return new AlertDialog.Builder(this)
			.setCancelable(true)
			.setTitle(getString(R.string.pref_advanced_clear_secret))
			.setMessage(getString(R.string.pref_advanced_txt_clear_secret))
			.setPositiveButton(android.R.string.cancel, null)
			.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					final boolean dispSecret = PreferenceUtil.getBooleanPreferenceValue(JorlleAdvancedPrefsActivity.this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false);
					
					// シークレット表示状態
					if (dispSecret) {
						// シークレット全解除
						SecretReleaseTask task = new SecretReleaseTask();
						task.execute();
						
					// シークレット非表示状態
					} else {
						
						// パスワードの確認
						final SecretCheckPasswordDialog checkDialog = new SecretCheckPasswordDialog(JorlleAdvancedPrefsActivity.this);
						checkDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
							
							@Override
							public void onDismiss(DialogInterface dialog) {
								// パスワードが正しければ、シークレット全解除
								if (checkDialog.mIsSetPassword) {
									SecretReleaseTask task = new SecretReleaseTask();
									task.execute();
								}
							}
						});
						checkDialog.show();
						dialog.dismiss();
					}
				}
			})
			.create();
		}
		return super.onCreateDialog(id);
	}
	protected class DeleteDatabaseTask extends AsyncTask<String, Integer, Long>{
		private ProgressDialog mProgressDialog = null;

		@Override
		protected Long doInBackground(String... params) {
			SQLiteDatabase db = OpenHelper.cache.getDatabase();
			db.beginTransaction();
			try {
				db.delete($TABLE, null, null);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			return null;
		}
		@Override
		protected void onPreExecute() {
			mProgressDialog = new ProgressDialog(JorlleAdvancedPrefsActivity.this);
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
	
	/**
	 * シークレット操作タスク
	 */
	protected class SecretReleaseTask extends AsyncTask<Void, Void, Boolean> {
		
		ProgressDialog mFolderProgress;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			// プログレスダイアログを設定　表示
			mFolderProgress = new ProgressDialog(JorlleAdvancedPrefsActivity.this);
			
			mFolderProgress.setMessage(getString(R.string.folder_message_progress_no_secret));
			mFolderProgress.setCancelable(false);
			mFolderProgress.show();
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			// シークレットを全解除
			// TODO 現在は、ひとまずSDカードを設定
			boolean result = MediaUtil.changeMediaSecret(mDatabase, Environment.getExternalStorageDirectory().getPath(), false, true);
			MediaUtil.scanMedia(getApplicationContext(), Environment.getExternalStorageDirectory(), true);
			
			return result;
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
			Toast.makeText(JorlleAdvancedPrefsActivity.this, message, Toast.LENGTH_SHORT).show();
		}
	}
	
}
