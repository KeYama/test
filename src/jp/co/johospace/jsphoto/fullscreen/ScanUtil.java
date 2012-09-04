package jp.co.johospace.jsphoto.fullscreen;

import java.io.File;
import java.util.Calendar;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

public class ScanUtil {
	public static void setParamByIntent(Context context, JorlleMediaScanner scanner, Intent intent){
		String folderPath = intent.getStringExtra(ApplicationDefine.INTENT_FOLDER_PATH);		
		// サブフォルダ検索の可否を取得
		boolean scanSub = intent.getBooleanExtra(ApplicationDefine.INTENT_FOLDER_SUB, false);
		
		// フィルタリング　シークレットのみを取得
		boolean folderSecret = intent.getBooleanExtra(ApplicationDefine.INTENT_FOLDER_SECRET, false);
		
		// フィルタリング　お気に入り状態を取得
		boolean folderFavorite = intent.getBooleanExtra(ApplicationDefine.INTENT_FOLDER_FAVORITE, false);
		
		// フィルタリング　開始日を取得
		Long startTime = intent.getLongExtra(ApplicationDefine.INTENT_FOLDER_START, -1);
		
		// フィルタリング　終了日を取得
		Long endTime = intent.getLongExtra(ApplicationDefine.INTENT_FOLDER_END, -1);
		
		// フィルタリング　タグを取得
		String tagName = intent.getStringExtra(ApplicationDefine.INTENT_FOLDER_TAG);
		
		// フィルタリング　タグ一覧を取得
		String[] folderTagList = intent.getStringArrayExtra(ApplicationDefine.INTENT_FOLDER_TAG_LIST);
			

		// シークレットのみ
		if (folderSecret) {
			scanner.scanOnlySecret();
		}
		
		// お気に入り
		if (folderFavorite) {
			scanner.favorite();
		} 
		
		// 開始日
		if (startTime >= 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(startTime);
			scanner.startTime(cal);
		}
		
		// 終了日
		if (endTime >= 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(endTime);
			scanner.endTime(cal);
		}
		
		// タグ
		if (tagName != null) {
			scanner.addTag(tagName);
		}
		
		// タグリスト
		if (folderTagList != null) {
			for (String tag : folderTagList) {
				scanner.addTag(tag);
			}
		}
		File folder;
		
		if (folderPath == null) {
			// フォルダパス未設定ならば、SDカード内部を表示
			folder = Environment.getExternalStorageDirectory();
			folderPath = folder.getAbsolutePath();
		} else {
			// 取得したパスを元に、フォルダ内の画像一覧を表示
			folder = new File(folderPath);
		}
		// 隠しフォルダ表示状態
		boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(context, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(context, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false);
		
		//実際の探す処理はRadioButtonのハンドラが行う
		scanner.sort(new JorlleMediaScanner.DateAscender()).baseFolder(folder).scanNomedia(hiddenState).scanSecret(secretState).scanSubfolder(scanSub);
		
	}
}
