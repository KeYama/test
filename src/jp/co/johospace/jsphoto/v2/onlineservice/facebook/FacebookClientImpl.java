package jp.co.johospace.jsphoto.v2.onlineservice.facebook;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.BypassJsMediaClient;
import android.content.Context;
import android.util.Pair;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.gson.reflect.TypeToken;

/**
 * facebook クライアント実装
 */
class FacebookClientImpl extends BypassJsMediaClient {

	private static final HttpTransport sTransport = AndroidHttp.newCompatibleTransport();
	private final HttpRequestFactory mFactory;
	
	public FacebookClientImpl(Context context) {
		super(context);
		mFactory = sTransport.createRequestFactory();
	}

	@Override
	public String getServiceType() {
		return ServiceType.FACEBOOK;
	}

	private final Map<String, String> mExtCredentials =
			Collections.synchronizedMap(new HashMap<String, String>());
	@Override
	public synchronized void setAuthCredentials(Map<String, String> credentials) {
		mExtCredentials.clear();
		for (String account : credentials.keySet()) {
			String json = credentials.get(account);
			// TODO 実際の形式に合わせる
			Map<String, String> credential = JsonUtil.fromJson(json,
					new TypeToken<Map<String, String>>() {}.getType());
			mExtCredentials.put(account, credential.get("access_token"));
		}
	}
	
	public static class Thumbnail {
		public int height;
		public int width;
		public String source;
		
		static final Type LIST_TYPE =
				new TypeToken<List<Thumbnail>>() {}.getType();
	}

	@Override
	public InputStream getThumbnail(Media media, int sizeHint)
			throws IOException {
		List<Thumbnail> thumbnails =
				JsonUtil.fromJson(media.thumbnailData, Thumbnail.LIST_TYPE);
		String thumbUrl = null;
		for (Thumbnail thumb : thumbnails) {
			if (Math.min(thumb.width, thumb.height) < sizeHint) {
				break;
			}
			thumbUrl = thumb.source;
		}
		
		if (thumbUrl != null) {
			GenericUrl url = new GenericUrl(thumbUrl);
			HttpResponse response = executeAuthorizedGet(media.account, url, true);
			return response.getContent();
		} else {
			return null;
		}
	}

	@Override
	public InputStream getLargeThumbnail(Media media) throws IOException {
		List<Thumbnail> thumbnails =
				JsonUtil.fromJson(media.thumbnailData, Thumbnail.LIST_TYPE);
		String thumbUrl = null;
		for (Thumbnail thumb : thumbnails) {
			if (Math.min(thumb.width, thumb.height) < 480) {
				break;
			}
			thumbUrl = thumb.source;
		}
		
		if (thumbUrl != null) {
			GenericUrl url = new GenericUrl(thumbUrl);
			HttpResponse response = executeAuthorizedGet(media.account, url, true);
			return response.getContent();
		} else {
			return null;
		}
	}
	
	@Override
	public Pair<String, String> getContentsUrl(Media media, String contentType) {
		return new Pair<String, String>(media.mediaUri, contentType);
	}
	
	@Override
	public InputStream getMediaContent(Media media, String[] out_contentType)
			throws IOException {
		GenericUrl url = new GenericUrl(media.mediaUri);
		HttpResponse response = executeAuthorizedGet(media.account, url, true);
		out_contentType[0] = response.getContentType();
		return response.getContent();
	}
	
	@Override
	public String getMediaContentType(Media media) throws IOException {
		GenericUrl url = new GenericUrl(media.mediaUri);
		HttpResponse response = executeAuthorizedGet(media.account, url, false);
		return response.getContentType();
	}

	@Override
	public Media insertMedia(String account, String dirId, InputStream content,
			String filename, Collection<Metadata> metadata) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Media updateMedia(String account, String mediaId,
			InputStream content, String filename, Collection<Metadata> metadata)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteMedia(String account, String mediaId) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	protected HttpResponse executeAuthorizedGet(String account, GenericUrl url, boolean includeBody) throws IOException {
		url.set("access_token", mExtCredentials.get(account));
		HttpRequest request = includeBody ? mFactory.buildGetRequest(url) : mFactory.buildHeadRequest(url);
		return request.execute();
	}

}
