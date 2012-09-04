package jp.co.johospace.jsphoto.ux.widget.memory.grid;

import android.graphics.Point;
import android.graphics.Rect;
import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;

/**
 *
 * グリッドの個々のアイテム
 *
 */
class GridItem {
	UXImageResource mResource;
	int mCategory;
	int mItemNumber;
	int mPosition;

	GridItem(){
	}

	void reset(UXImageResource resource, int category, int itemNumber, int position){
		mResource = resource;
		mCategory = category;
		mItemNumber = itemNumber;
		mPosition  = position;
	}

	void purgeMemory(){
		if(mResource != null)
			mResource.purgeMemory();
	}

	void dispose(){
		if(mResource != null)
			mResource.dispose();
	}

	int getPosition(){
		return mPosition;
	}

	int getCategory(){
		return mCategory;
	}

	int getItemNumber(){
		return mItemNumber;
	}

	UXImageResource getResource(){
		return mResource;
	}

	private static final Rect tmpSrc = new Rect();
	private static final Rect tmpDst = new Rect();

	void draw(int width, int height, Point p){
		float ratio = (float)width / (float)height;
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
		tmpDst.set(p.x, p.y, p.x+width, p.y+height);

		mResource.draw(tmpSrc, tmpDst, 1);
	}
}
