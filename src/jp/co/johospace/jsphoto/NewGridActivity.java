package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.accessor.MediaIndexesAccessor;
import jp.co.johospace.jsphoto.accessor.MediaMetaDataAccessor;
import jp.co.johospace.jsphoto.cache.ImageCache;
import jp.co.johospace.jsphoto.cache.LocalCachedThumbnailLoader;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.SecretCheckPasswordDialog;
import jp.co.johospace.jsphoto.dialog.SimpleEditDialog;
import jp.co.johospace.jsphoto.grid.AsyncFileOperation;
import jp.co.johospace.jsphoto.grid.ExifView;
import jp.co.johospace.jsphoto.grid.ExtUtil;
import jp.co.johospace.jsphoto.grid.FavoriteUtil;
import jp.co.johospace.jsphoto.grid.WallpaperHelper;
import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import jp.co.johospace.jsphoto.util.HeaderController;
import jp.co.johospace.jsphoto.util.MediaStoreOperation;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.widget.UXGridDataSource;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget;
import jp.co.johospace.jsphoto.ux.widget.UXStage;
import jp.co.johospace.jsphoto.ux.widget.UXUnit;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * OpenGL版グリッドアクティビティ。
 * 
 * Intentについて。 
 * INTENT_FOLDER_TYPE: FOLDER_TYPE_***のいずれかを指定
 * INTENT_TARGET/INTETN_TARGET_LIST: FOLDER_TYPEに依存。詳細は該当変数のソースにて。
 * 
 */
public class NewGridActivity extends NavigatableActivity implements OnClickListener {
	/** フォルダタイプ。int。FOLDER_TYPE_***を指定 */
	public static final String INTENT_FOLDER_TYPE = "FolderType";
	/**
	 * FolderTypeに依存したターゲット。String
	 * 
	 * PATH: 起点となるパス 
	 * TAG: タグ名
	 */
	public static final String INTENT_TARGET = "Target";

	/**
	 * FolderTypeに依存したターゲットリスト。StringArray
	 * 
	 * TAG_LIST: タグのリスト
	 */
	public static final String INTENT_TARGET_LIST = "TargetList";

	/** 遷移元の判定 */
	public static final String INTENT_TARGET_PARENT = "Parent";
	public static final String PARENT_LOCAL = "Local";
	public static final String PARENT_TAG = "Tag";

	public static final int FOLDER_TYPE_NONE = 0;
	/** ローカルパス */
	public static final int FOLDER_TYPE_PATH = 1;
	/** シークレット */
	public static final int FOLDER_TYPE_SECRET = 2;
	/** お気に入り */
	public static final int FOLDER_TYPE_FAVORITE = 3;
	/** タグ */
	public static final int FOLDER_TYPE_TAG = 4;
	/** タグリスト */
	public static final int FOLDER_TYPE_TAG_LIST = 5;

	/** 遷移元による列数 */
	private static final int 
		COLUMN_TUTORIAL_PORTLAIT = 2,
		COLUMN_TUTORIAL_LANDSCAPE = 4, 
		COLUMN_NORMAL_PORTLAIT = 3,
		COLUMN_NORMAL_LANDSCAPE = 5;

	/** QuickPicのサムネイルサイズと同値 */
	private static final int WIDTH = 228;
	private String base = null;
	private AsyncFileOperation mFileOp;
	private ArrayList<String> mFileNameList = new ArrayList<String>();
	private JorlleMediaScanner mScanner;
	private UXStage mStage;
	private UXGridWidget mGrid;
	private String mBaseFolder;
	private HeaderController mHeaderController;
	private View mHeader;

	/** 検索画面からの遷移フラグ */
	private boolean mIsFromSearch;

	/** チュートリアル画面からの遷移フラグ */
	private boolean mIsMoveTutorial;

	/** 列数 */
	private int mColumnCount;

	private AlertDialog nameDialog = null;
	/** 名前変更入力EditText */
	private EditText mEditText;
	/** 壁紙表示ヘルパー */
	private WallpaperHelper mWallpaper;

	/** 遷移元判定文字列 */
	private String mParentName;

	/** 複数選択フラグ */
	private boolean mMultiMode = false;

	/** Exif情報参照フラグ */
	private boolean mIsReflectionExif;
	
	/** データベース */
	private SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();

	/** お気に入り、シークレット状態キャッシュ */
	private HashMap<String, Boolean> mCashFavorite = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> mCashSecret = new HashMap<String, Boolean>();
	private HashMap<String, String> mCachePath = new HashMap<String, String>();
	private HashMap<String, Boolean> mCashVideo = new HashMap<String, Boolean>();
	
	/** プログレス */
	private ProgressDialog mProgress;
	
	/** ダイアログID */
	private static final int DIALOG_SECRET = 0;
	private static final int DIALOG_NO_SECRET = 1;
	private static final int DIALOG_INFO = 2;
	private static final int DIALOG_CONFIRM_DELETE = 3;
	private static final int DIALOG_CONFIRM_MULTI_DELETE = 4;
	private static final int DIALOG_SORT = 5;

	/** 選択されたファイルのパス */
	private String mTargetPath;

	/** ソート内容 */
	private int mSortAlgorithm = SORT_DATE_ASC;

	/** 表示順序ダイアログの項目 */
	protected static final int 
		SORT_DATE_ASC = 0, 
		SORT_DATE_DESC = 1,
		SORT_NAME_ASC = 2, 
		SORT_NAME_DESC = 3;

	/** オーバレイ格納 */
	private UXGridWidget.OverlayGrid mFactory;
	/** オーバレイID お気に入り */
	private int mOverlayFavId;
	/** オーバレイID シークレット */
	private int mOverlaySecId;
	/** オーバーレイIDチェックボックス */
	private int mOverlayCheckId;
	private boolean[] mCheckState;
	/** オーバレイID ビデオ **/
	private int mOverlayVideoId;
	/** ハンドラ */
	protected Handler mHandler = new Handler();
	/** 複数選択メニュー1 */
	private LinearLayout mMultiMenu1;
	/** 複数選択メニュー2 */
	private LinearLayout mMultiMenu2;

	/** 複数選択テキスト お気に入り */
	private TextView mTxtMultiFavorite;
	/** 複数選択テキスト シークレット */
	private TextView mTxtMultiSecret;

	/** お気に入り処理タスク */
	private FavoriteTask mMultiFavoriteTask;
	
	/** カメラ画面から戻った際にリスキャンをかける **/
	private boolean isRescan = false;
	
