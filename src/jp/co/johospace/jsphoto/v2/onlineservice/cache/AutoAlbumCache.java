package jp.co.johospace.jsphoto.v2.onlineservice.cache;

import java.io.IOException;
import java.util.List;

import jp.co.johospace.jsphoto.v2.onlineservice.clients.ContentsNotModifiedException;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Memory;

public interface AutoAlbumCache {
	List<CategoryInfo> getCategories(int mediaLimit);
	Memory getMemory(String categoryName);
	void updateCache() throws IOException, ContentsNotModifiedException;
	void clearCache();
	
	class CategoryInfo{
		public String categoryName;
		public int headerCount;
		public int mediaCount;
		public List<Media> mediaInfos;
	}
}
