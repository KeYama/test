package jp.co.johospace.jsphoto;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import jp.co.johospace.jsphoto.accessor.MediaIndexesAccessor;
import jp.co.johospace.jsphoto.accessor.MediaMetaDataAccessor;
import jp.co.johospace.jsphoto.cache.ImageCache;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.SecretPasswordDialog;
import jp.co.johospace.jsphoto.grid.AsyncFileOperation;
import jp.co.johospace.jsphoto.grid.AsyncImageAdapter;
import jp.co.johospace.jsphoto.grid.AsyncImageView;
import jp.co.johospace.jsphoto.grid.ExifView;
import jp.co.johospace.jsphoto.grid.ExtUtil;
import jp.co.johospace.jsphoto.grid.FavoriteUtil;
import jp.co.johospace.jsphoto.grid.GeoTag;
import jp.co.johospace.jsphoto.grid.ImageShrinker;
import jp.co.johospace.jsphoto.grid.WallpaperHelper;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner.OnFoundListener;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.app.ActivityGroup;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * 画像一覧画面
 */
public class GridActivity extends ActivityGroup implements OnFoundListener, OnItemClickListener {
	protected AsyncImageAdapter mAdapter;
	private AsyncFileOperation mFileOp;
	private List<String> mFileOpTarget;
	private static final int MAX_NAME_P = 8;
	private static final int MAX_NAME_L = 16;
	/** 選択中アイテムのファイルパス */
	private String mFilePath;
	/** 選択中のフォルダのパス */
	private String mFolderPath;
	/** タグ名 */
	private String mTagName;
	/** カテゴリ名 */
	private String mCategoryName;
	
	private GeoTag mGeoTag;
	private int mPortraitSize;
	private int mLandscapeSize;
	private boolean mMultiMode;
	protected String mFullTitle;
	
	private WallpaperHelper mWallpaper;
	
	private Handler mHandler = new Handler();
	
	
	/** メディアスキャナ */
	private JorlleMediaScanner mMediaScanner;
	
	/** データベース */
	private SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();
	/** ホーム画面から遷移したかどうかのフラグ */
	private boolean mFlgChangeHome = false;
	/** ソーティングビュー */
	private RadioGroup mSortingView;
	private int mSortAlgorithm;
	//スキャン前の位置
	private int mSelection;
	//スキャン後、GridViewが表示すべきパス位置
	private String mGridTargetPath;
	private File mTmpFile;
	
	private ImageShrinker mShrinker;
	
	private static final int REQUEST_COPY_SINGLE_FILE = 1;
	private static final int REQUEST_MOVE_SINGLE_FILE = 2;
	private static final int REQUEST_COPY_MULTI_FILE = 3;
	private static final int REQUEST_MOVE_MULTI_FILE = 4;
	private static final int REQUEST_FULLSCREEN = 5;
	private static final int REQUEST_ROTATE = 6;
	private static final int REQUEST_SHRINK = 7;
	private static final int REQUEST_CROP_IMAGE = 8;
	
	/** コピーのメニューID */
	private static final int MENU_COPY = 15;
	/** タグ編集のメニューID */
	private static final int MENU_TAG = 1;
	/** 共有のメニューID */
	private static final int MENU_SHARE = 2;
	/** 登録のメニューID */
	private static final int MENU_REGISTER = 3;
	/** お気に入り登録のメニューID */
	private static final int MENU_FAVORITE = 4;
	/** マップに表示のメニューID */
	private static final int MENU_SHOW_MAP = 5;
	/** シークレットファイル化メニューID */
	private static final int MENU_TO_SECRET = 6;
	/** シークレットファイル化解除メニューID */
	private static final int MENU_UNSECRET = 7;
	/** 編集メニューID */
	protected static final int MENU_EDIT = 8;
	/** 情報ID */
	protected static final int MENU_INFO = 9;
	protected static final int MENU_MOVE = 10;
	protected static final int MENU_DELETE = 11;
	
	private static final int MENU_ROTATE_90 = 12;
	private static final int MENU_ROTATE_180 = 13;
	private static final int MENU_ROTATE_270 = 14;
	
	private static final int MENU_SHRINK = 16;
	
	private static final int MENU_SHRINK_START = 100;
	
	/** メニュー項目 */
	protected static final int
		MENU_ITEM_SETTING = 1,
		MENU_ITEM_SORT = 2,
		MENU_ITEM_MULTI = 3;
	
	/** 表示順序ダイアログの項目 */
	protected static final int
		SORT_DATE_ASC = 0,
		SORT_DATE_DESC = 1,
		SORT_NAME_ASC = 2,
		SORT_NAME_DESC = 3;
	
	private static final int DIALOG_SECRET = 0;
	private static final int DIALOG_NO_SECRET = 1;
	private static final int DIALOG_INFO = 2;
	private static final int DIALOG_CONFIRM_DELETE = 3;
	private static final int DIALOG_CONFIRM_MULTI_DELETE = 4;
	private static final int DIALOG_SORT = 5;
	
