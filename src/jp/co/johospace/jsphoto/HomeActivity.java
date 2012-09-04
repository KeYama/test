package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.co.johospace.jsphoto.accessor.MediaIndexesAccessor;
import jp.co.johospace.jsphoto.accessor.MediaMetaDataAccessor;
import jp.co.johospace.jsphoto.accessor.TagMasterAccessor;
import jp.co.johospace.jsphoto.cache.PicasaCache;
import jp.co.johospace.jsphoto.cache.PicasaCache.MediaEntry;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.CMediaTagMaster;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.CategoryAddDialog;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner.OnFoundListener;
import jp.co.johospace.jsphoto.util.DateUtil;
import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.view.CategoryView;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * ホーム画面のアクティビティです
 */
public class HomeActivity extends Activity implements OnClickListener, OnDismissListener {
	
	/** メニューID */
	private static final int MENU_CATEGORY_SETTING = 0;
	private static final int MENU_ITEM_SETTING = 1;
	
	/** メディア画像　取得件数 */
	private static final int MEDIA_CALLBACK = 1;
	
	/** インデックス　お気に入り */
	private static final int
		INDEX_FAVORITE_ID = 0,
		INDEX_FAVORITE_DIRPATH = 1,
		INDEX_FAVORITE_NAME = 2;

	/** インデックス　タグ種類検索 */
	private static final int
		INDEX_TAG_METADATA = 1;

	/** インデックス　タグ名検索 */
	private static final int
		INDEX_TAG_SINGLE_ID = 0,
		INDEX_TAG_SINGLE_DIRPATH = 1,
		INDEX_TAG_SINGLE_NAME = 2;
	
	/** DBアクセス */
	private static SQLiteDatabase mDatabase;
	
	/** メディアスキャナ ローカル */
	private JorlleMediaScanner mLocalScanner;
	/** メディアスキャナ　同期 */
	private JorlleMediaScanner mSyncScanner;
	/** メディアスキャナ シークレット */
	private JorlleMediaScanner mSecretScanner;
	
	/** ソート条件　日付の新しい順 */
	private DateAscender mDateAscender = new DateAscender();
	
	/** カテゴリ状態設定ダイアログ */
	private CategoryAddDialog mCategoryDialog;
	
	/** ウィンドウマネージャ */
	private WindowManager wm; 
	
	/** ディスプレイ */
	private Display display; 
	
	/** カメラアイコン */
	private ImageView mImageCamera;
	
	/** レイアウトパラメータ */
	private LayoutParams mLayoutParams;
	
	/** カテゴリレイアウト */
	private LinearLayout mLytCategory;
	
	/** ローカルカテゴリーのビュー */
	private CategoryView mLocal;
	/** 同期カテゴリーのビュー */
	private CategoryView mSync;
	/** シークレットカテゴリーのビュー */
	private CategoryView mSecret;
	
	
	/** カテゴリ状態設定ボタン */
	private Button mBtnCategorySetting;
	
	/** 描画領域　長さ */
	private Integer mParentLength;
	
	/** フォルダ情報リスト */
	private List<FolderEntry> mEntryLocal = new ArrayList<FolderEntry>();	
	private List<FolderEntry> mEntrySync = new ArrayList<FolderEntry>();
	private List<FolderEntry> mEntrySecret = new ArrayList<FolderEntry>();
	
	/** メディア総数 */
	private int mSyncMediaCount = 0;
	private int mSecretMediaCount = 0;
	
	/** 重複判定フラグ */
	private boolean mIsDuplication = false;

	/** 初回起動時かどうかのキー */
	private static final String KEY_NOT_FIRST_TIME = "notFirstTime";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		//初回起動時、イントロ画面を表示する
		boolean notFirst = PreferenceUtil.getBooleanPreferenceValue(this, KEY_NOT_FIRST_TIME);
		if(!notFirst){
			PreferenceUtil.setBooleanPreferenceValue(this, KEY_NOT_FIRST_TIME, true);
			startActivity(new Intent(this, IntroActivity.class));
		}
		
		// 初期起動時、表示画面の設定値を取得
		String initKey = PreferenceUtil.getPreferenceValue(this, ApplicationDefine.KEY_INIT_ACTIVITY, ApplicationDefine.INIT_ACTIVITY_HOME);
		
