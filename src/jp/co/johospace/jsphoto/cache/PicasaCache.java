package jp.co.johospace.jsphoto.cache;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import jp.co.johospace.jsphoto.LocalFolderActivity.FolderEntry;
import jp.co.johospace.jsphoto.onlineservice._cost;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.PicasaClient;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.PicasaClient.EntryIterator;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.AlbumEntry;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.LitePhotoEntry;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.UserFeed;
import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.JsonUtil;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.Time;

/**
 * Picasaキャッシュ
 * TODO 同期化を検討する
 */
public class PicasaCache extends ContextWrapper {

	private final PicasaClient mClient;
	private final File mRootPath;
	private final File mTopLevelEtagPath;
	
	public PicasaCache(Context context, String account) {
		super(context);
		mClient = new PicasaClient(context, account);
		mRootPath =
				new File(new File(getExternalCacheDir(), getClass().getSimpleName()), account);
		mTopLevelEtagPath = new File(mRootPath, "etag");
		mRootPath.mkdirs();
		mTopLevelEtagPath.getParentFile().mkdirs();
	}
	
	public String getAccount() {
		return mClient.getServiceAccount();
	}
	
	public IOIterator<FolderEntry> listDirs(final int imgLimit, final boolean saveEtag) throws IOException {
		EntryIterator<UserFeed, AlbumEntry> _itr;
		try {
			_itr = mClient.iterateDirectory();
		} catch (IOException e) {
//			e.printStackTrace();		/*$debug$*/
			_itr = null;
		}
		
		String cachedEtag = loadTopLevelEtag();
		if (_itr == null || (!TextUtils.isEmpty(cachedEtag) && cachedEtag.equals(_itr.getEtag()))) {
			if (_itr != null) {
				_itr.terminate();
			}
			
			return new IOIterator<FolderEntry>() {
				
				final String[] mDirIds = listCachedDirIds();
				int mPos;
				
				@Override
				public void terminate() throws IOException {
				}
				
				@Override
				public FolderEntry next() throws IOException, NoSuchElementException {
					final String dirId = mDirIds[mPos++];
					final AlbumEntry entry = loadDirEntry(dirId);
					final List<String> tags;
					if (0 < imgLimit) {
						tags = listCachedMedia(entry.gphotoId, true, imgLimit);
					} else {
						tags = new ArrayList<String>();
					}
					
					return new FolderEntry() {
						
						@Override
						public String getName() {
							return entry.getName();
						}

						@Override
						public String getPath() {
							return entry.gphotoId;
						}

//						final List<String> mImages = new ArrayList<String>();
						@Override
						public List<String> getImages() {
//							return mImages;
							return tags;
						}

						@Override
						public int getMediaCount() {
							return entry.numPhotos;
						}

						final long mUpdated; {
							Time t = new Time();
							t.parse3339(entry.updated);
							mUpdated = t.toMillis(false);
						}
						@Override
						public long getLastModified() {
							return mUpdated;
						}
						
						@Override
						public String toSerializeValue() {
					    	String value = getPath() + "," + getMediaCount() + "," + getLastModified();
						    for(String path : getImages()) {
						    	value += "," + path;
						    }
							return value;
						}
					};
				}
				
				@Override
				public boolean hasNext() throws IOException {
					return mPos < mDirIds.length;
				}
			};
		} else {
			final EntryIterator<UserFeed, AlbumEntry> itr = _itr;
			final String etag = itr.getEtag();
			return new IOIterator<FolderEntry>() {

				@Override
				public FolderEntry next() throws IOException, NoSuchElementException {
					final AlbumEntry entry = itr.next();
					AlbumEntry cached = loadDirEntry(entry.gphotoId);
					if (cached == null || !cached.etag.equals(entry.etag)) {
						setDirty(entry.gphotoId, true);
						saveDirEntry(entry);
					}
					final ArrayList<String> tags = new ArrayList<String>();
					if (0 < imgLimit) {
						IOIterator<MediaEntry> itrMedia = listMediasAt(entry.gphotoId, imgLimit);
						try {
							for (int i = 0; itrMedia.hasNext() && i < imgLimit; i++) {
								MediaEntry media = itrMedia.next();
								tags.add(encodeTag(media.getDirId(), media.getMediaId()));
							}
						} finally {
							itrMedia.terminate();
						}
					}
					return new FolderEntry() {
						
						@Override
						public String getName() {
							return entry.getName();
						}

						@Override
						public String getPath() {
							return entry.getID();
						}

						final long mUpdated; {
							Time t = new Time();
							t.parse3339(entry.updated);
							mUpdated = t.toMillis(false);
						}
						@Override
						public long getLastModified() {
							return mUpdated;
						}

//						final List<String> mImages = new ArrayList<String>();
						@Override
						public List<String> getImages() {
//							return mImages;
							return tags;
						}

						@Override
						public int getMediaCount() {
							return entry.numPhotos;
						}
						
						@Override
						public String toSerializeValue() {
					    	String value = getPath() + "," + getMediaCount() + "," + getLastModified();
						    for(String path : getImages()) {
						    	value += "," + path;
						    }
							return value;
						}
					};
				}

				@Override
				public boolean hasNext() throws IOException {
					return itr.hasNext();
				}

				@Override
				public void terminate() throws IOException {
					try {
						if (!hasNext() && saveEtag) {
							saveTopLevelEtag(etag);
						}
					} finally {
						itr.terminate();
					}
				}
			};
		}
	}
	