	private static final String PREF_SORTING = "sort";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.grid);
		
		// フォルダパスを取得
		Intent intent = getIntent();
		mFolderPath = intent.getStringExtra(ApplicationDefine.INTENT_FOLDER_PATH);
		
		String pathName = mFolderPath;
		
		// カテゴリ名
		mCategoryName = intent.getStringExtra(ApplicationDefine.INTENT_CATEGORY_NAME);
		
		mFlgChangeHome = intent.getBooleanExtra(ApplicationDefine.INTENT_CHANGE_HOME, false);
		
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
		mTagName = intent.getStringExtra(ApplicationDefine.INTENT_FOLDER_TAG);
		
		// フィルタリング　タグ一覧を取得
		String[] folderTagList = intent.getStringArrayExtra(ApplicationDefine.INTENT_FOLDER_TAG_LIST);
		
		File folder;
		
		if (mFolderPath == null) {
			// フォルダパス未設定ならば、SDカード内部を表示
			folder = Environment.getExternalStorageDirectory();
			mFolderPath = folder.getAbsolutePath();
		} else {
			// 取得したパスを元に、フォルダ内の画像一覧を表示
			folder = new File(mFolderPath);
		}
		
		GridView view = (GridView)findViewById(R.id.gridImage);
		view.setAdapter(mAdapter = createAdapter());
		view.setOnItemClickListener(this);
		
		mMediaScanner = new JorlleMediaScanner();
		
		// シークレットのみ
		if (folderSecret) {
			mMediaScanner.scanOnlySecret();
		}
		
		// お気に入り
		if (folderFavorite) {
			mMediaScanner.favorite();
		} 
		
		// 開始日
		if (startTime >= 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(startTime);
			mMediaScanner.startTime(cal);
		}
		
		// 終了日
		if (endTime >= 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(endTime);
			mMediaScanner.endTime(cal);
		}
		
		// タグ
		if (mTagName != null) {
			mMediaScanner.addTag(mTagName);
		}
		
		// タグリスト
		if (folderTagList != null) {
			for (String tag : folderTagList) {
				mMediaScanner.addTag(tag);
			}
		}
		
		// 隠しフォルダ表示状態
		boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false);
		
		//実際の探す処理はRadioButtonのハンドラが行う
		mMediaScanner.sort(new JorlleMediaScanner.DateDescender()).baseFolder(folder).scanNomedia(hiddenState).scanSecret(secretState).scanSubfolder(scanSub);
		
		
//		mMediaScanner.sort(new JorlleMediaScanner.DateDescender()).baseFolder(folder).scanNomedia(false).scanSubfolder(scanSub).findMedia(this);
		
		registerForContextMenu(view);
		mFileOp = new AsyncFileOperation(this);
		
		mFileOp.setOnCompleteListener(new AsyncFileOperation.OnCompleteListener() {
			
			@Override
			public void onComplete() {
				rescanMedia();
			}
			
			@Override
			public void onCancel() {
				rescanMedia();
			}
		});
		
		setupTopMenu();
		
//		if (mCategoryName != null) {
//			((TextView)findViewById(R.id.tvTitle)).setText(mCategoryName);
//		}else if(pathName != null){
//			String folderName = folder.getName();
//			((TextView)findViewById(R.id.tvTitle)).setText(folderName);
//		}else if(mTagName != null){
//			((TextView)findViewById(R.id.tvTitle)).setText(mTagName);	
//		}else if(folderTagList != null){
//			String tags = folderTagList[0];
//			for(int n = 1; n < folderTagList.length; ++n){
//				tags += "," + folderTagList[n];
//			}
//			((TextView)findViewById(R.id.tvTitle)).setText(tags);	
//		}
		