		// フォルダ一覧、ツールバーアイコンから遷移した場合の、判断フラグを取得
		Intent intent = getIntent();
		boolean moveFolder = intent.getBooleanExtra(ApplicationDefine.INTENT_MOVE_FOLDER, true);

		
		// SDカードの判定
		if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
			// DB取得・もしくは作成
			mDatabase = OpenHelper.external.getDatabase();
		} else {
			
			// 存在しない場合は、別画面を起動し、処理停止
			Intent intentNoMount = new Intent(this, NoMountActivity.class);
//			intent.setClass(HomeActivity.this, NoMountActivity.class);
			
			startActivity(intentNoMount);
			finish();
			return;
		}
		
		// アプリ起動時のみ、DBの同期処理を実行
		if (moveFolder) {
			FileCheckTask checkTask = new FileCheckTask();
			checkTask.execute();
		}
		
		// アプリ起動時のみ、DBの同期処理を実行
		if (moveFolder) {
			FileCheckTask checkTask = new FileCheckTask();
			checkTask.execute();
		}
	
		
		// 初期表示値の設定がフォルダ、かつフォルダ一覧画面から遷移していない場合
		if (initKey.equals(ApplicationDefine.INIT_ACTIVITY_FOLDER) && moveFolder) {
			
			// フォルダ一覧に遷移
			Intent intentFolder = new Intent(this, LocalFolderActivity.class);
			intentFolder.putExtra(ApplicationDefine.INTENT_CHANGE_HOME, true);
			intentFolder.putExtra(ApplicationDefine.INTENT_INIT_FILTER, true);
			startActivityForResult(intentFolder, ApplicationDefine.REQUEST_LOCAL);
		}
		
		setContentView(R.layout.home);
		
		// 初期化処理
		init();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// 次回起動時の初期表示画面を、ホームに設定
		PreferenceUtil.setPreferenceValue(this, ApplicationDefine.KEY_INIT_ACTIVITY, ApplicationDefine.INIT_ACTIVITY_HOME);
		
		setCategory();
	}
	
	@Override
	protected void onDestroy() {
		if (mLocalScanner != null) {
		mLocalScanner.dispose();
		}
		super.onDestroy();
	}
	
	
	
	/**
	 * 初期化処理
	 */
	public void init() {
		
		// カメラアイコン
		mImageCamera = (ImageView) findViewById(R.id.iconCamera);
		
		// レイアウト
		mLytCategory = (LinearLayout) findViewById(R.id.lytCategory);
		
		// ボタン
//		mBtnCategorySetting = (Button) findViewById(R.id.btnCategorySetting);
		
		// リスナー設定
		mImageCamera.setOnClickListener(this);
//		mBtnCategorySetting.setOnClickListener(this);
		
		// 画像のサイズ取得・設定
		setDisplayLength();
	}
	
	
	
    /**
	 * メニュー作成イベント
	 * @param menu
	 * @return true:成功、false:失敗
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_CATEGORY_SETTING, 0, getString(R.string.home_menu_category_setting)).setIcon(R.drawable.ic_other);
		menu.add(0, MENU_ITEM_SETTING, 0, getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);

		return true;
	}

	
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		// カメラアイコン
		case R.id.iconCamera:
			
			Intent intentCamera = new Intent();
			intentCamera.setAction(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
			startActivity(intentCamera);
			break;
		
		// カテゴリ追加ボタン
//		case R.id.btnCategorySetting:
//
//			if (!mIsDuplication) {
//
//				mIsDuplication = true;
//				
//				// 各カテゴリの選択状態取得
//				mCategoryDialog = new CategoryAddDialog(this, getVisibility());
//				
//				mCategoryDialog.setOnDismissListener(this);
//				mCategoryDialog.show();
//			}
//			
//			break;
		}	
	}

	
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		
		mIsDuplication = false;
		
		if (dialog == mCategoryDialog) {
			// 再描画
			setVisibility();
		}
	}
	
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		case MENU_CATEGORY_SETTING:
			// カテゴリ選択ダイアログ
			mCategoryDialog = new CategoryAddDialog(this, getVisibility());
			mCategoryDialog.setOnDismissListener(this);
			mCategoryDialog.show();
			break;
		
		// メニュー　設定
		case MENU_ITEM_SETTING:
			Intent intent = new Intent();
			intent = new Intent(HomeActivity.this, JorllePrefsActivity.class);
			startActivity(intent);
			break;
		}
		return true;
	}
	
	
	
	/**
	 * 各カテゴリの表示状態を取得します
	 */
	public List<CategoryState> getVisibility() {
		
		List<CategoryState> listState = new ArrayList<CategoryState>();
		
		int size = mLytCategory.getChildCount();
		
		// レイアウトの子要素を取得
		for (int i = 0; i < size; i++) {
			CategoryView cv = (CategoryView) mLytCategory.getChildAt(i);
			CategoryState cs = new CategoryState(cv.mCategoryName, cv.mDisplayName, cv.mVisivility, cv.mIsTag);
			
			listState.add(cs);
		}
		
		return listState;
	}
	
	
	
	/**
	 * 各カテゴリの表示状態を設定します
	 */
	public void setVisibility() {
		
		// レイアウト内のカテゴリビューの数
		int size = mLytCategory.getChildCount();
		
		for (int i = 0; i < size; i++) {
			CategoryView cv = (CategoryView) mLytCategory.getChildAt(i);
			
			String categoryName = cv.mCategoryName;
			
			// 表示状態をDB、設定値として保持
			if (cv.mIsTag) {
				cv.mVisivility = TagMasterAccessor.getTagHide(mDatabase, categoryName);
			} else {
				cv.mVisivility = PreferenceUtil.getBooleanPreferenceValue(this, categoryName, true);
			}
			
			// カテゴリの表示状態をセット
			if (cv.mVisivility) {
				cv.setVisibility(View.VISIBLE);
			} else {
				cv.setVisibility(View.GONE);
			}
			
			// 該当カテゴリを削除し、設定しなおす
			mLytCategory.removeViewAt(i);
			mLytCategory.addView(cv, i);
		}
	}
	
	
	
	/**
	 * 各カテゴリのレイアウトを設定します
	 */
	public void setCategory() {

		// カテゴリのレイアウトを初期化
		mLytCategory.removeAllViews();
		
		// フォルダ情報リストを初期化
		mEntryLocal.clear();
		mEntrySync.clear();
		mEntrySecret.clear();
		
		mSyncMediaCount = 0;
		mSecretMediaCount = 0;
		
		// ローカル
		setLayoutLocal();
		// 同期フォルダ
		setLayoutSync();
		// シークレット
		setLayoutSecret();
		// Picasa参照
		setLayoutPicasa();
		// お気に入り
		setLayoutFavorite();
		// タグ
		setLayoutTag();
		
		// 表示状態設定
		setVisibility();
	}
	
	
	
	/**
	 * カテゴリ　ローカル設定
	 */
	public void setLayoutLocal() {
		
		// 隠しフォルダ表示状態
		boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false);
		
		// 画像フォルダ・画像一覧をスキャニング
		mLocalScanner = new JorlleMediaScanner();
		mLocalScanner.sort(new JorlleMediaScanner.DateDescender()).maxCallback(MEDIA_CALLBACK).scanSubfolder(true).scanNomedia(hiddenState).scanSecret(secretState).findMedia(mLocalListener);

		mLocal = new CategoryView(this, mLayoutParams);
		
		String localText = getString(R.string.home_label_local);
		
		mLocal.setCategoryName(ApplicationDefine.KEY_CATEGORY_LOCAL);
		mLocal.setHeaderName(localText);
		mLocal.setTextTop(localText);
		
		// 描画領域を表示するため、最初にダミー画像を表示
		mLocal.setDummyImage(mParentLength);
		
		boolean visible = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_CATEGORY_LOCAL, true);
		
		mLocal.setStatusVisible(visible);
		
		mLytCategory.addView(mLocal);
	}
	
	
	
	/**
	 * ローカルカテゴリの詳細データ
	 */
	public void setLocalData() {
		
		if (mLocal == null) return;
		
		String dataText = null;
		
		// フォルダの中で最新の一件の情報を取得
		if (mEntryLocal.size() > 0) {
			
			FolderEntry fe = mEntryLocal.get(0);
	
			if (fe.images.size() > 0) {
				
				File image = new File(fe.images.get(0));
				mLocal.setImage(fe.path.getPath(), image.getName(), mParentLength);
				
				// 最新更新日付の取得・設定
				dataText = DateUtil.getDateString(image.lastModified());
			}
			
			
			// カテゴリ詳細レイアウトにクリックリスナをセット
			mLocal.setCategoryClickListener(mLocalClickListener);
		}
		
		// フォルダ数の取得・設定
		String countText = getString(R.string.home_label_folder_count) + " (" + mEntryLocal.size() + ")";

		if (dataText == null) {
			dataText = countText;
		} else {
			dataText = dataText + "\n" + countText;
		}
		
		mLocal.setTextData(dataText);
	}
	
	
	
	/**
	 * カテゴリ　同期フォルダ設定
	 */
	public void setLayoutSync() {
		
		mSync = new CategoryView(this, mLayoutParams);
		
		String syncText = getString(R.string.home_label_sync);
		
		mSync.setCategoryName(ApplicationDefine.KEY_CATEGORY_SYNC);
		mSync.setHeaderName(syncText);
		
		// 描画領域を表示するため、最初にダミー画像を表示
		mSync.setDummyImage(mParentLength);
		
		// 同期フォルダ未設定だった場合は、メッセージを表示させ、設定画面への遷移を行う
		if (!PicasaPrefsActivity.isSyncable(this)) {
			
			mSync.setImage(R.drawable.ic_share, mParentLength);
			mSync.setTextTop(getString(R.string.home_message_no_sync));
			mSync.setCategoryClickListener(mNoSyncClickListener);
			
		} else {
			// 設定された同期フォルダを取得
			String syncLocal = PreferenceUtil.getPreferenceValue(this, PicasaPrefsActivity.PREF_PICASA_SYNC_LOCAL, null);
			
			if (syncLocal == null) return;
			
			// 隠しフォルダ表示状態
			boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
			// シークレット表示状態
			boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false);
			
			
			// 同期フォルダをスキャニング
			mSyncScanner = new JorlleMediaScanner();
			mSyncScanner.sort(new JorlleMediaScanner.DateDescender()).maxCallback(MEDIA_CALLBACK).
									scanSubfolder(false).scanNomedia(hiddenState).scanSecret(secretState).
									baseFolder(syncLocal).findMedia(mSyncListener);
		}
		
		boolean visible = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_CATEGORY_SYNC, true);
		
		mSync.setStatusVisible(visible);
		
		mLytCategory.addView(mSync);
	}
	
	
	
	/**
	 * 同期カテゴリの詳細データ
	 */
	public void setSyncData() {
		
		if (mSync == null) return;

		if (mEntrySync.size() > 0) {
			
			// フォルダの中で最新の一件の情報を取得
			FolderEntry fe = mEntrySync.get(0);
	
			if (fe.images.size() > 0) {
				
				// サムネイルの設定
				File image = new File(fe.images.get(0));
				mSync.setImage(fe.path.getPath(), image.getName(), mParentLength);
				
				// メディアの最新更新日付を設定
				mSync.setTextData(DateUtil.getDateString(image.lastModified()));
			}
			
			// カテゴリ詳細レイアウトにクリックリスナをセット
			mSync.setCategoryClickListener(mSyncClickListener);
		}
		
		// フォルダ数の取得・設定
		String countText = getString(R.string.home_label_sync) + " (" + mSyncMediaCount + ")";
		
		mSync.setTextTop(countText);
	}
	
	
	
	/**
	 * カテゴリ　シークレット設定
	 */
	public void setLayoutSecret() {

		mSecret = new CategoryView(this, mLayoutParams);
		
		String secretText = getString(R.string.home_label_secret);
		
		mSecret.setCategoryName(ApplicationDefine.KEY_CATEGORY_SECRET);
		mSecret.setHeaderName(secretText);
		
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false);

		if (secretState) {
			
			// 隠しフォルダ表示状態
			boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
	
			
			// 画像フォルダ・画像一覧をスキャニング
			mSecretScanner = new JorlleMediaScanner();
			mSecretScanner.sort(new JorlleMediaScanner.DateDescender()).maxCallback(MEDIA_CALLBACK).
									scanOnlySecret().scanSubfolder(true).scanNomedia(hiddenState).findMedia(mSecretListener);
			
			// 描画領域を表示するため、最初にダミー画像を表示
			mSecret.setDummyImage(mParentLength);
			
		} else {
			mSecret.setImage(R.drawable.ic_secret, mParentLength);
			mSecret.setTextTop(getString(R.string.home_message_no_secret));
			mSecret.setCategoryClickListener(mNoSecretClickListener);
		}
		
		boolean visible = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_CATEGORY_SECRET, true);
		
		mSecret.setStatusVisible(visible);
		
		mLytCategory.addView(mSecret);
	}
	
	
	
	/**
	 * シークレットカテゴリの詳細データ
	 */
	public void setSecretData() {
		
		if (mSecret == null) return;

		if (mEntrySecret.size() > 0) {
			
			// フォルダの中で最新の一件の情報を取得
			FolderEntry fe = mEntrySecret.get(0);
	
			if (fe.images.size() > 0) {
				
				// サムネイルの設定
				File image = new File(fe.images.get(0));
				mSecret.setImage(fe.path.getPath(), image.getName(), mParentLength);
			}
			
			// カテゴリ詳細レイアウトにクリックリスナをセット
			mSecret.setCategoryClickListener(mSecretClickListener);
		}
		
		// フォルダ数の取得・設定
		String countText = getString(R.string.home_label_secret) + " (" + mSecretMediaCount + ")";
		
		mSecret.setTextTop(countText);
	}
	
	
	
	/**
	 * カテゴリ　Picasa参照設定
	 */
	public void setLayoutPicasa() {
		final CategoryView picasa = new CategoryView(this, mLayoutParams);
		
		final String picasaText = getString(R.string.home_label_picasa);
		
		picasa.setCategoryName(ApplicationDefine.KEY_CATEGORY_PICASA);
		picasa.setHeaderName(picasaText);
		picasa.setTextTop(picasaText);
		
		// 描画領域を表示するため、最初にダミー画像を表示
		picasa.setDummyImage(mParentLength);
		
		final String account = PicasaPrefsActivity.getPicasaAccount(this);
		if (TextUtils.isEmpty(account)) {
			// Picasaが利用できない
			picasa.setImage(R.drawable.ic_share, mParentLength);
			picasa.setTextTop(getString(R.string.home_message_no_picasa));
			picasa.setCategoryClickListener(mNoPicasaClickListener);
		} else {
			picasa.setCategoryClickListener(mPicasaClickListener);
			// アルバム数とサムネイルを非同期で取得
			class PicasaData {
				Bitmap bitmap;
				int albums;
				Exception thrown;
			}
			new AsyncTask<Void, Void, PicasaData>() {
				@Override
				protected PicasaData doInBackground(Void... params) {
					PicasaData data = new PicasaData();
					try {
						PicasaCache cache = new PicasaCache(getApplicationContext(), account);
						IOIterator<LocalFolderActivity.FolderEntry> itr = cache.listDirs(0, false);
						LocalFolderActivity.FolderEntry entry = null;
						try {
							while (itr.hasNext()) {
								LocalFolderActivity.FolderEntry e = itr.next();
								if (entry == null) {
									entry = e;
								}
								data.albums++;
							}
						} finally {
							itr.terminate();
						}
						
						if (entry != null) {
							IOIterator<MediaEntry> itrM = cache.listMediasAt(entry.getPath(), 1);
							try {
								if (itrM.hasNext()) {
									MediaEntry m = itrM.next();
									BitmapFactory.Options options = new BitmapFactory.Options();
									options.inScaled = false;
									options.inPurgeable = true;
									data.bitmap = cache.loadMiniThumb(
											m.getDirId(), m.getMediaId(), options);
								}
							} finally {
								itrM.terminate();
							}
							
						}
						
					} catch (IOException e) {
						data.thrown = e;
						return data;
					}
					
					return data;
				}
				
				@Override
				protected void onPostExecute(PicasaData result) {
					if (result.thrown != null) {
//						result.thrown.printStackTrace();		/*$debug$*/
					} else {
						picasa.setTextTop(String.format("%s (%d)", picasaText, result.albums));
						picasa.setImage(result.bitmap, mParentLength);
					}
				}
			}.execute();
		}
		
		boolean visible = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_CATEGORY_PICASA, true);
		
		picasa.setStatusVisible(visible);
		
		mLytCategory.addView(picasa);
	}
	
	
	
	/**
	 * カテゴリ　お気に入り設定
	 */
	public void setLayoutFavorite() {
		
		// 隠しフォルダ表示フラグ
		final boolean isDisplayHidden = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		
		// ディレクトリのリスト
		List<String> dir = new ArrayList<String>();
		
		Cursor cursor = null;
		
		// 検索項目・条件の設定
		String[] columns = {CMediaMetadata._ID, CMediaMetadata.DIRPATH, CMediaMetadata.NAME};
		String selection = CMediaMetadata.METADATA_TYPE + " = ? ";
		
		// お気に入りの取得条件
		String selectionFavorite = null;
		
		// お気に入りの条件値
		List<String> selectionArgs = new ArrayList<String>();
		selectionArgs.add(ApplicationDefine.MIME_FAVORITE);

		String orderBy = CMediaMetadata.UPDATE_TIMESTAMP + " DESC";
		
		
		// 一行のレイアウト作成
		CategoryView favorite = new CategoryView(this, mLayoutParams);
		
		// 描画領域を表示するため、最初にダミー画像を表示
		favorite.setDummyImage(mParentLength);
		
		try {
			
			// 隠しフォルダを表示しない場合
			if (!isDisplayHidden) {
				
				// メタデータに登録されている隠しフォルダ一覧を取得
				dir = MediaMetaDataAccessor.queryHiddenFolder(mDatabase, ApplicationDefine.MIME_FAVORITE, false);
				
				if (dir.size() > 0) {
					
					selection += " AND " + CMediaMetadata.DIRPATH + " NOT IN ( ";
					
					boolean isFirst = true;
					
					int size = dir.size();
					
					// ディレクトリが隠しフォルダか判定
					for (int i = 0; i < size; i++) {
							
						// 合わせてwhere条件を作成
						if (isFirst) {
							selection += "?";
							isFirst = false;
						} else {
							selection += ",?";
						}
					}
					selection += " ) ";
					
					selectionArgs.addAll(dir);
				}
			}
			
						
			// シークレットを表示
			if (PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false)) {
				selectionFavorite = selection;
				
			// シークレットを非表示
			} else {
				selectionFavorite = selection + " AND " + CMediaMetadata.NAME + " NOT LIKE ? " ;
				selectionArgs.add("%" + ApplicationDefine.SECRET);
			}
			

			// お気に入りの画像を取得
			cursor = mDatabase.query(CMediaMetadata.$TABLE, columns, selectionFavorite, selectionArgs.toArray(new String[1]), null, null, orderBy);
			
			String favoriteText = getString(R.string.home_label_favorite);
		
			favorite.setCategoryName(ApplicationDefine.KEY_CATEGORY_FAVORITE);
			favorite.setHeaderName(favoriteText);
			
			int favoriteCount = 0;
			
			// 最新の一件を取得し、情報を設定
			if (cursor.moveToFirst()) {
				
				String pathDir = cursor.getString(INDEX_FAVORITE_DIRPATH);
				String pathName = cursor.getString(INDEX_FAVORITE_NAME);

				favorite.setImage(pathDir, pathName, mParentLength);
				
				favoriteCount = cursor.getCount();
			
				// カテゴリ詳細レイアウトにクリックリスナをセット
				favorite.setCategoryClickListener(mFavoriteClickListener);
				
			// 一件も存在しない場合は、ダミーの画像を表示
			} else {
				favorite.setDummyImage(mParentLength);
			}
			
			// お気に入り画像数
			String countText = favoriteText + " (" + favoriteCount + ")";
			
			favorite.setTextTop(countText);
			
			mLytCategory.addView(favorite);
						
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
	
	
	
	/**
	 * カテゴリ　タグ設定
	 */
	public void setLayoutTag() {

		// 隠しフォルダ表示フラグ
		final boolean isDisplayHidden = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		
		// タグ一覧取得用
		Cursor cursor = null;
		String[] columns = {CMediaTagMaster._ID, CMediaTagMaster.NAME};

		// タグ詳細取得用
		Cursor cursorTagName = null;
		String[] columnsTagName = {CMediaMetadata._ID, CMediaMetadata.DIRPATH, CMediaMetadata.NAME};
		String selectionText = CMediaMetadata.METADATA + " = ? ";
		String orderBy = CMediaMetadata.UPDATE_TIMESTAMP + " DESC";
		
		// タグ詳細検索時の条件値リスト
		List<String> selectionArgsTagName = new ArrayList<String>();
		
		// タグ名
		String tagName = null;
		
		// ディレクトリのリスト
		List<String> dir = new ArrayList<String>();

		
		try {
			// 隠しフォルダを表示しない場合
			if (!isDisplayHidden) {
				
				// メタデータに登録されている隠しフォルダ一覧を取得
				dir = MediaMetaDataAccessor.queryHiddenFolder(mDatabase, ApplicationDefine.MIME_TAG, false);
				
				if (dir.size() > 0) {
					
					selectionText += " AND " + CMediaMetadata.DIRPATH + " NOT IN ( ";
					
					boolean isFirst = true;
					
					int size = dir.size();
					
					// ディレクトリが隠しフォルダか判定
					for (int i = 0; i < size; i++) {
							
						// 合わせてwhere条件を作成
						if (isFirst) {
							selectionText += "?";
							isFirst = false;
						} else {
							selectionText += ",?";
						}
					}
					selectionText += " ) ";
				}
			}
			
			
			// タグ一覧を取得
			cursor = mDatabase.query(CMediaTagMaster.$TABLE, columns, null, null, null, null, null);
			
			// タグ名ごとに処理を実行
			while (cursor.moveToNext()) {
				
				// タグ詳細条件値リストの初期化
				selectionArgsTagName.clear();
				
				// タグ名取得し、条件値に追加
				tagName = cursor.getString(INDEX_TAG_METADATA);
				selectionArgsTagName.add(tagName);
				
				// ディレクトリが存在する場合は、条件にマージ
				if (dir.size() > 0) {
					selectionArgsTagName.addAll(dir);
				}
				
				String selectionTagName = null;
				
				// シークレットを表示
				if (PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false)) {
					selectionTagName = selectionText;
					
				// シークレットを非表示
				} else {
					selectionTagName = selectionText + " AND " + CMediaMetadata.NAME + " NOT LIKE ? " ;
					selectionArgsTagName.add("%" + ApplicationDefine.SECRET);
				}
				
				
				// タグ名単体の登録情報を取得
				cursorTagName = mDatabase.query(CMediaMetadata.$TABLE, columnsTagName, selectionTagName, (String[])selectionArgsTagName.toArray(new String[1]), 
													null, null, orderBy);
				
				
				// タグカテゴリの作成
				CategoryView tag = null;
				
				// 一行分のレイアウトを取得
				
				
				// 更新日付が最新のものを取得し、表示
				if (cursorTagName.moveToFirst()) {
					
					String pathDir = cursorTagName.getString(INDEX_TAG_SINGLE_DIRPATH);
					String pathName = cursorTagName.getString(INDEX_TAG_SINGLE_NAME);

					tag = new CategoryView(this, mLayoutParams);
					
					// 描画領域を表示するため、最初にダミー画像を表示
					tag.setDummyImage(mParentLength);
					
					// ヘッダフォーマット（「タグ：」 + タグ名）
					String formatTagName = getString(R.string.home_label_tag) + "：" + tagName;
					
					tag.setCategoryName(tagName);
					tag.setHeaderName(formatTagName);

					// 画像ファイル取得
					File image = new File(pathDir, pathName);
					
					// 画像設定
					tag.setImage(image.getParent(), image.getName(), mParentLength);

					// タグフラグをセット
					tag.setIsTag(true);
				
					// カテゴリ詳細レイアウトにクリックリスナをセット
					tag.setCategoryClickListener(new TagClickListener(tagName));
					
					// タグ名・件数設定
					tag.setTextTop(tagName + " (" + cursorTagName.getCount() + ")");
					
					mLytCategory.addView(tag);
				}
			}
			
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			
			if (cursorTagName != null) {
				cursorTagName.close();
			}
		}
	}
	
	
	
	/** ローカルのクリックリスナー */
	View.OnClickListener mLocalClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			// フォルダ一覧タイプをローカルに設定
			PreferenceUtil.setPreferenceValue(HomeActivity.this, ApplicationDefine.KEY_FOLDER_TYPE, ApplicationDefine.FOLDER_TYPE_LOCAL);
			
			Intent intent = new Intent(HomeActivity.this, LocalFolderActivity.class);
			intent.putExtra(ApplicationDefine.INTENT_CHANGE_HOME, true);
			startActivityForResult(intent, ApplicationDefine.REQUEST_LOCAL);
		}
	};
	
	
	
	/** 同期のクリックリスナー */
	View.OnClickListener mSyncClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			String syncFolder = PreferenceUtil.getPreferenceValue(HomeActivity.this, PicasaPrefsActivity.PREF_PICASA_SYNC_LOCAL, null);
			
			Intent intent = new Intent(HomeActivity.this, GridActivity.class);
			intent.putExtra(ApplicationDefine.INTENT_CHANGE_HOME, true);
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_SUB, false);
			intent.putExtra(ApplicationDefine.INTENT_CATEGORY_NAME, getString(R.string.home_label_sync));
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_PATH, syncFolder);
			startActivity(intent);
		}
	};
	
	
	
	/** 同期未設定時のクリックリスナー */
	View.OnClickListener mNoSyncClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			Intent intent = new Intent(HomeActivity.this, PicasaPrefsActivity.class);
			startActivity(intent);
		}
	};
	
	
	
	/** シークレットのクリックリスナー */
	View.OnClickListener mSecretClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			Intent intent = new Intent(HomeActivity.this, GridActivity.class);
			intent.putExtra(ApplicationDefine.INTENT_CHANGE_HOME, true);
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_SUB, true);
			intent.putExtra(ApplicationDefine.INTENT_CATEGORY_NAME, getString(R.string.home_label_secret));
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_SECRET, true);
			startActivity(intent);
		}
	};
	
	
	
	/** シークレット非表示時のクリックリスナー */
	View.OnClickListener mNoSecretClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			Intent intent = new Intent(HomeActivity.this, JorlleAdvancedPrefsActivity.class);
			startActivity(intent);
		}
	};
	
	
	
	/** Picasaのクリックリスナー */
	View.OnClickListener mPicasaClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(HomeActivity.this, PicasaFolderViewActivity.class);
			intent.putExtra(ApplicationDefine.INTENT_CHANGE_HOME, true);
			startActivity(intent);
		}
	};
	
	/** Picasaが利用できないときのクリックリスナー */
	View.OnClickListener mNoPicasaClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(HomeActivity.this, PicasaPrefsActivity.class);
			startActivity(intent);
		}
	};
	
	
	
	/** お気に入りのクリックリスナー */
	View.OnClickListener mFavoriteClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(HomeActivity.this, GridActivity.class);
			intent.putExtra(ApplicationDefine.INTENT_CHANGE_HOME, true);
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_SUB, true);
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_FAVORITE, true);
			intent.putExtra(ApplicationDefine.INTENT_CATEGORY_NAME, getString(R.string.home_label_favorite));
			startActivity(intent);
		}
	};
	
	
	
	/** タグのクリックリスナークラス */
	private class TagClickListener implements OnClickListener {
		
		private String mTagName;
		
		public TagClickListener(String tagName) {
			mTagName = tagName;
		}
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(HomeActivity.this, GridActivity.class);
			intent.putExtra(ApplicationDefine.INTENT_CHANGE_HOME, true);
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_SUB, true);
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_TAG, mTagName);
			startActivity(intent);
		}
	};
	
	
	
	/**
	 * ファイルの存在チェックを行うタスク
	 */
	private class FileCheckTask extends AsyncTask<Void, Void, Boolean>{

		@Override
		protected Boolean doInBackground(Void... params) {
			
			boolean metadata = MediaMetaDataAccessor.checkDBFolder(mDatabase);
			boolean index = MediaIndexesAccessor.checkDBFolder(OpenHelper.cache.getDatabase());
			
			return index && metadata;
		}
	}
	
	
	
	/**
	 * 画面サイズに応じた描画サイズを計算します
	 *
	 * @param view	描画ビュー
	 */
	public void setDisplayLength() {

		wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		display = wm.getDefaultDisplay(); 
		
		Configuration config = getResources().getConfiguration();

		// 画面の向きによって、描画サイズの計算式を変更
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mParentLength = display.getWidth() / 7;
		} else {
			mParentLength = display.getWidth() / 5;
		}
		
		// 一行分のレイアウトパラメータを設定
		mLayoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	}
	

	
	/**
	 * フォルダ情報格納クラス
	 */
	public class FolderEntry {
		/** フォルダファイル */
		public File path;
		/** サムネイル用画像のパスリスト */
		public List<String> images = new ArrayList<String>();
	}
	
	
	
	/**
	 * ローカルフォルダのスキャナリスナー
	 */
	public OnFoundListener mLocalListener = new OnFoundListener() {
		
		@Override
		public void onStartFolder(File folder) {
			// フォルダ情報クラスを生成し、フォルダパスを格納
			FolderEntry fe = new FolderEntry();
			fe.path = folder;
			mEntryLocal.add(fe);
		}
		
		@Override
		public void onFound(File file) {
			// フォルダ内のメディアファイル情報を格納
			if (mEntryLocal != null) {
				mEntryLocal.get(mEntryLocal.size() -1).images.add(file.getPath());
			}
		}

		@Override
		public void onEndFolder(File folder, int mediaCount) {
		}

		@Override
		public void onComplete() {
			// リストをソートし、ローカルのレイアウトを設定
			Collections.sort(mEntryLocal, mDateAscender);
			setLocalData();
		}
	};
	
	
	
	/**
	 * 同期フォルダのスキャナリスナー
	 */
	public OnFoundListener mSyncListener = new OnFoundListener() {
		
		@Override
		public void onStartFolder(File folder) {
			// フォルダ情報クラスを生成し、フォルダパスを格納
			FolderEntry fe = new FolderEntry();
			fe.path = folder;
			mEntrySync.add(fe);
		}
		
		@Override
		public void onFound(File file) {
			// フォルダ内のメディアファイル情報を格納
			if (mEntrySync != null) {
				mEntrySync.get(mEntrySync.size() -1).images.add(file.getPath());
			}
		}
		
		@Override
		public void onEndFolder(File folder, int mediaCount) {
			// メディアファイルの数を保持
			mSyncMediaCount += mediaCount;
		}
		
		@Override
		public void onComplete() {
			// リストをソートし、同期レイアウトを設定
			Collections.sort(mEntrySync, mDateAscender);
			setSyncData();
		}
	};
	
	
	
	/**
	 * シークレットのスキャナリスナー
	 */
	public OnFoundListener mSecretListener = new OnFoundListener() {
		
		@Override
		public void onStartFolder(File folder) {
			// フォルダ情報クラスを生成し、フォルダパスを格納
			FolderEntry fe = new FolderEntry();
			fe.path = folder;
			mEntrySecret.add(fe);
		}
		
		@Override
		public void onFound(File file) {
			// フォルダ内のメディアファイル情報を格納
			if (mEntrySecret != null) {
				mEntrySecret.get(mEntrySecret.size() -1).images.add(file.getPath());
			}
		}

		@Override
		public void onEndFolder(File folder, int mediaCount) {
			// メディアファイルの数を保持
			mSecretMediaCount += mediaCount;
		}

		@Override
		public void onComplete() {
			// リストをソートし、シークレットのレイアウトを設定
			Collections.sort(mEntrySecret, mDateAscender);
			setSecretData();
		}
	};
	

	
	/**
	 * 他アクティビティからのコールバックメソッド
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (requestCode == ApplicationDefine.REQUEST_LOCAL && resultCode == ApplicationDefine.RESULT_LOCAL) {
			finish();
		}
	}


	
	/**
	 * 日付の新しい順
	 */
	public static class DateAscender implements Comparator<FolderEntry>{

		@Override
		public int compare(FolderEntry lhs, FolderEntry rhs) {
			
			long d = rhs.path.lastModified() - lhs.path.lastModified();
			if(d > 0)return 1;
			if(d < 0)return -1;
			return 0;
		}
	}
	
	
	
	/**
	 * カテゴリ情報クラス
	 */
	public class CategoryState {
		
		/** カテゴリ名 */
		public String category;
		/** 表示名 */
		public String name;
		/** 表示チェック */
		public boolean checked;
		/** タグフラグ */
		public boolean isTag;
		
		public CategoryState(String category, String name, boolean checked, boolean isTag) {
			this.category = category;
			this.name = name;
			this.checked = checked;
			this.isTag = isTag;
		}
	}
}
