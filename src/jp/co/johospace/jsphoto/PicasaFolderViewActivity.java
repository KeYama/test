package jp.co.johospace.jsphoto;

import java.io.IOException;
import java.util.Arrays;

import jp.co.johospace.jsphoto.cache.PicasaCache;
import jp.co.johospace.jsphoto.folder.AsyncFolderAdapter;
import jp.co.johospace.jsphoto.folder.AsyncListAdapter;
import jp.co.johospace.jsphoto.folder.AsyncPicasaFolderAdapter;
import jp.co.johospace.jsphoto.folder.AsyncPicasaListAdapter;
import jp.co.johospace.jsphoto.util.IOIterator;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Picasa参照フォルダ一覧
 */
public class PicasaFolderViewActivity extends LocalFolderActivity {
	private static final String tag = PicasaFolderViewActivity.class.getSimpleName();

	private static final int DIALOG_PROGRESS = 1;
	
	private PicasaCache mCache;
	
	private static final int FOUND = 1;
	private static final int ADD_IMAGES = 2;
	private static final int NOTIFY = 3;
	private static final int ERROR = -1;
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FOUND:
				mEntries.add((FolderEntry) msg.obj);
				notifyAdpterDataSetChanged();
				break;
				
			case ADD_IMAGES:
				String[] tags = (String[]) msg.obj;
				mEntries.get(msg.arg1).getImages().addAll(Arrays.asList(tags));
				break;
				
			case NOTIFY:
				notifyAdpterDataSetChanged();
				break;
				
			case ERROR:
//				Log.e(tag, "failed to list album.", (Exception) msg.obj);		/*$debug$*/
				// TODO メッセージ
				break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String account = PicasaPrefsActivity.getPicasaAccount(this);
		mCache = new PicasaCache(this, account);
		
//		ImageView iconFilter = (ImageView) findViewById(R.id.iconFiltering);
//		if (iconFilter != null) {
//			iconFilter.setVisibility(View.GONE);
//		}
	}
	
	@Override
	protected void doOnResume() {
	}
	
	@Override
	protected void showScanProgress() {
	}
	
	@Override
	protected AsyncFolderAdapter createFolderAdapter() {
		return new AsyncPicasaFolderAdapter(this, mEntries);
	}
	
	@Override
	protected AsyncListAdapter createListAdapter() {
		return new AsyncPicasaListAdapter(this, mEntries);
	}
	
	@Override
	protected void doFindMedia(final int callback) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				showDialog(DIALOG_PROGRESS);
			}
			
			@Override
			protected Void doInBackground(Void... params) {
				try {
					listDirs(callback);
				} catch (IOException e) {
					sendError(e);
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				removeDialog(DIALOG_PROGRESS);
			}
			
		}.execute();
		
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					listDirs(callback);
//				} catch (IOException e) {
//					sendError(e);
//				}
//			}
//		}.start();
	}
	
	private void listDirs(int imgLimit) throws IOException {
		IOIterator<FolderEntry> itr = mCache.listDirs(imgLimit, true);
		try {
			while (itr.hasNext()) {
				sendFound(itr.next());
				sendNotify();
			}
		} finally {
			itr.terminate();
		}
		
//		for (int i = 0; i < mEntries.size(); i++) {
//			ArrayList<String> tags = new ArrayList<String>();
//			IOIterator<MediaEntry> itrMedia =
//					mCache.listMediasAt(mEntries.get(i).getPath(), imgLimit);
//			try {
//				while (itrMedia.hasNext() && tags.size() < imgLimit) {
//					MediaEntry entry = itrMedia.next();
//					tags.add(PicasaCache.encodeTag(entry.getDirId(), entry.getMediaId()));
//				}
//			} finally {
//				itrMedia.terminate();
//			}
//			
//			sendAddImages(i, tags.toArray(new String[0]));
//		}
//		sendNotify();
	}
	
	private void sendError(Exception e) {
		Message message = mHandler.obtainMessage(ERROR, e);
		mHandler.sendMessage(message);
	}
	
	private void sendFound(FolderEntry entry) {
		Message message = mHandler.obtainMessage(FOUND, entry);
		mHandler.sendMessage(message);
	}
	
	private void sendAddImages(int pos, String[] tags) {
		Message message = mHandler.obtainMessage(ADD_IMAGES, pos, 0, tags);
		mHandler.sendMessage(message);
	}
	
	private void sendNotify() {
		Message message = mHandler.obtainMessage(NOTIFY);
		mHandler.sendMessage(message);
	}
	
	@Override
	protected void startGridActivity(FolderEntry fe) {
		Intent intent = new Intent(this, PicasaGridActivity.class);
		intent.putExtra(PicasaGridActivity.EXTRA_DIRID, fe.getPath());
		startActivity(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ITEM_SETTING, 0,
				getResources().getString(R.string.menu_setting))
			.setIcon(R.drawable.ic_setting);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// 設定
		case MENU_ITEM_SETTING:
			Intent intent = new Intent(this, JorllePrefsActivity.class);
			startActivity(intent);
			break;
		}

		return true;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_PROGRESS:
			ProgressDialog d = new ProgressDialog(this);
			d.setTitle(R.string.folder_picasa_progress_get_album);
			d.setCancelable(false);
			return d;
		}
		
		return null;
	}
}