	public IOIterator<MediaEntry> listMediasAt(final String dirId, int limit) throws IOException {
		if (isDirty(dirId)) {
			try {
				return listOnlineMediaAt(dirId, limit);
			} catch (IOException e) {
//				e.printStackTrace();		/*$debug$*/
				return listOfflineMediaAt(dirId, limit);
			}
			
		} else {
			return listOfflineMediaAt(dirId, limit);
		}
	}

	protected IOIterator<MediaEntry> listOnlineMediaAt(final String dirId,
			int limit) throws IOException {
		int imgMax = limit <= 0 ? 0 : limit + 1;
long st = SystemClock.elapsedRealtime();//TODO
		final IOIterator<LitePhotoEntry> itr =
				mClient.listMediaLite(dirId, imgMax,
						new String[] {computeMiniThumbParam(), computeLargeThumbParam()}, "d");
_cost.network += (SystemClock.elapsedRealtime() - st);//TODO
		return new IOIterator<MediaEntry>() {
			
			@Override
			public void terminate() throws IOException {
				if (!hasNext()) {
					setDirty(dirId, false);
				}
			}
			
			@Override
			public MediaEntry next() throws IOException, NoSuchElementException {
long st = SystemClock.elapsedRealtime();//TODO
				final LitePhotoEntry entry = itr.next();
_cost.network += (SystemClock.elapsedRealtime() - st);//TODO
st = SystemClock.elapsedRealtime();//TODO
long st2 = SystemClock.elapsedRealtime();//TODO
				LitePhotoEntry prev = loadMediaEntry(entry.gphotoAlbumId, entry.gphotoId);
_cost.loadMediaEntry += (SystemClock.elapsedRealtime() - st2);//TODO
				if (prev == null || !prev.etag.equals(entry.etag)) {
st2 = SystemClock.elapsedRealtime();//TODO
					deleteImageCaches(entry.gphotoAlbumId, entry.gphotoId);
_cost.deleteImageCaches += (SystemClock.elapsedRealtime() - st2);//TODO
st2 = SystemClock.elapsedRealtime();//TODO
					saveMediaEntry(entry);
_cost.saveMediaEntry += (SystemClock.elapsedRealtime() - st2);//TODO
				}
				MediaEntry wrapped = new MediaEntry() {
					
//					@Override
//					public String getName() {
//						return entry.title;
//					}
					
					@Override
					public String getMediaId() {
						return entry.gphotoId;
					}
					
					@Override
					public String getDirId() {
						return entry.gphotoAlbumId;
					}
					
					@Override
					public File getCachePath() {
						return toMediaEntryPath(entry.gphotoAlbumId, entry.gphotoId);
					}
				};
_cost.cacheOperation += (SystemClock.elapsedRealtime() - st);//TODO
				return wrapped;
			}
			
			@Override
			public boolean hasNext() throws IOException {
				return itr.hasNext();
			}
		};
	}

