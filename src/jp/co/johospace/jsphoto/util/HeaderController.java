package jp.co.johospace.jsphoto.util;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.ux.widget.UXStage;
import jp.co.johospace.jsphoto.ux.widget.UXStage.OverScrollListener;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

/**
 * オーバーレイのヘッダー制御クラス
 */
public class HeaderController {
	public static final long LENGTH_DEFAULT = 3000;

	private static final int HEAD_SHOW = 1;

	private static final int HEAD_HIDE = 2;

	/**
	 * 非表示要求がスキップされた際のリスナインターフェース
	 */
	public interface OnHideSkipListener {
		void onHideSkip(HeaderController sender);
	}

	/*
	private class MultiListener implements OnScrollListener, OnTouchListener {

		private static final float FLICK_THRESHOLD = 200;

		protected static final int DIR_UNKNOWN = 0;
		protected static final int DIR_DOWN = 1;
		protected static final int DIR_UP = 2;

		private boolean mScrolling = false;
		private boolean mIsCapture;
		private MotionEvent mDownEvent;

		private int mLastTop = -1;
		private int mDirection = DIR_UNKNOWN;
		
		private Rect mLastRect = new Rect();
		private Rect mParentRect = new Rect();

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (totalItemCount <= 0) {
				return;
			}

			if (DEBUG)
				Log.e("onScroll", String.format("firstVisibleItem:%d, visibleItemCount:%d, totalItemCount:%d", firstVisibleItem, visibleItemCount, totalItemCount));
			
			view.getGlobalVisibleRect(mParentRect);
			if (DEBUG)
				Log.i("scroll parent", String.format("Rect:{left:%d, top:%d, right:%d, bottom:%d}",
					mParentRect.left, mParentRect.top, mParentRect.right, mParentRect.bottom));
			
			final boolean isFirstItemVisible = firstVisibleItem == 0;
			if (isFirstItemVisible) {
				View v = view.getChildAt(firstVisibleItem);
				v.getGlobalVisibleRect(mLastRect);
				
				if (DEBUG)
					Log.v("scroll first", String.format(
						"position:%d, Rect:{left:%d, top:%d, right:%d, bottom:%d}",
						firstVisibleItem, mLastRect.left, mLastRect.top, mLastRect.right, mLastRect.bottom));
				
				if (mLastRect.bottom - v.getMeasuredHeight() == mParentRect.top) {
					show(HeaderController.LENGTH_DEFAULT);
					
					if (DEBUG)
						Log.v("scroll first", "show");
				}
			}
			
			final boolean isLastItemVisible = firstVisibleItem + visibleItemCount == totalItemCount;
			if (isLastItemVisible) {
				try {
					final int lastItemPosition = visibleItemCount - 1;
					View v = view.getChildAt(lastItemPosition);
					v.getGlobalVisibleRect(mLastRect);
					
					if (DEBUG)
						Log.v("scroll last", String.format(
							"position:%d, Rect:{left:%d, top:%d, right:%d, bottom:%d}",
							lastItemPosition, mLastRect.left, mLastRect.top, mLastRect.right, mLastRect.bottom));
					
					if (mLastRect.top + v.getMeasuredHeight() == mParentRect.bottom) {
						show(HeaderController.LENGTH_DEFAULT);
						
						if (DEBUG)
							Log.v("scroll last", "show");
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if (mLastTop < firstVisibleItem) {
				mDirection = DIR_DOWN;
			} else if (mLastTop > firstVisibleItem) {
				mDirection = DIR_UP;
			} else {
				mDirection = DIR_UNKNOWN;
			}
			
			mLastTop = firstVisibleItem;
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// タッチアップで onScroll を呼ぶかどうか判断するために、実際にスクロールが発生するかどうかを判定する
			mScrolling  = scrollState != OnScrollListener.SCROLL_STATE_IDLE
					&& view.getFirstVisiblePosition() + view.getLastVisiblePosition() + 1 < view.getCount();
			
			if (DEBUG)
				Log.d("", "header onScrollStateChange - scrolling: " + mScrolling);
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (DEBUG)
					Log.d("", "head onTouch down");
				// キャプチャ開始
				mIsCapture = true;
				
				// 移動距離を取得するためダウン地点を保存
				mDownEvent = MotionEvent.obtain(event);
				break;
			case MotionEvent.ACTION_MOVE:
				// 厳密に処理したいときにはここで移動距離を計算する
				if (DEBUG) {
					if (mIsCapture) {
						Log.d("", "head onTouch capture move");
					} else {
						Log.d("", "head onTouch uncapture move");
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				// キャプチャ中か
				if (mIsCapture) {
					// ダウン地点からアップ地点のY移動距離を計算（単なる引き算）
					float distance = distance(mDownEvent, event);
					if (DEBUG)
						Log.d("", "head onTouch up distance: + " + distance);
					
					// 移動距離がしきい値以上かどうか
					// TODO しきい値の値を調整
					if (distance > FLICK_THRESHOLD || distance < -FLICK_THRESHOLD) {
						// スクロール中かヘッダが表示されていたら無視
						if (!mScrolling && !isHeaderDisplaied()) {
							AbsListView list = (AbsListView) v;
							final int firstVisibleItem = list.getFirstVisiblePosition();
							final int lastVisiblePosition = list.getLastVisiblePosition();
							final int visibleItemCount = lastVisiblePosition - firstVisibleItem + 1;
							final int totalItemCount = list.getCount();
							
							if (DEBUG)
								Log.d("", "head call onScroll");
							// onScroll の引数を自分で計算してスクロール時のイベントを呼び出す
							onScroll(list, firstVisibleItem, visibleItemCount, totalItemCount);
						} else if (DEBUG) {
							Log.d("", "head not call onScroll - scrolling: " + mScrolling + ", headerDisplaied: " + isHeaderDisplaied());
						}
					} else if (DEBUG) {
						Log.d("", "head onTouch up shorted");
					}
					
					mIsCapture = false;
				}
				break;
			default:
				if (DEBUG)
					Log.d("", String.format("head onTouch what: %d", event.getAction()));
			}
			return false;
		}

		private float distance(MotionEvent e1, MotionEvent e2) {
			return e2.getY() - e1.getY();
		}
		
	}
*/
	private class MultiListener implements OnScrollListener, OnTouchListener {

