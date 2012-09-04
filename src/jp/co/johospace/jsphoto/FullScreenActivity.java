package jp.co.johospace.jsphoto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import jp.co.johospace.jsphoto.accessor.MediaIndexesAccessor;
import jp.co.johospace.jsphoto.accessor.MediaMetaDataAccessor;
import jp.co.johospace.jsphoto.cache.ImageCache;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.SecretCheckPasswordDialog;
import jp.co.johospace.jsphoto.dialog.SimpleEditDialog;
import jp.co.johospace.jsphoto.fullscreen.ImageSurfaceView;
import jp.co.johospace.jsphoto.fullscreen.ScanUtil;
import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoaderFactory;
import jp.co.johospace.jsphoto.fullscreen.loader.LocalImageLoaderFactory;
import jp.co.johospace.jsphoto.grid.AsyncFileOperation;
import jp.co.johospace.jsphoto.grid.ExifView;
import jp.co.johospace.jsphoto.grid.ExtUtil;
import jp.co.johospace.jsphoto.grid.FavoriteUtil;
import jp.co.johospace.jsphoto.grid.GeoTag;
import jp.co.johospace.jsphoto.grid.ImageShrinker;
import jp.co.johospace.jsphoto.grid.WallpaperHelper;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import jp.co.johospace.jsphoto.util.MediaStoreOperation;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.JsMediaAuth;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;


public class FullScreenActivity extends AbstractFullscreenActivity {

	public static final String RESIZE_SUFFIX = "-resized";
	public static final String INTENT_FILE_PATH_LIST = "filePathList";
	public static final String INTENT_INITIAL_POSITION = "initialPosition";
	public static final String INTENT_SORT = "sortAlgorithm";
	public static final String INTENT_CHANGE = "change";
	
	private boolean mHome;
	private AsyncFileOperation mFileOp;
	protected List<String> mTags;
	private GeoTag mGeoTag;
	private EditText mEditText;
	private ImageShrinker mShrinker;
	protected ImageLoaderFactory mFactory;
	private JorlleMediaScanner mScanner;
	protected int mTagPos;
	private String mRenamedTag;
	private WallpaperHelper mWallpaper;

	/** シークレット設定時パス */
	private String mSecretTag;
	
	/** ファイル操作を行ったパスのリスト */
	private ArrayList<String> mListChangeFile = new ArrayList<String>();
	
	/** データベース */
	private SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();
	
	/** その他オプションメニュー */
	private boolean mIsOther = false;
	
	
	/** 表示順序ダイアログの項目 */
	protected static final int
		SORT_DATE_ASC = 0,
		SORT_DATE_DESC = 1,
		SORT_NAME_ASC = 2,
		SORT_NAME_DESC = 3;
	
	
	private static final int
		DIALOG_INFO = 1,
		DIALOG_CONFIRM_DELETE = 2,
		DIALOG_RENAME = 3,
		DIALOG_SECRET = 4,
		DIALOG_NO_SECRET = 5;
	
	private static final int
		REQUEST_COPY = 1,
		REQUEST_MOVE = 2,
		REQUEST_SHRINK = 3,
		REQUEST_TAG = 4,
		REQUEST_SETTING = 5,
		REQUEST_ROTATE = 6,
		REQUEST_CROP = 7;
	
	protected static final int 
		MENU_SHARE = 1,
		MENU_INFO = 2,
		MENU_MOVE = 3,
		MENU_SHRINK = 4,
		MENU_COPY = 5,
		MENU_OTHER = 6,
		MENU_MOVE_SYNC = 7,
		MENU_REGISTER_FAVORITE = 8,
		MENU_UNREGISTER_FAVORITE = 9,
		MENU_SECRET = 10,
		MENU_UNSECRET = 11,
		MENU_EDIT_TAG = 12,
		MENU_RENAME = 13,
		MENU_MAP = 14,
		MENU_DELETE = 15,
		MENU_SETTING = 16,
		MENU_ROTATE_90 = 17,
		MENU_ROTATE_180 = 18,
		MENU_ROTATE_270 = 19,
		MENU_WALLPAPER = 20,
		MENU_DETAIL = 21,
		MENU_SHRINK_START = 100;
	
	
	protected static final int DIAROG_PROGRESS_VIEW_SYNCED_MEDIA = 10;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mFileOp = new AsyncFileOperation(this);
		
