package jp.co.johospace.jsphoto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.scanner.JorlleMediaScanner;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.util.SizeConv;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import jp.co.johospace.jsphoto.ux.view.UXViewLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.AutoAlbumCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.AutoAlbumCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ContentsNotModifiedException;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.AuthPreference;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Memory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.MemoryDivider;
import jp.co.johospace.jsphoto.v2.onlineservice.ui.SearchResultThumbnailLoader;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * オートアルバムフォルダ一覧アクティビティです
 */
public class AutoAlbumListActivity extends NavigatableActivity implements OnClickListener{
	private static final String tag = AutoAlbumListActivity.class.getSimpleName();

	public static boolean autoUpdated;
	
	/** 一行のレイアウト番号 */
	private static final int
		LAYOUT_NUMBER_FIRST = 0,
		LAYOUT_NUMBER_SECOND = 1,
		LAYOUT_NUMBER_THIRD = 2,
		LAYOUT_NUMBER_FOUTH = 3;

	private static final int MENU_ITEM_CALENDAR = 1;
	private static final int MENU_ITEM_REFRESH = 2;
	private static final int MENU_ITEM_SEARCH = 3;
	private static final int MENU_ITEM_SETTING = 4;

	private static final int REQUEST_CODE_PREFERENCES = 1;
	
	
	

//	private class MemoryTask extends AsyncTask<Boolean, Integer, List<Memory>> {
//		@Override
//		protected void onPreExecute() {
//			startProgress();
//		}
//		
//		@Override
//		protected List<Memory> doInBackground(Boolean... params) {
//			JsMediaServerClient jsMedia = ClientManager.getJsMediaServerClient(AutoAlbumListActivity.this);
////			ExternalServiceClient external = ClientManager.getExternalServiceClient(this, "");
//			ExternalServiceCache cache = new ExternalServiceCacheImpl(AutoAlbumListActivity.this);
//			
//			CachingAccessor accessor = new CachingAccessor(AutoAlbumListActivity.this, jsMedia, null/*external*/, cache, true);
//			if (params.length > 0 && params[0]) {
//				try {
//					return accessor.updateMemoriesCache();
//				} catch (IOException e) {
//					handleException(e, params.length > 0 && params[0]);
//					return null;
//				}
//			}
//
//			try {
//				return accessor.searchMemories();
//			} catch (IOException e) {
//				handleException(e, params.length > 0 && params[0]);
//				return new ArrayList<Memory>();
//			}
//		}
//		
//		@Override
//		protected void onPostExecute(List<Memory> result) {
//			if (result != null) {
//				mEntries.clear();
//				mEntries.addAll(result);
//				
//				createAlbumList();
//			}
//			
//			stopProgress();
//		}
//	}
	
	private class ViewTag{
		int ran;
		AutoAlbumCache.CategoryInfo info;
		UXAsyncImageView[] views;
	}
	
	private class CacheInfo{
		List<AutoAlbumCache.CategoryInfo> categoryInfos;
		AutoAlbumCache cache;
	}
	
	static final List<AutoAlbumCache.CategoryInfo> NOT_MODIFIED = new ArrayList<AutoAlbumCache.CategoryInfo>();
	private class AutoAlbumCacheTask extends AsyncTask<Boolean, Void, CacheInfo>{
		@Override
		protected void onPreExecute(){
			startProgress();
		}
		
