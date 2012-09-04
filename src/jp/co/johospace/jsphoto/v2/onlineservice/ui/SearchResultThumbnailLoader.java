package jp.co.johospace.jsphoto.v2.onlineservice.ui;

import java.util.HashMap;
import java.util.Map;

import jp.co.johospace.jsphoto.cache.LocalCachedThumbnailLoader;
import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCacheImpl;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.content.Context;

/**
 * 検索結果向けサムネイルローダ
 */
public class SearchResultThumbnailLoader implements UXThumbnailLoader {

	private final Context mContext;
	private final Map<String, CachingAccessor> mAccessors = new HashMap<String, CachingAccessor>();
	private final UXThumbnailLoader mLocalLoader;
	
	public SearchResultThumbnailLoader(Context context) {
		this(context, true);
	}
	
	public SearchResultThumbnailLoader(Context context, boolean compress) {
		super();
		mContext = context;
		mLocalLoader = new LocalCachedThumbnailLoader();
		ExternalServiceCache cache = new ExternalServiceCacheImpl(context);
		JsMediaServerClient jsMedia = ClientManager.getJsMediaServerClient(context);
		String[] services = {
				ServiceType.PICASA_WEB,
				ServiceType.TWITTER,
				ServiceType.FACEBOOK,
		};
		
		for (String service : services) {
			ExternalServiceClient client = ClientManager.getExternalServiceClient(context, service);
			mAccessors.put(service, new CachingAccessor(context, jsMedia, client, cache, compress));
		}
	}
	
	@Override
	public boolean loadCachedThumbnail(Object info, int sizeHint,
			UXImageInfo out) {
		Media media = (Media) info;
		if (ServiceType.JORLLE_LOCAL.equals(media.service)) {
			return mLocalLoader.loadCachedThumbnail(media.mediaId, sizeHint, out);
		} else {
			CachingAccessor accessor = mAccessors.get(media.service);
			if (accessor != null) {
				return accessor.loadCachedThumbnail(info, sizeHint, out);
			} else {
				return false;
			}
		}
	}
	
	@Override
	public boolean loadThumbnail(Object info, int sizeHint, UXImageInfo out) {
		Media media = (Media) info;
		if (ServiceType.JORLLE_LOCAL.equals(media.service)) {
			return mLocalLoader.loadThumbnail(media.mediaId, sizeHint, out);
		} else {
			CachingAccessor accessor = mAccessors.get(media.service);
			if (accessor != null) {
				return accessor.loadThumbnail(info, sizeHint, out);
			} else {
				return false;
			}
		}
	}
	
	@Override
	public void updateCachedThumbnail(Object info, int sizeHint, UXImageInfo in) {
		Media media = (Media) info;
		if (ServiceType.JORLLE_LOCAL.equals(media.service)) {
			mLocalLoader.updateCachedThumbnail(media.mediaId, sizeHint, in);
		} else {
			CachingAccessor accessor = mAccessors.get(media.service);
			if (accessor != null) {
				accessor.updateCachedThumbnail(info, sizeHint, in);
			} else {
			}
		}
	}
}
