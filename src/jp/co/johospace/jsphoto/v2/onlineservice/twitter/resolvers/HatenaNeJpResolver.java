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
 * はてなリゾルバ
 */
public class HatenaNeJpResolver implements MediaUrlResolver {

	public static final Pattern urlPattern =
			Pattern.compile(HTTP_PATTERN + "f\\.hatena\\.ne\\.jp\\/([\\w\\-]+)/(\\d{8})(\\d+)");
	
	@Override
	public String getStorageType() {
		return "f.hatena.ne.jp";
	}

	@Override
	public String resolveFullSizeUrl(Media media) {
		String resolved = resolve(media.thumbnailData, "");
		return resolved != null ? resolved + ".jpg" : resolved;
	}

	@Override
	public String resolveLargeThumbnailUrl(Media media) {
		String resolved = resolve(media.thumbnailData, "");
		return resolved != null ? resolved + ".jpg" : resolved;
	}

	@Override
	public String resolveThumbnailUrl(String thumbnailData, int sizeHint) {
		String resolved = resolve(thumbnailData, "_120");
		return resolved != null ? resolved + ".jpg" : resolved;
	}
	
	private String resolve(String thumbnailData, String size) {
		Map<String, String> thumbData =
				JsonUtil.fromJson(thumbnailData, STRING_MAP_TYPE);
		String dataUrl = thumbData.get("url");
		Matcher matcher = urlPattern.matcher(dataUrl);
		if (matcher.find()) {
			String userId = matcher.group(1);
			String ymd = matcher.group(2);
			String imageId = matcher.group(3);
			return String.format(
					"http://img.f.hatena.ne.jp/images/fotolife/%s/%s/%s/%s%s%s",
					userId.substring(0, 1), userId, ymd, ymd, imageId, size);
		} else {
			return null;
		}
	}
	
	@Override
	public Pair<String, String> resolveVideoUrl(Media media) {
		return new Pair<String, String>(media.mediaUri, "text/html");
	}
	
	@Override
	public String resolveContentType(GetExecutor executor, Media media)
			throws IOException {
		String resolved = resolve(media.thumbnailData, "");
		
		HttpResponse response;
		try {
			response = executor.executeGet(media.account, new GenericUrl(resolved + ".flv"), false);
			if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
				return "video/-";
			}
		} catch (IOException e) {
			;
		}
		
		return "image/jpeg";
	}

}