		boolean mUpdate;
		@Override
		protected CacheInfo doInBackground(Boolean... params){
			mUpdate = (params.length != 0 && params[0])? true: false;
			
			boolean firstContact = false;
			try{
				CacheInfo info = new CacheInfo();
				info.cache = createAutoAlbumCache(mUpdate);
				
				
				List<AutoAlbumCache.CategoryInfo> infos;
				try {
					if(mUpdate){
						info.cache.updateCache();
						infos = info.cache.getCategories(8);
					} else {
						infos = info.cache.getCategories(8);
						if (infos == null) {
							firstContact = true;
							autoUpdated = true;
							info.cache.updateCache();
							infos = info.cache.getCategories(8);
						}
					}
					info.categoryInfos = infos;
					
				} catch (ContentsNotModifiedException e) {
					Log.d(tag, "received NOT MODIFIED");/*$debug$*/
					info.categoryInfos = NOT_MODIFIED;
				}
				
				
				return info;
			}catch(Exception e){
				handleException(e, params[0] || firstContact);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(CacheInfo info){
			if(info != null){
				if (info.categoryInfos != NOT_MODIFIED) {
					mAutoAlbumCache = info.cache;
					mCategoryInfos = info.categoryInfos;
					
					createAlbumList();
					mAdapter.notifyDataSetChanged();
				}
			}
			
			stopProgress();
			
			if (!mUpdate && !autoUpdated) {
				Log.d(tag, "auto reloading...");/*$debug$*/
				startLoadMemory(true);
				autoUpdated = true;
			}
		}
	}
	
	/**
	 * 設定済みサービスのチェックタスク
	 */
	private class CheckServiceTask extends AsyncTask<List<AuthPreference>, Void, Boolean> {
		
		@Override
		protected Boolean doInBackground(List<AuthPreference>... params) {
			// 片方向同期分
			try {
				// サービスの一覧を取得します
				List<AuthPreference> prefs;
				if (0 < params.length && params[0] != null) {
					prefs = params[0];
				} else {
					JsMediaServerClient client =
							ClientManager.getJsMediaServerClient(AutoAlbumListActivity.this);
					prefs = client.getAuthPreferences(false);
					if (prefs == null) {
						try {
							prefs = client.getAuthPreferences(true);
						} catch (IOException e) {
							prefs = new ArrayList<AuthPreference>();
						}
					} else {
						new UpdateAuthPrefsTask().execute();
					}
				}

				int mediaServices = 0;
				int mediaServiceSettled = 0;
				int schedulerServices = 0;
				int schedulerServiceSettled = 0;
				for (AuthPreference pref : prefs) {
					// メディアを保持するサービスを対象
					if (ClientManager.hasMedia(pref.service)) {
						mediaServices++;
						
						// アカウントが設定されているサービスであれば
						if (pref.accounts.size() > 0)
							mediaServiceSettled++;
					}

					// スケジューラのサービスを対象
					if (ClientManager.isScheduler(pref.service)) {
						schedulerServices++;
						
						// アカウントが設定されているサービスであれば
						if (pref.accounts.size() > 0) {
							schedulerServiceSettled++;
						}
						
						if ((pref.expired != null && pref.expired)
								&& (mLastReAuthNotified == null || DateUtils.MINUTE_IN_MILLIS * 15 < System.currentTimeMillis() - mLastReAuthNotified)) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(AutoAlbumListActivity.this,
											R.string.online_message_notify_reauth, Toast.LENGTH_LONG).show();
								}
							});
							mLastReAuthNotified = System.currentTimeMillis();
						}
						
					}
					
					
				}
				
				// 全サービスアカウントが設定されていたら設定ボタンを非表示
				mIsShowSearviceAddButton = mediaServices == 0 || mediaServices > mediaServiceSettled;
				mIsShowSelectCalendarButton = schedulerServices == 0 || schedulerServiceSettled == 0;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateButtonVisibility();
					}
				});
				
				return true;
			} catch (IOException e) {
//				e.printStackTrace();		/*$debug$*/
				// エラーの場合は、設定画面にいけないと困るので
				return false;
			}
		}
	}
	
	private static Long mLastReAuthNotified;

	/** ウィンドウマネージャ */
	private WindowManager wm; 
	
	/** ディスプレイ */
	private Display display; 
	
	/** アルバム一覧 */
//	private LinearLayout mLytAlbum;
	private GridView mGridAlbum;

	private RelativeLayout mRytSettingSyncContainer;
	private RelativeLayout mRytSettingDispContainer;
	
	/** ビュー一覧のリスト */
	private List<View> mList = new ArrayList<View>();
	
	/** アダプタ */
	private AlbumAdapter mAdapter;
	
	/** 画像サイズ */
	private int mImageSizeLarge = 0;
	private int mImageSizeSmall = 0;
	
	private UXThumbnailLoader mLoader;

	/** メディアスキャナ */
	private JorlleMediaScanner mMediaScanner;
	
	/** 描画領域　レイアウト */
	private ViewGroup.LayoutParams mParamsLayout;
	private LinearLayout.LayoutParams mParamsLargeImage;
	private LinearLayout.LayoutParams mParamsSmallImage;
	
	/** フォルダ情報リスト */
