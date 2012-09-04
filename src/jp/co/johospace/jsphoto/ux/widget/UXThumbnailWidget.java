package jp.co.johospace.jsphoto.ux.widget;

import android.graphics.Rect;
import android.graphics.RectF;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;

/**
 *
 * サムネイル表示を行うウィジェット
 *
 */
public class UXThumbnailWidget extends UXWidget{
	private UXImageResource mResource;
	private UXThumbnailLoader mLoader;
	private Object mInfo;
	private int mWidthHint;
	private boolean mActivated;

	private static final Rect tmpSrc = new Rect();
	private static final Rect tmpDst = new Rect();

	public UXThumbnailWidget(Object info, int widthHint, UXThumbnailLoader loader){
		mInfo = info;
		mLoader = loader;
		mWidthHint = widthHint;
	}

	@Override
	public void loadResource(UXRenderEngine engine){
		if(mResource == null){
			mResource = engine.createImageResource();
		}
	}

	@Override
	public void activate() {
		super.activate();

		if(!mActivated && mResource != null){
			mResource.loadImage(mInfo, mWidthHint,  mLoader, null);

			mActivated = true;
		}
	}

	@Override
	public void deactivate() {
		super.deactivate();

		mResource.purgeMemory();
		mActivated = false;
	}

	@Override
	public boolean protectedDraw(UXRenderEngine engine, RectF viewPort){
		if(!checkVisible(viewPort))return false;
		if(mResource == null || !mResource.isValid()) return true;

		float ratio = mWidthPx / mHeightPx;
		float srcRatio = (float)mResource.getWidth() / (float)mResource.getHeight();
		int srcW = 0;
		int srcH = 0;

		if(ratio < srcRatio){
			//縦をあわせる
			srcH = mResource.getHeight();
			srcW = (int)(srcH * ratio);
		}else{
			//横をあわせる
			srcW = mResource.getWidth();
			srcH = (int)(srcW / ratio);
		}

		int dx = (mResource.getWidth() - srcW) / 2;
		int dy = (mResource.getHeight() - srcH) / 2;

		tmpSrc.set(dx, dy, srcW + dx, srcH + dy);

		float dstX = mXPx + mWorldX - viewPort.top;
		float dstY = mYPx + mWorldY - viewPort.left;

		tmpDst.set(
				(int)(dstX),
				(int)(dstY),
				(int)(dstX + mWidthPx),
				(int)(dstY + mHeightPx)
		);

		mResource.draw(tmpSrc, tmpDst, 1);
		return true;
	}
}
