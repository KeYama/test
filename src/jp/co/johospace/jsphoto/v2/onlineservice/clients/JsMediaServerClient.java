package jp.co.johospace.jsphoto.v2.onlineservice.clients;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.AuthPreference;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.CameraPath;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Memory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.RelatedMedia;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.SyncPreference;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.JsMediaUrl;

import com.google.api.client.http.HttpResponse;

/**
 * メディアサーバクライアント
 */
public interface JsMediaServerClient {

	/**
	 * 認証認可設定を取得します。
	 * @param forceRequest サーバへのリクエストを強制する場合true
	 * @return 認証認可設定
	 * @throws IOException
	 */
	List<AuthPreference> getAuthPreferences(boolean forceRequest) throws IOException;
	
	/**
	 * 非同期インデクシングの開始を要求します。
	 * @param serviceType 対象のサービス。全サービスの場合null。
	 * @param serviceAccount 対象のアカウント。全サービスの場合null。
	 * @param async 非同期に行う場合true
	 * @throws IOException
	 */
	void requestIndexing(String serviceType, String serviceAccount, boolean async) throws IOException;
	
	/**
	 * インデックスの再構築を要求します。
	 * @throws IOException
	 */
	void requestRecreateIndex() throws IOException;
	
	/**
	 * 現在のメディアバージョンを取得します。
	 * @param serviceType サービスタイプ
	 * @param serviceAccount サービスアカウント
	 * @return 現在のバージョン。未同期の場合null。
	 * @throws IOException
	 */
	Long getCurrentMediaVersion(String serviceType, String serviceAccount) throws IOException;
	
	/**
	 * メディアディレクトリを取得します。
	 * @param serviceType サービスタイプ
	 * @param serviceAccount サービスアカウント
	 * @param from 差分を要求する場合、開始バージョン。現在の全件を要求する場合null。
	 * @param to 差分を要求する場合、終了バージョン。現在の全件を要求する場合null。
	 * @param mediaLimit 各ディレクトリと同時に取得するメディア数の上限
	 * @param syncExt 外部サービスと同期を行う場合true
	 * @return メディアディレクトリ
	 * @throws IOException
	 */
	List<Directory> getDirectories(String serviceType, String serviceAccount,
			Long from, Long to, int mediaLimit, boolean syncExt) throws IOException;
	
	/**
	 * メディアを取得します。
	 * @param serviceType サービスタイプ
	 * @param serviceAccount サービスアカウント
	 * @param dirId ディレクトリID
	 * @param includeMetadata メタデータを含む場合true
	 * @param from 差分を要求する場合、開始バージョン。現在の全件を要求する場合null。
	 * @param to 差分を要求する場合、終了バージョン。現在の全件を要求する場合null。
	 * @return メディア
	 * @throws IOException
	 */
	List<Media> getMediaList(String serviceType, String serviceAccount, String dirId,
			boolean includeMetadata, Long from, Long to) throws IOException;
	
	/**
	 * キーワード検索を行います。
	 * @param keyword キーワード
	 * @param includeLinkage スケジュール連動画像を含む場合true
	 * @return 検索結果
	 * @throws IOException
	 */
	List<Media> searchMediaByKeyword(String keyword, boolean includeLinkage) throws IOException;
	
	/**
	 * 関連画像を取得します。
	 * @param serviceType サービスタイプ
	 * @param serviceAccount サービスアカウント
	 * @param mediaId メディアID
	 * @return 関連画像
	 * @throws IOException
	 */
	RelatedMedia searchRelatedMedia(String serviceType, String serviceAccount, String mediaId) throws IOException;
	
	/**
	 * 思い出検索を行います。
	 * @return 思い出検索結果
	 * @throws IOException
	 * @throws ContentsNotModifiedException コンテンツに変更がない場合
	 */
	RespondedContents<IOIterator<Memory>> searchMemories(String etag) throws IOException, ContentsNotModifiedException;
	
	/**
	 * ローカルメディアのインデックスを更新します。
	 * @param localMedia ローカルメディア
	 * @param out_next 次回更新。長さ1の配列を渡して、要素0に返る。
	 * @return 更新済みメディア
	 * @throws IOException
	 */
	IOIterator<Media> updateLocalMediaIndices(IOIterator<Media> localMedia, long[] out_next) throws IOException;
	
	/**
	 * カメラのパスリストを取得します。
	 * @return カメラパス
	 * @throws IOException
	 */
	List<CameraPath> getCameraPathList() throws IOException;
	
	/**
	 * 双方向同期を設定します。
	 * @param serviceType サービスタイプ
	 * @param serviceAccount サービスアカウント
	 * @return 双方向同期設定
	 * @throws IOException
	 */
	SyncPreference setupSync(String serviceType, String serviceAccount) throws IOException;
	
	/**
	 * 外部サービスのCredentialを取得します。
	 * @param serviceType サービスタイプ
	 * @param forceRequest サーバにリクエストを強制する場合true
	 * @return キー：アカウント、値：CredentialのJSONシリアライズ
	 * @throws IOException
	 */
	Map<String, String> getExternalServiceCredentials(String serviceType, boolean forceRequest) throws IOException;
	
	/**
	 * メディア管理ローカル用にアカウントを発行します。
	 * @return アカウント情報
	 * @throws IOException
	 */
	Map<String, String> createAccount() throws IOException;
	
	/**
	 * GETリクエストします。
	 * @param url URL
	 * @return レスポンス
	 * @throws IOException
	 */
	HttpResponse executeGet(JsMediaUrl url) throws IOException;
}
