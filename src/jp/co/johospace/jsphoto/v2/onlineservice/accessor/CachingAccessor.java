package jp.co.johospace.jsphoto.v2.onlineservice.accessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.ExternalServiceCache;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientWrapper;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ContentsNotModifiedException;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.RespondedContents;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Memory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * キャッシュするアクセサ
 */
public class CachingAccessor extends ClientWrapper implements UXThumbnailLoader {

	
	@Override
	public List<Media> searchMediaByKeyword(String keyword,
			boolean includeLinkage) throws IOException {
		List<Media> medias = super.searchMediaByKeyword(keyword, includeLinkage);
		saveCashedSearchMedias(medias);
		
		return medias;
	}
	
	/**
	 * 最後に検索した結果を取得します
	 * @return
	 * @throws IOException
	 */
	public List<Media> searchMediaByLastCashed() throws IOException {
		return loadCachedMedias();
	}
	
	private final ExternalServiceCache mCache;
	private final boolean mCompress;
	private final Context mContext;
	public CachingAccessor(Context context, JsMediaServerClient jsMedia, ExternalServiceClient external, ExternalServiceCache cache, boolean compress) {
		super(jsMedia, external);
		mCache = Preconditions.checkNotNull(cache);
		mCompress = compress;
		mContext = context;
	}
	
	public ExternalServiceCache getCache() {
		return mCache;
	}
	
	public void clearMediaTree(String serviceType, String serviceAccount) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = pref.edit();
		Map<String, ?> versions = pref.getAll();
		String prefix =  String.format("%s|ver|", getClass().getSimpleName());
		for (String versionKey : versions.keySet()) {
			if (!TextUtils.isEmpty(versionKey)
					&& versionKey.startsWith(prefix)) {
				boolean delete = false;
				if (serviceType == null && serviceAccount == null) {
					delete = true;
				} else {
					if (serviceType != null) {
						if (serviceAccount != null) {
							delete = versionKey.equals(String.format("%s|ver|%s|%s", getClass().getSimpleName(), serviceType, serviceAccount));
						} else {
							delete = versionKey.startsWith(String.format("%s|ver|%s|", getClass().getSimpleName(), serviceType));
						}
					}
				}
				if (delete) {
					editor.remove(versionKey);
				}
			}
		}
		
