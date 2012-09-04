package jp.co.johospace.jsphoto.ux.loader;

import android.graphics.Bitmap;


/**
 * 実際にイメージを読み込みを担当するクラス
 */
public interface UXThumbnailLoader {
	/**
	 *
	 * キャッシュ読み込み依頼
	 *
	 * @param info これをもとに読み込む
	 * @param sizeHint 長辺の大きさのヒント。これも検索のキーにしてもよい。
	 * @param out 出力。orientationとcompressedImageが埋まっていることを期待される
	 * @return 成功でtrue、失敗、もしくはキャッシュの更新が必要な場合false
	 */
	boolean loadCachedThumbnail(Object info, int sizeHint, UXImageInfo out);

	/**
	 *
	 * サムネイル読み込み依頼
	 *
	 * @param info これをもとに読み込む
	 * @param sizeHint 長辺の大きさのヒント。
	 * @param out 出力。orientationとbitmapが埋まっていることを期待される
	 * @return 成功でtrue、失敗でfalse
	 */
	boolean loadThumbnail(Object info, int sizeHint, UXImageInfo out);

	/**
	 *
	 * キャッシュアップデート依頼
	 *
	 * @param info これをもとに保存する
	 * @param sizeHint 長辺の大きさのヒント。これをキーにしてもよい。
	 * @param in 入力。orientationとcompressedImageが埋まっている。
	 */
	void updateCachedThumbnail(Object info, int sizeHint, UXImageInfo in);
}
