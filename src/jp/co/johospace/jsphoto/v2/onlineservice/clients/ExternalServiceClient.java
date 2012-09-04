package jp.co.johospace.jsphoto.v2.onlineservice.clients;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import android.util.Pair;




/**
 * 外部サービスクライアント
 */
public interface ExternalServiceClient {

	/**
	 * このクライアントが対象にする外部サービスの識別子を返します。
	 * @return 外部サービスタイプ
	 */
	String getServiceType();
	
	/**
	 * 外部サービスにアクセスするための認証認可の証明情報を設定します。
	 * @param credentials キー：アカウント、値：JSONシリアライズされた証明情報
	 */
	void setAuthCredentials(Map<String, String> credentials);
	
	/**
	 * メディアのメタデータを取得します。
	 * @param account アカウント
	 * @param mediaId メディアID
	 * @return メタデータ
	 * @throws IOException
	 */
	List<Metadata> getMetadata(String account, String mediaId) throws IOException;
	
	/**
	 * メディアのサムネイルを取得します。
	 * @param media メディア
	 * @param sizeHint サムネイルサイズのヒント
	 * @return サムネイルデータのストリーム
	 * @throws IOException
	 */
	InputStream getThumbnail(Media media, int sizeHint) throws IOException;
	
	/**
	 * メディアの大サムネイルを取得します。
	 * @param media メディア
	 * @return サムネイルデータのストリーム
	 * @throws IOException
	 */
	InputStream getLargeThumbnail(Media media) throws IOException;
	
	/**
	 * メディアのコンテンツを取得します。
	 * @param media メディア
	 * @param out_contentType 要素0にコンテンツタイプを返す
	 * @return コンテンツデータのストリーム
	 * @throws IOException
	 */
	InputStream getMediaContent(Media media, String[] out_contentType) throws IOException;
	
	/**
	 * メディアのコンテンツタイプを取得します。
	 * @param media メディア
	 * @return コンテンツタイプ
	 * @throws IOException
	 */
	String getMediaContentType(Media media) throws IOException;
	
	/**
	 * コンテンツURLを取得します。
	 * @param media メディア
	 * @param コンテンツタイプ
	 * @return コンテンツURLとコンテンツタイプのペア。
	 */
	Pair<String, String> getContentsUrl(Media media, String contentType);
	
	/**
	 * メディアを登録します。
	 * @param account アカウント
	 * @param dirId メディアのディレクトリID
	 * @param content メディアデータのストリーム
	 * @param filename ファイル名
	 * @param metadata メディアのメタデータ
	 * @return 登録されたメディア
	 * @throws IOException
	 */
	Media insertMedia(String account, String dirId, InputStream content,
			String filename, Collection<Metadata> metadata) throws IOException;
	
	/**
	 * メディアを更新します。
	 * @param account アカウント
	 * @param mediaId メディアID
	 * @param content メディアデータのストリーム
	 * @param filename ファイル名
	 * @param metadata メディアのメタデータ
	 * @return 更新されたメディア
	 * @throws IOException
	 */
	Media updateMedia(String account, String mediaId, InputStream content,
			String filename, Collection<Metadata> metadata) throws IOException;
	
	/**
	 * メディアを削除します。
	 * @param account アカウント
	 * @param mediaId メディアID
	 * @throws IOException
	 */
	void deleteMedia(String account, String mediaId) throws IOException;
}
