package jp.co.johospace.jsphoto.ux.widget.memory.grid;

import android.graphics.Rect;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import jp.co.johospace.jsphoto.ux.widget.memory.UXMemoryDataSource;
import jp.co.johospace.jsphoto.ux.widget.memory.UXMemoryView;

/**
 *
 * グリッド表示版のグリッドビュー
 *
 */
public class UXGridMemoryView implements UXMemoryView {
	private UXRenderEngine mEngine;
	private Grid mGrid;
	private UXMemoryDataSource mSource;
	private GridItemBucket mItemBucket;
	private HeaderBucket mHeaderBucket;
	private Category mCategory;

	public UXGridMemoryView(UXRenderEngine engine, UXMemoryDataSource source){
		mSource = source;
		mEngine  = engine;
		mItemBucket = new GridItemBucket(source, engine, 200, 50);
		mGrid = new Grid(10, 200, 200, 3, 0, mEngine, mItemBucket, mSource);

		mHeaderBucket = new HeaderBucket(engine, source, 512, 50);
		mCategory = new Category(mGrid, mHeaderBucket, 0);
	}

	@Override
	public void dispose() {
		mItemBucket.clear();
		mHeaderBucket.clear();
	}

	@Override
	public void invalidate() {
		mCategory.draw(new Rect(0,0, mEngine.getWidth(), mEngine.getHeight()));

		mHeaderBucket.collectUnused();
	}

	@Override
	public void notifyCategoryItemAdded(int category, int numItems) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void notifyCategoryAdded(int numCategories) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void notifyItemChanged(int category, int item) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void notifyDataSourceChanged() {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
