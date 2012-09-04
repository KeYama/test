package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.util.DateUtil;
import jp.co.johospace.jsphoto.util.HeaderController;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.ux.widget.UXGridDataSource;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget.ItemLongPressListener;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget.ItemTapListener;
import jp.co.johospace.jsphoto.ux.widget.UXHeaderWidget;
import jp.co.johospace.jsphoto.ux.widget.UXStackContainer;
import jp.co.johospace.jsphoto.ux.widget.UXStage;
import jp.co.johospace.jsphoto.ux.widget.UXUnit;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.AutoAlbumCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.AutoAlbumCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ContentsNotModifiedException;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.MemoryDivider;
import jp.co.johospace.jsphoto.v2.onlineservice.ui.SearchResultThumbnailLoader;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 検索結果OpenGL版グリッドアクティビティ。
 * 
 * Intentについて。
 * INTENT_FOLDER_TYPE: FOLDER_TYPE_***のいずれかを指定
 * INTENT_TARGET/INTETN_TARGET_LIST: FOLDER_TYPEに依存。詳細は該当変数のソースにて。
 * 
 */
public class SearchResultGridActivity extends NavigatableActivity implements OnClickListener {
	/**
	 * フォルダタイプ。int。FOLDER_TYPE_***を指定
	 **/
	public static final String INTENT_RESULT_TYPE = "ResultType";
	/**
	 * アルバムの要素を指定します
	 */
	public static final String INTENT_MEMORY_POSITION = "MemoryPosition";
	
	/** アルバムの名称（キーワード）を指定します */
	public static final String INTENT_MEMORY_KEYWORD = "MemoryKeyword";
	/**
	 * オートアルバムタイプ
	 */
	public static final int RESULT_TYPE_MEMORY = 1;
	
	private static final String TAG = "SearchResultGridActivity";
	private static final int WIDTH = 200;

	private static final int MENU_ITEM_SETTING = 3;
	
	private AutoAlbumCache createAutoAlbumCache()throws Exception{
		JsMediaServerClient jsMedia = ClientManager.getJsMediaServerClient(this);
		
		return new AutoAlbumCacheImpl(this, jsMedia);
	}

	/** 列数 */
	private static final int 
		COLUMN_NORMAL_PORTLAIT = 3,
		COLUMN_NORMAL_LANDSCAPE = 5;

	private class MemoryTask extends AsyncTask<String, Integer, List<MemoryDivider>> {
		private boolean mIsRefresh;

		public MemoryTask(boolean isRefresh) {
			mIsRefresh = isRefresh;
		}
		
		@Override
		protected void onPreExecute() {
			mBtnRefresh.setBackgroundResource(R.drawable.cancel);

			mHeaderController.setDisplayKeep(true);
			mHeaderController.show();
		}
		
		@Override
		protected List<MemoryDivider> doInBackground(String... params) {
			AutoAlbumCache cache = null;
			try {
				cache = createAutoAlbumCache();
				
				
				if (mIsRefresh) {
					//mAccessor.updateMemoriesCache();
					cache.updateCache();
				}
			} catch (ContentsNotModifiedException e) {
				Log.d(TAG, "received NOT MODIFIED");/*$debug$*/
				return null;
			} catch (Exception e) {
				handleException(e, mIsRefresh);
			}
			
			try {
				return cache.getMemory(params[0]).dividers;
				
				
//				// アルバムをキャッシュから取得
//				List<Memory> memories = new ArrayList<Memory>();
//				IOIterator<Memory> itr = mAccessor.searchMemories();
//				try {
//					while (itr.hasNext()) {
//						memories.add(itr.next());
//					}
//				} finally {
//					itr.terminate();
//				}
//				
//				// 前画面で指定されたMemoryを取得
//				String keywd = params[0];
//				Memory mem = null;
//				for (Memory m : memories) {
//					if (m.keyword.equals(keywd)) {
//						mem = m;
//						break;
//					}
//				}
//				// 更新されて表示中のカテゴリがなくなったかどうか
//				if (mem == null) {
//					return new ArrayList<MemoryDivider>();
//				}
//				
//				// Dividerがなければゼロ件となる
//				if (mem.dividers == null || mem.dividers.size() == 0) {
//					return new ArrayList<MemoryDivider>();
//				}
//				
//				// 集めたメディアを返却
//				return new ArrayList<MemoryDivider>(mem.dividers);
			} catch (Exception e) {
				handleException(e, mIsRefresh);
				return new ArrayList<MemoryDivider>();
			}
		}
		
