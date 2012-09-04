package jp.co.johospace.jsphoto.v2.onlineservice.twitter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.BypassJsMediaClient;
import jp.co.johospace.jsphoto.v2.onlineservice.twitter.resolvers.MediaUrlResolver;
import jp.co.johospace.jsphoto.v2.onlineservice.twitter.resolvers.MediaUrlResolverManager;
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
 * twitter クライアント実装
 */
class TwitterClientImpl extends BypassJsMediaClient {

	private static final HttpTransport sTransport = AndroidHttp.newCompatibleTransport();
	private final HttpRequestFactory mFactory;
	
	public TwitterClientImpl(Context context) {
		super(context);
		mFactory = sTransport.createRequestFactory();
	}

	@Override
	public String getServiceType() {
		return ServiceType.TWITTER;
	}

	private final Map<String, String> mExtCredentials =
			Collections.synchronizedMap(new HashMap<String, String>());
	@Override
	public synchronized void setAuthCredentials(Map<String, String> credentials) {
		// twitter投稿画像は外部フォトストレージ上の公開画像を前提としているのでCredentialは不要だろう
		mExtCredentials.clear();
		for (String account : credentials.keySet()) {
			String json = credentials.get(account);
			Map<String, String> credential = JsonUtil.fromJson(json,
					new TypeToken<Map<String, String>>() {}.getType());
			mExtCredentials.put(account, credential.get("access_token"));
		}
	}
	
	private static final Type STRING_MAP_TYPE =
			new TypeToken<Map<String, String>>() {}.getType();

	@Override
	public InputStream getThumbnail(Media media, int sizeHint)
			throws IOException {
		// TODO media.thumbnailData の解析 → sizeHint に最もマッチするURLを選択
		Map<String, String> thumbData =
				JsonUtil.fromJson(media.thumbnailData, STRING_MAP_TYPE);
		String type = thumbData.get("type");
		MediaUrlResolver resolver = MediaUrlResolverManager.getResolver(type);
		if (resolver != null) {
			String thumbUrl = resolver.resolveThumbnailUrl(media.thumbnailData, sizeHint);
			if (thumbUrl != null) {
				GenericUrl url = new GenericUrl(thumbUrl);
				HttpResponse response = executeAuthorizedGet(media.account, url, true);
				return response.getContent();
			}
		}
		return null;
	}
	
	@Override
	public InputStream getLargeThumbnail(Media media) throws IOException {
		Map<String, String> thumbData =
				JsonUtil.fromJson(media.thumbnailData, STRING_MAP_TYPE);
		String type = thumbData.get("type");
		MediaUrlResolver resolver = MediaUrlResolverManager.getResolver(type);
		if (resolver != null) {
			String thumbUrl = resolver.resolveLargeThumbnailUrl(media);
			if (thumbUrl != null) {
				GenericUrl url = new GenericUrl(thumbUrl);
				HttpResponse response = executeAuthorizedGet(media.account, url, true);
				return response.getContent();
			}
		}
		return null;
	}

	
	@Override
	public Pair<String, String> getContentsUrl(Media media, String contentType) {
		Map<String, String> thumbData =
				JsonUtil.fromJson(media.thumbnailData, STRING_MAP_TYPE);
		String type = thumbData.get("type");
		MediaUrlResolver resolver = MediaUrlResolverManager.getResolver(type);
		if (resolver != null) {
			if (contentType != null && contentType.startsWith("video/")) {
				Pair<String, String> videoUrl = resolver.resolveVideoUrl(media);
				if (videoUrl != null) {
					if (videoUrl.second != null) {
						contentType = videoUrl.second;
					}
					return new Pair<String, String>(videoUrl.first, contentType);
				}
			} else {
				String url = resolver.resolveFullSizeUrl(media);
				if (url != null) {
					return new Pair<String, String>(url, contentType);
				}
			}
		}
		
		return null;
	}
	
	private String resolveFullSizeUrl(Media media) {
		Map<String, String> thumbData =
				JsonUtil.fromJson(media.thumbnailData, STRING_MAP_TYPE);
		String type = thumbData.get("type");
		MediaUrlResolver resolver = MediaUrlResolverManager.getResolver(type);
		if (resolver != null) {
			return resolver.resolveFullSizeUrl(media);
		} else {
			return null;
		}
	}
	
	@Override
	public InputStream getMediaContent(Media media, String[] out_contentType)
			throws IOException {
		String contentUrl = resolveFullSizeUrl(media);
		if (contentUrl != null) {
			GenericUrl url = new GenericUrl(contentUrl);
			HttpResponse response = executeAuthorizedGet(media.account, url, true);
			out_contentType[0] = response.getContentType();
			return response.getContent();
		} else {
			out_contentType[0] = null;
			return null;
		}
	}
	
	@Override
	public String getMediaContentType(final Media media) throws IOException {
		Map<String, String> thumbData =
				JsonUtil.fromJson(media.thumbnailData, STRING_MAP_TYPE);
		String type = thumbData.get("type");
		MediaUrlResolver resolver = MediaUrlResolverManager.getResolver(type);
		if (resolver != null) {
			return resolver.resolveContentType(mExecutor, media);
		}
		
		return null;
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
	
	private final MediaUrlResolver.GetExecutor mExecutor = new MediaUrlResolver.GetExecutor() {
		@Override
		public HttpResponse executeGet(String account, GenericUrl url,
				boolean includeBody) throws IOException {
			HttpRequest request = includeBody ? mFactory.buildGetRequest(url) : mFactory.buildHeadRequest(url);
			return request.execute();
		}
	};
	
	protected HttpResponse executeAuthorizedGet(String account, GenericUrl url, boolean includeBody) throws IOException {
		url.set("access_token", mExtCredentials.get(account));
		HttpRequest request = includeBody ? mFactory.buildGetRequest(url) : mFactory.buildHeadRequest(url);
		return request.execute();
	}

}
