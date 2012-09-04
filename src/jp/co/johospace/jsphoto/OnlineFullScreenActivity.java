package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.fullscreen.ImageSurfaceView;
import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoader;
import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoaderFactory;
import jp.co.johospace.jsphoto.grid.ExifView;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.ui.OnlineImageLoader;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

/**
 * オンラインフルスクリーン
 */
public class OnlineFullScreenActivity extends AbstractFullscreenActivity {

	public static final String EXTRA_SERVICE_TYPE =
			OnlineFullScreenActivity.class.getName() + ".EXTRA_SERVICE_TYPE";
	public static final String EXTRA_SERVICE_ACCOUNT =
			OnlineFullScreenActivity.class.getName() + ".EXTRA_SERVICE_ACCOUNT";
	public static final String EXTRA_DIRID =
			OnlineFullScreenActivity.class.getName() + ".EXTRA_DIRID";
	public static final String EXTRA_POSITION =
			OnlineFullScreenActivity.class.getName() + ".EXTRA_POSITION";
	
	private List<Media> mMedias;
	private int mPosition;
	
	
	private JsMediaServerClient mJsMedia;
	private CachingAccessor mClient;
	private ExternalServiceCache mCache;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.fullscreen);
		findViewById(R.id.lytListHeader).setVisibility(View.GONE);
		
		mJsMedia = ClientManager.getJsMediaServerClient(this);
		ExternalServiceClient external =
				ClientManager.getExternalServiceClient(this,
						getIntent().getStringExtra(EXTRA_SERVICE_TYPE));
		mCache = new ExternalServiceCacheImpl(this);
		mClient = new CachingAccessor(this, mJsMedia, external, mCache, false);
		
		final String service = getIntent().getStringExtra(EXTRA_SERVICE_TYPE);
		final String account = getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT);
		final String dirId = getIntent().getStringExtra(EXTRA_DIRID);
		final int position = getIntent().getIntExtra(EXTRA_POSITION, 0);
		
		prepareInfoLinkOnCreate();
		
		new AsyncTask<Void, Void, List<Media>>() {
			@Override
			protected List<Media> doInBackground(Void... params) {
				try {
					return mClient.getMediaList(service, account, dirId);
				} catch (IOException e) {
//					e.printStackTrace();		/*$debug$*/
					handleException(e, true);
					return null;
				}
			}
			
			@Override
			protected void onPostExecute(List<Media> result) {
				if (result != null) {
					mMedias = result;
					recreateSurfaceView(position);
				} else {
					// TODO $ol
					finish();
				}
			}
		}.execute();
	}
	
	protected void recreateSurfaceView(int pos) {
		FrameLayout frame = (FrameLayout)findViewById(R.id.flFullscreen);
		frame.removeAllViews();
		if(mSurfaceView != null)mSurfaceView.dispose();
		
		mPosition = pos;
		
		frame.addView(
			mSurfaceView = new ImageSurfaceView(this, new ImageLoaderFactory() {
				@Override
				public ImageLoader create() {
					return new OnlineImageLoader(getApplicationContext(), mClient);
				}
			}, mMedias, mPosition)
		);
		
		startInfoLinkNavigation();
	}
	
	protected String getCurrentName() {
		Media media = (Media) mSurfaceView.getCurrentTag();
		return media.fileName;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBackKey();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU) {
			openOptionsMenu();
			return true;
		}
		return super.onKeyDown(keyCode, event);
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








	
	private static final int REQUEST_SELECT_FOLDER_FOR_COPY_SINGLE = 1;
	
	private static final int
		OPTION_ITEM_DETAIL = 1,
		OPTION_ITEM_COPY = 2,
		OPTION_ITEM_COPY_TO_SYNC = 3,
		OPTION_ITEM_PLAY = 4;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		try {
			Media media = (Media) mSurfaceView.getCurrentTag();
			String contentType = mClient.getMediaContentType(media);
			if (contentType != null && contentType.startsWith("video/")) {
				Pair<String, String> content = mClient.getContentsUrl(media, contentType);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse(content.first), content.second);
				menu.add(0, OPTION_ITEM_PLAY, 0, getResources().getString(R.string.image_context_play)).setIcon(R.drawable.ic_detail).setIntent(intent);
			} else {
				menu.add(0, OPTION_ITEM_DETAIL, 0, getResources().getString(R.string.image_context_info)).setIcon(R.drawable.ic_detail);
			}
		} catch (IOException e) {
			;
		}
		menu.add(0, OPTION_ITEM_COPY, 0, getResources().getString(R.string.image_context_copy)).setIcon(R.drawable.ic_copy);
		if (getSyncLocalDir() != null) {
			menu.add(0, OPTION_ITEM_COPY_TO_SYNC, 0, getResources().getString(R.string.image_context_move_sync)).setIcon(R.drawable.ic_update);
		}
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return onCreateOptionsMenu(menu);
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
		switch (item.getItemId()) {
		case OPTION_ITEM_COPY: {
			Intent copyIntent = new Intent(getApplicationContext(), SelectFolderActivity.class);
			copyIntent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.image_context_copy));
			copyIntent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
			startActivityForResult(copyIntent, REQUEST_SELECT_FOLDER_FOR_COPY_SINGLE);
		}
		break;
		
		case OPTION_ITEM_COPY_TO_SYNC: {
			Media media = (Media) mSurfaceView.getCurrentTag();
			String localDir = getSyncLocalDir();
			new CopyTask(media, new File(localDir)).execute();
		}
		break;
		
		case OPTION_ITEM_DETAIL: {
			Media media = (Media) mSurfaceView.getCurrentTag();
			new ShowDetailTask().execute(media);
		}
		break;
		
		case OPTION_ITEM_PLAY: {
			return false;
		}
		}

		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_SELECT_FOLDER_FOR_COPY_SINGLE: {
			if (resultCode == RESULT_OK) {
				Media media = (Media) mSurfaceView.getCurrentTag();
				String dest = data.getStringExtra(SelectFolderActivity.PARAM_START_PATH);
				new CopyTask(media, new File(dest)).execute();
			}
		}
		break;
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
//				e.printStackTrace();		/*$debug$*/
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
					Toast.makeText(OnlineFullScreenActivity.this,
							R.string.online_message_copied, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(OnlineFullScreenActivity.this,
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
					ScrollView scrollview = new ScrollView(OnlineFullScreenActivity.this);
					scrollview.setLayoutParams(new ViewGroup.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
					scrollview.addView(new ExifView(OnlineFullScreenActivity.this, result.getAbsolutePath()));
					
					new AlertDialog.Builder(OnlineFullScreenActivity.this)
					.setTitle(getString(R.string.image_context_info))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setView(scrollview)
					.setPositiveButton(android.R.string.ok, null)
					.show();
				} else {
					Toast.makeText(OnlineFullScreenActivity.this,
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
			ProgressDialog d = new ProgressDialog(OnlineFullScreenActivity.this);
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
			ProgressDialog d = new ProgressDialog(OnlineFullScreenActivity.this);
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

	@Override
	protected void onDestroy() {
		if(mSurfaceView != null)mSurfaceView.dispose();
		mSurfaceView = null;
		mHandler.removeCallbacksAndMessages(null);
		super.onDestroy();
	}
	
}
