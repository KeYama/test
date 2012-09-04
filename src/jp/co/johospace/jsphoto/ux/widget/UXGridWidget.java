
package jp.co.johospace.jsphoto.ux.widget;

import java.util.ArrayList;
import java.util.Collections;

import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource.CanvasRenderer;
import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * グリッド表示を行うウィジェット
 */
public class UXGridWidget extends UXWidget{
	private static final int DEFAULT_COLUMN = 3;
	private static final int DEFAULT_WIDTH = 100;
	private static final int SELECTION_COLOR = Color.argb(195, 102, 187, 230);

	private int mThumbnailWidth;
	private UXThumbnailLoader mLoader;
	private UXGridDataSource mSource;
	private int mColumn = DEFAULT_COLUMN;
	private GridItemBucket mBucket;

	private float mGridWidth = DEFAULT_WIDTH;
	private int mGridWidthUnit = UXUnit.PX;
	private float mGridWidthPx = DEFAULT_WIDTH;

	private float mPaddingSize = 0;
	private int mPaddingUnit = UXUnit.NONE;
	private float mPaddingWidthPx = 0;

	private float mGridHeight = DEFAULT_WIDTH;
	private int mGridHeightUnit = UXUnit.PX;
	private float mGridHeightPx = DEFAULT_WIDTH;

	private static Rect mScale = new Rect();
	private static Rect mDst = new Rect();
	private boolean mInvalidateFlag;

	private CornerTapListener mCornerListener;
	private float mCornerSize;
	private int mCornerUnit;
	private float mCornerSizePx;

	/** 描画に使用したアイテム番号 */
	private int mDrawItemNumber = Integer.MAX_VALUE;
	private int mDrawEndItemNumber;

	private ItemTapListener mItemTapListener;
	private ItemLongPressListener mItemLongPressListener;

	/** 選択時に表示する半透明画像**/
	private UXCanvasResource mSelecttionImage;

	public UXGridWidget(int width, UXThumbnailLoader loader){
		mThumbnailWidth = width;
		mLoader = loader;
	}
	
	private float getGridWidth(){
		return mGridWidthPx;
	}
	
	private float getGridHeight(){
		return mGridHeightPx;
	}

	public void setThumbnailWidth(int width) {
		mThumbnailWidth = width;
	}
	
	public UXGridWidget gridWidth(float width, int unit){
		mGridWidth = width;
		mGridWidthUnit = unit;

		//Paddingを無効化
		mPaddingUnit = UXUnit.NONE;

		return this;
	}

	public void setOnCornerTapListener(CornerTapListener listener, float cornerSize, int cornerUnit){
		mCornerListener = listener;
		mCornerSize = cornerSize;
		mCornerUnit = cornerUnit;
	}

	public synchronized void invalidateData(){
		mInvalidateFlag = true;
	}

	private synchronized boolean getAndClearInvalidateFlag(){
		boolean ret = mInvalidateFlag;
		mInvalidateFlag = false;
		return ret;
	}

	public UXGridWidget gridHeight(float height, int unit){
		mGridHeight = height;
		mGridHeightUnit = unit;

		return this;
	}

	public UXGridWidget padding(float p, int unit){
		mPaddingSize = p;
		mPaddingUnit = unit;

		return this;
	}

	public float getItemWorldY(int position){
		int row = position / mColumn;
		return  mGridHeightPx * row
				+ mPaddingWidthPx * (row + 1)
				+ getAbsY();
	}

	public float getItemScreenX(int position){
		int column = position % mColumn;
		return  mGridWidthPx * column +
				mPaddingWidthPx * (column + 1);
	}

	public UXGridWidget gridSize(float size, int unit){
		mGridWidth = mGridHeight = size;
		mGridWidthUnit = mGridHeightUnit = unit;

		return this;
	}

	public UXGridWidget dataSource(UXGridDataSource source){
		mSource = source;
		return this;
	}