//		String limit = ((TextView)findViewById(R.id.tvTitle)).getText().toString();
//		mFullTitle = limit;
//		int maxName = 0;
//		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
//			maxName = MAX_NAME_L;
//		}else{
//			maxName = MAX_NAME_P;
//		}
//		if(limit.length() > maxName){
//			limit = limit.substring(0, maxName - 3) + "...";
//		}
//		((TextView)findViewById(R.id.tvTitle)).setText(limit);
		
		changeGridSize();
		setupToolbar();
		leaveMultiMode();
	}
	
	protected AsyncImageAdapter createAdapter() {
		return new AsyncImageAdapter(getApplicationContext());
	}
	
	private void enterMultiMode(){
		mMultiMode = true;
		mAdapter.setMultiMode(true);
		
		((GridView)findViewById(R.id.gridImage)).setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				boolean result = mAdapter.toggleItem(pos);
				((AsyncImageView)view).setCheck(result);
				checkToolbar();
			}

		});
		
		findViewById(R.id.llToolbar).setVisibility(View.VISIBLE);
	}
	

	/**
	 * ツールバーのFavorite,Secretのアイコンの表示、非表示を切り替える
	 * 
	 * @param items
	 */
	private void checkToolbar() {
		List<String> items = mAdapter.getSelectedList();
		if(checkFavorite(items)){
			findViewById(R.id.ivFavorite).setVisibility(View.GONE);
			findViewById(R.id.ivUnfavorite).setVisibility(View.VISIBLE);
		}else{
			findViewById(R.id.ivFavorite).setVisibility(View.VISIBLE);
			findViewById(R.id.ivUnfavorite).setVisibility(View.GONE);
		}
		if(checkSecret(items)){
			findViewById(R.id.ivSecret).setVisibility(View.GONE);
			findViewById(R.id.ivUnsecret).setVisibility(View.VISIBLE);					
		}else{
			findViewById(R.id.ivSecret).setVisibility(View.VISIBLE);
			findViewById(R.id.ivUnsecret).setVisibility(View.GONE);					
		}
	}
	
	private boolean checkFavorite(List<String> items){
		for(String item: items){
			if(!FavoriteUtil.isFavorite(new File(item))){
				return false;
			}
		}
		return true;
	}
	
	private boolean checkSecret(List<String> items){
		for(String item: items){
			if(!ExtUtil.isSecret(new File(item))){
				return false;
			}
		}
		return true;
	}
	
	
	private void leaveMultiMode(){
		mMultiMode = false;
		mAdapter.setMultiMode(false);
		
		((GridView)findViewById(R.id.gridImage)).setOnItemClickListener(this);
		findViewById(R.id.llToolbar).setVisibility(View.GONE);
	}
	
	private void setupToolbar(){
		ToolbarListener l = new ToolbarListener();
		findViewById(R.id.ivCopy).setOnClickListener(l);
		findViewById(R.id.ivMove).setOnClickListener(l);
		findViewById(R.id.ivDelete).setOnClickListener(l);
		findViewById(R.id.ivShare).setOnClickListener(l);
		findViewById(R.id.ivRotato).setOnClickListener(l);
		findViewById(R.id.ivTag).setOnClickListener(l);
		findViewById(R.id.ivFavorite).setOnClickListener(l);
		findViewById(R.id.ivUnfavorite).setOnClickListener(l);
		findViewById(R.id.ivSecret).setOnClickListener(l);
		findViewById(R.id.ivUnsecret).setOnClickListener(l);
		findViewById(R.id.ivAllSelect).setOnClickListener(l);
		
		checkToolbar();
	}
	
	private class ToolbarListener implements View.OnClickListener{

		@Override
		public void onClick(View v) {
			switch(v.getId()){
			case R.id.ivCopy:{
				if(!mAdapter.isSelectedAnyItem())return;
				Intent intent = new Intent(getApplicationContext(), SelectFolderActivity.class);
				intent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.image_context_copy));
				intent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
				intent.putExtra(SelectFolderActivity.PARAM_START_PATH, new File(mFolderPath).getParent());
				startActivityForResult(intent, REQUEST_COPY_MULTI_FILE);

				break;
			}
				
			case R.id.ivMove:{
				if(!mAdapter.isSelectedAnyItem())return;
				Intent intent = new Intent(getApplicationContext(), SelectFolderActivity.class);
				intent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.image_context_move));
				intent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
				intent.putExtra(SelectFolderActivity.PARAM_START_PATH, new File(mFolderPath).getParent());
				startActivityForResult(intent, REQUEST_MOVE_MULTI_FILE);

				break;
			}
				
			case R.id.ivDelete:{
				if(!mAdapter.isSelectedAnyItem())return;
				showDialog(DIALOG_CONFIRM_MULTI_DELETE);
				break;
			}
				
			case R.id.ivShare:{
				if(!mAdapter.isSelectedAnyItem())return;
				Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
				List<String> selected = mAdapter.getSelectedList();
				ArrayList<Uri> uris = new ArrayList<Uri>();
				
				for(int n = 0; n < selected.size(); ++n){
					File f = new File(selected.get(n));
					uris.add(Uri.fromFile(f));
				}
				intent.setType(ExtUtil.getMimeType(selected.get(0)));
				intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
				startActivity(Intent.createChooser(intent, getString(R.string.image_context_share)));
				break;
			}
				
			case R.id.ivRotato:
				//TODO 実装
				break;
				
			case R.id.ivTag:{
				if(!mAdapter.isSelectedAnyItem())return;
				ArrayList<String> selected = mAdapter.getSelectedList();
				
				Intent intent = new Intent(GridActivity.this, TagEditActivity.class);
				intent.putStringArrayListExtra(ApplicationDefine.INTENT_PATH_LIST, selected);
				startActivityForResult(intent, ApplicationDefine.REQUEST_TAG);
//				startActivity(intent);
				break;
			}
				
			case R.id.ivFavorite:{
				if(!mAdapter.isSelectedAnyItem())return;
				List<String> selected = mAdapter.getSelectedList();
				for(String selectedPath: selected){
					File file = new File(selectedPath);
					if(!FavoriteUtil.isFavorite(file))
						FavoriteUtil.addFavorite(file);
				}
				mAdapter.notifyDataSetChanged();
				rescanMedia();
				break;
			}
				
			case R.id.ivUnfavorite:{
				if(!mAdapter.isSelectedAnyItem())return;
				List<String> selected = mAdapter.getSelectedList();
				for(String selectedPath: selected){
					FavoriteUtil.removeFavorite(new File(selectedPath));
				}
				rescanMedia();
//				mAdapter.notifyDataSetChanged();
				break;
			}
			
			case R.id.ivAllSelect:
				mAdapter.toggleAllSelect();
				checkToolbar();
				break;
				
			case R.id.ivSecret:{
				if(!mAdapter.isSelectedAnyItem())return;
				showDialog(DIALOG_SECRET);

				break;
			}
			
			case R.id.ivUnsecret:{
				if(!mAdapter.isSelectedAnyItem())return;
				showDialog(DIALOG_NO_SECRET);
				break;
			}
			}
		}
		
	}
	
	protected void changeGridSize(){
		int size = Integer.parseInt(PreferenceUtil.getPreferenceValue(this, ApplicationDefine.PREF_FOLDERGRID_SIZE, "0"));
		switch(size){
		case 0:
			mLandscapeSize = 5;
			mPortraitSize = 3;
			break;
		case 1:
			mLandscapeSize = 6;
			mPortraitSize = 4;
			break;
		case 2:
			mLandscapeSize = 7;
			mPortraitSize = 5;
			break;
		}
		
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
			changeGridSize(mPortraitSize);
		}else{
			changeGridSize(mLandscapeSize);
		}
	}
	
	private void changeGridSize(int size){
		((GridView)findViewById(R.id.gridImage)).setNumColumns(size);
	}
	
	private void setupTopMenu(){
		OnTopClickListener listener = new OnTopClickListener();
//		findViewById(R.id.iconCamera).setOnClickListener(listener);
//		findViewById(R.id.iconHome).setOnClickListener(listener);
//		findViewById(R.id.iconFiltering).setOnClickListener(listener);
//		findViewById(R.id.iconMulti).setOnClickListener(listener);
		
		SharedPreferences pref = getPreferences(MODE_PRIVATE);
		int id = pref.getInt(PREF_SORTING, R.id.rbDateDescender);
		
		createSortingMenu(id);
	}
	
	
