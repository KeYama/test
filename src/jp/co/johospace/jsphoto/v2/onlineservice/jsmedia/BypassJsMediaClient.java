package jp.co.johospace.jsphoto.v2.onlineservice.jsmedia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.JsMediaAuth.Credential;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.gson.reflect.TypeToken;

/**
 * メディアサーバを経由する外部サービスクライアント
 */
public abstract class BypassJsMediaClient extends AbstractJsMediaClient implements ExternalServiceClient {

	protected BypassJsMediaClient(Context context) {
		super(context);
	}
	
	public abstract String getServiceType();
	
	@Override
	public void setAuthCredentials(Map<String, String> credentials) {
	}

	@Override
	public List<Metadata> getMetadata(String account, String mediaId)
			throws IOException {
		JsMediaUrl url = JsMediaUrl.mediaMetadata(mContext);
		url.type = getServiceType();
		url.account = account;
		url.mediaId = mediaId;
		
		HttpResponse response = executeGet(url);
		return JsonUtil.fromJson(response.parseAsString(),
				new TypeToken<List<Metadata>>() {}.getType());
	}

	@Override
	public InputStream getThumbnail(Media media, int sizeHint)
			throws IOException {
		JsMediaUrl url = JsMediaUrl.thumbnail(mContext);
		url.type = getServiceType();
		url.account = media.account;
		url.mediaId = media.mediaId;
		url.set("widthHint", sizeHint);
		
		HttpResponse response = executeGet(url);
		return response.getContent();
	}

	@Override
	public InputStream getMediaContent(Media media, String[] out_contentType)
			throws IOException {
		JsMediaUrl url = JsMediaUrl.media(mContext);
		url.type = getServiceType();
		url.account = media.account;
		url.mediaId = media.mediaId;
		
		HttpResponse response = executeGet(url);
		out_contentType[0] = response.getContentType();
		return response.getContent();
	}

	@Override
	public Media insertMedia(String account, String dirId, InputStream content,
			String filename, Collection<Metadata> metadata)
			throws IOException {
		Media media = new Media();
		media.service = getServiceType();
		media.account = account;
		media.mediaId = null;
		media.directoryId = dirId;
		media.fileName = filename;
		media.metadata = metadata;
		return updateMedia(media, content, filename);
	}

	@Override
	public Media updateMedia(String account, String mediaId,
			InputStream content, String filename, Collection<Metadata> metadata) throws IOException {
		Media media = new Media();
		media.service = getServiceType();
		media.account = account;
		media.mediaId = mediaId;
		media.metadata = metadata;
		return updateMedia(media, content, filename);
	}
	
	protected Media updateMedia(Media media, InputStream content, String filename) throws IOException {
		JsMediaUrl url = JsMediaUrl.mediaUpdate(mContext);
//		HttpRequest request = mFactory.buildPostRequest(url, null);
//		
//		JsonHttpContent json = new JsonHttpContent(new GsonFactory(), media);
//		InputStreamContent binary = new InputStreamContent(MediaUtil.getMimeTypeFromPath(filename), content);
//		MultipartRelatedContent httpContent = new MultipartRelatedContent(json, binary);
//		httpContent.forRequest(request);
		
//		MultipartFormdataContent formdata = new MultipartFormdataContent();
//		formdata.addContent("media", new JsonHttpContent(new GsonFactory(), media));
//		formdata.addContent("content", new InputStreamContent(MediaUtil.getMimeTypeFromPath(filename), content));
//		
//		FileOutputStream out = new FileOutputStream(new File("/mnt/sdcard/multipart"));
//		formdata.writeTo(out);
//		out.close();
//		
//		HttpRequest request = mFactory.buildPostRequest(url, formdata);
//		
//		HttpResponse response = execute(request);
//		return JsonUtil.fromJson(response.parseAsString(), Media.class);
		
		HttpPost request = new HttpPost(url.build());
		Credential credential = mAuth.loadCredential();
		if (credential != null) {
			request.addHeader("Authorization", credential.generateAuthorizationHeader());
		}
		
		MultipartEntity entity = new MultipartEntity();
		entity.addPart("media", new StringBody(JsonUtil.toJson(media), "application/json", Charset.forName("UTF-8")));
		entity.addPart("content", new InputStreamBody(content, MediaUtil.getMimeTypeFromPath(filename), filename));
		request.setEntity(entity);
		
		// タイムアウトを設定
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, 20000);	// 接続タイムアウト
		HttpConnectionParams.setSoTimeout(params, 20000);			// リードタイムアウト
		
		HttpClient client = new DefaultHttpClient(params);
		org.apache.http.HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() != 200) {
			String message;
			try {
				String responsedContent =IOUtil.readString(
						response.getEntity().getContent(), Charset.forName("UTF-8"));
				message = String.format("%s\n%s", response.getStatusLine().toString(), responsedContent);
			} catch (Exception e) {
				message = response.getStatusLine().toString();
			}
			throw new IOException(message);
		}
		HttpEntity responsedEntity = response.getEntity();
		String json = IOUtil.readString(responsedEntity.getContent(), Charset.forName("UTF-8"));
		return JsonUtil.fromJson(json, Media.class);
	}

	@Override
	public void deleteMedia(String account, String mediaId) throws IOException {
		JsMediaUrl url = JsMediaUrl.mediaDelete(mContext);
		url.type = getServiceType();
		url.account = account;
		url.mediaId = mediaId;
		
		HttpRequest request = mFactory.buildPostRequest(url, null);
		execute(request);
	}
}
