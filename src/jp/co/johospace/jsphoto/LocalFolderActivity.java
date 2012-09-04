package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.co.johospace.jsphoto.accessor.MediaIndexesAccessor;
import jp.co.johospace.jsphoto.accessor.MediaMetaDataAccessor;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.SecretCheckPasswordDialog;
import jp.co.johospace.jsphoto.dialog.SimpleEditDialog;
import jp.co.johospace.jsphoto.folder.AsyncFolderAdapter;
import jp.co.johospace.jsphoto.folder.AsyncListAdapter;
import jp.co.johospace.jsphoto.folder.AsyncListAdapter.ViewHolder;
import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner.OnFoundListener;
import jp.co.johospace.jsphoto.util.EncryptionUtil;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.MediaStoreOperation;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * フォルダ一覧画面アクティビティです
 */
public class LocalFolderActivity extends NavigatableActivity implements OnFoundListener, OnClickListener, OnItemClickListener {

	/** スキャナで取得するフォルダ内画像件数 */
	private static final int FOLDER_IMAGE_NUMBER = 10;

	/** フォルダ一覧キャッシュファイル名 */
	private static final String LISTCACHE_FILE_NAME = "FolderEntriesCache.dat";

	protected final ArrayList<FolderEntry> mFolderEntries =
			new ArrayList<LocalFolderActivity.FolderEntry>();

	protected ArrayList<FolderEntry> mEntries =
			new ArrayList<LocalFolderActivity.FolderEntry>();

	/** ソート情報を保存するプリファレンスのキー **/
	private static String LAST_TIME_SORT = "last_time_sort";

	/** フォルダ表示形式　判定フラグ */
	private boolean mFlgUseListStyle;

	/** 選択しているフォルダのパス */
	private String mSelectFolder;

//	/** フォルダ情報リスト */
//	List<FolderEntry> mEntryList = new ArrayList<FolderEntry>();

	/** フィルタリング　お気に入りチェック */
	private boolean mFilteringFavorite;
	/** フィルタリング　開始日 */
	private Calendar mStartDate;
	/** フィルタリング　終了日 */
	private Calendar mEndDate;
	/** フィルタリング　タグ一覧 */
	private List<String> mTagList = new ArrayList<String>();

	/** 長押し選択フォルダの情報 */
	private FolderEntry mSelectEntry;

	/** DBアクセス */
	private static SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();

	/** ソート条件 ファイル */
	Comparator<File> mComparatorMedia = new JorlleMediaScanner.DateAscender();
	/** ソート条件 フォルダ*/
	Comparator<FolderEntry> mComparator;

	/** 画像アダプタ フォルダグリッド形式 */
	protected AsyncFolderAdapter mFolderAdapter;

	/** 画像アダプタ リスト形式 */
	protected AsyncListAdapter mListAdapter;

	/** メディアスキャナ */
	private JorlleMediaScanner mMediaScanner;

	/** プログレス */
	private ProgressDialog mProgressDialog;

	/** フォルダ操作中フラグ */
	private boolean mIsControllFolder = false;
	
	/** フォルダ操作完了メッセージ */
	private String mCompMessage;

	/** フォルダ表示用リスト */
	private ListView mList;

	/** フォルダ表示用グリッド */
	private GridView mGrid;

	/** フィルタリング項目表示用のフレームレイアウト */
	private FrameLayout mFrameLayout;

	/** 名前変更ダイアログの入力テキスト */
	private EditText mChangeText;

	/** プログレスダイアログ */
//	private ProgressDialog mScanProgress;
//	private ProgressDialog mFolderProgress;


	/** リストの位置を保存するプリファレンスのKey **/
	private static final String
		PREF_LIST_TOP = "pref_list_top",
		PREF_LIST_TOP_Y = "pref_list_top_y";
	
	/** ダイアログID */
	private static final int
		DIALOG_SORT = 0,
		DIALOG_FOLDER_HIDDEN = 1,
		DIALOG_FOLDER_NO_HIDDEN = 2,
		DIALOG_FOLDER_SECRET = 3,
		DIALOG_FOLDER_NO_SECRET = 4,
		DIALOG_FOLDER_FINISH = 5;

	/** メニュー項目 */
	protected static final int
		MENU_ITEM_NEW_FOLDER = 1,
		MENU_ITEM_SORT = 2,
		MENU_ITEM_SEARCH = 3,
		MENU_ITEM_SETTING = 4;


	/** 表示順序ダイアログの項目 */
	private static final int
		SORT_NAME_ASC = 0,
		SORT_NAME_DESC = 1,
		SORT_DATE_ASC = 2,
		SORT_DATE_DESC = 3,
		SORT_COUNT_DESC = 4,
		SORT_COUNT_ASC = 5;


