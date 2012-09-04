package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.DetailDialog;
import jp.co.johospace.jsphoto.fullscreen.ImageSurfaceView;
import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoader;
import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoaderFactory;
import jp.co.johospace.jsphoto.util.SizeConv;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import jp.co.johospace.jsphoto.ux.view.UXViewLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.RelatedMedia;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Schedule;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.SnsContent;
import jp.co.johospace.jsphoto.v2.onlineservice.ui.SearchResultImageLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.ui.SearchResultThumbnailLoader;
import jp.co.johospace.jsphoto.view.ProgressPopupWindow;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * フルスクリーン基底
 */
public abstract class AbstractFullscreenActivity extends AbstractActivity implements OnClickListener, OnItemClickListener {

	public static final String EXTRA_CATEGORY_NAME =
			AbstractFullscreenActivity.class.getName().concat(".EXTRA_CATEGORY_NAME");
	
	public static final int TIME_TO_HIDE = 3000;
	
	protected static final int TBS_DEFAULT = 0;
	protected static final int TBS_ILNOTFOUND = 1;
	protected static final int TBS_ILFINDED = 2;
	
	public static final int MAX_NAME_P = 15;
	public static final int MAX_NAME_L = 30;
	protected boolean mShowTop;
	
	protected RelatedMedia mInfoLinks;
	protected InfoLinkAdapter mInfoLinkAdapter;
	
	protected LoadInfoLinkTask mLoadInfoLinkTask;
	
	protected Handler mHandler = new Handler();
	protected ImageSurfaceView mSurfaceView;

	protected SearchResultThumbnailLoader mThumbLoader;
	protected UXViewLoader mLoader;
	
	protected JsMediaServerClient mClient;
	
	/** ウィンドウマネージャ */
	protected WindowManager mWindowManager; 
	
	/** ディスプレイ */
	protected Display mDisplay; 
	
