package jp.co.johospace.jsphoto.v2.onlineservice.picasa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.BypassJsMediaClient;
import android.content.Context;
import android.util.Pair;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.gson.reflect.TypeToken;

/**
 * picasa クライアント実装
 */
class PicasaClientImpl extends BypassJsMediaClient implements HttpRequestInitializer, HttpExecuteInterceptor {

	private static final HttpTransport sTransport = AndroidHttp.newCompatibleTransport();
	private final HttpRequestFactory mFactory;
	
	public PicasaClientImpl(Context context) {
		super(context);
		mFactory = sTransport.createRequestFactory();
	}

	@Override
	public String getServiceType() {
		return ServiceType.PICASA_WEB;
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
		public Image[] images;
		public static class Image {
			public int width;
			public int height;
			public String source;
		}
	}

	@Override
	public InputStream getThumbnail(Media media, int sizeHint)
			throws IOException {
		Thumbnail thumb =
				JsonUtil.fromJson(media.thumbnailData, Thumbnail.class);
		String thumbUrl = null;
		if (thumb != null && 0 < thumb.images.length) {
			thumbUrl = thumb.images[0].source;
		}
//		for (Thumbnail.Image image : thumb.images) {
//			thumbUrl = image.source;
//			if (sizeHint < Math.min(image.width, image.height)) {
//				break;
//			}
//		}
		
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
		Thumbnail thumb =
				JsonUtil.fromJson(media.thumbnailData, Thumbnail.class);
		String thumbUrl = null;
		for (Thumbnail.Image image : thumb.images) {
			thumbUrl = image.source;
			if (480 < Math.min(image.width, image.height)) {
				break;
			}
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
	
	protected HttpResponse executeAuthorizedGet(String account, GenericUrl url, boolean includeBody) throws IOException {
//		url.set("access_token", mExtCredentials.get(account));
		HttpRequest request = includeBody ? mFactory.buildGetRequest(url) : mFactory.buildHeadRequest(url);
		request.getHeaders().setAuthorization(
				String.format("Bearer %s", mExtCredentials.get(account)));
		return request.execute();
	}

	@Override
	public void intercept(HttpRequest request) throws IOException {
		request.getHeaders().put("GData-Version", "2");
	}

	@Override
	public void initialize(HttpRequest request) throws IOException {
		request.setInterceptor(this);
	}

}
