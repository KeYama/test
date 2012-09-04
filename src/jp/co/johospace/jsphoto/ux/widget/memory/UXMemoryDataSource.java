package jp.co.johospace.jsphoto.ux.widget.memory;

/**
 *
 * メモリー表示のデータソース
 * これを元に情報を表示する
 *
 */
public interface UXMemoryDataSource {
	boolean getFavoriteFlag(int category, int item);
	boolean getSecretFlag(int category, int item);
	boolean getSelectFlag(int category, int item);
	int getRotation(int category, int item);
	Object getInfo(int category, int item);
	String getCategoryName(int category);
	boolean isReady(int category, int item);

	int getCategoryItemNumber(int category);
	int getCategoryNumber();
}
