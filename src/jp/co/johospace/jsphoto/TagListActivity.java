package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.accessor.MediaMetaDataAccessor;
import jp.co.johospace.jsphoto.accessor.TagMasterAccessor;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.folder.AsyncTagAdapter;
import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner.OnFoundListener;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

/**
 * タグ一覧アクティビティです
 */
public class TagListActivity extends NavigatableActivity implements OnClickListener, OnItemClickListener {
	
	/** データベース */
	private static SQLiteDatabase mDataBase = OpenHelper.external.getDatabase();
	/** 初回起動時かどうかのキー */
	public static final String KEY_NOT_FIRST_TAG_TIME = "notFirstTagTime";
	
	/** チュートリアル画面　リクエスト */
	private static final int REQUEST_TUTORIAL = 100;
	
	/** シークレットのエントリ */
	private TagEntryImpl mSecret;
	
	/** お気に入り・シークレット表示用コンテナ */
	private LinearLayout mFavSecLayout;
	
	
	/** リストビュー */
	private ListView mListView;
	/** 画像アダプタ */
	protected AsyncTagAdapter mTagAdapter;
	
	
	/** タグのエントリ */
	private ArrayList<TagEntry> mTagEntry;
	
	/** シークレット検索用スキャナ */
	private JorlleMediaScanner mSecretScanner;
	/** シークレットメディア数 */
	private int mSecretMediaCount;

	/** インデックス id */
	private int INDEX_TAG_ID = 0;
	/** インデックス パス */
	private int INDEX_TAG_PATH = 1;
	/** インデックス ファイル名 */
	private int INDEX_TAG_FILE_NAME = 2;
	/** インデックス データタイプ */
	private int INDEX_TAG_DATA_TYPE = 3;
	/** インデックス タグ名 */
	private int INDEX_TAG_NAME = 4;
	/** インデックス 最終更新日時 */
	private int INDEX_TAG_UPDATE_TIME = 5;
	
	/** インデックス　お気に入り */
	private static final int
		INDEX_FAVORITE_ID = 0,
		INDEX_FAVORITE_DIRPATH = 1,
		INDEX_FAVORITE_NAME = 2;
	
	/** メニュー項目 */
	protected static final int
		MENU_ITEM_SEARCH = 1,
		MENU_ITEM_SETTING = 2;

	/** 初期処理タスク */
	private AsyncTask<Void, Void, Void> mLoadDataTask;
	/** 初期処理タスクプログレスバーの値 */
	private int mNumProgress = 0;

	/** シークレット表示状態 */
	boolean secretState;

