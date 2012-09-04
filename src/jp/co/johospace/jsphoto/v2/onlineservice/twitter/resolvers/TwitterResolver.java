package jp.co.johospace.jsphoto.v2.onlineservice.twitter.resolvers;

import java.io.IOException;
import java.util.Map;

import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.util.Pair;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;

/**
 * twitter公式リゾルバ
 */
public class TwitterResolver implements MediaUrlResolver {

	@Override
	public String getStorageType() {
		return "twitter.com";
	}

	@Override
	public String resolveFullSizeUrl(Media media) {
		Map<String, String> thumbData =
				JsonUtil.fromJson(media.thumbnailData, STRING_MAP_TYPE);
		String dataUrl = thumbData.get("url");
		return dataUrl + ":large";
	}

	@Override
	public String resolveLargeThumbnailUrl(Media media) {
		Map<String, String> thumbData =
				JsonUtil.fromJson(media.thumbnailData, STRING_MAP_TYPE);
		String dataUrl = thumbData.get("url");
		return dataUrl + ":medium";
	}

	@Override
	public String resolveThumbnailUrl(String thumbnailData, int sizeHint) {
		Map<String, String> thumbData =
				JsonUtil.fromJson(thumbnailData, STRING_MAP_TYPE);
		String dataUrl = thumbData.get("url");
		return dataUrl + ":thumb";
	}
	
	@Override
	public Pair<String, String> resolveVideoUrl(Media media) {
		return null;
	}
	
	@Override
	public String resolveContentType(GetExecutor executor, Media media)
			throws IOException {
		String contentUrl = resolveFullSizeUrl(media);
		if (contentUrl != null) {
			GenericUrl url = new GenericUrl(contentUrl);
			HttpResponse response = executor.executeGet(media.account, url, false);
			return response.getContentType();
		} else {
			return null;
		}
	}

}
