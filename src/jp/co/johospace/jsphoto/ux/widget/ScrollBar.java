package jp.co.johospace.jsphoto.ux.widget;

import java.util.Timer;
import java.util.TimerTask;

import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class ScrollBar {
	private static final long NON_DISPLAY_TIME = 3000;
	private UXCanvasResource mCanvasResource;
	private int mBarWidth;
	private int mBarHeight;
	private Bitmap mImage;
	private Rect mDrawPosition;
	private boolean mIsSeize;
	private boolean mIsOneBeforeSeize;
	private boolean mIsFirstDraw;
	private Timer mTimer;
	private boolean mIsDisplay;	//バーの表示、非表示
	public ScrollBar(UXStage stage, int barWidth, int barHeight, Bitmap image){
		mBarWidth = barWidth;
		mBarHeight = barHeight;
		mImage = image;
		mCanvasResource = stage.getEngine().createCanvasResource(barWidth, barHeight, new BarRenderer(), false);
		mDrawPosition = new Rect();
		mIsFirstDraw = true;
	}
	
	public void recreateResource(UXRenderEngine engine){
		mCanvasResource = engine.createCanvasResource(mBarWidth, mBarHeight, new BarRenderer(), false);
		mCanvasResource.invalidate();
	}

	/**
	 * バーをつかんで移動させた際のviewPortを返します。
	 * @param height
	 * @return
	 */
	public RectF getViewPort(RectF viewPort, float height){

		//比率を計算
		float ratio = (height - viewPort.height()) / (viewPort.height() - mBarHeight);

		float viewHeight = viewPort.height();
		viewPort.top = (float)mDrawPosition.top * ratio;
		viewPort.bottom = viewPort.top + viewHeight;
		return viewPort;
	}

	/**
	 * CanvasResourceを破棄する
	 */
	public void dispose(){
		mCanvasResource.dispose();
		if(mImage != null)
			mImage.recycle();

		if(mTimer != null){
			mTimer.cancel();
			mTimer = null;
		}
	}

	/**
	 * バーの表示、非表示を管理する
	 * @param engine
	 * @param isTap
	 * @param isScroll
	 */
	public void managingDisplay(final UXRenderEngine engine, boolean isTap, boolean isScroll){
		if(!isTap && !isScroll){
			//スクロールが止まった時
			if(mTimer == null && mIsDisplay){

				mTimer = new Timer();
				mTimer.schedule(new TimerTask() {

					@Override
					public void run() {

						//バーを非表示にする
						mIsDisplay = false;
						mTimer = null;
						if(engine != null)
							engine.invalidate();
					}
				}, NON_DISPLAY_TIME);
			}
		}else{

			//スクロールしたときにタイマーが生きていればキャンセルする。
			if(mTimer != null){
				mTimer.cancel();
				mTimer = null;
			}

			//スクロールしてる時は常にバーを表示
			mIsDisplay = true;
		}
	}

	/**
	 * スクロールバーを描画します。
	 */
	
	private static Rect mSrcRect = new Rect();
	public void draw(RectF viewPort, float height, float tapX, float tapY){

		//スクロールする必要が無ければバーを描画しない。
		if(viewPort.height() >= height || !mIsDisplay)
			return;

		Rect src = mSrcRect;
		src.set(0, 0, mBarWidth, mBarHeight);
		Rect dst = null;
		if(mIsSeize || mIsFirstDraw){

			//つかんだ時の表示
			dst = getSeizeDst(viewPort, tapY);

			mCanvasResource.draw(src, dst, 1.0f);
			mDrawPosition = dst;
			mIsFirstDraw = false;
		}else{

			dst = getNotSeizeDst(viewPort, height);
			mCanvasResource.draw(src, dst, 1.0f);
		}
	}

	/**
	 * バーをつかんで移動させた時にバーの位置を調整する。
	 * @param viewPort
	 * @param tapY
	 * @return
	 */
	private static Rect mSizeDstRect = new Rect();
	private Rect getSeizeDst(RectF viewPort, float tapY){
		//トップを設定
		int top = (int)tapY - (mBarHeight / 2);
		//画面上からはみ出ないようにする
		if(top < 0)
			top = 0;
		//画面下からはみ出ないようにする
		else if(top + mBarHeight > viewPort.height())
			top = (int)(viewPort.height() - mBarHeight);

		//画面右端に調整
		int left = (int)viewPort.width() - mBarWidth;
		mSizeDstRect.set(left, top, left + mBarWidth, top + mBarHeight);
		return mSizeDstRect;
	}

	/**
	 * 画面を移動させた時にバーの位置を調整する。
	 * @param viewPort
	 * @param height
	 * @return
	 */
	private Rect getNotSeizeDst(RectF viewPort, float height){

		Rect dst = mDrawPosition;

		//トップとボトムを調整
		float raito = (viewPort.height() - mBarHeight) / (height - viewPort.height());
		dst.top = (int) (viewPort.top * raito);
		dst.bottom = dst.top + mBarHeight;

		//回転時にバーが画面外に下がるのを調整する
		if(dst.bottom > viewPort.height()){

			dst.bottom = (int)viewPort.height();
			dst.top = dst.bottom - mBarHeight;
		}

		//画面右端に調整
		int left = (int)viewPort.width() - mBarWidth;
		dst.left = left;
		dst.right = left + mBarWidth;
		return dst;
	}

	public boolean getSeize(){
		return mIsSeize;
	}

	/**
	 * バーをつかむ
	 * @param tapX
	 * @param tapY
	 */
	public void gradBar(float tapX, float tapY){

		//タップした場所にバーが存在するか確かめる
		if(tapX >= mDrawPosition.left && tapX <= mDrawPosition.right
				&& tapY >= mDrawPosition.top && tapY <= mDrawPosition.bottom){
			mIsOneBeforeSeize = mIsSeize;
			mIsSeize = true;
		}
	}

	/**
	 * バーを離す
	 */
	public void releseBar(){
		mIsOneBeforeSeize = mIsSeize;
		mIsSeize = false;
	}

	public boolean isOneBeforeSeize(){
		return mIsOneBeforeSeize;
	}

	/**
	 * スクロールバーの画像をレンダリングします。
	 */
	private class BarRenderer implements UXCanvasResource.CanvasRenderer{

		private Paint mPaint = new Paint();

		@Override
		public void draw(Canvas canvas) {
			Rect src = new Rect(0, 0, mImage.getWidth(), mImage.getHeight());
			Rect dst = new Rect(0, 0, mBarWidth, mBarHeight);
			canvas.drawBitmap(mImage, src, dst, mPaint);
		}
	}
}