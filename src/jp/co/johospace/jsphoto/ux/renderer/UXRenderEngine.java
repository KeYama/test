package jp.co.johospace.jsphoto.ux.renderer;

import java.util.Collection;

import android.view.MotionEvent;
import android.view.View;

/**
 *
 * 描画系のAbstract factoryと管理。
 * invalidate以外はすべてUXRenderer内部から呼び出す事。
 *
 */
public interface UXRenderEngine {

	/**
	 * 画面更新要求。これが呼び出されるまで描画スレッドは停止状態になる。
	 * これのみレンダリングスレッド外から呼べる。
	 */
	void invalidate();

	int getWidth();
	int getHeight();

	/**
	 * バックグラウンドカラーを設定
	 *
	 * @param color
	 */
	void setBackgroundColor(int color);

	/**
	 * レジューム時に呼ぶメソッド
	 */
	void onResume();

	/**
	 * ポーズ時によぶメソッド
	 */
	void onPause();

	/**
	 * リソースを全部破棄する。
	 *
	 * @param without 破棄例外
	 */
	void disposeAllResources(Collection<UXResource> without);

	/**
	 * 指定された情報でキャンバスリソースを作成する
	 *
	 * @param width
	 * @param height
	 * @param renderer
	 * @param isDirect 直接描画を行うか。ソフトウェアレンダラのみ有効
	 * @return
	 */
	UXCanvasResource createCanvasResource(int width, int height, UXCanvasResource.CanvasRenderer renderer, boolean isDirect);

	/**
	 * イメージリソースを作成する
	 *
	 * @return
	 */
	UXImageResource createImageResource();

	/**
	 * イメージの読み込み完了通知を受け取るリスナを登録する
	 *
	 * @param listener
	 */
	void setImageListener(ImageListener listener);

	/**
	 * 登録すべきビューを得る
	 *
	 * @return
	 */
	View getView();

	/**
	 * 安全にスレッドのリソース等を破棄する
	 */
	void dispose();

	/**
	 * 描画にかかった時間をFPS単位で取得する
	 *
	 * @return
	 */
	int getFps();


	/**
	 * デコードスレッドを指定数増やす
	 * 
	 * @param num
	 */
	void addDecoderThread(int num);
	
	/**
	 * 描画順番を変えてよいかのフラグ
	 *
	 * @param flag
	 */
	void setReorderFlag(boolean flag);

	/**
	 * モーションイベントを受け取るリスナを設定
	 *
	 * @param listener
	 */
	void setMotionListener(MotionListener listener);

	void drawRect(int left, int top, int right, int bottom, int color);

	interface ImageListener{
		/**
		 * イメージ読み込みが完了したときに呼ばれるリスナ
		 *
		 * @param resource
		 * @param info
		 */
		void onLoadImage(UXImageResource resource, Object info);

		/**
		 * イメージ読み込みに失敗したときに呼ばれるリスナ
		 *
		 * @param resource
		 * @param info
		 */
		void onFailedLoadingImage(UXImageResource resource, Object info);
	}

	interface MotionListener{
		/**
		 * 入力を受け取るリスナー
		 *
		 * @param event
		 * @return
		 */
		boolean onMotion(MotionEvent event);
	}
}
