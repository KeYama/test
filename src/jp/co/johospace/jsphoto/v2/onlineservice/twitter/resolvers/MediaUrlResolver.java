package jp.co.johospace.jsphoto.v2.onlineservice.twitter.resolvers;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.util.Pair;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.gson.reflect.TypeToken;

/**
 * メディアURLのリソルバ
 */
public interface MediaUrlResolver {

	Type STRING_MAP_TYPE =
			new TypeToken<Map<String, String>>() {}.getType();
			
	String HTTP_PATTERN = "(?:http[s]?:\\/\\/)?";

	/**
	 * ストレージタイプを返します。
	 * @return ストレージタイプ
	 */
	String getStorageType();
	
	/**
	 * フルサイズ画像のURLを解決します。
	 * @param media メディア
	 * @return URL
	 */
	String resolveFullSizeUrl(Media media);

	/**
	 * 大サムネイル画像のURLを解決します。
	 * @param media メディア
	 * @return URL
	 */
	String resolveLargeThumbnailUrl(Media media);
	
	/**
	 * サムネイル画像のURLを解決します。
	 * @param thumbnailData サムネイルデータ
	 * @param sizeHint サイズヒント
	 * @return URL
	 */
	String resolveThumbnailUrl(String thumbnailData, int sizeHint);
	
	/**
	 * 動画のURLを解決します。
	 * @param media メディア
	 * @return URLとコンテンツタイプのペア。コンテンツタイプは固定のものがある場合に設定され、nullの場合は動画自体のコンテンツタイプ。
	 */
	Pair<String, String> resolveVideoUrl(Media media);
	
	/**
	 * メディアのコンテンツタイプを解決します。
	 * @param executor リクエスト実行器
	 * @param media メディア
	 * @return コンテンツタイプ
	 * @throws IOException
	 */
	String resolveContentType(GetExecutor executor, Media media) throws IOException;
	
	/**
	 * GETリクエスト実行器
	 */
	interface GetExecutor {
		/**
		 * GETリクエストを実行します。
		 * @param account アカウント
		 * @param url URL
		 * @param includeBody コンテンツボディを含む場合true
		 * @throws IOException
		 */
		HttpResponse executeGet(String account, GenericUrl url, boolean includeBody) throws IOException;
	}
}
