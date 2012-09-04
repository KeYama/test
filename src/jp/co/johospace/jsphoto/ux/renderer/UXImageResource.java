package jp.co.johospace.jsphoto.ux.renderer;

import android.graphics.Bitmap;
import android.graphics.Rect;
import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;

/**
 * 実際の画像の描画に関わるリソースを描画するオブジェクト
 */
public interface UXImageResource extends UXResource{
	/** loader内部使用用のインターフェイス */
	void setImage(Object info, Bitmap bitmap, byte[] compressed, int orientation);

	/**
	 * 自動回転を行うかどうか
	 *
	 * @param flag 自動回転するか否か
	 */
	void setAutoRotation(boolean flag);

	/**
	 * データソースを変更してイメージロードを開始する
	 *
	 * @param info
	 * @param widthHint
	 */
	void loadImage(Object info, int widthHint, UXThumbnailLoader loader, ImageCallback callback);

	/**
	 * 描画する。rotationが追加されている。
	 * rotationは0, 90, 180, 270の4種類
	 *
	 * @param src
	 * @param dst
	 * @param alpha
	 * @param rotation
	 */
	void draw(Rect src, Rect dst, float alpha, int rotation);

	interface ImageCallback{
		void onLoad(UXImageResource resource);
		void onFailed(UXImageResource resource);
	}
	
	boolean cancel();
}