	/** 画像サイズ */
	protected Integer mSize;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mThumbLoader = new SearchResultThumbnailLoader(this, true);
		mLoader = new UXViewLoader(mThumbLoader);
		mClient = ClientManager.getJsMediaServerClient(this);
	}
	
	private ProgressPopupWindow mProgress;
	/**
	 * InfoLinkの読み込みを行うタスク
	 */
	protected class LoadInfoLinkTask extends AsyncTask<Void, Void, RelatedMedia> {
//		private List<String> mResult = new ArrayList<String>();
		
		@Override
		protected void onPreExecute() {
			// ポップアップを作成し、表示する
			mProgress = new ProgressPopupWindow(AbstractFullscreenActivity.this);
			mProgress.showProgress();
			mProgress.setMessage(getString(R.string.fullscreen_message_infolink_loading));
			
			View anchor = findViewById(R.id.category_name);
			mProgress.showAtLocation(anchor, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 0);
		}
		
		@Override
		protected RelatedMedia doInBackground(Void... params) {
			
			try {
				MediaIdentifier mediaIdr = getCurrentMedia();
				if (mediaIdr != null) {
					RelatedMedia rm = mClient.searchRelatedMedia(mediaIdr.serviceType, mediaIdr.serviceAccount, mediaIdr.mediaId);
					if (rm == null || rm.schedules == null)
						return rm;

					// 予定は先頭の３件
					for (; rm.schedules.size() > ApplicationDefine.INFOLINK_MAX_SCHEDULES; )
						rm.schedules.remove(ApplicationDefine.INFOLINK_MAX_SCHEDULES);
					
					return rm;
				} else {
					return new RelatedMedia();
				}
			} catch (IOException e) {
				handleException(e, true);
			} finally {
			}
			return null;
		}

		@Override
		protected void onCancelled() {
			// ポップアップを閉じる
			if (mProgress.isShowing()) {
				mProgress.dismiss();
			}
			
			mInfoLinks = null;
			mInfoLinkAdapter.notifyDataSetChanged();
			mRelatedImages.removeAllViews();
			mRelatedImages.setVisibility(View.GONE);
		}
		
		@Override
		protected void onPostExecute(RelatedMedia result) {
			if (isCancelled()) {
				return;
			}

			// 取得したデータをリストに表示する
			mInfoLinks = result;
			mInfoLinkAdapter.notifyDataSetChanged();
			mRelatedImages.removeAllViews();
			mRelatedImages.setVisibility(View.GONE);
			if (mInfoLinks != null && !mInfoLinks.isEmpty()) {
				// ポップアップを閉じる
				if (mProgress.isShowing()) {
					mProgress.dismiss();
				}
				if (mInfoLinks.medias != null) {
					for (Media media : mInfoLinks.medias) {
						mRelatedImages.addView(createImageView(media));
						if (mRelatedImages.getVisibility() != View.VISIBLE) {
							mRelatedImages.setVisibility(View.VISIBLE);
						}
					}
				}
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						// 内容がかわるかもしれないので一度閉じてから、開く
						hideTopbar();
						showTopbar();
					}
				});
			} else {
				hideTopbar();
				mProgress.hideProgress();
				mProgress.setMessage(getString(R.string.fullscreen_message_infolink_nocontents));
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						// ポップアップを閉じる
						if (mProgress.isShowing()) {
							mProgress.dismiss();
						}
						// 内容がかわるかもしれないので一度閉じてから、開く
						showTopbar();
					}
				}, 3000L);
			}
		}
	}
	
	protected class InfoLinkAdapter extends BaseAdapter {
		private LayoutInflater mInflater = getLayoutInflater();
		
		@Override
		public int getCount() {
			if (mInfoLinks == null) {
				return 0;
			} else {
				return (mInfoLinks.schedules == null ? 0 : mInfoLinks.schedules.size())
						+ (mInfoLinks.snsContents == null ? 0 : 1);
			}
		}
		@Override
		public Object getItem(int pos) {
			if (mInfoLinks == null) {
				return null;
			} else {
				if (mInfoLinks.schedules != null && pos < mInfoLinks.schedules.size()) {
					return mInfoLinks.schedules.get(pos);
				} else {
					return mInfoLinks.snsContents;
				}
			}
		}
		@Override
		public long getItemId(int pos) {
			if (mInfoLinks == null) {
				return -1;
			} else {
				return pos;
			}
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			Object item = getItem(position);

			if (v != null && v.getTag().getClass() != item.getClass()) {
				v = null;
			}

			if (item instanceof Schedule) {
				Schedule schedule = (Schedule) item;
				if (v == null) {
					v = mInflater.inflate(R.layout.fullscreen_item_event, parent, false);
				}
				// サービスのアイコン
				ImageView img = (ImageView) v.findViewById(R.id.icon);
				img.setImageResource(ClientManager.getIconResource(AbstractFullscreenActivity.this, schedule.service));
				// 日付
				TextView txtDate = (TextView) v.findViewById(R.id.txtDate);
				SimpleDateFormat dfDate = new SimpleDateFormat(getString(R.string.fullscreen_format_event_date));
				txtDate.setText(dfDate.format(new Date(schedule.dtstart)));

				Time st = new Time();
				st.set(schedule.dtstart);
				Time en = new Time();
				en.set(schedule.dtend);
				TextView txtTime = (TextView) v.findViewById(R.id.txtTime);
				if(st.hour==0 && st.minute==0 && en.hour==23 && en.minute==59) {
					// 時間を表示しない
					txtTime.setVisibility(View.GONE);
				} else {
					txtTime.setVisibility(View.VISIBLE);
					// 時間（イベントの場合は 開始-終了)
					SimpleDateFormat dfTime = new SimpleDateFormat("H:mm");
					txtTime.setText(String.format("%s-%s",
							dfTime.format(new Date(schedule.dtstart)), dfTime.format(new Date(schedule.dtend))));
				}
				
				// 本文
				TextView txt = (TextView) v.findViewById(R.id.text1);
				txt.setText(schedule.title);
			} else {
				SnsContent sns = (SnsContent) item;
				if (v == null) {
					v = mInflater.inflate(R.layout.fullscreen_item_message, parent, false);
				}
				// サービスのアイコン
				ImageView img = (ImageView) v.findViewById(R.id.icon);
				img.setImageResource(ClientManager.getIconResource(AbstractFullscreenActivity.this, sns.service));
//				// 日付
//				TextView txtDate = (TextView) v.findViewById(R.id.txtDate);
//				SimpleDateFormat dfDate = new SimpleDateFormat("yyyy/MM/dd");
//				txtDate.setText(dfDate.format(new Date(sns.dtposted)));
//				// 時間（イベントの場合は 開始-終了)
//				TextView txtTime = (TextView) v.findViewById(R.id.txtTime);
//				SimpleDateFormat dfTime = new SimpleDateFormat("HH:mm");
//				txtTime.setText(dfTime.format(new Date(sns.dtposted)));
				// 本文
				TextView txt = (TextView) v.findViewById(R.id.text1);
				txt.setText(sns.postedcontent);
			}
			
			v.setTag(item);
			return v;
		}
	}
	
	@Override
	public void onClick(View v) {
		// InfoLinkボタン
		if (v.getId() == R.id.btnInfoLink) {
			showInfolink();
		}
		// 閉じるボタン
		else if (v.getId() == R.id.btnClose) {
			closeInfolink();
		}
		// InfoLinkとは
		else if (v.getId() == R.id.btnHowtoInfoLink) {
			startTutorialInfoActivity();
		}
		
	}
	
	protected void cancelInfolink() {
		if (mLoadInfoLinkTask != null && mLoadInfoLinkTask.getStatus() == Status.RUNNING) {
			mLoadInfoLinkTask.cancel(true);
			mLoadInfoLinkTask = null;
		}
	}
	
	protected void closeInfolink() {
		
		closeOnlyInfolink();

		showTopbar();
		
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				hideTopbar();
			}
		}, TIME_TO_HIDE);

	}

	protected void closeOnlyInfolink() {
		cancelInfolink();
		mInfoLinks = null;
		mInfoLinkAdapter.notifyDataSetChanged();
		mRelatedImages.removeAllViews();
		mRelatedImages.setVisibility(View.GONE);
		
		removeRelatedSurface();
	}

	protected void showInfolink() {
		cancelInfolink();
		mLoadInfoLinkTask = new LoadInfoLinkTask();
		mLoadInfoLinkTask.execute();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		// TODO ジョルテとか起動
		String service;
		Intent intent = null;
		if (v.getTag() instanceof Schedule) {
			service = ((Schedule) v.getTag()).service;
			// TODO ジョルテの対応
			//Toast.makeText(this, "ジョルテ起動に必要な情報を取得します", Toast.LENGTH_SHORT).show();
			Schedule sche = (Schedule) v.getTag();
			DetailDialog dialog = new DetailDialog(this, sche);
			dialog.show();
		} else if (v.getTag() instanceof SnsContent) {
			service = ((SnsContent) v.getTag()).service;
			MediaIdentifier mediaIdr = getCurrentMedia();
			intent = ClientManager.getShareIntent(this, service, mediaIdr.serviceAccount, mediaIdr.mediaId);
		} else {
			return;
		}

		if (intent == null) {
			return;
		}
		startActivity(intent);
	}
	
	protected LinearLayout mRelatedImages;
	protected View mRelatedImagesContainer;
	protected FrameLayout mContainer;
	protected void prepareInfoLinkOnCreate() {
		findViewById(R.id.btnInfoLink).setOnClickListener(this);
		findViewById(R.id.btnClose).setOnClickListener(this);
		findViewById(R.id.btnHowtoInfoLink).setOnClickListener(this);
		
		mInfoLinkAdapter = new InfoLinkAdapter();
		ListView lv = ((ListView)findViewById(R.id.lstInfolink));
		lv.setAdapter(mInfoLinkAdapter);
		lv.setOnItemClickListener(this);
		
		mRelatedImages = (LinearLayout) findViewById(R.id.related_images);
		mRelatedImagesContainer = findViewById(R.id.related_images_container);
		
		mContainer = (FrameLayout) findViewById(R.id.flFullscreen);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		cancelInfolink();
	}
	
	protected void hideTopbar(){
		if (mInfoLinks == null) {
			mShowTop = false;
			findViewById(R.id.lytListHeader).setVisibility(View.GONE);
			findViewById(R.id.related_images_container).setVisibility(View.GONE);
			// InfoLinkを検索していない場合か、InfoLinkを検索したが、見つからなかった場合はtrue
			mSurfaceView.setShowEnabled(mInfoLinks==null || mInfoLinks.isEmpty());		
		}
	}
	
	protected void showTopbar(){
		if(mSurfaceView == null)return;

		// 表示するボタンを切り替える
		int state;
		if (mInfoLinks==null) {
			state = 0;
		} else if (!mInfoLinks.isEmpty()) {
			state = 1;
		} else {
			state = 2;
			mInfoLinks = null;
		}
		findViewById(R.id.btnInfoLink).setVisibility(		        state == 0 ? View.VISIBLE : View.GONE);
		findViewById(R.id.btnClose).setVisibility(			        state == 1 ? View.VISIBLE : View.GONE);
		findViewById(R.id.related_images_container).setVisibility(	state == 1 ? View.VISIBLE : View.GONE);
		findViewById(R.id.btnHowtoInfoLink).setVisibility(	        state == 2 ? View.VISIBLE : View.GONE);
		

		mShowTop = true;
//		String name = getCurrentName();
//		int maxName = 0;
//		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
//			maxName = MAX_NAME_L;
//		}else{
//			maxName = MAX_NAME_P;
//		}
//		if(name.length() > maxName){
//			name = name.substring(0, maxName - 3) + "...";
//		}
//		((TextView)findViewById(R.id.tvHeader)).setText(name);
		TextView txtCategoryName = (TextView) findViewById(R.id.category_name);
		String categoryName = getCategoryName();
		txtCategoryName.setText(TextUtils.isEmpty(categoryName) ? " " : categoryName);
		TextView txtProductionDate = (TextView) findViewById(R.id.production_date);
		Long date = getCurrentDate();
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		txtProductionDate.setText(date == null ? " " : df.format(new Date(date)));
		findViewById(R.id.lytListHeader).setVisibility(View.VISIBLE);
		mSurfaceView.setShowEnabled(false);		
	}
	
	protected void startInfoLinkNavigation() {
		mSurfaceView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mShowTop){
					if (mProgress == null || !mProgress.isShowing()) {
						hideTopbar();
						mHandler.removeCallbacksAndMessages(null);
					}
				}else{
					showTopbar();
					mHandler.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							hideTopbar();
						}
					}, TIME_TO_HIDE);
				}
			}
		});

		mSurfaceView.setOnMoveListener(new ImageSurfaceView.OnMoveListener() {
			@Override
			public void onMove(MotionEvent event) {
				if(mRelatedImages.getVisibility()!=View.GONE) {
					closeOnlyInfolink();
				}
			}
		});
				
		mSurfaceView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if(AbstractFullscreenActivity.this instanceof OnlineFullScreenActivity) return false;
				if(AbstractFullscreenActivity.this instanceof SearchResultFullScreenActivity) return false;
				// バイブレータ  
				try {
					int HapticFeedbackEnabled = android.provider.Settings.System.getInt(AbstractFullscreenActivity.this.getContentResolver(), android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 0);
					if(HapticFeedbackEnabled==1) {
						Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
						vibrator.vibrate(10);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				openOptionsMenu();
				return true;
			}
		});
		
		
		// 画面遷移時、最初にトップバーを表示
		showTopbar();
		mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				hideTopbar();
			}
		}, TIME_TO_HIDE);
	}
	
	protected abstract Long getCurrentDate();
	
	protected boolean removeRelatedSurface() {
		if (mRelatedSurface != null) {
			mContainer.removeView(mRelatedSurface);
			mRelatedSurface.dispose();
			mRelatedSurface = null;
			mContainer.addView(mSavedSurface);
			return true;
		}
		
		return false;
	}
	
	protected void onBackKey() {
		if (removeRelatedSurface()) {
			return;
		}

		if (mLoadInfoLinkTask != null
				&& mLoadInfoLinkTask.getStatus() == Status.RUNNING) {
			cancelInfolink();
		} else {
			if (mProgress != null && mProgress.isShowing()) {
				mProgress.dismiss();
			}
			finish();
		}
	}
	
	protected UXAsyncImageView createImageView(Media media) {
		
		// サイズ取得
		setImageSize();
		
		SizeConv conv = SizeConv.getInstance(this);
		int size = (int) conv.getSize(70.0f);
		int margin = (int) conv.getSize(5.0f);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
		params.leftMargin = params.rightMargin = params.topMargin = params.bottomMargin = margin;
		UXAsyncImageView view = new UXAsyncImageView(this);
		view.setTag(media);
		view.setLayoutParams(params);
		view.setViewLoader(mLoader);
		
		view.loadImage(media, mSize);
		
		view.setOnClickListener(mOnClickRelatedImage);
		view.setBackgroundDrawable(getResources().getDrawable(R.drawable.infolink_up));
		return view;
	}
	
	
	private ImageSurfaceView mRelatedSurface;
	private ImageSurfaceView mSavedSurface;
	protected View.OnClickListener mOnClickRelatedImage = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (mRelatedSurface != null) {
				mRelatedSurface.dispose();
				mContainer.removeView(mRelatedSurface);
			}
			if (mSavedSurface == null) {
				mSavedSurface = (ImageSurfaceView) mContainer.getChildAt(0);
			}
			mContainer.removeAllViews();
			Media media = (Media) v.getTag();
			ImageLoaderFactory factory = new ImageLoaderFactory() {
				@Override
				public ImageLoader create() {
					return new SearchResultImageLoader(AbstractFullscreenActivity.this);
				}
			};
			List<?> tags = Arrays.asList(new Media[] {media});
			mRelatedSurface = new ImageSurfaceView(AbstractFullscreenActivity.this, factory, tags, 0);
			mContainer.addView(mRelatedSurface);
		}
	};

	protected abstract MediaIdentifier getCurrentMedia();
	
	protected Long getExifDateTime(File file) {
		try {
			ExifInterface exif = new ExifInterface(file.getAbsolutePath());
			String datetime = exif.getAttribute(ExifInterface.TAG_DATETIME);
			SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
			return format.parse(datetime).getTime();
		} catch (Exception e) {
			return file.lastModified();
		}
	}
	
	protected static class MediaIdentifier {
		public final String serviceType;
		public final String serviceAccount;
		public final String mediaId;
		public MediaIdentifier(String serviceType, String serviceAccount, String mediaId) {
			super();
			this.serviceType = serviceType;
			this.serviceAccount = serviceAccount;
			this.mediaId = mediaId;
		}
	}
	
	protected String getCategoryName() {
		return getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
	}
	
	/**
	 * 画面サイズに応じた描画サイズを計算します
	 *
	 * @param view	描画ビュー
	 */
	protected void setImageSize() {

		mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		mDisplay = mWindowManager.getDefaultDisplay(); 
		
		Configuration config = getResources().getConfiguration();
		
		// 画像サイズの計算
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mSize = mDisplay.getWidth() / 8;
		} else {
			mSize = mDisplay.getWidth() / 5;
		}
	}

	/**
	 * Infolinkのチュートリアルを開始します
	 */
	private void startTutorialInfoActivity() {
		mInfoLinks = null;
		
		Intent intent = new Intent(this, TutorialInfolinkActivity.class);
		startActivity(intent);
	}
	
}
