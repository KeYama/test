package jp.co.johospace.jsphoto.define;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import android.os.Environment;

/**
 * 共通定数クラス
 */
public class ApplicationDefine {
	
	/** ネットワークの並列読み込み数 */
	public static final int NUM_NETWORK_THREAD = 3;

	/** インテントに使用するkey */
	/** 画像パス */
	public static final String INTENT_PATH = "path";
	/** 画像パス一覧 */
	public static final String INTENT_PATH_LIST = "pathlist";
	/** タイトル */
	public static final String INTENT_TITLE = "title";
	/** タグ追加　最終的に追加するタグ */
	public static final String INTENT_RESULT = "result";
	/** フォルダパス */
	public static final String INTENT_FOLDER_PATH = "folderPath";
	/** フィルタリング　シークレット */
	public static final String INTENT_FOLDER_SECRET = "secret";
	/** フィルタリング　お気に入り */
	public static final String INTENT_FOLDER_FAVORITE = "favorite";
	/** フィルタリング　開始日 */
	public static final String INTENT_FOLDER_START = "start";
	/** フィルタリング　終了日 */
	public static final String INTENT_FOLDER_END = "end";
	/** フィルタリング　タグ */
	public static final String INTENT_FOLDER_TAG = "tag";
	/** フィルタリング　タグ一覧 */
	public static final String INTENT_FOLDER_TAG_LIST = "tagList";
	/** フィルタリング　サブフォルダのスキャン */
	public static final String INTENT_FOLDER_SUB = "scanSub";
	/** カテゴリ名 */
	public static final String INTENT_CATEGORY_NAME = "categoryName";

	public static final String EXTRA_LAUNCHER =
			ApplicationDefine.class.getSimpleName() + ".EXTRA_LAUNCHER";

	/** ホーム画面から遷移 */
	public static final String INTENT_CHANGE_HOME = "changeHome";
	/** フィルタリング初期化 */
	public static final String INTENT_INIT_FILTER = "initFilter";
	/** フォルダ一覧から遷移 */
	public static final String INTENT_MOVE_FOLDER = "moveFolder";

	/** タブ遷移管理_KEY */
	public static final String NAVIGATION_TAB = "navi";

	/** リクエストコード */
	public static final int REQUEST_LOCAL = 100;
	/** リクエストコード　画像一覧 */
	public static final int REQUEST_GRID = 101;
	/** リクエストコード　タグ編集 */
	public static final int REQUEST_TAG = 102;
	/** リクエストコード　設定画面 */
	public static final int REQUEST_PREF_SETTING = 103;
	/** リクエストコード　イントロチュートリアル画面 */
	public static final int REQUEST_PREF_TUTORIAL_INTRO = 110;

	/** リザルトコード */
	public static final int RESULT_LOCAL = 100;
	/** リザルトコード:ホームボタン */
	public static final int RESULT_HOME = 101;

	/** リザルトコード：インフォリンクチュートリアル */
	public static final int RESULT_TUTORIALINFOLINK_SEARCH = 0;
	/** リザルトコード：インフォリンクチュートリアル */
	public static final int RESULT_TUTORIALINFOLINK_ONLINE = 1;
	/** リザルトコード：インフォリンクチュートリアル */
	public static final int RESULT_TUTORIALINFOLINK_AUTOALBUM = 2;

	/** 日付ダイアログ　ボタンインデックス 設定*/
	public static final int INDEX_DATE_SETTING = 0;
	/** 日付ダイアログ　ボタンインデックス キャンセル*/
	public static final int INDEX_DATE_CANCEL = 1;


	/** メタデータのタイプ */
	public static final String METADATA_TYPE_TAG = "tag";

	/** .nomediaファイル名 */
	public static final String NO_MEDIA = ".nomedia";
	/** シークレット拡張子 */
	public static final String SECRET = ".secret";

	/** jsphotoフォルダのパス */
	public static final String PATH_JSPHOTO = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/jsphoto";
	
	/** ダミー画像ファイル名 */
	public static final String DUMMY_IMAGE_FILENAME = "dummy_jsphoto.jpg";

	/** メタデータタイプ　タグ */
	public static final String MIME_TAG = CMediaMetadata.TYPE_TAG;
	/** メタデータタイプ　お気に入り */
	public static final String MIME_FAVORITE = CMediaMetadata.TYPE_FAVORITE;

