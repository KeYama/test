package jp.co.johospace.jsphoto.ux.widget;

import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;

/**
 *
 * コンテナ内に追加できるウィジェットの基本クラス
 *
 */
public class UXWidget {
	public static final int INVALID_ID = Integer.MAX_VALUE;

	protected static final int ORIENTATION_X = 0;
	protected static final int ORIENTATION_Y = 1;

	/** 相対座標（Unit依存） */
	protected float mX, mY;
	/** サイズ（Unit依存） */
	protected float mWidth, mHeight;

	/** 相対座標（Px） */
	protected float mXPx, mYPx;
	/** 世界位置（Px) *Px + World*で実際の座標が算出できる */
	protected float mWorldX, mWorldY;
	/** サイズ（Px） */
	protected float mWidthPx, mHeightPx;

	/** 相対座標の単位系 */
	protected int mXUnit, mYUnit;
	/** サイズの単位系 */
	protected int mWidthUnit, mHeightUnit;

	protected int mId = INVALID_ID;

	private boolean mActiveFlag;
	private UXStage mStage;

	protected UXContainer mParent;

	/**
	 * ステージをもとに再配置を行う
	 *
	 * @param stage
	 */
	public void layout(UXStage stage){
		computeMetrics(stage);
	}

	/**
	 * ステージを設定する
	 *
	 * @param stage
	 */
	public void setStage(UXStage stage){
		mStage = stage;
		if(mId != INVALID_ID && stage != null){
			stage.register(mId, this);
		}
	}

	public UXStage getStage(){
		return mStage;
	}

	public UXWidget id(int myId){
		mId = myId;

		return this;
	}

	public int getId(){
		return mId;
	}

	public float getX(){
		return mXPx;
	}

	public float getY(){
		return mYPx;
	}


	public float getWorldX(){
		return mWorldX;
	}

	public float getWorldY(){
		return mWorldY;
	}

	public UXWidget worldPosition(float x, float y){
		mWorldX = x;
		mWorldY = y;

		return this;
	}


	public void reserve(UXRenderEngine engine, RectF r){
		if(!checkVisible(r)) return;
		activate();
	}

	public boolean onSingleTap(RectF viewPort, MotionEvent e){
		return false;
	}

	public boolean onLongPress(RectF viewPort, MotionEvent e){
		return false;
	}


	private static final RectF tmpRect = new RectF();


	/**
	 * 指定範囲内で見えるかどうかチェックする
	 *
	 * @param viewPort
	 * @return
	 */
	public boolean checkVisible(RectF viewPort){
		tmpRect.set(0, 0, getWidth(), getHeight());
		tmpRect.offset(getAbsX(), getAbsY());

		return tmpRect.intersect(viewPort);
	}

	public float getAbsX(){
		return mXPx + mWorldX;
	}

	public float getAbsY(){
		return mYPx + mWorldY;
	}

	/**
	 * 描画開始の合図。描画前に一回必ず呼ばれる。
	 */
	public void beginDraw(){
		mActiveFlag = false;
	}

	/**
	 * 描画終了の合図。描画後に一回必ず呼ばれる
	 */
	public void endDraw(){
		if(!mActiveFlag){
			deactivate();
		}
	}


	public void loadResource(UXRenderEngine engine){
	}

	public UXWidget position(float x, float y, int unit){
		mX = x;
		mY = y;
		mXUnit = unit;
		mYUnit = unit;

		return this;
	}

	public UXWidget width(float w, int unit){
		mWidth = w;
		mWidthUnit = unit;

		return this;
	}

	public UXWidget height(float h, int unit){
		mHeight = h;
		mHeightUnit = unit;

		return this;
	}

	public UXWidget addTo(UXContainer container){
		container.add(this);

		return this;
	}

	public UXWidget addTo(UXStage stage, int id){
		UXWidget w = stage.findWidgetById(id);
		if(w == null){
			throw new RuntimeException("invalid id");
		}

		if(!(w instanceof UXContainer)){
			throw new RuntimeException("id is not container");
		}

		UXContainer container = (UXContainer)w;
		container.add(this);

		return this;
	}

	public void setParent(UXContainer parent){
		mParent = parent;
	}

	public float getWidth(){
		return mWidthPx;
	}

	public float getHeight(){
		return mHeightPx;
	}

	public void activate(){
		mActiveFlag  = true;
	}


	public void deactivate(){
	}

	public final void draw(UXRenderEngine engine, RectF viewPort){
		if(protectedDraw(engine, viewPort)){
			activate();
		}
	}

	protected boolean protectedDraw(UXRenderEngine engine, RectF viewPort){
		return false;
	}

	public void drawTransparencyImage(int x, int y, RectF viewPort){
		protectedDrawTransImage(x, y, viewPort);
	}

	protected void protectedDrawTransImage(int x, int y, RectF viewPort) {
	}

	private void computeMetrics(UXStage stage){
		mXPx = computeUnit(stage, mX, ORIENTATION_X, mXUnit);
		mYPx = computeUnit(stage, mY, ORIENTATION_Y, mYUnit);
		mWidthPx = computeUnit(stage, mWidth, ORIENTATION_X, mWidthUnit);
		mHeightPx = computeUnit(stage, mHeight, ORIENTATION_Y, mHeightUnit);
	}

	protected float computeUnit(UXStage stage, float value, int orientation, int unit){
		switch(unit){
		case UXUnit.DP:
			return stage.dp2px(value);

		case UXUnit.PX:
			return value;

		case UXUnit.GRID:
			if(mParent == null) return 0;

			if(orientation == ORIENTATION_X){
				return value * mParent.getGridWidth();
			}else{
				return value * mParent.getGridHeight();
			}

		case UXUnit.SCREEN_WIDTH:
			return stage.getScreenWidth() * value;
		}

		return 0;
	}
}