		Intent intent = getIntent();
		
		List<String> path = intent.getStringArrayListExtra(INTENT_FILE_PATH_LIST);
		int pos = intent.getIntExtra(INTENT_INITIAL_POSITION, 0);
		
		if (path != null) {
			mRenamedTag = path.get(pos);
		}

		// スキャニングの結果で判断する
//		//表示するものがない
//		if(path.size()== 0){
//			finish();
//			return;
//		}
		
		mHome = intent.getBooleanExtra(ApplicationDefine.INTENT_CHANGE_HOME, false);
		mTags = path;
		mEditText = new EditText(this);
		
		setContentView(R.layout.fullscreen);

		mFactory = createImageLoaderFactory();
		recreateSurfaceView(pos);
		
		findViewById(R.id.lytListHeader).setVisibility(View.GONE);
		findViewById(R.id.iconHome).setVisibility(View.GONE);
//		findViewById(R.id.iconHome).setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				onTapHomeIcon();
//			}
//		});
		
		prepareInfoLinkOnCreate();
		
		mFileOp.setOnCompleteListener(new AsyncFileOperation.OnCompleteListener() {
			
			@Override
			public void onComplete() {
				recreateSurfaceView(getCurrentNumber());
			}
			
			@Override
			public void onCancel() {
				recreateSurfaceView(getCurrentNumber());
			}
		});
	}

	protected void onTapHomeIcon() {
		if(mHome){
			setResult(ApplicationDefine.RESULT_HOME);
			finish();
		}else{
			Intent intent = new Intent(FullScreenActivity.this, HomeActivity.class);
			startActivity(intent);
		}
	}


	@Override
	protected void onDestroy() {
		if(mSurfaceView != null)mSurfaceView.dispose();
		mSurfaceView = null;
		if(mScanner != null)mScanner.dispose();
		mFileOp.dispose();
		mHandler.removeCallbacksAndMessages(null);
		
		super.onDestroy();
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if(mSurfaceView != null){
//			String name = getCurrentName();
//			int maxName = 0;
//			if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
//				maxName = MAX_NAME_L;
//			}else{
//				maxName = MAX_NAME_P;
//			}
//			if(name.length() > maxName){
//				name = name.substring(0, maxName - 3) + "...";
//			}
//			((TextView)findViewById(R.id.tvHeader)).setText(name);
		}
		
		//recreateSurfaceView(getCurrentNumber());
		
		
		super.onConfigurationChanged(newConfig);
	}


	protected ImageLoaderFactory createImageLoaderFactory() {
		return new LocalImageLoaderFactory();
	}
	
	protected void showInfo() {
		removeDialog(DIALOG_INFO);
		showDialog(DIALOG_INFO);
	}

	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		
		// 共有
		case MENU_SHARE:{
			File f = new File(mSurfaceView.getCurrentTag().toString());
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType(MediaUtil.getMimeTypeFromPath(f.getAbsolutePath()));
			i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
			startActivity(Intent.createChooser(i, getString(R.string.image_context_share)));
			break;
		}
		
		// 詳細
		case MENU_INFO:{
			showInfo();
			break;
		}
		
		// 移動
		case MENU_MOVE:{
			Intent i = new Intent(this, SelectFolderActivity.class);
			i.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.folder_title_change_name));
			i.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
			i.putExtra(SelectFolderActivity.PARAM_START_PATH, new File(mSurfaceView.getCurrentTag().toString()).getParent());
			startActivityForResult(i, REQUEST_MOVE);
			break;
		}
		
		case MENU_SHRINK:{
			if(mShrinker.getAvailableSize().size() == 0)
				Toast.makeText(this, R.string.toast_no_shrink, Toast.LENGTH_SHORT).show();
			break;
		}
		
		// コピー
		case MENU_COPY:{
			Intent i = new Intent(this, SelectFolderActivity.class);
			i.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.folder_copy_title_select));
			i.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
			i.putExtra(SelectFolderActivity.PARAM_START_PATH, new File(mSurfaceView.getCurrentTag().toString()).getParent());
			startActivityForResult(i, REQUEST_COPY);
			break;
		}
		
		// 詳細
		case MENU_DETAIL:{
			ScrollView scrollview = new ScrollView(this);
			scrollview.setLayoutParams(new ViewGroup.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
			scrollview.addView(new ExifView(this, mSurfaceView.getCurrentTag().toString()));
			
			new AlertDialog.Builder(this)
			.setTitle(getString(R.string.image_context_info))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setView(scrollview)
			.setPositiveButton(android.R.string.ok, null)
			.show();
			break;
		}
		
		// シークレット
		case MENU_SECRET:
			showDialog(DIALOG_SECRET);
			break;
		
		// シークレット解除
		case MENU_UNSECRET:
			showDialog(DIALOG_NO_SECRET);
			break;
			
		// 同期フォルダにコピー
		case MENU_MOVE_SYNC: {
			ArrayList<String> path = new ArrayList<String>();
			path.add((mSurfaceView.getCurrentTag().toString()));
			File pathExternalPublicDir = new File(ApplicationDefine.PATH_JSPHOTO);
			if (!pathExternalPublicDir.exists()) pathExternalPublicDir.mkdirs();
			String dir = pathExternalPublicDir.getPath();
			// 移動に成功したらToast表示
			if (mFileOp.copyToFolder(dir, path, 1, false)) {
				Toast.makeText(getApplicationContext(), getString(R.string.image_context_move_sync_success), Toast.LENGTH_SHORT).show();
			}
			
			break;
		}
		
		// その他
		case MENU_OTHER:{
			// OptionMenuが閉じたタイミングで、その他用のメニューを表示してもらいたい
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mIsOther = true;
					openOptionsMenu();
				}
			});
			break;
		}
		
		// お気に入り登録
		case MENU_REGISTER_FAVORITE:{
			
			File fileFavorite = new File(mSurfaceView.getCurrentTag().toString());
			String fileFavoritePath = fileFavorite.getPath();
			
			FavoriteUtil.addFavorite(fileFavorite);
			
			// ファイル操作パスリストに追加
			addChangePathList(fileFavoritePath);
			
			recreateSurfaceView(getCurrentNumber());
			
			startSync();
			break;
		}
		
		// お気に入り解除
		case MENU_UNREGISTER_FAVORITE:{
			
			File fileRemoveFavorite = new File(mSurfaceView.getCurrentTag().toString());
			
			FavoriteUtil.removeFavorite(fileRemoveFavorite);
			
			// ファイル操作パスリストに追加
			addChangePathList(fileRemoveFavorite.getPath());
			
			recreateSurfaceView(getCurrentNumber());
			
			startSync();
			break;
		}
		
		// タグ編集
		case MENU_EDIT_TAG:{
			Intent i = new Intent(this, TagEditActivity.class);
			i.putExtra(ApplicationDefine.INTENT_PATH, mSurfaceView.getCurrentTag().toString());
			startActivityForResult(i, REQUEST_TAG);
			break;
		}
		
		// 名前変更
		case MENU_RENAME: {
			File f = new File(mSurfaceView.getCurrentTag().toString());
			mEditText.setText(ExtUtil.getPureName(f));
			showDialog(DIALOG_RENAME);
			break;
		}
		
		// 位置情報取得
		case MENU_MAP: {
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(mGeoTag.toGeoLocation()));
			startActivity(i);
			break;
		}
		
		// 削除
		case MENU_DELETE: {
			showDialog(DIALOG_CONFIRM_DELETE);
			break;
		}
		
		// 設定
		case MENU_SETTING: {
			Intent i = new Intent(this, JorllePrefsActivity.class);
			startActivityForResult(i, REQUEST_SETTING);
			break;
		}
		
		// 回転90度
		case MENU_ROTATE_90:{
			startRotate(ImageOpActivity.ROTATE_90);
			break;
		}
		
		// 回転180度
		case MENU_ROTATE_180:{
			startRotate(ImageOpActivity.ROTATE_180);
			break;
		}
		
		// 回転270度
		case MENU_ROTATE_270:{
			startRotate(ImageOpActivity.ROTATE_270);
			break;
		}
		
		// 壁紙
		case MENU_WALLPAPER:{
			mWallpaper = new WallpaperHelper();
			mWallpaper.startCropActivity(this, mSurfaceView.getCurrentTag().toString(), REQUEST_CROP);
			break;
		}
		
		

		}//end switch
		
		if(item.getItemId() >= MENU_SHRINK_START){
			int n = item.getItemId() - MENU_SHRINK_START;
			ImageShrinker.Size size = mShrinker.getAvailableSize().get(n);
			Intent i = new Intent(getApplicationContext(), ImageOpActivity.class);
			i.putExtra(ImageOpActivity.INTENT_TARGET_PATH, mSurfaceView.getCurrentTag().toString());
			i.putExtra(ImageOpActivity.INTENT_RESIZE, true);
			i.putExtra(ImageOpActivity.INTENT_RESIZE_HEIGHT, size.height);
			i.putExtra(ImageOpActivity.INTENT_RESIZE_WIDTH, size.width);
			i.putExtra(ImageOpActivity.INTENT_TITLE, getString(R.string.title_shrink));
			mRenamedTag = mSurfaceView.getCurrentTag().toString();
			startActivityForResult(i, REQUEST_SHRINK);
		}
		
		return super.onOptionsItemSelected(item);
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

	private void startRotate(int tag){
		Intent i = new Intent(getApplicationContext(), ImageOpActivity.class);
		i.putExtra(ImageOpActivity.INTENT_ROTATE, true);
		i.putExtra(ImageOpActivity.INTENT_ROTATE_ORIENTATION, tag);
		i.putExtra(ImageOpActivity.INTENT_TARGET_PATH, mSurfaceView.getCurrentTag().toString());
		i.putExtra(ImageOpActivity.INTENT_TITLE, getString(R.string.image_context_rotate));
		
		mRenamedTag = mSurfaceView.getCurrentTag().toString();
		
		startActivityForResult(i, REQUEST_ROTATE);
	}

	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// 戻る
		if (keyCode == KeyEvent.KEYCODE_BACK){
			
			// 操作があったファイルパスを画像一覧へ返す
			if (mListChangeFile.size() > 0) {
				Intent renameIntent = new Intent();
				renameIntent.putExtra(INTENT_CHANGE, mListChangeFile);
				setResult(RESULT_OK, renameIntent);
			}
			
			onBackKey();
		// メニュー
		} else if (keyCode == KeyEvent.KEYCODE_MENU) {
			mIsOther = false;
			openOptionsMenu();
		}
		
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		
		if(mSurfaceView == null)return false;
		
		mGeoTag = new GeoTag(mSurfaceView.getCurrentTag().toString());
		mShrinker = new ImageShrinker(mSurfaceView.getCurrentTag().toString());
		
		File f = new File(mSurfaceView.getCurrentTag().toString());
		
		String checkString;
		
		if (ExtUtil.isSecret(f)) {
			checkString = ExtUtil.unSecret(f).getPath();
		} else {
			checkString = f.getPath();
		}
		
		// 通常オプションメニュー
