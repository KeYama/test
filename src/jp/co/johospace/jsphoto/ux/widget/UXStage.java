package jp.co.johospace.jsphoto.ux.widget;

import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;

import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderer;
import jp.co.johospace.jsphoto.ux.renderer.gl.UXGLRenderEngine;
import jp.co.johospace.jsphoto.ux.renderer.soft.UXSoftRenderEngine;
import jp.co.johospace.jsphoto.ux.util.OverScroller;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * すべての親要素
 *
 * リソース破棄戦略：
 * Draw時、呼び出されなかったら破棄。
 *
 */
public class UXStage extends UXContainer{
	public static final int SOFT_RENDERER = 0;
	public static final int GL_RENDERER = 1;

	private static final int SCROLL_START = 1;
	private static final int SCROLL_END = 2;
	
	private static final float FLASH_EPS = 20;
	
	private static final float VELOCITY_COEFFICIENT = 1.0f;
	private static final float FRICTION_COEFFICIENT = 1.0f;

	private WeakHashMap<Integer, UXWidget> mIdMap = new WeakHashMap<Integer, UXWidget>();

	private float mDensity;
	private UXRenderEngine mEngine;
	private Semaphore mSemaphore;
	private RectF mViewPort;
	private OverScroller mOverScroller;
	private boolean mLastOne;
	private boolean mModified;
	private boolean mTapDownFlag;
	private ScrollListener mScrollListener;
	private boolean mBeforeTapFlag;
	private boolean isScroll;
	private float mTapX;
	private float mTapY;
	private Drawable mFlashImage;
	private float mBarTapX, mBarTapY; //とりあえずこれで、後々mTapX, mTapYに統合しよう
	private ScrollBar mScrollBar;
	private boolean mScrollResSet;
	private OverScrollListener mOverScrollListener;
	
	private float mSliderWidth, mSliderHeight;

