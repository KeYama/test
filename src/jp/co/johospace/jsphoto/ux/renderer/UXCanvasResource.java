package jp.co.johospace.jsphoto.ux.renderer;

import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * キャンバスに自由描画できるオブジェクト
 */
public interface UXCanvasResource extends UXResource{

	/**
	 * キャンバスレンダラ起動命令
	 * キャンバスレンダラに変更があって、再描画が必要な際は常に呼ぶ事
	 * レンダリングスレッド外から呼べる
	 */
	void invalidate();

	/**
	 * 指定座標に描画する
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	boolean draw(int x, int y);

	/**
	 * 9スケール描画
	 *
	 * @param scale スケールするRect
	 * @param dst 描画先
	 * @return
	 */
	boolean draw9scale(Rect scale, Rect dst);

	/**
	 * 中間バッファを使用しているかどうか
	 * ソフトウェアレンダラのみtrueの可能性あり
	 *
	 * @return
	 */
	boolean isDirect();

	/**
	 * 直接描画するかどうか
	 * ソフトウェアレンダラのみtrueの値を取れる
	 *
	 * @return 設定後の値
	 */
	boolean setDirect(boolean flag);

	CanvasRenderer getRenderer();
	void setRenderer(CanvasRenderer renderer);

	interface CanvasRenderer {
		void draw(Canvas canvas);
	}
}