	/** 長押しメニューの項目 */
	private static final int
		MENU_CONTEXT_HIDDEN = 0,
		MENU_CONTEXT_NO_HIDDEN = 1,
		MENU_CONTEXT_SECRET = 2,
		MENU_CONTEXT_NO_SECRET = 3,
		MENU_CONTEXT_NAME = 4,
		MENU_CONTEXT_DELETE = 5;



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.folder);

		init();
	}
	
	/**
	 * 初期化処理
	 */
	public void init() {

		// リストの設定
		mList = (ListView) findViewById(R.id.listImage);
		mList.setOnItemClickListener(this);
		mList.setRecyclerListener(new RecyclerListener() {

			@Override
			public void onMovedToScrapHeap(View view) {
				ViewHolder holder = ((ViewHolder)view.getTag());
				for(int i = 0; i < holder.layout.getChildCount(); i++) {
					((UXAsyncImageView)holder.layout.getChildAt(i)).recycle();
				}
			}
		});
//		mList.setRecyclerListener(; {
//
//			@Override
//			public void onMovedToScrapHeap(View view) {
//				// TODO Auto-generated method stub
//				LinearLayout layout = ((LinearLayout)view.getTag());
//				for(int i = 0; i < layout.getChildCount(); i++) {
//					((UXAsyncImageView)layout.getChildAt(0)).recycle();
//				}
//			}
//		});

		// グリッドの設定
		mGrid = (GridView)findViewById(R.id.gridImage);
		mGrid.setOnItemClickListener(this);

		// リストアダプタ設定
		if (mListAdapter == null) {
			mListAdapter = createListAdapter();
		}

		// フォルダアダプタ設定
		if (mFolderAdapter == null) {
			mFolderAdapter = createFolderAdapter();
		}

		mListAdapter.clearLength();
		mFolderAdapter.clearLength();

		mFlgUseListStyle = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_FOLDER_VIEW_MODE, true);

		// リスト形式
		if (mFlgUseListStyle) {
			mListAdapter.setDisplayLength();
			mListAdapter.setRowLength();
			mList.setAdapter(mListAdapter);
			registerForContextMenu(mList);
			mList.setVisibility(View.VISIBLE);
			mGrid.setVisibility(View.GONE);

		}
		
		//リストの位置を設定
		mList.setSelectionFromTop(
				PreferenceUtil.getIntPreferenceValue(this, PREF_LIST_TOP, 0), 
				PreferenceUtil.getIntPreferenceValue(this, PREF_LIST_TOP_Y, 0));
		
		//リスト位置をクリアする
		PreferenceUtil.setIntPreferenceValue(this, PREF_LIST_TOP, 0);
		PreferenceUtil.setIntPreferenceValue(this, PREF_LIST_TOP_Y, 0);
		
		//ソートを設定する
		settingComparator(PreferenceUtil.getIntPreferenceValue(this, LAST_TIME_SORT, -1));

		// フィルタリング項目を非表示
		//showFilteringMenu(false);
	}

	/**
	 * コンパレーターを設定します。
	 */
	private void settingComparator(int sort){
		
		switch (sort){
		// 名前昇順
		case SORT_NAME_ASC:
			mComparator = new NameAscender();
			break;

		// 名前降順
		case SORT_NAME_DESC:
			mComparator = new NameDescender();
			break;

		// 日付昇順
		case SORT_DATE_ASC:
			mComparator = new DateAscender();
			break;

		// 日付降順
		case SORT_DATE_DESC:
			mComparator = new DateDescender();
			break;

		// 画像件数少ない順
		case SORT_COUNT_ASC:
			mComparator = new CountAscender();
			break;

		// 画像件数多い順, 初回起動時
		default:
		case SORT_COUNT_DESC:
			mComparator = new CountDescender();
			break;
		}		
	}

	protected AsyncListAdapter createListAdapter() {
		return new AsyncListAdapter(getApplicationContext(), mFolderEntries);
	}



	protected AsyncFolderAdapter createFolderAdapter() {
		return new AsyncFolderAdapter(getApplicationContext(), mFolderEntries);
	}



    /**
	 * メニュー作成イベント
	 * @param menu
	 * @return true:成功、false:失敗
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ITEM_NEW_FOLDER, 0, getResources().getString(R.string.menu_new_folder)).setIcon(R.drawable.ic_new_folder);
		menu.add(0, MENU_ITEM_SORT, 0, getResources().getString(R.string.menu_sort)).setIcon(R.drawable.ic_sort);
		menu.add(0, MENU_ITEM_SEARCH, 0, getResources().getString(R.string.menu_search)).setIcon(R.drawable.ic_search);
		menu.add(0, MENU_ITEM_SETTING, 0, getResources().getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);

		return true;
	}



	@Override
	protected void onResume() {
		super.onResume();
		boolean notFirst = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_NOT_FIRST_TIME);

		// 初回起動時にもスキャナが走ってしまう為抑制
		if (notFirst) {
			doOnResume();
		}
	}
	
	
	/**
	 * フォルダ一覧をキャッシュし、ファイルに出力
	 */
	private void saveCacheEntries() {
		try {
		    FileOutputStream fos = null;
		    ObjectOutputStream oos = null;
		    ArrayList<String> list = new ArrayList<String>();
		    for(FolderEntry fe : mDCIMFolder) {
		    	list.add(fe.toSerializeValue());
		    }
		    for(FolderEntry fe : mEntries) {
		    	list.add(fe.toSerializeValue());
		    }
		    try {
			    fos = openFileOutput(LISTCACHE_FILE_NAME, MODE_PRIVATE);
			    oos = new ObjectOutputStream(fos);
			    oos.writeObject(list);
		    } finally {
		    	if(oos!=null) oos.close();
		    	if(fos!=null) fos.close();
		    }
//		    Log.d("LocalFolderAdapter", "saveCacheEntries");		/*$debug$*/
		} catch (Exception e) {
//			e.printStackTrace();		/*$debug$*/
		}
	}
	
	/**
	 * フォルダ一覧をキャッシュファイルからロードする
	 */
	private boolean loadCacheEntries() {
		try {
			if(getFileStreamPath(LISTCACHE_FILE_NAME).exists()) {
			    ArrayList<String> list = null;
			    FileInputStream fis = null;
			    ObjectInputStream ois = null;
			    try {
			    	fis = openFileInput(LISTCACHE_FILE_NAME);
			    	ois = new ObjectInputStream(fis);
				    list = (ArrayList<String>) ois.readObject();
			    } finally {
			    	if(ois!=null) ois.close();
			    	if(fis!=null) fis.close();
			    }
			    if(list!=null) {
					if (mListAdapter != null) {
						mListAdapter.clearItem();
					}
					if (mFolderAdapter != null) {
						mFolderAdapter.clearItem();
					}

				    for(String path : list) {
				    	String[] datas = path.split(",");
						FolderEntryImpl fe = new FolderEntryImpl();
						fe.mFile = new File(datas[0]);
						if(datas.length>2) {
							try {
								fe.mMediaCount = Integer.parseInt(datas[1]);
								fe.mLastModified = Long.parseLong(datas[2]);
							} catch (Exception e) {
//								e.printStackTrace();		/*$debug$*/
							}
						}
						
						List<String> images = fe.getImages();
						for(int i=3;i<datas.length;i++) {
							images.add(datas[i]);
						}
						/*
						if(fe.getImages().size()>0) {
							// フォルダ内最新ファイルの更新日時を取得
							if (fe.mLastModified == null) {
								fe.mLastModified = new File(fe.getImages().get(0)).lastModified();
							}
						}
						*/
					    mFolderEntries.add(fe);
				    }

					//mFolderAdapter.sort(mComparator);
				    
				    return true;
			    }
			}
		} catch (Exception e) {
//			e.printStackTrace();		/*$debug$*/
		}
		return false;
	}

	/**
	 * フォルダ一覧が変更されたかどうか
	 * @return
	 */
	private boolean checkUpdateList() {
		String check = "";
	    for(FolderEntry fe : mDCIMFolder) {
	    	check += EncryptionUtil.digest(fe.toSerializeValue()) + ",";
	    }
	    for(FolderEntry fe : mEntries) {
	    	check += EncryptionUtil.digest(fe.toSerializeValue()) + ",";
	    }
	    
		String current = "";
	    for(FolderEntry fe : mFolderEntries) {
	    	current += EncryptionUtil.digest(fe.toSerializeValue()) + ",";
	    }
	    if(check.equals(current)) {
	    	return false;
	    } else {
	    	return true;
	    }
	}

	protected void doOnResume() {

		findLocalMedia();
		
		// 次回起動時の表示画面を、フォルダ一覧に設定
		PreferenceUtil.setPreferenceValue(this, ApplicationDefine.KEY_INIT_ACTIVITY, ApplicationDefine.INIT_ACTIVITY_FOLDER);
	}



	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		//showFilteringMenu(false);

		// 選択したフォルダのパスを保持
		if (mFlgUseListStyle) {
			mSelectEntry = (FolderEntry)mListAdapter.getItem(((AdapterView.AdapterContextMenuInfo)menuInfo).position);
		} else {
			mSelectEntry = (FolderEntry)mFolderAdapter.getItem(((AdapterView.AdapterContextMenuInfo)menuInfo).position);
		}

		// パス名のみを保持
		mSelectFolder = mSelectEntry.getPath();

		String hiddenText = null;
		int hiddenNum;

		String secretText = null;
		int secretNum;

		File targetDir = new File(mSelectEntry.getPath());

		// 隠しフォルダかどうかで、メニュー項目を変化させる
		if (MediaUtil.isContainsNomedia(targetDir)) {

			hiddenText = getString(R.string.folder_context_no_hidden);
			hiddenNum = MENU_CONTEXT_NO_HIDDEN;
		} else {

			hiddenText = getString(R.string.folder_context_hidden);
			hiddenNum = MENU_CONTEXT_HIDDEN;
		}

		// 隠しフォルダ表示状態
		boolean containsDotfile = PreferenceUtil.getBooleanPreferenceValue(LocalFolderActivity.this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);

		// シークレットフォルダかどうかで、メニュー項目を変化させる
		if (MediaUtil.checkSecret(targetDir, containsDotfile)) {
			secretText = getString(R.string.folder_context_no_secret);
			secretNum = MENU_CONTEXT_NO_SECRET;
		} else {
			secretText = getString(R.string.folder_context_secret);
			secretNum = MENU_CONTEXT_SECRET;
		}

		menu.setHeaderTitle(getResources().getString(R.string.folder_context_title));
		menu.add(0, hiddenNum, 0, hiddenText);
		menu.add(0, secretNum, 0, secretText);

		// 名前の変更は、SDカード以外で可能
		if (!Environment.getExternalStorageDirectory().getPath().equals(mSelectFolder)) {
			menu.add(0, MENU_CONTEXT_NAME, 0, getResources().getString(R.string.context_name));
		}

		menu.add(0, MENU_CONTEXT_DELETE, 0, getResources().getString(R.string.context_delete));

		super.onCreateContextMenu(menu, v, menuInfo);
	}



	@Override
	protected void onDestroy() {

		mListAdapter.dispose();
		mFolderAdapter.dispose();
		if (mMediaScanner != null) {
			mMediaScanner.dispose();
		}

		super.onDestroy();
	}



	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		// 画像一覧からホームボタンを押された際
		if (requestCode == ApplicationDefine.REQUEST_GRID && resultCode == ApplicationDefine.RESULT_HOME) {
			setResult(RESULT_CANCELED);
			finish();

//		// インフォリンクチュートリアルからタブ切替が指定された際
//		} else if (requestCode == MENU_ITEM_SEARCH) {
//
//			// タブを取得
//			TabHost tabHost = ((TabActivity) ((LocalNavigationGroup)getParent()).getParent()).getTabHost();
//
//			// 画面を切り替える
//			if (resultCode == ApplicationDefine.RESULT_TUTORIALINFOLINK_ONLINE){
//				// オンライン
//				tabHost.setCurrentTabByTag(ApplicationDefine.TAB_ONLINE);
//			} else if (resultCode == ApplicationDefine.RESULT_TUTORIALINFOLINK_AUTOALBUM){
//				// オートアルバム
//				tabHost.setCurrentTabByTag(ApplicationDefine.TAB_AUTO);
//			} else if (resultCode == ApplicationDefine.RESULT_TUTORIALINFOLINK_SEARCH){
//				// 検索
//				Intent intent = new Intent(LocalFolderActivity.this, SearchActivity.class);
//				startActivity(intent);
//			}
//
		}

		super.onActivityResult(requestCode, resultCode, data);
	}



	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		// メニューボタンの処理
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			// メニュー押下時、フィルタリング項目を非表示に
			//showFilteringMenu(false);
			openOptionsMenu();
			return true;
			
		// 戻り先が無い為、終了
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBackHistory();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}



	/**
	 * クリックイベント
	 */
	@Override
	public void onClick(View v) {

		switch (v.getId()) {

			// カメラ
			case R.id.iconCamera :

				//showFilteringMenu(false);

				Intent intentCamera = new Intent();
				intentCamera.setAction(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
				startActivity(intentCamera);
				break;


			// 表示切り替え
//			case R.id.iconChange :
//				changeViewMode();
//				break;

			// ホーム画面遷移
//			case R.id.iconHome :
//
//				// ホーム画面から遷移していた場合、フォルダ一覧を消去
//				if (mFlgChangeHome) {
//					setResult(RESULT_CANCELED);
//					finish();
//
//				// 新たにホーム画面作成
//				} else {
//					Intent intentHome = new Intent(FolderViewActivity.this, HomeActivity.class);
//					intentHome.putExtra(ApplicationDefine.INTENT_MOVE_FOLDER, false);
//					startActivity(intentHome);
//				}
//
//				break;

		}
	}



	/**
	 * メニュー選択イベント
	 * @param item
	 * @return true:成功、false:失敗
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			// 新しいフォルダ
			case MENU_ITEM_NEW_FOLDER:
				Intent newIntent = new Intent(LocalFolderActivity.this, SelectFolderActivity.class);
				startActivity(newIntent);
				break;

			// 並べ替え
			case MENU_ITEM_SORT:
				showDialog(DIALOG_SORT);
				break;
			// 検索
			case MENU_ITEM_SEARCH:
				Intent intent = new Intent(LocalFolderActivity.this, SearchActivity.class);
				startActivity(intent);
				break;
			// 設定
			case MENU_ITEM_SETTING:
				Intent settingIntent = new Intent(LocalFolderActivity.this, JorllePrefsActivity.class);
				startActivity(settingIntent);
				break;

		}

		return true;
	}



	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {

			// 隠しフォルダ
			case MENU_CONTEXT_HIDDEN:
				showDialog(DIALOG_FOLDER_HIDDEN);
				break;

			// 非隠しフォルダ
			case MENU_CONTEXT_NO_HIDDEN:
				showDialog(DIALOG_FOLDER_NO_HIDDEN);
				break;

			// シークレット
			case MENU_CONTEXT_SECRET:
				showDialog(DIALOG_FOLDER_SECRET);
				break;

			// シークレット解除
			case MENU_CONTEXT_NO_SECRET:
				showDialog(DIALOG_FOLDER_NO_SECRET);
				break;

			// 名前の変更
			case MENU_CONTEXT_NAME:
				showChangeNameDialog();
				break;

			// 削除
			case MENU_CONTEXT_DELETE:
				showDeleteDialog();
				break;
		}

		return true;
	}



	/**
	 * グリッド押下イベント
	 *
	 * @param parent	adapterView
	 * @param view		View
	 * @param position	選択ポジション
	 * @param id		選択ID
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		//showFilteringMenu(false);

		//リストの位置を保存する
		PreferenceUtil.setIntPreferenceValue(this, PREF_LIST_TOP, mList.getFirstVisiblePosition());
		PreferenceUtil.setIntPreferenceValue(this, PREF_LIST_TOP_Y, mList.getChildAt(0).getTop());
		
		// 取得したパスを元に、フォルダ内画像一覧画面に遷移
		FolderEntry fe = null;

		// リスト形式
		if (mFlgUseListStyle) {
			fe = (FolderEntry)mListAdapter.getItem(position);

		// フォルダ形式
		} else {
			fe = (FolderEntry)mFolderAdapter.getItem(position);
		}

		startGridActivity(fe);
	}


	protected void startGridActivity(FolderEntry fe) {
		Intent intent = new Intent(LocalFolderActivity.this, NewGridActivity.class);

		intent.putExtra(NewGridActivity.INTENT_FOLDER_TYPE, NewGridActivity.FOLDER_TYPE_PATH);
		intent.putExtra(NewGridActivity.INTENT_TARGET, fe.getPath());

		// お気に入りでフィルタリング
		if (mFilteringFavorite) {
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_FAVORITE, mFilteringFavorite);
		}

		// 開始日でフィルタリング
		if (mStartDate != null) {
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_START, mStartDate.getTimeInMillis());
		}

		// 終了日でフィルタリング
		if (mEndDate != null) {
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_END, mEndDate.getTimeInMillis());
		}

		// タグでフィルタリング
		if (mTagList.size() > 0) {
			intent.putExtra(ApplicationDefine.INTENT_FOLDER_TAG_LIST, (String[])mTagList.toArray(new String[0]));
		}

		// 遷移元判定
		intent.putExtra(NewGridActivity.INTENT_TARGET_PARENT, NewGridActivity.PARENT_LOCAL);

		// 画像一覧画面のActivityを遷移管理のActivityへ渡す
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		goNextHistory("NewGridActivity", intent);

	}

	/**
	 * ダイアログ表示
	 */
	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id) {

		// 隠しフォルダ設定ダイアログ
		case DIALOG_FOLDER_HIDDEN:
			return new AlertDialog.Builder(getParent())
			.setTitle(getResources().getString(R.string.folder_title_setting_hidden))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage(getResources().getString(R.string.folder_message_setting_hidden))
			.setPositiveButton(getResources().getString(android.R.string.cancel), null)
			.setNegativeButton(android.R.string.ok, mSetHiddenListener)
			.show();

		// 非隠しフォルダ設定ダイアログ
		case DIALOG_FOLDER_NO_HIDDEN:
			return new AlertDialog.Builder(getParent())
			.setTitle(getString(R.string.folder_title_setting_no_hidden))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage(getString(R.string.folder_message_setting_no_hidden))
			.setPositiveButton(getResources().getString(android.R.string.cancel), null)
			.setNegativeButton(android.R.string.ok, mSetUnHiddenListener)
			.show();

		// シークレット設定ダイアログ
		case DIALOG_FOLDER_SECRET:
			return new AlertDialog.Builder(getParent())
			.setTitle(getString(R.string.folder_title_setting_secret))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage(getString(R.string.folder_message_setting_secret))
			.setPositiveButton(getString(android.R.string.cancel), null)
			.setNegativeButton(android.R.string.ok, mSetSecretListener)
			.show();

		// シークレット解除ダイアログ
		case DIALOG_FOLDER_NO_SECRET:
			return new AlertDialog.Builder(getParent())
			.setTitle(getString(R.string.folder_title_setting_no_secret))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage(getString(R.string.folder_message_setting_no_secret))
			.setPositiveButton(getString(android.R.string.cancel), null)
			.setNegativeButton(android.R.string.ok, mSetUnSecretListener)
			.show();

		// 表示順序設定ダイアログ
		case DIALOG_SORT:
			return new AlertDialog.Builder(this.getParent())
			.setTitle(getString(R.string.menu_sort))
			.setIcon(android.R.drawable.ic_dialog_info)
			.setItems(getResources().getStringArray(R.array.folder_sort), mSetSortListener).show();

		// 終了確認ダイアログ
		case DIALOG_FOLDER_FINISH:
			return new AlertDialog.Builder(getParent())
			.setTitle(getString(R.string.folder_title_finish))
			.setIcon(android.R.drawable.ic_menu_help)
			.setMessage(getString(R.string.folder_message_finish))
			.setPositiveButton(getString(android.R.string.cancel), null)
			.setNegativeButton(android.R.string.ok, mFinishListener)
			.show();

		}
		return super.onCreateDialog(id);
	}



	/** フォルダ名変更ダイアログ */
	SimpleEditDialog mNamgeChangeDialog;
	/**
	 * 名前変更ダイアログを表示します
	 */
	public void showChangeNameDialog() {

		mNamgeChangeDialog = new SimpleEditDialog(LocalFolderActivity.this.getParent());
		mNamgeChangeDialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		mNamgeChangeDialog.mTxtTitle.setText(getResources().getString(R.string.folder_title_change_other_name));
		mNamgeChangeDialog.mTxtView.setText(getResources().getString(R.string.folder_message_change_name));
		mNamgeChangeDialog.mTxtEdit.setHint(R.string.folder_hint_new);
//		mNamgeChangeDialog.mTxtEdit.addTextChangedListener(mNamgeChangeDialog.mTextWatcher);
		
		mNamgeChangeDialog.mBtnOk.setOnClickListener(mNameChangeListener);
		mNamgeChangeDialog.mBtnChansel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				mNamgeChangeDialog.dismiss();
			}
		});
		// 初期状態は押下不能