	protected IOIterator<MediaEntry> listOfflineMediaAt(final String dirId, int limit) {
		final List<String> mediaIds = listCachedMedia(dirId, false, limit);
		return new IOIterator<MediaEntry>() {

			int mPos;
			
			@Override
			public MediaEntry next() throws IOException,
					NoSuchElementException {
				String mediaId = mediaIds.get(mPos++);
				final LitePhotoEntry entry = loadMediaEntry(dirId, mediaId);
				return new MediaEntry() {
					
//					@Override
//					public String getName() {
//						return entry.title;
//					}
					
					@Override
					public String getMediaId() {
						return entry.gphotoId;
					}
					
					@Override
					public String getDirId() {
						return entry.gphotoAlbumId;
					}
					
					@Override
					public File getCachePath() {
						return toMediaEntryPath(entry.gphotoAlbumId, entry.gphotoId);
					}
				};
			}

			@Override
			public boolean hasNext() throws IOException {
				return mPos < mediaIds.size();
			}

			@Override
			public void terminate() throws IOException {
			}
		};
	}
	
	public Bitmap loadMiniThumb(String dirId, String mediaId, BitmapFactory.Options options) throws IOException {
		File cacheFile = toMiniThumbCachePath(dirId, mediaId);
		updateThumbCache(dirId, mediaId, 0, cacheFile);
		if (cacheFile.exists()) {
			Bitmap bitmap = BitmapFactory.decodeFile(
					cacheFile.getAbsolutePath(), options);
//			PhotoEntry entry = loadMediaEntry(dirId, mediaId);//TODO パフォーマンス改善余地
//			if (entry.mediaGroup.content.type.startsWith("video/")) {
//				bitmap = ImageCache.editVideoThumbnail(bitmap);
//			}
			return bitmap;
		} else {
			return null;
		}
	}
	
	public File loadLargeThumb(String tag) throws IOException {
		String[] key = decodeTag(tag);
		File cacheFile = toLargeThumbCachePath(key[0], key[1]);
		updateThumbCache(key[0], key[1], 1, cacheFile);
		return cacheFile.exists() ? cacheFile : null;
	}
	
	public File loadFullSizeImage(String tag) throws IOException {
		String[] key = decodeTag(tag);
		File cacheFile = toFullImageCachePath(key[0], key[1]);
		if (!cacheFile.exists()) {
			LitePhotoEntry entry = loadMediaEntry(key[0], key[1]);
			if (entry == null) {
				return null;
			} else {
				String url = entry.mediaGroup.content.url;
				InputStream in = mClient.openContent(url);
				IOUtil.copy(in, new FileOutputStream(cacheFile));
			}
		}
		return cacheFile;
	}
	
	protected void updateThumbCache(String dirId, String mediaId, int thumbIndex, File cacheFile) throws IOException {
		if (!cacheFile.exists()) {
			LitePhotoEntry entry = loadMediaEntry(dirId, mediaId);
			if (entry == null) {
			} else {
				String url = entry.mediaGroup.thumbnails.get(thumbIndex).url;
				InputStream in = mClient.openContent(url);
				IOUtil.copy(in, new FileOutputStream(cacheFile));
			}
		}
	}
	
	public static String encodeTag(String dirId, String mediaId) {
		return dirId + File.separator + mediaId;
	}
	
	public static String[] decodeTag(String tag) {
		return tag.split(File.separator);
	}
	
	public File saveTopLevelEtag(String etag) throws IOException {
		FileOutputStream out = new FileOutputStream(mTopLevelEtagPath);
		try {
			out.write(etag.getBytes());
		} finally {
			out.close();
		}
		return mTopLevelEtagPath;
	}
	
	public String loadTopLevelEtag() throws IOException {
		if (mTopLevelEtagPath.exists()) {
			return IOUtil.readString(mTopLevelEtagPath, Charset.defaultCharset());
		} else {
			return null;
		}
	}
	
