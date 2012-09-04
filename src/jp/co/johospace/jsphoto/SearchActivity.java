package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.johospace.jsphoto.accessor.SyncProviderAccessor;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.managed.NavigationGroup;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.util.UxOverlayHelper;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.widget.UXGridDataSource;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget.CornerTapListener;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget.ItemLongPressListener;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget.ItemTapListener;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget.OverlayDataSource;
import jp.co.johospace.jsphoto.ux.widget.UXStage;
import jp.co.johospace.jsphoto.ux.widget.UXUnit;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.ui.SearchResultThumbnailLoader;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

/**
 * 検索チュートリアル画面アクティビティです
 */
public class SearchActivity extends NavigationGroup
										implements OnClickListener
													, TextWatcher, ItemTapListener, ItemLongPressListener {
	public static final String INTENT_TUTORIAL = "intent_tutorial";
	
	/** newGridActivity 検索から遷移時のフラグ */
	public static final String INTENT_NEWGRID_FROM_TUTORIAL = "newgrid_from_tutorial";
	
	public static final int COLUMN_NUMBER_PORT = 2;
	public static final int COLUMN_NUMBER_LAND = 3;

    private final static int REQUEST_CODE_VOICE = 2;       // リクエストコード 2:音声認識画面
	private static final int REQUEST_CODE_INFOLINK_TUTORIAL = 3;
	private static final int REQUEST_CODE_INFOLINK_TUTORIAL_CHAIN = 4;
	private static final int REQUEST_CODE_CONFIRM_SYNC_LOCAK = 5;

    private static final int DIALOG_PROGRESS_SEARCH = 1;

	private final static int POPUP_SEC_SEARCHMSG = 5000;   // 検索メッセージ表示周期[mSec]

	private static final int MENU_ITEM_SETTING = 3;

	protected static final int WIDTH = 200;

	private String mLatestKeyword;
	public class MediaSearchTask extends AsyncTask<Void, Integer, List<Media>> {
	
		String mKeyword;
		private boolean mExceptionHandled;
		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_PROGRESS_SEARCH);
		};
		
		
		@Override
		protected List<Media> doInBackground(Void... params) {
			try {
				mKeyword = getRawKeyword();
				boolean includeLinkage = PreferenceUtil.getBooleanPreferenceValue(SearchActivity.this, ApplicationDefine.KEY_SEARCH_SET_01, true);
				return mAccessor.searchMediaByKeyword(mKeyword, includeLinkage);
			} catch (Exception e) {
				mExceptionHandled = handleException(e, true);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(final List<Media> result) {
			mStage.lockStage(new Runnable() {
				@Override
				public void run() {
					mItems.clear();
					
					if (result != null) {
						mItems.addAll(result);
					}
					
					if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
						mGrid.column(COLUMN_NUMBER_LAND);
					} else {
						mGrid.column(COLUMN_NUMBER_PORT);
					}
				}
			});
			
			mLatestKeyword = mKeyword;
			mStage.invalidate();
			
			if (!mExceptionHandled && (mItems == null || mItems.isEmpty())) {
				Toast.makeText(SearchActivity.this, R.string.search_no_result, Toast.LENGTH_LONG).show();
			}
			
			dismissDialog(DIALOG_PROGRESS_SEARCH);
		}
	}

	private class MultiDataSource implements UXGridDataSource, OverlayDataSource, CornerTapListener {
		@Override
		public Object getInfo(int item) {
			return mItems.get(item);
		}
		
		@Override
		public int getItemCount() {
			return mItems.size();
		}
		
		@Override
		public Object getOverlayInfo(int item, int number) {
			return null;
		}
		
		@Override
		public int getRotation(int item) {
			return 0;
		}

		@Override
		public int getOverlayNumber(int itemPosition, int overlayId) {
			final Media media = mItems.get(itemPosition);
			final String service = media.service;
			if (mOverlayGrid.isTagLayer(overlayId)) {
				// FIXME 次フェーズ以降までふたをする
//				if (ServiceType.JORLLE_LOCAL.equals(service)) {
//					final File file = new File(media.mediaId);
//					if (!TagEditor.hasAllTags(mDatabase, file, getKeywords())) {
//						return mOverlayGrid.getOverlayNumber(media.service, overlayId);
//					}
//				}
				return -1;
			} else {
				if (ServiceType.PICASA_WEB.equals(media.service)) {
					if (SyncProviderAccessor.isSyncedMedia(SearchActivity.this, media.service, media.account, media.mediaId)) {
						return mOverlayGrid.getOverlayNumber(ServiceType.JORLLE_LOCAL, overlayId);
					} else {
						return mOverlayGrid.getOverlayNumber(media.service, overlayId);
					}
				} else if (ServiceType.JORLLE_LOCAL.equals(media.service)) {
					return -1;
				} else {
					return mOverlayGrid.getOverlayNumber(media.service, overlayId);
				}
			}
		}

		@Override
		public void onTap(int itemNumber) {
			// FIXME 次フェーズ以降までふたをする
//			final Media media = mItems.get(itemNumber);
//			final String service = media.service;
//			if (ServiceType.JORLLE_LOCAL.equals(service)) {
//				mHandler.post(new Runnable() {
//					@Override
//					public void run() {
//						File file = new File(media.mediaId);
//						if (TagEditor.insertIfNotExists(mDatabase, file, getKeywords()) > 0) {
//							mStage.invalidate();
//						}
//					}
//				});
//			}
		}
	}
	
	
	private ImageButton mBtnSearch;
	private ImageButton mBtnMic;
	private ImageButton mBtnSetting;
	private EditText mTxtSearch;

	/** 画像一覧格納用レイアウト */
	private LinearLayout mLayoutNewGrid;
	private UXStage mStage;
	private UXGridWidget mGrid;

	/** 検索メッセージポップアップ */
	private PopupWindow mPopSearchMsg;
	/** 検索設定ポップアップ */
	private PopupWindow mPopSearchSet;

	private Timer mTimerSearch;    // 検索メッセージ用Timer
	private Handler mHandler;      // 検索メッセージ用Handler

	private SQLiteDatabase mDatabase;
	private JsMediaServerClient mJsMedia;
