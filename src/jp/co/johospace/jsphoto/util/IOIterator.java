package jp.co.johospace.jsphoto.util;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * IOを伴うイテレータ
 * @param <E> 反復要素の型
 */
public interface IOIterator<E> {

	/**
	 * 次の要素を返します。
	 * @return 次の要素
	 * @throws IOException 入出力例外発生時
	 * @throws NoSuchElementException もう要素がない場合
	 */
	E next() throws IOException, NoSuchElementException;
	
	/**
	 * 次の要素があるかどうか調べます。
	 * @return 次の要素がある場合true
	 * @throws IOException 入出力例外発生時
	 */
	boolean hasNext() throws IOException;
	
	/**
	 * イテレーションを終了してリソースを開放します。
	 * @throws IOException 入出力例外発生時
	 */
	void terminate() throws IOException;
}