		mCache.clearMediaTree(serviceType, serviceAccount);
		editor.commit();
	}
	
	public void updateDirectoryCache(String serviceType, String serviceAccount) throws IOException {
//		Long version = mCache.getCachedRootVersion(serviceType, serviceAccount);
//		List<Directory> dirs = super.getDirectories(serviceType, serviceAccount,
//				version == null ? null : version + 1, null, 0);
//		mCache.updateDirectories(serviceType, serviceAccount, dirs);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
		String key = String.format("%s|ver|%s|%s", getClass().getSimpleName(), serviceType, serviceAccount);
		Long remote = super.getCurrentMediaVersion(serviceType, serviceAccount);
		long _local = pref.getLong(key, Long.MIN_VALUE);
		Long local = _local == Long.MIN_VALUE ? null : _local;
		
		if (local == null || (local != null && remote != null && local <= remote)) {
			List<Directory> dirs = super.getDirectories(serviceType, serviceAccount, local, remote, 0, true);
			mCache.updateDirectories(serviceType, serviceAccount, dirs);
			for (Directory dir : dirs) {
				List<Media> medias = super.getMediaList(serviceType, serviceAccount, dir.id, false, local, remote);
				mCache.updateMedias(serviceType, serviceAccount, dir.id, medias, true);
			}
			
			pref.edit().putLong(key, remote == null ? 1 : remote + 1).commit();
		}
	}
	
	public void updateMediaListCache(String serviceType, String serviceAccount, String dirId) throws IOException {
//		boolean dirty = mCache.isCachedMediaDirty(serviceType, serviceAccount, dirId);
//		if (dirty) {
//			Long version = mCache.getCachedMediaVersion(serviceType, serviceAccount, dirId);
//			List<Media> medias =
//					super.getMediaList(serviceType, serviceAccount, dirId, false,
//							version == null ? null : version + 1, null);
//			mCache.updateMedias(serviceType, serviceAccount, dirId, medias, true);
//		}
		updateDirectoryCache(serviceType, serviceAccount);
	}
	
	public List<Media> getMediaList(String serviceType, String serviceAccount, int limit) throws IOException {
		List<Directory> dirs = getDirectories(serviceType, serviceAccount, false, limit, false);
		ArrayList<Media> media = new ArrayList<Media>();
		for (Directory dir : dirs) {
			media.addAll(dir.media.subList(0, Math.min(dir.media.size(), limit - media.size())));
			if (limit <= media.size()) {
				break;
			}
		}
		return media;
	}
	
	public List<Directory> getDirectories(String serviceType, String serviceAccount, boolean includeEmptyDirs, int mediaLimit, boolean syncExt) throws IOException {
//long st = SystemClock.elapsedRealtime();/*$debug$*/
		List<Directory> cache = mCache.listDirectories(serviceType, serviceAccount, includeEmptyDirs);
		if (cache == null) {
			cache = super.getDirectories(serviceType, serviceAccount, null, null, mediaLimit, syncExt);
			mCache.updateDirectories(serviceType, serviceAccount, cache);
			for (Directory dir : cache) {
				if (dir.media == null) {
					dir.media = new ArrayList<Media>();
				}
				mCache.updateMedias(serviceType, serviceAccount, dir.id, dir.media, false);
			}
			cache = mCache.listDirectories(serviceType, serviceAccount, includeEmptyDirs);
			if (cache == null) {
				cache = new ArrayList<Directory>();
			}
		}
//System.out.println(String.format("  dir listing %d msec", SystemClock.elapsedRealtime() - st));/*$debug$*/
		
//st = SystemClock.elapsedRealtime();/*$debug$*/
		if(cache!=null) {
			for (Directory dir : cache) {
				List<Media> medias = mCache.listMedias(serviceType, serviceAccount, dir.id, mediaLimit, true, true);
				dir.media =
						medias == null ? new ArrayList<Media>() : medias;
//System.out.println(String.format("    media size : %d", dir.media.size()));/*$debug$*/
			}
		} else {
			cache = new ArrayList<Directory>();
		}
//System.out.println(String.format("  media listing %d msec", SystemClock.elapsedRealtime() - st));/*$debug$*/
		
		return cache;
	}

	
	public List<Media> getMediaList(String serviceType, String serviceAccount,
			String dirId) throws IOException {
		List<Media> cache = mCache.listMedias(serviceType, serviceAccount, dirId, null, false, false);
		if (cache == null) {
			cache = super.getMediaList(serviceType, serviceAccount, dirId, false, null, null);
			mCache.updateMedias(serviceType, serviceAccount, dirId, cache, true);
			Collections.sort(cache, new Comparator<Media>() {
				@Override
				public int compare(Media m1, Media m2) {
					int c = m1.productionDate.compareTo(m2.productionDate) * -1;
					if (c == 0) {
						return m1.mediaId.compareTo(m2.mediaId);
					} else {
						return c;
					}
				}
			});
		}
		return cache;
	}
	
	@Override
	public List<Metadata> getMetadata(String account, String mediaId)
			throws IOException {
		List<Metadata> cache = mCache.getMetadata(getServiceType(), account, mediaId);
		if (cache == null) {
			cache = super.getMetadata(account, mediaId);
			mCache.updateMetadata(getServiceType(), account, mediaId, cache);
		}
		return cache;
	}
	
	@Override
	public InputStream getThumbnail(Media media, int sizeHint)
			throws IOException {
		byte[] cache = mCache.getThumbnail(getServiceType(), media.account, media.mediaId, sizeHint);
		if (cache == null) {
			ByteArrayOutputStream data = new ByteArrayOutputStream();
			InputStream thumb = super.getThumbnail(media, sizeHint);
			if (thumb == null) {
				return null;
			}
			
			try {
				IOUtil.copy(thumb, data);
			} finally {
				thumb.close();
			}
			cache = data.toByteArray();
			mCache.updateThumbnail(getServiceType(), media.account, media.mediaId, sizeHint, cache);
		}
		
		return new ByteArrayInputStream(cache);
	}
	
	@Override
	public InputStream getLargeThumbnail(Media media) throws IOException {
		return new FileInputStream(getLargeThumbnailFile(media));
	}
	
	public File getLargeThumbnailFile(Media media) throws IOException {
		File cache = mCache.getLargeThumbnail(getServiceType(), media.account, media.mediaId);
		if (cache == null) {
			InputStream content = super.getLargeThumbnail(media);
			mCache.updateLargeThumbnail(getServiceType(), media.account, media.mediaId, content);
			cache = mCache.getLargeThumbnail(getServiceType(), media.account, media.mediaId);
		}
		return cache;
	}
	
	@Override
	public InputStream getMediaContent(Media media, String[] out_contentType)
			throws IOException {
		return new FileInputStream(getMediaContentFile(media, out_contentType));
	}
	
	public File getMediaContentFile(Media media, String[] out_contentType) throws IOException {
		File cache = mCache.getMediaContent(getServiceType(), media.account, media.mediaId, out_contentType);
		if (cache == null) {
			String[] contentType = new String[1];
			InputStream content = super.getMediaContent(media, contentType);
			mCache.updateMediaContent(getServiceType(), media.account, media.mediaId, content, contentType[0]);
			cache = mCache.getMediaContent(getServiceType(), media.account, media.mediaId, out_contentType);
		}
		return cache;
	}
	
	@Override
	public String getMediaContentType(Media media) throws IOException {
		String cache = mCache.getMediaContentType(media.service, media.account, media.mediaId);
		if (cache == null) {
			cache = super.getMediaContentType(media);
			mCache.updateMediaContentType(media.service, media.account, media.mediaId, cache);
		}
		return cache;
	}

	@Override
	public boolean loadCachedThumbnail(Object info, int sizeHint,
			UXImageInfo out) {
		Media media = (Media) info;
		byte[] cache = mCache.getThumbnail(
				media.service, media.account, media.mediaId, sizeHint);
		if (cache == null) {
			return false;
		}
		
//		if (mCompress) {
			out.compressedImage = cache;
			return out.compressedImage != null;
//		} else {
//			out.bitmap = BitmapFactory.decodeByteArray(cache, 0, cache.length);
//			return out.bitmap != null;
//		}
	}

	@Override
	public boolean loadThumbnail(Object info, int sizeHint, UXImageInfo out) {
		Media media = (Media) info;
		try {
			Bitmap thumb;
			InputStream content = getThumbnail(media, sizeHint);
			if (content == null) {
				return false;
			}
			try {
				thumb = BitmapFactory.decodeStream(content);
			} finally {
				content.close();
			}
			
			//RGB565ではないなら変換
			if(thumb.getConfig() != Bitmap.Config.RGB_565){
				Bitmap tmp = Bitmap.createBitmap(thumb.getWidth(), thumb.getHeight(), Bitmap.Config.RGB_565);
				Canvas c = new Canvas(tmp);
				c.drawBitmap(thumb, 0, 0, null);
				thumb.recycle();
				thumb = tmp;
			}
			
			out.bitmap = thumb;
			return true;
		} catch (IOException e) {
//			e.printStackTrace();		/*$debug$*/
			return false;
		}
	}

	@Override
	public void updateCachedThumbnail(Object info, int sizeHint, UXImageInfo in) {
		Media media = (Media) info;
		mCache.updateThumbnail(media.service, media.account, media.mediaId,
				sizeHint, in.compressedImage);
	}
	