	/** ローカル一覧からの遷移 */
	private boolean mIsLocalGrid = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		init();
	}

	private String mCategoryName;

	/**
	 * 初期表示処理
	 */
	private void init() {

		Intent intent = getIntent();
		if (intent == null)return;

		mScanner = new JorlleMediaScanner();

		switch (intent.getIntExtra(INTENT_FOLDER_TYPE, FOLDER_TYPE_NONE)) {
		case FOLDER_TYPE_PATH:
			setBaseFolder(intent, mScanner);
			mCategoryName = new File(mBaseFolder).getName();
			mIsLocalGrid = true;
			break;
		case FOLDER_TYPE_SECRET:
			setBaseFolder(intent, mScanner);
			mScanner.scanOnlySecret();
			mScanner.scanSubfolder(true);
			mCategoryName = getString(R.string.tagList_title_secret);
			break;
		case FOLDER_TYPE_FAVORITE:
			setBaseFolder(intent, mScanner);
			mScanner.favorite();
			mScanner.scanSubfolder(true);
			mCategoryName = getString(R.string.tagList_title_favorite);
			break;
		case FOLDER_TYPE_TAG:
			mScanner.baseFolder(Environment.getExternalStorageDirectory());
			mScanner.addTag(intent.getStringExtra(INTENT_TARGET));
			mScanner.scanSubfolder(true);
			mCategoryName = intent.getStringExtra(INTENT_TARGET);
			break;
		case FOLDER_TYPE_TAG_LIST:
			mScanner.baseFolder(Environment.getExternalStorageDirectory());
			String[] tags = intent.getStringArrayExtra(INTENT_TARGET_LIST);
			for (String tag : tags) {
				mScanner.addTag(tag);
			}
			mScanner.scanSubfolder(true);
			mCategoryName = TextUtils.join(", ", tags);
			break;
		default:
			throw new IllegalArgumentException("invalid INTENT_FOLDER_TYPE");
		}

		mIsFromSearch = intent.getBooleanExtra(SearchActivity.INTENT_NEWGRID_FROM_TUTORIAL, false);

		// チュートリアル画面からの遷移状態
		mIsMoveTutorial = intent.getBooleanExtra(SearchActivity.INTENT_TUTORIAL, false);

		// 表示列数取得
		getColumnCount();

		// 遷移元判定
		mParentName = intent.getStringExtra(INTENT_TARGET_PARENT);

		mFileOp = new AsyncFileOperation(this.getParent());
		mFileOp.setOnCompleteListener(new AsyncFileOperation.OnCompleteListener() {

			@Override
			public void onComplete() {
				rescanMedia(null);
			}

			@Override
			public void onCancel() {
				rescanMedia(null);
			}
		});

		// 隠しフォルダ表示状態
		boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this,ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this,ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, true);

		mScanner.scanNomedia(hiddenState).scanSecret(secretState);

		// 日付の新しい順
		mScanner.sort(new JorlleMediaScanner.DateAscender());

		mScanner.findMedia(mScanListener);

		// ここからがUXStageの使用
		mStage = new UXStage(this, UXStage.GL_RENDERER);
		mStage.setBackgroundColor(Color.rgb(20, 20, 20));
		mStage.setScrollbarResource(R.drawable.slider, 28, 65);

		// Stageに変更を加える場合、必ずlockStageを経由
		//
		// Stageに変更とは、Stageにぶら下がっているすべてのクラスの変更のこと。
		// たとえばDataSourceの中身に変更を加える場合でも必要となる。
		//
		mStage.lockStage(new Runnable() {

			@Override
			public void run() {

				// Exif情報取得フラグをセットし、ローダーを作成
				LocalCachedThumbnailLoader thumbnailLoader = new LocalCachedThumbnailLoader();
				mIsReflectionExif = PreferenceUtil.getBooleanPreferenceValue(NewGridActivity.this,ApplicationDefine.PREF_IMAGE_AUTO_ROTATION, true);
				thumbnailLoader.setReflectionExif(mIsReflectionExif);

				// mGrid = new UXGridWidget(WIDTH, new TmpThumbnailLoader());
				mGrid = new UXGridWidget(WIDTH, thumbnailLoader);
				// mGrid
				// .dataSource(mMyDataSource)
				// .padding(5, UXUnit.DP)
				// .itemType(new UXGridWidget.ThumbnailGrid())
				// .column(mColumnCount)
				// .addTo(mStage);

				mGrid.setOnItemTapListener(new UXGridWidget.ItemTapListener() {

					@Override
					public void onTap(int itemNumber) {
						onItemTap(itemNumber);
					}
				});

				mGrid.setOnItemLongPressListener(new UXGridWidget.ItemLongPressListener() {

					@Override
					public void onLongPress(int itemNumber) {
						// 複数選択モード時は抑止
						if (!mMultiMode) {
							mAttachContext2Options = true;
							mContextItemNumber = itemNumber;
							openOptionsMenu();
						}
					}
				});

				Drawable fav = getResources().getDrawable(R.drawable.icon_star);
				Drawable sec = getResources().getDrawable(R.drawable.icon_secret);

				mFactory = new UXGridWidget.OverlayGrid(new MyOverlayDataSource());
				UXGridWidget.OverlayItem[] favItem = new UXGridWidget.OverlayItem[1];
				favItem[0] = new UXGridWidget.OverlayItem(
						fav, // 表示するDrawable
						UXGridWidget.OverlayGrid.POSITION_LEFT_TOP,// 表示位置
						30, UXUnit.DP, // 幅
						30, UXUnit.DP, // 高さ
						-5, UXUnit.DP // マージン
				);
				UXGridWidget.OverlayItem[] secItem = new UXGridWidget.OverlayItem[1];
				secItem[0] = new UXGridWidget.OverlayItem(
						sec, // 表示するDrawable
						UXGridWidget.OverlayGrid.POSITION_LEFT_BOTTOM,// 表示位置
						30, UXUnit.DP, // 幅
						30, UXUnit.DP, // 高さ
						-5, UXUnit.DP // マージン
				);

				UXGridWidget.OverlayItem[] checkItem = new UXGridWidget.OverlayItem[2];
				checkItem[0] = new UXGridWidget.OverlayItem(
						getResources().getDrawable(R.drawable.check_off_normal), // 表示するDrawable
						UXGridWidget.OverlayGrid.POSITION_RIGHT_TOP,// 表示位置
						30, UXUnit.DP, // 幅
						30, UXUnit.DP, // 高さ
						0, UXUnit.DP //マージン
				);
				checkItem[1] = new UXGridWidget.OverlayItem(
						getResources().getDrawable(R.drawable.check_on_normal), // 表示するDrawable
						UXGridWidget.OverlayGrid.POSITION_RIGHT_TOP,// 表示位置
						30, UXUnit.DP, // 幅
						30, UXUnit.DP, // 高さ
						0, UXUnit.DP //マージン
				);

				UXGridWidget.OverlayItem[] videoItem = new UXGridWidget.OverlayItem[1];
				videoItem[0] = new UXGridWidget.OverlayItem(
						getResources().getDrawable(R.drawable.icon_movie), // 表示するDrawable
						UXGridWidget.OverlayGrid.POSITION_CENTER,// 表示位置
						57, UXUnit.DP, // 幅
						57, UXUnit.DP, // 高さ
						0, UXUnit.DP //マージン
				);
				
				// mFactory.setRibbonWidth(30, UXUnit.DP);

				mOverlayFavId = mFactory.addOverlay(favItem);
				mOverlaySecId = mFactory.addOverlay(secItem);
				mOverlayCheckId = mFactory.addOverlay(checkItem);
				mOverlayVideoId = mFactory.addOverlay(videoItem);
				mGrid
					.dataSource(mMyDataSource)
					.padding(11, UXUnit.DP)
					.itemType(mFactory)
					.column(mColumnCount)
					.addTo(mStage);

			}
		});

		mEditText = new EditText(this);

		// 最後にViewを追加
		setContentView(R.layout.newgrid_basis);
		((LinearLayout)findViewById(R.id.lytStage)).addView(mStage.getView());

		// ヘッダ関連の準備
		// オンラインでも使用しているレイアウトを使用
		mHeaderController = new HeaderController(this, R.layout.ol_folder_info,(ViewGroup) getWindow().getDecorView());
		mHeaderController.setHeaderEventTo(mStage);
		mHeader = mHeaderController.getView();
		mHeader.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT));

		setHeaderInfo();

		// ヘッダを表示
		mHeaderController.show(HeaderController.LENGTH_DEFAULT);


		// 複数選択メニューの縦幅計算
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		Configuration config = getResources().getConfiguration();

		int height;

		if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
			height = (int) (display.getHeight() / 4.2);
		} else {
			height = (int) (display.getHeight() / 3.5);
		}

		// オプションメニュー1(複数選択時用 初期はGONE)
		LayoutParams optionParams = new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, height);

		LayoutInflater inflater1 = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mMultiMenu1 = (LinearLayout) inflater1.inflate(R.layout.multiple_menu_first, null);
		mMultiMenu1.setVisibility(View.GONE);
		((LinearLayout)findViewById(R.id.lytMenu1)).addView(mMultiMenu1, optionParams);

		//クリックイベントを設定
		int[] multiMenu1Ids = {R.id.lytMultipleOff, R.id.lytTag, R.id.lytShare,R.id.lytDelete,R.id.lytDetail,R.id.lytOther};
		for(int id : multiMenu1Ids){
			mMultiMenu1.findViewById(id).setOnClickListener(this);
		}
		

		// オプションメニュー2(複数選択時用 初期はGONE)
		LayoutInflater inflater2 = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mMultiMenu2 = (LinearLayout) inflater2.inflate(R.layout.multiple_menu_second, null);
		mMultiMenu2.setVisibility(View.GONE);
		((LinearLayout)findViewById(R.id.lytMenu2)).addView(mMultiMenu2, optionParams);

		// 複数 お気に入り
		mTxtMultiFavorite = (TextView) mMultiMenu2.findViewById(R.id.txtFavorite);
		
		// 複数 シークレット
		mTxtMultiSecret = (TextView) mMultiMenu2.findViewById(R.id.txtSecret);

		//クリックイベントを設定
		int[] multiMenu2Ids = {R.id.lytMove, R.id.lytCopy, R.id.lytMoveSync, R.id.lytFavorite, R.id.lytSecret, R.id.lytSimpleMenu};
		for(int id : multiMenu2Ids){
			mMultiMenu2.findViewById(id).setOnClickListener(this);
		}
		
		if (mProgress == null) {
			mProgress = new ProgressDialog(getParent());
			mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgress.setMessage(getString(R.string.folder_message_progress));
			mProgress.setCancelable(false);
		}
		

	}

	/**
	 * チェック状態にあるファイルリストを得る
	 * 
	 * @return
	 */
	private ArrayList<String> getCheckedFileList() {
		ArrayList<String> ret = new ArrayList<String>();
		for (int n = 0; n < mCheckState.length; ++n) {
			if (mCheckState[n]) {
				ret.add(mFileNameList.get(n));
			}
		}

		return ret;
	}

	
	
	/**
	 * 複数選択モードに移行
	 * 
	 * @return
	 */
	private boolean enterMultiMode() {
		if (mCheckState == null)return false;
		if (mMultiMode)return false;

		mCheckState = new boolean[mFileNameList.size()];
		mMultiMode = true;
		mStage.invalidate();

		mTxtMultiFavorite.setText(getString(R.string.image_menu_multiple_favorite));
		mTxtMultiSecret.setText(getString(R.string.image_menu_multiple_seclet));
		
		return true;
	}

	/**
	 * 複数選択モードを出る
	 * 
	 * @return
	 */
	private boolean leaveMultiMode() {
		if (mCheckState == null)return false;
		if (!mMultiMode)return false;

		mMultiMode = false;
		mStage.invalidate();
		return true;
	}

	public class MyOverlayDataSource implements UXGridWidget.OverlayDataSource {
		@Override
		public int getOverlayNumber(int itemPosition, int overlayId) {

			//チェック状態オーバーレイ
			if (overlayId == mOverlayCheckId) {
				if (!mMultiMode || mCheckState == null|| mCheckState.length < itemPosition) {
					return -1;
				}
				if (mCheckState[itemPosition]) {
					return 1;
				} else {
					return 0;
				}
			}

			String path = null;
			
			if((path = mCachePath.get(mFileNameList.get(itemPosition))) == null){
				File file = new File(mFileNameList.get(itemPosition));
	
				// キャッシュ登録には、シークレット拡張子を解除したパスを使用
				path = ExtUtil.unSecret(file).getPath();
				mCachePath.put(mFileNameList.get(itemPosition), path);
			}

			boolean isCashFavorite = false;
			boolean isCashSecret = false;
			boolean isCashVideo = false;
			
			// お気に入りキャッシュチェック
			if (overlayId == mOverlayFavId) {
				if (mCashFavorite.containsKey(path)) {
					if (mCashFavorite.get(path)) {
						return 0;
					}

					isCashFavorite = true;

				} else {
					mCashFavorite.put(path, false);
				}
			}

			// シークレットキャッシュチェック
			if (overlayId == mOverlaySecId) {
				if (mCashSecret.containsKey(path)) {
					if (mCashSecret.get(path)) {
						return 0;
					}

					isCashSecret = true;

				} else {
					mCashSecret.put(path, false);
				}
			}
			
			// ビデオキャッシュチェック
			if(overlayId == mOverlayVideoId){
				if(mCashSecret.containsKey(path)){
					if(mCashVideo.get(path)){
						return 0;
					}

					isCashVideo = true;
					
				} else {
					mCashVideo.put(path, false);
				}
			}

			// キャッシュに登録されていて、お気に入りでもシークレットでも動画でもない場合、オーバーレイ不要
			if (isCashFavorite || isCashSecret || isCashVideo)return -1;

			// 以下、キャッシュ未登録時処理
			// お気に入り画像
			File file = new File(mFileNameList.get(itemPosition));
			boolean fav = FavoriteUtil.isFavorite(file);
			boolean secret = ExtUtil.isSecret(file);
			boolean video = ExtUtil.isVideo(file.getPath()) || isSeacretVideo(file);
			mCashFavorite.put(path, fav);
			mCashSecret.put(path, secret);
			mCashVideo.put(path, video);
			
			if (fav && overlayId == mOverlayFavId) {
				return 0; // favItem[]
				// シークレット画像
			} else if (secret && overlayId == mOverlaySecId) {
				return 0; // secItem[]
			
			} else if(video && overlayId == mOverlayVideoId){
				return 0;

				// オーバレイ不要
			} else {
				return -1;
			}
		}
	}

	/**
	 * お気に入り、シークレットのキャッシュを削除します
	 * 
	 * @param pathList 削除パスリスト
	 * @param removeFavorite お気に入り削除フラグ
	 * @param removeSecret シークレット削除フラグ
	 */
	private void removeCash(List<String> pathList, boolean removeFavorite,boolean removeSecret) {

		for (String path : pathList) {

			if (path == null) continue;
			
			String cachPath = ExtUtil.unSecret(new File(path)).getPath();
			
			if (removeFavorite)mCashFavorite.remove(cachPath);
			if (removeSecret)mCashSecret.remove(cachPath);
		}
	}

	/**
	 * 表示画像が目に見える形の変更をされた際にリスキャンをかける。
	 * @param comp ソート条件（気にしない場合はnullを指定)
	 */
	protected void rescanMedia(Comparator<File> comp) {
				
		// グリッド書き換え（かならずlockStaceを経由する）
		mStage.lockStage(new Runnable() {

			@Override
			public void run() {
				mFileNameList.clear();

				// 行数新規取得
				getColumnCount();

				mGrid.column(mColumnCount);
			}
		});
		if (mScanner != null)mScanner.dispose();
		mScanner = mScanner.newWithSameSetting();

		if (comp != null)mScanner.sort(comp);

		// 隠しフォルダ表示状態
		boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this,ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this,ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, true);

		// 隠しフォルダ・シークレットの検索条件を再設定
		mScanner.scanNomedia(hiddenState).scanSecret(secretState);

		mScanner.findMedia(mScanListener);

		mGrid.invalidateData();

		View view = getWindow().getDecorView();
		changeActivity(view);
	}

	private boolean mAttachContext2Options;
	private int mContextItemNumber;

	private static final int REQUEST_SELECT_FOLDER_FOR_MOVE_SINGLE = 1;
	private static final int REQUEST_SELECT_FOLDER_FOR_MOVE_MULTI = 10;
	private static final int REQUEST_SELECT_FOLDER_FOR_COPY_SINGLE = 2;
	private static final int REQUEST_SELECT_FOLDER_FOR_COPY_MULTI = 20;
	private static final int REQUEST_WALL = 3;
	private static final int REQUEST_ROTATE = 4;
	private static final int REQUEST_SEARCH = 5;
	private static final int REQUEST_FULLSCREEN = 6;
	private static final int REQUEST_SETTING = 7;

	// 基本的なオプションメニュー
	private static final int MENU_ITEM_MULTI = 1;
	private static final int MENU_ITEM_SORT = 2;
	private static final int MENU_ITEM_SEARCH = 3;
	private static final int MENU_ITEM_SETTING = 4;

	// 複数選択メニュー
	private static final int MENU_MULTI_RELEASE = 10;
	private static final int MENU_MULTI_TAG = 11;
	private static final int MENU_MULTI_SHARE = 12;
	private static final int MENU_MULTI_DELETE = 13;
	private static final int MENU_MULTI_INFO = 14;
	private static final int MENU_MULTI_OTHER = 15;
	private static final int MENU_MULTI_MOVE = 16;
	private static final int MENU_MULTI_COPY = 17;
	private static final int MENU_MULTI_COPY_SYNC = 18;
	private static final int MENU_MULTI_FAVORITE = 19;
	private static final int MENU_MULTI_SECRET = 20;
	private static final int MENU_MULTI_DEFAULT = 21;

	// コンテキストメニュー
	private static final int CONTEXT_ITEM_TAG = 1;
	private static final int CONTEXT_ITEM_SHARE = 2;
	private static final int CONTEXT_ITEM_DELETE = 3;
	private static final int CONTEXT_ITEM_INFO = 4;
	private static final int CONTEXT_ITEM_MOVE = 5;
	private static final int CONTEXT_ITEM_COPY = 6;
	private static final int CONTEXT_ITEM_MOVE_SYNC = 7;
	private static final int CONTEXT_ITEM_COPY_SYNC = 8;
	private static final int CONTEXT_ITEM_FAVORITE = 9;
	private static final int CONTEXT_ITEM_UNSECRET = 10;
	private static final int CONTEXT_ITEM_SECRET = 11;
	private static final int CONTEXT_ITEM_RENAME = 12;
	private static final int CONTEXT_ITEM_ROTATE = 13;
	private static final int CONTEXT_ITEM_REGISTER = 14;

	private static final int CONTEXT_ITEM_ROTATE_90 = 16;
	private static final int CONTEXT_ITEM_ROTATE_180 = 17;
	private static final int CONTEXT_ITEM_ROTATE_270 = 18;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, CONTEXT_ITEM_TAG, 0, getString(R.string.image_context_tag))
				.setIcon(R.drawable.ic_tag);
		menu.add(0, CONTEXT_ITEM_SHARE, Menu.NONE, R.string.image_context_share).setIcon(R.drawable.ic_share);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		menu.clear();

		// 画像長押しの場合
		if (mAttachContext2Options) {

			// 選択ファイルとパスを取得
			File f = new File(mFileNameList.get(mContextItemNumber));
			mTargetPath = f.getPath();
	
			String checkString;
	
			if (ExtUtil.isSecret(f)) {
				checkString = ExtUtil.unSecret(f).getPath();
			} else {
				checkString = mTargetPath;
			}

			// 動画
			if (ExtUtil.isVideo(checkString)) {
				// Video
				menu.add(Menu.NONE, CONTEXT_ITEM_TAG, Menu.NONE,getString(R.string.image_context_tag)).setIcon(R.drawable.ic_tag);
				menu.add(Menu.NONE, CONTEXT_ITEM_SHARE, Menu.NONE,getString(R.string.image_context_share)).setIcon(R.drawable.ic_share);
				menu.add(Menu.NONE, CONTEXT_ITEM_INFO, Menu.NONE,getString(R.string.image_context_info)).setIcon(R.drawable.ic_detail);
				menu.add(Menu.NONE, CONTEXT_ITEM_MOVE, Menu.NONE,getString(R.string.image_context_move)).setIcon(R.drawable.ic_move);
				menu.add(Menu.NONE, CONTEXT_ITEM_COPY, Menu.NONE,getString(R.string.image_context_copy)).setIcon(R.drawable.ic_copy);

				menu.add(Menu.NONE, CONTEXT_ITEM_DELETE, Menu.NONE,getString(R.string.image_context_delete)).setIcon(R.drawable.ic_delete);
				menu.add(Menu.NONE, CONTEXT_ITEM_MOVE_SYNC, Menu.NONE,getString(R.string.image_context_move_sync));
				// お気に入り 状態によって表示項目を変更
				menu.add(Menu.NONE,CONTEXT_ITEM_FAVORITE,Menu.NONE,
						getString((FavoriteUtil.isFavorite(f)) ? 
								R.string.image_context_unregister_favorite: 
								R.string.image_context_register_favorite));
				if (ExtUtil.isSecret(f)) {
					menu.add(Menu.NONE, CONTEXT_ITEM_SECRET, Menu.NONE,getString(R.string.image_context_unsecret));
				} else {
					menu.add(Menu.NONE, CONTEXT_ITEM_SECRET, Menu.NONE,getString(R.string.image_context_to_secret));
				}
				menu.add(Menu.NONE, CONTEXT_ITEM_RENAME, Menu.NONE,R.string.menu_rename);
			}

			// 画像
			else {

				menu.add(Menu.NONE, CONTEXT_ITEM_TAG, Menu.NONE,getString(R.string.image_context_tag)).setIcon(R.drawable.ic_tag);
				menu.add(Menu.NONE, CONTEXT_ITEM_SHARE, Menu.NONE,getString(R.string.image_context_share)).setIcon(R.drawable.ic_share);
				menu.add(Menu.NONE, CONTEXT_ITEM_DELETE, Menu.NONE,getString(R.string.image_context_delete)).setIcon(R.drawable.ic_delete);
				menu.add(Menu.NONE, CONTEXT_ITEM_INFO, Menu.NONE,getString(R.string.image_context_info)).setIcon(R.drawable.ic_detail);
				menu.add(Menu.NONE, CONTEXT_ITEM_MOVE, Menu.NONE,getString(R.string.image_context_move)).setIcon(R.drawable.ic_move);

				menu.add(Menu.NONE, CONTEXT_ITEM_COPY, Menu.NONE,getString(R.string.image_context_copy));
				menu.add(Menu.NONE, CONTEXT_ITEM_MOVE_SYNC, Menu.NONE,getString(R.string.image_context_move_sync));

				// お気に入り 状態によって表示項目を変更
				menu.add(Menu.NONE,CONTEXT_ITEM_FAVORITE,Menu.NONE,
						getString((FavoriteUtil.isFavorite(f)) ? 
								R.string.image_context_unregister_favorite: 
								R.string.image_context_register_favorite));

				// シークレット
				if (ExtUtil.isSecret(f)) {
					menu.add(Menu.NONE, CONTEXT_ITEM_SECRET, Menu.NONE,getString(R.string.image_context_unsecret));
				} else {
					menu.add(Menu.NONE, CONTEXT_ITEM_SECRET, Menu.NONE,getString(R.string.image_context_to_secret));
				}

				menu.add(Menu.NONE, CONTEXT_ITEM_RENAME, Menu.NONE,R.string.menu_rename);
				// jpeg時のみローテートメニューを表示
				// menu.add(Menu.NONE, CONTEXT_ITEM_ROTATE, Menu.NONE,getString(R.string.image_context_rotate));

				// jpeg時のみローテートメニューを表示
				if (MediaUtil.getMimeTypeFromPath(checkString).equals("image/jpeg")) {
					SubMenu rotate = menu.addSubMenu(getString(R.string.image_context_rotate));
					rotate.add(Menu.NONE, CONTEXT_ITEM_ROTATE_90, Menu.NONE,getString(R.string.image_context_rotate_90));
					rotate.add(Menu.NONE, CONTEXT_ITEM_ROTATE_180, Menu.NONE,getString(R.string.image_context_rotate_180));
					rotate.add(Menu.NONE, CONTEXT_ITEM_ROTATE_270, Menu.NONE,getString(R.string.image_context_rotate_270));
				}

				if(!ExtUtil.isSecret(f)){
					menu.add(Menu.NONE, CONTEXT_ITEM_REGISTER, Menu.NONE,getString(R.string.image_context_register));
				}
			}

			// オプションメニュー
		} else {
			// 複数選択の場合
			// if (mMultiMode) {
			// 簡易メニュー表示
			// if (!mMultiIsOther) {
			// menu.add(0, MENU_MULTI_RELEASE, 0,"複数選択解除").setIcon(R.drawable.ic_multi_release);
			// menu.add(0, MENU_MULTI_TAG, 0,"タグ編集").setIcon(R.drawable.ic_tag);
			// menu.add(0, MENU_MULTI_SHARE, 0,"共有").setIcon(R.drawable.ic_share);
			// menu.add(0, MENU_MULTI_DELETE, 0,"削除").setIcon(R.drawable.ic_delete);
			// menu.add(0, MENU_MULTI_INFO, 0,"詳細").setIcon(R.drawable.ic_detail);
			// menu.add(0, MENU_MULTI_OTHER, 0,"その他").setIcon(R.drawable.other);
			//
			// // その他メニュー表示
			// } else {
			// menu.add(0, MENU_MULTI_MOVE, 0,"移動").setIcon(R.drawable.ic_move);
			// menu.add(0, MENU_MULTI_COPY, 0,"コピー").setIcon(R.drawable.ic_copy);
			// menu.add(0, MENU_MULTI_COPY_SYNC, 0,"同期フォルダに移動").setIcon(R.drawable.ic_update);
			// menu.add(0, MENU_MULTI_FAVORITE, 0,"お気に入り登録").setIcon(R.drawable.ic_multi_favorite);
			// menu.add(0, MENU_MULTI_SECRET, 0,"シークレット化").setIcon(R.drawable.ic_multi_secret);
			// menu.add(0, MENU_MULTI_DEFAULT, 0,"簡易メニュー").setIcon(R.drawable.other);
			//
			// }

			// 基本的なオプションメニュー
			// } else {
			// ローカル、タグから遷移した際のみ、複数選択を表示
			if (mParentName != null&& (PARENT_LOCAL.equals(mParentName) || PARENT_TAG.equals(mParentName))) {
				menu.add(0, MENU_ITEM_MULTI, 0,getString(R.string.image_menu_multiple_select)).setIcon(R.drawable.ic_multiple_selection);
			}

			// ローカルから遷移した際のみ、ソート項目を表示
			if (mParentName != null && PARENT_LOCAL.equals(mParentName)) {
				menu.add(0, MENU_ITEM_SORT, Menu.NONE, R.string.menu_sort).setIcon(R.drawable.ic_sort);
			}

			menu.add(0, MENU_ITEM_SEARCH, Menu.NONE, R.string.menu_search).setIcon(R.drawable.ic_search);
			menu.add(0, MENU_ITEM_SETTING, Menu.NONE, R.string.menu_setting).setIcon(R.drawable.ic_setting);
			// }
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// 画像長押しの場合
		if (mAttachContext2Options) {
			switch (item.getItemId()) {
			// 移動
			case CONTEXT_ITEM_MOVE:
				Intent moveIntent = new Intent(getApplicationContext(),SelectFolderActivity.class);
				moveIntent.putExtra(SelectFolderActivity.PARAM_TITLE,getString(R.string.folder_title_select));
				moveIntent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
				moveIntent.putExtra(SelectFolderActivity.PARAM_START_PATH,
						getIntent().getStringExtra(INTENT_TARGET));
				startActivityForResult(moveIntent,REQUEST_SELECT_FOLDER_FOR_MOVE_SINGLE);
				break;
			// コピー
			case CONTEXT_ITEM_COPY:
				Intent copyIntent = new Intent(getApplicationContext(),SelectFolderActivity.class);
				copyIntent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.folder_copy_title_select));
				copyIntent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
				copyIntent.putExtra(SelectFolderActivity.PARAM_START_PATH,getIntent().getStringExtra(INTENT_TARGET));
				startActivityForResult(copyIntent,REQUEST_SELECT_FOLDER_FOR_COPY_SINGLE);
				break;
			// 同期フォルダに移動(コピー)
			case CONTEXT_ITEM_MOVE_SYNC:
				ArrayList<String> path = new ArrayList<String>();
				path.add(mFileNameList.get(mContextItemNumber));
				File pathExternalPublicDir = new File(ApplicationDefine.PATH_JSPHOTO);
				if (!pathExternalPublicDir.exists())pathExternalPublicDir.mkdirs();
				String dir = pathExternalPublicDir.getPath();
				// 移動に成功したらToast表示
				if (mFileOp.copyToFolder(dir, path, 1, false)) {
//					Toast.makeText(getApplicationContext(),getString(R.string.image_context_move_sync_success),Toast.LENGTH_SHORT).show();
				}

				break;
			// タグ編集
			case CONTEXT_ITEM_TAG:
				Intent tagEditIntent = new Intent(getApplicationContext(),TagEditActivity.class);
				tagEditIntent.putExtra(ApplicationDefine.INTENT_PATH,mFileNameList.get(mContextItemNumber));
				startActivityForResult(tagEditIntent,ApplicationDefine.REQUEST_TAG);
				break;

			// お気に入り
			case CONTEXT_ITEM_FAVORITE:

				List<String> pathList = new ArrayList<String>();

				if (mMultiMode) {
					// TODO　複数選択側へ移動？
				} else {
					File fileFavorite = new File(mFileNameList.get(mContextItemNumber));
					if (FavoriteUtil.isFavorite(fileFavorite)) {
						FavoriteUtil.removeFavorite(fileFavorite);
					} else {
						FavoriteUtil.addFavorite(fileFavorite);
					}
					// 双方向同期
					startSync();

					pathList.add(mFileNameList.get(mContextItemNumber));
				}

				// キャッシュから削除
				removeCash(pathList, true, false);

				rescanMedia(null);
				break;

			// シークレット
			case CONTEXT_ITEM_SECRET:
				File fileSecret = new File(mFileNameList.get(mContextItemNumber));
				if (ExtUtil.isSecret(fileSecret)) {
					showDialog(DIALOG_NO_SECRET);
				} else {
					showDialog(DIALOG_SECRET);
				}
				break;

			// 共有
			case CONTEXT_ITEM_SHARE:
//				File f = new File(ApplicationDefine.INTENT_PATH,mFileNameList.get(mContextItemNumber));
				File f = new File(mFileNameList.get(mContextItemNumber));
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType(MediaUtil.getMimeTypeFromPath(f.getAbsolutePath()));
				i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
				startActivity(Intent.createChooser(i,getString(R.string.image_context_share)));
				
				isRescan = false;
				
				break;
			// 削除
			case CONTEXT_ITEM_DELETE:
				new AlertDialog.Builder(NewGridActivity.this.getParent())
						.setTitle(android.R.string.dialog_alert_title)
						.setMessage(R.string.confirm_delete)
						.setPositiveButton(android.R.string.cancel, null)
						.setNegativeButton(android.R.string.ok,new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface arg0,int arg1) {
								// 指定した1ファイルの場合
								List<String> target = new ArrayList<String>();
								target.add(mFileNameList.get(mContextItemNumber));
								// List<String> target =mFileNameList.get(mContextItemNumber);
								mFileOp.deleteFiles(target,target.size());

								// キャッシュ削除
								removeCash(target, true, true);
								
								// 一覧を更新
								mGrid.invalidateData();
							}
						})
						.show();

				break;
			// 名前変更
			case CONTEXT_ITEM_RENAME:
				final File renameFile = new File(mFileNameList.get(mContextItemNumber));
				final SimpleEditDialog dialog = new SimpleEditDialog(NewGridActivity.this.getParent());
				dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				// 文字数制限(FAT-32は255文字まで、 255 - .jpeg.secret(12文字) = 243)
				int maxLength = 243;

				dialog.mTxtEdit.setText(ExtUtil.getPureName(renameFile));
				dialog.mTxtEdit.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxLength) });
				dialog.mTxtTitle.setText(R.string.menu_rename);
				dialog.mTxtView.setVisibility(View.GONE);
				
				dialog.mBtnOk.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						
						String rename = dialog.mTxtEdit.getText().toString().trim();
						rename = rename.replaceAll("[　|\\s]+", "");
						// FIXME 255文字以内なのに、androidで見えなくなってしまう。暫定対応として100文字制限をかけます。
						if (rename.length() == 0 || rename.length() > 100) {
							Toast.makeText(NewGridActivity.this,R.string.toast_failed_change_name,Toast.LENGTH_SHORT).show();
							return;
						}

						// File renameFile = new File(mFileNameList.get(mContextItemNumber));
						File to = new File(renameFile.getParentFile(), rename + ExtUtil.getExtWithSecret(renameFile));

						File checkSub;
						
						// チェック用ファイル作成
						if (ExtUtil.isSecret(to)) {
							checkSub = ExtUtil.unSecret(to);
						} else {
							checkSub = ExtUtil.toSecret(to);
						}
						
						// 変更後の名前が既に存在している場合
						if (to.exists() || checkSub.exists()) {
							
							to = ExtUtil.createEmptyPathConsideringSecret(to,"");
						}

						if (renameFile.renameTo(to)) {
							SQLiteDatabase dbExternal = OpenHelper.external.getDatabase();
							SQLiteDatabase dbCache = OpenHelper.cache.getDatabase();
							MediaMetaDataAccessor.updateMetaDataName(dbExternal,renameFile.getParent(),renameFile.getName(),to.getName());
							MediaIndexesAccessor.updateIndexesName(dbCache,renameFile.getParent(),renameFile.getName(),to.getName());

							List<String> pathList = new ArrayList<String>();
							pathList.add(renameFile.getPath());

							// キャッシュから削除
							removeCash(pathList, true, true);

							// グリッド書き換え（かならずlockStaceを経由する）
							mStage.lockStage(new Runnable() {

								@Override
								public void run() {
									mFileNameList.remove(mContextItemNumber);
								}
							});
							
							MediaStoreOperation.scanAndDeleteMediaStoreEntry(getApplicationContext(), renameFile, to, false);
							rescanMedia(null);
						} else {
							Toast.makeText(NewGridActivity.this,R.string.toast_failed_change_name,Toast.LENGTH_SHORT).show();
						}
						dialog.dismiss();
					}
				});
