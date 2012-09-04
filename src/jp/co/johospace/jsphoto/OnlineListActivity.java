package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.util.SizeConv;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import jp.co.johospace.jsphoto.ux.view.UXViewLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.AuthPreference;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * オンライン一覧アクティビティです
 */
public class OnlineListActivity extends NavigatableActivity implements View.OnClickListener {

	public static boolean autoUpdated;
	
	public static final String EXTRA_FORCE_REQUEST =
			OnlineListActivity.class.getName() + ".EXTRA_FORCE_REQUEST";
	private static final String SYNC_FOLDA = "jsphoto";
	
	private final class LoadDataTask extends
			AsyncTask<Boolean, Void, List<ListItem>> {
		private final List<AuthPreference> mPreLoaded;
		LoadDataTask(List<AuthPreference> preLoaded) {
			super();
			mPreLoaded = preLoaded;
		}
		
		@Override
		protected void onPreExecute() {
			if (mPreLoaded == null) {
				startProgress();
			}
		}
		
		private List<AuthPreference> mCachedPrefs;
		
		@Override
		protected List<ListItem> doInBackground(Boolean... params) {
			try {
//StopWatch sw = StopWatch.start();/*$debug$*/
				ArrayList<ListItem> items = new ArrayList<ListItem>();
				
				// 双方向同期分
				mIsShowSyncButton = true;
				Map<String, Map<String, SyncSetting>> settings =
						MediaSyncManagerV2.loadSyncSettings(getApplicationContext());
				for (String service : settings.keySet()) {
					Map<String, SyncSetting> accounts = settings.get(service);
					for (String account : accounts.keySet()) {
						SyncSetting setting = accounts.get(account);
//						SyncFolderItem item = new SyncFolderItem();
//						item.loader = new LocalCachedThumbnailLoader();
//						item.service = setting.service;
//						item.account = setting.account;
////						item.icon = ClientManager.getIconResource(OnlineListActivity.this, setting.service);
////						item.name = ClientManager.getServiceName(OnlineListActivity.this, setting.service);
//						item.icon = R.drawable.sync_folder;
//						item.name = new File(setting.localDir).getName();
//						item.lastUpdated = setting.lastUpdated;
//						item.localDir = setting.localDir;
//						item.files = list2WaySyncs(setting.localDir);
//						item.needReAuth = false;
//						items.add(item);
						mIsShowSyncButton = false;
						new File(setting.localDir).mkdirs();
					}
				}
//System.out.println(String.format("■双方向同期生成： %d msec", sw.lap()));/*$debug$*/
				
				// 片方向同期分
				boolean forceRequest = getIntent().getBooleanExtra(EXTRA_FORCE_REQUEST, false);
				List<AuthPreference> prefs;
				if (mPreLoaded == null) {
					try {
						prefs = mJsMedia.getAuthPreferences(forceRequest);
						if (!forceRequest && prefs == null) {
							prefs = mJsMedia.getAuthPreferences(true);
						} else if (!forceRequest && prefs != null) {
							mCachedPrefs = prefs;
						}
						
					} catch (IOException e) {
						prefs = mJsMedia.getAuthPreferences(false);
						if (prefs == null) {
							e.printStackTrace();		/*$debug$*/
							return null;
						}
					}
					
				} else {
					prefs = mPreLoaded;
				}
//System.out.println(String.format("■AuthPreference取得： %d msec", sw.lap()));/*$debug$*/
				
				int numServices = 0;
				int numSettledServices = 0;
				for (AuthPreference pref : prefs) {
					if (ClientManager.hasMedia(pref.service)) {
						numServices++;
						if (pref.accounts.size() > 0) {
							numSettledServices++;
						}
						
						for (String account : pref.accounts) {
							ServiceItem item;
							CachingAccessor accessor = getAccessor(pref.service);

							// キャッシュを更新する
							if (params.length > 0 && params[0]) {
								try {
									accessor.requestIndexing(pref.service, account, true);
								} catch (IOException e) {
//									e.printStackTrace();		/*$debug$*/
								}
								accessor.updateDirectoryCache(pref.service, account);
							}
							if (ClientManager.hasDirectory(pref.service)) {
								item = new HasFolderItem();
//long st = SystemClock.elapsedRealtime();/*$debug$*/
								List<Directory> dirs = accessor.getDirectories(pref.service, account, false, 10, false);
//System.out.println(String.format("accessor.getDirectories %d msec", SystemClock.elapsedRealtime() - st));/*$debug$*/
								((HasFolderItem) item).directories.addAll(dirs);
							} else {
								item = new NoFolderItem();
								List<Directory> dirs =
										accessor.getDirectories(pref.service, account, true, 10, false);
								if (dirs.isEmpty()) {
									continue;
								}
//								if (!dirs.isEmpty()) {
									((NoFolderItem) item).medias.addAll(dirs.get(0).media);
									((NoFolderItem) item).mediaCount = dirs.get(0).mediaCount;
									((NoFolderItem) item).dirId = dirs.get(0).id;
//								}
							}
							item.service = pref.service;
							item.account = account;
							item.accessor = accessor;
							item.loader = accessor;
							item.name = ClientManager.getServiceName(
									getApplicationContext(), pref.service);
							item.icon = ClientManager.getIconResource(getApplicationContext(), pref.service);
							item.lastUpdated = accessor.getCache().getLastUpdated(pref.service, account);
							item.needReAuth = pref.expired != null && pref.expired;
							items.add(item);
						}
					}
//System.out.println(String.format("■%s生成： %d msec", ClientManager.getServiceName(getApplicationContext(), pref.service), sw.lap()));/*$debug$*/
				}
				
				mIsShowSearviceAddButton = numServices - numSettledServices > 0;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateButtonVisibility();
					}
				});
				