//	public List<Memory> updateMemoriesCache() throws IOException {
//		List<Memory> memories = super.searchMemories();
//		saveCashedMemories(memories);
//		return memories;
//		throw new UnsupportedOperationException();
//	}
	
	@Override
	public RespondedContents<IOIterator<Memory>> searchMemories(String etag) throws IOException, ContentsNotModifiedException {
//		List<Memory> memories = loadCachedMemories();
//		if (memories == null) {
//			memories = updateMemoriesCache();
//		}
//		return memories;
		throw new UnsupportedOperationException();
	}
	
	private static final Object sSearchCacheLock = new Object();
	private void saveCashedSearchMedias(List<Media> medias) throws IOException {
		synchronized (sSearchCacheLock) {
			File dir = new File(mContext.getExternalCacheDir(), getClass().getSimpleName());
			dir.mkdirs();
			File file = new File(dir, Media.class.getSimpleName());
			File transientFile = new File(dir, Media.class.getSimpleName().concat(".transient"));
			Gson gson = new Gson();
			JsonWriter writer = new JsonWriter(new FileWriter(transientFile));
			try {
				gson.toJson(medias, new TypeToken<List<Media>>() {}.getType(), writer);
			} finally {
				writer.close();
			}
			file.delete();
			transientFile.renameTo(file);
			transientFile.delete();
		}
	}
	
	public static void clearCashedSearchMedias(Context context) {
		synchronized (sSearchCacheLock) {
			File dir = new File(context.getExternalCacheDir(), CachingAccessor.class.getSimpleName());
			new File(dir, Media.class.getSimpleName()).delete();
			new File(dir, Media.class.getSimpleName().concat(".transient")).delete();
		}
	}
	
	private List<Media> loadCachedMedias() throws IOException {
		synchronized (sSearchCacheLock) {
			File dir = new File(mContext.getExternalCacheDir(), getClass().getSimpleName());
			File file = new File(dir, Media.class.getSimpleName());
			if (!file.exists()) {
				return null;
			}
			
			Gson gson = new Gson();
			JsonReader reader = new JsonReader(new FileReader(file));
			try {
				return gson.fromJson(reader, new TypeToken<List<Media>>() {}.getType());
			} finally {
				reader.close();
			}
		}
	}
	
	
	private static final Object sMemoriesCacheLock = new Object();