	/** フォルダ一覧を初期表示 */
	public static final String INIT_ACTIVITY_HOME = "init_activity_home";
	public static final String INIT_ACTIVITY_FOLDER = "init_activity_folder";

	/** 表示フォルダ一覧のタイプ */
	public static final String FOLDER_TYPE_LOCAL = "folderTypeLocal";
	public static final String FOLDER_TYPE_PICASA = "folderTypePicasa";
	public static final String FOLDER_TYPE_TAG = "folderTypeTag";

	/** タグ種別名 */
	public static final String TAG_CATEGORY_FAVORITE = "favorite";
	public static final String TAG_CATEGORY_SECRET = "secret";
	public static final String TAG_CATEGORY_TAG = "tag";

	/** プリファレンスのキー */

	/** 初期表示画面 */
	public static final String KEY_INIT_ACTIVITY = "init_activity";
	public static final String KEY_FOLDER_TYPE = "folder_type";

	/** 初回起動時かどうかのキー */
	public static final String KEY_NOT_FIRST_TIME = "notFirstTime";

	/** 検索画面の設定 */
	public static final String KEY_SEARCH_SET_01 = "smartSearchSetting01";

	/** ホーム画面　カテゴリ */
	public static final String KEY_CATEGORY_LOCAL = "category_local";
	public static final String KEY_CATEGORY_SYNC = "category_sync";
	public static final String KEY_CATEGORY_SERVICE = "category_service";
	public static final String KEY_CATEGORY_SECRET = "category_secret";
	public static final String KEY_CATEGORY_PICASA = "category_picasa";
	public static final String KEY_CATEGORY_FAVORITE = "category_favorite";
	public static final String KEY_CATEGORY_AUTO_ALBUM = "category_auto_album";

	/** タブ */
	public static final String TAB_TOP = "top";
	public static final String TAB_LOCAL = "local";
	public static final String TAB_ONLINE = "online";
	public static final String TAB_TAG = "tag";
	public static final String TAB_AUTO = "auto";
	public static final String TAB_CAMERA = "camera";

	/** インフォリンク イベント最大数 */
	public static final int INFOLINK_MAX_SCHEDULES = 3;
	
	/** フォルダ一覧　表示形式 */
	public static final String KEY_FOLDER_VIEW_MODE = "folder_view_mode";

	/** フォルダ一覧（タグ） カテゴリ表示状態*/
	public static final String KEY_TAG_CATEGORY_FAVORITE ="tag_category_favorite";
	public static final String KEY_TAG_CATEGORY_SECRET ="tag_category_secret";
	public static final String KEY_TAG_CATEGORY_TAG = "tag_category_tag";

	/** シークレット　パスワード */
	public static final String KEY_SECRET_PASSWORD = "secret_password";

	/** フィルタリング　項目 */
	public static final String KEY_FILTERING_FAVORITE = "filtering_favorite";
	public static final String KEY_FILTERING_START_DATE = "filtering_start_date";
	public static final String KEY_FILTERING_END_DATE = "filtering_end_date";

	/** 設定ボタンの表示・非表示 */
	public static final String KEY_SHOW_SYNC_SET_BUTTON = "show_sync_set_button";
	public static final String KEY_SHOW_SERVICE_ADD_BUTTON = "show_service_add_button";
	public static final String KEY_SHOW_SELECT_CALENDAR_BUTTON = "show_select_calendar_button";

	/** 画像の向きの自動回転 */
	public static final String PREF_IMAGE_AUTO_ROTATION = "image_automatic_rotation";
	/** 動画ファイルを含むか否か */
	public static final String PREF_INCLUDE_VIDEOS = "include_videos";
	/** キャッシュの削除 */
	public static final String PREF_CLEAR_CACHE = "clear_cache";
	/** フォルダグリッド表示サイズ*/
	public static final String PREF_FOLDERGRID_SIZE = "foldergrid_size";
	/** シークレットフォルダ/画像のリセット */
	public static final String PREF_CLEAR_SECRET = "clear_secret";
	/** 隠しフォルダ表示/非表示 */
	public static final String PREF_HIDDEN_FOLDER_DISPLAY= "hidden_folder_display";
	/** シークレットフォルダ表示 */
	public static final String PREF_SECRET_FOLDER_DISPLAY = "secret_folder_display";
}
