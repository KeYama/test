package jp.co.johospace.jsphoto.onlineservice;

import java.io.IOException;
import java.util.Collection;


/**
 * メディア
 */
public interface Media extends RemoteData {

	/**
	 * メディアの識別子を文字列で返します。
	 * @return メディアID
	 */
	String getMediaID();
	
	/**
	 * メディアのタイトルを返します。
	 * @return メディアのタイトル
	 */
	String getTitle();
	
	/**
	 * 同期制御の情報に変換します。
	 * @return メディア同期
	 */
	MediaSync toMediaSync();
	
	/**
	 * 同期状態と比較して、リモートで変更があるかどうかを返します。
	 * @param sync 同期状態
	 * @return 変更がある場合true
	 */
	boolean isDirtyRemotely(MediaSync sync);
	
	/**
	 * メディアコンテンツが渡された時刻より新しいかどうかを返します。
	 * @param millis 比較する時刻
	 * @return メディアコンテンツの時刻が等しいか、より新しい場合true
	 */
	boolean isNewerEqual(long millis);
	
	/**
	 * このメディアに関連するメタデータを返します。
	 * @return　メタデータ
	 * @throws IOException　入出力例外発生時
	 */
	Collection<MediaMetadata> getMetadata() throws IOException;
}
