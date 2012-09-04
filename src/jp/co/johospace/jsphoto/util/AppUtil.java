package jp.co.johospace.jsphoto.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class AppUtil {
	private static final String tag = AppUtil.class.getSimpleName();

	/**
	 * Asset ファイルから文字列の取得
	 * @param assetManager
	 * @param fileName
	 * @return
	 */
	public static String getTextFromAssetFile(AssetManager assetManager, String fileName) {
		InputStream inputStream = null;
		BufferedReader bufferedReader = null;
		StringBuilder sentence = new StringBuilder();

		try {
			inputStream = assetManager.open(fileName);
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 64);

			String line;
			while((line = bufferedReader.readLine()) != null) {
				sentence.append(line + "\n");
			}
		} catch (IOException e) {
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}

				if (inputStream != null) {
					inputStream.close();
				}
			} catch(Exception ex) {}
		}

		return sentence.toString();
	}

	/**
	 * Assetsのファイルを文字列にして返します
	 * 
	 * @param assetManager
	 * @param fileName
	 * @return
	 */
	public static String getTextFromAsset(AssetManager assetManager, String fileName) {
		InputStream inputStream = null;
		BufferedReader bufferedReader = null;
		StringBuilder sentence = new StringBuilder();

		try {
			inputStream = assetManager.open(fileName);
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 64);

			String line;
			while((line = bufferedReader.readLine()) != null) {
				sentence.append(line + "\n");
			}
		} catch (IOException e) {
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}

				if (inputStream != null) {
					inputStream.close();
				}
			} catch(Exception ex) {}
		}

		return sentence.toString();
	}

	/**
	 * JSバックアップについてのファイル名の取得
	 * @param context
	 * @return
	 */
	public static String getAboutJorlleTextFileName(Context context) {
		if(isJapanase(context)) {
			return "terms_jp.txt";
		} else {
			return "terms_en.txt";
		}
	}

	/**
	 * ロケールの日本語可否
	 *
	 * @param context コンテキスト
	 * @return true:
	 */
	public static boolean isJapanase(Context context) {
		String locale = getLocale(context);
		if (locale.equals(Locale.JAPANESE.getLanguage())) {
			return true;
		}

		return false;
	}

	/**
	 * ロケールの取得
	 * @param context
	 * @return
	 */
	public static String getLocale(Context context) {
		return context.getResources().getConfiguration().locale.getLanguage();
	}

	/**
	 * ヘルプのHTMLファイル名を取得
	 * @param context
	 * @return
	 */
	public static String getHelpHtmlName (Context context) {
		if (isJapanase(context)) {
			return "help_jp.html";
		} else {
			return "help_en.html";
		}
	}
	
	/**
	 * 利用規約ファイル名の取得
	 * @param context
	 * @return
	 */
	public static String getTermsAgreementTextFileName(Context context) {
		if(isJapanase(context)) {
			return "terms_jp.txt";
		} else {
			return "terms_en.txt";
		}
	}
	
	/**
	 * ネットワークに接続されているかどうかを取得します。
	 * @param context
	 * @return
	 */
	public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if( ni != null ){
            return cm.getActiveNetworkInfo().isConnected();
        }
        return false;
	}
	
}