		@Override
		protected void onPostExecute(final List<MemoryDivider> result) {
			getColumnCount();
			
			if (result != null) {
				//Stageに変更を加える場合、必ずlockStageを経由
				//
				//Stageに変更とは、Stageにぶら下がっているすべてのクラスの変更のこと。
				//たとえばDataSourceの中身に変更を加える場合でも必要となる。
				//
				mStage.lockStage(new Runnable() {
					@Override
					public void run() {
						mItems.clear();
						mItems.addAll(result);
						
						mStage.clear();
						UXStackContainer stack = new UXStackContainer();
						stack.addTo(mStage);
						
						for (MemoryDivider divider : mItems) {
							stackUxWidgets(divider, stack);
						}
					}
				});
				
				// 変更を再描画してもらう
				mStage.invalidate();
			}
			
			// ヘッダの操作
			mHeaderController.setDisplayKeep(false);
			mHeaderController.hide(HeaderController.LENGTH_DEFAULT);
			mBtnRefresh.setBackgroundResource(R.drawable.button_refresh);

			if (result != null && result.size() == 0) {
				Toast.makeText(SearchResultGridActivity.this, R.string.image_message_no_media, Toast.LENGTH_LONG).show();
				onBackRefleshHistory();
				return;
			}
		}
		
		@Override
		protected void onCancelled() {
			// ヘッダの操作
			mHeaderController.setDisplayKeep(false);
			mHeaderController.hide(HeaderController.LENGTH_DEFAULT);
			mBtnRefresh.setBackgroundResource(R.drawable.button_refresh);
		}
	}
	
	private class GridSource implements UXGridDataSource, ItemTapListener, ItemLongPressListener {
		private MemoryDivider mDivider;
		
		public GridSource(MemoryDivider divider) {
			mDivider = divider;
		}
		
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
			if (mDivider.media == null)
				return 0;
			else
				return mDivider.media.size();
		}
		
		@Override
		public Object getInfo(int item) {
			if (mDivider.media == null)
				return null;
			else
				return mDivider.media.get(item);
		}

		@Override
		public void onTap(int itemNumber) {
			final int memoryIndex = getIntent().getIntExtra(INTENT_MEMORY_POSITION, -1);
			final int dividerIndex = mItems.indexOf(mDivider);
			
			final String data = memoryIndex + "/" + dividerIndex + "/" + itemNumber;
			
			
			final Media media = mDivider.media.get(itemNumber);
			new AsyncTask<Void, Void, Intent>() {
				@Override
				protected Intent doInBackground(Void... params) {
					String mime = MediaUtil.getMimeTypeFromPath(media.fileName);
					if ((mime == null || mime.startsWith("video/"))
							&& !ServiceType.JORLLE_LOCAL.equals(media.service)) {
						try {
							ExternalServiceClient client =
									ClientManager.getExternalServiceClient(SearchResultGridActivity.this, media.service);
							CachingAccessor accessor =
									new CachingAccessor(SearchResultGridActivity.this, mJsMedia, client, mCache, false);
							mime = accessor.getMediaContentType(media);
						} catch (IOException e) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(SearchResultGridActivity.this,
											R.string.error_failed_to_connect, Toast.LENGTH_LONG).show();
								}
							});
							return null;
						}
					}
					
					Intent intent;
					if (mime != null && mime.startsWith("video/")) {
						Uri uri;
						if (ServiceType.JORLLE_LOCAL.equals(media.service)) {
							uri = Uri.fromFile(new File(media.mediaId));
						} else {
							ExternalServiceClient client =
									ClientManager.getExternalServiceClient(SearchResultGridActivity.this, media.service);
							Pair<String, String> contents = client.getContentsUrl(media, mime);
							uri = Uri.parse(contents.first);
							mime = contents.second;
						}
						
						intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(uri, mime);
					} else {
						intent = new Intent(SearchResultGridActivity.this, SearchResultFullScreenActivity.class);
						intent.putExtra(SearchResultFullScreenActivity.EXTRA_TYPE, SearchResultFullScreenActivity.TYPE_MEMORY);
						intent.putExtra(SearchResultFullScreenActivity.EXTRA_DATA, data);
						intent.putExtra(SearchResultFullScreenActivity.INTENT_KEYWORD, mKeyword);
					}
					
					return intent;
				}
				
				@Override
				protected void onPostExecute(Intent result) {
					if (result != null) {
						startActivity(result);
					}
				}
			}.execute();
			
		}
		
		@Override
		public void onLongPress(int itemNumber) {
//			Toast.makeText(SearchResultGridActivity.this, "画像が長押しされました", Toast.LENGTH_SHORT).show();
		}
	}
	
	private int mColumnCount;
	private int mResultType;

	private UXStage mStage;
	private UXGridWidget mGrid;

	private JsMediaServerClient mJsMedia;;
