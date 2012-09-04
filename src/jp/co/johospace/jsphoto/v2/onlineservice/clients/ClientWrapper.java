package jp.co.johospace.jsphoto.v2.onlineservice.clients;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.AuthPreference;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.CameraPath;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Memory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.RelatedMedia;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.SyncPreference;
import jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.JsMediaUrl;
import android.util.Pair;

import com.google.api.client.http.HttpResponse;


/**
 * クライアントラッパー
 */
public class ClientWrapper implements JsMediaServerClient, ExternalServiceClient {

	private final JsMediaServerClient mJsMedia;
	private final ExternalServiceClient mExternal;
	public ClientWrapper(JsMediaServerClient jsMedia, ExternalServiceClient external) {
		super();
		mJsMedia = jsMedia;
		mExternal = external;
	}
	
	public JsMediaServerClient getBaseJsMediaServerClient() {
		return mJsMedia;
	}
	
	public ExternalServiceClient getBaseExternalServiceClient() {
		return mExternal;
	}
	
	@Override
	public List<AuthPreference> getAuthPreferences(boolean forceRequest) throws IOException {
		return mJsMedia.getAuthPreferences(forceRequest);
	}
	@Override
	public void requestIndexing(String serviceType, String serviceAccount, boolean async) throws IOException {
		mJsMedia.requestIndexing(serviceType, serviceAccount, async);
	}
	@Override
	public Long getCurrentMediaVersion(String serviceType, String serviceAccount)
			throws IOException {
		return mJsMedia.getCurrentMediaVersion(serviceType, serviceAccount);
	}
	@Override
	public List<Directory> getDirectories(String serviceType,
			String serviceAccount, Long from, Long to, int mediaLimit, boolean syncExt)
			throws IOException {
		return mJsMedia.getDirectories(serviceType, serviceAccount, from, to, mediaLimit, syncExt);
	}
	@Override
	public List<Media> getMediaList(String serviceType, String serviceAccount,
			String dirId, boolean includeMetadata, Long from, Long to)
			throws IOException {
		return mJsMedia.getMediaList(serviceType, serviceAccount, dirId, includeMetadata, from, to);
	}
	@Override
	public List<Media> searchMediaByKeyword(String keyword, boolean includeLinkage) throws IOException {
		return mJsMedia.searchMediaByKeyword(keyword, includeLinkage);
	}
	@Override
	public RelatedMedia searchRelatedMedia(String serviceType,
			String serviceAccount, String mediaId) throws IOException {
		return mJsMedia.searchRelatedMedia(serviceType, serviceAccount, mediaId);
	}
	@Override
	public RespondedContents<IOIterator<Memory>> searchMemories(String etag) throws IOException, ContentsNotModifiedException {
		return mJsMedia.searchMemories(etag);
	}
	@Override
	public IOIterator<Media> updateLocalMediaIndices(
			IOIterator<Media> localMedia, long[] out_next) throws IOException {
		return mJsMedia.updateLocalMediaIndices(localMedia, out_next);
	}
	@Override
	public List<CameraPath> getCameraPathList() throws IOException {
		return mJsMedia.getCameraPathList();
	}

	@Override
	public void setAuthCredentials(Map<String, String> credentials) {
		mExternal.setAuthCredentials(credentials);
	}

	@Override
	public List<Metadata> getMetadata(String account, String mediaId)
			throws IOException {
		return mExternal.getMetadata(account, mediaId);
	}

	@Override
	public InputStream getThumbnail(Media media, int sizeHint)
			throws IOException {
		return mExternal.getThumbnail(media, sizeHint);
	}

	@Override
	public InputStream getLargeThumbnail(Media media) throws IOException {
		return mExternal.getLargeThumbnail(media);
	}
	
	@Override
	public Pair<String, String> getContentsUrl(Media media, String contentType) {
		return mExternal.getContentsUrl(media, contentType);
	}
	
	@Override
	public InputStream getMediaContent(Media media, String[] out_contentType)
			throws IOException {
		return mExternal.getMediaContent(media, out_contentType);
	}
	
	@Override
	public String getMediaContentType(Media media) throws IOException {
		return mExternal.getMediaContentType(media);
	}

	@Override
	public Media insertMedia(String account, String dirId, InputStream content,
			String filename, Collection<Metadata> metadata) throws IOException {
		return mExternal.insertMedia(account, dirId, content, filename, metadata);
	}

	@Override
	public Media updateMedia(String account, String mediaId,
			InputStream content, String filename, Collection<Metadata> metadata)
			throws IOException {
		return mExternal.updateMedia(account, mediaId, content, filename, metadata);
	}

	@Override
	public void deleteMedia(String account, String mediaId) throws IOException {
		mExternal.deleteMedia(account, mediaId);
	}

	@Override
	public String getServiceType() {
		return mExternal.getServiceType();
	}
	
	@Override
	public SyncPreference setupSync(String serviceType, String serviceAccount)
			throws IOException {
		return mJsMedia.setupSync(serviceType, serviceAccount);
	}

	@Override
	public Map<String, String> getExternalServiceCredentials(
			String serviceType, boolean forceRequest) throws IOException {
		return mJsMedia.getExternalServiceCredentials(serviceType, forceRequest);
	}

	@Override
	public HttpResponse executeGet(JsMediaUrl url) throws IOException {
		return mJsMedia.executeGet(url);
	}

	@Override
	public Map<String, String> createAccount() throws IOException {
		return mJsMedia.createAccount();
	}
	
	@Override
	public void requestRecreateIndex() throws IOException {
		mJsMedia.requestRecreateIndex();
	}
}
