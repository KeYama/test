package jp.co.johospace.jsphoto.v2.onlineservice.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;

public interface ExternalServiceCache {

	List<Directory> listDirectories(String serviceType,
			String serviceAccount, boolean includeEmptyDirs);

	void updateDirectories(String serviceType,
			String serviceAccount, List<Directory> dirs);

	List<Media> listMedias(String serviceType,
			String serviceAccount, String dirId, Integer limit, boolean ignoreCached, boolean albumPhoto);

	void updateMedias(String serviceType,
			String serviceAccount, String dirId, List<Media> medias, boolean updateCached);

	List<Metadata> getMetadata(String serviceType,
			String serviceAccount, String mediaId);

	void updateMetadata(String serviceType,
			String serviceAccount, String mediaId, Collection<Metadata> metadata);

	byte[] getThumbnail(String serviceType,
			String serviceAccount, String mediaId, int sizeHint);

	void updateThumbnail(String serviceType,
			String serviceAccount, String mediaId, int sizeHint, byte[] data);

	File getLargeThumbnail(String serviceType,
			String serviceAccount, String mediaId);

	File updateLargeThumbnail(String serviceType,
			String serviceAccount, String mediaId, InputStream content)
			throws IOException;

	File getMediaContent(String serviceType,
			String serviceAccount, String mediaId, String[] out_contentType);

	File updateMediaContent(String serviceType,
			String serviceAccount, String mediaId, InputStream content, String contentType)
			throws IOException;

	boolean isCachedMediaDirty(String serviceType, String serviceAccount,
			String dirId);

	Long getCachedMediaVersion(String serviceType, String serviceAccount,
			String dirId);

	Long getCachedRootVersion(String serviceType, String serviceAccount);

	Long getLastUpdated(String serviceType, String serviceAccount, String dirId);

	Long getLastUpdated(String serviceType, String serviceAccount);

	void clearMediaContentCache(String serviceType, String serviceAccount);

	void clearLargeThumbnailCache(String serviceType, String serviceAccount);

	void clearThumbnailCache(String serviceType, String serviceAccount);
	
	String getMediaContentType(String serviceType,
			String serviceAccount, String mediaId);
	
	void updateMediaContentType(String serviceType,
			String serviceAccount, String mediaId, String contentType);

	void clearMediaTree(String serviceType, String serviceAccount);

}