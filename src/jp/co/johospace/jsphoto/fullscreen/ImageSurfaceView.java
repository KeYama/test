package jp.co.johospace.jsphoto.fullscreen;

import java.util.List;

import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoaderFactory;
import android.content.Context;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class ImageSurfaceView extends SurfaceView{
	private AnimationThread mAnimationThread;
	private MessageDispatcher mDispatcher;
	private ImageLoaderFactory mFactory;
	private GestureDetector mDetector;
	private ScaleGestureDetector mScaleDetector;
	private int mCurrentFrame;
	private List<?> mTags;
	private int mInitialPosition;
	private View.OnClickListener mClickListener;
	private View.OnLongClickListener mLongClickListener;
	private OnMoveListener mMoveListener;
	private boolean mDisableFlag;
	private boolean mNeutralFlag = true;
	private Handler mHandler = new Handler();
	private FrameMoveListener mFrameMoveListener;
	private boolean canScale = true;
	private boolean canLongPress = true;
	
	public static interface FrameMoveListener{
		public void onFrameMove();
	}
	
	public void setSwipeListener(FrameMoveListener listener){
		mFrameMoveListener = listener;
	}

	/**
	 * コンストラクタ。
	 * 
	 * @param context コンテキスト。普通のビューに渡すものと同じ
 	 * @param factory ImageLoaderFactory。これを入れ替えることで読み込み元を変えることができる
 	 * @param tags ImageLoaderに渡すタグ情報のリスト
	 * @param initial タグの初期位置
	 */
	public ImageSurfaceView(Context context, ImageLoaderFactory factory, List<?> tags, int initial) {
		super(context);
		mDispatcher = new MessageDispatcher();
		mFactory = factory;
		MyGestureListener listener = new MyGestureListener();
		mDetector = new GestureDetector(context, listener);
		mScaleDetector = new ScaleGestureDetector(context, listener);
		mTags = tags;
		mCurrentFrame = mInitialPosition = initial;
		
		mAnimationThread = new AnimationThread(mDispatcher, tags, mFactory);
		getHolder().addCallback(mAnimationThread);
	}
	
	/**
	 * 
	 * 現在表示中の画像に関連付けられたタグを取得
	 * タップででる画面を表示する際に必要な情報。
	 * 
	 * @return
	 */
	public Object getCurrentTag(){
		return mTags.get(mCurrentFrame);
	}
	
	/**
	 * 現在の表示位置を戻す
	 * 
	 * @return
	 */
	public int getCurrentNumber(){
		return mCurrentFrame;
	}
	
	/**
	 * 終了して終了を待つ
	 * 
	 */
	public void dispose(){
		mAnimationThread.stopThread();
		try {
			mAnimationThread.join();
		} catch (InterruptedException e) {
//			e.printStackTrace();		/*$debug$*/
		}
	}
	
	public void setShowEnabled(boolean flag){
		//mDisableFlag = !flag;
	}
	
	
	@Override
	public void setOnClickListener(View.OnClickListener listener){
		mClickListener = listener;
		super.setOnClickListener(listener);
	}
	
	@Override
	public void setOnLongClickListener(View.OnLongClickListener listener){
		mLongClickListener = listener;
		super.setOnLongClickListener(listener);
	}
	
	public void setOnMoveListener(OnMoveListener listener){
		mMoveListener = listener;
	}
	
	public interface OnMoveListener {
		void onMove(MotionEvent event);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(mDisableFlag)return super.onTouchEvent(event);
		
		int action = event.getAction();
		if(action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP){
			mDispatcher.putMessage(new MessageChannel.Up(event));
		}
		
		boolean inProgress = mScaleDetector.isInProgress();
		mScaleDetector.onTouchEvent(event);
		if(inProgress || mScaleDetector.isInProgress())return true;
		
		return mDetector.onTouchEvent(event);
	}

	private class MyGestureListener implements GestureDetector.OnGestureListener, 
		GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener{

		@Override
		public boolean onDown(MotionEvent e) {
			canScale = true;
			mDispatcher.putMessage(new MessageChannel.Down(e));
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if(mMoveListener != null) mMoveListener.onMove(e2);
			
			canScale = false;
			mDispatcher.putMessage(new MessageChannel.Fling(e1, e2, velocityX, velocityY));
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			if(!canLongPress) return;
			if(mLongClickListener != null){
				//mLongClickListener.onLongClick(ImageSurfaceView.this);
				return;
			}
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			if(mMoveListener != null) mMoveListener.onMove(e2);
			
			canScale = false;
			mDispatcher.putMessage(new MessageChannel.Scroll(e1, e2, distanceX, distanceY));
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {
			
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			mDispatcher.putMessage(new MessageChannel.DoubleTap(e));
			return true;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if(mClickListener != null){
				mClickListener.onClick(ImageSurfaceView.this);
				return true;
			}else{
				return false;
			}
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			if(mMoveListener != null){
				mMoveListener.onMove(null);
			}
			if(!canScale) return false;
			mDispatcher.putMessage(new MessageChannel.Scaling(detector.getScaleFactor()));
			canLongPress = false;
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			if(!canScale) return false;
			mDispatcher.putMessage(new MessageChannel.ScaleStart());
			canLongPress = false;
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			mDispatcher.putMessage(new MessageChannel.ScaleEnd());
			canLongPress = true;
		}
		
	}
	
	private class AnimationThread extends Thread implements SurfaceHolder.Callback{
		MessageDispatcher mDispatcher;
		List<?> mPathList;
		ImageLoaderFactory mFactory;

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			mDispatcher.putMessage(new MessageChannel.SurfaceChanged(holder, width, height));
			if(!isAlive())start();
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
//			stopThread();
//			try {
//				join();
//			} catch (InterruptedException e) {}
		}
		
		public AnimationThread(MessageDispatcher dispatcher, List<?> pathList, ImageLoaderFactory factory){
			mDispatcher = dispatcher;
			mPathList = pathList;
			mFactory = factory;
		}
		
		public void stopThread(){
			mDispatcher.putMessage(new MessageChannel.Exit());
		}
		
		@Override
		public void run(){
			MessageDispatcher dispatcher = mDispatcher;
			FrameDeck deck = new FrameDeck(dispatcher, mFactory, mPathList, mInitialPosition);
			
			while(true){
				try {
					Thread.sleep(1000/60);
				} catch (InterruptedException e) {}
				
				if(!dispatcher.handleMessage())break;
				
				mCurrentFrame = deck.getCurrentFrame();
				
				if(!deck.draw()){
					dispatcher.waitMessage();
				}
				
				
				boolean neutral = deck.isFrameNeutral();
				
				if(mNeutralFlag && !neutral && mFrameMoveListener != null){
					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							mFrameMoveListener.onFrameMove();
						}
					});
				}
				
				mNeutralFlag = neutral;
			}
			deck.dispose();
		}
	}

}
