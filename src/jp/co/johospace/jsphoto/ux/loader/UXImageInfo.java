package jp.co.johospace.jsphoto.ux.loader;

import android.graphics.Bitmap;

/**
 * 読み込んだイメージを格納するDO
 */
public class UXImageInfo {
	/**
	 * 角度。Exifと同じ回転方向で、0, 90, 180, 270を取る。
	 */
	public int orientation;

	/**
	 * 圧縮済みイメージデータ
	 */
	public byte[] compressedImage;

	/**
	 * 展開済みビットマップ
	 */
	public Bitmap bitmap;
}
