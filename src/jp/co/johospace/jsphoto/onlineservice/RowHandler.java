package jp.co.johospace.jsphoto.onlineservice;

import android.database.Cursor;

/**
 * カーソルの行ハンドラ
 * @param <T> 行オブジェクトの型
 */
public interface RowHandler<T> {

	/**
	 * カーソルの現在行をハンドルします。
	 * @param c カーソル
	 * @param row 現在行の内容を格納するオブジェクト
	 */
	void populateCurrentRow(Cursor c, T row);
}
