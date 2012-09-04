package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.grid.ExifView;
import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import jp.co.johospace.jsphoto.util.HeaderController;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.ux.widget.UXGridDataSource;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget;
import jp.co.johospace.jsphoto.ux.widget.UXStage;
import jp.co.johospace.jsphoto.ux.widget.UXUnit;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * オンライングリッド
 * 
 * TODO ヘッダの表示をタップイベントから拾うように切り替える
 */
public class OnlineGridActivity extends NavigatableActivity
		implements UXGridWidget.ItemTapListener, UXGridWidget.ItemLongPressListener {

	public static final String EXTRA_SERVICE_TYPE =
			OnlineFolderActivity.class.getName() + ".EXTRA_SERVICE_TYPE";
	public static final String EXTRA_SERVICE_ACCOUNT =
			OnlineFolderActivity.class.getName() + ".EXTRA_SERVICE_ACCOUNT";
	public static final String EXTRA_SERVICE_DIRID =
			OnlineFolderActivity.class.getName() + ".EXTRA_SERVICE_DIRID";
	public static final String EXTRA_SERVICE_DIRNAME =
			OnlineFolderActivity.class.getName() + ".EXTRA_SERVICE_DIRNAME";
	
	private static final int REQUEST_SELECT_FOLDER_FOR_COPY_SINGLE = 1;
	
	private static final int COLUMN_NORMAL_LANDSCAPE = 5;
	private static final int COLUMN_NORMAL_PORTLAIT = 3;
	
	private Button mBtnUpdateCache;
	private CachingAccessor mClient;

	private UXStage mStage;
	private UXGridWidget mGrid;
	private HeaderController mHeaderController;
	private View mHeader;
	
	private static final int WIDTH = 200;
	
	private List<Media> mMedias = new ArrayList<Media>();
	
	private int mColumnCount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		System.out.println("++++++++++++++++++++ onCreate " + getClass().getSimpleName());		/*$debug$*/
		
		String service = getIntent().getStringExtra(EXTRA_SERVICE_TYPE);
		String account = getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT);
		String dirid = getIntent().getStringExtra(EXTRA_SERVICE_DIRID);
		if (TextUtils.isEmpty(service) || TextUtils.isEmpty(account)|| TextUtils.isEmpty(dirid)) {
			throw new IllegalArgumentException("service type, account and dirid are required.");
		}
		
		JsMediaServerClient jsMedia = ClientManager.getJsMediaServerClient(this);
		ExternalServiceClient external = ClientManager.getExternalServiceClient(this, service);
		ExternalServiceCache cache = new ExternalServiceCacheImpl(this);
		
		mClient = new CachingAccessor(this, jsMedia, external, cache, true);
		
		
		mStage = new UXStage(this, UXStage.GL_RENDERER);
		mStage.addThread(ApplicationDefine.NUM_NETWORK_THREAD);
		mStage.setBackgroundColor(Color.rgb(20, 20, 20));
		mStage.setScrollbarResource(R.drawable.slider, 28, 65);
		
		getColumnCount();
		
		//Stageに変更を加える場合、必ずlockStageを経由
		//
		//Stageに変更とは、Stageにぶら下がっているすべてのクラスの変更のこと。
		//たとえばDataSourceの中身に変更を加える場合でも必要となる。
		//
		mStage.lockStage(new Runnable() {
			@Override
			public void run() {
				mGrid = new UXGridWidget(WIDTH, mClient);
				mGrid	
					.dataSource(mOnlineDataSource)
					.padding(5, UXUnit.DP)
					.itemType(new UXGridWidget.ThumbnailGrid())
					.column(mColumnCount)
					.addTo(mStage);
				
				mGrid.setOnItemTapListener(OnlineGridActivity.this);
				
				mGrid.setOnItemLongPressListener(OnlineGridActivity.this);
				
				mStage.invalidate();
			}
		});
		
		//最後にViewを追加
		setContentView(mStage.getView());

		// ヘッダ関連の準備
		mHeaderController = new HeaderController(this, R.layout.ol_folder_info, (ViewGroup) getWindow().getDecorView());
		mHeaderController.setHeaderEventTo(mStage);
		mHeader = mHeaderController.getView();
		mHeader.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
		mBtnUpdateCache = (Button) mHeader.findViewById(R.id.btnRefresh);
		mBtnUpdateCache.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mUpdateCacheTask != null && mUpdateCacheTask.getStatus() == Status.RUNNING) {
					// 更新中
					mUpdateCacheTask.cancel(true);
				} else {
					mUpdateCacheTask = new UpdateCacheTask();
					mUpdateCacheTask.execute();
				}
			}
		});
		
		setHeaderInfo();

		// ヘッダを表示
		mHeaderController.show(HeaderController.LENGTH_DEFAULT);

		new LoadMediasTask().execute();
	}

	private UXGridDataSource mOnlineDataSource = new UXGridDataSource() {
		
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
			return mMedias.size();
		}
		
		@Override
		public Object getInfo(int item) {
			return mMedias.get(item);
		}
	};
	
	
	private boolean mAttachContext2Options;
	private int mContextItemNumber;

	@Override
	public void onLongPress(int itemNumber) {
		mAttachContext2Options = true;
		mContextItemNumber = itemNumber;
		openOptionsMenu();
	}
	
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		mAttachContext2Options = false;
		super.onOptionsMenuClosed(menu);
	}

	@Override
	public void onTap(final int itemNumber) {
		if (itemNumber < mMedias.size()) {
			final Media media = mMedias.get(itemNumber);
			new AsyncTask<Void, Void, Intent>() {
				@Override
				protected Intent doInBackground(Void... params) {
					String mime = MediaUtil.getMimeTypeFromPath(media.fileName);
					if (mime == null || mime.startsWith("video/")) {
						try {
							mime = mClient.getMediaContentType(media);
						} catch (IOException e) {
//							runOnUiThread(new Runnable() {
//								@Override
//								public void run() {
//									Toast.makeText(OnlineGridActivity.this,
//											R.string.error_failed_to_connect, Toast.LENGTH_LONG).show();
//								}
//							});
							handleException(e, true);
							return null;
						}
					}
					
					Intent intent;
					if (mime != null && mime.startsWith("video/")) {
						ExternalServiceClient client =
								ClientManager.getExternalServiceClient(OnlineGridActivity.this, media.service);
						Pair<String, String> content = client.getContentsUrl(media, mime);
						intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse(content.first), content.second);
					} else {
						intent = new Intent(OnlineGridActivity.this, OnlineFullScreenActivity.class);
						intent.putExtra(OnlineFullScreenActivity.EXTRA_CATEGORY_NAME,
								getIntent().getStringExtra(EXTRA_SERVICE_DIRNAME));
						intent.putExtra(OnlineFullScreenActivity.EXTRA_SERVICE_TYPE,
								getIntent().getStringExtra(EXTRA_SERVICE_TYPE));
						intent.putExtra(OnlineFullScreenActivity.EXTRA_SERVICE_ACCOUNT,
								getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT));
						intent.putExtra(OnlineFullScreenActivity.EXTRA_DIRID,
								getIntent().getStringExtra(EXTRA_SERVICE_DIRID));
						intent.putExtra(OnlineFullScreenActivity.EXTRA_POSITION, itemNumber);
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
	}
	
	private class LoadMediasTask extends AsyncTask<Void, Void, List<Media>> {
		@Override
		protected void onPreExecute() {
			// TODO $ol
			super.onPreExecute();
		}
		
		@Override
		protected List<Media> doInBackground(Void... params) {
			try {
				return mClient.getMediaList(
						getIntent().getStringExtra(EXTRA_SERVICE_TYPE),
						getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT),
						getIntent().getStringExtra(EXTRA_SERVICE_DIRID));
			} catch (IOException e) {
//				e.printStackTrace();		/*$debug$*/
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(final List<Media> result) {
			mMedias.clear();
			if (result != null) {
				mMedias.addAll(result);
			}

			if (mMedias.isEmpty()) {
				// データがない場合は前画面に戻る
				Toast.makeText(OnlineGridActivity.this, getString(R.string.image_message_no_media), Toast.LENGTH_LONG).show();
				onBackRefleshHistory();
			} else {
				mStage.lockStage(new Runnable() {
					@Override
					public void run() {
						mStage.invalidate();
					}
				});
			}
		}
	}
	
	private void setHeaderInfo() {
		TextView txtName = (TextView) mHeader.findViewById(R.id.txt_name);
		
		String title;
		if (getIntent().hasExtra(EXTRA_SERVICE_DIRNAME)) {
			title = getIntent().getStringExtra(EXTRA_SERVICE_DIRNAME);
		} else {
			title = ClientManager.getServiceName(
					getApplicationContext(), getIntent().getStringExtra(EXTRA_SERVICE_TYPE));
		}
		txtName.setText(title);
		
		TextView txtUp = (TextView) mHeader.findViewById(R.id.txtLastUpdated);
		final Long lastUpdatedObj = 
				mClient.getCache().getLastUpdated(
						getIntent().getStringExtra(EXTRA_SERVICE_TYPE),
						getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT),
						getIntent().getStringExtra(EXTRA_SERVICE_DIRID));
		if (lastUpdatedObj != null) {
			final long lastUpdated = lastUpdatedObj;
			final Time time = new Time();
			time.set(lastUpdated);
			String lastupdatedString = getString(R.string.online_format_last_modified2, time.format("%Y/%m/%d %H:%M"));
			txtUp.setText(lastupdatedString);
			
			txtUp.setVisibility(View.VISIBLE);
		} else {
			txtUp.setVisibility(View.INVISIBLE);
		}
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
		mHeaderController.show(HeaderController.LENGTH_DEFAULT);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mStage.dispose();
		mHeaderController.onDestroy();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBackRefleshHistory();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU) {
			openOptionsMenu();
			return true;
		}
		return super.onKeyDown(keyCode, event);
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
	
	private UpdateCacheTask mUpdateCacheTask;
	private class UpdateCacheTask extends AsyncTask<Void, Void, Exception> {
		
		@Override
		protected void onPreExecute() {
			mUpdateCacheTask = this;
			mBtnUpdateCache.setBackgroundResource(R.drawable.cancel);
			
			mHeaderController.setDisplayKeep(true);
			if (!mHeaderController.isHeaderDisplaied()) {
				mHeaderController.show();
			}
		}
		
		@Override
		protected Exception doInBackground(Void... params) {
			try {
				mClient.updateDirectoryCache(
						getIntent().getStringExtra(EXTRA_SERVICE_TYPE),
						getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT));
			} catch (IOException e) {
				handleException(e, true);
				return e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Exception result) {
			try {
				if (result == null) {
					if (!isCancelled()) {
						new LoadMediasTask().execute();
					}
				} else {
					// TODO $ol
				}
			} finally {
				mBtnUpdateCache.setBackgroundResource(R.drawable.button_refresh);
				mUpdateCacheTask = null;
				
				mHeaderController.setDisplayKeep(false);
				mHeaderController.hide(HeaderController.LENGTH_DEFAULT);
			}
		}
		
		@Override
		protected void onCancelled() {
			try {
			} finally {
				mBtnUpdateCache.setBackgroundResource(R.drawable.button_refresh);
				mUpdateCacheTask = null;

				mHeaderController.setDisplayKeep(false);
				mHeaderController.hide(HeaderController.LENGTH_DEFAULT);
			}
		}
	}
	
	
	
	
	protected static final int
		MENU_ITEM_SEARCH = 1,
		MENU_ITEM_SETTING = 2;
	protected static final int
		CONTEXT_ITEM_DETAIL = 1,
		CONTEXT_ITEM_COPY = 2,
		CONTEXT_ITEM_COPY_TO_SYNC = 3,
		CONTEXT_ITEM_PLAY = 4;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		if (mAttachContext2Options) {
			// コンテキストメニュー
			try {
				Media media = (Media) mMedias.get(mContextItemNumber);
				String contentType = mClient.getMediaContentType(media);
				if (contentType != null && contentType.startsWith("video/")) {
					Pair<String, String> content = mClient.getContentsUrl(media, contentType);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse(content.first), content.second);
					menu.add(0, CONTEXT_ITEM_PLAY, 0, getResources().getString(R.string.image_context_play)).setIcon(R.drawable.ic_detail).setIntent(intent);
				} else {
					menu.add(0, CONTEXT_ITEM_DETAIL, 0, getResources().getString(R.string.image_context_info)).setIcon(R.drawable.ic_detail);
				}
			} catch (IOException e) {
				;
			}
			
			menu.add(0, CONTEXT_ITEM_COPY, 0, getResources().getString(R.string.image_context_copy)).setIcon(R.drawable.ic_copy);
			
			if (getSyncLocalDir() != null) {
				menu.add(0, CONTEXT_ITEM_COPY_TO_SYNC, 0, getResources().getString(R.string.image_context_move_sync)).setIcon(R.drawable.ic_update);
			}
		} else {
			// オプションメニュー
			menu.add(0, MENU_ITEM_SEARCH, 0, getResources().getString(R.string.menu_search)).setIcon(R.drawable.ic_search);
			menu.add(0, MENU_ITEM_SETTING, 0, getResources().getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);
		}
		
		return true;
	}
	
	private String getSyncLocalDir() {
		Map<String, Map<String, SyncSetting>> settings =
				MediaSyncManagerV2.loadSyncSettings(getApplicationContext());
		for (String service : settings.keySet()) {
			Map<String, SyncSetting> accounts = settings.get(service);
			for (String account : accounts.keySet()) {
				SyncSetting setting = accounts.get(account);
				return setting.localDir;
			}
		}
		return null;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mAttachContext2Options) {
			// コンテキストメニュー
			switch (item.getItemId()) {
			case CONTEXT_ITEM_COPY: {
				Intent copyIntent = new Intent(getApplicationContext(), SelectFolderActivity.class);
				copyIntent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.image_context_copy));
				copyIntent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
				startActivityForResult(copyIntent, REQUEST_SELECT_FOLDER_FOR_COPY_SINGLE);
			}
			break;
			
			case CONTEXT_ITEM_COPY_TO_SYNC: {
				Media media = mMedias.get(mContextItemNumber);
				String localDir = getSyncLocalDir();
				new CopyTask(media, new File(localDir)).execute();
			}
			break;
			
			case CONTEXT_ITEM_DETAIL: {
				new ShowDetailTask().execute(mMedias.get(mContextItemNumber));
			}
			break;
			
			case CONTEXT_ITEM_PLAY: {
				return false;
			}
			}
		} else {
			// オプションメニュー
			switch(item.getItemId()) {
			// 検索
			case MENU_ITEM_SEARCH:
				Intent intent = new Intent(OnlineGridActivity.this, SearchActivity.class);
				startActivity(intent);
				break;
			// 設定
			case MENU_ITEM_SETTING:
				Intent settingIntent = new Intent(OnlineGridActivity.this, JorllePrefsActivity.class);
				startActivity(settingIntent);
				break;

		}
		}

		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_SELECT_FOLDER_FOR_COPY_SINGLE: {
			if (resultCode == RESULT_OK) {
				Media media = mMedias.get(mContextItemNumber);
				String dest = data.getStringExtra(SelectFolderActivity.PARAM_START_PATH);
				new CopyTask(media, new File(dest)).execute();
			}
		}
		break;
		}
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
	
	private CopyTask mCopyTask;
	private class CopyTask extends AsyncTask<Void, Void, Exception> {
		final Media mSource;
		final File mDestDir;
		CopyTask(Media source, File dest) {
			super();
			mSource = source;
			mDestDir = dest;
		}
		
		@Override
		protected void onPreExecute() {
			mCopyTask = this;
			showDialog(DIALOG_DOWNLOADING_COPY);
		}
		
		@Override
		protected Exception doInBackground(Void... params) {
			InputStream sourceStream = null;
			OutputStream destStream = null;
			try {
				String[] out_contentType = new String[1];
				String contentType = mClient.getMediaContentType(mSource);
				if (contentType != null && contentType.startsWith("video/")) {
					Pair<String, String> content = mClient.getContentsUrl(mSource, contentType);
					if (content.second != null) {
						contentType = content.second;
					}
					sourceStream = new URL(content.first).openStream();
					
				} else {
					File mediaFile = mClient.getMediaContentFile(mSource, out_contentType);
					sourceStream = new FileInputStream(mediaFile);
				}
				
				String destFileName;
				String namedType = MediaUtil.getMimeTypeFromPath(mSource.fileName);
				if (namedType == null) {
					String detectedExt = MediaUtil.getTypicalExtensionFromMimeType(contentType);
					destFileName =
							detectedExt != null ? String.format("%s.%s", mSource.fileName, detectedExt) : mSource.fileName;
				} else {
					destFileName = mSource.fileName;
				}
				
				File destFile = new File(mDestDir, destFileName);
				int seq = 0;
				while (destFile.exists()) {
					int lastDot = destFileName.lastIndexOf(".");
					String name;
					String ext;
					if (0 <= lastDot) {
						name = destFileName.substring(0, lastDot);
						ext = destFileName.substring(lastDot);
					} else {
						name = destFileName;
						ext = "";
					}
					destFile = new File(mDestDir,
							String.format("%s_%d%s", name, ++seq, ext));
				}
				if (!isCancelled()) {
					destStream = new FileOutputStream(destFile);
					IOUtil.copy(sourceStream, destStream);
					Context context = getApplicationContext();
					MediaUtil.scanMedia(context, destFile, false);
				}
			} catch (Exception e) {
				handleException(e, true);
				return e;
			} finally {
				if (sourceStream != null) {
					try {
						sourceStream.close();
					} catch (IOException e) {
						;
					}
				}
				if (destStream != null) {
					try {
						destStream.close();
					} catch (IOException e) {
						;
					}
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Exception result) {
			try {
				removeDialog(DIALOG_DOWNLOADING_COPY);
				if (result == null) {
					Toast.makeText(OnlineGridActivity.this,
							R.string.online_message_copied, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(OnlineGridActivity.this,
							R.string.error_failed_to_download, Toast.LENGTH_LONG).show();
				}
			} finally {
				mCopyTask = null;
			}
		}
		
		@Override
		protected void onCancelled() {
			mCopyTask = null;
		}
	}
	
	private ShowDetailTask mShowDetailTask;
	private class ShowDetailTask extends AsyncTask<Media, Void, File> {
		
		@Override
		protected void onPreExecute() {
			mShowDetailTask = this;
			showDialog(DIALOG_DOWNLOADING_DETAIL);
		}
		
		@Override
		protected File doInBackground(Media... params) {
			try {
				String[] contentType = new String[1];
				File mediaFile = mClient.getMediaContentFile(params[0], contentType);
				return mediaFile;
			} catch (IOException e) {
				handleException(e, true);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(File result) {
			try {
				removeDialog(DIALOG_DOWNLOADING_DETAIL);
				if (result != null) {
					ScrollView scrollview = new ScrollView(OnlineGridActivity.this);
					scrollview.setLayoutParams(new ViewGroup.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
					scrollview.addView(new ExifView(OnlineGridActivity.this, result.getAbsolutePath()));
					
					new AlertDialog.Builder(getParent())
					.setTitle(getString(R.string.image_context_info))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setView(scrollview)
					.setPositiveButton(android.R.string.ok, null)
					.show();
				} else {
					Toast.makeText(OnlineGridActivity.this,
							R.string.error_failed_to_download, Toast.LENGTH_LONG).show();
				}
			} finally {
				mShowDetailTask = null;
			}
		}
		
		@Override
		protected void onCancelled() {
			mShowDetailTask = null;
		}
	}
	
	private static final int DIALOG_DOWNLOADING_DETAIL = 1;
	private static final int DIALOG_DOWNLOADING_COPY = 2;
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DOWNLOADING_DETAIL: {
			ProgressDialog d = new ProgressDialog(getParent());
			d.setTitle(getString(R.string.image_context_info));
			d.setMessage(getString(R.string.online_message_downloading));
			d.setCancelable(true);
			d.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					dialog.dismiss();
					if (mShowDetailTask != null) {
						mShowDetailTask.cancel(true);
					}
				}
			});
			return d;
		}
		case DIALOG_DOWNLOADING_COPY: {
			ProgressDialog d = new ProgressDialog(getParent());
			d.setTitle(getString(R.string.image_context_copy));
			d.setMessage(getString(R.string.online_message_downloading));
			d.setCancelable(true);
			d.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					dialog.dismiss();
					if (mCopyTask != null) {
						mCopyTask.cancel(true);
					}
				}
			});
			return d;
		}
		}
		return super.onCreateDialog(id);
	}

	/**
	 * データを読み込み中かどうかを取得します
	 * @return
	 */
	private boolean isDataLoading() {
		return mUpdateCacheTask != null && mUpdateCacheTask.getStatus() == Status.RUNNING;
	}

}
