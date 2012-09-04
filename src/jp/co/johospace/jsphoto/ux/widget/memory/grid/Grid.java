package jp.co.johospace.jsphoto.ux.widget.memory.grid;

import android.graphics.Point;
import android.graphics.Rect;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import jp.co.johospace.jsphoto.ux.widget.memory.UXMemoryDataSource;

/**
 *
 * グリッド
 *
 */
class Grid {
	private UXMemoryDataSource mSource;
	private UXRenderEngine mEngine;
	private GridItemBucket mItemBucket;
	private int mMargin;
	private int mGridWidth;
	private int mGridHeight;
	private int mNumColumns;
	private int mCategoryNumber;

	Grid(int margin, int gridWidth, int gridHeight, int numColumns, int categoryNumber,
		 UXRenderEngine engine, GridItemBucket bucket, UXMemoryDataSource source){
		mMargin = margin;
		mGridWidth = gridWidth;
		mGridHeight = gridHeight;
		mNumColumns = numColumns;
		mCategoryNumber = categoryNumber;
		mSource = source;
		mItemBucket = bucket;
		mEngine = engine;
	}

	private static final Point tmpPoint = new Point();

	void draw(Rect rect, int position){
		int start = getStartNumber(rect, position);
		int end = getEndNumber(rect, start);
		for(int n = start; n <= end; ++n){
			getItemPosition(position, n, tmpPoint);
			GridItem item = mItemBucket.get(tmpPoint.y, mCategoryNumber, n);
			tmpPoint.y -= rect.top;
			item.draw(mGridWidth, mGridHeight, tmpPoint);
		}
	}

	private void getItemPosition(int position, int n, Point out){
		int row = n / mNumColumns;
		int mod = n % mNumColumns;

		int padding = getPadding();

		out.y = row * mGridHeight + (row+1) * mMargin + position;
		out.x = mod * mGridWidth + (mod+1) * padding;
	}

	private int getPadding(){
		int left = mEngine.getWidth() - mNumColumns * mGridWidth;
		return left / (mNumColumns+1);
	}

	private int getStartNumber(Rect rect, int position){
		int height = mMargin + mGridHeight;
		int diff = rect.top - position;
		if(diff < 1)return 0;

		//少し大きめに取る
		int diffRow = diff / height - 1;
		return diffRow * mNumColumns;
	}

	private int getEndNumber(Rect rect, int start){
		int height = mMargin + mGridHeight;
		int rectHeight = rect.height();

		int row = height / rectHeight + 1;
		int count = row * mNumColumns;

		if(count + start < mSource.getCategoryItemNumber(mCategoryNumber))
			return mSource.getCategoryItemNumber(mCategoryNumber) - 1;
		else
			return count + start;
	}

	int getHeight(){
		int numItems = mSource.getCategoryItemNumber(mCategoryNumber);
		int mod = numItems % mNumColumns;
		int numRow = numItems / mNumColumns;
		if(mod != 0)numRow += 1;

		return numRow * mGridHeight + (numRow+1) * mMargin;
	}
}
