package jp.co.johospace.jsphoto.v2.onlineservice.twitter.resolvers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.util.Pair;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;

/**
 * twitpicリゾルバ
 */
public class TwitpicResolver implements MediaUrlResolver {

	public static final Pattern urlPattern =
			Pattern.compile(HTTP_PATTERN + "twitpic\\.com\\/(\\w+)");
	
	@Override
	public String getStorageType() {
		return "twitpic.com";
	}

	@Override
	public String resolveFullSizeUrl(Media media) {
		return resolve(media.thumbnailData, "full");
	}

	@Override
	public String resolveLargeThumbnailUrl(Media media) {
		return resolve(media.thumbnailData, "large");
	}

	@Override
	public String resolveThumbnailUrl(String thumbnailData, int sizeHint) {
		return resolve(thumbnailData, "thumb");
	}
	
	private String resolve(String thumbnailData, String size) {
		Map<String, String> thumbData =
				JsonUtil.fromJson(thumbnailData, STRING_MAP_TYPE);
		String dataUrl = thumbData.get("url");
		Matcher matcher = urlPattern.matcher(dataUrl);
		if (matcher.find()) {
			return String.format(
					"http://twitpic.com/show/%s/%s", size, matcher.group(1));
		} else {
			return null;
		}
	}
	
	@Override
	public Pair<String, String> resolveVideoUrl(Media media) {
		String url = resolve(media.thumbnailData, "video");
		if (url != null) {
			return new Pair<String, String>(url, null);
		} else {
			return null;
		}
	}
	
	@Override
	public String resolveContentType(GetExecutor executor, Media media)
			throws IOException {
		HttpResponse response;
		
		Pair<String, String> videoUrl = resolveVideoUrl(media);
		if (videoUrl != null) {
			try {
				response = executor.executeGet(media.account, new GenericUrl(videoUrl.first), true);
				if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
					return response.getContentType();
				}
			} catch (IOException e) {
//				e.printStackTrace();		/*$debug$*/
				;
			}
		}
		
		response = executor.executeGet(media.account, new GenericUrl(resolveFullSizeUrl(media)), false);
		return response.getContentType();
	}

}