//		if (!mIsOther) {
			// タグ編集
			MenuItem menuTag = menu.add(0, MENU_EDIT_TAG, Menu.NONE, R.string.menu_edit_tag);
			menuTag.setIcon(R.drawable.ic_tag);
			// 共有
			MenuItem menuShare = menu.add(0, MENU_SHARE, Menu.NONE, R.string.image_context_share);
			menuShare.setIcon(R.drawable.ic_share);
			// 削除
			MenuItem menuDelete = menu.add(0, MENU_DELETE, Menu.NONE, R.string.image_context_delete);
			menuDelete.setIcon(R.drawable.ic_delete);
			// 詳細
			MenuItem menuDetail = menu.add(0, MENU_DETAIL, Menu.NONE, R.string.menu_detail);
			menuDetail.setIcon(R.drawable.ic_detail);
			
			// 移動
			MenuItem menuMove = menu.add(0, MENU_MOVE, Menu.NONE, R.string.image_context_move);
			menuMove.setIcon(R.drawable.ic_move);
			
			// コピー
			MenuItem menuCopy = menu.add(0, MENU_COPY, Menu.NONE, R.string.image_context_copy);
			
			// 同期フォルダへコピー
			menu.add(0, MENU_MOVE_SYNC, Menu.NONE, R.string.image_context_move_sync);

			// お気に入り 状態によって表示項目を変更
			menu.add(0, 
					
					(FavoriteUtil.isFavorite(f)?
							MENU_UNREGISTER_FAVORITE:
							MENU_REGISTER_FAVORITE),
							Menu.NONE,
					getString((FavoriteUtil.isFavorite(f))? 
							R.string.image_context_unregister_favorite: 
							R.string.image_context_register_favorite));
			
			// シークレット
			if(ExtUtil.isSecret(f)){
				menu.add(0, MENU_UNSECRET, Menu.NONE, R.string.image_context_unsecret);
			}else{
				menu.add(0, MENU_SECRET, Menu.NONE, R.string.image_context_to_secret);
			}
			
			// 名前変更
			menu.add(0, MENU_RENAME, Menu.NONE, R.string.menu_rename);
			
			// jpeg時のみローテートメニューを表示
			if(MediaUtil.getMimeTypeFromPath(checkString).equals("image/jpeg")){
				SubMenu rotate = menu.addSubMenu(getString(R.string.image_context_rotate));
				
				rotate.add(0, MENU_ROTATE_90, Menu.NONE, getString(R.string.image_context_rotate_90));
				rotate.add(0, MENU_ROTATE_180, Menu.NONE, getString(R.string.image_context_rotate_180));
				rotate.add(0, MENU_ROTATE_270, Menu.NONE, getString(R.string.image_context_rotate_270));
			}
			
			// 壁紙
			if (!ExtUtil.isSecret(f)) {
				menu.add(0, MENU_WALLPAPER, Menu.NONE, R.string.image_context_register);
			}
			