//	private ExternalServiceClient mExternal;
	private ExternalServiceCache mCache;
	private CachingAccessor mAccessor;
	private UXThumbnailLoader mLoader;
	private UxOverlayHelper mOverlayGrid;

	private ArrayList<Media> mItems = new ArrayList<Media>();

	private AsyncTask<Void, Integer, List<Media>> mSearchTask;

	private OnEditorActionListener mOnEditorAction = new OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			switch (actionId) {
			case EditorInfo.IME_ACTION_SEARCH:
				exeSearch();
				return true;
			}
			
			return false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.search);

		boolean first = PreferenceUtil.getBooleanPreferenceValue(this, TutorialInfolinkActivity.KEY_NOT_FIRST_SEARCH_TIME, true);
		if (first) {
			startTutorialInfolink(true);
		}
		// インデクシング確認判定
		else if (SyncLocalConfirmActivity.needConfirm(this)) {
			startConfirmSyncLocal();
		}

		// 初期処理
		init();

		// チュートリアルではない場合にキーボードを表示する
		if (!first) {
			showKeyboard();
		}
	}

    @Override
    protected void onResume(){
		super.onResume();

		// 検索メッセージタイマーをセット
    	if (isOkeyTimerSearchMsg()) {

        	// タイマーを開始
            startTimerSearchMsg();

    	}
    	
    	mStage.onResume();
    }

    @Override
    protected void onStop(){
		super.onStop();

    	// 検索メッセージ表示タイマーを停止
		stopTimerSearchMsg();

    }

    @Override
    protected void onPause() {
		super.onPause();

    	// 検索メッセージ表示タイマーを停止
		stopTimerSearchMsg();

		mStage.onPause();
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	stopSearch();
    	
    	mStage.dispose();
    }
    
	@Override
	public void onClick(View view) {

		// 検索ボタン押下
		if (view.getId() == R.id.btnSearch) {

			exeSearch();

			stopTimerSearchMsg();
			
		// マイクボタン押下
		} else if (view.getId() == R.id.btnMic) {

        	// インテント作成
        	Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // ACTION_WEB_SEARCH
        	intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        	intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.search_search_word)); // お好きな文字に変更できます

        	// インテント発行
        	startActivityForResult(intent, REQUEST_CODE_VOICE);

			stopTimerSearchMsg();
        	
		// 設定ボタン押下
		} else if (view.getId() == R.id.btnSetting) {

		    if (mPopSearchSet != null && mPopSearchSet.isShowing()) {
		    	mPopSearchSet.dismiss();
		    } else {
		    	// 検索メッセージが表示されている場合は閉じる
			    if (mPopSearchMsg != null && mPopSearchMsg.isShowing()) {
			    	mPopSearchMsg.dismiss();
			    }
				LinearLayout layoutSet = (LinearLayout) mPopSearchSet.getContentView();
				((CheckBox)layoutSet.findViewById(R.id.chkMenu01)).setChecked(PreferenceUtil.getBooleanPreferenceValue(SearchActivity.this, ApplicationDefine.KEY_SEARCH_SET_01, true));
		    	mPopSearchSet.showAsDropDown(view);
		    }

		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {

	    switch (event.getAction()) {
		    case MotionEvent.ACTION_DOWN:
		        break;
		    case MotionEvent.ACTION_UP:

	        	// タイマーを開始
	            startTimerSearchMsg();

		        break;
		    case MotionEvent.ACTION_MOVE:
		        break;
		    case MotionEvent.ACTION_CANCEL:
		        break;
	    }

		return super.dispatchTouchEvent(event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// アプリ絞り込みPopupWindowが開いている場合、閉じる
			if (mPopSearchSet != null && mPopSearchSet.isShowing()) {
				mPopSearchSet.dismiss();
				return false;
			}

        	// 検索メッセージ表示タイマーを停止
        	stopTimerSearchMsg();

	    	// 検索メッセージが表示されている場合は閉じる
		    if (mPopSearchMsg != null && mPopSearchMsg.isShowing()) {
		    	mPopSearchMsg.dismiss();
		    }

		    finish();
		}
		else if (keyCode == KeyEvent.KEYCODE_MENU) {
			openOptionsMenu();
			return true;
		}
		else {
			return false;
		}

		return true;
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

    	if (requestCode == REQUEST_CODE_VOICE && resultCode == RESULT_OK){
    		// 音声認識画面

    		// 結果文字列リスト
    		ArrayList<String> results = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

    		// 文字を追加
    		if (results.size() > 0) {

    			((EditText)this.findViewById(R.id.txtSearch)).setText(results.get(0));
    			
    			exeSearch();
    		}

    	}
    	// チュートリアルからの戻り
    	else if (requestCode == REQUEST_CODE_INFOLINK_TUTORIAL || requestCode == REQUEST_CODE_INFOLINK_TUTORIAL_CHAIN) {
    		if (requestCode == REQUEST_CODE_INFOLINK_TUTORIAL_CHAIN) {
    			if (!startConfirmSyncLocal()) {
    				showKeyboard();
    			}
    		}
    	}
    	// ローカルファイルの同期確認からの戻り
    	else if (requestCode == REQUEST_CODE_CONFIRM_SYNC_LOCAK) {
    		showKeyboard();
    	}
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		final int columns;
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			columns = COLUMN_NUMBER_LAND;
		} else {
			columns = COLUMN_NUMBER_PORT;
		}

		mStage.lockStage(new Runnable() {
			@Override
			public void run() {
				mGrid.column(columns);
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
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS_SEARCH:
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(getString(R.string.search_message_progress));
			dialog.setCancelable(true);
			dialog.setButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			dialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					stopSearch();
				}
			});
			dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					removeDialog(DIALOG_PROGRESS_SEARCH);
				}
			});
			return dialog;
		default:
			return super.onCreateDialog(id);
		}
	}
	
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,int after) {
		// TextWatcherからbeforeTextChangedイベントを取得

		// 何もしない
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TextWatcherからonTextChangedイベントを取得

		// 何もしない

	}

	@Override
	public void afterTextChanged(Editable s) {
		// TextWatcherからafterTextChangedイベントを取得

    	// 検索メッセージタイマーをセット
    	if (isOkeyTimerSearchMsg()){

        	// タイマーを開始
            startTimerSearchMsg();

    	}

	}

	@Override
	public void onLongPress(int itemNumber) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onTap(final int itemNumber) {
		final Media media = mItems.get(itemNumber);
		new AsyncTask<Void, Void, Intent>() {
			@Override
			protected Intent doInBackground(Void... params) {
				String mime = MediaUtil.getMimeTypeFromPath(media.fileName);
				if ((mime == null || mime.startsWith("video/"))
						&& !ServiceType.JORLLE_LOCAL.equals(media.service)) {
					try {
						ExternalServiceClient client =
								ClientManager.getExternalServiceClient(SearchActivity.this, media.service);
						CachingAccessor accessor =
								new CachingAccessor(SearchActivity.this, mJsMedia, client, mCache, false);
						mime = accessor.getMediaContentType(media);
					} catch (IOException e) {
//						runOnUiThread(new Runnable() {
//							@Override
//							public void run() {
//								Toast.makeText(SearchActivity.this,
//										R.string.error_failed_to_connect, Toast.LENGTH_LONG).show();
//							}
//						});
						handleException(e, true);
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
								ClientManager.getExternalServiceClient(SearchActivity.this, media.service);
						Pair<String, String> content = client.getContentsUrl(media, mime);
						uri = Uri.parse(content.first);
						mime = content.second;
					}
					
					intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(uri, mime);
				} else {
					intent = new Intent(SearchActivity.this, SearchResultFullScreenActivity.class);
					intent.putExtra(SearchResultFullScreenActivity.EXTRA_TYPE, SearchResultFullScreenActivity.TYPE_SEARCH);
					intent.putExtra(SearchResultFullScreenActivity.EXTRA_DATA, String.valueOf(itemNumber));
					intent.putExtra(SearchResultFullScreenActivity.EXTRA_CATEGORY_NAME, mLatestKeyword);
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
	
	/**
	 * 初期処理
	 *
	 */
	private void init() {
		mDatabase = OpenHelper.external.getDatabase();
		
		mJsMedia = ClientManager.getJsMediaServerClient(this);
//		external = ClientManager.getExternalServiceClient(this, "");
		mCache = new ExternalServiceCacheImpl(this);
		mAccessor = new CachingAccessor(this, mJsMedia, null/*external*/, mCache, true);
		mLoader = new SearchResultThumbnailLoader(this);

		// 検索設定ポップアップを初期化
		initPopSearchSet();

		mStage = new UXStage(this, UXStage.GL_RENDERER);
		mStage.addThread(ApplicationDefine.NUM_NETWORK_THREAD);
		mStage.setBackgroundColor(Color.rgb(20, 20, 20));
		mStage.setScrollbarResource(R.drawable.slider, 28, 65);
//		mStage.setScrollListener(this);
		
		final int columns;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			columns = COLUMN_NUMBER_LAND;
		} else {
			columns = COLUMN_NUMBER_PORT;
		}
		//Stageに変更を加える場合、必ずlockStageを経由
		//
		//Stageに変更とは、Stageにぶら下がっているすべてのクラスの変更のこと。
		//たとえばDataSourceの中身に変更を加える場合でも必要となる。
		//
		mStage.lockStage(new Runnable() {
			@Override
			public void run() {
				MultiDataSource dataSource = new MultiDataSource();
				mOverlayGrid = new UxOverlayHelper(dataSource);
				mOverlayGrid.init(SearchActivity.this);
				
				mGrid = new UXGridWidget(WIDTH, mLoader);
				mGrid	
					.dataSource(dataSource)
					.padding(5, UXUnit.DP)
					.itemType(mOverlayGrid)
					.column(columns)
					.addTo(mStage);
				
				mGrid.setOnItemTapListener(SearchActivity.this);
				mGrid.setOnItemLongPressListener(SearchActivity.this);
				mGrid.setOnCornerTapListener(dataSource, 25, UXUnit.DP);
			}
		});
		
		//最後にViewを追加
		View v = mStage.getView();
		v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		((ViewGroup)findViewById(R.id.layContent)).addView(v);
	}

	/**
	 * 検索設定ポップアップを初期化
	 */
	private void initPopSearchSet(){

    	// ------------------------------------------------------ //
    	// --- 検索に使用するオブジェクトを準備
    	// ------------------------------------------------------ //
		mBtnSearch = (ImageButton)this.findViewById(R.id.btnSearch);
		mBtnSearch.setOnClickListener(this);

		mBtnMic = (ImageButton)this.findViewById(R.id.btnMic);
		mBtnMic.setOnClickListener(this);

		mBtnSetting = (ImageButton)this.findViewById(R.id.btnSetting);
		mBtnSetting.setOnClickListener(this);

		mTxtSearch = (EditText)this.findViewById(R.id.txtSearch);
		mTxtSearch.addTextChangedListener(this);
		mTxtSearch.setOnEditorActionListener(mOnEditorAction );

    	// ------------------------------------------------------ //
    	// --- 検索メッセージを作成
    	// ------------------------------------------------------ //
		LinearLayout layoutMsg = (LinearLayout) getLayoutInflater().inflate(R.layout.smart_search_msg, null);
		layoutMsg.setClickable(true);
		layoutMsg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startTutorialInfolink(false);
			}
		});

		// ポップアップにセット
	    mPopSearchMsg = new PopupWindow((View)findViewById(R.id.txtSearch));
	    mPopSearchMsg.setContentView(layoutMsg);
	    mPopSearchMsg.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
	    mPopSearchMsg.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

	    // ポップアップウィンドウ領域外タップ時は閉じる
	    mPopSearchMsg.setBackgroundDrawable(new BitmapDrawable());
	    mPopSearchMsg.setOutsideTouchable(true);

    	// ------------------------------------------------------ //
    	// --- 検索設定ポップアップを作成
    	// ------------------------------------------------------ //
		LinearLayout layoutSet = (LinearLayout) getLayoutInflater().inflate(R.layout.smart_search_setting, null);

		// 値をセット
		((CheckBox)layoutSet.findViewById(R.id.chkMenu01)).setText(getResources().getText(R.string.tutorial_message_search_set01));
		((Button)layoutSet.findViewById(R.id.btnOk)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// プリファレンスをセット
				LinearLayout layoutSet = (LinearLayout) mPopSearchSet.getContentView();
				PreferenceUtil.setBooleanPreferenceValue(SearchActivity.this, ApplicationDefine.KEY_SEARCH_SET_01, ((CheckBox)layoutSet.findViewById(R.id.chkMenu01)).isChecked());
				mPopSearchSet.dismiss();

	        	// タイマーを開始
	            startTimerSearchMsg();
			}
		});
		((Button)layoutSet.findViewById(R.id.btnClose)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPopSearchSet.dismiss();

	        	// タイマーを開始
	            startTimerSearchMsg();
			}
		});

		// ポップアップにセット
	    mPopSearchSet = new PopupWindow((View)findViewById(R.id.btnSearch));

	    mPopSearchSet.setContentView(layoutSet);
	    mPopSearchSet.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
	    mPopSearchSet.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

	    // ポップアップウィンドウ領域外タップ時は閉じる
	    mPopSearchSet.setBackgroundDrawable(new BitmapDrawable());
	    mPopSearchSet.setOutsideTouchable(true);

    	// ------------------------------------------------------ //
    	// --- 検索メッセージタイマーを作成
    	// ------------------------------------------------------ //
    	// 検索メッセージタイマーを開始
        startTimerSearchMsg();

	}

	/**
	 * 検索メッセージタイマーが動作してよいかを判定
	 */
	private boolean isOkeyTimerSearchMsg(){

		// 検索テキストにフォーカスがなければ false
        if (getCurrentFocus() != null && getCurrentFocus().getId() != R.id.txtSearch) {
        	return false;
        }

		// 検索テキストに入力があれば false
        if (!mTxtSearch.getText().toString().trim().equals("")) {
        	return false;
        }

		// 検索メッセージポップアップが表示されていれば false
        if (mPopSearchMsg != null && mPopSearchMsg.isShowing() == true){
        	return false;
        }

		// 検索設定ポップアップが表示されていれば false
        if (mPopSearchSet != null && mPopSearchSet.isShowing() == true){
        	return false;
        }

        // 正常
		return true;
	}

	/**
	 * TODO
	 * @return
	 */
	private String getRawKeyword() {
		return mTxtSearch.getText().toString().trim();
	}
	/**
	 * 検索キーワードをスペースで区切った配列として取得します
	 * @return
	 */
	private String[] getKeywords() {
		return getRawKeyword().split(" ");
	}

	/**
	 * 検索を行い、結果を表示します
	 */
	private void exeSearch() {
		if (mSearchTask != null && mSearchTask.getStatus() == Status.RUNNING)
			return;
		
		if (TextUtils.isEmpty(getRawKeyword())) {
//			final String TAG = "SearchActivity";		/*$debug$*/
//			Log.d(TAG, "Since there is no keyword does not perform a search");		/*$debug$*/
			return;
		}
		
		mSearchTask = new MediaSearchTask().execute();
	}

	private void stopSearch() {
		if (mSearchTask != null && mSearchTask.getStatus() == Status.RUNNING) {
			mSearchTask.cancel(false);
			mSearchTask = null;
		}
	}
	
	/**
	 * 検索メッセージ表示タイマーを開始
	 */
	private void startTimerSearchMsg(){
	
		// 検索メッセージ表示タイマーを停止
		stopTimerSearchMsg();
	
	    // タイマーを生成
	    mTimerSearch = new Timer(false);
	    mHandler = new Handler();
	
	    // スケジュールタスクをセット
	    mTimerSearch.schedule(new TimerTask() {
	        public void run() {
	
	            // Handlerに、UIスレッドへの値設定処理をPOSTする。
	            mHandler.post(new Runnable() {
	                public void run() {
	                    if (isOkeyTimerSearchMsg()){
	
	                    	// 検索メッセージをポップアップで表示
	                    	mPopSearchMsg.showAsDropDown(mTxtSearch);
	
	                    }
	
	                	// 検索メッセージ表示タイマーを停止
	                	stopTimerSearchMsg();
	
	                }
	            });
	
	        }
	
	    }, POPUP_SEC_SEARCHMSG, POPUP_SEC_SEARCHMSG); // 初回起動の遅延 と 周期
	
	}

	/**
	 * 検索メッセージ表示タイマーを停止
	 */
	private void stopTimerSearchMsg(){
		// Timerクラスを破棄
		if (mTimerSearch != null){
			mTimerSearch.cancel(); // タイマー停止
			mTimerSearch = null;
		}
		// Handlerクラスを破棄
		if (mHandler != null){
			mHandler = null;
		}
	}
	
	/**
	 * 設定画面を表示します
	 */
	private void startSettingActivity() {
		Intent settingIntent = new Intent(this, JorllePrefsActivity.class);
		startActivity(settingIntent);
	}

	/**
	 * インフォリンクのチュートリアルを開始します
	 * @param chain 
	 */
	private void startTutorialInfolink(boolean chain) {
		Intent searchIntent = new Intent(this, TutorialInfolinkActivity.class);
		if (chain) {
			startActivityForResult(searchIntent, REQUEST_CODE_INFOLINK_TUTORIAL_CHAIN);
		} else {
			startActivityForResult(searchIntent, REQUEST_CODE_INFOLINK_TUTORIAL);
		}
	}

	/**
	 * ローカルのファイルをインデックス化するかどうかを設定する画面を開始します
	 */
	private boolean startConfirmSyncLocal() {
		if (SyncLocalConfirmActivity.needConfirm(this)) {
			Intent intent = new Intent(this, SyncLocalConfirmActivity.class);
			intent.putExtra(SyncLocalConfirmActivity.EXTRA_MESSAGE, getString(R.string.sync_local_confirm_message_search));
			startActivityForResult(intent, REQUEST_CODE_CONFIRM_SYNC_LOCAK);
			
			return true;
		} else {
			return false;
		}
	}

    /**
     * 検索キーワード入力用にキーボードを表示します
     */
    private void showKeyboard() {
    	final InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//		mHandler.post(new Runnable(){
//			@Override
//			public void run() {
//				manager.hideSoftInputFromWindow(mTxtSearch.getWindowToken(), 0);
//			}
//		});
    	// TODO こんなことをしたいんじゃないんです
    	mTxtSearch.postDelayed(new Runnable() {
			@Override
			public void run() {
				manager.showSoftInput(mTxtSearch, 0);
//				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			}
		}, 500);
	}

}
