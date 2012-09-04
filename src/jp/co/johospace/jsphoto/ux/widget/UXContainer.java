package jp.co.johospace.jsphoto.ux.widget;

import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;

import java.util.ArrayList;

/**
 * UXScrollViewに追加できるコンテナオブジェクト
 */
public class UXContainer extends UXWidget{
	protected ArrayList<UXWidget> mChildren;
	protected int mGridX, mGridY;

	public UXContainer(){
		mChildren = new ArrayList<UXWidget>();
		mGridX = 1;
		mGridY = 1;
	}

	public UXContainer add(UXWidget widget){
		widget.setParent(this);
		widget.setStage(getStage());
		mChildren.add(widget);
		return this;
	}

	@Override
	public void setStage(UXStage stage) {
		super.setStage(stage);

		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			mChildren.get(n).setStage(stage);
		}
	}

	@Override
	public void reserve(UXRenderEngine engine, RectF r) {
		super.reserve(engine, r);

		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			mChildren.get(n).reserve(engine, r);
		}
	}

	@Override
	protected void protectedDrawTransImage(int x, int y, RectF viewPort) {
		super.protectedDrawTransImage(x, y, viewPort);

		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			mChildren.get(n).drawTransparencyImage(x, y, viewPort);
		}
	}

	public UXContainer divide(int horizontal, int vertical){
		mGridX = horizontal;
		mGridY = vertical;

		return this;
	}

	public float getGridWidth(){
		return getWidth() / mGridX;
	}

	public float getGridHeight(){
		return getHeight() / mGridY;
	}

	@Override
	public void beginDraw() {
		super.beginDraw();

		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			mChildren.get(n).beginDraw();
		}
	}

	@Override
	public void endDraw() {
		super.endDraw();

		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			mChildren.get(n).endDraw();
		}
	}

	@Override
	public boolean protectedDraw(UXRenderEngine engine, RectF viewPort){
		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			mChildren.get(n).draw(engine, viewPort);
		}

		return true;
	}

	@Override
	public boolean onSingleTap(RectF viewPort, MotionEvent e) {
		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			UXWidget widget = mChildren.get(n);
			if(widget.onSingleTap(viewPort, e)){
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean onLongPress(RectF viewPort, MotionEvent e) {
		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			UXWidget widget = mChildren.get(n);
			if(widget.onLongPress(viewPort, e)){
				return true;
			}
		}

		return false;
	}

	@Override
	public void layout(UXStage stage){
		super.layout(stage);
		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			mChildren.get(n).layout(stage);
		}
	}

	@Override
	public void loadResource(UXRenderEngine engine){
		int size = mChildren.size();
		for(int n = 0; n < size; ++n){
			mChildren.get(n).loadResource(engine);
		}
	}

}