//		mNamgeChangeDialog.mBtnOk.setEnabled(false);
		mNamgeChangeDialog.show();
		
//		mChangeText = new EditText(this);
//		mChangeText.setInputType(InputType.TYPE_CLASS_TEXT);
//		mChangeText.setText("");
//		mChangeText.setHint(R.string.folder_hint_new);
//
//		new AlertDialog.Builder(getParent())
//			.setTitle(getResources().getString(R.string.folder_title_change_other_name))
//			.setIcon(android.R.drawable.ic_dialog_info)
//			.setView(mChangeText)
//			.setMessage(getResources().getString(R.string.folder_message_change_name))
//			.setPositiveButton(android.R.string.cancel, null)
//			.setNegativeButton(android.R.string.ok, mNameChangeListener)
//			.show();
	}



	/**
	 * 削除ダイアログを表示し、フォルダの削除を行います
	 */
	public void showDeleteDialog() {

		// メッセージ作成
		String message;

		// 隠しフォルダ表示状態
		boolean containsDotfile = PreferenceUtil.getBooleanPreferenceValue(LocalFolderActivity.this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		// シークレット表示状態
		boolean isSecret = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, true);

		// メッセージをシークレット非表示状態でフォルダ内にシークレットファイルが有れば変更
		File targetDir = new File(mSelectFolder);
		if (!isSecret && MediaUtil.checkSecret(targetDir, containsDotfile)) {
			message = getResources().getString(R.string.folder_message_delete_secret);
		} else {
			message = getResources().getString(R.string.folder_message_delete);
		}

		message = message.replace("[0]", mSelectFolder);

		new AlertDialog.Builder(getParent())
		.setTitle(getResources().getString(R.string.folder_title_delete))
		.setIcon(android.R.drawable.ic_dialog_info)
		.setMessage(message)
		.setPositiveButton(getResources().getString(android.R.string.cancel), null)
		.setNegativeButton(getResources().getString(android.R.string.ok), mDeleteListener)
		.show();
	}



	/** 隠しフォルダ設定リスナー */
	DialogInterface.OnClickListener mSetHiddenListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			
			NomediaTask nomediaTask = new NomediaTask(true);
			nomediaTask.execute();
		}
	};



	/** 非隠し設定リスナー */
	DialogInterface.OnClickListener mSetUnHiddenListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			
			NomediaTask nomediaTask = new NomediaTask(false);
			nomediaTask.execute();
			