//	protected final ArrayList<Memory> mEntries = new ArrayList<Memory>();

	/** データ読み込みタスク */
	private AsyncTask<Boolean, Void, CacheInfo> mLoadTask;

	private AsyncTask<List<AuthPreference>, Void, Boolean> mCheckServiceTask;

	private boolean mIsShowSelectCalendarButton = false;
	private boolean mIsShowSearviceAddButton = false;
	
	/** キャッシュ */
	private AutoAlbumCache mAutoAlbumCache;
	private List<AutoAlbumCache.CategoryInfo> mCategoryInfos = new ArrayList<AutoAlbumCache.CategoryInfo>();
	

	/**
	 * この部分を入れ替えたら動作変更
	 * 
	 * @return
	 */
	private AutoAlbumCache createAutoAlbumCache(boolean update)throws Exception{
		JsMediaServerClient jsMedia = ClientManager.getJsMediaServerClient(AutoAlbumListActivity.this);
		
		return new AutoAlbumCacheImpl(this, jsMedia);
		//ExternalServiceCache cache = new ExternalServiceCacheImpl(AutoAlbumListActivity.this);
		//return new MockAutoAlbumCache(this, jsMedia, null/*external*/, cache, true, update);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.autoalbum);
		
		UXViewLoader.addThread(3);
		
		// インデクシング確認判定
		boolean isConfirm = SyncLocalConfirmActivity.needConfirm(this);
		if (isConfirm) {
			Intent intent = new Intent(this, SyncLocalConfirmActivity.class);
			intent.putExtra(SyncLocalConfirmActivity.EXTRA_MESSAGE, getString(R.string.sync_local_confirm_message_autoalbum));
			startActivity(intent);
		}
		
		
		init();
		
		startLoadMemory(false);
		
		startCheckService();
	}
	
	private void init() {
//		mLytAlbum = (LinearLayout) findViewById(R.id.lytAutoAlbum);
		
		Configuration config = getResources().getConfiguration();
		
		int columnNumber;
		
		// 画像の向きに応じて、描画サイズの計算
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			columnNumber = 2;
		} else {
			columnNumber = 1;
		}
		
		mGridAlbum = (GridView) findViewById(R.id.gridAutoAlbum);
		mGridAlbum.setNumColumns(columnNumber);
		mAdapter = new AlbumAdapter();
		mGridAlbum.setAdapter(mAdapter);
		mGridAlbum.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				onClick(view);
			}
		});

		mRytSettingSyncContainer = (RelativeLayout) findViewById(R.id.rytSettingSyncContainer);
		((Button) findViewById(R.id.btnSettingServiceSync)).setOnClickListener(this);
		((Button) findViewById(R.id.btnSettingServiceSyncHide)).setOnClickListener(this);
		mRytSettingDispContainer = (RelativeLayout) findViewById(R.id.rytSettingDispContainer);
		((Button) findViewById(R.id.btnSettingServiceDisp)).setOnClickListener(this);
		((Button) findViewById(R.id.btnSettingServiceDispHide)).setOnClickListener(this);
		
		mLoader = new SearchResultThumbnailLoader(AutoAlbumListActivity.this, true);
		
		setEmptyStatus(false);

		updateButtonVisibility();
	}
	
	/**
	 * アルバム一覧を表示します。
	 */
	private void createAlbumList() {
		disposeImageResources();
		mList.clear();
		
		if (mGridAlbum == null) return;
		
//		if (mEntries == null || mEntries.size() <= 0) {
		if(mCategoryInfos == null || mCategoryInfos.size() <= 0){
			setEmptyStatus(true);
			mAdapter.notifyDataSetChanged();
			return;
		} else {
			setEmptyStatus(false);
		}
		
		// サイズ情報がなければ取得
		if (mParamsLargeImage == null || mParamsSmallImage == null) {
			getImageParams();
		}
		
		//TODO アルバムの数
//		int entrySize = mEntries.size();
		int entrySize = mCategoryInfos.size();
		
        Random rnd = new Random();

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		for (int i = 0; i < entrySize; i++) {
			
			View view;
			
//			Memory memory = mEntries.get(i);
			
			// Memory内のMedia数を取得
//			final Pair<Integer, Integer> count = getMediaCount(memory);
//			final int separateCount = count.first;
//			final int mediaCount = count.second;
			AutoAlbumCache.CategoryInfo info = mCategoryInfos.get(i);
			final int separateCount = info.headerCount;
			final int mediaCount = info.mediaCount;
			
			int ran = 0;
			
			// メディアファイルの数に応じて、選択可能パターンを絞り込む
			if (mediaCount >= 8) {
				ran = rnd.nextInt(4);
			} else if (mediaCount >= 5 && mediaCount < 8) {
				ran = rnd.nextInt(3);
			} else if (mediaCount < 5) {
				ran = LAYOUT_NUMBER_THIRD;
			}
			
			// 1:4
			if (ran == LAYOUT_NUMBER_FIRST) 
			{
				view = inflater.inflate(R.layout.autoalbum_item_first, null);
			} 
			
			// 4:1
			else if (ran == LAYOUT_NUMBER_SECOND) {
				view = inflater.inflate(R.layout.autoalbum_item_second, null);
			} 
			
			// 1:1
			else if (ran == LAYOUT_NUMBER_THIRD) {
				view = inflater.inflate(R.layout.autoalbum_item_third, null);
			} 
			
			// 4:4
			else {
				view = inflater.inflate(R.layout.autoalbum_item_fourth, null);
			}
			
			// 表示パターンをタグに設定
			view.setTag(ran);
			
			// サムネイル表示ビューを取得
			List<UXAsyncImageView> viewList = getImageList(view);
			
			//int viewListsize = viewList.size();
			
			
			// 画像のロード
//			if (memory.dividers != null) {
//				int j = 0;
//				DIVIDER_BREAK:	// 多重のループを抜けるため
//					for (MemoryDivider divider : memory.dividers) {
//						for (Media media : divider.media) {
//							if (viewListsize <= j)
//								break DIVIDER_BREAK;		// 表示枚数を超えたら終わり
//							
//							//TODO ImageViewのサイズ / 2
//							((UXAsyncImageView)viewList.get(j)).loadImage(media, mImageSizeLarge / 2);
//							((UXAsyncImageView)viewList.get(j)).setBorder(true, getResources().getColor(R.color.deepgray), 1.5f);
//
//							j++;
//						}
//					}
//			}
			
			
			// オーバーレイのレイアウト作成
			LinearLayout lytOverlay = (LinearLayout) view.findViewById(R.id.lytOverlay);
			
			ViewGroup.LayoutParams params = lytOverlay.getLayoutParams();
			params.height = mParamsLargeImage.height;
			params.width = mParamsLargeImage.height * 2;
			lytOverlay.setLayoutParams(params);
			
			// タイトル
			TextView txtTitle = (TextView) view.findViewById(R.id.txtTitle);
//			txtTitle.setText(memory.keyword);
			txtTitle.setText(info.categoryName);
			
			// メディア数
//			setMediaCount(view, mediaCount);
			setMediaCount(view, info.mediaCount);

			// 分割数
			TextView txtSeparateCount = (TextView) view.findViewById(R.id.txtSeparateCount);
			txtSeparateCount.setText(String.valueOf(separateCount));
			
			// 選択処理のため、エントリー情報をセット
//			view.setTag(memory);
			ViewTag tag = new ViewTag();
			tag.info = info;
			tag.ran = ran;
			tag.views = viewList.toArray(new UXAsyncImageView[viewList.size()]);
			view.setTag(tag);
//			loadImageToView(view);
			
//			view.setOnClickListener(this);
			
			mList.add(view);
			
			
//			mGridAlbum.addView(view);
		}
		mAdapter.notifyDataSetChanged();
		
	}
	
	private void loadImageToView(View v){
		ViewTag tag = (ViewTag)v.getTag();
		AutoAlbumCache.CategoryInfo info = tag.info;
		UXAsyncImageView[] viewList = tag.views;
		
		for(int n = 0;n < viewList.length && n < info.mediaInfos.size(); ++n){
			UXAsyncImageView imgView = viewList[n];
			imgView.setDisposeImageResourceOnDetached(false);// 手動で開放する
			imgView.loadImage(info.mediaInfos.get(n), mImageSizeLarge / 2);
			imgView.setBorder(true, getResources().getColor(R.color.deepgray), 1.5f);
		}
	}
	
	private void disposeImageResources() {
		List<View> rows = mList;
		if (rows != null) {
			for (View v : rows) {
				ViewTag tag = (ViewTag) v.getTag();
				UXAsyncImageView[] images = tag != null ? tag.views : null;
				if (images != null) {
					for (UXAsyncImageView image : images) {
						try {
							image.disposeImageResources();
						} catch (Exception e) {
							;
						}
					}
				}
			}
		}
	}

