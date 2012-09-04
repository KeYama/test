package jp.co.johospace.jsphoto.fullscreen.loader;

import android.graphics.Bitmap;

/**
 * 画像読み込みインターフェイス
 * 
 * ImageLoaderFactoryから生成される。
 * 
 */
public interface ImageLoader {
	/**
	 * フルサイズのイメージを読み込む。
	 *
	 * @param tag 読み込みに必要なタグ名
	 * @return 失敗時にはnull
	 */
	public Bitmap loadFullImage(Object tag);
	
	/**
	 * スクリーンサイズをヒントにサムネイル画像を読み込む
	 * 
	 * @param tag
	 * @param screenWidth
	 * @param screenHeight
	 * @return 失敗時にはnull
	 */
	public Bitmap loadThumbnailImage(Object tag, int screenWidth, int screenHeight);
	
	/**
	 * 指定タグの読み込みがもう必要なくなった時に呼ばれる。
	 * 既に読み込み済みのタグに関しても呼ばれる事がある。
	 * 正確にキャンセルする必要は無く、読み込みを続けても問題は無い。
	 * 
	 * load系とは別スレッドから呼ばれる。さらに、すぐに戻ってくる事。
	 * 
	 * @param tag
	 */
	public void cancel(Object tag);
}
