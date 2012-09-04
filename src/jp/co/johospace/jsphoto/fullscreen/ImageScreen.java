package jp.co.johospace.jsphoto.fullscreen;

import android.graphics.Matrix;


/**
 * ImageFrameの描画領域を表現
 * 
 */
public class ImageScreen implements MessageDispatcher.Handler{
	private static final float SCALE_FACTOR = 2.0f;
	private static final float SCALE_LIMIT = SCALE_FACTOR*SCALE_FACTOR - 0.1f;

	private Measure mMeasure;
	private ImageFrame mFrame;
	private int mDisableCount = 0;
	private ScrollAnimator mScroller;
	
	private Matrix mSrcMatrix = new Matrix();
	private Matrix mDstMatrix = new Matrix();
	private Matrix mCurrentMatrix = new Matrix();
	
	private Matrix mInitialScaleMatrix = new Matrix();
	
	private float mT;
	private boolean mCheckLimitFlag = false;
	
	public ImageScreen(ImageFrame frame, Measure measure){
		mMeasure = measure;
		mFrame = frame;
		mScroller = new ScrollAnimator();
		mT = 1;
	}
	
	public boolean process(){
		boolean processed = mScroller.process(mDstMatrix);
//		if(!processed){
//			checkLimit();
//		}
		if(mDisableCount > 0)mDisableCount--;
		
		if(mT < 1){
			mT += 0.1f;
			return true;
		}else{
			mCurrentMatrix.set(mDstMatrix);
		}
		
		return processed;
	}
	
	private void checkLimit(){
		if(mT < 1)return;
		float[] pts = new float[]{
			0, 0, 
			mFrame.getWidth(), mFrame.getHeight()
		};
		Matrix thisMatrix = getMatrix();
		thisMatrix.mapPoints(pts);
		float screenWidth = mMeasure.getScreenWidth();
		float screenHeight = mMeasure.getScreenHeight();
		float scale = getScale(thisMatrix);
		float adjustX = 0;
		float adjustY = 0;
		
		if(pts[2] - pts[0] > screenWidth){
			//画像端と合わせる
			
			if(pts[0] > 0){
				//画像が右すぎる
				adjustX =  - pts[0];
			}
			if(pts[2] < screenWidth){
				//画像が左すぎる
				adjustX = screenWidth - pts[2];
			}
		}else{
			//中央に配置
			float center = (pts[2] - pts[0])/2 + pts[0];
			if(Math.abs(screenWidth/2 - center) > 0.01f){
				adjustX = screenWidth/2 - center;
			}
		}
		
		if(pts[3] - pts[1] > screenHeight){
			//画面端と合わせる
			if(pts[1] > 0){
				//画像が下すぎる
				adjustY = - pts[1];
			}
			if(pts[3] < screenHeight){
				//画像が上すぎる
				adjustY = screenHeight - pts[3];
			}
		}else{
			float center = (pts[3] - pts[1])/2 + pts[1];
			if(Math.abs(screenHeight/2 - center) > 0.01f){
				adjustY = screenHeight/2 - center;
			}
		}
		
		if(adjustX != 0 || adjustY != 0){
			Matrix fit = new Matrix();
			calcFit(fit);
			float fitScale = getScale(fit);
			
			Matrix m = new Matrix(mDstMatrix);
			m.preTranslate(adjustX*fitScale/scale, adjustY*fitScale/scale);
//			startAnimation(m);
			mDstMatrix.set(m);
			mCurrentMatrix.set(m);
		}
	}
	
	private float getScale(Matrix m){
		float[] values = new float[9];
		m.getValues(values);
		return values[0];
	}
	
	public boolean isZoom(){
		return getScale(mDstMatrix) != 1;
	}
	
	public Matrix getMatrix(){
		if(!mCheckLimitFlag){
			mCheckLimitFlag = true;
			checkLimit();
			mCheckLimitFlag = false;
		}
		
		
		Matrix m = new Matrix();
		if(mT < 1){
			float[] srcValues = new float[9];
			float[] dstValues = new float[9];
			float[] currentValues = new float[9];
			float t = CurveUtil.easeOutSin(mT);
			
			mSrcMatrix.getValues(srcValues);
			mDstMatrix.getValues(dstValues);
			for(int n = 0; n < 9; ++n){
				currentValues[n] = (1-t)*srcValues[n] + t*dstValues[n];
			}
			mCurrentMatrix.setValues(currentValues);
			
		}
		calcFit(m);
		m.postConcat(getScreenMatrix());
		
		return m;
	}
	
	@Override
	public boolean handleMessage(Object msg) {
		if(mDisableCount > 0 && msg instanceof MessageChannel.Motion){
			return true;
		}
		if(msg instanceof MessageChannel.DoubleTap){
			onDoubleTap((MessageChannel.DoubleTap)msg);
			return true;
		}else if(msg instanceof MessageChannel.Scroll){
			return mScroller.onScroll((MessageChannel.Scroll)msg);
		}else if(msg instanceof MessageChannel.Fling){
			return mScroller.onFling((MessageChannel.Fling)msg);
		}else if(msg instanceof MessageChannel.Scaling){
			onScaling(((MessageChannel.Scaling)msg).scale);
			return true;
		}else if(msg instanceof MessageChannel.ScaleEnd){
			onScaleEnd();
			return true;
		}else if(msg instanceof MessageChannel.ScaleStart){
			onScaleStart();
			return true;
		}
		
		return false;
	}
	
