package jp.co.johospace.jsphoto.ux.widget.memory;

/**
 * 思い出表示ビュー
 * ヘッダとグリッド表示（あるいはピンタレスト表示）からなる
 */
public interface UXMemoryView {

	///通知系///

	/**
	 * 指定されたcategoryにnumItems分のアイテムが追加されたことを通知する
	 *
	 * @param category
	 * @param numItems
	 */
	void notifyCategoryItemAdded(int category, int numItems);

	/**
	 * カテゴリがnumCategories分だけ追加されたことを通知する
	 *
	 * @param numCategories
	 */
	void notifyCategoryAdded(int numCategories);

	/**
	 * 特定カテゴリアイテムが変更された事を通知する
	 *
	 * @param category
	 * @param item
	 */
	void notifyItemChanged(int category, int item);

	/**
	 * データソースが大規模に変更された事を通知する
	 */
	void notifyDataSourceChanged();


	///描画系///

	/**
	 * 再描画命令
	 *
	 */
	void invalidate();


	///終了系///

	/**
	 *
	 * 安全にリソースを破棄する
	 *
	 */
	void dispose();
}
