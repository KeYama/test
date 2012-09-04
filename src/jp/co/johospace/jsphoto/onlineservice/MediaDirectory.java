package jp.co.johospace.jsphoto.onlineservice;


/**
 * メディアを格納するディレクトリ
 */
public interface MediaDirectory extends RemoteData {

	/**
	 * リモートディレクトリを一意にあらわすIDを返します。
	 * @return ディレクトリID
	 */
	String getID();
	
	/**
	 * ディレクトリ名を返します。
	 * @return ディレクトリ名
	 */
	String getName();
}