//			if (IOUtil.deleteFile(new File(mSelectFolder, ApplicationDefine.NO_MEDIA))) {
//				Toast.makeText(LocalFolderActivity.this, getResources().getString(R.string.folder_message_setting_no_hidden_success), Toast.LENGTH_SHORT).show();
//			} else {
//				Toast.makeText(LocalFolderActivity.this, getResources().getString(R.string.folder_message_setting_no_hidden_failure), Toast.LENGTH_SHORT).show();
//			}
		}
	};



	/** シークレット設定リスナー */
	DialogInterface.OnClickListener mSetSecretListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			
			// シークレットに設定
			SecretTask task = new SecretTask(true);
			task.execute();
			
			/*
			String password = PreferenceUtil.getPreferenceValue(LocalFolderActivity.this, ApplicationDefine.KEY_SECRET_PASSWORD, null);

			// パスワード未設定の場合は、パスワード設定ダイアログを表示
			if (password == null) {

				final SecretPasswordDialog spd = new SecretPasswordDialog(getParent());
				spd.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						if(spd.mIsSetPassword) {
							// シークレットに設定
							SecretTask task = new SecretTask(true);
							task.execute();
						}
					}
				});
				spd.show();
				dialog.dismiss();

			} else {
				// シークレットに設定
				SecretTask task = new SecretTask(true);
				task.execute();
			}*/
			}
	};



	/** シークレット解除リスナー */
	DialogInterface.OnClickListener mSetUnSecretListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {

			String password = PreferenceUtil.getPreferenceValue(LocalFolderActivity.this,ApplicationDefine.KEY_SECRET_PASSWORD,null);

			// パスワード設定済みの場合は、パスワード確認ダイアログを表示
			if (password != null) {

				final SecretCheckPasswordDialog spd = new SecretCheckPasswordDialog(getParent());
				spd.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						if (spd.mIsSetPassword) {
							// シークレット解除
							SecretTask task = new SecretTask(false);
							task.execute();
						}
					}
				});
				spd.show();
				dialog.dismiss();
				// パスワード未設定の場合は、無条件で解除
			} else {
				// シークレット解除
				SecretTask task = new SecretTask(false);
				task.execute();
			}
		}
	};

	/**
	 * フォルダ名変更タスク
	 */
	public class NameChangeTask extends AsyncTask<Void, Void, Boolean> {

		String mErrorExist = null;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mIsControllFolder = true;
			
			// プログレスダイアログを設定　表示
			mProgressDialog = new ProgressDialog(LocalFolderActivity.this.getParent());
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setMessage(getString(R.string.folder_message_change_progress));
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {

			// フォルダ名変更
//			String newName = deleteTopDot(mChangeText.getText().toString());
			String newName = deleteTopDot(mNamgeChangeDialog.mTxtEdit.getText().toString().trim());

			newName = newName.replaceAll("[　|\\s]+", "").trim();
			
			File file = new File(mSelectEntry.getPath());
			
				String newPath = file.getParent() + "/" + newName.trim();
			// FIXME 255文字以内なのに、androidで見えなくなってしまう。暫定対応として100文字制限をかけます。
			if (newPath.length() > 100) return false;
				
				File newFile = new File(newPath);
				
			boolean result = false;
				
			String newFolderPath = IOUtil.changeFolderName(newName, file);
				
			if (newFolderPath == null) {
				if (newFile.exists()) mErrorExist = getString(R.string.folder_message_change_existing);
			} else {
				
				// DBのデータを書き換える
				boolean metadataChange = MediaMetaDataAccessor.updateDirPath(mDatabase, mSelectEntry.getPath(), newFolderPath);
				boolean indexChange = MediaIndexesAccessor.updateIndexesPath(OpenHelper.cache.getDatabase(), mSelectEntry.getPath(), newFolderPath);
				
				MediaStoreOperation.scanAndDeleteMediaStoreEntry(getApplicationContext(), file, new File(newFolderPath), true);
				
				result = indexChange && metadataChange;
			}

			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			mNamgeChangeDialog.dismiss();
			
			String message;

			if (result) {
				mCompMessage = getString(R.string.folder_message_change_name_success);
			} else {
				if (mErrorExist == null) {
					mCompMessage = getString(R.string.folder_message_change_name_failure);
				} else {
					mCompMessage = mErrorExist;
				}
			}

			// 終了メッセージ表示
//			Toast.makeText(LocalFolderActivity.this, message, Toast.LENGTH_SHORT).show();

			mIsControllFolder = false;
			
			findLocalMedia();
		}
	}

	
	
	/**
	 * 隠しファイル操作タスク
	 */
	public class NomediaTask extends AsyncTask<Void, Void, Boolean> {

		/** シークレット設定/解除フラグ */
		private boolean mSetNomedia;

		public NomediaTask(boolean setNomedia) {
			mSetNomedia = setNomedia;
		}


		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mIsControllFolder = true;
			
			mProgressDialog = new ProgressDialog(LocalFolderActivity.this.getParent());
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			
			if (mSetNomedia) {
				mProgressDialog.setMessage(getString(R.string.folder_message_progress_nomedia));	
			} else {
				mProgressDialog.setMessage(getString(R.string.folder_message_progress_no_nomedia));
			}
			
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			
			boolean result = false;
			
			if (mSetNomedia) {
				
				try {
					result = MediaUtil.setNomedia(mSelectFolder);
				} catch (IOException e) {
//					e.printStackTrace();		/*$debug$*/
				}
				
			} else {
				result = IOUtil.deleteFile(new File(mSelectFolder, ApplicationDefine.NO_MEDIA));
			}
			
			
			// 隠しフォルダ処理
			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			// シークレット設定時
			if (mSetNomedia) {

				if (result) {
					mCompMessage = getString(R.string.folder_message_setting_hidden_success);
				} else {
					mCompMessage = getString(R.string.folder_message_setting_hidden_failure);
				}

			// シークレット解除時
			} else {

				if (result) {
					mCompMessage = getString(R.string.folder_message_setting_no_hidden_success);
				} else {
					mCompMessage = getString(R.string.folder_message_setting_no_hidden_failure);
				}
			}
			
			mIsControllFolder = false;
			
			findLocalMedia();
		}
	}
	


	/**
	 * シークレット操作タスク
	 */
	public class SecretTask extends AsyncTask<Void, Void, Boolean> {

		/** シークレット設定/解除フラグ */
		private boolean mSetSecret;

		public SecretTask(boolean setSecret) {
			mSetSecret = setSecret;
		}


		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mIsControllFolder = true;
			
			mProgressDialog = new ProgressDialog(LocalFolderActivity.this.getParent());
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			
			if (mSetSecret) {
				mProgressDialog.setMessage(getString(R.string.folder_message_progress_secret));	
			} else {
				mProgressDialog.setMessage(getString(R.string.folder_message_progress_no_secret));
			}
			
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			// 隠しフォルダ表示状態
			boolean containsDotFile = PreferenceUtil.getBooleanPreferenceValue(LocalFolderActivity.this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);

			// シークレットに設定
			boolean result = MediaUtil.changeMediaSecret(mDatabase, mSelectFolder, mSetSecret, containsDotFile);
			if(mSetSecret && result){
				MediaStoreOperation.deleteMediaStoreEntry(getApplicationContext(), new File(mSelectFolder));
			}
			if(!mSetSecret && result){
				MediaUtil.scanMedia(getApplicationContext(), new File(mSelectFolder), false);
			}
			
			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			String message;

			// シークレット設定時
			if (mSetSecret) {

				if (result) {
					mCompMessage = getString(R.string.folder_message_setting_secret_success);
				} else {
					mCompMessage = getString(R.string.folder_message_setting_secret_failure);
				}

			// シークレット解除時
			} else {

				if (result) {
					mCompMessage = getString(R.string.folder_message_setting_no_secret_success);
				} else {
					mCompMessage = getString(R.string.folder_message_setting_no_secret_failure);
				}
			}

//			mProgressDialog.dismiss();
			
			// 終了メッセージ表示
//			Toast.makeText(LocalFolderActivity.this, message, Toast.LENGTH_SHORT).show();

			mIsControllFolder = false;
			
			findLocalMedia();
		}
	}



	/**
	 * メディアファイル削除タスク
	 */
	public class DeleteTask extends AsyncTask<Void, Void, Boolean> {

		/** シークレット設定/解除フラグ */
		private String mBasePath;


		public DeleteTask(String basePath) {
			mBasePath = basePath;
		}


		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mIsControllFolder = true;
			
			// プログレスダイアログを設定　表示
			mProgressDialog = new ProgressDialog(LocalFolderActivity.this.getParent());
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setMessage(getString(R.string.folder_message_delete_progress));
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			// 隠しフォルダ表示状態
			boolean isHidden = PreferenceUtil.getBooleanPreferenceValue(LocalFolderActivity.this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);

			// メディアファイルの削除
			return MediaUtil.deleteAllMedia(mDatabase, mBasePath, isHidden);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			String message;

			if (result) {
				mCompMessage = getString(R.string.folder_message_delete_success);
				
				File base = new File(mBasePath);
				
				// メディアファイル以外のファイルも、フォルダ内に存在しなければ、フォルダそのものを削除
				if (base.listFiles().length == 0) {
					if(!base.delete()) {
						mCompMessage = getString(R.string.folder_message_delete_folder);
					}
				}
				MediaStoreOperation.deleteMediaStoreEntry(getApplicationContext(), new File(mBasePath));
			} else {
				mCompMessage = getString(R.string.folder_message_delete_failure);
			}

			// 終了メッセージ表示
//			Toast.makeText(LocalFolderActivity.this, message, Toast.LENGTH_SHORT).show();
			mIsControllFolder = false;
			
			findLocalMedia();
		}
	}



	/** ソート設定リスナー */
	DialogInterface.OnClickListener mSetSortListener = new DialogInterface.OnClickListener() {

		public void onClick(DialogInterface dialog, int which) {

			mComparator = null;

			//コンプレータを設定
			settingComparator(which);

			//プリファレンスにソート情報を登録
			PreferenceUtil.setIntPreferenceValue(LocalFolderActivity.this, LAST_TIME_SORT, which);
//			if (mFlgUseListStyle) {
//				mListAdapter.sort(mComparator);
//			} else {
//				mFolderAdapter.sort(mComparator);
//			}
			
			mIsControllFolder = false;
			
			findLocalMedia();
//			findMedia();
		}
	};



	/** フォルダ名変更リスナー */
	OnClickListener mNameChangeListener = new OnClickListener() {
		
		@Override
		public void onClick(View view) {
			NameChangeTask task = new NameChangeTask();
			task.execute();
		}
	};



	/** フォルダ内メディアファイルの削除リスナー */
	DialogInterface.OnClickListener mDeleteListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {

			// メディアファイルを削除
			DeleteTask task = new DeleteTask(mSelectFolder);
			task.execute();
		}
	};



	/** 終了確認ダイアログリスナー */
	DialogInterface.OnClickListener mFinishListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			finish();
		}
	};



	/**
	 * スキャニング処理実行
	 */
	public void findMedia() {

		// プログレスを念のために削除
//		if (mScanProgress != null && mScanProgress.isShowing()) mScanProgress.dismiss();

		// スキャナプログレス表示
		//showScanProgress();

		int callback;

		// アダプタに登録されているデータをクリア
		callback = FOLDER_IMAGE_NUMBER;

		// 値のクリア
		mDCIMFolder.clear();
		mEntries.clear();
//		mFolderEntries.clear();

		/*
		if (mListAdapter != null) {
			mListAdapter.clearItem();
		}
		if (mFolderAdapter != null) {
			mFolderAdapter.clearItem();
		}
		*/
		doFindMedia(callback);
	}



	protected void showScanProgress() {
//		mScanProgress = new ProgressDialog(this);
//		mScanProgress.setMessage(getString(R.string.folder_message_progress));
//		mScanProgress.setCancelable(false);
//		mScanProgress.show();
	}



	protected void doFindMedia(int callback) {
		// 画像フォルダ・画像一覧をスキャニング
		mMediaScanner = new JorlleMediaScanner();

		// お気に入りでフィルタリング
		if (mFilteringFavorite) {
			mMediaScanner.favorite();
		}

		// 開始日でフィルタリング
		if (mStartDate != null) {

			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(mStartDate.getTimeInMillis());

			mMediaScanner.startTime(cal);
		}

		// 終了日でフィルタリング
		if (mEndDate != null) {

			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(mEndDate.getTimeInMillis());

			mMediaScanner.endTime(cal);
		}

		// タグでフィルタリング
		if (mTagList.size() > 0) {
			for (String tag : mTagList) {
				mMediaScanner.addTag(tag);
			}
		}

		// 隠しフォルダ表示状態
		boolean hiddenState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);
		// シークレット表示状態
		boolean secretState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, true);

		mMediaScanner.sort(mComparatorMedia).maxCallback(callback).scanSubfolder(true).scanSecret(secretState).scanNomedia(hiddenState).findMedia(this);
	}



	/**
	 * フォルダ一覧の表示形式を切り替えます
	 */
	public void changeViewMode() {

		// フォルダグリッド形式への切り替え
		if (mFlgUseListStyle) {

//			mImageChange.setImageResource(R.drawable.ic_list);

			// 表示状態切り替え
			mGrid.setVisibility(View.VISIBLE);
			mList.setVisibility(View.GONE);

			// リストの中身と、ソート条件を受け渡す
//			mFolderAdapter.setEntryList(mListAdapter.getEntryList());
			mFolderAdapter.setComparator(mListAdapter.getComparator());

			// アダプタ再設定
			mGrid.setAdapter(mFolderAdapter);

			registerForContextMenu(mGrid);

			mFlgUseListStyle = false;


		// リスト形式への切り替え
		} else {

//			mImageChange.setImageResource(R.drawable.ic_grid);

			// 表示状態切り替え
			mList.setVisibility(View.VISIBLE);
			mGrid.setVisibility(View.GONE);

			// リストの中身と、ソート条件を受け渡す
//			mListAdapter.setEntryList(mFolderAdapter.getEntryList());
			mListAdapter.setComparator(mFolderAdapter.getComparator());

			// アダプタ再設定
			mList.setAdapter(mListAdapter);

			registerForContextMenu(mList);

			mFlgUseListStyle = true;
		}

		PreferenceUtil.setBooleanPreferenceValue(this, ApplicationDefine.KEY_FOLDER_VIEW_MODE, mFlgUseListStyle);
	}



	/**
	 * 文字列の先頭の「.」を削除します
	 *
	 * @param text	文字列
	 * @return	フォーマット文字列
	 */
	public String deleteTopDot(String text) {

		int textSize = text.length();
		String result = text;

		// 先頭からチェックを行い、「.」があれば除去
		for (int i = 0; i < textSize; i++) {
			if (".".equals(result.substring(0, 1))) {
				result = text.substring(i + 1, text.length());
			} else {
				break;
			}
		}

		return result;
	}



	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// 画面の向き切り替え時に、入力情報を引き継ぐ
		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT || newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setViewDate();
			View view = getWindow().getDecorView();
			changeActivity(view);
		}
	}



	/**
	 * 画面切り替え時、切り替え前の値を引き継ぎます
	 */
	public void setViewDate() {

		// レイアウトのビューをクリアし、内部の親子関係をクリアする
		if(mFrameLayout!=null) {
			mFrameLayout.removeAllViews();
		}
		mFrameLayout = null;

		// 現在の向きに応じたレイアウトを再設定
		setContentView(R.layout.folder);

		// レイアウトの初期化
		init();

		//showFilteringMenu(filtering);

		// フォルダ一覧のソート
		// リスト形式
//		if (mFlgUseListStyle) {	//**修正
//			mListAdapter.sort(mComparator);
//
//		// フォルダ形式
//		} else {
//			mFolderAdapter.sort(mComparator);
//		}
	}

	private boolean isFinding = false;
	private boolean isFindRequest = false;
	
	synchronized private void findLocalMedia() {

		if(isFinding) {
			isFindRequest = true;
			return;
		}
		isFinding = true;
		isFindRequest = false;
		
		if(loadCacheEntries()) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					findLocalMediaExec();
				}
			});
			return;
		}

		findLocalMediaExec();
	}
	
	private void findLocalMediaExec() {
		
		// お気に入りチェック状態取得
		mFilteringFavorite = false;

		// 開始日取得
		mStartDate = null;

		// 終了日取得
		mEndDate = null;

		// 開始日をフォーマットし、設定値に登録
		if (mStartDate != null) {
			String prefDate = mStartDate.get(Calendar.YEAR) + "," + mStartDate.get(Calendar.MONTH) + "," + mStartDate.get(Calendar.DAY_OF_MONTH);
			PreferenceUtil.setPreferenceValue(LocalFolderActivity.this, ApplicationDefine.KEY_FILTERING_START_DATE, prefDate);
		}

		// 終了日をフォーマットし、設定値に登録
		if (mEndDate != null) {
			String prefDate = mEndDate.get(Calendar.YEAR) + "," + mEndDate.get(Calendar.MONTH) + "," + mEndDate.get(Calendar.DAY_OF_MONTH);
			PreferenceUtil.setPreferenceValue(LocalFolderActivity.this, ApplicationDefine.KEY_FILTERING_END_DATE, prefDate);
		}

		// スキャニング
		findMedia();

		//notifyAdpterDataSetChanged();
	}

	/** DCIM直下フォルダの格納リスト */
	private List<FolderEntry> mDCIMFolder = new ArrayList<LocalFolderActivity.FolderEntry>();

	/**
	 * スキャナ　列挙開始時処理
	 */
	@Override
	public void onStartFolder(File folder) {
		// フォルダ情報クラスを生成し、フォルダパスを格納
		FolderEntryImpl fe = new FolderEntryImpl();
		fe.mFile = folder;

		//DCIMのPathを取得
		if(mDcimPath == null)
			mDcimPath = Environment.getExternalStoragePublicDirectory("DCIM").getPath();

		if(folder.getPath().indexOf(mDcimPath) >= 0){
			// DCIMフォルダは別リストに退避
			mDCIMFolder.add(fe);
//			mEntries.add(0, fe);
		}else{
			mEntries.add(fe);
		}

	}

	private static String mDcimPath;

	/**
	 * スキャナ　メディアファイル発見時処理
	 */
	@Override
	public void onFound(File file) {

		// フォルダ内のメディアファイル情報を格納
		if (mEntries != null) {
			FolderEntryImpl fe = null;
			if(mDcimPath != null)
				if(file.getPath().indexOf(mDcimPath) >= 0)
					// DCIMリストからエントリを取得
					fe = (FolderEntryImpl) mDCIMFolder.get(mDCIMFolder.size() - 1);
//					fe = (FolderEntryImpl)mEntries.get(0);

			if(fe == null)
				fe = (FolderEntryImpl) mEntries.get(mEntries.size() -1);

			fe.getImages().add(file.getPath());

			// フォルダ内最新ファイルの更新日時を取得
			if (fe.mLastModified == null) {
				fe.mLastModified = new File(fe.getImages().get(0)).lastModified();
			}
		}
	}

	/**
	 * スキャナ　フォルダのメディアファイル列挙終了時処理
	 */
	@Override
	public void onEndFolder(File folder, int mediaCount) {

//		boolean isDcimPath = false;

		FolderEntry fe = null;
		if(mDcimPath != null)
			if(folder.getPath().indexOf(mDcimPath) >= 0) {
				// DCIMリストからエントリを取得
				fe = mDCIMFolder.get(mDCIMFolder.size() -1);
//				fe = (FolderEntryImpl)mEntries.get(0);

//				isDcimPath = true;
			}

		if(fe == null)
			fe = mEntries.get(mEntries.size() -1);

		// メディアファイルの件数を格納
		if (fe instanceof FolderEntryImpl) {
			((FolderEntryImpl) fe).mMediaCount = mediaCount;
		}
	}



	/**
	 * 処理終了時
	 */
	@Override
	public void onComplete() {

		int resultSize = 0;

		// 処理終了時にソートをかける
		if (mFlgUseListStyle) {
			if(mComparator != null) {
				Collections.sort(mDCIMFolder, mComparator);
				Collections.sort(mEntries, mComparator);
			}
		}
		
		if(checkUpdateList()) {
			if (mListAdapter != null) {
				mListAdapter.clearItem();
			}
			if (mFolderAdapter != null) {
				mFolderAdapter.clearItem();
			}
			
		    mFolderEntries.addAll(mDCIMFolder);
		    mFolderEntries.addAll(mEntries);
		    
			notifyAdpterDataSetChanged();
		    
			// フォルダキャッシュを保存
			saveCacheEntries();
		}
		
		
		
		// リスト形式
		if (mFlgUseListStyle) {
			/*
			mListAdapter.sort(mComparator);

			// DCIM直下のフォルダをソート
			if(mComparator != null)
				Collections.sort(mDCIMFolder, mComparator);
			
			mListAdapter.addAllPosition(mDCIMFolder, 0);
			*/
			mListAdapter.setComparator(mComparator);

			resultSize = mListAdapter.getCount();

		// フォルダ形式
		} else {
			mFolderAdapter.sort(mComparator);
			resultSize = mFolderAdapter.getCount();

		}

		boolean notFirst = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_NOT_FIRST_TIME);
		
		// 該当フォルダが存在しない
		if (resultSize <= 0 && notFirst) {
			Toast.makeText(LocalFolderActivity.this, getString(R.string.folder_message_media_nothing), Toast.LENGTH_SHORT).show();

			// ビューを更新
			notifyAdpterDataSetChanged();
		}

		
		// フォルダ操作後の再描画処理だった場合、プログレスを消去
		if (mProgressDialog != null && mProgressDialog.isShowing() && !mIsControllFolder) {
			
			mProgressDialog.dismiss();
			mProgressDialog = null;
			
			Toast.makeText(LocalFolderActivity.this, mCompMessage, Toast.LENGTH_SHORT).show();
		}
		
		isFinding = false;
		if(isFindRequest) {
			findLocalMedia();
		}
		
		// プログレス削除
