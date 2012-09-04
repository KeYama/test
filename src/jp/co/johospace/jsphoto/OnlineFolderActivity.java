package jp.co.johospace.jsphoto;

import java.io.IOException;
import java.util.List;

import jp.co.johospace.jsphoto.folder.AsyncListAdapter.ViewHolder;
import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.util.HeaderController;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import jp.co.johospace.jsphoto.ux.view.UXViewLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import jp.co.johospace.jsphoto.v2.onlineservice.ui.OnlineFolderAdapter;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * オンラインフォルダ一覧
 */
public class OnlineFolderActivity extends NavigatableActivity implements OnItemClickListener {

	public static final String EXTRA_SERVICE_TYPE =
			OnlineFolderActivity.class.getName() + ".EXTRA_SERVICE_TYPE";
	public static final String EXTRA_SERVICE_ACCOUNT =
			OnlineFolderActivity.class.getName() + ".EXTRA_SERVICE_ACCOUNT";
	
	private static final int MENU_ITEM_NEW_FOLDER = 1;
	private static final int MENU_ITEM_SORT = 2;
	private static final int MENU_ITEM_SEARCH = 3;
	private static final int MENU_ITEM_SETTING = 4;
	
	private ListView mList;
	private View mHeader;
	private Button mBtnUpdateCache;

	private CachingAccessor mClient;
	private HeaderController mHeaderController;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		System.out.println("++++++++++++++++++++ onCreate " + getClass().getSimpleName());		/*$debug$*/
		
		setContentView(R.layout.online_folder);
		