	private Handler mScrollHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(mScrollListener == null) return;
			switch(msg.arg1){
			case SCROLL_START:
				mScrollListener.onScrollStart();
				break;
			case SCROLL_END:
				mScrollListener.onScrollEnd();
				break;
			}
		}
	};

	Context mContext ;
	public UXStage(Context context, int type){
		/**追加**/
		mContext = context;


		mDensity = context.getResources().getDisplayMetrics().density;

		if(type == SOFT_RENDERER){
			mEngine = new UXSoftRenderEngine(context, new MyRenderer());
		}else{
			mEngine = new UXGLRenderEngine(context, new MyRenderer());
		}
		mSemaphore = new Semaphore(1);

		mViewPort = new RectF();
		mOverScroller = new OverScroller(context);
		mOverScroller.setFriction(ViewConfiguration.getScrollFriction()*FRICTION_COEFFICIENT);

		mEngine.setMotionListener(new MyMotionListener(context));
		mEngine.setImageListener(new UXRenderEngine.ImageListener() {
			@Override
			public void onLoadImage(UXImageResource resource, Object info) {
				mEngine.invalidate();
			}

			@Override
			public void onFailedLoadingImage(UXImageResource resource, Object info) {
				mEngine.invalidate();
			}
		});
	}
	
	public void addThread(int num){
		mEngine.addDecoderThread(num);
	}

	public void setOnOverScrollListener(OverScrollListener listener){
		mOverScrollListener = listener;
	}

	/**
	 * クリア。ステージをロックして呼び出す。
	 */
	public void clear(){
		mChildren.clear();
		mEngine.disposeAllResources(null);
		mScrollBar = null;
	}

	public void setBackgroundColor(int color){
		mEngine.setBackgroundColor(color);
	}

	public void setScrollListener(ScrollListener listener){
		mScrollListener = listener;
	}

	@Override
	public void layout(UXStage stage) {
		super.layout(stage);

		int size = mChildren.size();
		mHeightPx = 0;
		for(int n = 0; n < size; ++n){
			mHeightPx += mChildren.get(n).getHeight();
		}
	}

	private int mImaRes;
	public void setScrollbarResource(int res, float widthDp, float heightDp){
		mImaRes = res;
		mScrollResSet = true;
		
		mSliderWidth = dp2px(widthDp);
		mSliderHeight = dp2px(heightDp);
	}


	public float dp2px(float dp){
		return dp * mDensity;
	}
	
	public float getDisplayWidth(){
		return mContext.getResources().getDisplayMetrics().widthPixels;
	}

	public float getScreenWidth(){
		return mEngine.getWidth();
	}

	public float getScreenHeight(){
		return mEngine.getHeight();
	}

	public UXWidget findWidgetById(int id){
		return mIdMap.get(id);
	}

	@Override
	public UXContainer add(UXWidget widget) {
		super.add(widget);
		widget.setStage(this);

		return this;
	}

	public void dispose(){
		mEngine.dispose();
		if(mScrollBar != null)mScrollBar.dispose();
	}

	public void invalidate(){
		mEngine.invalidate();
	}

	public View getView(){
		return mEngine.getView();
	}

	public UXRenderEngine getEngine(){
		return mEngine;
	}

	public void lockStage(Runnable r){
		try {
			mSemaphore.acquire();
			r.run();
			mModified = true;
		} catch (InterruptedException e) {}
		finally{
			mSemaphore.release();
		}
	}

	void beginAnimation(){
		try {
			mSemaphore.acquire();
		} catch (InterruptedException e) {
		}
	}

	void endAnimation(){
		mSemaphore.release();
		invalidate();
	}

	void scroll(float dy){
		mViewPort.offset(0, dy);
	}

	public void register(int id, UXWidget widget){
		mIdMap.put(id, widget);
	}
	
	public void setFlashResource(Drawable d){
		mFlashImage = d;
	}
	
	public Drawable getFlashResource(){
		return mFlashImage;
	}

	public void onPause(){
		mEngine.onPause();
	}

	public void onResume(){
		mEngine.onResume();
	}

	private static RectF reserveRect = new RectF();

	private class MyRenderer implements UXRenderer{
		@Override
		public void render(UXRenderEngine engine) {

			if(mScrollBar == null && mScrollResSet){
				Bitmap barBitmap = BitmapFactory.decodeResource(mContext.getResources(), mImaRes);
				mScrollBar = new ScrollBar(UXStage.this,(int)(mSliderWidth), (int)(mSliderHeight), barBitmap);
			}

			try {
				mSemaphore.acquire();
				if(mModified){
					mModified = false;

					layout(UXStage.this);
					loadResource(engine);
					
					if(mViewPort.height() < mHeightPx && 
							mViewPort.bottom > mHeightPx)
						mViewPort.offset(0, mHeightPx - mViewPort.bottom);
				}

				mOverScroller.computeScrollOffset();

				RectF r = mViewPort;
				if(mScrollBar != null && mScrollBar.getSeize()){
					r = mScrollBar.getViewPort(mViewPort, mHeightPx);
			
				}else{
					r.set(r.left, r.top, r.left+mEngine.getWidth(), r.top+mEngine.getHeight());
				}
				beginDraw();
				draw(engine, r);

				//描画する必要があるかを判断する
				if(mScrollBar != null){
					mScrollBar.managingDisplay(mEngine, mTapDownFlag, !mOverScroller.isFinished());
					mScrollBar.draw(r, mHeightPx, mBarTapX, mBarTapY);
				}

				//タップ時に半透明画像を描画する
				if(mTapDownFlag && Math.abs(isTapParam - mViewPort.top) < FLASH_EPS && !isScroll){
					drawTransparencyImage((int)mTapX, (int)mTapY, mViewPort);

				}else if(isTapParam != mViewPort.top){
					isScroll = true;
				}

				reserveRect.set(r);
				reserveRect .inset(0, -getScreenHeight()/2);
				if(!mOverScroller.isFinished()){
					if(mOverScroller.isScrollingInDirection(0, -1)){
						//上にスクロール中
						reserveRect.offset(0, -getScreenHeight()/2);
					}else{
						//下にスクロール中
						reserveRect.offset(0, getScreenHeight()/2);
					}
				}
				//reserve(engine, reserveRect);
				endDraw();

				if(mLastOne){
					mViewPort.top = mOverScroller.getCurrY();
					mViewPort.bottom = mViewPort.top + mEngine.getHeight();
					mLastOne = false;
					invalidate();
				}

				if(!mOverScroller.isFinished()){
					invalidate();

					if(mScrollBar != null){
						if(!mScrollBar.getSeize()){
							mViewPort.top = mOverScroller.getCurrY();
							mViewPort.bottom = mViewPort.top + mEngine.getHeight();
							mLastOne = true;
						}
					}else{
						mViewPort.top = mOverScroller.getCurrY();
						mViewPort.bottom = mViewPort.top + mEngine.getHeight();
						mLastOne = true;
					}
				}
				checkScroll();

			} catch (InterruptedException e) {
			}finally{
				mSemaphore.release();
			}
		}

		@Override
		public void initialize(UXRenderEngine engine) {
		}

		@Override
		public void surfaceChanged(UXRenderEngine engine) {
			layout(UXStage.this);
		}
	}


	private class MyMotionListener implements UXRenderEngine.MotionListener{
		private GestureDetector mDetector;

		public MyMotionListener(Context context){
			mDetector = new GestureDetector(context, new MyGestureListener());
		}

		@Override
		public boolean onMotion(MotionEvent event) {

			mBarTapX = event.getX();
			mBarTapY = event.getY();

			beginAnimation();
			
			applyScroll(event);
			
			if((event.getAction() == MotionEvent.ACTION_UP ||
					event.getAction() == MotionEvent.ACTION_CANCEL) &&
					mOverScroller.isFinished()){

				mOverScroller.forceFinished(true);
				scrollBack();
			}
			int action = event.getAction();
			if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
				/** 追加 バーを離す**/
				if(mScrollBar != null)mScrollBar.releseBar();

				mTapDownFlag = false;
			}
			if(action == MotionEvent.ACTION_DOWN){
				/** 追加 バーをつかむ**/
				if(mScrollBar != null)mScrollBar.gradBar(event.getX(), event.getY());

				mTapX = event.getX();
				mTapY = event.getY();
				mTapDownFlag = true;
				isTapParam = mViewPort.top;
				isScroll = false;
			}
			endAnimation();

			boolean flag =  mDetector.onTouchEvent(event);

			beginAnimation();
			checkScroll();
			endAnimation();
			return flag;
		}
	}
	
	private float mScrollStartY;
	
	private void applyScroll(MotionEvent e){
		
		switch(e.getAction()){
		case MotionEvent.ACTION_DOWN:
			mScrollStartY = e.getY();
			break;
			
		case MotionEvent.ACTION_MOVE:
			mViewPort.offset(0, mScrollStartY - e.getY());
			mScrollStartY = e.getY();
			scrollBack();
			break;
		}
		
		//android.util.Log.e("onScroll", ""+distanceY);
		//mViewPort.offset(0, distanceY);
	}
	
	
	float isTapParam;

	private void checkScroll(){
		//android.util.Log.e("dbg", "TapDownFlag:"+mTapDownFlag+" mBeforeTapFlag:"+mBeforeTapFlag+" finish:"+mOverScroller.isFinished());
		if(mTapDownFlag && !mBeforeTapFlag){
			mBeforeTapFlag = true;
			Message msg = mScrollHandler.obtainMessage();
			msg.arg1 = SCROLL_START;
			mScrollHandler.sendMessage(msg);

		}
		if(!mTapDownFlag && mBeforeTapFlag && mOverScroller.isFinished()){
			mBeforeTapFlag = false;
			Message msg = mScrollHandler.obtainMessage();
			msg.arg1 = SCROLL_END;
			mScrollHandler.sendMessage(msg);
		}
	}
	
	/**
	 * ViewPortを指定の数移動させる。
	 * トップが0以下になる際は、そのまま
	 * @param y
	 * @return 移動できたら true, 出来なければ false
	 */
	public boolean moveViewPort(float y){
		
		if(mViewPort.top - y > 0){
			mViewPort.offset(0, -y);
			invalidate();
			return true;

		}else{
			return false;
		}
	}
	
	private void scrollBack(){
		
		if(mViewPort.height() < mHeightPx){
			//Overscroll削除
//			if(mViewPort.top < 0){
//				mOverScroller.forceFinish(true);
//				mOverScroller.startScroll(0, (int)mViewPort.top, 0, (int)-mViewPort.top, 500);
//			}
//			if(mViewPort.bottom > mHeightPx ){
//				mOverScroller.forceFinish(true);
//				mOverScroller.startScroll(0, (int)mViewPort.top, 0, (int)(mHeightPx-mEngine.getHeight()-mViewPort.top), 500);
//			}

			if(mViewPort.top < 0){
				mOverScroller.forceFinished(true);
				mViewPort.offset(0, -mViewPort.top);
				if(mOverScrollListener != null)mOverScrollListener.onOverScroll();
			}
			if(mViewPort.bottom > mHeightPx){
				mOverScroller.forceFinished(true);
				mViewPort.offset(0, mHeightPx - mViewPort.bottom);
				if(mOverScrollListener != null)mOverScrollListener.onOverScroll();
			}
			//スクロールバーを離した際には、慣性を付けない
			if(mScrollBar != null){
				if(!mScrollBar.getSeize() && mScrollBar.isOneBeforeSeize()){
					mOverScroller.abortAnimation();
				}
			}

		}else{
			//OverScroll削除
//			if(mViewPort.top < 0.1f || mViewPort.top > 0.1f){
//				mOverScroller.forceFinished(true);
//				mOverScroller.1(0, (int)mViewPort.top, 0, (int)-mViewPort.top, 500);
//			}
			if(mViewPort.top < 0.1f || mViewPort.top > 0.1f){
				mOverScroller.forceFinished(true);
				mViewPort.offset(0, -mViewPort.top);
				if(mOverScrollListener != null)mOverScrollListener.onOverScroll();
			}
		}

	}

	private class MyGestureListener extends GestureDetector.SimpleOnGestureListener{

		@Override
		public boolean onDown(MotionEvent e) {
			beginAnimation();
			mOverScroller.forceFinished(true);
			endAnimation();
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			beginAnimation();
			//Overscroll削除
//			mOverScroller.fling(0, (int) mViewPort.top, 0, (int) -velocityY,
//					0, 0, 0, (int)mHeightPx - mEngine.getHeight(), 0, (int)dp2px(100));
			mOverScroller.fling(0, (int)mViewPort.top, 0, (int)(-velocityY*VELOCITY_COEFFICIENT),
					0, 0, 0, (int)mHeightPx - mEngine.getHeight(), 0, 0);

			scrollBack();
			endAnimation();

			return super.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public void onLongPress(MotionEvent e) {
//			int size = mChildren.size();
//			for(int n = 0; n < size; ++n){
//				UXWidget widget = mChildren.get(n);
//				if(widget.onLongPress(mViewPort, e)){
//					break;
//				}
//			}
			UXStage.this.onLongPress(mViewPort, e);

			super.onLongPress(e);
		}
		
		

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			onSingleTap(mViewPort, e);			
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
//			int size = mChildren.size();
//			for(int n = 0; n < size; ++n){
//				UXWidget widget = mChildren.get(n);
//				if(widget.onSingleTap(mViewPort, e)){
//					break;
//				}
//			}


			return super.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//			beginAnimation();
//			//android.util.Log.e("onScroll", ""+distanceY);
//			mViewPort.offset(0, distanceY);
//			scrollBack();
//			endAnimation();
			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}

	public static interface ScrollListener{
		void onScrollStart();
		void onScrollEnd();
	}

	public static interface OverScrollListener{
		void onOverScroll();
	}
}
