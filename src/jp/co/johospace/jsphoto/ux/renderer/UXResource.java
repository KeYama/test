package jp.co.johospace.jsphoto.ux.renderer;

import android.graphics.Rect;

/**
 * リソースのベースインターフェイス
 */
public interface UXResource {
	/**
	 * 内部メモリを解放する
	 */
	void purgeMemory();

	/**
	 * 破棄。今後使わない場合は必ず呼び出す。
	 */
	void dispose();

	/**
	 * リソースの幅を得る。not 内部表現の幅（テクスチャサイズ等）
	 *
	 * @return
	 */
	int getWidth();

	/**
	 * リソースの高さを得る not 内部表現の幅（テクスチャサイズ等）
	 *
	 * @return
	 */
	int getHeight();

	/**
	 * 指定の大きさに拡大縮小を行って表示を行う
	 *
	 * @param src
	 * @param dst
	 * @param alpha
	 * @return
	 */
	boolean draw(Rect src, Rect dst, float alpha);

	/**
	 * 内部リソースが有効かどうか。
	 * 言い換えると描画可能かどうかを返す
	 *
	 * @return
	 */
	boolean isValid();

}