		String service = getIntent().getStringExtra(EXTRA_SERVICE_TYPE);
		String account = getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT);
		if (TextUtils.isEmpty(service) || TextUtils.isEmpty(account)) {
			throw new IllegalArgumentException("service type is required.");
		}
		
		JsMediaServerClient jsMedia = ClientManager.getJsMediaServerClient(this);
		ExternalServiceClient external = ClientManager.getExternalServiceClient(this, service);
		ExternalServiceCache cache = new ExternalServiceCacheImpl(this);
		mClient = new CachingAccessor(this, jsMedia, external, cache, true);
		
		mList = (ListView) findViewById(R.id.list);
		mList.setOnItemClickListener(this);
		mList.setRecyclerListener(new AbsListView.RecyclerListener() {
			@Override
			public void onMovedToScrapHeap(View view) {
				ViewHolder holder = ((ViewHolder)view.getTag());
				for(int i = 0; i < holder.layout.getChildCount(); i++) {
					((UXAsyncImageView)holder.layout.getChildAt(i)).recycle();
				}
			}
		});
		
		// ヘッダ関連の準備
		FrameLayout parent = (FrameLayout) findViewById(R.id.frmHeader);
		mHeaderController = new HeaderController(this, R.layout.ol_folder_info, parent);
		mHeaderController.setHeaderEventTo(mList);
		mHeader = mHeaderController.getView();
		mBtnUpdateCache = (Button) mHeader.findViewById(R.id.btnRefresh);
		mBtnUpdateCache.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mUpdateCacheTask != null) {
					// 更新中
					mUpdateCacheTask.cancel(true);
				} else {
					mUpdateCacheTask = new UpdateCacheTask();
					mUpdateCacheTask.execute();
				}
			}
		});
		mHeader.setVisibility(View.INVISIBLE);
		
		setHeaderInfo();

		mHeaderController.show(HeaderController.LENGTH_DEFAULT);
		
		new LoadDirectoryTask().execute();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		OnlineFolderAdapter adapter =
				(OnlineFolderAdapter) mList.getAdapter();
		Directory dir = (Directory) adapter.getItem(position);
		
		Intent intent = new Intent(this, OnlineGridActivity.class);
		intent.putExtra(OnlineGridActivity.EXTRA_SERVICE_TYPE,
				getIntent().getStringExtra(EXTRA_SERVICE_TYPE));
		intent.putExtra(OnlineGridActivity.EXTRA_SERVICE_ACCOUNT,
				getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT));
		intent.putExtra(OnlineGridActivity.EXTRA_SERVICE_DIRID, dir.id);
		intent.putExtra(OnlineGridActivity.EXTRA_SERVICE_DIRNAME, dir.name);
		
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		goNextHistory(intent);
	}
	
	private class LoadDirectoryTask extends AsyncTask<Void, Void, List<Directory>> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();// TODO $ol
		}
		
		@Override
		protected List<Directory> doInBackground(Void... params) {
			try {
				return mClient.getDirectories(
						getIntent().getStringExtra(EXTRA_SERVICE_TYPE),
						getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT), false, 10, false);
			} catch (IOException e) {
				handleException(e, true);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<Directory> result) {
			if (result != null) {
				if (result.isEmpty()) {
					// データがない場合は前画面に戻る
					Toast.makeText(OnlineFolderActivity.this,
							getString(R.string.image_message_no_media), Toast.LENGTH_LONG).show();
					onBackRefleshHistory();
				} else {
					OnlineFolderAdapter adapter =
							new OnlineFolderAdapter(getApplicationContext(), mClient, result);
					mList.setAdapter(adapter);
					setHeaderInfo();
				}
			} else {
				// TODO $ol
			}
		}
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
				try {
					mClient.requestIndexing(
							getIntent().getStringExtra(EXTRA_SERVICE_TYPE),
							getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT), false);
				} catch (IOException e) {
					; // インデクシング要求のタイムアウトはいつまでも待たない
				}
				if (isCancelled()) {
					return null;
				}
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
						new LoadDirectoryTask().execute();
						
						setHeaderInfo();
					}
				} else {
					// TODO $ol
				}
			} finally {
				mBtnUpdateCache.setBackgroundResource(R.drawable.button_refresh);
				mUpdateCacheTask = null;
				
				mHeaderController.setDisplayKeep(false);
				mHeaderController.show(HeaderController.LENGTH_DEFAULT);
			}
		}
		
		@Override
		protected void onCancelled() {
			try {
			} finally {
				mBtnUpdateCache.setBackgroundResource(R.drawable.button_refresh);
				mUpdateCacheTask = null;
				
				mHeaderController.setDisplayKeep(false);
				mHeaderController.show(HeaderController.LENGTH_DEFAULT);
			}
		}
	}
	
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ListAdapter adapter = mList.getAdapter();
		mList.setAdapter(null);
		mList.setAdapter(adapter);
	}

	private void setHeaderInfo() {
		TextView txtName = (TextView) mHeader.findViewById(R.id.txt_name);
		String serviceName = ClientManager.getServiceName(
				getApplicationContext(), getIntent().getStringExtra(EXTRA_SERVICE_TYPE));
		txtName.setText(serviceName);
		
		TextView txtUp = (TextView) mHeader.findViewById(R.id.txtLastUpdated);
		final Long lastUpdatedObj = 
				mClient.getCache().getLastUpdated(
						getIntent().getStringExtra(EXTRA_SERVICE_TYPE),
						getIntent().getStringExtra(EXTRA_SERVICE_ACCOUNT));
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBackRefleshHistory();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ITEM_SEARCH, 0, getResources().getString(R.string.menu_search)).setIcon(R.drawable.ic_search);
		menu.add(0, MENU_ITEM_SETTING, 0, getResources().getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			// 検索
			case MENU_ITEM_SEARCH:

				Intent intent = new Intent(OnlineFolderActivity.this, SearchActivity.class);
				startActivity(intent);
				break;
			// 設定
			case MENU_ITEM_SETTING:
				Intent settingIntent = new Intent(OnlineFolderActivity.this, JorllePrefsActivity.class);
				startActivity(settingIntent);
				break;

		}

		return true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		UXViewLoader.dispose();
		if(mList != null){
			OnlineFolderAdapter adapter = (OnlineFolderAdapter) mList.getAdapter();
			if(adapter != null)
				adapter.dispose();
		}
	}
}