	public UXGridWidget column(int num){
		mColumn = num;
		mBucket.clear();
		return this;
	}

	@Override
	public void layout(UXStage stage) {
		mGridWidthPx = computeUnit(stage, mGridWidth, ORIENTATION_X, mGridWidthUnit);
		mGridHeightPx = computeUnit(stage, mGridHeight, ORIENTATION_Y, mGridHeightUnit);

		if(mPaddingUnit == UXUnit.NONE){
			//Widthから算出
			float tmpWidth = stage.getScreenWidth() - mGridWidthPx * mColumn;
			mPaddingWidthPx = tmpWidth / (mColumn + 1);
			if(mPaddingWidthPx < 0)mPaddingWidthPx = 0;
		}else{
			//Paddingから算出
			mPaddingWidthPx = computeUnit(stage, mPaddingSize, ORIENTATION_X, mPaddingUnit);
			float tmpWidth = stage.getScreenWidth() - mPaddingWidthPx * (mColumn + 1);
			mGridWidthPx = tmpWidth / mColumn;
			mGridHeightPx = mGridWidthPx;
			if(mGridWidthPx < 0) mGridWidthPx = 0;
		}

		int row = mSource.getItemCount() / mColumn;
		if(mSource.getItemCount() % mColumn > 0) row ++;
		height(
				mGridHeightPx * row +
						mPaddingWidthPx * (row + 1),
				UXUnit.PX
		);
		width(stage.getScreenWidth(), UXUnit.PX);

		//canvasResのサイズを設定
		if(mSelecttionImage == null){
			mSelecttionImage = createCanvas(stage.getEngine());
			mSelecttionImage.invalidate();
		}else{
			mSelecttionImage.dispose();
			mSelecttionImage = createCanvas(stage.getEngine());
			mSelecttionImage.invalidate();
		}

		mCornerSizePx = computeUnit(stage, mCornerSize, ORIENTATION_X, mCornerUnit);

		mBucket.layout(stage, this);
		super.layout(stage);
	}

	private UXCanvasResource createCanvas(UXRenderEngine engine){

		return engine.createCanvasResource((int)mGridWidthPx, (int)mGridHeightPx, new SelectionCanvasRenderer(), false);
	}
	
	/**
	 * 描画をdrawableに変更になった時に使用する
	 */
	private UXCanvasResource createCanvas(UXRenderEngine engine, Drawable flashImage){
		return engine.createCanvasResource((int)mGridWidthPx, (int)mGridHeightPx, new SelectionCanvasRenderer(flashImage), false);
	}
	
	@Override
	protected void protectedDrawTransImage(int x, int y, RectF viewPort) {

		float localX = x + viewPort.left - getAbsX();
		float localY = y + viewPort.top - getAbsY();

		int itemNum = getItemNumber(localX, localY);

		//画像が存在しなければ半透明画像を表示しない。
		if(itemNum < 0 || itemNum >= mSource.getItemCount())
			return;

		localX = getItemScreenX(itemNum);
		localY = getItemWorldY(itemNum);

		localX = localX - viewPort.left;
		localY = localY - viewPort.top;

		mScale.set(0, 0, 0, 0);
		mDst.set((int)localX, (int)localY, (int)localX +
				(int)mGridWidthPx, (int)localY + (int)mGridHeightPx);

		mSelecttionImage.draw9scale(mScale, mDst);
	}

	@Override
	protected boolean protectedDraw(UXRenderEngine engine, RectF viewPort) {

		if(getAndClearInvalidateFlag()){
			mBucket.clear();
		}

		if(!checkVisible(viewPort))return false;
		if(mSource.getItemCount() == 0)return true;

		int startItem = getStartItem(viewPort);
		int endItem = getEndItem(viewPort);
		
		//範囲外をキャンセル処理
		if(mDrawItemNumber < startItem){
			for(int n = mDrawItemNumber; n < startItem; n++){
				mBucket.cancel(n);
			}
		}
		
		if(mDrawEndItemNumber > endItem){
			for(int n = endItem + 1; n < mDrawEndItemNumber; n++){
				mBucket.cancel(n);
			}
		}

		mDrawItemNumber = startItem;
		mDrawEndItemNumber = endItem;

		for(int n = startItem; n <= endItem; ++n){
			GridItem item = mBucket.get(engine, (int)getItemWorldY(n), n);
			item.draw(engine, this, viewPort, mGridWidthPx, mGridHeightPx);
		}
		return true;
	}

