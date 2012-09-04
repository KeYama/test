package jp.co.johospace.jsphoto.ux.widget.memory.grid;

import android.graphics.Rect;


/**
 *
 * カテゴリー。グリッド表示とヘッダからなる
 *
 */
class Category {
	private Grid mGrid;
	private int mPosition;
	private HeaderBucket mHeaderBucket;
	private int mCategory;

	private static final Rect tmpRect = new Rect();
	private static final int HEADER_MARGIN_HORIZONTAL = 10;
	private static final int HEADER_PADDING_VERTICAL = 10;

	Category(Grid grid, HeaderBucket headerBucket, int category){
		mGrid = grid;
		mHeaderBucket = headerBucket;
		mCategory  = category;
	}


	void draw(Rect rect){
		computeHeaderRect(tmpRect, mPosition);
		if(tmpRect.intersect(rect)){
			mHeaderBucket.get(mCategory)
					.draw(HEADER_MARGIN_HORIZONTAL, HEADER_PADDING_VERTICAL + mPosition - rect.top);
		}

		mGrid.draw(rect, tmpRect.height() + HEADER_PADDING_VERTICAL + mPosition);
	}

	void computeHeaderRect(Rect r, int position){
		r.set(0,0,mHeaderBucket.getWidth(), mHeaderBucket.getHeight());
		r.offset(HEADER_MARGIN_HORIZONTAL, HEADER_PADDING_VERTICAL);
		r.offset(0, position);
	}

	void layout(int thisPosition){
		mPosition = thisPosition;
	}

	int getHeight(){
		return mGrid.getHeight();
	}
}