//				dialog.mTxtEdit.addTextChangedListener(dialog.mTextWatcher);
				dialog.mBtnChansel.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View view) {
						dialog.dismiss();
					}
				});
				
				dialog.show();

				break;
			// 詳細
			case CONTEXT_ITEM_INFO:
				ScrollView scrollview = new ScrollView(this);
				scrollview.setLayoutParams(new ViewGroup.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
				scrollview.addView(new ExifView(this, mFileNameList.get(mContextItemNumber)));
				
				new AlertDialog.Builder(NewGridActivity.this.getParent())
						.setTitle(getString(R.string.image_context_info))
						.setIcon(android.R.drawable.ic_dialog_info)
						.setView(scrollview)
						.setPositiveButton(android.R.string.ok, null)
						.show();
				// mGrid.invalidateData();
				break;
			// 壁紙に登録
			case CONTEXT_ITEM_REGISTER:
				mWallpaper = new WallpaperHelper();
				mWallpaper.startCropActivity(NewGridActivity.this.getParent(),mFileNameList.get(mContextItemNumber), REQUEST_WALL);
				break;

			// 90度回転
			case CONTEXT_ITEM_ROTATE_90:
				startRotate(ImageOpActivity.ROTATE_90);
				break;

			// 180度回転
			case CONTEXT_ITEM_ROTATE_180:
				startRotate(ImageOpActivity.ROTATE_180);
				break;

			// 270度回転
			case CONTEXT_ITEM_ROTATE_270:
				startRotate(ImageOpActivity.ROTATE_270);
				break;
			}

		} else {
			// メニュー項目選択時
			switch (item.getItemId()) {
			// 複数選択
			case MENU_ITEM_MULTI:
				// 画像一覧に複数選択用のチェックボックスを表示する処理 TODO
				if (!enterMultiMode()) {
					leaveMultiMode();
				}

				// オプションメニューを表示
				mMultiMenu1.setVisibility(View.VISIBLE);

				break;

			// 複数選択解除
			case MENU_MULTI_RELEASE:
				mMultiMode = false;
				closeOptionsMenu();

				break;
			// タグの編集
			// 共有
			// 削除
			// 詳細
			// その他
			case MENU_MULTI_OTHER:

				// オプションメニュー2を表示
				mMultiMenu1.setVisibility(View.GONE);
				mMultiMenu2.setVisibility(View.VISIBLE);

				break;

			// 移動
			// コピー
			// 同期フォルダに移動
			// お気に入り登録
			// シークレット化
			// 簡易メニュー
			case MENU_MULTI_DEFAULT:

				// オプションメニュー1を表示
				mMultiMenu1.setVisibility(View.VISIBLE);
				mMultiMenu2.setVisibility(View.GONE);

				break;

			// 表示順序 TODO
			case MENU_ITEM_SORT:
				new AlertDialog.Builder(this.getParent())
					.setTitle(getString(R.string.menu_sort))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setItems(getResources().getStringArray(R.array.media_sort), mSetSortListener).show();
				break;
			// 検索
			case MENU_ITEM_SEARCH: {
				Intent intent = new Intent(NewGridActivity.this,SearchActivity.class);
				startActivity(intent);
				break;
			}
			// 設定
			case MENU_ITEM_SETTING:
				Intent intent = new Intent(NewGridActivity.this,JorllePrefsActivity.class);
				startActivityForResult(intent, REQUEST_SETTING);
				break;
			}

		}
		return true;
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		mAttachContext2Options = false;
		super.onOptionsMenuClosed(menu);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		//onResumeでリスキャンをしない
		isRescan = false;
		
		// TODO
		switch (requestCode) {
		// 移動
		case REQUEST_SELECT_FOLDER_FOR_MOVE_SINGLE:
			if (resultCode == RESULT_OK) {

				String newPath = data.getStringExtra(SelectFolderActivity.RESULT_PATH);

				String target = getIntent().getStringExtra(INTENT_TARGET);

				// 移動先が別のパスであれば、ファイルを移動
				if (target == null || (!target.equals(newPath))) {

					List<String> pathList = new ArrayList<String>();
					
					// タグ一覧などから遷移した場合は、リストを取得
					if (target == null) {
						pathList = getCheckedFileList(); 
					} else {
						
						File targetFile = new File(target);
						
						if (targetFile.exists()) {
							pathList.add(target);
						} else {
							pathList = getCheckedFileList();
						}
					}
					
					// キャッシュを削除
					removeCash(pathList, true, true);
					
					List<String> moveSource = 
							Arrays.asList(new String[] { mFileNameList.get(mContextItemNumber) });
					mFileOp.moveToFolder(
							data.getStringExtra(SelectFolderActivity.RESULT_PATH),moveSource, moveSource.size());

					//TODO 暫定処理　タグ一覧からの複数選択時は、チェック状態をクリア
					if (!mIsLocalGrid && mCheckState != null) {
						mCheckState = new boolean[mFileNameList.size()];
					}
					
					// グリッド書き換え（かならずlockStaceを経由する）
					mStage.lockStage(new Runnable() {

						@Override
						public void run() {
							if (mIsLocalGrid) mFileNameList.remove(mContextItemNumber);
						}
					});

					mGrid.invalidateData();
				}
			}
			break;
		// 移動(複数)
		case REQUEST_SELECT_FOLDER_FOR_MOVE_MULTI:
			if (resultCode == RESULT_OK) {
				String newPath = data.getStringExtra(SelectFolderActivity.RESULT_PATH);
				String target = getIntent().getStringExtra(INTENT_TARGET);
				// 移動先が別のパスであれば、ファイルを移動
				if (target == null || (!target.equals(newPath))) {

					List<String> pathList = new ArrayList<String>();
					
					// タグリストなどから遷移した場合は、リスト取得
					if (target == null) {
						pathList = getCheckedFileList(); 
					} else {
						
						File targetFile = new File(target);
						
						if (targetFile.exists()) {
							pathList.add(target);
						} else {
							pathList = getCheckedFileList();
						}
					}
					
					// キャッシュを削除
					removeCash(pathList, true, true);
					
					ArrayList<String> moveSource = getCheckedFileList();
					mFileOp.moveToFolder(
						data.getStringExtra(SelectFolderActivity.RESULT_PATH), moveSource, moveSource.size());
					
					//TODO 暫定処理　タグ一覧からの複数選択時は、チェック状態をクリア
					if (!mIsLocalGrid && mCheckState != null) {
						mCheckState = new boolean[mFileNameList.size()];
					}
					
					// グリッド書き換え（かならずlockStaceを経由する）
					mStage.lockStage(new Runnable() {
						
						@Override
						public void run() {
							if (mIsLocalGrid) mFileNameList.remove(mContextItemNumber);
						}
					});
					
					mGrid.invalidateData();
				}
			}
			
			break;
		// コピー
		case REQUEST_SELECT_FOLDER_FOR_COPY_SINGLE:
			if (resultCode == RESULT_OK) {

				Bundle bundle = data.getExtras();
				String path = bundle.getString(SelectFolderActivity.PARAM_START_PATH);

				List<String> copySource = 
						Arrays.asList(new String[] { mFileNameList.get(mContextItemNumber) });
				mFileOp.copyToFolder(path, copySource, copySource.size(), true);
			}
			break;
		// コピー(複数)
		case REQUEST_SELECT_FOLDER_FOR_COPY_MULTI:
			if (resultCode == RESULT_OK) {
				
				Bundle bundle = data.getExtras();
				String path = bundle.getString(SelectFolderActivity.PARAM_START_PATH);
				
				List<String> copySource =	getCheckedFileList();
				mFileOp.copyToFolder(path, copySource, copySource.size(), true);
			}
			break;
		// 壁紙変更
		case REQUEST_WALL:
			mWallpaper.onActivityResult(this, resultCode);
			break;

		// 回転
		case REQUEST_ROTATE: {
			File media = new File(mTargetPath);
			OpenHelper.cache.getDatabase().delete(ImageCache.$TABLE, 
					ImageCache.DIRPATH + " = ? AND " + ImageCache.NAME + " = ?", 
					new String[] {media.getParent(), media.getName() });

			MediaUtil.scanMedia(getApplicationContext(), media, false);
			rescanMedia(null);
			break;
		}

		// // インフォリンクチュートリアルからタブ切替が指定された際
		// case REQUEST_SEARCH :
		//
		// // タブを取得
		// TabHost tabHost = ((TabActivity) ((LocalNavigationGroup)getParent()).getParent()).getTabHost();
		//
		// // 画面を切り替える
		// if (resultCode == ApplicationDefine.RESULT_TUTORIALINFOLINK_ONLINE){
		// // オンライン
		// tabHost.setCurrentTabByTag(ApplicationDefine.TAB_ONLINE);
		// } else if (resultCode == ApplicationDefine.RESULT_TUTORIALINFOLINK_AUTOALBUM){
		// // オートアルバム
		// tabHost.setCurrentTabByTag(ApplicationDefine.TAB_AUTO);
		// } else if (resultCode == ApplicationDefine.RESULT_TUTORIALINFOLINK_SEARCH){
		// // 検索
		// Intent intent = new Intent(NewGridActivity.this, SearchActivity.class);
		// startActivity(intent);
		// }
		// break;

		// タグ
		case ApplicationDefine.REQUEST_TAG:
			if(resultCode == RESULT_OK){
				rescanMedia(null);
			}
			break;

		// 設定画面から復帰
		case REQUEST_SETTING:

			// グリッド書き換え（かならずlockStaceを経由する）
			mStage.lockStage(new Runnable() {

				@Override
				public void run() {
					mFileNameList.clear();
				}
			});

			init();

			// rescanMedia(null);
			break;

		// 全画面
		case REQUEST_FULLSCREEN:

			ArrayList<String> listChangePath = null;
			
			// ファイル操作があった場合は、変更前のキャッシュを削除
			if (data != null) {
				listChangePath = data.getStringArrayListExtra(FullScreenActivity.INTENT_CHANGE);
			}
			
			if (listChangePath != null && listChangePath.size() > 0) {
				removeCash(listChangePath, true, true);
			}
			
			rescanMedia(null);
			break;
			
		}

		// 一覧を更新
		mGrid.invalidateData();
	}

	/** ソート設定リスナー */
	DialogInterface.OnClickListener mSetSortListener = new DialogInterface.OnClickListener() {

		public void onClick(DialogInterface dialog, int which) {

			Comparator<File> mComparatorMedia = null;

			switch (which) {
			// 名前昇順
			case SORT_NAME_ASC:
				mSortAlgorithm = SORT_NAME_ASC;
				mComparatorMedia = new JorlleMediaScanner.NameAscender();
				break;

			// 名前降順
			case SORT_NAME_DESC:
				mSortAlgorithm = SORT_NAME_DESC;
				mComparatorMedia = new JorlleMediaScanner.NameDescender();
				break;

			// 日付昇順
			case SORT_DATE_ASC:
				mSortAlgorithm = SORT_DATE_ASC;
				mComparatorMedia = new JorlleMediaScanner.DateAscender();
				break;

			// 日付降順
			case SORT_DATE_DESC:
				mSortAlgorithm = SORT_DATE_DESC;
				mComparatorMedia = new JorlleMediaScanner.DateDescender();
				break;
			}

			rescanMedia(mComparatorMedia);
		}
	};

	/**
	 * 画像のシークレット状態を操作します
	 * 
	 * @param isSecret true:シークレットに設定 false:シークレット解除
	 */
	private boolean setSecret(boolean isSecret) {

		boolean result = true;
		
		try {
			// 複数選択リストを取得
			ArrayList<String> checkList = getCheckedFileList();
	
			// シークレット書き換え
			List<String> resultList = MediaUtil.setSecret(mDatabase, isSecret, mTargetPath, mMultiMode,checkList);
			
			if(isSecret){
				if(mMultiMode){
					for(String path: checkList){
						MediaStoreOperation.deleteMediaStoreEntry(getApplicationContext(), new File(path));
					}
				}else{
					MediaStoreOperation.deleteMediaStoreEntry(getApplicationContext(), new File(mTargetPath));
				}
			}else{
				new ScanMediaTask().execute(resultList);
			}
	
			// 以前のファイル名の登録を削除
			removeCash(resultList, true, true);
			
		} catch (Exception e) {
//			e.printStackTrace();		/*$debug$*/
			result = false;
		}
		
		return result;
	}
	
	private class ScanMediaTask extends AsyncTask<List<String>, Void, Void>{

		@Override
		protected Void doInBackground(List<String>... params) {
			List<String> fileList = params[0];
			for(String name: fileList){
				MediaUtil.scanMedia(getApplicationContext(), new File(name), true);
			}
			return null;
		}
	
	}

	/**
	 * 複数選択時のシークレット処理の内容を判定します
	 * 
	 * @return true:複数シークレット解除 false:複数シークレット化
	 */
	private boolean checkSecret(List<String> items){
		for(String item: items){
			if(!ExtUtil.isSecret(new File(item))){
				return false;
			}
		}
		return true;
	}

	/**
	 * 選択されたファイルのお気に入り状態を操作します
	 * 
	 * @param isFavorite true:お気に入り登録 false:お気に入り解除
	 */
	private boolean setFavorite(boolean isFavorite) {

		boolean result = true;
		
		try {
			// 複数選択リストを取得
			ArrayList<String> checkList = getCheckedFileList();
	
			int size = checkList.size();
	
			// お気に入り登録・解除
			for (int i = 0; i < size; i++) {
	
				File file = new File(checkList.get(i));
	
				if (isFavorite) {
					if (!FavoriteUtil.isFavorite(file))FavoriteUtil.addFavorite(file);
				} else {
					FavoriteUtil.removeFavorite(file);
				}
			}
	
			// お気に入りキャッシュのみ削除
			removeCash(checkList, true, false);
			
			// 双方向同期
			startSync();
			
		} catch (Exception e) {
//			e.printStackTrace();		/*$debug$*/
			result = false;
		}
		
		return result;
	}
	
	private void startSync() {
		Map<String, Map<String, SyncSetting>> settings =
				MediaSyncManagerV2.loadSyncSettings(this);
		for (String service : settings.keySet()) {
			Map<String, SyncSetting> accounts = settings.get(service);
			if (!accounts.isEmpty()) {
				Long interval = accounts.values().iterator().next().interval;
				if (interval == null || 0 < interval) {
					MediaSyncManagerV2.startSyncMedia(this, null);
				}
			}
		}
	}

	/**
	 * 複数選択時のお気に入り処理の内容を判定します
	 * 
	 * @return true:お気に入り解除 false:お気に入り登録
	 */
	private boolean checkFavorite(List<String> items){
		for(String item: items){
			if(!FavoriteUtil.isFavorite(new File(item))){
				return false;
			}
		}
		return true;
	}

	/**
	 * 画像を回転します
	 * 
	 * @param tag 回転角度
	 */
	private void startRotate(int tag) {
		Intent i = new Intent(getApplicationContext(), ImageOpActivity.class);
		i.putExtra(ImageOpActivity.INTENT_ROTATE, true);
		i.putExtra(ImageOpActivity.INTENT_ROTATE_ORIENTATION, tag);
		i.putExtra(ImageOpActivity.INTENT_TARGET_PATH, mTargetPath);
		i.putExtra(ImageOpActivity.INTENT_TITLE,getString(R.string.image_context_rotate));

		startActivityForResult(i, REQUEST_ROTATE);
	}

	@Override
	protected void onPause() {
		super.onPause();

		mStage.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		mStage.onResume();

		mHeaderController.show(3000);

		//カメラ画面から戻った際に再読み込みする
		if(isRescan){
			rescanMedia(null);

		}else{
			isRescan = true;
	}
	}
	private void onItemTap(final int itemNumber) {

		//アイテムが存在しない際は処理をしない
		if(mFileNameList.size() < itemNumber){
			return;
		}
		
		if (mMultiMode) {
			if (mCheckState != null && mCheckState.length > itemNumber) {

				mStage.lockStage(new Runnable() {

					@Override
					public void run() {
						mCheckState[itemNumber] = !mCheckState[itemNumber];

						List<String> list = getCheckedFileList();
						
						int size = list.size();
						
						if (size <= 0) {
							mTxtMultiSecret.setText(getString(R.string.image_menu_multiple_seclet));
						} else if (checkSecret(list)) {
							mTxtMultiSecret.setText(getString(R.string.image_menu_multiple_unseclet));
						} else {
							mTxtMultiSecret.setText(getString(R.string.image_menu_multiple_seclet));
						}
						
						
						if (size <= 0) {
							mTxtMultiFavorite.setText(getString(R.string.image_menu_multiple_favorite));
						} else if (checkFavorite(list)) {
							mTxtMultiFavorite.setText(getString(R.string.image_context_unregister_favorite));
						} else {
							mTxtMultiFavorite.setText(getString(R.string.image_menu_multiple_favorite));
						}
					}
				});
			}

			mStage.invalidate();

			return;
		}

		File f = new File(mFileNameList.get(itemNumber));
		if(isSeacretVideo(f)){
			Toast.makeText(this, getString(R.string.image_toast_message_secret_video), Toast.LENGTH_LONG).show();
			return;

		} else if (ExtUtil.isVideo(f.getPath())) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(new File(mFileNameList.get(itemNumber))),"video/*");
			startActivity(intent);
		
		} else {
			Intent i = new Intent(getApplicationContext(),FullScreenActivity.class);
			i.putExtra(FullScreenActivity.EXTRA_CATEGORY_NAME, mCategoryName);

			// 画像一覧の画像取得条件を、詳細画面に渡す
			Intent old = getIntent();

			switch (old.getIntExtra(INTENT_FOLDER_TYPE, FOLDER_TYPE_NONE)) {

			case FOLDER_TYPE_PATH:
				break;
			case FOLDER_TYPE_SECRET:
				i.putExtra(ApplicationDefine.INTENT_FOLDER_SECRET, true);
				i.putExtra(ApplicationDefine.INTENT_FOLDER_SUB, true);
				break;
			case FOLDER_TYPE_FAVORITE:
				i.putExtra(ApplicationDefine.INTENT_FOLDER_FAVORITE, true);
				i.putExtra(ApplicationDefine.INTENT_FOLDER_SUB, true);
				break;
			case FOLDER_TYPE_TAG:
				i.putExtra(ApplicationDefine.INTENT_FOLDER_TAG,old.getStringExtra(INTENT_TARGET));
				i.putExtra(ApplicationDefine.INTENT_FOLDER_SUB, true);
				break;
			case FOLDER_TYPE_TAG_LIST:
				List<String> tagList = new ArrayList<String>();
				for (String tag : old.getStringArrayExtra(INTENT_TARGET_LIST)) {
					tagList.add(tag);
				}
				i.putExtra(ApplicationDefine.INTENT_FOLDER_TAG_LIST,tagList.toArray());
				i.putExtra(ApplicationDefine.INTENT_FOLDER_SUB, true);
				break;
			default:
				throw new IllegalArgumentException("invalid INTENT_FOLDER_TYPE");
			}

			i.putStringArrayListExtra(FullScreenActivity.INTENT_FILE_PATH_LIST,mFileNameList);
			i.putExtra(FullScreenActivity.INTENT_INITIAL_POSITION, itemNumber);
			i.putExtra(ApplicationDefine.INTENT_FOLDER_PATH, mBaseFolder);
			i.putExtra(FullScreenActivity.INTENT_SORT, mSortAlgorithm);
			startActivityForResult(i, REQUEST_FULLSCREEN);
		}
	}
	
	/**
	 * シークレットビデオファイルならTRUE
	 * @param file
	 * @return
	 */
	private boolean isSeacretVideo(File file){
		if(ExtUtil.isSecret(file)){
			file = ExtUtil.unSecret(file);
			if(ExtUtil.isVideo(file.getPath())){
				return true;
			}
		}
		return false;
	}

	private UXGridDataSource mMyDataSource = new UXGridDataSource() {

		@Override
		public int getRotation(int item) {
			return 0;
		}

		@Override
		public Object getOverlayInfo(int item, int number) {
			return null;
		}

		@Override
		public int getItemCount() {
			return mFileNameList.size();
		}

		@Override
		public Object getInfo(int item) {
			return mFileNameList.get(item);
		}
	};

	private class AddFileRunnable implements Runnable {
		private File mFile;

		public void set(File f) {
			mFile = f;
		}

		@Override
		public void run() {
			mFileNameList.add(mFile.getAbsolutePath());
			mStage.invalidate();
		}
	}

	private JorlleMediaScanner.OnFoundListener mScanListener = new JorlleMediaScanner.OnFoundListener() {
		private AddFileRunnable mRunnable = new AddFileRunnable();

		@Override
		public void onStartFolder(File folder) {
		}

		@Override
		public void onFound(File file) {
			mRunnable.set(file);
			// 必ずlockStageを経由
			mStage.lockStage(mRunnable);
		}

		@Override
		public void onEndFolder(File folder, int size) {
		}

		@Override
		public void onComplete() {
			// メディアが存在しない場合は、前の画面に戻る
			if (mFileNameList.size() <= 0) {
				Toast.makeText(getParent(),getString(R.string.image_message_no_media),Toast.LENGTH_LONG).show();
				onBackRefleshHistory();
			}

			int size = mFileNameList.size();
			
			if(mCheckState == null || size != mCheckState.length) {
				mCheckState = new boolean[size];
			
			} else {
				if (size <= 0) {
					mTxtMultiSecret.setText(getString(R.string.image_menu_multiple_seclet));
				} else if (checkSecret(getCheckedFileList())) {
					mTxtMultiSecret.setText(getString(R.string.image_menu_multiple_unseclet));
				} else {
					mTxtMultiSecret.setText(getString(R.string.image_menu_multiple_seclet));
				}
				
				if (size <= 0) {
					mTxtMultiFavorite.setText(getString(R.string.image_menu_multiple_favorite));
				} else if (checkFavorite(getCheckedFileList())) {
					mTxtMultiFavorite.setText(getString(R.string.image_context_unregister_favorite));
				} else {
					mTxtMultiFavorite.setText(getString(R.string.image_menu_multiple_favorite));
				}
			}
			
		}
	};
	
	private void setBaseFolder(Intent intent, JorlleMediaScanner scanner) {
		base = intent.getStringExtra(INTENT_TARGET);
		if (base == null) {
			base = Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		scanner.baseFolder(base);
		mBaseFolder = base;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mHeaderController.onDestroy();

		mScanner.dispose();
		mStage.dispose();
	}

	private class TmpThumbnailLoader implements UXThumbnailLoader {
		@Override
		public boolean loadCachedThumbnail(Object info, int widthHint,UXImageInfo out) {
			return false;
		}

		@Override
		public boolean loadThumbnail(Object info, int widthHint, UXImageInfo out) {
			String path = (String) info;
			byte[] thumbnail = null;
			Bitmap thumbnailBitmap = null;
			Bitmap ret = null;

			try {
				ExifInterface exif = new ExifInterface(path);
				thumbnail = exif.getThumbnail();
			} catch (IOException e) {
			}

			if (thumbnail == null) {
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(path, opt);
				opt.inJustDecodeBounds = false;

				int divide = ((opt.outWidth < opt.outHeight) ? opt.outHeight: opt.outWidth);
				opt.inSampleSize = divide / WIDTH;
				if (opt.inSampleSize == 0)opt.inSampleSize = 1;

				thumbnailBitmap = BitmapFactory.decodeFile(path, opt);
			} else {
				thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnail, 0,thumbnail.length);
			}

			if (thumbnailBitmap == null) {
				return false;
			}

			float heightRate = (float) thumbnailBitmap.getHeight()/ (float) thumbnailBitmap.getWidth();
			int height = (int) (WIDTH * heightRate);

			ret = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.RGB_565);
			Canvas c = new Canvas(ret);
			c.drawBitmap(thumbnailBitmap,new Rect(0, 0, thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight()), 
					new Rect(0, 0, WIDTH, height),new Paint());
			thumbnailBitmap.recycle();

			out.bitmap = ret;
			return true;
		}

		@Override
		public void updateCachedThumbnail(Object info, int widthHint,UXImageInfo in) {
		}
	}

	/**
	 * 表示する列数を取得します
	 */
	private void getColumnCount() {

		Configuration config = getResources().getConfiguration();

		// 遷移元と画面向きによって、列数を変える
		if (mIsMoveTutorial) {

			if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mColumnCount = COLUMN_TUTORIAL_LANDSCAPE;
			} else {
				mColumnCount = COLUMN_TUTORIAL_PORTLAIT;
			}

		} else {

			if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mColumnCount = COLUMN_NORMAL_LANDSCAPE;
			} else {
				mColumnCount = COLUMN_NORMAL_PORTLAIT;
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {

		// シークレット設定ダイアログ
		case DIALOG_SECRET:

			return new AlertDialog.Builder(getParent())
					.setTitle(getString(R.string.folder_title_setting_secret_file))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setMessage(getString(R.string.folder_message_setting_secret_file))
					.setPositiveButton(getString(android.R.string.cancel), null)
					.setNegativeButton(android.R.string.ok,new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,int which) {
									// シークレットに設定
//									setSecret(true);
									SecretTask secretTask = new SecretTask(true);
									secretTask.execute();
								}
							})
					.show();

		// シークレット解除ダイアログ
		case DIALOG_NO_SECRET:
			return new AlertDialog.Builder(getParent())
					.setTitle(getString(R.string.folder_title_setting_no_secret))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setMessage(getString(R.string.folder_message_setting_no_secret_file))
					.setPositiveButton(getString(android.R.string.cancel), null)
					.setNegativeButton(android.R.string.ok,new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,int which) {
									String password = PreferenceUtil.getPreferenceValue(NewGridActivity.this,ApplicationDefine.KEY_SECRET_PASSWORD,null);

									// パスワード設定済みの場合は、パスワード確認ダイアログを表示
									if (password != null) {

										final SecretCheckPasswordDialog spd = new SecretCheckPasswordDialog(getParent());
										spd.setOnDismissListener(new OnDismissListener() {

											@Override
											public void onDismiss(DialogInterface dialog) {
												if (spd.mIsSetPassword) {
													// シークレットに設定
//													setSecret(false);
													SecretTask secretTask = new SecretTask(false);
													secretTask.execute();
												}
											}
										});
										spd.show();
										dialog.dismiss();
										// パスワード未設定の場合は、無条件で解除
									} else {
//										setSecret(false);
										SecretTask secretTask = new SecretTask(false);
										secretTask.execute();
									}
								}
							})
					.show();
		}

		return super.onCreateDialog(id);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// バックキー押下
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// 複数選択状態(オプション1が表示されている)の場合
			if (mMultiMode && mMultiMenu1.isShown()) {
				// オプションメニューを非表示にし
				mMultiMenu1.setVisibility(View.GONE);
				// 複数選択状態を解除
				leaveMultiMode();

				//viewPortの位置を調整
				mStage.moveViewPort((float)mMultiMenu1.getHeight());
				return true;

				// オプションメニュー2が表示されている場合
			} else if (mMultiMode && mMultiMenu2.isShown()) {
				// オプション2を非表示にし
				mMultiMenu2.setVisibility(View.GONE);
				// オプション1を表示(複数選択状態は維持)
				mMultiMenu1.setVisibility(View.VISIBLE);
				return true;

				// 複数選択では無い場合
			} else {
				onBackRefleshHistory();
			}
			return true;
			// メニューボタン押下
		} else if (keyCode == KeyEvent.KEYCODE_MENU) {
			// 複数選択モード中はデフォルトのメニューは出さない
			if (!mMultiMode) {
				openOptionsMenu();
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// 検索画面以外から遷移されていた場合は、縦横切り替え処理を行う
		if (!mIsFromSearch) {

			// グリッド書き換え（かならずlockStaceを経由する）
			mStage.lockStage(new Runnable() {

				@Override
				public void run() {
					mFileNameList.clear();
				}
			});

			mStage.dispose();

			boolean isOption1 = true;
			if (mMultiMode) {
				if (mMultiMenu1.isShown()) {
					isOption1 = true;
				} else {
					isOption1 = false;
				}
			}
			
			init();

			// オプション表示状態を復元
			if (mMultiMode) {
				if (isOption1) {
				mMultiMenu1.setVisibility(View.VISIBLE);
				} else {
					mMultiMenu2.setVisibility(View.VISIBLE);
				}
				
			}

			View view = getWindow().getDecorView();
			changeActivity(view);

		}

		// init();
		// View view = getWindow().getDecorView();
		// changeActivity(view);
		// mStage.dispose();
	}

	private CachingAccessor mClient;

	private void setHeaderInfo() {
		// カテゴリ名表示
		TextView txtName = (TextView) mHeader.findViewById(R.id.txt_name);
		txtName.setText(mCategoryName);
		// ローカルでは更新ボタン,更新日時が不要な為、表示無し
		Button mBtnUpdateCache = (Button) mHeader.findViewById(R.id.btnRefresh);
		mBtnUpdateCache.setVisibility(View.GONE);
		TextView txtUp = (TextView) mHeader.findViewById(R.id.txtLastUpdated);
		txtUp.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		/*複数選択メニュー1*/
		//複数選択解除
		case R.id.lytMultipleOff:
			leaveMultiMode();
			mMultiMenu1.setVisibility(View.GONE);
			mMultiMenu2.setVisibility(View.GONE);
			break;
		
		//タグの編集
		case R.id.lytTag:
			ArrayList<String> listTag = getCheckedFileList();
			if(listTag.size() == 0)return;
			Intent tagEditIntent = new Intent(getApplicationContext(), TagEditActivity.class);
			
			//選択されている画像を取得
			tagEditIntent.putExtra(ApplicationDefine.INTENT_PATH_LIST, listTag);
			startActivityForResult(tagEditIntent, ApplicationDefine.REQUEST_TAG);
			break;
		
		//共有
		case R.id.lytShare:
			ArrayList<String> listSync = getCheckedFileList();
			if(listSync.size() == 0)return;
		
			ArrayList<Uri> uris = new ArrayList<Uri>();
			Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			
			String type = null;
			
			int size = listSync.size();
			
			for (int i = 0; i < size; i++) {
				
				String path = listSync.get(i);
				
//				File f = new File(ApplicationDefine.INTENT_PATH, path);
				File f = new File(path);
				
				// シークレットが含まれていた場合、処理中断
				if (ExtUtil.isSecret(f)) {
					Toast.makeText(getParent(), getString(R.string.image_context_message_sync_multitype), Toast.LENGTH_SHORT).show();
					return;
				}
				
				
				String mimeType = ExtUtil.getMimeType(path);
				
				String typeName = mimeType.substring(0, mimeType.indexOf("/") + 1);
				
				// 初回登録
				if (type == null) {
					type = typeName;				
				} 
				// MimeTypeを比較し、別タイプのファイルが含まれていれば、処理中断
				else if (!type.equals(typeName)) {
					Toast.makeText(getParent(), getString(R.string.image_context_message_sync_multitype), Toast.LENGTH_SHORT).show();
					return;
				}
				
				uris.add(Uri.fromFile(f));
			}
			
			// 取得したMimeTypeをセット
//			shareIntent.setType("*/*");
			shareIntent.setType(type + "*");
			shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			startActivity(Intent.createChooser(shareIntent, getString(R.string.image_context_share)));
			
			isRescan = false;
			
//			Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
//			List<String> selected = mAdapter.getSelectedList();
//			ArrayList<Uri> uris = new ArrayList<Uri>();
//			
//			for(int n = 0; n < selected.size(); ++n){
//				File f = new File(selected.get(n));
//				uris.add(Uri.fromFile(f));
//			}
//			intent.setType(ExtUtil.getMimeType(selected.get(0)));
//			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
//			startActivity(Intent.createChooser(intent, getString(R.string.image_context_share)));
			
			
			break;
		
		//削除
		case R.id.lytDelete:
			
			final List<String> deleteTarget = getCheckedFileList();
			
			if (deleteTarget.size() == 0) return;
			
			 new AlertDialog.Builder(NewGridActivity.this.getParent())
			.setTitle(android.R.string.dialog_alert_title)
			.setMessage(R.string.confirm_delete)
			.setPositiveButton(android.R.string.cancel, null)
			.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					mFileOp.deleteFiles(deleteTarget, deleteTarget.size());
					// キャッシュ削除
					removeCash(deleteTarget, true, true);
					
					//TODO 暫定処理　タグ一覧からの複数選択時は、チェック状態をクリア
					if (!mIsLocalGrid && mCheckState != null) {
						mCheckState = new boolean[mFileNameList.size()];
					}
					
					// 一覧を更新
					mGrid.invalidateData();
				}
			})
			.show();
			break;
		
		//詳細
		case R.id.lytDetail:
			ArrayList<String> detailTarget = new ArrayList<String>();
			detailTarget = getCheckedFileList();
			if (detailTarget.size() == 0) return;
			new AlertDialog.Builder(NewGridActivity.this.getParent())
			.setTitle(getString(R.string.image_context_info))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setView(new ExifView(NewGridActivity.this, detailTarget))
			.setPositiveButton(android.R.string.ok, null)
			.show();
			
			break;
		
		//その他
		case R.id.lytOther:
			// オプションメニュー1を非表示にし、オプションメニュー2を表示
			mMultiMenu1.setVisibility(View.GONE);
			mMultiMenu2.setVisibility(View.VISIBLE);
			break;
			
		/*複数選択メニュー2*/
		//移動
		case R.id.lytMove:
			ArrayList<String> moveTarget = new ArrayList<String>();
			moveTarget = getCheckedFileList();
			if (moveTarget.size() == 0) return;
			
			Intent moveIntent = new Intent(getApplicationContext(), SelectFolderActivity.class);
			moveIntent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.folder_title_select));
			moveIntent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
			moveIntent.putExtra(SelectFolderActivity.PARAM_START_PATH, getIntent().getStringExtra(INTENT_TARGET));
			startActivityForResult(moveIntent, REQUEST_SELECT_FOLDER_FOR_MOVE_MULTI);
			
			break;
		//コピー
		case R.id.lytCopy:
			ArrayList<String> copyTarget = new ArrayList<String>();
			copyTarget = getCheckedFileList();
			if (copyTarget.size() == 0) return;
			
			Intent copyIntent = new Intent(getApplicationContext(), SelectFolderActivity.class);
			copyIntent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.folder_copy_title_select));
			copyIntent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
			copyIntent.putExtra(SelectFolderActivity.PARAM_START_PATH, getIntent().getStringExtra(INTENT_TARGET));
			startActivityForResult(copyIntent, REQUEST_SELECT_FOLDER_FOR_COPY_MULTI);
			break;
	
		//同期フォルダにコピー
		case R.id.lytMoveSync:
			ArrayList<String> syncTarget = new ArrayList<String>();
			syncTarget = getCheckedFileList();
			if (syncTarget.size() == 0) return;
			
			File pathExternalPublicDir = new File(ApplicationDefine.PATH_JSPHOTO);
			if (!pathExternalPublicDir.exists()) pathExternalPublicDir.mkdirs();
			String dir = pathExternalPublicDir.getPath();
			
			mFileOp.copyToFolder(dir, syncTarget, syncTarget.size(), false);
			
			break;
			
		//お気に入り登録
		case R.id.lytFavorite:

			// 既にタスクが走っているならば、処理中断
			if (mMultiFavoriteTask != null && mMultiFavoriteTask.getStatus() == AsyncTask.Status.RUNNING) {
				break;
			}
			
			List<String> listFavorite = getCheckedFileList();
			if (listFavorite.size() <= 0) return ; 
			

			
			mMultiFavoriteTask = new FavoriteTask();
			mMultiFavoriteTask.execute();
			
			break;
			
		//シークレット
		case R.id.lytSecret:
			
			List<String> listSeclet = getCheckedFileList();
			if (listSeclet.size() <= 0) return ; 

			if (checkSecret(listSeclet)) {
				showDialog(DIALOG_NO_SECRET);
			} else {
				showDialog(DIALOG_SECRET);
			}

			break;

		//簡易メニュー
		case R.id.lytSimpleMenu:
			// オプションメニュー2を非表示にし、オプションメニュー1を表示
			mMultiMenu2.setVisibility(View.GONE);
			mMultiMenu1.setVisibility(View.VISIBLE);
			break;
		}

	}
	
	
	/**
	 * お気に入り操作タスク
	 */
	public class FavoriteTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			mProgress.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			
			boolean result;
			
			// お気に入りの状態によって、処理を判断
			if (checkFavorite(getCheckedFileList())) {
				result = setFavorite(false);
			} else {
				result = setFavorite(true);
			}
			
			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			mProgress.dismiss();
			
			//TODO 暫定処理　タグ一覧からの複数選択時は、チェック状態をクリア
			if (!mIsLocalGrid && mCheckState != null) {
				mCheckState = new boolean[mFileNameList.size()];
			}
			
			// 最後にリスキャンをかける
			rescanMedia(null);
		}
	}
	
	/**
	 * シークレット操作タスク
	 */
	public class SecretTask extends AsyncTask<Void, Void, Boolean> {

		boolean mIsSecret;
		
		public SecretTask(boolean isSecret) {
			mIsSecret = isSecret;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			mProgress.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			
			return setSecret(mIsSecret);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			String message;
			
			if (result) {
				if (mIsSecret) {
					message = getString(R.string.folder_message_setting_secret_success);
				} else {
					message = getString(R.string.folder_message_setting_no_secret_success);
				}
			} else {
				if (mIsSecret) {
					message = getString(R.string.folder_message_setting_secret_failure);
				} else {
					message = getString(R.string.folder_message_setting_no_secret_failure);
				}
			}
			
			Toast.makeText(getParent(), message, Toast.LENGTH_SHORT).show();
			
			mProgress.dismiss();
			
			//TODO 暫定処理　タグ一覧からの複数選択時は、チェック状態をクリア
			if (!mIsLocalGrid && mCheckState != null) {
				mCheckState = new boolean[mFileNameList.size()];
			}
			
			// 最後にリスキャンをかける
			rescanMedia(null);
		}
	}
}
