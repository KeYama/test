package jp.co.johospace.jsphoto;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.fullscreen.ImageSurfaceView;
import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoader;
import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoaderFactory;
import jp.co.johospace.jsphoto.util.DateUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.AutoAlbumCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.AutoAlbumCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Memory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.MemoryDivider;
import jp.co.johospace.jsphoto.v2.onlineservice.ui.SearchResultImageLoader;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 検索結果フルスクリーン
 */
public class SearchResultFullScreenActivity extends AbstractFullscreenActivity {
	
	public static final String INTENT_KEYWORD = "SearchResultFullScreenActivity.Keyword";
	
	private AutoAlbumCache createAutoAlbumCache()throws Exception{
		JsMediaServerClient jsMedia = ClientManager.getJsMediaServerClient(this);
		
		return new AutoAlbumCacheImpl(this, jsMedia);
	}

	private final class SearchLoadTask extends
			AsyncTask<String, Void, List<Media>>{
		@Override
		protected List<Media> doInBackground(String... params) {
			try {
				if (params.length < 1)
					throw new IllegalArgumentException("required position of search result");
				
				// 初回表示の位置を保存
				mPosition = Integer.parseInt(params[0]);
				
				return mClient.searchMediaByLastCashed();
			} catch (IOException e) {
				handleException(e, true);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<Media> result) {
			onDataLoaded(result, null);
		}
	}
	
	private final class MemoryLoadTask extends
			AsyncTask<String, Void, Pair<List<Media>, List<MemoryDivider>>> {

		@Override
		protected Pair<List<Media>, List<MemoryDivider>> doInBackground(String... params) {
			try {
				if (params.length < 0)
					throw new IllegalArgumentException("require argument");
				
				final String arg = params[0];
				final String[] parts = arg.split("/");
//				final int memoryIndex = Integer.parseInt(parts[0]);
				final int memoryIndex = 0;//一つだけ
				final String memoryKeyword = mMemoryKeyword;
				final int dividerIndex = Integer.parseInt(parts[1]);
				final int mediaIndex = Integer.parseInt(parts[2]);
				
				List<Memory> memories = new ArrayList<Memory>();
//				IOIterator<Memory> itr = mClient.searchMemories();
//				try {
//					memories.add(itr.next());
//				} finally {
//					itr.terminate();
//				}
				memories.add(createAutoAlbumCache().getMemory(memoryKeyword));
				
				List<Media> result = new ArrayList<Media>();
				List<MemoryDivider> dividerMap = new ArrayList<MemoryDivider>();
				for (int i=0; i<memories.size(); i++) {
					List<MemoryDivider> dividers = memories.get(i).dividers;
					if (dividers == null) continue;
					// 指定Memory内でスワイプ
					else if (i != memoryIndex) continue;
					
					for (int j=0; j<dividers.size(); j++) {
						List<Media> medias = dividers.get(j).media;
						if (medias == null) continue;
						
						// MemoryDividerはまたいでスワイプ
						for (int k=0; k<medias.size(); k++) {
							// 要素を追加する前にサイズを得ること
							if (i == memoryIndex && j == dividerIndex && k == mediaIndex)
								mPosition = result.size();
							
							result.add(medias.get(k));
							dividerMap.add(dividers.get(j));
						}
					}
					break;
				}
				return new Pair<List<Media>, List<MemoryDivider>>(result, dividerMap);
			} catch (Exception e) {
				handleException(e, true);
				return null;
			}
		}

		@Override
		protected void onPostExecute(Pair<List<Media>, List<MemoryDivider>> result) {
			onDataLoaded(result.first, result.second);
		}
	}

	public static final String EXTRA_TYPE = 
	SearchResultFullScreenActivity.class.getName() + ".EXTRA_TYPE";

	public static final String EXTRA_DATA =
			SearchResultFullScreenActivity.class.getName() + ".EXTRA_DATA";

	public static final int TYPE_MEMORY = 1;
	public static final int TYPE_SEARCH = 2;
	
	private List<Media> mMedias;
	private List<MemoryDivider> mDividers;
	private int mPosition;
	
	
	private JsMediaServerClient mJsMedia;
	private CachingAccessor mClient;
	private ExternalServiceCache mCache;
	private AsyncTask<String, Void, ?> mLoadTask;
	private String mMemoryKeyword;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.fullscreen);
		findViewById(R.id.lytListHeader).setVisibility(View.GONE);
		