//			// その他
//			MenuItem menuOther = menu.add(0, MENU_OTHER, Menu.NONE, R.string.menu_other);
//			menuOther.setIcon(R.drawable.other);
			
//		// その他オプションメニュー TODO
//		} else {
//			// 削除
//			MenuItem menuDelete = menu.add(0, MENU_DELETE, Menu.NONE, R.string.image_context_delete);
//			menuDelete.setIcon(R.drawable.ic_delete);
//			// 詳細
//			MenuItem menuDetail = menu.add(0, MENU_DETAIL, Menu.NONE, R.string.menu_detail);
//			menuDetail.setIcon(R.drawable.ic_detail);
//		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){		
		case DIAROG_PROGRESS_VIEW_SYNCED_MEDIA: {
			ProgressDialog d = new ProgressDialog(this);
			d.setTitle(R.string.fullscreen_view_synced_progress);
			d.setCancelable(false);
			return d;
		}
		case DIALOG_INFO:
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.image_context_info))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setView(new ExifView(this, mSurfaceView.getCurrentTag().toString()))
			.setPositiveButton(android.R.string.ok, null)
			.show();
			
		case DIALOG_CONFIRM_DELETE:
			return new AlertDialog.Builder(this)
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.confirm_delete)
				.setPositiveButton(android.R.string.cancel, null)
				.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						List<String> l = new ArrayList<String>();
						if(mSurfaceView != null)l.add(mSurfaceView.getCurrentTag().toString());
						mFileOp.deleteFiles(l, l.size());
						
						// ファイル操作パスリストに追加
						addChangePathList(mSurfaceView.getCurrentTag().toString());
					}
				})
				.show();
			
		case DIALOG_RENAME:
			
			final SimpleEditDialog dialog = new SimpleEditDialog(FullScreenActivity.this);
			dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			dialog.mTxtTitle.setText(R.string.menu_rename);
			dialog.mTxtView.setVisibility(View.GONE);
			dialog.mTxtEdit.setText(mEditText.getText().toString().trim());
			
			dialog.mBtnOk.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View view) {
					
					if(mSurfaceView == null){	
						Toast.makeText(FullScreenActivity.this, R.string.toast_failed_change_name, Toast.LENGTH_SHORT).show();
						return;
					}
					
					String rename = dialog.mTxtEdit.getText().toString().trim();
					rename = rename.replaceAll("[　|\\s]+", "");
					
					// FIXME 255文字以内なのに、androidで見えなくなってしまう。暫定対応として100文字制限をかけます。
					if (rename.length() == 0 || rename.length() > 100) {
						Toast.makeText(FullScreenActivity.this, R.string.toast_failed_change_name, Toast.LENGTH_SHORT).show();
						return;
					}
					
					File from = new File(mSurfaceView.getCurrentTag().toString());
					
//					File to = new File(from.getParentFile(), mEditText.getText().toString() + ExtUtil.getExt(from));
					File to = new File(from.getParentFile(), rename + ExtUtil.getExtWithSecret(from));
					
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
					
					if(from.renameTo(to)){	
						SQLiteDatabase dbExternal = OpenHelper.external.getDatabase();
						SQLiteDatabase dbCache = OpenHelper.cache.getDatabase();
						MediaMetaDataAccessor.updateMetaDataName(dbExternal, from.getParent(), from.getName(), to.getName());
						MediaIndexesAccessor.updateIndexesName(dbCache, from.getParent(), from.getName(), to.getName());
						mRenamedTag = to.getAbsolutePath();
						recreateSurfaceView(0);
						
						// ファイル操作パスリストに追加
						addChangePathList(from.getPath());
						
						MediaStoreOperation.scanAndDeleteMediaStoreEntry(FullScreenActivity.this, from, to, false);
					}else{
						Toast.makeText(FullScreenActivity.this, R.string.toast_failed_change_name, Toast.LENGTH_SHORT).show();
					}
					dialog.dismiss();
				}
			});
			
			//TODO ひとまず、テキストチェックは保留