	private class SelectionCanvasRenderer implements CanvasRenderer{

		private Paint paint ;
		private Drawable flashImage;
		
		/**
		 * 描画をdrawableに変更になった時用
		 */
		public SelectionCanvasRenderer(Drawable flashImage) {
			this.flashImage = flashImage;
		}

		public SelectionCanvasRenderer(){
			paint = new Paint();
			paint.setColor(SELECTION_COLOR);
		}

		@Override
		public void draw(Canvas canvas) {

			canvas.drawRect(new Rect(0, 0, (int)mGridWidthPx, (int)mGridHeightPx), paint);
		}
	}

	private int getStartItem(RectF viewPort){
		int startY = (int)(viewPort.top - getAbsY());
		int height = (int)(mGridHeightPx + mPaddingWidthPx);
		int startRow = startY / height - 1;
		int startItem = startRow * mColumn;

		if(startItem >= mSource.getItemCount())startItem = mSource.getItemCount() - 1;
		if(startItem < 0)startItem = 0;

		return startItem;
	}

	private int getEndItem(RectF viewPort){
		int endY = (int)(viewPort.bottom - getAbsY());
		int height = (int)(mGridHeightPx + mPaddingWidthPx);
		int endRow = endY / height + 1;
		int endItem = endRow * mColumn;

		if(endItem >= mSource.getItemCount())
			endItem = mSource.getItemCount() - 1;
		if(endItem < 0)endItem = 0;

		return endItem;
	}

	@Override
	public void deactivate() {
		mBucket.clear();

		super.deactivate();
	}

	@Override
	public void reserve(UXRenderEngine engine, RectF reserveRect) {
		super.reserve(engine, reserveRect);

		int startItem = getStartItem(reserveRect);
		int endItem = getEndItem(reserveRect);

		if(mSource.getItemCount() == 0)return;

		for(int n = mDrawItemNumber; n > startItem; --n){
			mBucket.get(engine,(int)getItemWorldY(n), n);
		}

		for(int n = startItem; n <= endItem; ++n){
			mBucket.get(engine, (int)getItemWorldY(n), n);
		}
	}

	private static final RectF tmpRect = new RectF();

