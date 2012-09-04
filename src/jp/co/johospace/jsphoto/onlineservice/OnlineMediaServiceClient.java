package jp.co.johospace.jsphoto.onlineservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import jp.co.johospace.jsphoto.util.IOIterator;

import android.content.Intent;

/**
 * オンラインのメディアストレージサービスのクライアント
 */
public interface OnlineMediaServiceClient<D extends MediaDirectory, M extends Media> {

	/**
	 * サービスの認可を行います。
	 * @param authHandler 認可処理のハンドラ
	 * @param expired 有効期限切れの場合true
	 */
	void authorize(AuthorizationHandler authHandler, boolean expired);
	
	/**
	 * このクライアントが対象とするオンラインサービスの識別子を返します。
	 * @return サービスタイプ
	 */
	String getServiceType();
	
	/**
	 * このクライアントインスタンスが使うアカウントを返します。
	 * @return アカウント
	 */
	String getServiceAccount();
	
	/**
	 * ディレクトリを列挙します。
	 * @return メディアディレクトリの反復
	 * @throws IOException 入出力例外発生時
	 */
	IOIterator<D> iterateDirectory() throws IOException;
	
	/**
	 * メディアを列挙します。
	 * @param directoryID ディレクトリID
	 * @return メディアの反復
	 * @throws IOException 入出力例外発生時
	 */
	IOIterator<M> iterateMedia(String directoryID) throws IOException;
	
	/**
	 * 現在のメディアデータを取得します。
	 * @param sync 同期データ
	 * @return メディア
	 * @throws IOException 入出力例外発生時
	 */
	M getMedia(MediaSync sync) throws IOException;
	
	/**
	 * メディアコンテンツをオープンします。
	 * @param media メディア
	 * @return コンテンツストリーム
	 * @throws IOException 入出力例外発生時
	 */
	InputStream openContent(Media media) throws IOException;
	
	/**
	 * メディアを登録します。
	 * @param directoryID ディレクトリID
	 * @param name メディアの名前
	 * @param content メディアコンテンツ
	 * @param metadata メタデータ
	 * @return メディア
	 * @throws IOException 入出力例外発生時
	 */
	M insertMedia(String directoryID, String name, InputStream content, Collection<MediaMetadata> metadata) throws IOException;
	
	/**
	 * メディアを更新します。
	 * @param sync メディア同期
	 * @param content メディアコンテンツ
	 * @param metadata メタデータ
	 * @throws IOException 入出力例外発生時
	 */
	M updateMedia(MediaSync sync, InputStream content, Collection<MediaMetadata> metadata) throws IOException;
	
	/**
	 * メディアを削除します。
	 * @param sync メディア同期
	 * @throws IOException 入出力例外発生時
	 */
	void deleteMedia(MediaSync sync) throws IOException;
	
	/**
	 * 認可処理をハンドリングします。
	 */
	interface AuthorizationHandler {
		
		/**
		 * ユーザとの対話を開始します。
		 * @param intent 開始要求
		 */
		void startInteraction(Intent intent, InteractionCallback callback);
		
		/**
		 * 認可処理が終了しました。
		 * @param account アカウント
		 * @param authorized 認可された場合true
		 */
		void authorizationFinished(String account, boolean authorized);
	}
	
	/**
	 * ユーザとの対話をコールバックします。
	 */
	interface InteractionCallback {
		/**
		 * ユーザとの対話が終わり、結果が得られました。
		 * @param resultCode 結果コード
		 * @param data 結果
		 */
		void onInteractionResult(int resultCode, Intent data);
	}
	
	/**
	 * メディアをシリアライズします。
	 * @param media メディア
	 * @return シリアライズデータ。サポートしない場合null。
	 */
	byte[] serializeMedia(Media media);
	
	/**
	 * メディアをデシリアライズします。
	 * @param data シリアライズデータ
	 * @return メディア
	 * @throws UnsupportedOperationException サポートしない場合
	 */
	M deserializeMedia(byte[] data) throws UnsupportedOperationException;
}