//			dialog.mTxtEdit.addTextChangedListener(dialog.mTextWatcher);
			
			dialog.mBtnChansel.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View view) {
					dialog.dismiss();
				}
			});
			dialog.show();
			break;
			
//			return new AlertDialog.Builder(this)
//				.setTitle(R.string.menu_rename)
//				.setIcon(android.R.drawable.ic_dialog_info)
//				.setView(mEditText)
//				.setPositiveButton(android.R.string.cancel, null)
//				.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//					
//					@Override
//					public void onClick(DialogInterface arg0, int arg1) {
//						if(mEditText.getText().toString().length() == 0){
//							Toast.makeText(FullScreenActivity.this, R.string.toast_failed_change_name, Toast.LENGTH_SHORT).show();
//							return;
//						}
//						
//						if(mSurfaceView == null){	
//							Toast.makeText(FullScreenActivity.this, R.string.toast_failed_change_name, Toast.LENGTH_SHORT).show();
//							return;
//						}
//						
//						File from = new File(mSurfaceView.getCurrentTag().toString());
//						File to = new File(from.getParentFile(), mEditText.getText().toString() + ExtUtil.getExt(from));
//						
//						if(from.renameTo(to)){	
//							SQLiteDatabase db = OpenHelper.external.getDatabase();
//							MediaIndexesAccessor.updateIndexesName(db, from.getParent(), from.getName(), to.getName());
//							MediaMetaDataAccessor.updateMetaDataName(db, from.getParent(), from.getName(), to.getName());
//							mRenamedTag = to.getAbsolutePath();
//							recreateSurfaceView(0);
//							
//							Intent renameIntent = new Intent();
//							renameIntent.putExtra("rename", from.getPath());
//							setResult(RESULT_OK, renameIntent);
//							
//						}else{
//							Toast.makeText(FullScreenActivity.this, R.string.toast_failed_change_name, Toast.LENGTH_SHORT).show();
//						}
//					}
//				})
//				.show();
			
		// シークレット設定ダイアログ
		case DIALOG_SECRET:
			
			
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.folder_title_setting_secret_file))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage(getString(R.string.folder_message_setting_secret_file))
			.setPositiveButton(getString(android.R.string.cancel), null)
			.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					// シークレットに設定
					setSecret(true);
					
					/*
					String password = PreferenceUtil.getPreferenceValue(FullScreenActivity.this, ApplicationDefine.KEY_SECRET_PASSWORD, null);
					
					// パスワード未設定の場合は、パスワード設定ダイアログを表示
					if (password == null) {

						final SecretPasswordDialog spd = new SecretPasswordDialog(FullScreenActivity.this);
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
					}*/
				}
			})
			.show();
			
		
		// シークレット解除ダイアログ
		case DIALOG_NO_SECRET:
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.folder_title_setting_no_secret))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage(getString(R.string.folder_message_setting_no_secret_file))
			.setPositiveButton(getString(android.R.string.cancel), null)
			.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String password = PreferenceUtil.getPreferenceValue(FullScreenActivity.this, ApplicationDefine.KEY_SECRET_PASSWORD, null);
					
					// パスワード設定済みの場合は、パスワード確認ダイアログを表示
					if (password != null) {

						final SecretCheckPasswordDialog spd = new SecretCheckPasswordDialog(FullScreenActivity.this);
						spd.setOnDismissListener(new OnDismissListener() {
							
							@Override
							public void onDismiss(DialogInterface dialog) {
								if(spd.mIsSetPassword) {
									
									// シークレット解除に設定
									setSecret(false);
								}
							}
						});
						spd.show();
						dialog.dismiss();
					} else {
						// シークレット解除に設定
						setSecret(false);
					}
				}
			})
			.show();
		}
		return super.onCreateDialog(id);
	}

	
	/**
	 * 画像のシークレット状態を操作します
	 * 
	 * @param isSecret true:シークレットに設定	false:シークレット解除
	 */
	private void setSecret(boolean isSecret) {
		
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(FullScreenActivity.this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, true);
		
		File from = new File(mSurfaceView.getCurrentTag().toString());
		
		// シークレット表示時は、位置再検索のためファイル名を保持
		if (secretState) {
			
			// シークレット処理の内容によって、位置検索時のファイル名を生成
			if (isSecret) {
				mSecretTag = ExtUtil.toSecret(from).getPath();
			} else {
				mSecretTag = ExtUtil.unSecret(from).getPath();
			}
		}
		
		ArrayList<String> resultList = (ArrayList<String>) MediaUtil.setSecret(mDatabase, isSecret, mSurfaceView.getCurrentTag().toString(), false, null);
		if (resultList.size() > 0) {
			// シークレットに成功
			if (isSecret) {
				MediaStoreOperation.deleteMediaStoreEntry(getApplicationContext(), new File(mSurfaceView.getCurrentTag().toString()));
				Toast.makeText(FullScreenActivity.this, getString(R.string.folder_message_setting_secret_success), Toast.LENGTH_SHORT).show();
			// シークレット解除に成功
			} else {
				MediaUtil.scanMedia(getApplicationContext(), new File(resultList.get(0)), false);
				Toast.makeText(FullScreenActivity.this, getString(R.string.folder_message_setting_no_secret_success), Toast.LENGTH_SHORT).show();
			}
		} else {
			// シークレットに失敗
			if (isSecret) {
				Toast.makeText(FullScreenActivity.this, getString(R.string.folder_message_setting_secret_failure), Toast.LENGTH_SHORT).show();
			// シークレット解除に失敗
			} else {
				Toast.makeText(FullScreenActivity.this, getString(R.string.folder_message_setting_no_secret_failure), Toast.LENGTH_SHORT).show();
			}
		}
		
		
		// ファイル操作パスリストに追加
		addChangePathList(from.getPath());
		
		recreateSurfaceView(mTagPos);
	}
	
	/**
	 * ファイル操作パスリストに、パスを追加します
	 * 
	 * @param path	追加パス
	 */
	private void addChangePathList(String path) {
		
		if (path == null) return;
		
		if (!mListChangeFile.contains(path)) mListChangeFile.add(path);
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
		case REQUEST_COPY:{
			if(resultCode == RESULT_CANCELED) break;
			ArrayList<String> list = new ArrayList<String>();
			if(mSurfaceView != null)list.add(mSurfaceView.getCurrentTag().toString());			
			mFileOp.copyToFolder(data.getStringExtra(SelectFolderActivity.RESULT_PATH), list, list.size(), true);
			break;
		}
		case REQUEST_MOVE:{
			if(resultCode == RESULT_CANCELED) break;
			ArrayList<String> list = new ArrayList<String>();
			if(mSurfaceView != null)list.add(mSurfaceView.getCurrentTag().toString());			
			mFileOp.moveToFolder(data.getStringExtra(SelectFolderActivity.RESULT_PATH), list, list.size());
			break;
		}
		case REQUEST_SHRINK:{
			break;
		}
		case REQUEST_ROTATE:{
			File media = new File(mSurfaceView.getCurrentTag().toString());
			OpenHelper.cache.getDatabase().delete(ImageCache.$TABLE,
					ImageCache.DIRPATH + " = ? AND " + ImageCache.NAME + " = ?",
					new String[] {media.getParent(), media.getName()});
			break;
		}
		
		case REQUEST_CROP:{
			mWallpaper.onActivityResult(this, resultCode);
			break;
		}
		}
		
		recreateSurfaceView(getCurrentNumber());
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private int getCurrentNumber(){
		if(mSurfaceView == null){
			return 0;
		}else{
			return mSurfaceView.getCurrentNumber();
		}
	}
	
	/**
	 * 現在指している要素をタグ情報から削除してサーフェイスビューを再生成
	 * 
	 */
	private void deleteAndRecreateSurfaceView(){
		int pos = getCurrentNumber();
		if(mTags.size() == 1){
			//もう表示するものがない
			finish();
		}else{
			mTags.remove(pos);
			if(pos != 0)pos--;
			recreateSurfaceView(pos);
		}
	}


	protected void recreateSurfaceView(int pos) {
		FrameLayout frame = (FrameLayout)findViewById(R.id.flFullscreen);
		frame.removeAllViews();
		if(mSurfaceView != null)mSurfaceView.dispose();
		
		mTagPos = pos;
		mTags = new ArrayList<String>();

		
		if(mScanner != null)mScanner.dispose();
		mScanner = new JorlleMediaScanner();
		ScanUtil.setParamByIntent(this, mScanner, getIntent());

		switch(getIntent().getIntExtra(INTENT_SORT, SORT_DATE_ASC)){
		case SORT_DATE_ASC:
			mScanner.sort(new JorlleMediaScanner.DateAscender());
			break;
		case SORT_DATE_DESC:
			mScanner.sort(new JorlleMediaScanner.DateDescender());
			break;
		case SORT_NAME_ASC:
			mScanner.sort(new JorlleMediaScanner.NameAscender());
			break;
		case SORT_NAME_DESC:
			mScanner.sort(new JorlleMediaScanner.NameDescender());
			break;
			
		}
		
		// 隠しフォルダ表示状態
		boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, true);
		mScanner.scanNomedia(hiddenState).scanSecret(secretState);
		
		
		mScanner.findMedia(mediaListener);
	}

	private List<String> removeVideo(List<String> path){
		List<String> result = new ArrayList<String>();
		
		for(String pathStr: path){
			if(isFullScreenTarget(pathStr)){
				result.add(pathStr);
			}
		}
		return result;
	}
	
	protected boolean isFullScreenTarget(String tag) {
		return !ExtUtil.isVideo(tag);
	}
	
	
	private JorlleMediaScanner.OnFoundListener mediaListener = new JorlleMediaScanner.OnFoundListener() {
		
		@Override
		public void onStartFolder(File folder) {
			
		}
		
		@Override
		public void onFound(File file) {
			if(!ExtUtil.isVideo(file.getAbsolutePath())){
				mTags.add(file.getAbsolutePath());
			}
		}
		
		@Override
		public void onEndFolder(File folder, int size) {
			
		}
		
		@Override
		public void onComplete() {		
			if(mTags.size() == 0){
				finish();
				return;
			}
			if(mRenamedTag != null){
				//リネーム後だったら、リネーム位置を探す
				mTagPos = mTags.indexOf(mRenamedTag);
				if(mTagPos == -1){
					mTagPos = 0;
				}
				mRenamedTag = null;
			}
			if(mTags.size() <= mTagPos){
				mTagPos = mTags.size() - 1;
			}
			
			// シークレット処理後だった場合、位置を探す
			if (mSecretTag != null) {
				mTagPos = mTags.indexOf(mSecretTag);
				if (mTagPos == -1) {
					mTagPos = 0;
				}
				mSecretTag = null;
			}
			
			FrameLayout frame = (FrameLayout)findViewById(R.id.flFullscreen);

			frame.addView(
				mSurfaceView = new ImageSurfaceView(FullScreenActivity.this, mFactory, mTags, mTagPos)
			);
			
			startInfoLinkNavigation();
		}
	};
	
	@Override
	protected MediaIdentifier getCurrentMedia() {
		JsMediaAuth auth = new JsMediaAuth(this);
		JsMediaAuth.Credential credential = auth.loadCredential();
		if (credential != null) {
			return new MediaIdentifier(ServiceType.JORLLE_LOCAL,
					credential.deviceId,
					mSurfaceView.getCurrentTag().toString());
		} else {
			return null;
		}
	}
	
	@Override
	protected Long getCurrentDate() {
		long currentDate = getExifDateTime(new File(mSurfaceView.getCurrentTag().toString()));
		// TODO Galaxy Nexus初期バージョンのみUTCが入っている為対応(アップデート前はSDK_VERSIONは14、後は15）
		if(Build.MODEL.equals("Galaxy Nexus") && Build.VERSION.SDK_INT <= 14) {
			currentDate += TimeZone.getDefault().getRawOffset();
		}
		return currentDate;
	}
}
