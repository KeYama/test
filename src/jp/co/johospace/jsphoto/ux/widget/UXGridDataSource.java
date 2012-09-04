package jp.co.johospace.jsphoto.ux.widget;

/**
 *
 * Gridのデータソース
 *
 */
public interface UXGridDataSource {
	/**
	 * オーバーレイ表示のための情報を返す
	 *
	 * @param item 位置
	 * @param number オーバーレイ表示カテゴリ名
	 * @return オーバーレイ情報
	 */
	Object getOverlayInfo(int item, int number);

	/**
	 * 画像の回転具合を返す
	 *
	 * @param item 位置
	 * @return 回転角度、exifと同じ方向で0, 90, 180, 270
	 */
	int getRotation(int item);

	/**
	 * ThumbnailLoaderに渡す情報を返す
	 *
	 * @param item 位置
	 * @return 情報
	 */
	Object getInfo(int item);


	/**
	 * アイテムの数を返す
	 *
	 * @return アイテムの数
	 */
	int getItemCount();
}