		mJsMedia = ClientManager.getJsMediaServerClient(this);
		mCache = new ExternalServiceCacheImpl(this);
		mClient = new CachingAccessor(this, mJsMedia, null, mCache, true);
		
		mMemoryKeyword = getIntent().getStringExtra(INTENT_KEYWORD);

		prepareInfoLinkOnCreate();
		
		try {
			startLoad();
		} catch (Exception e) {
			// TODO エラー表示
//			e.printStackTrace();		/*$debug$*/
			onBackKey();
		}
	}

	protected void recreateSurfaceView(int pos) {
		FrameLayout frame = (FrameLayout)findViewById(R.id.flFullscreen);
		frame.removeAllViews();
		if(mSurfaceView != null)mSurfaceView.dispose();
		
		mPosition = pos;
		
		frame.addView(
			mSurfaceView = new ImageSurfaceView(this, new ImageLoaderFactory() {
				private ImageLoader mLoader = new SearchResultImageLoader(SearchResultFullScreenActivity.this);
				@Override
				public ImageLoader create() {
					return mLoader;
				}
			}, mMedias, mPosition)
		);
		
		startInfoLinkNavigation();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBackKey();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void startLoad() {
		cancelLoad();
		
		Intent intent = getIntent();
		final int type = intent.getIntExtra(EXTRA_TYPE, Integer.MIN_VALUE);
		final String data = intent.getStringExtra(EXTRA_DATA);
		
		// タスクの実装を切り替える
		switch (type) {
		case TYPE_MEMORY:
			mLoadTask = new MemoryLoadTask().execute(data);
			break;
		case TYPE_SEARCH:
			mLoadTask = new SearchLoadTask().execute(data);
			break;
		default:
			
		}
	}

	private void cancelLoad() {
		if (mLoadTask != null && mLoadTask.getStatus() == Status.RUNNING) {
			mLoadTask.cancel(false);
		}
	}
	private void onDataLoaded(List<Media> result, List<MemoryDivider> dividerMap) {
		if (result != null) {
			mMedias = result;
			mDividers = dividerMap;
			recreateSurfaceView(mPosition);
		} else {
			// TODO $ol
			finish();
		}
	}
	
	@Override
	protected MediaIdentifier getCurrentMedia() {
		Media media = (Media) mSurfaceView.getCurrentTag();
		return new MediaIdentifier(media.service, media.account, media.mediaId);
	}
	
	@Override
	protected Long getCurrentDate() {
		Media media = (Media) mSurfaceView.getCurrentTag();
		return media.productionDate;
	}
	
	@Override
	protected String getCategoryName() {
		if (getIntent().getIntExtra(EXTRA_TYPE, Integer.MIN_VALUE) == TYPE_MEMORY) {
			if (mDividers == null) {
				return "";
			} else {
				MemoryDivider divider = mDividers.get(mSurfaceView.getCurrentNumber());
				if (divider.isEvent) {
					return divider.title;
				} else {
					try {
						Time time = new Time();
						
						DateUtil.parse(divider.date, "yyyyMM", time);
						time.monthDay = 1;
						time.hour = time.minute = time.second = 0;
						
						time.normalize(false);
						return time.format(getString(R.string.search_result_grid_format_ym));
					} catch (ParseException e) {
						return " ";
					}
				}
			}
		} else {
			return super.getCategoryName();
		}
	}
	
	@Override
	protected void onDestroy() {
		if(mSurfaceView != null)mSurfaceView.dispose();
		mSurfaceView = null;
		mHandler.removeCallbacksAndMessages(null);
		super.onDestroy();
	}
}