//	@Override
//	public void onConfigurationChanged(Configuration newConfig) {
//		if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
//			changeGridSize(mLandscapeSize);
//		}else{
//			changeGridSize(mPortraitSize);
//		}
//		
//		String limit = mFullTitle;
//		int maxName = 0;
//		if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
//			maxName = MAX_NAME_L;
//		}else{
//			maxName = MAX_NAME_P;
//		}
//		if(limit.length() > maxName){
//			limit = limit.substring(0, maxName - 3) + "...";
//		}
//		((TextView)findViewById(R.id.tvTitle)).setText(limit);
//		
//		
//		super.onConfigurationChanged(newConfig);
//	}

	@Override
	protected void onDestroy() {
		mAdapter.dispose();
		mMediaScanner.dispose();
		
		super.onDestroy();
	}

	
    /**
	 * メニュー作成イベント
	 * @param menu
	 * @return true:成功、false:失敗
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ITEM_SETTING, 0, getResources().getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);
		menu.add(0, MENU_ITEM_SORT, 0, getResources().getString(R.string.menu_sort)).setIcon(R.drawable.ic_sort);
		menu.add(0, MENU_ITEM_MULTI, 0, getResources().getString(R.string.image_menu_multiple_select)).setIcon(R.drawable.ic_all_select);
		
		return true;
	}
	
	
	/**
	 * メニュー選択イベント
	 * @param item
	 * @return true:成功、false:失敗
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			switch(item.getItemId()) {
			
				// 設定
				case MENU_ITEM_SETTING:
					Intent intent = new Intent();
					intent = new Intent(GridActivity.this, JorllePrefsActivity.class);
					startActivityForResult(intent, ApplicationDefine.REQUEST_PREF_SETTING);
//					startActivity(intent);
					
					break;
					
				// 並べ替え
				case MENU_ITEM_SORT:
					mSortingView.setVisibility(View.GONE);
					showDialog(DIALOG_SORT);
					break;
					
				// 複数選択
				case MENU_ITEM_MULTI:
					if(mMultiMode){
						leaveMultiMode();
					}else{
						enterMultiMode();
					}
					
					break;
			}

			return true;
			
		} catch (Throwable t) {
			return true;
		}
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return doDispatchKeyEvent(event);
	}

	protected boolean doDispatchKeyEvent(KeyEvent event) {
		if(event.getKeyCode() == KeyEvent.KEYCODE_BACK && mMultiMode){
			leaveMultiMode();
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
		case REQUEST_COPY_SINGLE_FILE:
			if(resultCode == RESULT_OK){
				mFileOp.copyToFolder(
						data.getStringExtra(SelectFolderActivity.RESULT_PATH), mFileOpTarget, mFileOpTarget.size(), true);
			}
			break;
			
		case REQUEST_MOVE_SINGLE_FILE:
			if(resultCode == RESULT_OK){
				mFileOp.moveToFolder(
						data.getStringExtra(SelectFolderActivity.RESULT_PATH), mFileOpTarget, mFileOpTarget.size());
			}
			break;
			
		case REQUEST_COPY_MULTI_FILE:
			if(resultCode == RESULT_OK){
				List<String> target = mAdapter.getSelectedList();
				mFileOp.copyToFolder(
						data.getStringExtra(SelectFolderActivity.RESULT_PATH), target, target.size(), true);
			}
			break;

		case REQUEST_MOVE_MULTI_FILE:
			if(resultCode == RESULT_OK){
				List<String> target = mAdapter.getSelectedList();
				mFileOp.moveToFolder(
						data.getStringExtra(SelectFolderActivity.RESULT_PATH), target, target.size());
			}
			break;
			
		// タグ編集画面
		case ApplicationDefine.REQUEST_TAG:
			if (resultCode == RESULT_OK) {
				rescanMedia();
			}
			break;
			
		case REQUEST_FULLSCREEN:
			if(resultCode == ApplicationDefine.RESULT_HOME){
				setResult(ApplicationDefine.RESULT_HOME);
				finish();
			}else{
				changeGridSize();
				rescanMedia();
			}
			break;
			
		case REQUEST_ROTATE:{
			File media = new File(mFilePath);
			OpenHelper.cache.getDatabase().delete(ImageCache.$TABLE,
					ImageCache.DIRPATH + " = ? AND " + ImageCache.NAME + " = ?",
					new String[] {media.getParent(), media.getName()});
			mAdapter.setModifiedImagePath(mFilePath);
			rescanMedia();
			mGridTargetPath = mFilePath;
			break;
		}
			
		// 設定画面
		case ApplicationDefine.REQUEST_PREF_SETTING:
			
			// 隠しフォルダ表示状態
			boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
			// シークレット表示状態
			boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false);
			
			String syncPath = null;
			
			if (mCategoryName != null && mCategoryName.equals(getString(R.string.home_label_sync))) {
				syncPath = PreferenceUtil.getPreferenceValue(this, PicasaPrefsActivity.PREF_PICASA_SYNC_LOCAL, null);
			}
			
			changeGridSize();
			rescanMedia(syncPath, hiddenState, secretState);
			break;
			
		case REQUEST_SHRINK:
			rescanMedia();
			break;
			
		case REQUEST_CROP_IMAGE:
			mWallpaper.onActivityResult(this, resultCode);
			break;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		try{
			switch(item.getItemId()){
			case MENU_COPY:{ 
				Intent intent = new Intent(getApplicationContext(), SelectFolderActivity.class);
				intent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.image_context_copy));
				intent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
				intent.putExtra(SelectFolderActivity.PARAM_START_PATH, new File(mFilePath).getParent());
				startActivityForResult(intent, REQUEST_COPY_SINGLE_FILE);
				break;
			}
			
			case MENU_DELETE:{ 
				showDialog(DIALOG_CONFIRM_DELETE);
				break;
			}
			
			case MENU_MOVE:{ 
				Intent intent = new Intent(getApplicationContext(), SelectFolderActivity.class);
				intent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.image_context_move));
				intent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
				intent.putExtra(SelectFolderActivity.PARAM_START_PATH, new File(mFilePath).getParent());
				startActivityForResult(intent, REQUEST_MOVE_SINGLE_FILE);
				break;
			}
				
			case MENU_TAG:{
				Intent tagEditIntent = new Intent(getApplicationContext(), TagEditActivity.class);
				
				tagEditIntent.putExtra(ApplicationDefine.INTENT_PATH, mFilePath);
				startActivityForResult(tagEditIntent, ApplicationDefine.REQUEST_TAG);
//				startActivity(tagEditIntent);
				break;
			}
				
			case MENU_SHARE:{
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType(ExtUtil.getMimeType(mFilePath));
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mFilePath)));
				startActivity(Intent.createChooser(intent, getString(R.string.image_context_share)));
				break;
			}
			
			case MENU_REGISTER:{
				mWallpaper = new WallpaperHelper();
				mWallpaper.startCropActivity(this, mFilePath, REQUEST_CROP_IMAGE);
				break;
			}
			
			case MENU_FAVORITE:{
				File f = new File(mFilePath);
				if(FavoriteUtil.isFavorite(f)){
					FavoriteUtil.removeFavorite(f);
				}else{
					FavoriteUtil.addFavorite(f);
				}
//				mAdapter.notifyDataSetChanged();
				rescanMedia();
				break;
			}
			
			case MENU_SHOW_MAP:{
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mGeoTag.toGeoLocation()));
				startActivity(intent);
				break;
			}
			case MENU_TO_SECRET: {
				showDialog(DIALOG_SECRET);
				break;
			}
			case MENU_UNSECRET: {
				showDialog(DIALOG_NO_SECRET);
				break;
			}
			
			case MENU_EDIT: {
				Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(mFilePath));
				intent.setType(ExtUtil.getMimeType(mFilePath));
				startActivity(Intent.createChooser(intent, getString(R.string.image_context_edit)));
				break;
			}
			
			case MENU_INFO: {
				removeDialog(DIALOG_INFO);
				showDialog(DIALOG_INFO);
				break;
			}
			
			case MENU_ROTATE_90:{
				startRotate(ImageOpActivity.ROTATE_90);
				break;
			}
			case MENU_ROTATE_180:{
				startRotate(ImageOpActivity.ROTATE_180);
				break;
			}
			case MENU_ROTATE_270:{
				startRotate(ImageOpActivity.ROTATE_270);
				break;
			}
			
			case MENU_SHRINK:{
				if(mShrinker.getAvailableSize().size() == 0)
					Toast.makeText(this, R.string.toast_no_shrink, Toast.LENGTH_SHORT).show();
				break;
			}
			
			}//end switch
			
			//縮小メニューが選ばれたら、縮小開始
			if(item.getItemId() >= MENU_SHRINK_START){
				int n = item.getItemId() - MENU_SHRINK_START;
				ImageShrinker.Size size = mShrinker.getAvailableSize().get(n);
				Intent i = new Intent(getApplicationContext(), ImageOpActivity.class);
				i.putExtra(ImageOpActivity.INTENT_TARGET_PATH, mFilePath);
				i.putExtra(ImageOpActivity.INTENT_RESIZE, true);
				i.putExtra(ImageOpActivity.INTENT_RESIZE_HEIGHT, size.height);
				i.putExtra(ImageOpActivity.INTENT_RESIZE_WIDTH, size.width);
				i.putExtra(ImageOpActivity.INTENT_TITLE, getString(R.string.title_shrink));
				startActivityForResult(i, REQUEST_SHRINK);
			}

		}catch(ActivityNotFoundException e){
		}
		
		return true;
	}
	
	
	private void startRotate(int tag){
		Intent i = new Intent(getApplicationContext(), ImageOpActivity.class);
		i.putExtra(ImageOpActivity.INTENT_ROTATE, true);
		i.putExtra(ImageOpActivity.INTENT_ROTATE_ORIENTATION, tag);
		i.putExtra(ImageOpActivity.INTENT_TARGET_PATH, mFilePath);
		i.putExtra(ImageOpActivity.INTENT_TITLE, getString(R.string.image_context_rotate));
		
		startActivityForResult(i, REQUEST_ROTATE);
	}
	
	protected void rescanMedia(){
		if(mMediaScanner != null)mMediaScanner.dispose();
		mMediaScanner = mMediaScanner.newWithSameSetting();
		

		// 隠しフォルダ表示状態
		boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false);
		mMediaScanner.scanNomedia(hiddenState).scanSecret(secretState);
		
		GridView view = (GridView)findViewById(R.id.gridImage);
		mSelection = view.getFirstVisiblePosition();
		
		mAdapter.clear();
		mAdapter.notifyDataSetChanged();
		mMediaScanner.findMedia(this);
	}
	
	private void rescanMedia(Comparator<File> comp){
		if(mMediaScanner != null)mMediaScanner.dispose();
		mMediaScanner = mMediaScanner.newWithSameSetting();
		mMediaScanner.sort(comp);
		
		GridView view = (GridView)findViewById(R.id.gridImage);
		mSelection = view.getFirstVisiblePosition();
		
		
		mAdapter.clear();
		mAdapter.notifyDataSetChanged();
		mMediaScanner.findMedia(this);		
	}
	
	/** 同期フォルダ・隠しフォルダ・シークレットの状態を更新してスキャニング */
	private void rescanMedia(String syncPath, boolean hidden, boolean secret) {
		if(mMediaScanner != null)mMediaScanner.dispose();
		mMediaScanner = mMediaScanner.newWithSameSetting();
		
		GridView view = (GridView)findViewById(R.id.gridImage);
		mSelection = view.getFirstVisiblePosition();
		
		
		mAdapter.clear();
		mAdapter.notifyDataSetChanged();
		
		// 同期カテゴリだった場合
		if (syncPath != null) {
			mMediaScanner.baseFolder(syncPath);
			mMediaScanner.scanSubfolder(false);
		}
		
		mMediaScanner.scanNomedia(hidden).scanSecret(secret).findMedia(this);
	}
	
	
	protected void createSortingMenu(int defaultCheck){
		if(mSortingView == null){
			mSortingView = (RadioGroup)getLayoutInflater().inflate(R.layout.sorting, null);
			SortingListener listener = new SortingListener();
			mSortingView.setOnCheckedChangeListener(listener);
			mSortingView.check(defaultCheck);
			
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
					RadioGroup.LayoutParams.WRAP_CONTENT,
					RadioGroup.LayoutParams.WRAP_CONTENT
					);
			lp.gravity = Gravity.RIGHT;
			
			mSortingView.setLayoutParams(lp);
			mSortingView.setVisibility(View.GONE);
			((FrameLayout)findViewById(R.id.flContent)).addView(mSortingView);
		}		
	}
	
	private void toggleSortingMenu(){
		if(mSortingView.getVisibility() == View.GONE){
			mSortingView.setVisibility(View.VISIBLE);
		}else{
			mSortingView.setVisibility(View.GONE);
		}
	}
	

	/**
	 * 画像のシークレット状態を操作します
	 * 
	 * @param isSecret true:シークレットに設定	false:シークレット解除
	 */
	private void setSecret(boolean isSecret) {
		List<String> pathList = null;
		if(mMultiMode){
			pathList = mAdapter.getSelectedList();
		}else{
			pathList = new ArrayList<String>();
			pathList.add(mFilePath);
		}
		
		for(String path: pathList){
			File src = new File(path);
			File dest;
			
			if (isSecret) {
				dest = ExtUtil.toSecret(src);
			} else {
				dest = ExtUtil.unSecret(src);
			}
			
			String oldName = src.getName();
			String newName = dest.getName();
			
			if(!src.equals(dest)) {
				src.renameTo(dest);
			
				// 成功したら、DBも更新
				MediaMetaDataAccessor.updateMetaDataName(mDatabase, src.getParent(), oldName, newName);
				MediaIndexesAccessor.updateIndexesName(OpenHelper.cache.getDatabase(), src.getParent(), oldName, newName);
			}
		}
		rescanMedia();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		mFileOpTarget = new ArrayList<String>();
		mFilePath = (String)mAdapter.getItem(((AdapterView.AdapterContextMenuInfo)menuInfo).position);
		mFileOpTarget.add(mFilePath);
		File f = new File(mFilePath);
		mGeoTag = new GeoTag(mFilePath);
		
		
		menu.setHeaderTitle(getString(R.string.image_context_title));
		if(!ExtUtil.isVideo(mFilePath)){
			menu.add(menu.NONE, MENU_TAG, menu.NONE, getString(R.string.tag_title_edit));
			menu.add(menu.NONE, MENU_SHARE, menu.NONE, getString(R.string.image_context_share));
			menu.add(menu.NONE, MENU_MOVE, menu.NONE, getString(R.string.image_context_move));
			menu.add(menu.NONE, MENU_COPY, menu.NONE, getString(R.string.image_context_copy));
			menu.add(menu.NONE, MENU_DELETE, menu.NONE, getString(R.string.image_context_delete));
			if(checkCanRegister())
				menu.add(menu.NONE, MENU_REGISTER, menu.NONE, getString(R.string.image_context_register));
			//menu.add(menu.NONE, MENU_EDIT, menu.NONE, getString(R.string.image_context_edit));
			menu.add(menu.NONE, MENU_INFO, menu.NONE, getString(R.string.image_context_info));
			menu.add(menu.NONE, MENU_FAVORITE, menu.NONE, 
					getString((FavoriteUtil.isFavorite(f))? 
							R.string.image_context_unregister_favorite: 
							R.string.image_context_register_favorite));

			if(ExtUtil.isSecret(f)){
				menu.add(menu.NONE, MENU_UNSECRET, menu.NONE, getString(R.string.image_context_unsecret));	
			}else{
				menu.add(menu.NONE, MENU_TO_SECRET, menu.NONE, getString(R.string.image_context_to_secret));	
			}
			
			mShrinker = new ImageShrinker(mFilePath);
			List<ImageShrinker.Size> sizeList = mShrinker.getAvailableSize();
			if(sizeList.size() != 0){
				SubMenu shrink = menu.addSubMenu(menu.NONE, MENU_SHRINK, menu.NONE, getString(R.string.image_context_shrink));
				for(int n = 0; n < sizeList.size(); ++n){
					shrink.add(0, MENU_SHRINK_START+n, Menu.NONE, sizeList.get(n).width + "x" + sizeList.get(n).height);
				}
			}else{
				menu.add(menu.NONE, MENU_SHRINK, menu.NONE, getString(R.string.image_context_shrink));
			}
			
			if(mGeoTag.hasGeoTag()){
				menu.add(menu.NONE, MENU_SHOW_MAP, menu.NONE, getString(R.string.image_context_map));
			}
			
			//jpeg時のみローテートメニューを表示
			if(MediaUtil.getMimeTypeFromPath(mFilePath).equals("image/jpeg")){
				SubMenu rotate = menu.addSubMenu(getString(R.string.image_context_rotate));
				rotate.add(menu.NONE, MENU_ROTATE_90, menu.NONE, getString(R.string.image_context_rotate_90));
				rotate.add(menu.NONE, MENU_ROTATE_180, menu.NONE, getString(R.string.image_context_rotate_180));
				rotate.add(menu.NONE, MENU_ROTATE_270, menu.NONE, getString(R.string.image_context_rotate_270));	
			}
		}else{
			//Video
			menu.add(menu.NONE, MENU_MOVE, menu.NONE, getString(R.string.image_context_move));
			menu.add(menu.NONE, MENU_COPY, menu.NONE, getString(R.string.image_context_copy));
			menu.add(menu.NONE, MENU_DELETE, menu.NONE, getString(R.string.image_context_delete));
			menu.add(menu.NONE, MENU_TAG, menu.NONE, getString(R.string.tag_title_edit));
			menu.add(menu.NONE, MENU_SHARE, menu.NONE, getString(R.string.image_context_share));			
			menu.add(menu.NONE, MENU_FAVORITE, menu.NONE, 
					getString((FavoriteUtil.isFavorite(f))? 
							R.string.image_context_unregister_favorite: 
							R.string.image_context_register_favorite));
			if(ExtUtil.isSecret(f)){
				menu.add(menu.NONE, MENU_UNSECRET, menu.NONE, getString(R.string.image_context_unsecret));	
			}else{
				menu.add(menu.NONE, MENU_TO_SECRET, menu.NONE, getString(R.string.image_context_to_secret));	
			}
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	private boolean checkCanRegister(){
		return WallpaperHelper.checkCanCrop(this, mFilePath);
	}

	@Override
	public void onComplete() {
		
		// メディアが存在しない場合は、前の画面に戻る
		if (mAdapter.getCount() <= 0) {
			Toast.makeText(this, getString(R.string.image_message_no_media), Toast.LENGTH_LONG).show();
			finish();
		}
		
		//位置をリストア
		final GridView view = (GridView)findViewById(R.id.gridImage);
		
		//ターゲットパスが指定されている場合、その位置を復元位置にする
		if(mGridTargetPath != null){
			mSelection = mAdapter.getArrayList().indexOf(mGridTargetPath);
			mGridTargetPath = null;
		}
		
		//なぜか即時指定だと動かない
		mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				view.setSelection(mSelection);
			}
		}, 100);
		
		
		checkToolbar();
	}

	@Override
	public void onFound(File file) {
		mAdapter.addItem(file.getPath());
	}

	@Override
	public void onStartFolder(File folder) {
	}

	@Override
	public void onEndFolder(File folder, int size) {
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		String value = (String)mAdapter.getItem(position);
		openContent(position, value);
	}

	protected void openContent(int position, String gridItem) {
		if(ExtUtil.isVideo(gridItem)){
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(new File(gridItem)), "video/*");
			startActivity(intent);
		}else{
			Intent intent = new Intent(GridActivity.this, FullScreenActivity.class);
			intent.putStringArrayListExtra(FullScreenActivity.INTENT_FILE_PATH_LIST, mAdapter.getArrayList());
			intent.putExtra(FullScreenActivity.INTENT_INITIAL_POSITION, position);
			intent.putExtra(ApplicationDefine.INTENT_CHANGE_HOME, mFlgChangeHome);
			//リスキャンのための情報を渡す
			Intent old = getIntent();
			String folderPath = old.getStringExtra(ApplicationDefine.INTENT_FOLDER_PATH);
			if(folderPath != null)intent.putExtra(ApplicationDefine.INTENT_FOLDER_PATH, folderPath);
			
			// サブフォルダ検索の可否を取得
			boolean scanSub = old.getBooleanExtra(ApplicationDefine.INTENT_FOLDER_SUB, false);
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_SUB, scanSub);
			// フィルタリング　シークレットのみを取得
			boolean folderSecret = old.getBooleanExtra(ApplicationDefine.INTENT_FOLDER_SECRET, false);
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_SECRET, folderSecret);
			
			// フィルタリング　お気に入り状態を取得
			boolean folderFavorite = old.getBooleanExtra(ApplicationDefine.INTENT_FOLDER_FAVORITE, false);
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_FAVORITE, folderFavorite);
			
			// フィルタリング　開始日を取得
			Long startTime = old.getLongExtra(ApplicationDefine.INTENT_FOLDER_START, -1);
			if(startTime != -1)intent.putExtra(ApplicationDefine.INTENT_FOLDER_START, startTime);
			
			// フィルタリング　終了日を取得
			Long endTime = old.getLongExtra(ApplicationDefine.INTENT_FOLDER_END, -1);
			if(endTime != -1)intent.putExtra(ApplicationDefine.INTENT_FOLDER_END, endTime);
			
			// フィルタリング　タグを取得
			String tagName = old.getStringExtra(ApplicationDefine.INTENT_FOLDER_TAG);
			if(tagName != null)intent.putExtra(ApplicationDefine.INTENT_FOLDER_TAG, tagName);
			
			// フィルタリング　タグ一覧を取得
			String[] folderTagList = old.getStringArrayExtra(ApplicationDefine.INTENT_FOLDER_TAG_LIST);
			if(folderTagList != null)intent.putExtra(ApplicationDefine.INTENT_FOLDER_TAG_LIST, folderTagList);
			
			intent.putExtra(FullScreenActivity.INTENT_SORT, mSortAlgorithm);
			