	/** 隠しフォルダ表示状態 */
	boolean isDisplayHidden;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.tag_list);
	}

	@Override
	protected void onResume() {
		super.onResume();

		//初回起動時、チュートリアル画面を表示する
		boolean first = PreferenceUtil.getBooleanPreferenceValue(this, KEY_NOT_FIRST_TAG_TIME, true);
		
		if(first){
//			PreferenceUtil.setBooleanPreferenceValue(this, KEY_NOT_FIRST_TAG_TIME, false);
			Intent intent = new Intent(TagListActivity.this, TutorialTagActivity.class);
			goNextHistory("TutorialTagActivity", intent);
		} else {
			init();
		}
	}
	
	/**
	 * 初期処理
	 * 
	 */
	private void init() {

		// リストビュー設定
		mListView = (ListView)findViewById(R.id.listTagImage);
		mListView.setOnItemClickListener(this);
		mListView.setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				// TODO Auto-generated method stub
				LinearLayout layout = ((LinearLayout)view.getTag());
				for(int i = 0; i < layout.getChildCount(); i++) {
					((UXAsyncImageView)layout.getChildAt(i)).recycle();
				}
			}
		});

		mTagEntry = new ArrayList<TagEntry>();

		// シークレット表示設定取得
		secretState = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, true);

		// 隠しフォルダ表示フラグ
		isDisplayHidden = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);

		// シークレット表示判定
		if (secretState) {
			// シークレットの場合はスキャナでシークレットファイルを取得後に描画処理を走らせる
			setSecret();
		} else {
			startLoadData();
		}
	}
	
	@Override
	protected void onDestroy() {
		if (mTagAdapter != null) {
			mTagAdapter.dispose();
		}
		
		if (mSecretScanner != null) {
			mSecretScanner.dispose();
		}
		
		super.onDestroy();
	}
	

	/**
	 * カテゴリ　シークレット設定
	 */
	public void setSecret() {

		mSecretMediaCount = 0;
		
		mSecret = new TagEntryImpl();
		
		// 表示名
		mSecret.mTagName = getString(R.string.tagList_title_secret);
		
		// タグカテゴリ名
		mSecret.mTagCategory = ApplicationDefine.TAG_CATEGORY_SECRET;

		// スキャナが走らせる前にプログレスを回す
		startProgress();

		// 画像フォルダ・画像一覧をスキャニング
		mSecretScanner = new JorlleMediaScanner();
		mSecretScanner.sort(new JorlleMediaScanner.DateAscender()).
								scanOnlySecret().scanSubfolder(true).scanNomedia(isDisplayHidden).findMedia(mSecretListener);
	}

	/**
	 * カテゴリ　お気に入り設定
	 */
	public void setFavorite() {
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
		
		try {
			
			// 隠しフォルダを表示しない場合
			if (!isDisplayHidden) {

				selection += " AND " + CMediaMetadata.NAME + " NOT LIKE ?";
				selection += " AND " + CMediaMetadata.DIRPATH + " NOT LIKE ?";
				selectionArgs.add("." + "%");
				selectionArgs.add("%" + "/." + "%");

				// メタデータに登録されている隠しフォルダ一覧を取得
				dir = MediaMetaDataAccessor.queryHiddenFolder(mDataBase, ApplicationDefine.MIME_FAVORITE, false);
				
				if (dir.size() > 0) {
					
					selection += " AND " + CMediaMetadata.DIRPATH + " NOT LIKE ";
					
					boolean isFirst = true;
					
					int size = dir.size();
					
					// ディレクトリが隠しフォルダか判定
					for (int i = 0; i < size; i++) {
							
						// 合わせてwhere条件を作成
						if (isFirst) {
							selection += "?";
							isFirst = false;
						} else {
							selection += " AND " + CMediaMetadata.DIRPATH + " NOT LIKE ?";
						}
					}
					
					for (String dirpath : dir) {
						selectionArgs.add(dirpath + "%");
					}
				}
			}
			
						
			// シークレットを表示
			if (secretState) {
				selectionFavorite = selection;
				
			// シークレットを非表示
			} else {
				selectionFavorite = selection + " AND " + CMediaMetadata.NAME + " NOT LIKE ? " ;
				selectionArgs.add("%" + ApplicationDefine.SECRET);
			}
			
			TagEntryImpl entry = new TagEntryImpl();

			// お気に入りの画像を取得
			cursor = mDataBase.query(CMediaMetadata.$TABLE, columns, selectionFavorite, selectionArgs.toArray(new String[1]), null, null, orderBy);
			
			// カテゴリ名
			entry.mTagName = getString(R.string.tagList_title_favorite);
			
			// タグ種別
			entry.mTagCategory = ApplicationDefine.TAG_CATEGORY_FAVORITE;

			// 最新の一件を取得し、情報を設定
			while (cursor.moveToNext()) {
				
				String pathDir = cursor.getString(INDEX_FAVORITE_DIRPATH);
				String pathName = cursor.getString(INDEX_FAVORITE_NAME);

				// 画像のパス
				entry.mPathList.add(pathDir + "/" + pathName);
			}
			
			// タグに該当する件数
			entry.mMediaCount = entry.mPathList.size();

			boolean isCategoryExist = entry.mMediaCount > 0;
			boolean isCategoryHide = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_TAG_CATEGORY_FAVORITE, true);
			
			if (isCategoryExist) {
				mTagEntry.add(entry);
				PreferenceUtil.setBooleanPreferenceValue(this, ApplicationDefine.KEY_TAG_CATEGORY_FAVORITE, true);
			} else {
				// メディア数が０、かつカテゴリを非表示に設定されていたら
				if (isCategoryHide) {
					mTagEntry.add(entry);
				}
			}

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
	
	/**
	 * タグが付けられている画像をListViewに表示
	 */
	private void setTag() {

		int tagCount = 0;
		
		// 設定されているタグの一覧を取得し表示
		Cursor c = null;
		Cursor oneTag = null;

		// 隠しフォルダのWHERE句追加フラグ
		boolean isSelectionAdd = false;

		String hiddenSelection = "";

		// ディレクトリのリスト
		List<String> dir = new ArrayList<String>();

		try {
			
			// TODO
			c = TagMasterAccessor.queryTag(mDataBase, true, true);

			// 隠しフォルダを表示しない場合
			if (!isDisplayHidden) {

				// メタデータに登録されている隠しフォルダ一覧を取得
				dir = MediaMetaDataAccessor.queryHiddenFolder(mDataBase, ApplicationDefine.MIME_TAG, false);
				
				if (dir.size() > 0) {
					
					hiddenSelection += " AND " + CMediaMetadata.DIRPATH + " NOT LIKE ";
					
					boolean isFirst = true;
					
					int size = dir.size();
					
					// ディレクトリが隠しフォルダか判定
					for (int i = 0; i < size; i++) {
							
						// 合わせてwhere条件を作成
						if (isFirst) {
							hiddenSelection += "?";
							isFirst = false;
						} else {
							hiddenSelection += " AND " + CMediaMetadata.DIRPATH + " NOT LIKE ?";
						}
					}

					isSelectionAdd = true;
				}
			}

			// 取得したタグ名分処理を行う
			while (c.moveToNext()) {
				
				TagEntryImpl entry = new TagEntryImpl();

				String selection = "";
				List<String> selectionArgs = new ArrayList<String>();

				// タグ名
				entry.mTagName = c.getString(1);

				// シークレットを表示
				if (secretState) {
					selection = CMediaMetadata.METADATA_TYPE + " = ?" + 
						    " AND " + CMediaMetadata.METADATA + " = ?";
					
					selectionArgs.add(CMediaMetadata.TYPE_TAG);
					selectionArgs.add(entry.mTagName);
					
				// シークレットを非表示
				} else {
					selection = CMediaMetadata.METADATA_TYPE + " = ?" + 
						    " AND " + CMediaMetadata.METADATA + " = ?" + 
						    " AND " + CMediaMetadata.NAME + " NOT LIKE ?";
					
					selectionArgs.add(CMediaMetadata.TYPE_TAG);
					selectionArgs.add(entry.mTagName);
					selectionArgs.add("%" + ApplicationDefine.SECRET);
				}

				if (!isDisplayHidden) {
					selection += " AND " + CMediaMetadata.NAME + " NOT LIKE ?";
					selection += " AND " + CMediaMetadata.DIRPATH + " NOT LIKE ?";
					selectionArgs.add("." + "%");
					selectionArgs.add("%" + "/." + "%");
				}

				if (isSelectionAdd) {
					selection += hiddenSelection;
					for (String dirpath : dir) {
						selectionArgs.add(dirpath + "%");
					}
				}

				// タグ種別
				entry.mTagCategory = ApplicationDefine.TAG_CATEGORY_TAG;

				// 取得したタグ名でファイルを取得する
				oneTag = mDataBase.query(CMediaMetadata.$TABLE,
												null,
												selection,
												(String[])selectionArgs.toArray(new String[0]),
												null, null, null);

				while (oneTag.moveToNext()) {
					// 画像のパス
					entry.mPathList.add(oneTag.getString(INDEX_TAG_PATH) + "/" + oneTag.getString(INDEX_TAG_FILE_NAME));
					// 画像の最終更新日時
					entry.mLastModified.add(oneTag.getLong(INDEX_TAG_UPDATE_TIME));
				}

				// タグに該当する件数
				entry.mMediaCount = entry.mPathList.size();

				// タグの件数が0以上のものを追加する
				if (entry.getMediaCount() > 0) {
					mTagEntry.add(entry);
					tagCount++;
				}
			}

			// 表示タグが一件も存在しなかった場合、メッセージを表示させるために0件のタグ情報をセット
			if (tagCount < 1) {
				
				boolean isCategoryHide = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_TAG_CATEGORY_TAG, true);
				
				// 非表示状態ならば、カテゴリはセットしない
				if (isCategoryHide) {
					TagEntryImpl entry = new TagEntryImpl();
					entry.mTagCategory = ApplicationDefine.TAG_CATEGORY_TAG;
					entry.mMediaCount = 0;
					mTagEntry.add(entry);
				} 
				
			} else {
				PreferenceUtil.setBooleanPreferenceValue(this, ApplicationDefine.KEY_TAG_CATEGORY_TAG, true);
			}
			
		} finally {
			if (c != null) {
				c.close();
			}
			if (oneTag != null) {
				oneTag.close();
			}
		}
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}
	
	protected AsyncTagAdapter createTagAdapter() {
		AsyncTagAdapter adapter = new AsyncTagAdapter(getApplicationContext(), mTagEntry);
		adapter.setSecretHide(mSecretHideListener);
		adapter.setCategoryHide(mCategoryHideListener);
		return adapter;
	}
	
	
    /**
	 * メニュー作成イベント
	 * @param menu
	 * @return true:成功、false:失敗
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ITEM_SEARCH, 0, getResources().getString(R.string.menu_search)).setIcon(R.drawable.ic_search);
		menu.add(0, MENU_ITEM_SETTING, 0, getResources().getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);
		
		return true;
	}
	
	
	/**
	 * メニュー選択イベント
	 * @param item
	 * @return true:成功、false:失敗
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
				
			// 検索
			case MENU_ITEM_SEARCH:
				Intent intent = new Intent(TagListActivity.this, SearchActivity.class);
				startActivity(intent);
				break;
			// 設定
			case MENU_ITEM_SETTING:
				Intent settingIntent = new Intent(TagListActivity.this, JorllePrefsActivity.class);
				startActivity(settingIntent);
				break;

		}

		return true;
	}

	
	
	/**
	 * タグ情報格納クラス
	 */
	public static interface TagEntry {
		/** タグ名 */
		String getName();
		/** パスリスト */
		ArrayList<String> getPathList();
		/** 最終更新日時 */
		ArrayList<Long> getLastModified();
		/** フォルダ内のメディアファイル数 */
		int getMediaCount();
		/** タグ種別(お気に入り、シークレット、タグ) */
		String getTagCategory();
	}

	class TagEntryImpl implements TagEntry {

		String mTagName;
		ArrayList<String> mPathList = new ArrayList<String>();
		ArrayList<Long> mLastModified = new ArrayList<Long>();
		int mMediaCount;
		String mTagCategory;
		
		@Override
		public String getName() {
			return mTagName;
		}
		@Override
		public ArrayList<String> getPathList() {
			return mPathList;
		}
		@Override
		public ArrayList<Long> getLastModified() {
			return mLastModified;
		}
		@Override
		public int getMediaCount() {
			return mMediaCount;
		}
		@Override
		public String getTagCategory() {
			return mTagCategory;
		}
	}
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		// 画面の向き切り替え時に、入力情報を引き継ぐ
		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT || newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			
			// 初回起動時、チュートリアル画面を表示する
			boolean first = PreferenceUtil.getBooleanPreferenceValue(this, KEY_NOT_FIRST_TAG_TIME, true);
			
			// 初回起動ではなく、カテゴリ表示時のみ、再読み込みを行う
			if (!first && isCategoryExist()) {
			
				setViewDate();
				View view = getWindow().getDecorView();
				changeActivity(view);
			}
		}
	}
	
	
	/**
	 * 画面切り替え時、切り替え前の値を引き継ぎます
	 */
	public void setViewDate() {
		
		// 現在の向きに応じたレイアウトを再設定
		setContentView(R.layout.tag_list);

		// レイアウトの初期化
		init();
	}
	
	
	private class TmpThumbnailLoader implements UXThumbnailLoader {
		@Override
		public boolean loadCachedThumbnail(Object info, int widthHint, UXImageInfo out) {
			return false;
		}

		@Override
		public boolean loadThumbnail(Object info, int widthHint, UXImageInfo out) {
			String path = (String)info;
			byte[] thumbnail = null;
			Bitmap thumbnailBitmap = null;
			Bitmap ret = null;

			try {
				ExifInterface exif = new ExifInterface(path);
				thumbnail = exif.getThumbnail();
			} catch (IOException e) {
			}

			if(thumbnail == null){
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(path, opt);
				opt.inJustDecodeBounds = false;

				int divide = ((opt.outWidth < opt.outHeight)? opt.outHeight: opt.outWidth);
				opt.inSampleSize = divide / 200;

				thumbnailBitmap =  BitmapFactory.decodeFile(path, opt);
			}else{
				thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
			}

			if(thumbnailBitmap == null){
				return false;
			}

			float heightRate = (float)thumbnailBitmap.getHeight() / (float)thumbnailBitmap.getWidth();
			int height = (int)(200 * heightRate);

			ret = Bitmap.createBitmap(200, height, Bitmap.Config.RGB_565);
			Canvas c = new Canvas(ret);
			c.drawBitmap(thumbnailBitmap, new Rect(0,0,thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight()),
					new Rect(0,0, 200, height), new Paint());
			thumbnailBitmap.recycle();

			out.bitmap = ret;
			out.orientation = 270;
			return true;
		}

		@Override
		public void updateCachedThumbnail(Object info, int widthHint, UXImageInfo in) {
		}
	}

	
	/**
	 * シークレットのスキャナリスナー
	 */
	public OnFoundListener mSecretListener = new OnFoundListener() {
		
		@Override
		public void onStartFolder(File folder) {
		}
		
		@Override
		public void onFound(File file) {
			mSecret.mPathList.add(file.getPath());
		}

		@Override
		public void onEndFolder(File folder, int mediaCount) {
			// メディアファイルの数を保持
			mSecretMediaCount += mediaCount;
		}

		@Override
		public void onComplete() {
			mSecret.mMediaCount = mSecretMediaCount;

			// スキャナが終わった為、非同期でデータ取得、画面描画処理
			startLoadData();
		}
	};
	
	/**
	 * シークレット　非表示ボタンのリスナ
	 */
	public OnClickListener mSecretHideListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			PreferenceUtil.setBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, false);
			Toast.makeText(TagListActivity.this, getResources().getString(R.string.tagList_message_secret_guid), Toast.LENGTH_LONG).show();
			init();
		}
	};

	/**
	 * オーバーレイ削除ボタンのリスナ
	 */
	public OnClickListener mCategoryHideListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String categoryTag = String.valueOf(v.getTag());
			
			if (ApplicationDefine.TAG_CATEGORY_FAVORITE.equals(categoryTag)) {
				PreferenceUtil.setBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.KEY_TAG_CATEGORY_FAVORITE, false);
			} else if (ApplicationDefine.TAG_CATEGORY_SECRET.equals(categoryTag)) {
				PreferenceUtil.setBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.KEY_TAG_CATEGORY_SECRET, false);
			} else {
				PreferenceUtil.setBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.KEY_TAG_CATEGORY_TAG, false);
			}
			
			// 表示カテゴリの有無で、処理を切り分け
			if (isCategoryExist()) {
				init();
			} else {
				Intent intent = new Intent(TagListActivity.this, TutorialTagActivity.class);
				goNextHistory("TutorialTagActivity", intent);
			}
		}
	};
	
	/**
	 * 表示するカテゴリーの有無を返します
	 * 
	 * @return	true:表示カテゴリーあり　false:表示カテゴリーなし
	 */
	private boolean isCategoryExist() {
		
		boolean isFavoriteShow = PreferenceUtil.getBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.KEY_TAG_CATEGORY_FAVORITE, true);
		boolean isSecretShow = PreferenceUtil.getBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.KEY_TAG_CATEGORY_SECRET, true);
		boolean isTagShow = PreferenceUtil.getBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.KEY_TAG_CATEGORY_TAG, true);
		boolean isSecretDisp = PreferenceUtil.getBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.PREF_SECRET_FOLDER_DISPLAY, true);
				
		return (isFavoriteShow || (isSecretShow && isSecretDisp) || isTagShow);
	}
	

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// 取得したパスを元に、フォルダ内画像一覧画面に遷移
		TagEntryImpl fe = (TagEntryImpl)mTagAdapter.getItem(position);

		startGridActivity(fe);
	}
	
	protected void startGridActivity(TagEntryImpl fe) {
		Intent intent = new Intent(TagListActivity.this, NewGridActivity.class);
		if (fe == null) {
			return;
		} 
		// お気に入り
		else if (ApplicationDefine.TAG_CATEGORY_FAVORITE.equals(fe.getTagCategory())) {
			
			intent.putExtra(NewGridActivity.INTENT_FOLDER_TYPE, NewGridActivity.FOLDER_TYPE_FAVORITE);

		} 
		// シークレット
		else if (ApplicationDefine.TAG_CATEGORY_SECRET.equals(fe.getTagCategory())) {
			
			intent.putExtra(NewGridActivity.INTENT_FOLDER_TYPE, NewGridActivity.FOLDER_TYPE_SECRET);
			
		} 
		// タグ
		else if (ApplicationDefine.TAG_CATEGORY_TAG.equals(fe.getTagCategory())) {

			intent.putExtra(NewGridActivity.INTENT_FOLDER_TYPE, NewGridActivity.FOLDER_TYPE_TAG);
			intent.putExtra(NewGridActivity.INTENT_TARGET, fe.getName());
		}
		// 不明ならば処理中断
		else {
			return;
		}
		
		// 遷移元判定
		intent.putExtra(NewGridActivity.INTENT_TARGET_PARENT, NewGridActivity.PARENT_TAG);
		
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		goNextHistory("NewGridActivity", intent);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		// 戻るボタン押下時
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// TODO 複数選択時メニューが出ていたら閉じること
			onBackHistory();
			return true;
		// メニューボタン押下時
		} else if (keyCode == KeyEvent.KEYCODE_MENU) {
			openOptionsMenu();
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	private final class LoadDataTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected void onPreExecute() {
			if (!secretState) {
				startProgress();
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			// お気に入り
			setFavorite();

			// シークレット表示確認
			if (secretState && mSecret != null) {
				boolean isCategoryExist = mSecret.mMediaCount > 0;
				boolean isCategoryHide = PreferenceUtil.getBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.KEY_TAG_CATEGORY_SECRET, true);
				
				if (isCategoryExist) {
					mTagEntry.add(mSecret);
					PreferenceUtil.setBooleanPreferenceValue(TagListActivity.this, ApplicationDefine.KEY_TAG_CATEGORY_SECRET, true);
				} else {
					// メディア数が０、かつカテゴリを非表示に設定されていたら
					if (isCategoryHide) {
						mTagEntry.add(mSecret);
					}
				}
			}


			// 一般タグ取得、アダプターセット処理
			setTag();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (isCategoryExist()) {
				// タグアダプタ設定
				mTagAdapter = createTagAdapter();
				mTagAdapter.setDisplayLength();
				mTagAdapter.setRowLength();
				mListView.setAdapter(mTagAdapter);
			} else {
				Intent intent = new Intent(TagListActivity.this, TutorialTagActivity.class);
				goNextHistory("TutorialTagActivity", intent);
			}
			stopProgress();
		}
	}

	/**
	 * 処理中のプログレスを開始します
	 */
	private void startProgress() {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
		mNumProgress++;
	}
	
	/**
	 * 処理中のプログレスを終了します
	 */
	private void stopProgress() {
		mNumProgress--;
		if (mNumProgress <= 0) {
			findViewById(R.id.progress).setVisibility(View.GONE);
			mNumProgress = 0;
		}
		findViewById(R.id.progress).setVisibility(View.GONE);
	}

	private void startLoadData() {
		cancelLoadData();
		mLoadDataTask = new LoadDataTask().execute();
	}

	private void cancelLoadData() {
		if (mLoadDataTask != null && mLoadDataTask.getStatus() == Status.RUNNING) {
			mLoadDataTask.cancel(false);
		}
	}
}
