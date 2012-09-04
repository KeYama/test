package jp.co.johospace.jsphoto.util;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * 設定値ユーティリティ
 */
public class PreferenceUtil {

	/**
	 * 設定値の取得
	 *
	 * @param context コンテキスト
	 * @param key キー
	 * @return 設定値
	 */
	public static String getPreferenceValue(Context context, String key) {
		return PreferenceUtil.getPreferenceValue(context, key, "");
	}
	
	
	/**
	 * 設定値の取得 デフォルト値あり
	 *
	 * @param context コンテキスト
	 * @param key キー
	 * @return 設定値
	 */
	public static String getPreferenceValue(Context context, String key, String def) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, def);
	}

	
	/**
	 * 数値設定値の取得
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static int getIntPreferenceValue(Context context, String key) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getInt(key, -1);
	}
	
	public static int getIntPreferenceValue(Context context, String key, int def) {

		return PreferenceManager.getDefaultSharedPreferences(context)
				.getInt(key, def);
	}
	
	
	/**
	 * 真偽設定値の取得
	 *
	 * @param context コンテキスト
	 * @param key キー
	 * @return 設定値
	 */
	public static boolean getBooleanPreferenceValue(Context context, String key) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(key, false);
	}
	
	public static boolean getBooleanPreferenceValue(Context context, String key, boolean def) {

		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(key, def);
	}

	
	/**
	 * 設定値を設定
	 * 
	 * @param context コンテキスト
	 * @param key キー
	 * @param value 設定値
	 */
	public static void setPreferenceValue(Context context, String key, String value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(key, value);
		editor.commit();
	}

	
	/**
	 * 数値設定値の設定
	 * 
	 * @param context	コンテキスト
	 * @param key		キー
	 * @param value	設定値
	 */
	public static void setIntPreferenceValue(Context context, String key, int value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putInt(key, value);
		editor.commit();
	}
	
	/**
	 * 真偽設定値の設定
	 *
	 * @param context コンテキスト
	 * @param key キー
	 * @param value 設定値
	 */
	public static void setBooleanPreferenceValue(Context context, String key, boolean value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(key, value);
		editor.commit();
	}
	
	/**
	 * 設定値を削除します
	 * 
	 * @param context	コンテキスト
	 * @param key		キー
	 */
	public static void deletePreferenceValue(Context context, String key) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.remove(key);
		editor.commit();
	}
}
