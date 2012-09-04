package jp.co.johospace.jsphoto.ux.widget.memory.grid;

import android.graphics.Rect;

/**
 *
 * 表示位置を制御するクラス
 *
 */
class Viewport {
	CategoryList mCategoryList;

	void draw(Rect rect){
		mCategoryList.draw(rect);
	}
}