	@Override
	public boolean onSingleTap(RectF viewPort, MotionEvent e) {
		if(!checkVisible(viewPort))return false;

		float localX = e.getX() + viewPort.left - getAbsX();
		float localY = e.getY() + viewPort.top - getAbsY();

		int itemNumber = getItemNumber(localX, localY);

		if(itemNumber >= 0 && itemNumber < mSource.getItemCount()){
			if(mCornerListener != null){
				float cornerX = getItemScreenX(itemNumber) + mGridWidthPx - mCornerSizePx;
				float cornerY = getItemWorldY(itemNumber) - viewPort.top;

				tmpRect.set(cornerX, cornerY, cornerX+mCornerSizePx, cornerY+mCornerSizePx);
				if(tmpRect.contains(e.getX(), e.getY())){
					mCornerListener.onTap(itemNumber);
					return true;
				}
			}

			if(mItemTapListener != null){
				mItemTapListener.onTap(itemNumber);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onLongPress(RectF viewPort, MotionEvent e) {
		if(!checkVisible(viewPort))return false;

		float localX = e.getX() + viewPort.left - getAbsX();
		float localY = e.getY() + viewPort.top - getAbsY();

		int itemNumber = getItemNumber(localX, localY);

		if(itemNumber >= 0 && itemNumber < mSource.getItemCount()){
			if(mItemLongPressListener != null){
				mItemLongPressListener.onLongPress(itemNumber);
				return true;
			}
		}

		return false;
	}

	private int getItemNumber(float localX, float localY){
		float halfPadding = mPaddingWidthPx / 2;
		int x = (int)((localX - halfPadding) / (halfPadding * 2 + mGridWidthPx));
		float y = ((localY - halfPadding) / (halfPadding * 2 + mGridHeightPx));
		if(y < 0f)return -1;

		//Grid表示エリア外だった際にxの値を調整する
		localX = (int)(localX - halfPadding);
		int gridSize = (int)(halfPadding * 2 + mGridWidthPx);
		if(gridSize * mColumn < (int)localX ){
			x = mColumn - 1;
		}

		return (int)y * mColumn + x;
	}

	public UXGridWidget itemType(GridItemFactory factory){
		if(mBucket == null){
			mBucket = new GridItemBucket(factory, mLoader);
		}
		return this;
	}

		/** 破棄命令 */
	private static interface GridItem{
		void dispose();
		/** 空の場合リソースを再確保 */
		void reset(UXRenderEngine engine, UXThumbnailLoader loader,
		           UXGridDataSource source, int width, int position, int itemNumber);
		/** 位置を得る */
		int getItemNumber();
		/** 表示座標を得る */
		int getPosition();
		/** メモリを解放する */
		void purgeMemory();
		boolean cancel();
		/** 描画 */
		void draw(UXRenderEngine engine, UXGridWidget widget, RectF viewport, float width, float height);
	}

	public static interface GridItemFactory{
		/** グリッドアイテムを作成する */
		GridItem create();
		/** レイアウト */
		void layout(UXStage stage, UXGridWidget widget);
	}

	/**
	 *
	 * グリッドアイテムの体系的破棄をつかさどるかご
	 *
	 * 基本戦略：配列は作るが、中のオブジェクトはリユース
	 *
	 */
	private class GridItemBucket{
		private static final int DEFAULT_BUCKET_SIZE = 200;

		private ArrayList<GridItem> mItems = new ArrayList<GridItem>();
		private ArrayList<GridItem> mItemPool = new ArrayList<GridItem>();

		private GridItemFactory mFactory;
		private UXThumbnailLoader mLoader;
		private int mBucketSize = DEFAULT_BUCKET_SIZE;
		private int mItemNum;
		private int mMinItemNum = Integer.MAX_VALUE;
		private int mMaxItemNum = Integer.MIN_VALUE;

		public GridItemBucket(GridItemFactory factory, UXThumbnailLoader loader){
			mFactory = factory;
			mLoader = loader;
		}

		public void layout(UXStage stage, UXGridWidget widget){
			if(mFactory != null)mFactory.layout(stage, widget);
		}

		public void setBucketSize(int size){
			mBucketSize = size;
		}
		
		public void cancel(int num){
			GridItem item = getItem(num);
			if(item != null){
				if(item.cancel()){
					unregisterItem(item);
				}
			}
		}

		private void clear(){
			int size = mItems.size();
			for(int n = 0; n < size; ++n){
				GridItem item = mItems.get(n);
				if(item != null) item.dispose();
				mItems.set(n, null);
			}

			for(GridItem item: mItemPool){
				item.dispose();
			}
			mItemPool.clear();
			mItemNum = 0;
		}

		public GridItem get(UXRenderEngine engine, int pos, int itemNumber){
			GridItem item = getItem(itemNumber);

			if(item == null){
				item = claimItem();
				item.reset(engine, mLoader, mSource, mThumbnailWidth, pos ,itemNumber);

				registerItem(item);
			}

			return item;
		}

		private GridItem getItem(int itemNumber){
			if(mItems.size() - 1 < itemNumber){
				return null;
			}

			return mItems.get(itemNumber);
		}

		private GridItem claimItem(){
			if(mItemPool.size() != 0){
				return mItemPool.remove(0);
			}

			return mFactory.create();
		}

		private void registerItem(GridItem item){
			//配列拡張
			ArrayList<GridItem> items = mItems;
			if(items.size() - 1 < item.getItemNumber()){
				for(int n = items.size()-1; n < item.getItemNumber(); ++n){
					items.add(null);
				}
			}

			//そして登録
			items.set(item.getItemNumber(), item);
			mItemNum++;
			int itemNumber = item.getItemNumber();
			if(mMinItemNum > itemNumber){
				mMinItemNum = itemNumber;
			}
			if(mMaxItemNum < itemNumber){
				mMaxItemNum = itemNumber;
			}

			//はみでた部分を削除処理
			if(mItems.size() != 0 && mItemNum > mBucketSize){
				int dMin = itemNumber - mMinItemNum;
				int dMax = mMaxItemNum - itemNumber;
				if(dMin < dMax){
					//大きいほうから削除
					for(int n = mMaxItemNum; n >= mMinItemNum; --n){
						GridItem toDel = mItems.get(n);
						if(toDel != null){
							unregisterItem(toDel);
							mMaxItemNum = n-1;
							break;
						}
					}
				}else{
					//小さいほうから削除
					for(int n = mMinItemNum; n <= mMaxItemNum; ++n){
						GridItem toDel = mItems.get(n);
						if(toDel != null){
							unregisterItem(toDel);
							mMinItemNum = n+1;
							break;
						}
					}
				}
			}
		}

		private void unregisterItem(GridItem item){
			item.purgeMemory();
			mItemPool.add(item);
			mItems.set(item.getItemNumber(), null);
			mItemNum--;
		}

	}

	public static interface ItemTapListener{
		void onTap(int itemNumber);
	}

	public static interface ItemLongPressListener{
		void onLongPress(int itemNumber);
	}


	public void setOnItemTapListener(ItemTapListener listener){
		mItemTapListener = listener;
	}

	public void setOnItemLongPressListener(ItemLongPressListener listener){
		mItemLongPressListener = listener;
	}


	/**
	 * サムネイルを表示するだけのグリッドアイテムを生成するファクトリ
	 */
	public static class ThumbnailGrid implements GridItemFactory{
		@Override
		public GridItem create() {
			return new ThumbnailGridItem();
		}

		@Override
		public void layout(UXStage stage, UXGridWidget widget) {
			//do nothing
		}

		static class ThumbnailGridItem implements GridItem{
			UXImageResource mResource;
			int mItemNumber;
			int mPosition;
			private static Rect tmpSrc = new Rect();
			private static Rect tmpDst = new Rect();
			@Override
			public void draw(UXRenderEngine engine, UXGridWidget widget, RectF viewPort, float width, float height) {

				//TODO 回転
				int resourceWidth = mResource.getWidth();
				int resourceHeight = mResource.getHeight();

				if(height == 0)return;

				float ratio = width / height;
				float resourceRatio = (float)resourceWidth / (float)resourceHeight;
				int srcWidth = 0;
				int srcHeight = 0;

				if(ratio < resourceRatio){
					//縦を合わせる
					srcHeight = resourceHeight;
					srcWidth = (int)(srcHeight * ratio);
				}else{
					//横を合わせる
					srcWidth = resourceWidth;
					srcHeight = (int)(resourceWidth / ratio);
				}

				int dx = (resourceWidth - srcWidth) / 2;
				int dy = (resourceHeight - srcHeight) / 2;

				tmpSrc.set(dx, dy, srcWidth+dx, srcHeight+dy);

				int ix = (int)widget.getItemScreenX(mItemNumber);
				int iy = (int)(widget.getItemWorldY(mItemNumber) - viewPort.top);

				tmpDst.set(ix, iy, (int)width+ix, (int)height+iy);

				mResource.draw(tmpSrc, tmpDst, 1.0f);
			}
			
			@Override
			public boolean cancel(){
				if(mResource != null){
					return mResource.cancel();
				}
				return false;
			}

			@Override
			public void dispose() {
				if(mResource != null){
					mResource.dispose();
					mResource = null;
				}
			}

			@Override
			public void reset(UXRenderEngine engine, UXThumbnailLoader loader,
			                  UXGridDataSource source, int width, int position, int itemNumber) {
				if(mResource == null){
					mResource = engine.createImageResource();
				}

				mResource.loadImage(source.getInfo(itemNumber), width, loader, null);

				mPosition = position;
				mItemNumber = itemNumber;
			}

			@Override
			public int getItemNumber() {
				return mItemNumber;
			}

			@Override
			public int getPosition() {
				return mPosition;
			}

			@Override
			public void purgeMemory() {
				mResource.purgeMemory();
			}
		}
	}

	public static class OverlayGrid extends ThumbnailGrid{
		public static final int POSITION_LEFT_TOP = 1;
		public static final int POSITION_LEFT_BOTTOM = 2;
		public static final int POSITION_RIGHT_TOP = 3;
		public static final int POSITION_RIGHT_BOTTOM = 4;
		public static final int POSITION_CENTER = 5;

		private ArrayList<ArrayList<OverlayItem>> mOverlayList = new ArrayList<ArrayList<OverlayItem>>();
		private OverlayDataSource mOverlaySource;

		private float mRibbonWidth;
		private int mRibbonUnit;
		private float mRibbonWidthPx;
		private UXCanvasResource mRibbonResource;

		public OverlayGrid(OverlayDataSource source){
			mOverlaySource = source;
		}

		@Override
		public GridItem create() {
			return new OverlayGridItem();
		}

		@Override
		public void layout(UXStage stage, UXGridWidget widget) {
			for(ArrayList<OverlayItem> items: mOverlayList){
				for(OverlayItem item: items){
					item.widthPx = widget.computeUnit(stage, item.width, ORIENTATION_X, item.widthUnit);
					item.heightPx = widget.computeUnit(stage, item.height, ORIENTATION_Y, item.heightUnit);
					item.marginPx = widget.computeUnit(stage, item.margin, ORIENTATION_X, item.marginUnit);
				}
			}

			mRibbonWidthPx = widget.computeUnit(stage, mRibbonWidth, ORIENTATION_Y, mRibbonUnit);
		}

		public void setRibbonWidth(float ribbonWidth, int ribbonUnit){
			mRibbonWidth = ribbonWidth;
			mRibbonUnit = ribbonUnit;
		}

		/**
		 * オーバーレイを追加して、オーバーレイIDを取得する
		 *
		 * @param items setするOverlay
		 * @return id。１から始まり、連番になることを期待してよい。
		 */
		public int addOverlay(OverlayItem[] items){
			ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
			Collections.addAll(overlayItems, items);
			mOverlayList.add(overlayItems);

			return mOverlayList.size();
		}

		private static final Rect tmpScale = new Rect(5,5, 10, 10);
		private static final Rect tmpDst = new Rect();

		class OverlayGridItem extends ThumbnailGridItem{
			@Override
			public void draw(UXRenderEngine engine, UXGridWidget widget, RectF viewPort, float width, float height) {
				super.draw(engine, widget, viewPort, width, height);

				setupResource(engine);

				int itemX = (int)widget.getItemScreenX(mItemNumber);
				int itemY = (int)(widget.getItemWorldY(mItemNumber) - viewPort.top);

				if(mRibbonWidthPx != 0){
					tmpDst.set((int)itemX, (int)(itemY+height-mRibbonWidthPx), (int)(itemX+width), (int)(itemY+height));
					mRibbonResource.draw9scale(tmpScale, tmpDst);
				}

				float leftTop = 0;
				float leftBottom = 0;
				float rightTop = width;
				float rightBottom = width;

				for(int n = 0; n < mOverlayList.size(); ++n){
					ArrayList<OverlayItem> items = mOverlayList.get(n);
					int num = mOverlaySource.getOverlayNumber(mItemNumber, n+1);
					if(num < 0)continue;

					OverlayItem item = items.get(num);

					switch(item.position){
					case POSITION_LEFT_BOTTOM:
						item.resource.draw(
								(int)(itemX+leftBottom+item.marginPx),
								(int)(itemY + height - item.heightPx - item.marginPx));
						leftBottom += item.widthPx + item.marginPx;
						break;
					case POSITION_LEFT_TOP:
						item.resource.draw((int)(itemX+leftTop+item.marginPx), (int)(itemY+item.marginPx));
						leftTop+= item.widthPx + item.marginPx;
						break;
					case POSITION_RIGHT_BOTTOM:
						item.resource.draw(
								(int)(itemX+rightBottom-item.widthPx - item.marginPx),
								(int)(itemY + height - item.heightPx - item.marginPx));
						rightBottom -= item.widthPx + item.marginPx;
						break;
					case POSITION_RIGHT_TOP:
						item.resource.draw(
								(int)(itemX+rightTop-item.widthPx-item.marginPx),
								(int)(itemY+item.marginPx)
						);
						rightTop -= item.widthPx + item.marginPx;
						break;
					case POSITION_CENTER:
						item.resource.draw(
								(int)(itemX+widget.getGridWidth()/2-item.widthPx/2), 
								(int)(itemY+widget.getGridHeight()/2-item.heightPx/2)
						);
						break;
					}
				}
			}
		}

		private void setupResource(UXRenderEngine engine){
			if(mRibbonResource == null){
				mRibbonResource = engine.createCanvasResource(16,16,new CanvasRenderer() {
					@Override
					public void draw(Canvas canvas) {
						canvas.drawColor(Color.argb(30, 0, 0, 0));
					}
				}, false);
			}

			for(ArrayList<OverlayItem> items: mOverlayList){
				for(OverlayItem item: items){
					if(item.resource == null){
						UXCanvasResource res = engine.createCanvasResource(
								(int)item.widthPx, (int)item.heightPx,
								createCanvasRenderer(item), false
						);
						res.invalidate();
						item.resource = res;
					}else{
						return;
					}
				}
			}
		}

		private CanvasRenderer createCanvasRenderer(final OverlayItem item){
			return new CanvasRenderer() {
				@Override
				public void draw(Canvas canvas) {
					Drawable r = item.drawable;
					Rect bounds = new Rect(0, 0, (int)item.widthPx, (int)item.heightPx);
					r.setBounds(bounds);
					r.draw(canvas);
				}
			};
		}
	}
	public static class OverlayItem{
		int position;
		Drawable drawable;
		float width, height;
		int widthUnit, heightUnit;
		float widthPx, heightPx;
		UXCanvasResource resource;
		float margin;
		float marginPx;
		int marginUnit;

		public OverlayItem(Drawable argDrawable, int argPosition,
		                   float argWidth, int argWidthUnit,
		                   float argHeight, int argHeightUnit,
		                   float argMargin, int argMarginUnit
		){
			drawable = argDrawable;
			position = argPosition;

			width = argWidth;
			widthUnit = argWidthUnit;
			height = argHeight;
			heightUnit = argHeightUnit;
			margin = argMargin;
			marginUnit = argMarginUnit;
		}
	}

	public static interface OverlayDataSource{
		/**
		 * 指定されたオーバーレイIDの状態を返す
		 *
		 * @param itemPosition アイテム位置
		 * @param overlayId 表示するオーバーレイID
		 * @return オーバーレイ状態。0~配列数。マイナスの値で描画無し。
		 */
		int getOverlayNumber(int itemPosition, int overlayId);
	}

	public static interface CornerTapListener {
		public void onTap(int itemNumber);
	}
}