//	private View createAlbumCategory(View convertView, int position) {
//		if (mGridAlbum == null) return convertView;
//		
//		if(mCategoryInfos == null || mCategoryInfos.size() <= 0){
//			setEmptyStatus(true);
//			mAdapter.notifyDataSetChanged();
//			return convertView;
//		} else {
//			setEmptyStatus(false);
//		}
//		
//		// サイズ情報がなければ取得
//		if (mParamsLargeImage == null || mParamsSmallImage == null) {
//			getImageParams();
//		}
//		
//		//TODO アルバムの数
//		int entrySize = mCategoryInfos.size();
//		
//        Random rnd = new Random();
//		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		View view = convertView;
//
//		AutoAlbumCache.CategoryInfo info = mCategoryInfos.get(position);
//		final int separateCount = info.headerCount;
//		final int mediaCount = info.mediaCount;
//
//		if(view == null){
//			
//			int ran = 0;
//			
//			// メディアファイルの数に応じて、選択可能パターンを絞り込む
//			if (mediaCount >= 8) {
//				ran = rnd.nextInt(4);
//			} else if (mediaCount >= 5 && mediaCount < 8) {
//				ran = rnd.nextInt(3);
//			} else if (mediaCount < 5) {
//				ran = LAYOUT_NUMBER_THIRD;
//			}
//			
//			// 1:4
//			if (ran == LAYOUT_NUMBER_FIRST) 
//			{
//				view = inflater.inflate(R.layout.autoalbum_item_first, null);
//			} 
//			
//			// 4:1
//			else if (ran == LAYOUT_NUMBER_SECOND) {
//				view = inflater.inflate(R.layout.autoalbum_item_second, null);
//			} 
//			
//			// 1:1
//			else if (ran == LAYOUT_NUMBER_THIRD) {
//				view = inflater.inflate(R.layout.autoalbum_item_third, null);
//			} 
//			
//			// 4:4
//			else {
//				view = inflater.inflate(R.layout.autoalbum_item_fourth, null);
//			}
//			
//			view.setTag(ran);
//			List<UXAsyncImageView> viewList = getImageList(view);
//			
//			ViewTag tag = new ViewTag();
//			tag.info = info;
//			tag.ran = ran;
//			tag.views = viewList.toArray(new UXAsyncImageView[viewList.size()]);
//			
//			view.setTag(tag);
//		}
//		
//		UXAsyncImageView[] viewArray = ((ViewTag)view.getTag()).views;
//		int viewListSize = viewArray.length;
//		
//		for(int n = 0;n < viewListSize && n < info.mediaInfos.size(); ++n){
//			UXAsyncImageView imgView = viewArray[n];
//			imgView.loadImage(info.mediaInfos.get(n), mImageSizeLarge / 2);
//			imgView.setBorder(true, getResources().getColor(R.color.deepgray), 1.5f);
//		}
//		
//		// オーバーレイのレイアウト作成
//		LinearLayout lytOverlay = (LinearLayout) view.findViewById(R.id.lytOverlay);
//		
//		ViewGroup.LayoutParams params = lytOverlay.getLayoutParams();
//		params.height = mParamsLargeImage.height;
//		params.width = mParamsLargeImage.height * 2;
//		lytOverlay.setLayoutParams(params);
//		
//		// タイトル
//		TextView txtTitle = (TextView) view.findViewById(R.id.txtTitle);
//		txtTitle.setText(info.categoryName);
//		
//		// メディア数
//		setMediaCount(view, info.mediaCount);
//
//		// 分割数
//		TextView txtSeparateCount = (TextView) view.findViewById(R.id.txtSeparateCount);
//		txtSeparateCount.setText(String.valueOf(separateCount));
//		
//		return view;
//	}
	
	
	/**
	 * 分割数とメディア数を数えます
	 * @param media
	 * @return
	 */
	private Pair<Integer, Integer> getMediaCount(Memory media) {
		int numSeparate = 0;
		int numMedia = 0;
		if (media.dividers == null)
			return new Pair<Integer, Integer>(0, 0);
		
		for (MemoryDivider divider : media.dividers) {
			numSeparate++;
			numMedia += divider.media.size();
		}
		
		return new Pair<Integer, Integer>(numSeparate, numMedia);
	}

	/**
	 * 画像の表示パターンによって、ビューを設定します
	 * 
	 * @param view	一行のビュー
	 * @return	画像ビューの一覧
	 */
	private List<UXAsyncImageView> getImageList(View view) {
		
		List<UXAsyncImageView> viewList = new ArrayList<UXAsyncImageView>();
		
		// 画像表示ローダ
		UXViewLoader loader = new UXViewLoader(mLoader);
		
		int layoutNumber = (Integer) view.getTag();
		
		UXAsyncImageView imgOne = (UXAsyncImageView) view.findViewById(R.id.imgOne);
		UXAsyncImageView imgTwo = (UXAsyncImageView) view.findViewById(R.id.imgTwo);
		
		imgOne.setViewLoader(loader);
		imgTwo.setViewLoader(loader);
		
		//「１：４」「１：１」の判定
		if (layoutNumber == LAYOUT_NUMBER_FIRST || layoutNumber == LAYOUT_NUMBER_THIRD) {
			imgOne.setLayoutParams(mParamsLargeImage);
		} else {
			imgOne.setLayoutParams(mParamsSmallImage);
		}
		
		// 「１：１」の判定
		if (layoutNumber == LAYOUT_NUMBER_THIRD) {
			imgTwo.setLayoutParams(mParamsLargeImage);
		} else {
			imgTwo.setLayoutParams(mParamsSmallImage);
		}
		
		viewList.add(imgOne);
		viewList.add(imgTwo);
		
		
		// 「１：１」以外は、それ以降のビューを作成
		if (layoutNumber != LAYOUT_NUMBER_THIRD) {
			UXAsyncImageView imgThird = (UXAsyncImageView) view.findViewById(R.id.imgThree);
			UXAsyncImageView imgFour = (UXAsyncImageView) view.findViewById(R.id.imgFour);
			UXAsyncImageView imgFive = (UXAsyncImageView) view.findViewById(R.id.imgFive);
			imgThird.setViewLoader(loader);
			imgFour.setViewLoader(loader);
			imgFive.setViewLoader(loader);
			imgThird.setLayoutParams(mParamsSmallImage);
			imgFour.setLayoutParams(mParamsSmallImage);
			
			//「１：４」の判定
			if (layoutNumber == LAYOUT_NUMBER_SECOND) {
				imgFive.setLayoutParams(mParamsLargeImage);
			} else {
				imgFive.setLayoutParams(mParamsSmallImage);
			}
			
			viewList.add(imgThird);
			viewList.add(imgFour);
			viewList.add(imgFive);
			
			//「４：４」ならば、全ビューを作成
			if (layoutNumber == LAYOUT_NUMBER_FOUTH) {
				UXAsyncImageView imgSix = (UXAsyncImageView) view.findViewById(R.id.imgSix);
				UXAsyncImageView imgSeven = (UXAsyncImageView) view.findViewById(R.id.imgSeven);
				UXAsyncImageView imgEight = (UXAsyncImageView) view.findViewById(R.id.imgEight);
				imgSix.setViewLoader(loader);
				imgSeven.setViewLoader(loader);
				imgEight.setViewLoader(loader);
				imgSix.setLayoutParams(mParamsSmallImage);
				imgSeven.setLayoutParams(mParamsSmallImage);
				imgEight.setLayoutParams(mParamsSmallImage);
				viewList.add(imgSix);
				viewList.add(imgSeven);
				viewList.add(imgEight);
			}
		}
		
		return viewList;
	}
	
	
	@Override
	protected void onDestroy() {
		if (mMediaScanner != null) {
			mMediaScanner.dispose();
		}
		
		if (mUpdateAuthPrefsTask != null) {
			mUpdateAuthPrefsTask.cancel(true);
		}
		
		disposeImageResources();
		UXViewLoader.dispose();
		UXAsyncImageView.dispose();
		super.onDestroy();
	}
	
	
	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.btnSettingServiceDisp) {
			startSettingService();
		}
		else if (view.getId() == R.id.btnSettingServiceDispHide) {
			PreferenceUtil.setBooleanPreferenceValue(this, ApplicationDefine.KEY_SHOW_SERVICE_ADD_BUTTON, false);
			updateButtonVisibility();
			Toast.makeText(AutoAlbumListActivity.this, getString(R.string.online_message_resetting), Toast.LENGTH_LONG).show();
		}
		else if (view.getId() == R.id.btnSettingServiceSync) {
			startSettingServiceCalendar();
		}
		else if (view.getId() == R.id.btnSettingServiceSyncHide) {
			PreferenceUtil.setBooleanPreferenceValue(this, ApplicationDefine.KEY_SHOW_SELECT_CALENDAR_BUTTON, false);
			updateButtonVisibility();
			Toast.makeText(AutoAlbumListActivity.this, getString(R.string.online_message_resetting), Toast.LENGTH_LONG).show();
		}
		else {
			AutoAlbumCache.CategoryInfo info = ((ViewTag)view.getTag()).info;
			if(info == null) return;
			
			try{
				startGridActivityFromCategoryInfo(info);
			}catch(Exception e){
				
			}
			
//			Memory memory = (Memory) view.getTag();
//			
//			if (memory == null) return; 
//			
//			try {
//				startGridActivity(memory);
//			} catch (Exception e) {
//				// TODO 例外表示
////				e.printStackTrace();		/*$debug$*/
//			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ITEM_CALENDAR, 0, getString(R.string.auto_album_menu_calendar)).setIcon(R.drawable.ic_calendar);
		menu.add(0, MENU_ITEM_REFRESH, 0, getString(R.string.auto_album_menu_refresh)).setIcon(R.drawable.ic_refresh);
		menu.add(0, MENU_ITEM_SEARCH, 0, getString(R.string.menu_search)).setIcon(R.drawable.ic_search);
		menu.add(0, MENU_ITEM_SETTING, 0, getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_CALENDAR:
			startSchedulerSetup();
			return true;
		case MENU_ITEM_REFRESH:
			refreshMemories();
			return true;
		case MENU_ITEM_SEARCH:
			startSearchActivity();
			return true;
		case MENU_ITEM_SETTING:
			startSettingActivity();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void startSchedulerSetup() {
		Intent intent = new Intent(this, OnlineSchedulerSetupActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		super.goNextHistory(intent);
	}

	private void startSearchActivity() {
		Intent intent = new Intent(this, SearchActivity.class);
		startActivity(intent);
	}

	/**
	 * 設定画面を表示します
	 */
	private void startSettingActivity() {
		Intent settingIntent = new Intent(this, JorllePrefsActivity.class);
		startActivityForResult(settingIntent, REQUEST_CODE_PREFERENCES);
	}

	/**
	 * 一覧を更新します
	 */
	private void refreshMemories() {
		startLoadMemory(true);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_PREFERENCES: {
			updateButtonVisibility();
			break;
		}
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		// 戻るボタン押下時
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// TODO 複数選択時メニューが出ていたら閉じること
			onBackHistory();
			return true;
		}
		// メニューボタン押下時
		else if (keyCode == KeyEvent.KEYCODE_MENU) {
			openOptionsMenu();
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	
	/**
	 * 画面サイズに応じた描画サイズを計算します
	 *
	 * @param view	描画ビュー
	 */
	public void getImageParams() {

		wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		display = wm.getDefaultDisplay(); 
		
		Configuration config = getResources().getConfiguration();
		SizeConv sc = SizeConv.getInstance(this);
		
		// 画像の向きに応じて、描画サイズの計算
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//			mImageSizeLarge = display.getWidth() / 2 - 40;
//			mImageSizeLarge = display.getWidth() / 4 - 40;
			mImageSizeLarge = (int)((display.getWidth() - sc.getSize(69)) / 4);
		} else {
//			mImageSizeLarge = display.getWidth() / 2 - 30;
			mImageSizeLarge = (int)((display.getWidth() - sc.getSize(40)) / 2);
		}
		
		mImageSizeSmall = (int) Math.floor(mImageSizeLarge / 2);
		
		mParamsLargeImage = new LinearLayout.LayoutParams(mImageSizeLarge, mImageSizeLarge);
		mParamsSmallImage = new LinearLayout.LayoutParams(mImageSizeSmall, mImageSizeSmall);
	}

	/**
	 * グリッド一覧に遷移します
	 * 
	 * @param memory メモリー
	 */
	protected void startGridActivity(Memory memory) {
//		if (memory == null)
//			throw new IllegalArgumentException("argument is null");
//		
//		int position = mEntries.indexOf(memory);
//		if (position < 0)
//			throw new IllegalArgumentException("Position can not be found for argument");
//		
//		startGridActivity(mEntries.indexOf(memory), memory.keyword);
	}
	
	protected void startGridActivityFromCategoryInfo(AutoAlbumCache.CategoryInfo info){
		if (info == null)
			throw new IllegalArgumentException("argument is null");
		
		int position = mCategoryInfos.indexOf(info);
		if (position < 0)
			throw new IllegalArgumentException("Position can not be found for argument");
		
		startGridActivity(mCategoryInfos.indexOf(info), info.categoryName);
	}
	
	protected void startGridActivity(int position, String keyword) {
		Intent intent = new Intent(this, SearchResultGridActivity.class);
		
		intent.putExtra(SearchResultGridActivity.INTENT_RESULT_TYPE, SearchResultGridActivity.RESULT_TYPE_MEMORY);
		intent.putExtra(SearchResultGridActivity.INTENT_MEMORY_POSITION, position);
		intent.putExtra(SearchResultGridActivity.INTENT_MEMORY_KEYWORD, keyword);
		
		// 画像一覧画面のActivityを遷移管理のActivityへ渡す
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP/* | Intent.FLAG_ACTIVITY_SINGLE_TOP*/);
		goNextHistory("SearchResultGridActivity", intent);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		mGridAlbum = null;
//		mEntries.clear();
		mCategoryInfos = null;
//		mList.clear();
		
		mParamsLargeImage = null;
		mParamsSmallImage = null;
		
		init();
		
		startLoadMemory(false);
		
		View view = getWindow().getDecorView();
		changeActivity(view);
	}
	
	private void startLoadMemory(boolean refresh) {
		cancelLoadMemory();
//		mLoadTask = new MemoryTask().execute(refresh);
		mLoadTask = new AutoAlbumCacheTask().execute(refresh);
	}
	
	private void cancelLoadMemory() {
		if (mLoadTask != null && mLoadTask.getStatus() == Status.RUNNING) {
			mLoadTask.cancel(false);
			mLoadTask = null;
		}
	}
	
	public class AlbumAdapter extends BaseAdapter implements RecyclerListener{  
		@Override
		public int getCount() {
			if(mCategoryInfos == null)return 0;
			return mCategoryInfos.size();
		}

		@Override
		public Object getItem(int position) {
			if(mCategoryInfos == null)return null;
			return mCategoryInfos.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override  
		public View getView(int position, View convertView, ViewGroup parent) {  
			View view =  mList.get(position);
			loadImageToView(view);
			return view;
		}

		@Override
		public void onMovedToScrapHeap(View view) {
//			for(int imgId: new int[]{R.id.imgOne,R.id.imgTwo, R.id.imgThree, R.id.imgFour, R.id.imgFive, R.id.imgSix, R.id.imgSeven, R.id.imgEight}){
//				((UXAsyncImageView)view.findViewById(imgId)).recycle();
//			}
			for(UXAsyncImageView imgView: ((ViewTag)view.getTag()).views){
				imgView.recycle();
			}
		}  
	}

	/**
	 * 設定済みのサービスをチェックします
	 */
	private void startCheckService() {
		cancelCheckService();
		mCheckServiceTask = new CheckServiceTask().execute();
	}
	
	/**
	 * 実行中のサービスのチェックをキャンセルします
	 */
	private void cancelCheckService() {
		if (mCheckServiceTask != null && mCheckServiceTask.getStatus() == Status.RUNNING) {
			mCheckServiceTask.cancel(false);
			mCheckServiceTask = null;
		}
	}

	/**
	 * サービスの設定画面を表示します
	 */
	private void startSettingService() {
		Intent intent = new Intent(AutoAlbumListActivity.this, OnlineSetupActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(OnlineSetupActivity.EXTRA_BACK_MODE, OnlineSetupActivity.BACK_MODE_AUTO_ALBUM);
		goNextHistory(intent);
	}

	/**
	 * サービスの設定画面のボタンを非表示にします
	 */
	private void setSettingServiceDispButtonVisibility(boolean isVisible) {
		mRytSettingDispContainer.setVisibility(isVisible ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * カレンダー（スケジューラ）の設定画面を表示します
	 */
	private void startSettingServiceCalendar() {
		Intent intent = new Intent(AutoAlbumListActivity.this, OnlineSchedulerSetupActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(OnlineSchedulerSetupActivity.EXTRA_BACK_MODE, OnlineSchedulerSetupActivity.BACK_MODE_POPSTACK);
		goNextHistory(intent);
	}

	/**
	 * カレンダー（スケジューラ）のボタンを非表示にします
	 */
	private void setSettingServiceSyncButtonVisibility(boolean isVisible) {
		mRytSettingSyncContainer.setVisibility(isVisible ? View.VISIBLE : View.GONE);
	}

	/**
	 * ボタンの表示状態を切り替えます
	 */
	private void updateButtonVisibility() {
		boolean b;
		
		b = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_SHOW_SELECT_CALENDAR_BUTTON, true);
		setSettingServiceSyncButtonVisibility(b && mIsShowSelectCalendarButton);
		
		b = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_SHOW_SERVICE_ADD_BUTTON, true);
		setSettingServiceDispButtonVisibility(b && mIsShowSearviceAddButton);
	}
	
	
	private UpdateAuthPrefsTask mUpdateAuthPrefsTask;
	private class UpdateAuthPrefsTask extends AsyncTask<Void, Void, List<AuthPreference>> {
		@Override
		protected void onPreExecute() {
			mUpdateAuthPrefsTask = this;
		}
		
		@Override
		protected List<AuthPreference> doInBackground(Void... params) {
			JsMediaServerClient client =
					ClientManager.getJsMediaServerClient(AutoAlbumListActivity.this);
			List<AuthPreference> prefs;
			try {
				prefs = client.getAuthPreferences(true);
			} catch (IOException e) {
//				e.printStackTrace();		/*$debug$*/
				return null;
			}
			
			return prefs;
		}
		
		@Override
		protected void onPostExecute(List<AuthPreference> result) {
			try {
				if (result != null) {
					new CheckServiceTask().execute(result);
				}
				
			} finally {
				mUpdateAuthPrefsTask = null;
			}
		}
		
		@Override
		protected void onCancelled() {
			mUpdateAuthPrefsTask = null;
		}
	}

	private void setEmptyStatus(boolean isEmpty) {
		View empty = findViewById(R.id.empty);
		empty.setVisibility(View.GONE);
		if (isEmpty) {
			mGridAlbum.setEmptyView(empty);
		} else {
			mGridAlbum.setEmptyView(null);
		}
	}

	/**
	 * メディア数を設定します
	 * @param view
	 * @param mediaCount
	 */
	private void setMediaCount(View view, final int mediaCount) {
		ImageView imgMediaCount = (ImageView) view.findViewById(R.id.imgMediaCount);
		TextView txtMediaCount = (TextView) view.findViewById(R.id.txtMediaCount);
		if (mediaCount < 1000) {
			imgMediaCount.setImageResource(R.drawable.file_black);
			txtMediaCount.setText(String.valueOf(mediaCount));
			txtMediaCount.setVisibility(View.VISIBLE);
		} else {
			imgMediaCount.setImageResource(R.drawable.file_over_black);
			txtMediaCount.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * 処理中のプログレスを開始します
	 */
	private void startProgress() {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
	}
	
	/**
	 * 処理中のプログレスを終了します
	 */
	private void stopProgress() {
		findViewById(R.id.progress).setVisibility(View.GONE);
	}
}