//	private void saveCashedMemories(List<Memory> memories) throws IOException {
//		synchronized (sMemoriesCacheLock) {
//			File dir = new File(mContext.getExternalCacheDir(), getClass().getSimpleName());
//			dir.mkdirs();
//			File file = new File(dir, Memory.class.getSimpleName());
//			File transientFile = new File(dir, Memory.class.getSimpleName().concat(".transient"));
//			Gson gson = new Gson();
//			JsonWriter writer = new JsonWriter(new FileWriter(transientFile));
//			try {
//				gson.toJson(memories, new TypeToken<List<Memory>>() {}.getType(), writer);
//				writer.flush();
//			} finally {
//				writer.close();
//			}
//			file.delete();
//			transientFile.renameTo(file);
//			transientFile.delete();
//		}
//	}
//	
//	private List<Memory> loadCachedMemories() throws IOException {
//		synchronized (sMemoriesCacheLock) {
//			File dir = new File(mContext.getExternalCacheDir(), getClass().getSimpleName());
//			File file = new File(dir, Memory.class.getSimpleName());
//			if (!file.exists()) {
//				return null;
//			}
//			
//			Gson gson = new Gson();
//			JsonReader reader = new JsonReader(new FileReader(file));
//			try {
//				return gson.fromJson(reader, new TypeToken<List<Memory>>() {}.getType());
//			} catch (JsonSyntaxException e) {
////				e.printStackTrace();		/*$debug$*/
//				try {
//					file.delete();
//				} catch (Exception ex) {
////					ex.printStackTrace();		/*$debug$*/
//				}
//				return null;
//			} finally {
//				reader.close();
//			}
//		}
//	}
	
	public static void clearCachedMemories(Context context) {
		synchronized (sMemoriesCacheLock) {
			File dir = new File(context.getExternalCacheDir(), CachingAccessor.class.getSimpleName());
			new File(dir, Memory.class.getSimpleName()).delete();
			new File(dir, Memory.class.getSimpleName().concat(".transient")).delete();
		}
	}
	
	public static boolean isMemoriesCached(Context context) {
		File dir = new File(context.getExternalCacheDir(),
				CachingAccessor.class.getSimpleName());
		File file = new File(dir, Memory.class.getSimpleName());
		return file.exists();
	}
}
