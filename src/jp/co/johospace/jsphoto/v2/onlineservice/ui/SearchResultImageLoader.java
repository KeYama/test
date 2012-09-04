package jp.co.johospace.jsphoto.v2.onlineservice.ui;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jp.co.johospace.jsphoto.fullscreen.loader.LocalImageLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.content.Context;
import android.graphics.Bitmap;

/**
 * 検索結果イメージローダ
 */
public class SearchResultImageLoader extends LocalImageLoader {

	private final Context mContext;
	private final Map<String, CachingAccessor> mAccessors = new HashMap<String, CachingAccessor>();
	public SearchResultImageLoader(Context context) {
		super();
		mContext = context;
		ExternalServiceCache cache = new ExternalServiceCacheImpl(context);
		JsMediaServerClient jsMedia = ClientManager.getJsMediaServerClient(context);
		String[] services = {
				ServiceType.PICASA_WEB,
				ServiceType.TWITTER,
				ServiceType.FACEBOOK,
		};
		
		for (String service : services) {
			ExternalServiceClient client = ClientManager.getExternalServiceClient(context, service);
			mAccessors.put(service, new CachingAccessor(context, jsMedia, client, cache, false));
		}
	}
	
	private String[] out_contentType = new String[1];
	
	@Override
	public Bitmap loadFullImage(Object tag) {
		Media media = (Media) tag;
		if (ServiceType.JORLLE_LOCAL.equals(media.service)) {
			return super.loadFullImage(media.mediaId);
		} else {
			CachingAccessor accessor = mAccessors.get(media.service);
			if (accessor != null) {
				File cacheFile;
				try {
					String contentType = accessor.getMediaContentType(media);
					if (contentType != null && contentType.startsWith("video/")) {
						cacheFile = accessor.getLargeThumbnailFile(media);
					} else {
						cacheFile = accessor.getMediaContentFile(media, out_contentType);
					}
				} catch (IOException e) {
					return null;
				}
				return super.loadFullImage(cacheFile.getAbsolutePath());
				
			} else {
				return null;
			}
		}
	}
	
	@Override
	public Bitmap loadThumbnailImage(Object tag, int screenWidth,
			int screenHeight) {
		Media media = (Media) tag;
		if (ServiceType.JORLLE_LOCAL.equals(media.service)) {
			return super.loadThumbnailImage(media.mediaId, screenWidth, screenHeight);
		} else {
			CachingAccessor accessor = mAccessors.get(media.service);
			if (accessor != null) {
				File cacheFile;
				try {
					cacheFile = accessor.getLargeThumbnailFile(media);
				} catch (IOException e) {
					return null;
				}
				return super.loadThumbnailImage(
						cacheFile.getAbsolutePath(), screenWidth, screenHeight);
				
			} else {
				return null;
			}
		}
	}
}
