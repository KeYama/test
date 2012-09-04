package jp.co.johospace.jsphoto.ux.widget.memory.grid;

import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import jp.co.johospace.jsphoto.ux.widget.memory.UXMemoryDataSource;

import java.util.ArrayList;

/**
 * GridItemを溜め込むかご
 *
 * ・基本戦略
 * 配列は確保するが、中のオブジェクトはリユースするスタイル。
 *
 */
class GridItemBucket {
	private UXMemoryDataSource mSource;
	private UXRenderEngine mEngine;
	private int mWidth;
	private int mBucketSize;

	private ArrayList<ArrayList<GridItem>> mItemMap = new ArrayList<ArrayList<GridItem>>();
	private ArrayList<GridItem> mItems = new ArrayList<GridItem>();
	private ArrayList<GridItem> mItemPool = new ArrayList<GridItem>();
	private ArrayList<UXImageResource> mResourcePool = new ArrayList<UXImageResource>();

	GridItemBucket(UXMemoryDataSource source, UXRenderEngine engine, int width, int bucketSize){
		mSource = source;
		mEngine = engine;
		mWidth = width;
		mBucketSize  =bucketSize;
	}

	void clear(){
		mItemMap.clear();

		for(GridItem item: mItems){
			item.dispose();
		}
		mItems.clear();

		for(GridItem item: mItemPool){
			item.dispose();
		}
		mItemPool.clear();

		for(UXImageResource res: mResourcePool){
			res.dispose();
		}
		mResourcePool.clear();
	}

	void reset(int width, int bucketSize){
		clear();

		mWidth = width;
		mBucketSize = bucketSize;
	}

	GridItem get(int position, int category, int itemNumber){
		GridItem item = getItem(category, itemNumber);

		if(item == null){
			item = claimItem();
			item.reset(claimResource(category, itemNumber), category, itemNumber, position);

			registerItem(item);
		}

		return item;
	}

	/**
	 *
	 * 既に存在するグリッドアイテムを得る
	 *
	 * @param category
	 * @param itemNumber
	 * @return
	 */
	private GridItem getItem(int category, int itemNumber){
		if(mItemMap.size() - 1 < category){
			return null;
		}
		ArrayList<GridItem> mItems = mItemMap.get(category);
		if(mItems.size() - 1 < itemNumber){
			return null;
		}

		return mItems.get(itemNumber);
	}

	/**
	 *
	 * グリッドアイテムを要求する。作ったりプールから取りにいく。
	 *
	 * @return
	 */
	private GridItem claimItem(){
		if(mItemPool.size() != 0){
			return mItemPool.remove(0);
		}

		return new GridItem();
	}

	/**
	 *
	 * リソースを要求する。作ったりプールから取りにいく
	 *
	 * @param category
	 * @param itemNumber
	 * @return
	 */
	private UXImageResource claimResource(int category, int itemNumber){
		UXImageResource res = null;
		if(mResourcePool.size() != 0){
			res =  mResourcePool.remove(0);
		}else{
			res = mEngine.createImageResource();
		}
		res.loadImage(mSource.getInfo(category, itemNumber), mWidth, null, null);
		return res;
	}

	private void registerItem(GridItem item){
		//配列拡張
		if(mItemMap.size() - 1 < item.getCategory()){
			for(int n = mItemMap.size()-1; n <= item.getCategory(); ++n){
				mItemMap.add(new ArrayList<GridItem>());
			}
		}
		ArrayList<GridItem> items = mItemMap.get(item.getCategory());
		if(items.size() - 1 < item.getItemNumber()){
			for(int n = items.size()-1; n < item.getItemNumber(); ++n){
				items.add(null);
			}
		}

		//そして登録
		items.set(item.getItemNumber(), item);


		//はみでた部分を削除処理
		if(mItems.size() != 0 && mItems.size() > mBucketSize){
			GridItem head = mItems.get(0);
			GridItem tail = mItems.get(mItems.size() - 1);
			if(head.getPosition() >= item.getPosition()){
				//ヘッドより前にあるので、ヘッドに追加してしっぽを削除
				mItems.add(0, item);
				unregisterItem(mItems.remove(mItems.size() - 1));
			}else if(tail.getPosition() <= item.getPosition()){
				//しっぽより後ろにあるので、しっぽに追加してヘッドを削除
				mItems.add(item);
				unregisterItem(mItems.remove(0));
			}else{
				//挿入ソート。削除位置はあたま固定
				int size = mItems.size();
				for(int n = 0; n < size; ++n){
					if(mItems.get(n).getPosition() <= item.getPosition()){
						mItems.add(n, item);
						unregisterItem(mItems.remove(0));
					}

					//しっぽより後ろにあることはないのでその条件は考える必要無し
				}
			}
		}
	}

	/**
	 * グリッドアイテムの登録を解除して、リソースをプールにためる
	 *
	 * @param item
	 */
	private void unregisterItem(GridItem item){
		item.purgeMemory();
		mItemPool.add(item);
		mItemMap.get(item.getCategory()).set(item.getItemNumber(), null);
		mResourcePool.add(item.getResource());
	}
}
