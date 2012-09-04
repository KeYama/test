package jp.co.johospace.jsphoto.v2.onlineservice.twitter.resolvers;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.util.Pair;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;

/**
 * mobyリゾルバ
 */
public class MobyPictureResolver implements MediaUrlResolver {

	public static final Pattern urlPattern =
			Pattern.compile(HTTP_PATTERN + "moby\\.to\\/(\\w+)");
	
	@Override
	public String getStorageType() {
		return "moby.to";
	}

	@Override
	public String resolveFullSizeUrl(Media media) {
		return resolve(media.thumbnailData, "full");
	}

	@Override
	public String resolveLargeThumbnailUrl(Media media) {
		return resolve(media.thumbnailData, "medium");
	}

	@Override
	public String resolveThumbnailUrl(String thumbnailData, int sizeHint) {
		return resolve(thumbnailData, "square");
	}
	
	private String resolve(String thumbnailData, String size) {
		Map<String, String> thumbData =
				JsonUtil.fromJson(thumbnailData, STRING_MAP_TYPE);
		String dataUrl = thumbData.get("url");
		Matcher matcher = urlPattern.matcher(dataUrl);
		if (matcher.find()) {
			return String.format(
					"http://moby.to/%s:%s", matcher.group(1), size);
		} else {
			return null;
		}
	}
	
	@Override
	public Pair<String, String> resolveVideoUrl(Media media) {
		String url = resolveFullSizeUrl(media);
		return new Pair<String, String>(url, null);
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