		private static final int DISTANCE_THRESHOLD = 0;
		private Rect mParentRect = new Rect();
		private Rect mChildRect = new Rect();
		private int mScrollState;
		
		private MotionEvent mEvent1;
		private float mDistance;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
//				Log.d("", "header onTouch down");		/*$debug$*/
				mEvent1 = MotionEvent.obtain(event);
				mDistance = 0;
				break;
			case MotionEvent.ACTION_MOVE:
//				Log.d("", "header onTouch move");		/*$debug$*/
				mDistance = event.getY() - mEvent1.getY();
				break;
			case MotionEvent.ACTION_UP:
//				Log.d("", "header onTouch up");		/*$debug$*/
				if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					onScrollStateChanged(((AbsListView) v), mScrollState);
				} else if (mScrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
					final AbsListView view = (AbsListView) v;
					mHandler.postDelayed(new Runnable() {
							@Override
							public void run() {
								// フリンジの状態のままアイドル状態にならないパターンの対応
								if (mScrollState == OnScrollListener.SCROLL_STATE_FLING && !isHeaderDisplaied()) {
									final int firstVisibleItem = view.getFirstVisiblePosition();
									final int lastVisibleItem = view.getLastVisiblePosition();
									if (mDistance > 0 && firstVisibleItem == 0) {
										show(LENGTH_DEFAULT);
									} else if (mDistance < 0 && lastVisibleItem + 1 == view.getCount()) {
										show(LENGTH_DEFAULT);
									}
								}
							}
					} , 500);
				}
				break;
			}
			return false;
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
//			Log.d("", "header onScrollStateChanged: " + scrollState);		/*$debug$*/
			if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
				final int totalItemCount = view.getCount();
				if (totalItemCount > 0 && !isHeaderDisplaied()) {
					final int firstVisibleItem = view.getFirstVisiblePosition();
					final int lastVisibleItem = view.getLastVisiblePosition();
					final int visibleItemCount = lastVisibleItem - firstVisibleItem +  1;

					view.getGlobalVisibleRect(mParentRect);
					
					if (Math.abs(mDistance) > DISTANCE_THRESHOLD) {
						if (firstVisibleItem == 0) {
							View v = view.getChildAt(firstVisibleItem);
							
							v.getGlobalVisibleRect(mChildRect);
							if (mChildRect.bottom - v.getMeasuredHeight() == mParentRect.top) {
								show(LENGTH_DEFAULT);
							}
						}
						
						if (lastVisibleItem + 1 == totalItemCount) {
							View v = view.getChildAt(visibleItemCount - 1);
							
							v.getGlobalVisibleRect(mChildRect);
							if (mChildRect.top + v.getMeasuredHeight() == mParentRect.bottom) {
								show(LENGTH_DEFAULT);
							}
						}
					}
				}
			}
			
			mScrollState = scrollState;
		}
		
	}
	
	/**
	 * アニメーションを用いたヘッダーの表示・非表示きりかえハンドラ
	 */
	private class MyHandler extends Handler {
		boolean isFirst = true;
		
		public MyHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			Animation anim;
			switch (what) {
			case HEAD_SHOW:
				if (hasMessages(HEAD_SHOW))
					return;
				
				anim = mFadein;
				
				if(isFirst){
					//初回起動時、リスナーが応答しないことがあるので、直接起動する
					//注意.この方法だと、onAnimationStartが2回よばれる(現状呼ばれても動作に問題はない)
					//TODO
					mView.setAnimation(anim);
					mAnimListener.onAnimationStart(anim);
			
					isFirst = false;
				}else{
					
					mView.startAnimation(anim);
				}
				break;
			case HEAD_HIDE:
				if (hasMessages(HEAD_HIDE))
					return;
				
				if (mKeep) {
					mSkipCount++;
					if (mOnHideSkipListener != null)
						mOnHideSkipListener.onHideSkip(HeaderController.this);
				} else {
					mSkipCount = 0;
					anim = mFadeout;
					mView.startAnimation(anim);
				}
				break;
			}
		}
	}

	/** アニメーションのリスナ */
	private AnimationListener mAnimListener = new AnimationListener() {

		
		@Override
		public void onAnimationStart(Animation animation) {
			
			if (animation == mFadein) {
				mView.setVisibility(View.VISIBLE);
			}
		}
		
		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
		@Override
		public void onAnimationEnd(Animation animation) {
			
			// フェードアウト
			if (animation == mFadeout) {
				mView.setVisibility(View.GONE);
			}
			// フェードイン完了後、３秒経過でフェードアウト
			else if (animation == mFadein && mHideDellay > 0) {
				mHandler.sendEmptyMessageDelayed(HEAD_HIDE, mHideDellay);
			}
		}
	};

	private Handler mHandler;
	private ViewGroup mParent;
	private View mView;
	private Animation mFadein;
	private Animation mFadeout;

	private boolean mKeep = false;
	private long mHideDellay = -1;
	private OnHideSkipListener mOnHideSkipListener;
	private int mSkipCount = 0;

	private final boolean DEBUG;

	/**
	 * コンストラクタ
	 * @param context
	 * @param layoutResId
	 * @param parent
	 */
	public HeaderController(Context context, int layoutResId, ViewGroup parent) {
		this(context, layoutResId, parent, context.getMainLooper());
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 * @param layoutResId
	 * @param parent
	 * @param looper
	 */
	public HeaderController(Context context, int layoutResId, ViewGroup parent, Looper looper) {
		mHandler = new MyHandler(looper);
		
		mParent = parent;
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = inflater.inflate(layoutResId, parent, false);
		mView.setVisibility(View.GONE);
		
		mParent.addView(mView);
		
		mFadein = AnimationUtils.loadAnimation(context, R.anim.fade_in_header);
		mFadeout = AnimationUtils.loadAnimation(context, R.anim.fade_out_header);
		
		mFadein.setAnimationListener(mAnimListener);
		mFadeout.setAnimationListener(mAnimListener);
		
		DEBUG  = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE;
	}
	
	/**
	 * ヘッダを表示します。自動で隠されません。
	 */
	public void show() {
		show(0);
	}
	
	/**
	 * ヘッダを表示して、指定された遅延時間が経過すると自動で隠します
	 * @param autoHideDellay
	 */
	public void show(long autoHideDellay) {
		mHandler.removeMessages(HEAD_HIDE);
		mHandler.removeMessages(HEAD_SHOW);
		
		mHideDellay  = autoHideDellay;
		mHandler.sendEmptyMessage(HEAD_SHOW);
	}
	
	/**
	 * 直ちに隠します
	 */
	public void hide() {
		hide(0);
	}
	
	/**
	 * 遅延時間が経過後に隠します
	 * @param autoHideDellay
	 */
	public void hide(long autoHideDellay) {
		mHandler.removeMessages(HEAD_HIDE);
		mHandler.removeMessages(HEAD_SHOW);
		
		mHandler.sendEmptyMessageDelayed(HEAD_HIDE, autoHideDellay);
	}
	
	/**
	 * 非表示をスキップした際のリスナを設定します
	 * @param l
	 */
	public void setOnHideSkipListener(OnHideSkipListener l) {
		mOnHideSkipListener = l;
	}

	/**
	 * 捜査の対象となるビューを取得します
	 * @return
	 */
	public View getView() {
		return mView;
	}

	/**
	 * ヘッダを破棄します
	 */
	public void onDestroy() {
		mParent.removeView(mView);
	}

	/**
	 * 隠す処理がスキップされたかどうか
	 * @return
	 */
	public boolean isHideSkiped() {
		return mSkipCount  > 0;
	}
	
	/**
	 * 自動で隠す処理を無効化するかどうかを設定します
	 * @param keep
	 */
	public void setDisplayKeep(boolean keep) {
		mKeep = keep;
	}

	/**
	 * ヘッダーが表示されているかどうかを取得します
	 * @return
	 */
	public boolean isHeaderDisplaied() {
		return mView.getVisibility() == View.VISIBLE;
	}

	/**
	 * アダプタをサポートするビューにヘッダ表示用イベントを設定します
	 * @param list
	 */
	public void setHeaderEventTo(AbsListView list) {
		MultiListener l = new MultiListener();
		list.setOnTouchListener(l);
		list.setOnScrollListener(l);
	}

	/**
	 * UXStageにヘッダ表示用イベントを設定します
	 * @param stage
	 */
	public void setHeaderEventTo(UXStage stage) {
		stage.setOnOverScrollListener(new OverScrollListener() {
			@Override
			public void onOverScroll() {
				if (!isHeaderDisplaied()) {
					show(LENGTH_DEFAULT);
				}
			}
		});
	}
}