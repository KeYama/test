package jp.co.johospace.jsphoto.v2.onlineservice.ui;

import java.io.File;
import java.io.IOException;

import jp.co.johospace.jsphoto.fullscreen.loader.LocalImageLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.accessor.CachingAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.content.Context;
import android.graphics.Bitmap;

/**
 * オンライン イメージローダ
 */
public class OnlineImageLoader extends LocalImageLoader {

	private final CachingAccessor mClient;
	public OnlineImageLoader(Context context, CachingAccessor accessor) {
		super();
		mClient = accessor;
	}
	
	
	String[] out_contentType = new String[1];
	@Override
	public Bitmap loadFullImage(Object tag) {
		Media media = (Media) tag;
		File cacheFile;
		try {
			String contentType = mClient.getMediaContentType(media);
			if (contentType != null && contentType.startsWith("video/")) {
				cacheFile = mClient.getLargeThumbnailFile(media);
			} else {
				cacheFile = mClient.getMediaContentFile(media, out_contentType);
			}
		} catch (IOException e) {
			return null;
		}
		
		return super.loadFullImage(cacheFile.getAbsolutePath());
	}

	@Override
	public Bitmap loadThumbnailImage(Object tag, int screenWidth,
			int screenHeight) {
		Media media = (Media) tag;
		File cacheFile;
		try {
			cacheFile = mClient.getLargeThumbnailFile(media);
		} catch (IOException e) {
			return null;
		}
		
		return super.loadThumbnailImage(cacheFile.getAbsolutePath(), screenWidth, screenHeight);
	}

}
