package jp.co.johospace.jsphoto.ux.widget.memory.grid;

import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import jp.co.johospace.jsphoto.ux.widget.memory.UXMemoryDataSource;

import java.util.ArrayList;

/**
 *
 * ヘッダオブジェクトを詰め込むかご
 *
 * ・基本戦略
 * レンダリングしなかったヘッダをリユースする。
 * その判断はcollectUnusedが呼ばれる前にgetされたか否かで行う。
 *
 */
class HeaderBucket {
	private UXRenderEngine mEngine;
	private UXMemoryDataSource mSource;
	private int mHeaderWidth;
	private int mHeaderHeight;

	private ArrayList<Header> mHeaderPool = new ArrayList<Header>();
	private ArrayList<Header> mUsedHeader = new ArrayList<Header>();
	private ArrayList<Header> mHeaders = new ArrayList<Header>();

	HeaderBucket(UXRenderEngine engine, UXMemoryDataSource source, int headerWidth, int headerHeight){
		mEngine = engine;
		mSource = source;

		mHeaderWidth = headerWidth;
		mHeaderHeight = headerHeight;
	}

	int getWidth(){
		return mHeaderWidth;
	}

	int getHeight(){
		return mHeaderHeight;
	}

	void clear(){
		for(Header header: mHeaderPool){
			header.dispose();
		}
		mHeaderPool.clear();

		for(Header header: mUsedHeader){
			header.dispose();
		}
		mUsedHeader.clear();

		for(Header header: mHeaders){
			header.dispose();
		}
		mHeaders.clear();
	}

	Header get(int category){
		int size = mHeaders.size();
		for(int n = 0; n < size; ++n){
			Header header = mHeaders.get(n);
			if(header.getCategory() == category){
				mUsedHeader.add(header);
				mHeaders.remove(n);

				return header;
			}
		}

		size = mUsedHeader.size();
		for(int n = 0; n <size; ++n){
			Header header = mUsedHeader.get(n);
			if(header.getCategory() == category){
				return header;
			}
		}

		Header header = null;
		if(mHeaderPool.size() != 0){
			header = mHeaderPool.remove(0);
		}else{
			header = new Header(mEngine, mSource, mHeaderWidth, mHeaderHeight);
		}
		header.reset(category);

		mUsedHeader.add(header);
		return header;
	}

	void collectUnused(){
		mHeaderPool.addAll(mHeaders);
		mHeaders.clear();

		ArrayList<Header> tmp = mUsedHeader;
		mUsedHeader = mHeaders;
		mHeaders = tmp;
	}
}
