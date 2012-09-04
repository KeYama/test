package jp.co.johospace.jsphoto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.cache.PicasaCache;
import jp.co.johospace.jsphoto.cache.PicasaCache.MediaEntry;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.grid.AsyncImageAdapter;
import jp.co.johospace.jsphoto.grid.AsyncPicasaImageAdapter;
import jp.co.johospace.jsphoto.onlineservice._cost;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.AlbumEntry;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.LitePhotoEntry;
import jp.co.johospace.jsphoto.util.IOIterator;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;

/**
 * Picasa参照 画像一覧
 */
public class PicasaGridActivity extends GridActivity {
	private static final String tag = PicasaGridActivity.class.getSimpleName();

	public static final String EXTRA_DIRID =
			PicasaGridActivity.class.getSimpleName() + ".EXTRA_DIRID";
	
	private static final int DIALOG_PROGRESS = 1;
	
	private PicasaCache mCache;
	private AlbumEntry mEntry;
	
	private static final int FOUND = 1;
	private static final int ERROR = -1;
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FOUND:
				MediaEntry entry = (MediaEntry) msg.obj;
				mAdapter.addItem(PicasaCache.encodeTag(
						entry.getDirId(), entry.getMediaId()));
//				mAdapter.notifyDataSetChanged();
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
		
		String account =
				PicasaPrefsActivity.getPicasaAccount(this);
		mCache = new PicasaCache(this, account);
		
		try {
			mEntry = mCache.loadDirEntry(
					getIntent().getStringExtra(EXTRA_DIRID));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
//		((TextView)findViewById(R.id.tvTitle)).setText(mEntry.getName());
		mFullTitle = mEntry.getName();
		
//		View iconFilter = findViewById(R.id.iconFiltering);
//		if (iconFilter != null) {
			//iconFilter.setVisibility(View.GONE);
//		}
//		View iconMulti = findViewById(R.id.iconMulti);
//		if (iconMulti != null) {
//			iconMulti.setVisibility(View.GONE);
//		}
		
		listMedia();
	}
	
	private void listMedia() {
		new AsyncTask<Void, Void, List<String>>() {

long entire;//TODO
			@Override
			protected void onPreExecute() {
				showDialog(DIALOG_PROGRESS, null);
_cost.cacheOperation = 0;//TODO
_cost.network = 0;//TODO
_cost.loadMediaEntry = 0;//TODO
_cost.deleteImageCaches = 0;//TODO
_cost.saveMediaEntry = 0;//TODO
			}
			
			private Exception mThrown;
			@Override
			protected List<String> doInBackground(Void... params) {
long st = SystemClock.elapsedRealtime();//TODO
				ArrayList<String> tags = new ArrayList<String>();
				try {
					IOIterator<MediaEntry> itr =
							mCache.listMediasAt(mEntry.gphotoId, 1000);
					try {
						while (itr.hasNext()) {
//							if (mDisposed) {
//								return;
//							}
//							sendFound(itr.next());
							MediaEntry entry = itr.next();
							tags.add(PicasaCache.encodeTag(entry.getDirId(), entry.getMediaId()));
						}
					} finally {
						itr.terminate();
					}
					
entire = SystemClock.elapsedRealtime() - st;//TODO
					return tags;
					
				} catch (IOException e) {
//					Log.e(tag, "failed to list medias.", e);		/*$debug$*/
					mThrown = e;
					return null;
				}
			}
			
			@Override
			protected void onPostExecute(List<String> result) {
//System.out.println(String.format("!!!!!!!!!!!!!!PAF!!!!!!!!!!!!!!!!!! Total:%d (Network:%d, Cache:%d(Load:%d, Delete:%d, Save:%d))",
//		entire, _cost.network, _cost.cacheOperation, _cost.loadMediaEntry, _cost.deleteImageCaches, _cost.saveMediaEntry));		/*$debug$*/ //TODO
				removeDialog(DIALOG_PROGRESS);
				if (mThrown != null) {
					sendError(mThrown);
				} else {
					mAdapter.addAll(result);
				}
			}
			
		}.execute();
	}

//	private boolean mDisposed;
//	@Override
//	protected void onDestroy() {
//		super.onDestroy();
//		mDisposed = true;
//	}
	
	@Override
	protected AsyncImageAdapter createAdapter() {
		return new AsyncPicasaImageAdapter(this);
	}
	
	@Override
	protected void createSortingMenu(int defaultCheck) {
	}
	
	private void sendError(Exception e) {
		Message message = mHandler.obtainMessage(ERROR, e);
		mHandler.sendMessage(message);
	}
	
	private void sendFound(MediaEntry entry) {
		Message message = mHandler.obtainMessage(FOUND, entry);
		mHandler.sendMessage(message);
	}
	
	@Override
	protected void openContent(int position, String gridItem) {
		String[] key = PicasaCache.decodeTag(gridItem);
		LitePhotoEntry entry;
		try {
			entry = mCache.loadMediaEntry(key[0], key[1]);
		} catch (IOException e) {
//			Log.e(tag, "failed to load entry - " + gridItem);		/*$debug$*/
			return;
		}
		
		if (entry == null) {
//			Log.e(tag, "failed to load entry - " + gridItem);		/*$debug$*/
		} else {
			String mime = entry.mediaGroup.content.type;
			String url = entry.mediaGroup.content.url;
			if(mime.startsWith("video/")){
				// ビデオはとりあえずストリーミングで再生
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse(url), mime);
				startActivity(intent);
			}else{
				Intent intent = new Intent(this, PicasaFullScreenActivity.class);
				intent.putStringArrayListExtra(FullScreenActivity.INTENT_FILE_PATH_LIST, mAdapter.getArrayList());
				intent.putExtra(FullScreenActivity.INTENT_INITIAL_POSITION, position);
				startActivity(intent);
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ApplicationDefine.REQUEST_PREF_SETTING:
			changeGridSize();
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ITEM_SETTING, 0,
				getResources().getString(R.string.menu_setting))
			.setIcon(R.drawable.ic_setting);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_SETTING:
			Intent intent = new Intent();
			intent = new Intent(this, JorllePrefsActivity.class);
			startActivityForResult(intent,
					ApplicationDefine.REQUEST_PREF_SETTING);
			break;
		}

		return true;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		int pos = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
		String tag = (String) mAdapter.getItem(pos);
		
		Intent intent = new Intent(this, PicasaMediaInfoActivity.class);
		intent.putExtra(PicasaMediaInfoActivity.EXTRA_ACCOUNT, mCache.getAccount());
		intent.putExtra(PicasaMediaInfoActivity.EXTRA_TAG, tag);
		menu.add(ContextMenu.NONE, MENU_INFO, ContextMenu.NONE,
				getString(R.string.image_context_info))
			.setIntent(intent);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case MENU_INFO: {
			startActivity(item.getIntent());
			break;
		}
		}
		
		return true;
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_PROGRESS:
			ProgressDialog d = new ProgressDialog(this);
			d.setCancelable(false);
			d.setTitle(R.string.grid_picasa_progress_get_photo);
			return d;
		}
		
		return null;
	}
	@Override
	protected void rescanMedia() {
		GridView grid = (GridView)findViewById(R.id.gridImage);
		ListAdapter adapter = grid.getAdapter();
		grid.setAdapter(null);
		grid.setAdapter(adapter);
	}
	
	@Override
	protected void onTapHomeIcon() {
		Intent intent = new Intent(this, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
}