//		intent.putExtra(ApplicationDefine.INTENT_FOLDER_PATH, mFolderPath);
//		intent.putExtra(ApplicationDefine.INTENT_FOLDER_TAG, mTagName);
			startActivityForResult(intent, REQUEST_FULLSCREEN);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		
		// シークレット設定ダイアログ
		case DIALOG_SECRET:
			
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.folder_title_setting_secret_file))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage(getString(R.string.folder_message_setting_secret_file))
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String password = PreferenceUtil.getPreferenceValue(GridActivity.this, ApplicationDefine.KEY_SECRET_PASSWORD, null);
					
					// パスワード未設定の場合は、パスワード設定ダイアログを表示
					if (password == null) {

						final SecretPasswordDialog spd = new SecretPasswordDialog(GridActivity.this);
						spd.setOnDismissListener(new OnDismissListener() {
							
							@Override
							public void onDismiss(DialogInterface dialog) {
								if(spd.mIsSetPassword) {
									
									// シークレットに設定
									setSecret(true);
								}
							}
						});
						spd.show();
						dialog.dismiss();
					} else {
						
						// シークレットに設定
						setSecret(true);
					}
				}
			})
			.setNegativeButton(getString(android.R.string.cancel), null)
			.show();
			
		
		// シークレット解除ダイアログ
		case DIALOG_NO_SECRET:
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.folder_title_setting_no_secret))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage(getString(R.string.folder_message_setting_no_secret_file))
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// シークレット解除
					setSecret(false);
				}
			})
			.setNegativeButton(getString(android.R.string.cancel), null)
			.show();
			
		case DIALOG_INFO:
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.image_context_info))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setView(new ExifView(this, mFilePath))
			.setPositiveButton(android.R.string.ok, null)
			.show();
			
		case DIALOG_CONFIRM_DELETE:
			return new AlertDialog.Builder(this)
			.setTitle(android.R.string.dialog_alert_title)
			.setMessage(R.string.confirm_delete)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					mFileOp.deleteFiles(mFileOpTarget, mFileOpTarget.size());
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
			
		case DIALOG_CONFIRM_MULTI_DELETE:
			return new AlertDialog.Builder(this)
			.setTitle(android.R.string.dialog_alert_title)
			.setMessage(R.string.confirm_delete)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					List<String> target = mAdapter.getSelectedList();
					mFileOp.deleteFiles(target, target.size());
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
		
		// 表示順序ダイアログ
		case DIALOG_SORT:
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.menu_sort))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setItems(getResources().getStringArray(R.array.media_sort), mSetSortListener)
			.show();
		}	
		
		return super.onCreateDialog(id);
	}
	
	
	public class OnTopClickListener implements View.OnClickListener{

		@Override
		public void onClick(View v) {

			switch (v.getId()) {
			
				// カメラ
				case R.id.iconCamera :
					Intent intentCamera = new Intent();
					intentCamera.setAction(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
					startActivity(intentCamera);
					break;
				
				// ホーム画面遷移
				case R.id.iconHome :
					onTapHomeIcon();
					break;
					
//				case R.id.iconMulti:
//					if(mMultiMode){
//						leaveMultiMode();
//					}else{
//						enterMultiMode();
//					}
//					break;
				
				// フィルタリング
//				case R.id.iconFiltering:
//					setGridSize();
////					if(!mMultiMode)toggleSortingMenu();
//					break;
			}
		}
	}

	protected void onTapHomeIcon() {
		// ホーム画面から遷移していた場合、フォルダ一覧を消去
		if (mFlgChangeHome) {
			setResult(ApplicationDefine.RESULT_HOME); 
			finish();
			
		// 新たにホーム画面作成
		} else {
			Intent intentHome = new Intent(GridActivity.this, HomeActivity.class);
			intentHome.putExtra(ApplicationDefine.INTENT_MOVE_FOLDER, false);
			startActivity(intentHome);
		}
	}

	
	/**
	 * グリッドサイズをセットします
	 */
	private void setGridSize() {
		int size = Integer.parseInt(PreferenceUtil.getPreferenceValue(this, ApplicationDefine.PREF_FOLDERGRID_SIZE, "0"));
		
		if (size >= 2) {
			size = 0;
		} else {
			size++;
		}
		
		PreferenceUtil.setPreferenceValue(this, ApplicationDefine.PREF_FOLDERGRID_SIZE, String.valueOf(size));
		
		changeGridSize();
		rescanMedia();
	}
	
	private class SortingListener implements RadioGroup.OnCheckedChangeListener{

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch(checkedId){
			case R.id.rbDateAscender:
				mSortAlgorithm = SORT_DATE_ASC;
				rescanMedia(new JorlleMediaScanner.DateAscender());
				break;
			case R.id.rbDateDescender:
				mSortAlgorithm = SORT_DATE_DESC;
				rescanMedia(new JorlleMediaScanner.DateDescender());
				break;
			case R.id.rbNameAscender:
				mSortAlgorithm = SORT_NAME_ASC;
				rescanMedia(new JorlleMediaScanner.NameAscender());
				break;
			case R.id.rbNameDescender:
				mSortAlgorithm = SORT_NAME_DESC;
				rescanMedia(new JorlleMediaScanner.NameDescender());
				break;
			}
			toggleSortingMenu();
		}
	}
	
	/** ソート設定リスナー */
	DialogInterface.OnClickListener mSetSortListener = new DialogInterface.OnClickListener() {
		
		public void onClick(DialogInterface dialog, int which) {

			Integer checkId = null;
			
			switch(which) {
			
			// 日付昇順
			case SORT_DATE_ASC:
				checkId = R.id.rbDateAscender;
				break;
				
			// 日付降順
			case SORT_DATE_DESC:
				checkId = R.id.rbDateDescender;
				break;
			
			// 名前昇順
			case SORT_NAME_ASC:
				checkId = R.id.rbNameAscender;
				break;
				
			// 名前降順
			case SORT_NAME_DESC:
				checkId = R.id.rbNameDescender;
				break;
			}
			
			if (checkId != null) {
				mSortingView.check(checkId);
				mSortingView.setVisibility(View.GONE);
			}
		}
	};
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent(GridActivity.this, LocalFolderActivity.class);
			Window childActivity = this.getLocalActivityManager().startActivity("down", intent);
			setContentView(childActivity.getDecorView());
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
}