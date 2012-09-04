package jp.co.johospace.jsphoto.v2.onlineservice.twitter.resolvers;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.util.Pair;

/**
 * yfrogリゾルバ
 */
public class YFrogResolver implements MediaUrlResolver {

	public static final Pattern urlPattern =
			Pattern.compile(HTTP_PATTERN + "yfrog\\.com\\/(\\w+)");
	
	@Override
	public String getStorageType() {
		return "yfrog.com";
	}

	@Override
	public String resolveFullSizeUrl(Media media) {
		return resolve(media.thumbnailData, "small");
	}

	@Override
	public String resolveLargeThumbnailUrl(Media media) {
		return resolve(media.thumbnailData, "small");
	}

	@Override
	public String resolveThumbnailUrl(String thumbnailData, int sizeHint) {
		return resolve(thumbnailData, "small");
	}
	
	private String resolve(String thumbnailData, String size) {
		Map<String, String> thumbData =
				JsonUtil.fromJson(thumbnailData, STRING_MAP_TYPE);
		String dataUrl = thumbData.get("url");
		Matcher matcher = urlPattern.matcher(dataUrl);
		if (matcher.find()) {
			return String.format(
					"http://yfrog.com/%s:%s", matcher.group(1), size);
		} else {
			return null;
		}
	}
	
	@Override
	public Pair<String, String> resolveVideoUrl(Media media) {
		return null;
	}
	
	@Override
	public String resolveContentType(GetExecutor executor, Media media)
			throws IOException {
		return null;
	}

}
