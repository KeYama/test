package jp.co.johospace.jsphoto.onlineservice;

import java.io.IOException;
import java.util.Iterator;

/**
 * 終了可能な反復
 */
public interface TerminatableIterator<E> extends Iterator<E> {

	/**
	 * 反復を終了してリソースを開放します。
	 * @throws IOException 入出力例外発生時
	 */
	void terminate() throws IOException;
	
}