				Collections.sort(items, new Comparator<ListItem>() {
					@Override
					public int compare(ListItem o1,
							ListItem o2) {
						if (o1 instanceof SyncFolderItem == o2 instanceof SyncFolderItem) {
							int order1 = ClientManager.getDisplayOrder(o1.service);
							int order2 = ClientManager.getDisplayOrder(o2.service);
							int compare = Integer.valueOf(order1).compareTo(Integer.valueOf(order2));
							if (compare == 0) {
								return o1.account.compareTo(o2.account);
							}
							return compare;
						} else {
							return o1 instanceof SyncFolderItem ? -1 : 1;
						}
					}
				});
				return items;
				
			} catch (IOException e) {
				handleException(e, params.length > 0 && params[0]);
				return null;
			}
		}

		protected void onPostExecute(List<ListItem> result) {
			if (result != null) {
				mItems.clear();
				mItems.addAll(result);
				listServices();
				if (mLastReAuthNotified == null
						|| DateUtils.MINUTE_IN_MILLIS * 15 < System.currentTimeMillis() - mLastReAuthNotified) {
					for (ListItem item : result) {
						if (item.needReAuth) {
							Toast.makeText(OnlineListActivity.this,
									R.string.online_message_notify_reauth, Toast.LENGTH_LONG).show();
							break;
						}
					}
					mLastReAuthNotified = System.currentTimeMillis();
				}
			} else {
			}
			
			if (mCachedPrefs != null) {
				new UpdateAuthPrefsTask(mCachedPrefs).execute();
			}
			
			if (mPreLoaded == null) {
				stopProgress();
			}
		}
	}
	
	private static Long mLastReAuthNotified;

	public class ServiceViewHolder {

		public ImageView serviceIcon;
		public TextView serviceName;
		public TextView lastUpdated;
		public TextView medias;
		public ImageView mediasOver;
		public TextView folders;
		public LinearLayout serviceContainer;
		public LinearLayout imageContainer;
		public View select;

	}
	
	private JsMediaServerClient mJsMedia;
	
	private LayoutInflater mInflater;
	private int mParentColumns;
	private int mParentLength;
	
	private LinearLayout mLytServiceContainer;

	private RelativeLayout mRytSettingSyncContainer;
	private RelativeLayout mRytSettingDispContainer;
	private Button mBtnSettingSync;
	private Button mBtnSettingSyncHide;
	private Button mBtnSettingDisp;
	private Button mBtnSettingDispHide;

	private AsyncTask<Boolean, Void, List<ListItem>> mLoadDataTask;

	private boolean mIsShowSyncButton = false;
	private boolean mIsShowSearviceAddButton = false;

	private int mNumProgress = 0;
	
	/** メニュー項目 */
	protected static final int
		MENU_ITEM_SERVICE = 1,
		MENU_ITEM_UPDATE = 2,
		MENU_ITEM_SEARCH = 3,
		MENU_ITEM_SETTING = 4;

	private OnClickListener mOnClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v == mBtnSettingSync) {
				Intent intent = new Intent(OnlineListActivity.this, OnlineSetupActivity.class);
				for (ListItem item : mItems) {
					if (ClientManager.isBidirectional(item.service)) {
						intent.putExtra(OnlineSetupActivity.EXTRA_SETUP_BIDIRECTIONAL, true);
						intent.putExtra(OnlineSetupActivity.EXTRA_SETUP_BIDIRECTIONAL_SERVICE, item.service);
						intent.putExtra(OnlineSetupActivity.EXTRA_SETUP_BIDIRECTIONAL_ACCOUNT, item.account);
						break;
					}
				}
				intent.putExtra(OnlineSetupActivity.EXTRA_BACK_MODE,
						OnlineSetupActivity.BACK_MODE_ONLINE_LIST);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				goNextHistory(intent);
			} else if (v == mBtnSettingSyncHide) {
				PreferenceUtil.setBooleanPreferenceValue(OnlineListActivity.this, ApplicationDefine.KEY_SHOW_SYNC_SET_BUTTON, false);
				updateButtonVisibility();
				Toast.makeText(OnlineListActivity.this, getString(R.string.online_message_resetting), Toast.LENGTH_LONG).show();
			} else if (v == mBtnSettingDisp) {
				Intent intent = new Intent(OnlineListActivity.this, OnlineSetupActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				goNextHistory(intent);
			} else if (v == mBtnSettingDispHide) {
				PreferenceUtil.setBooleanPreferenceValue(OnlineListActivity.this, ApplicationDefine.KEY_SHOW_SERVICE_ADD_BUTTON, false);
				updateButtonVisibility();
				Toast.makeText(OnlineListActivity.this, getString(R.string.online_message_resetting), Toast.LENGTH_LONG).show();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		UXViewLoader.addThread(ApplicationDefine.NUM_NETWORK_THREAD);
		
		mJsMedia = ClientManager.getJsMediaServerClient(this);
		
		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.online_list);
		
		init();
	}

	@Override
	protected void onDestroy() {
		UXViewLoader.dispose();
		UXAsyncImageView.dispose();
		if (mUpdateAuthPrefsTask != null) {
			mUpdateAuthPrefsTask.cancel(true);
		}
		
		super.onDestroy();
	}
	
	private void init() {
		mInflater = getLayoutInflater();
		
		mLytServiceContainer = (LinearLayout) findViewById(R.id.lytServiceContainer);
		mRytSettingSyncContainer = (RelativeLayout) findViewById(R.id.rytSettingSyncContainer);
		mRytSettingDispContainer = (RelativeLayout) findViewById(R.id.rytSettingDispContainer);
		mBtnSettingSync = (Button) findViewById(R.id.btnSettingServiceSync);
		mBtnSettingSyncHide = (Button) findViewById(R.id.btnSettingServiceSyncHide);
		mBtnSettingDisp = (Button) findViewById(R.id.btnSettingServiceDisp);
		mBtnSettingDispHide = (Button) findViewById(R.id.btnSettingServiceDispHide);
		
		mBtnSettingSync.setOnClickListener(mOnClick);
		mBtnSettingSyncHide.setOnClickListener(mOnClick);
		mBtnSettingDisp.setOnClickListener(mOnClick);
		mBtnSettingDispHide.setOnClickListener(mOnClick);
		
		// 同期の設定ボタンはデータ読み込み時に切り替える
		updateButtonVisibility();
		
		startLoadData(false);
	}
	
	private void listServices() {
		LinearLayout l = mLytServiceContainer;
		l.removeAllViews();
		for (ListItem item : mItems) {
			l.addView(getServiceView(item));
		}
	}
	
	private View getServiceView(ListItem item) {
		View v = mInflater.inflate(R.layout.online_list_item, mLytServiceContainer, false);
		ServiceViewHolder holder = new ServiceViewHolder();
		v.setTag(item);
		v.setOnClickListener(this);
		v.setOnTouchListener(mTouchItemListener);
		
		holder.serviceIcon = (ImageView) v.findViewById(R.id.imgIcon);
		holder.serviceName = (TextView) v.findViewById(R.id.txt_name);
		holder.lastUpdated = (TextView) v.findViewById(R.id.txtLastUpdated);
		holder.folders = (TextView) v.findViewById(R.id.txtFolderCount);
		holder.medias = (TextView) v.findViewById(R.id.txtMediaCount);
		holder.mediasOver = (ImageView) v.findViewById(R.id.media_over);
		holder.serviceContainer = (LinearLayout) v.findViewById(R.id.lytServiceItemContainer);
		holder.imageContainer = (LinearLayout) v.findViewById(R.id.lytImageContainer);
		holder.select = (View) v.findViewById(R.id.viewOver);
		
		final boolean hasFolder =
				item instanceof HasFolderItem;
		// 階層構造を持ったサービスかどうか
		if (!hasFolder) {
			holder.folders.setVisibility(View.GONE);
			holder.imageContainer.setBackgroundResource(R.drawable.folder_container_inside);
			
			holder.serviceContainer.setBackgroundResource(R.drawable.folder_container_outside);

		} else {
			holder.serviceContainer.setBackgroundResource(R.drawable.album_folder_container_outside);
		}

		if (item instanceof SyncFolderItem) {
			v.setBackgroundColor(getResources().getColor(R.color.silver));
		} else {
			v.setBackgroundColor(Color.WHITE);
		}
		// サービスの画像
		Drawable icon = getResources().getDrawable(item.icon);
		holder.serviceIcon.setImageDrawable(icon);
		
		String txt;
		
		// サービス名
		if(item.name.equals(SYNC_FOLDA)){
			txt = getResources().getString(R.string.home_label_sync);
		}else{
			txt = item.name;
		}
		holder.serviceName.setText(txt);
		
		// 最終更新
		if (item.lastUpdated != null) {
			Time time = new Time();
			time.set(item.lastUpdated);
			txt = time.format("%m/%d");
			holder.lastUpdated.setText(getString(R.string.online_format_last_modified, txt));
			holder.lastUpdated.setVisibility(View.VISIBLE);
		} else {
			holder.lastUpdated.setVisibility(View.INVISIBLE);
		}

		// メディア数
		final int NUM_MAX = 1000;
		final int medias = item.countMedia();
		if (NUM_MAX <= medias) {
			holder.medias.setVisibility(View.INVISIBLE);
			holder.medias.setText(null);
			holder.mediasOver.setVisibility(View.VISIBLE);
		} else {
			holder.medias.setVisibility(View.VISIBLE);
			holder.medias.setText(String.valueOf(medias));
			holder.mediasOver.setVisibility(View.INVISIBLE);
		}
		
		// フォルダ数
		final int folders = item.countFolder();
		holder.folders.setText(String.valueOf(folders));
		
		// 画像を取得
		ArrayList<View> images = getServiceImages(item);
		for (View im : images) {
			holder.imageContainer.addView(im);
		}
		
		return v;
	}
	
	/**
	 * サービス選択時のリスナ
	 */
	private OnTouchListener mTouchItemListener = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View view, MotionEvent motionevent) {
			
			// 一行の高さを取得し、オーバーレイのサイズを設定
			View overlay = (View) view.findViewById(R.id.viewOver);
			LinearLayout layout = (LinearLayout) view.findViewById(R.id.lytServiceItemContainer);
			
//			LayoutParams params = layout.getLayoutParams();
			LayoutParams overlayParams = overlay.getLayoutParams();
			
			overlayParams.height = layout.getHeight();
			overlayParams.width = layout.getWidth();
			
			overlay.setLayoutParams(overlayParams);
			
			// アクションごとに、オーバーレイの表示状態を変化
			if (motionevent.getAction() == MotionEvent.ACTION_DOWN) {
				overlay.setBackgroundDrawable(getResources().getDrawable(R.drawable.container_press));
			} else if  (motionevent.getAction() == MotionEvent.ACTION_UP ){
				overlay.setBackgroundDrawable(getResources().getDrawable(R.drawable.view_online_default));
			} else if (motionevent.getAction() == MotionEvent.ACTION_CANCEL) {
				overlay.setBackgroundDrawable(getResources().getDrawable(R.drawable.view_online_default));
			}
			
			return false;
		}
	};
	
	private ArrayList<View> getServiceImages(ListItem item) {
		ArrayList<View> result = new ArrayList<View>();

		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay(); 
		
		Configuration config = getResources().getConfiguration();

		SizeConv sc = SizeConv.getInstance(this);
		
		final int BORDER_COLOR = getResources().getColor(R.color.black);
		
		// 画面の向きによって、描画サイズの計算式を変更
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mParentColumns = 10;
		} else {
			mParentColumns = 6;
		}
		if (item instanceof HasFolderItem) {
			mParentColumns -= 1;
		}
		mParentLength = display.getWidth() / mParentColumns - 10;
		
		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mParentLength, mParentLength);
		final LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		UXViewLoader loader = new UXViewLoader(item.loader);
		if (item instanceof HasFolderItem) {
			HasFolderItem folderItem = (HasFolderItem) item;
			final int folderCount = folderItem.countFolder();
			
			params.width = params.height = mParentLength;
			params.leftMargin = params.rightMargin = params.topMargin = params.bottomMargin = 2;
			
//			final LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			
			final int distance3 = (int)((mParentLength - sc.getSize(8.5f)) / 2);
			final LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(distance3, distance3);
//			params3.leftMargin = 1;
//			params3.rightMargin = 1;
//			params3.topMargin = 1;
//			params3.bottomMargin = 1;
			
			for (int i=0; i<mParentColumns; i++) {
				LinearLayout base = new LinearLayout(this);
				LinearLayout head = new LinearLayout(this);
				LinearLayout tail = new LinearLayout(this);
				
				base.setLayoutParams(wrapParams);
				base.setOrientation(LinearLayout.VERTICAL);
				base.setBackgroundResource(R.drawable.folder_container_inside);
				
				head.setLayoutParams(wrapParams);
				head.setOrientation(LinearLayout.HORIZONTAL);
				tail.setLayoutParams(wrapParams);
				tail.setOrientation(LinearLayout.HORIZONTAL);
				
				base.addView(head);
				base.addView(tail);
				
				List<?> imageData;
				if (i < folderCount) {
					imageData = folderItem.getImageData(i);
					base.setVisibility(View.VISIBLE);
				} else {
					imageData = null;
					base.setVisibility(View.INVISIBLE);
				}
				for (int j=0; j<4; j++) {
					UXAsyncImageView img = new UXAsyncImageView(this);
					img.setLayoutParams(params3);
					if (imageData != null && j < imageData.size()) {
						img.setVisibility(View.VISIBLE);
						img.setBackgroundColor(Color.BLACK);
						img.setBorder(true, BORDER_COLOR, 1.5f);
						img.setViewLoader(loader);
						img.loadImage(imageData.get(j), mParentLength / 2-8);
					} else {
						img.setVisibility(View.INVISIBLE);
					}

					LinearLayout parent;
					if (j<2) {
						parent = head;
					} else {
						parent = tail;
					}
					parent.addView(img);
				}
				
				result.add(base);
			}
		} else {
			params.leftMargin = params.rightMargin = params.topMargin = params.bottomMargin = 0;

			List<?> imageData = item.getImageData(0);
			for (int i=0; i<mParentColumns; i++) {
				UXAsyncImageView img = new UXAsyncImageView(this);
				img.setLayoutParams(params);
				img.setBackgroundColor(Color.BLACK);
				img.setBorder(true, BORDER_COLOR, 1.5f);
				img.setViewLoader(loader);
				if (i < imageData.size()) {
					img.setVisibility(View.VISIBLE);
					
					img.loadImage(imageData.get(i), mParentLength * 2);
				} else {
					img.setVisibility(View.INVISIBLE);
				}
				
				result.add(img);
			}
		}
		return result;
	}
	
	private final List<ListItem> mItems = new ArrayList<ListItem>();
	
	private abstract class ListItem {
		UXThumbnailLoader loader;
		String service;
		String account;
		int icon;
		String name;
		Long lastUpdated;
		boolean needReAuth;
		abstract List<?> getImageData(int folder);
		abstract int countFolder();
		abstract int countMedia();
	}
	
	private abstract class ServiceItem extends ListItem {
		CachingAccessor accessor;
	}
	
	private class SyncFolderItem extends ListItem {
		String localDir;
		File[] files;
		@Override
		List<?> getImageData(int folder) {
			return Arrays.asList(files);
		}
		@Override
		int countFolder() {
			return 0;
		}
		@Override
		int countMedia() {
			return files.length;
		}
	}
	
	private class HasFolderItem extends ServiceItem {
		final List<Directory> directories = new ArrayList<Directory>();
		@Override
		List<?> getImageData(int folder) {
			return directories.get(folder).media;
		}
		@Override
		int countFolder() {
			return directories.size();
		}
		@Override
		int countMedia() {
			int sum = 0;
			for (Directory dir : directories) {
				sum += dir.mediaCount;
			}
			return sum;
		}
	}
	
	private class NoFolderItem extends ServiceItem {
		final List<Media> medias = new ArrayList<Media>();
		int mediaCount;
		String dirId;
		@Override
		List<?> getImageData(int folder) {
			return medias;
		}
		@Override
		int countFolder() {
			return 0;
		}
		@Override
		int countMedia() {
			return mediaCount;
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		listServices();
	}
	
	private final Map<String, CachingAccessor> mAccessors = new HashMap<String, CachingAccessor>();
	private CachingAccessor getAccessor(String serviceType) {
		CachingAccessor accessor = mAccessors.get(serviceType);
		if (accessor == null) {
			ExternalServiceClient client =
					ClientManager.getExternalServiceClient(this, serviceType);
			ExternalServiceCache cache = new ExternalServiceCacheImpl(this);
			accessor = new CachingAccessor(this, mJsMedia, client, cache, true);
			mAccessors.put(serviceType, accessor);
		}
		return accessor;
	}

	@Override
	public void onClick(View v) {
		
		ListItem item = (ListItem) v.getTag();
		Intent intent = null;
		if (item instanceof SyncFolderItem) {
			intent = new Intent(this, NewGridActivity.class);
			intent.putExtra(NewGridActivity.INTENT_FOLDER_TYPE, NewGridActivity.FOLDER_TYPE_PATH);
			intent.putExtra(NewGridActivity.INTENT_TARGET, ((SyncFolderItem) item).localDir);
		} else if (item instanceof NoFolderItem) {
			intent = new Intent(this, OnlineGridActivity.class);
			intent.putExtra(OnlineGridActivity.EXTRA_SERVICE_TYPE, item.service);
			intent.putExtra(OnlineGridActivity.EXTRA_SERVICE_ACCOUNT, item.account);
			intent.putExtra(OnlineGridActivity.EXTRA_SERVICE_DIRID, ((NoFolderItem) item).dirId);
			intent.putExtra(OnlineGridActivity.EXTRA_SERVICE_DIRNAME, ClientManager.getServiceName(this, item.service));
		} else if (item instanceof HasFolderItem) {
			intent = new Intent(this, OnlineFolderActivity.class);
			intent.putExtra(OnlineFolderActivity.EXTRA_SERVICE_TYPE, item.service);
			intent.putExtra(OnlineFolderActivity.EXTRA_SERVICE_ACCOUNT, item.account);
		}
		
		if (intent != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			goNextHistory(intent);
		}
	}
	
//	private File[] list2WaySyncs(String dir) {
//		File[] files = new File(dir).listFiles(new FileFilter() {
//			@Override
//			public boolean accept(File paramFile) {
//				String mime = MediaUtil.getMimeTypeFromPath(paramFile.getName());
//				return mime != null && (mime.startsWith("image/") || mime.startsWith("video/"));
//			}
//		});
//		return files != null ? files : new File[0];
//	}
	
	
	private static final int OPTIONS_MENU_EDIT_SERVICE = 1;
	private static final int OPTIONS_MENU_UPDATE = 2;
	private static final int OPTIONS_MENU_SEARCH = 3;
	private static final int OPTIONS_MENU_PREF = 4;
	
	private static final int REQUEST_CODE_PREFERENCES = 1;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, OPTIONS_MENU_EDIT_SERVICE, 0, getResources().getString(R.string.menu_edit_service)).setIcon(R.drawable.ic_sns_settings);
		menu.add(0, OPTIONS_MENU_UPDATE, 0, getResources().getString(R.string.menu_update)).setIcon(R.drawable.ic_refresh);
		menu.add(0, OPTIONS_MENU_SEARCH, 0, getResources().getString(R.string.menu_search)).setIcon(R.drawable.ic_search);
		menu.add(0, OPTIONS_MENU_PREF, 0, getResources().getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		// サービス編集
		case OPTIONS_MENU_EDIT_SERVICE: {
			Intent intent = new Intent(OnlineListActivity.this, OnlineSetupActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			goNextHistory(intent);
		}
		break;
			
		// 更新
		case OPTIONS_MENU_UPDATE: {
			startLoadData(true);
		}
		break;
		
		// 検索
		case OPTIONS_MENU_SEARCH: {
			Intent intent = new Intent(OnlineListActivity.this, SearchActivity.class);
			startActivity(intent);
		}
		break;
		
		// 設定
		case OPTIONS_MENU_PREF: {
			Intent settingIntent = new Intent(OnlineListActivity.this, JorllePrefsActivity.class);
			startActivityForResult(settingIntent, REQUEST_CODE_PREFERENCES);
		}
		break;
		}
	return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			openOptionsMenu();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_PREFERENCES: {
			updateButtonVisibility();
			startLoadData(false);
			break;
		}
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private void startLoadData(boolean clearCache) {
		cancelLoadData();
		mLoadDataTask = new LoadDataTask(null).execute(clearCache);
	}
	
	private void cancelLoadData() {
		if (mLoadDataTask != null && mLoadDataTask.getStatus() == Status.RUNNING) {
			mLoadDataTask.cancel(false);
		}
	}

	/**
	 * ボタンの表示状態を切り替えます
	 */
	private void updateButtonVisibility() {
		boolean b;
		
		b = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_SHOW_SYNC_SET_BUTTON, true);
		mRytSettingSyncContainer.setVisibility(b && mIsShowSyncButton ? View.VISIBLE : View.GONE);

		b = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_SHOW_SERVICE_ADD_BUTTON, true);
		mRytSettingDispContainer.setVisibility(b && mIsShowSearviceAddButton ? View.VISIBLE : View.GONE);
	}
	
	private UpdateAuthPrefsTask mUpdateAuthPrefsTask;
	private class UpdateAuthPrefsTask extends AsyncTask<Void, Void, List<AuthPreference>> {
		final List<AuthPreference> mCached;
		
		UpdateAuthPrefsTask(List<AuthPreference> cached) {
			super();
			mCached = cached;
		}
		
		@Override
		protected void onPreExecute() {
			mUpdateAuthPrefsTask = this;
			
//			startProgress();
		}
		
		@Override
		protected List<AuthPreference> doInBackground(Void... params) {
			JsMediaServerClient clinet =
					ClientManager.getJsMediaServerClient(OnlineListActivity.this);
			List<AuthPreference> prefs;
			try {
				prefs = clinet.getAuthPreferences(true);
			} catch (IOException e) {
//				e.printStackTrace();		/*$debug$*/
				return null;
			}
			
			return prefs;
		}
		
		@Override
		protected void onPostExecute(List<AuthPreference> result) {
			try {
				if (isCancelled()) {
					return;
				}
				
				if (result != null) {
					if (mCached == null || !mCached.equals(result)) {
						new LoadDataTask(result).execute(false);
					}
				}
			} finally {
				mUpdateAuthPrefsTask = null;
//				stopProgress();
			}
		}
		
		@Override
		protected void onCancelled() {
			mUpdateAuthPrefsTask = null;
//			stopProgress();
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
	}
}