	public boolean deleteImageCaches(String dirId, final String mediaId) {
		File dir = toDirEntryPath(dirId).getParentFile();
		File[] caches = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String name = pathname.getName();
				return pathname.isFile() && name.startsWith(mediaId) && !name.endsWith(".entry");
			}
		});
		
		boolean deleted = false;
		for (File cache : caches) {
			deleted = deleted || cache.delete();
		}
		return deleted;
	}
	
	public File saveDirEntry(AlbumEntry entry) throws IOException {
		String json = JsonUtil.toJson(entry);
		File file = toDirEntryPath(entry.getID());
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		try {
			out.write(json.getBytes());
		} finally {
			out.close();
		}
		return file;
	}
	
	public AlbumEntry loadDirEntry(String dirId) throws IOException {
		File file = toDirEntryPath(dirId);
		if (file.exists()) {
			String json = IOUtil.readString(file, Charset.defaultCharset());
			return JsonUtil.fromJson(json, AlbumEntry.class);
		} else {
			return null;
		}
	}
	
	public File saveMediaEntry(LitePhotoEntry entry) throws IOException {
		String json = JsonUtil.toJson(entry);
		File file = toMediaEntryPath(entry.gphotoAlbumId, entry.gphotoId);
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		try {
			out.write(json.getBytes());
		} finally {
			out.close();
		}
		return file;
	}
	
	public LitePhotoEntry loadMediaEntry(String dirId, String mediaId) throws IOException {
		File file = toMediaEntryPath(dirId, mediaId);
		if (file.exists()) {
			String json = IOUtil.readString(file, Charset.defaultCharset());
			LitePhotoEntry entry = JsonUtil.fromJson(json, LitePhotoEntry.class);
			return entry;
		} else {
			return null;
		}
	}
	
	public String[] listCachedDirIds() {
		return mRootPath.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				return !filename.equals("etag");
			}
		});
	}
	
	public List<String> listCachedMedia(String dirId, boolean tag) {
		return listCachedMedia(dirId, tag, 0);
	}
	
	public List<String> listCachedMedia(String dirId, boolean tag, int limit) {
//		Log.d("PAF", "listCachedMediaTags");
		File dir = toDirEntryPath(dirId).getParentFile();
		File[] files = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String name = pathname.getName();
				return name.endsWith(".entry") && !name.equals("dir.entry");
			}
		});
		
		ArrayList<String> tags = new ArrayList<String>();
		final int total = 0 < limit ? Math.min(files.length, limit) : files.length;
		for (int i = 0; i < total; i++) {
			if (tag) {
				tags.add(encodeTag(dirId, files[i].getName().replace(".entry", "")));
			} else {
				tags.add(files[i].getName().replace(".entry", ""));
			}
		}
		
		return tags;
	}
	
	protected File toMiniThumbCachePath(String dirId, String mediaId) {
		File dir = new File(mRootPath, dirId);
		return new File(dir, mediaId + ".s");
	}
	
	protected File toLargeThumbCachePath(String dirId, String mediaId) {
		File dir = new File(mRootPath, dirId);
		return new File(dir, mediaId + ".m");
	}
	
	protected File toFullImageCachePath(String dirId, String mediaId) {
		File dir = new File(mRootPath, dirId);
		return new File(dir, mediaId + ".l");
	}
	
	protected File toDirEntryPath(String dirId) {
		File dir = new File(mRootPath, dirId);
		return new File(dir, "dir.entry");
	}
	
	protected File toMediaEntryPath(String dirId, String mediaId) {
		File dir = new File(mRootPath, dirId);
		return new File(dir, mediaId + ".entry");
	}
	
	protected void setDirty(String dirId, boolean dirty) throws IOException {
		File file = new File(toDirEntryPath(dirId).getParent(), "dirty");
		if (dirty) {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
		} else {
			file.delete();
		}
	}
	
	protected boolean isDirty(String dirId) {
		return new File(toDirEntryPath(dirId).getParent(), "dirty").exists();
	}
	
	public static String computeMiniThumbParam() {
		return "150c";
	}
	
	public static String computeLargeThumbParam() {
		return "800u";
	}
	
	public static interface MediaEntry {
		String getDirId();
		String getMediaId();
//		String getName();
		File getCachePath();
	}
}
