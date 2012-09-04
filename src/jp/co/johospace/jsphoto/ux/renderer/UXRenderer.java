package jp.co.johospace.jsphoto.ux.renderer;

/**
 * レンダリングスレッドの内部
 */
public interface UXRenderer {

	/**
	 * 更新要求がきたときに呼ばれる
	 *
	 * @param engine
	 */
	void render(UXRenderEngine engine);

	/**
	 * 一回だけ呼ばれる。
	 * 初期化したいとき用
	 *
	 * @param engine
	 */
	void initialize(UXRenderEngine engine);

	/**
	 * サーフェイスが変更されたときに呼ばれる
	 *
	 * @param engine
	 */
	void surfaceChanged(UXRenderEngine engine);
}