//	private ExternalServiceClient mExternal;
	private ExternalServiceCache mCache;
	private CachingAccessor mAccessor;
	private AsyncTask<String, Integer, List<MemoryDivider>> mLoadTask;
	
	/** 検索結果メディアコレクション */
	private ArrayList<MemoryDivider> mItems;

	private HeaderController mHeaderController;
	private Button mBtnRefresh;
	private String mKeyword;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		init();
		
		mResultType = getIntent().getIntExtra(INTENT_RESULT_TYPE, -1);
		mKeyword = getIntent().getStringExtra(INTENT_MEMORY_KEYWORD);
		
		startLoad(false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		mStage.onResume();
		
		mHeaderController.show(3000);
	}
	
	@Override
	protected void onPause() {
		cancelLoad();
		
		mStage.onPause();
		
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		mHeaderController.onDestroy();
		
		mStage.dispose();
		
		super.onDestroy();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		getColumnCount();
		
		mStage.lockStage(new Runnable() {
			@Override
			public void run() {
				mGrid.column(mColumnCount);
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ITEM_SETTING, 0, getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_SETTING:
			startSettingActivity();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnRefresh) {
			startLoad(true);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		// 戻るボタン押下時
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// TODO 複数選択時メニューが出ていたら閉じること
			onBackRefleshHistory();
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
	 * 初期か処理を行います
	 */
	private void init() {
		mJsMedia = ClientManager.getJsMediaServerClient(this);
//		external = ClientManager.getExternalServiceClient(this, "");
		mCache = new ExternalServiceCacheImpl(this);
		mAccessor = new CachingAccessor(this, mJsMedia, null/*external*/, mCache, true);

		mItems = new ArrayList<MemoryDivider>();
		
		//ここからがUXStageの使用
		mStage = new UXStage(this, UXStage.GL_RENDERER);
		mStage.addThread(ApplicationDefine.NUM_NETWORK_THREAD);
		
		mStage.setBackgroundColor(Color.rgb(0xf3, 0xf3, 0xf3));
		mStage.setScrollbarResource(R.drawable.slider, 28, 65);
		getColumnCount();
		
		//最後にViewを追加
		setContentView(mStage.getView());
		
		mHeaderController = new HeaderController(this,
				R.layout.ol_search_result_header,
				(ViewGroup) getWindow().getDecorView());
		mHeaderController.setHeaderEventTo(mStage);
		View v = mHeaderController.getView();
		mBtnRefresh = (Button) v.findViewById(R.id.btnRefresh);
		mBtnRefresh.setOnClickListener(this);

		// キーワードを設定
		TextView txt = (TextView) v.findViewById(R.id.txt_name);
		txt.setText(getIntent().getStringExtra(INTENT_MEMORY_KEYWORD));
	}

	/**
	 * UxWidgetをスタックに積みます
	 * <p>
	 * ※オートアルバム用
	 * </p>
	 * @param divider
	 * @param stack
	 */
	private void stackUxWidgets(MemoryDivider divider,
			UXStackContainer stack) {
		
		String text, rightText;
		int size = 16;
		int iconSize = 20;
		
		final Drawable icon;
		if (divider.isEvent) {
			icon = getResources().getDrawable(R.drawable.ic_autoalbum_google);
			
			Time time = new Time();
			String dayText;
			try {
				DateUtil.parse(divider.date, "yyyyMMdd", time);
				time.hour = 0;
				time.minute = 0;
				time.second = 0;
				time.normalize(false);
				dayText = time.format(getString(R.string.search_result_grid_format_ymd));
			} catch (ParseException e) {
//				e.printStackTrace();		/*$debug$*/
				dayText = "";
			}
			
			text = String.format("%s　%s",	dayText, divider.title);
		} else {
			icon = getResources().getDrawable(R.drawable.ic_autoalbum_header);
			
			Time time = new Time();
			String dayText;
			try {
				DateUtil.parse(divider.date, "yyyyMM", time);
				time.monthDay = 1;
				time.hour = time.minute = time.second = 0;
				time.normalize(false);
				dayText = time.format(getString(R.string.search_result_grid_format_ym));
			} catch (ParseException e) {
//				e.printStackTrace();		/*$debug$*/
				dayText = "";
			}
			text = String.format("%s",		dayText, null);
		}
		rightText = getResources().getQuantityString(R.plurals.search_result_grid_medias, divider.media.size(), divider.media.size());
		
		UXHeaderWidget header = new UXHeaderWidget(text, rightText, size, UXUnit.DP);
		header.icon(icon,
				iconSize, UXUnit.DP,
				iconSize, UXUnit.DP);
		
		header.margin(10, UXUnit.DP).textColor(Color.BLACK).addTo(stack);
		header.backgroundColor(Color.rgb(0xe3, 0xe3, 0xe3), true);
		
		GridSource s = new GridSource(divider);
		
		mGrid = new UXGridWidget(WIDTH, new SearchResultThumbnailLoader(SearchResultGridActivity.this));
		mGrid
			.dataSource(s)
			.padding(5, UXUnit.DP)
			.itemType(new UXGridWidget.ThumbnailGrid())
			.column(mColumnCount)
			.addTo(stack);
		
		mGrid.setOnItemTapListener(s);
		mGrid.setOnItemLongPressListener(s);
	}

	/**
	 * データの読み込みを開始します
	 */
	private void startLoad(boolean isRefresh) {
		if (isDataLoading())
			return;
		
		switch (mResultType) {
		case RESULT_TYPE_MEMORY:
//			int position = getIntent().getIntExtra(INTENT_MEMORY_POSITION, -1);
//			if (position >= 0) {
//				mLoadTask = new MemoryTask().execute(position);
//			}
			String keywd = getIntent().getStringExtra(INTENT_MEMORY_KEYWORD);
			if (!TextUtils.isEmpty(keywd)) {
				mLoadTask = new MemoryTask(isRefresh).execute(keywd);
			}
			break;
		default:
//			Log.d(TAG, "Uknown result type: " + mResultType);		/*$debug$*/
		}
	}
	
	/**
	 * データの読み込みをキャンセルします
	 */
	private void cancelLoad() {
		if (isDataLoading()) {
			mLoadTask.cancel(false);
			mLoadTask = null;
		}
	}
	
	/**
	 * データを読み込み中かどうかを取得します
	 * @return
	 */
	private boolean isDataLoading() {
		return mLoadTask != null && mLoadTask.getStatus() == Status.RUNNING;
	}

	/**
	 * 表示する列数を取得します
	 */
	private void getColumnCount() {
		
		Configuration config = getResources().getConfiguration();
		
		// 遷移元と画面向きによって、列数を変える
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mColumnCount = COLUMN_NORMAL_LANDSCAPE;
		} else {
			mColumnCount = COLUMN_NORMAL_PORTLAIT;
		}
	}

	/**
	 * 設定画面を表示します
	 */
	private void startSettingActivity() {
		Intent settingIntent = new Intent(this, JorllePrefsActivity.class);
		startActivity(settingIntent);
	}

}
