package jp.co.johospace.jsphoto.onlineservice;

import android.content.Intent;

/**
 * 認可クライアント
 */
public interface Authorizer {

	/**
	 * サービスの認可を行います。
	 * @param authHandler 認可処理のハンドラ
	 */
	void authorize(AuthorizationHandler authHandler);
	
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
		 * @param authorized 認可された場合true
		 */
		void authorizationFinished(boolean authorized);
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
}