//		if (mScanProgress != null) {
//			mScanProgress.dismiss();
//		}
	}



	protected void notifyAdpterDataSetChanged() {
		if (mFlgUseListStyle) {
			mListAdapter.notifyDataSetChanged();
		} else {
			mFolderAdapter.notifyDataSetChanged();
		}
	}



	/**
	 * フォルダ情報格納クラス
	 */
	public static interface FolderEntry {
		/** フォルダパス */
		String getPath();
		/** 最終更新日時 */
		long getLastModified();
		/** 名前 */
		String getName();
		/** サムネイル用画像のパスリスト */
		List<String> getImages();
		/** フォルダ内のメディアファイル数 */
		int getMediaCount();
		/** シリアライズ値 */
		String toSerializeValue();
	}

	class FolderEntryImpl implements FolderEntry {
		private static final long serialVersionUID = 1L;
		
		File mFile;
		ArrayList<String> mImages = new ArrayList<String>();
		int mMediaCount;
		Long mLastModified;

		@Override
		public String getPath() {
			return mFile.getPath();
		}

		@Override
		public long getLastModified() {
			return mLastModified;
		}

		@Override
		public String getName() {
			return mFile.getName();
		}

		@Override
		public List<String> getImages() {
			return mImages;
		}

		@Override
		public int getMediaCount() {
			return mMediaCount;
		}

		@Override
		public String toSerializeValue() {
	    	String value = getPath() + "," + mMediaCount + "," + mLastModified;
		    for(String path : getImages()) {
		    	value += "," + path;
		    }
			return value;
		}
		
	}


	/**
	 * 日付の新しい順
	 */
	public static class DateAscender implements Comparator<FolderEntry>{

		@Override
		public int compare(FolderEntry lhs, FolderEntry rhs) {

			long d = rhs.getLastModified() - lhs.getLastModified();
			if(d > 0)return 1;
			if(d < 0)return -1;
			return 0;
		}
	}

	/**
	 * 日付の古い順
	 */
	public static class DateDescender implements Comparator<FolderEntry>{

		@Override
		public int compare(FolderEntry lhs, FolderEntry rhs) {
			long d = lhs.getLastModified() - rhs.getLastModified();
			if(d > 0)return 1;
			if(d < 0)return -1;
			return 0;
		}
	}

	/**
	 * 名前昇順
	 */
	public static class NameAscender implements Comparator<FolderEntry>{

		@Override
		public int compare(FolderEntry lhs, FolderEntry rhs) {
			return lhs.getName().compareToIgnoreCase(rhs.getName());
		}
	}

	/**
	 * 名前降順
	 */
	public static class NameDescender implements Comparator<FolderEntry>{

		@Override
		public int compare(FolderEntry lhs, FolderEntry rhs) {
			return rhs.getName().compareToIgnoreCase(lhs.getName());
		}
	}

	/**
	 * 件数昇順
	 */
	public static class CountAscender implements Comparator<FolderEntry>{

		@Override
		public int compare(FolderEntry lhs, FolderEntry rhs) {
			long d = lhs.getMediaCount() - rhs.getMediaCount();
			if(d > 0)return 1;
			if(d < 0)return -1;
			return 0;
		}
	}

	/**
	 * 件数降順
	 */
	public static class CountDescender implements Comparator<FolderEntry>{

		@Override
		public int compare(FolderEntry lhs, FolderEntry rhs) {
			long d = rhs.getMediaCount() - lhs.getMediaCount();
			if(d > 0)return 1;
			if(d < 0)return -1;
			return 0;
		}
	}
}