	private void onScaling(float scale){
		Matrix m = new Matrix();
		mDstMatrix.set(mInitialScaleMatrix);
		m.set(calcScaleAt(mMeasure.getScreenWidth()/2, mMeasure.getScreenHeight()/2, (scale-1)*2.5f+1));
		mDstMatrix.set(m);
	}
	
	private void onScaleStart(){
		// 前後のサムネイルクリア（OutOfMemory対策）
		mFrame.clearThumbnailHolderOther();
		
		mInitialScaleMatrix.set(getScreenMatrix());
	}
	
	private void onScaleEnd(){
		mDisableCount = 3;
		
		float currentScale = getScale(mDstMatrix);
		if(currentScale > SCALE_LIMIT){
			float scale = SCALE_LIMIT / getScale(mDstMatrix) + 0.1f;//調整用
			startAnimation(calcScaleAt(mMeasure.getScreenWidth()/2, mMeasure.getScreenHeight()/2, scale));
		}else if(currentScale < 1){
			startAnimation(new Matrix());
		}
	}
	
	private void onDoubleTap(MessageChannel.DoubleTap msg){
		// 前後のサムネイルクリア（OutOfMemory対策）
		mFrame.clearThumbnailHolderOther();
		
		if(getScale(mDstMatrix) > SCALE_LIMIT){
			mScroller.reset();
			startAnimation(new Matrix());
		}else{
			startAnimation(calcScaleAt(msg.e.getX(), msg.e.getY(), SCALE_FACTOR));
		}
	}
	
	private Matrix calcScaleAt(float x, float y, float scale){
		Matrix screen = getScreenMatrix();
		Matrix inverse = new Matrix();
		screen.invert(inverse);
		
		float[] pt = new float[]{x, y};
		inverse.mapPoints(pt);
		
		Matrix zoom = new Matrix();
		zoom.postTranslate(-pt[0], -pt[1]);
		zoom.postScale(scale, scale);
		zoom.postTranslate(pt[0], pt[1]);
		zoom.postConcat(screen);
		
		return zoom;
	}
	
	private void startAnimation(Matrix dst){
		mT = 0;
		mSrcMatrix.set(mDstMatrix);
		mDstMatrix.set(dst);
	}
	
	private Matrix getScreenMatrix(){
		Matrix m =  new Matrix();
		m.set(mCurrentMatrix);
		return m;
	}
	
	/**
	 * 画面にフィットするような行列を計算、セット
	 * 
	 * @param m
	 * @param b
	 */
	public void calcFit(Matrix m){
		float w = mFrame.getWidth();
		float h = mFrame.getHeight();
		 // -4は、区切り線を引くため
		float sw = mMeasure.getScreenWidth() - 4;
		float sh = mMeasure.getScreenHeight() - 4;
		
		float scaleW = sw / w;
		float scaleH = sh / h;
		
		float scale = scaleW;
		if(scaleW > scaleH)scale = scaleH;
		if(scale > 1)scale = 1;
		// 戻す
		sw = mMeasure.getScreenWidth();
		sh = mMeasure.getScreenHeight();
		
		m.reset();
		m.preTranslate(sw/2, sh/2);
		m.preScale(scale, scale);
		m.preTranslate(-w/2, -h/2);		
		
		
	}
	
	private class ScrollAnimator{
		private static final float VELOCITY_THRESHOLD = 5;
		private static final float K = 0.15f;
		
		private float mVx, mVy;
		private Matrix mMatrix = new Matrix();
		
		public boolean onFling(MessageChannel.Fling msg){
			if(!isZoom())return false;
			
			float [] pts = new float[2];
			pts[0] = msg.vx;
			pts[1] = msg.vy;
			float scale = getScale(mDstMatrix);
			pts[0] /= scale;
			pts[1] /= scale;
			
			mVx += pts[0];
			mVy += pts[1];
			
			return true;
		}
		
		public boolean onScroll(MessageChannel.Scroll msg){
			if(!isZoom())return false;
			
			float[] pts = new float[2];
			pts[0] = -msg.dx;
			pts[1] = -msg.dy;
			
			float scale = getScale(mDstMatrix);
			pts[0] /= scale;
			pts[1] /= scale;
			
			mMatrix.preTranslate(pts[0], pts[1]);
			
			return true;
		}
		
		public void reset(){
			mMatrix.reset();
			mVx = 0;
			mVy = 0;
		}
		
		public boolean process(Matrix m){
			if(mVx < VELOCITY_THRESHOLD && mVy < VELOCITY_THRESHOLD){
				m.preConcat(mMatrix);
				reset();
				return false;
			}else{
				mVx -= K * mVx;
				mVy -= K * mVy;
				mMatrix.preTranslate(mVx/30.0f, mVy/30.0f);
				m.preConcat(mMatrix);
				mMatrix.reset();
				return true;
			}
		}
	}
}
