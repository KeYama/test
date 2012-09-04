package jp.co.johospace.jsphoto.v2.onlineservice.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedFiles;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedMediaDirs;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedMedias;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedMetadata;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedThumbs;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedVersions;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CServiceIdentifier;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/**
 * 外部サービスキャッシュ実装
 */
public class ExternalServiceCacheImpl implements ExternalServiceCache {

	private final SQLiteDatabase mDb = OpenHelper.cache.getDatabase();
	private final Context mContext;
	private final File mContentRootDir;
	
	public ExternalServiceCacheImpl(Context context) {
		super();
		mContext = context;
		mContentRootDir = new File(mContext.getExternalCacheDir(), getClass().getSimpleName());
		mContentRootDir.mkdirs();
	}
	
	@Override
	public List<Directory> listDirectories(String serviceType, String serviceAccount, boolean includeEmptyDirs) {
		Cursor c = mDb.query(CCachedVersions.$TABLE,
				new String[] {
						CCachedVersions.VERSION
				},
				CCachedVersions.SERVICE_TYPE + " = ?" +
						" AND " + CCachedVersions.SERVICE_ACCOUNT + " = ?",
				new String[] {serviceType, serviceAccount},
				null, null, null, "1");
		try {
			if (!c.moveToFirst()) {
				return null;
			}
		} finally {
			c.close();
		}
		
		c = mDb.query(CCachedMediaDirs.$TABLE,
				new String[] {
						CCachedMediaDirs.DIR_ID,
						CCachedMediaDirs.DIR_NAME,
						CCachedMediaDirs.VERSION,
						CCachedMediaDirs.UPDATED,
						CCachedMediaDirs.MEDIA_COUNT,
				},
				CCachedMediaDirs.SERVICE_TYPE + " = ?" +
						" AND " + CCachedMediaDirs.SERVICE_ACCOUNT + " = ?"
						+ (includeEmptyDirs ? "" : " AND 0 < " + CCachedMediaDirs.MEDIA_COUNT),
				new String[] {serviceType, serviceAccount},
				null, null, null);
		try {
			ArrayList<Directory> dirs = new ArrayList<Directory>();
			while (c.moveToNext()) {
				Directory dir = new Directory();
				dir.id = c.getString(0);
				dir.name = c.getString(1);
				dir.version = c.getLong(2);
				dir.updatedTimestamp = c.getLong(3);
				dir.mediaCount = c.getInt(4);
				dirs.add(dir);
			}
			return dirs;
		} finally {
			c.close();
		}
	}
	
	@Override
	public void updateDirectories(String serviceType, String serviceAccount, List<Directory> dirs) {
		mDb.beginTransaction();
		try {
			for (Directory dir : dirs) {
				if (dir.deleted) {
					deleteCachedMedias(serviceType, serviceAccount, dir.id);
					mDb.delete(CCachedMediaDirs.$TABLE,
							CCachedMediaDirs.SERVICE_TYPE + " = ?" +
									" AND " + CCachedMediaDirs.SERVICE_ACCOUNT + " = ?" +
									" AND " + CCachedMediaDirs.DIR_ID + " = ?",
							new String[] {serviceType, serviceAccount, dir.id});
				} else {
					ContentValues values = new ContentValues();
					Cursor c = mDb.query(CCachedMediaDirs.$TABLE,
							new String[] {
									/*0*/CCachedMediaDirs._ID,
									/*1*/CCachedMediaDirs.DIR_ID,
									/*2*/CCachedMediaDirs.DIR_NAME,
									/*3*/CCachedMediaDirs.VERSION,
									/*4*/CCachedMediaDirs.UPDATED,
									/*5*/CCachedMediaDirs.MEDIA_CACHED,
									/*6*/CCachedMediaDirs.MEDIA_DIRTY,
									/*7*/CCachedMediaDirs.MEDIA_VERSION,
									/*8*/CCachedMediaDirs.MEDIA_COUNT,
							},
							CCachedMediaDirs.SERVICE_TYPE + " = ?" +
									" AND " + CCachedMediaDirs.SERVICE_ACCOUNT + " = ?" +
									" AND " + CCachedMediaDirs.DIR_ID + " = ?",
							new String[] {serviceType, serviceAccount, dir.id},
							null, null, null, "1");
					try {
						values.put(CCachedMediaDirs.SERVICE_TYPE, serviceType);
						values.put(CCachedMediaDirs.SERVICE_ACCOUNT, serviceAccount);
						values.put(CCachedMediaDirs.DIR_ID, dir.id);
						if (c.moveToFirst()) {
//							values.put(CCachedMediaDirs._ID, c.getLong(0));
//							values.put(CCachedMediaDirs.DIR_ID, c.getString(1));
							values.put(CCachedMediaDirs.DIR_NAME, c.getString(2));
							values.put(CCachedMediaDirs.VERSION, c.getLong(3));
							values.put(CCachedMediaDirs.UPDATED, c.getLong(4));
							values.put(CCachedMediaDirs.MEDIA_CACHED, c.getInt(5));
							values.put(CCachedMediaDirs.MEDIA_DIRTY, c.getInt(6));
							values.put(CCachedMediaDirs.MEDIA_VERSION, c.isNull(7) ? null : c.getLong(7));
							values.put(CCachedMediaDirs.MEDIA_COUNT, c.getInt(8));
						}
					} finally {
						c.close();
					}
					
					values.put(CCachedMediaDirs.DIR_NAME, dir.name);
					values.put(CCachedMediaDirs.VERSION, dir.version);
					values.put(CCachedMediaDirs.UPDATED, dir.updatedTimestamp);
					Long prevMediaVersion = values.getAsLong(CCachedMediaDirs.MEDIA_VERSION);
					boolean dirty;
					if (prevMediaVersion != null && dir.mediaVersion != null) {
						dirty = !prevMediaVersion.equals(dir.mediaVersion);
					} else {
						dirty = (prevMediaVersion == null && dir.mediaVersion != null)
								|| (prevMediaVersion != null && dir.mediaVersion == null);
					}
					values.put(CCachedMediaDirs.MEDIA_DIRTY, dirty ? 1 : 0);
					values.put(CCachedMediaDirs.MEDIA_VERSION, dir.mediaVersion);
					values.put(CCachedMediaDirs.MEDIA_COUNT, dir.mediaCount);
					
					mDb.replaceOrThrow(CCachedMediaDirs.$TABLE, null, values);
				}
			}
			
			ContentValues values = new ContentValues();
			values.put(CCachedVersions.LAST_UPDATED, System.currentTimeMillis());
			mDb.update(CCachedVersions.$TABLE, values,
					CCachedVersions.SERVICE_TYPE + " = ?" +
							" AND " + CCachedVersions.SERVICE_ACCOUNT + " = ?",
					new String[] {serviceType, serviceAccount});
			
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
		// TODO
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
		pref.edit().putLong(String.format("%s|%s|%s", getClass().getSimpleName(), serviceType, serviceAccount), System.currentTimeMillis()).commit();
	}
	
	private void deleteCachedMedias(String serviceType, String serviceAccount, String dirId) {
		Cursor c = mDb.query(CCachedMedias.$TABLE,
				new String[] {CCachedMedias.MEDIA_ID,},
				CCachedMedias.SERVICE_TYPE + " = ?" +
						" AND " + CCachedMedias.SERVICE_ACCOUNT + " = ?" +
						" AND " + CCachedMedias.DIR_ID + " = ?",
				new String[] {
						serviceType, serviceAccount, dirId
				}, null, null, null);
		try {
			while (c.moveToNext()) {
				String id = c.getString(0);
				File file = toLargeThumbFile(serviceType, serviceType, id);
				file.delete();
				file = toContentFile(serviceType, serviceType, id, NO_UPDATE, null);
				file.delete();
				mDb.delete(CCachedMedias.$TABLE,
						CCachedMedias._ID + " = ?", new String[] {id});
			}
		} finally {
			c.close();
		}
	}
	
	@Override
	public List<Media> listMedias(String serviceType, String serviceAccount, String dirId, Integer limit, boolean ignoreCached, boolean albumPhoto) {
//long g = SystemClock.elapsedRealtime();/*$debug$*/
		Cursor c = mDb.query(CCachedMediaDirs.$TABLE,
				new String[] {
						CCachedMediaDirs.MEDIA_CACHED
				},
				CCachedMediaDirs.SERVICE_TYPE + " = ?" +
						" AND " + CCachedMediaDirs.SERVICE_ACCOUNT + " = ?" +
						" AND " + CCachedMediaDirs.DIR_ID + " = ?",
				new String[] {serviceType, serviceAccount, dirId},
				null, null, null, "1");
		try {
			if (c.moveToFirst()) {
				if (!ignoreCached && c.getInt(0) != 1) {
					return null;
				}
			} else {
				return null;
			}
		} finally {
			c.close();
		}
		
//long st = SystemClock.elapsedRealtime();/*$debug$*/
		String[] colmuns = {
				CCachedMedias.MEDIA_ID,
				CCachedMedias.MEDIA_URI,
				CCachedMedias.FILE_NAME,
				CCachedMedias.PRODUCTION_DATE,
				CCachedMedias.THUMBNAIL_DATA,
				CCachedMedias.VERSION,
				CCachedMedias.UPDATED,
		};
		String order = CCachedMedias.SERVICE_TYPE + ", " +
				CCachedMedias.SERVICE_ACCOUNT + ", " +
				CCachedMedias.DIR_ID + ", " +
				CCachedMedias.PRODUCTION_DATE + " DESC, " +
				CCachedMedias.MEDIA_ID;
		if (albumPhoto) {
			c = mDb.query(CCachedMedias.$TABLE,
					colmuns,
					CCachedMedias.SERVICE_TYPE + " = ?" +
							" AND " + CCachedMedias.SERVICE_ACCOUNT + " = ?" +
							" AND " + CCachedMedias.DIR_ID + " = ?" +
							" AND " + CCachedMedias.ALBUM_PHOTO + " = 1",
					new String[] {
							serviceType, serviceAccount, dirId
					}, null, null,
					order,
					limit == null ? null : String.valueOf(limit));
		} else {
			c = mDb.query(CCachedMedias.$TABLE,
					colmuns,
					CCachedMedias.SERVICE_TYPE + " = ?" +
							" AND " + CCachedMedias.SERVICE_ACCOUNT + " = ?" +
							" AND " + CCachedMedias.DIR_ID + " = ?",
					new String[] {
							serviceType, serviceAccount, dirId
					}, null, null,
					order,
					limit == null ? null : String.valueOf(limit));
		}
//System.out.println(String.format("        media query : %d msec", SystemClock.elapsedRealtime() - st));/*$debug$*/
		try {
			ArrayList<Media> medias = new ArrayList<Media>();
			while (c.moveToNext()) {
//st = SystemClock.elapsedRealtime();/*$debug$*/
				Media media = new Media();
				media.service = serviceType;
				media.account = serviceAccount;
				media.directoryId = dirId;
				media.mediaId = c.getString(0);
				media.mediaUri = c.getString(1);
				media.fileName = c.getString(2);
				media.productionDate = c.getLong(3);
				media.thumbnailData = c.getString(4);
				media.version = c.getLong(5);
				media.updatedTimestamp = c.getLong(6);
				medias.add(media);
//System.out.println(String.format("          media fetch : %d msec", SystemClock.elapsedRealtime() - st));/*$debug$*/
			}
			return medias;
		} finally {
			c.close();
//System.out.println(String.format("★listMedias total %d msec", SystemClock.elapsedRealtime() - g));/*$debug$*/
		}
	}
	
	@Override
	public void updateMedias(String serviceType, String serviceAccount, String dirId, List<Media> medias, boolean updateCached) {
		mDb.beginTransaction();
		try {
			for (Media media : medias) {
				if (!dirId.equals(media.directoryId)) {
					throw new IllegalArgumentException(
							String.format("dirId is not match. %s vs %s", dirId, media.directoryId));
				}
				
				if (media.deleted) {
					File content = toContentFile(serviceType, serviceAccount, media.mediaId, NO_UPDATE, null);
					if (content != null) {
						content.delete();
					}
					File largeThumb = toLargeThumbFile(serviceType, serviceAccount, media.mediaId);
					if (content != null) {
						largeThumb.delete();
					}
					mDb.delete(CCachedMedias.$TABLE,
							CCachedMedias.SERVICE_TYPE + " = ?" +
									" AND " + CCachedMedias.SERVICE_ACCOUNT + " = ?" +
									" AND " + CCachedMedias.MEDIA_ID + " = ?",
							new String[] {serviceType, serviceAccount, media.mediaId});
				} else {
					StringBuilder sql = new StringBuilder();
					sql.append("INSERT OR REPLACE INTO " + CCachedMedias.$TABLE + "(")
						.append(CCachedMedias.SERVICE_TYPE).append(",")
						.append(CCachedMedias.SERVICE_ACCOUNT).append(",")
						.append(CCachedMedias.DIR_ID).append(",")
						.append(CCachedMedias.MEDIA_ID).append(",")
						.append(CCachedMedias.MEDIA_URI).append(",")
						.append(CCachedMedias.FILE_NAME).append(",")
						.append(CCachedMedias.PRODUCTION_DATE).append(",")
						.append(CCachedMedias.THUMBNAIL_DATA).append(",")
						.append(CCachedMedias.VERSION).append(",")
						.append(CCachedMedias.UPDATED).append(")")
						.append(" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					Object[] args = {
							serviceType,
							serviceAccount,
							media.directoryId,
							media.mediaId,
							media.mediaUri,
							media.fileName,
							media.productionDate,
							media.thumbnailData,
							media.version,
							media.updatedTimestamp,
					};
					mDb.execSQL(sql.toString(), args);
				}
			}
			
			// アルバム代表フォト更新
//long st = SystemClock.elapsedRealtime();/*$debug$*/
			ContentValues values = new ContentValues();
			values.put(CCachedMedias.ALBUM_PHOTO, 0);
			mDb.update(CCachedMedias.$TABLE, values,
					CCachedMedias.SERVICE_TYPE + " = ?" +
							" AND " + CCachedMedias.SERVICE_ACCOUNT + " = ?" +
							" AND " + CCachedMedias.DIR_ID + " = ?" +
							" AND " + CCachedMedias.ALBUM_PHOTO + " = 1",
					new String[] {serviceType, serviceAccount, dirId});
			String sql =
					"UPDATE " + CCachedMedias.$TABLE +
					" SET " + CCachedMedias.ALBUM_PHOTO + " = 1" +
							" WHERE " + CCachedMedias._ID + " IN (" +
									"SELECT " + CCachedMedias._ID +
									" FROM " + CCachedMedias.$TABLE +
									" WHERE " + CCachedMedias.SERVICE_TYPE + " = ?" +
											" AND " + CCachedMedias.SERVICE_ACCOUNT + " = ?" +
											" AND " + CCachedMedias.DIR_ID + " = ?" +
									" ORDER BY " + CCachedMedias.PRODUCTION_DATE + " DESC, " +
											CCachedMedias.MEDIA_ID +
									" LIMIT 15)";
			mDb.execSQL(sql, new String[] {serviceType, serviceAccount, dirId});
//System.out.println(String.format("AlbumPhoto更新： %d msec", SystemClock.elapsedRealtime() - st));/*$debug$*/
			
			if (updateCached) {
				long current = System.currentTimeMillis();
				values.clear();
				values.put(CCachedMediaDirs.MEDIA_CACHED, 1);
				values.put(CCachedMediaDirs.MEDIA_DIRTY, 0);
				values.put(CCachedMediaDirs.LAST_UPDATED, current);
				mDb.update(CCachedMediaDirs.$TABLE, values,
						CCachedMediaDirs.SERVICE_TYPE + " = ?" +
								" AND " + CCachedMediaDirs.SERVICE_ACCOUNT + " = ?" +
								" AND " + CCachedMediaDirs.DIR_ID + " = ?",
						new String[] {serviceType, serviceAccount, dirId});
				
				values.clear();
				values.put(CCachedVersions.LAST_UPDATED, current);
				mDb.update(CCachedVersions.$TABLE, values,
						CCachedVersions.SERVICE_TYPE + " = ?" +
								" AND " + CCachedVersions.SERVICE_ACCOUNT + " = ?",
						new String[] {serviceType, serviceAccount});
			}
			
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
		// TODO
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
		pref.edit().putLong(String.format("%s|%s|%s", getClass().getSimpleName(), serviceType, serviceAccount), System.currentTimeMillis()).commit();
	}
	
	@Override
	public List<Metadata> getMetadata(String serviceType, String serviceAccount, String mediaId) {
		Cursor c = mDb.query(CCachedMedias.$TABLE,
				new String[] {CCachedMedias.METADATA_CACHED},
				CCachedMedias.SERVICE_TYPE + " = ?" +
						" AND " + CCachedMedias.SERVICE_ACCOUNT + " = ?" +
						" AND " + CCachedMedias.MEDIA_ID + " = ?",
				new String[] {serviceType, serviceAccount, mediaId},
				null, null, null);
		try {
			if (c.moveToFirst() && c.getInt(0) == 1) {
			} else {
				return null;
			}
		} finally {
			c.close();
		}
		
		c = mDb.query(CCachedMetadata.$TABLE,
				new String[] {
						CCachedMetadata.METADATA_TYPE,
						CCachedMetadata.METADATA,
				},
				CCachedMetadata.SERVICE_TYPE + " = ?" +
						" AND " + CCachedMetadata.SERVICE_ACCOUNT + " = ?" +
						" AND " + CCachedMetadata.MEDIA_ID + " = ?",
				new String[] {
						serviceType, serviceAccount, mediaId
				},
				null, null, null);
		try {
			ArrayList<Metadata> list = new ArrayList<Metadata>();
			while (c.moveToNext()) {
				Metadata metadata = new Metadata();
				metadata.type = c.getString(0);
				metadata.data = c.getString(1);
				list.add(metadata);
			}
			return list;
		} finally {
			c.close();
		}
	}
	
	@Override
	public void updateMetadata(String serviceType, String serviceAccount, String mediaId, Collection<Metadata> metadata) {
		mDb.beginTransaction();
		try {
			mDb.delete(CCachedMetadata.$TABLE,
					CCachedMetadata.SERVICE_TYPE + " = ?" +
							" AND " + CCachedMetadata.SERVICE_ACCOUNT + " = ?" +
							" AND " + CCachedMetadata.MEDIA_ID + " = ?",
					new String[] {serviceType, serviceAccount, mediaId});
			ContentValues values = new ContentValues();
			for (Metadata meta : metadata) {
				values.put(CCachedMetadata.SERVICE_TYPE, serviceType);
				values.put(CCachedMetadata.SERVICE_ACCOUNT, serviceAccount);
				values.put(CCachedMetadata.MEDIA_ID, mediaId);
				values.put(CCachedMetadata.METADATA_TYPE, meta.type);
				values.put(CCachedMetadata.METADATA, meta.data);
				mDb.insertOrThrow(CCachedMetadata.$TABLE, null, values);
			}
			
			values.clear();
			values.put(CCachedMedias.METADATA_CACHED, 1);
			mDb.update(CCachedMedias.$TABLE, values,
					CCachedMedias.SERVICE_TYPE + " = ?" +
							" AND " + CCachedMedias.SERVICE_ACCOUNT + " = ?" +
							" AND " + CCachedMedias.MEDIA_ID + " = ?",
					new String[] {serviceType, serviceAccount, mediaId});
			
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}
	
	@Override
	public byte[] getThumbnail(String serviceType, String serviceAccount, String mediaId, int sizeHint) {
		Cursor c = mDb.query(CCachedThumbs.$TABLE,
				new String[] {
						CCachedThumbs.THUMBNAIL
				},
				CCachedThumbs.SERVICE_TYPE + " = ?" +
						" AND " + CCachedThumbs.SERVICE_ACCOUNT + " = ?" +
						" AND " + CCachedThumbs.MEDIA_ID + " = ?" +
						" AND " + CCachedThumbs.SIZE_HINT + " = ?",
				new String[] {serviceType, serviceAccount, mediaId, String.valueOf(sizeHint)},
				null, null, null);
		try {
			if (c.moveToFirst()) {
				return c.getBlob(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	@Override
	public void updateThumbnail(String serviceType, String serviceAccount, String mediaId, int sizeHint, byte[] data) {
		ContentValues values = new ContentValues();
		values.put(CCachedThumbs.SERVICE_TYPE, serviceType);
		values.put(CCachedThumbs.SERVICE_ACCOUNT, serviceAccount);
		values.put(CCachedThumbs.MEDIA_ID, mediaId);
		values.put(CCachedThumbs.SIZE_HINT, sizeHint);
		values.put(CCachedThumbs.THUMB_LENGTH, data.length);
		values.put(CCachedThumbs.THUMBNAIL, data);
		mDb.replaceOrThrow(CCachedThumbs.$TABLE, null, values);
	}
	
	@Override
	public File getLargeThumbnail(String serviceType, String serviceAccount, String mediaId) {
		File cache = toLargeThumbFile(serviceType, serviceAccount, mediaId);
		if (cache != null && cache.exists()) {
			return cache;
		} else {
			return null;
		}
	}
	
	@Override
	public File updateLargeThumbnail(String serviceType, String serviceAccount, String mediaId, InputStream content) throws IOException {
		try {
			File cache = toLargeThumbFile(serviceType, serviceAccount, mediaId);
			if (cache != null) {
				FileOutputStream out = new FileOutputStream(cache);
				try {
					IOUtil.copy(content, out);
					return cache;
				} finally {
					out.close();
				}
			} else {
				return null;
			}
			
		} finally {
			content.close();
		}
	}
	
	@Override
	public File getMediaContent(String serviceType, String serviceAccount, String mediaId, String[] out_contentType) {
		File cache = toContentFile(serviceType, serviceAccount, mediaId, NO_UPDATE, out_contentType);
		if (cache != null && cache.exists()) {
			return cache;
		} else {
			return null;
		}
	}
	
	@Override
	public File updateMediaContent(String serviceType, String serviceAccount, String mediaId, InputStream content, String contentType) throws IOException {
		try {
			File cache = toContentFile(serviceType, serviceAccount, mediaId, contentType, null);
			if (cache != null) {
				boolean existsBefore = cache.exists();
				FileOutputStream out = new FileOutputStream(cache);
				try {
					IOUtil.copy(content, out);
					if (existsBefore) {
						mDb.delete(CCachedThumbs.$TABLE,
								CCachedThumbs.SERVICE_TYPE + " = ?" +
										" AND " + CCachedThumbs.SERVICE_ACCOUNT + " = ?" +
										" AND " + CCachedThumbs.MEDIA_ID + " = ?",
								new String[] {serviceType, serviceAccount, mediaId});
						File largeThumb = getLargeThumbnail(serviceType, serviceAccount, mediaId);
						if (largeThumb != null) {
							largeThumb.delete();
						}
					}
					return cache;
				} finally {
					out.close();
				}
			} else {
				return null;
			}
			
		} finally {
			content.close();
		}
	}
	
	@Override
	public Long getCachedRootVersion(String serviceType, String serviceAccount) {
		Cursor c = mDb.query(CCachedVersions.$TABLE,
				new String[] {
						CCachedVersions.VERSION,
				},
				CCachedVersions.SERVICE_TYPE + " = ?" +
						" AND " + CCachedVersions.SERVICE_ACCOUNT + " = ?",
				new String[] {serviceType, serviceAccount},
				null, null, null, "1");
		try {
			if (c.moveToFirst() && !c.isNull(0)) {
				return c.getLong(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	@Override
	public Long getLastUpdated(String serviceType, String serviceAccount) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
		long t = pref.getLong(String.format("%s|%s|%s", getClass().getSimpleName(), serviceType, serviceAccount), Long.MIN_VALUE);
		return t == Long.MIN_VALUE ? null : t;
//		Cursor c = mDb.query(CCachedVersions.$TABLE,
//				new String[] {CCachedVersions.LAST_UPDATED},
//				CCachedVersions.SERVICE_TYPE + " = ?" +
//						" AND " + CCachedVersions.SERVICE_ACCOUNT + " = ?",
//				new String[] {serviceType, serviceAccount},
//				null, null, null, "1");
//		try {
//			if (c.moveToFirst()) {
//				return c.isNull(0) ? null : c.getLong(0);
//			} else {
//				return null;
//			}
//		} finally {
//			c.close();
//		}
	}
	
	@Override
	public Long getLastUpdated(String serviceType, String serviceAccount, String dirId) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
		long t = pref.getLong(String.format("%s|%s|%s", getClass().getSimpleName(), serviceType, serviceAccount), Long.MIN_VALUE);
		return t == Long.MIN_VALUE ? null : t;
//		Cursor c = mDb.query(CCachedMediaDirs.$TABLE,
//				new String[] {CCachedMediaDirs.LAST_UPDATED},
//				CCachedMediaDirs.SERVICE_TYPE + " = ?" +
//						" AND " + CCachedMediaDirs.SERVICE_ACCOUNT + " = ?" +
//						" AND " + CCachedMediaDirs.DIR_ID + " = ?",
//				new String[] {serviceType, serviceAccount, dirId},
//				null, null, null, "1");
//		try {
//			if (c.moveToFirst()) {
//				return c.isNull(0) ? null : c.getLong(0);
//			} else {
//				return null;
//			}
//		} finally {
//			c.close();
//		}
	}
	
	
	@Override
	public Long getCachedMediaVersion(String serviceType, String serviceAccount, String dirId) {
		Cursor c = mDb.query(CCachedMediaDirs.$TABLE,
				new String[] {
						CCachedMediaDirs.MEDIA_VERSION,
				},
				CCachedMediaDirs.SERVICE_TYPE + " = ?" +
						" AND " + CCachedMediaDirs.SERVICE_ACCOUNT + " = ?" +
						" AND " + CCachedMediaDirs.DIR_ID + " = ?",
				new String[] {serviceType, serviceAccount, dirId},
				null, null, null, "1");
		try {
			if (c.moveToFirst() && !c.isNull(0)) {
				return c.getLong(0);
			} else {
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	@Override
	public boolean isCachedMediaDirty(String serviceType, String serviceAccount, String dirId) {
		Cursor c = mDb.query(CCachedMediaDirs.$TABLE,
				new String[] {
						CCachedMediaDirs.MEDIA_DIRTY,
				},
				CCachedMediaDirs.SERVICE_TYPE + " = ?" +
						" AND " + CCachedMediaDirs.SERVICE_ACCOUNT + " = ?" +
						" AND " + CCachedMediaDirs.DIR_ID + " = ?",
				new String[] {serviceType, serviceAccount, dirId},
				null, null, null, "1");
		try {
			if (c.moveToFirst() && !c.isNull(0)) {
				return c.getInt(0) == 1;
			} else {
				return true;
			}
		} finally {
			c.close();
		}
	}
	
	
	private static final String NO_UPDATE = new String();
	private String createFileId(String serviceType, String serviceAccount, String mediaId, String contentType, String[] out_contentType) {
		synchronized (getClass()) {
			Cursor c = mDb.query(CCachedFiles.$TABLE,
					new String[] {CCachedFiles._ID, CCachedFiles.CONTENT_TYPE},
					CCachedFiles.SERVICE_TYPE + " = ?" +
							" AND " + CCachedFiles.SERVICE_ACCOUNT + " = ?" +
							" AND " + CCachedFiles.MEDIA_ID + " = ?",
					new String[] {serviceType, serviceAccount, mediaId},
					null, null, null, "1");
			try {
				if (c.moveToFirst()) {
					if (contentType != NO_UPDATE) {
						ContentValues values = new ContentValues();
						values.put(CCachedFiles.CONTENT_TYPE, contentType);
						mDb.update(CCachedFiles.$TABLE,
								values, CCachedFiles._ID + " = ?", new String[] {c.getString(0)});
					}
					if (out_contentType != null && 0 < out_contentType.length) {
						out_contentType[0] = contentType != NO_UPDATE ? contentType : c.getString(1);
					}
					return c.getString(0);
				} else {
					ContentValues values = new ContentValues();
					values.put(CCachedFiles.SERVICE_TYPE, serviceType);
					values.put(CCachedFiles.SERVICE_ACCOUNT, serviceAccount);
					values.put(CCachedFiles.MEDIA_ID, mediaId);
					if (contentType != NO_UPDATE) {
						values.put(CCachedFiles.CONTENT_TYPE, contentType);
					}
					if (out_contentType != null && 0 < out_contentType.length) {
						out_contentType[0] = contentType != NO_UPDATE ? contentType : null;
					}
					long id = mDb.insert(CCachedFiles.$TABLE, null, values);
					return 0 <= id ? String.valueOf(id) : null;
				}
			} finally {
				c.close();
			}
		}
	}
	
	@Override
	public String getMediaContentType(String serviceType,
			String serviceAccount, String mediaId) {
		String[] contentType = new String[1];
		createFileId(serviceType, serviceAccount, mediaId, NO_UPDATE, contentType);
		return contentType[0];
	}
	
	@Override
	public void updateMediaContentType(String serviceType,
			String serviceAccount, String mediaId, String contentType) {
		createFileId(serviceType, serviceAccount, mediaId, contentType, null);
	}
	
	private File toLargeThumbFile(String serviceType, String serviceAccount, String mediaId) {
		String id = createFileId(serviceType, serviceAccount, mediaId, NO_UPDATE, null);
		return id != null ? toLargeThumbFile(id) : null;
//		Cursor c = mDb.query(CCachedFiles.$TABLE,
//				new String[] {CCachedFiles._ID},
//				CCachedFiles.SERVICE_TYPE + " = ?" +
//						" AND " + CCachedFiles.SERVICE_ACCOUNT + " = ?" +
//						" AND " + CCachedFiles.MEDIA_ID + " = ?",
//				new String[] {serviceType, serviceAccount, mediaId},
//				null, null, null, "1");
//		try {
//			if (c.moveToFirst()) {
//				String id = c.getString(0);
//				return toLargeThumbFile(id);
//			} else {
//				return null;
//			}
//		} finally {
//			c.close();
//		}
	}
	
	private File toContentFile(String serviceType, String serviceAccount, String mediaId, String contentType, String[] out_contentType) {
		String id = createFileId(serviceType, serviceAccount, mediaId, contentType, out_contentType);
		return id != null ? toContentFile(id) : null;
//		Cursor c = mDb.query(CCachedFiles.$TABLE,
//				new String[] {CCachedFiles._ID},
//				CCachedFiles.SERVICE_TYPE + " = ?" +
//						" AND " + CCachedFiles.SERVICE_ACCOUNT + " = ?" +
//						" AND " + CCachedFiles.MEDIA_ID + " = ?",
//				new String[] {serviceType, serviceAccount, mediaId},
//				null, null, null, "1");
//		try {
//			if (c.moveToFirst()) {
//				String id = c.getString(0);
//				return toContentFile(id);
//			} else {
//				return null;
//			}
//		} finally {
//			c.close();
//		}
	}
	
	private File toContentFile(String id) {
		return new File(mContentRootDir, String.format("%s.l", id));
	}
	
	private File toLargeThumbFile(String id) {
		return new File(mContentRootDir, String.format("%s.m", id));
	}
	
	@Override
	public void clearThumbnailCache(String serviceType, String serviceAccount) {
		ArrayList<String> wheres = new ArrayList<String>();
		ArrayList<String> args = new ArrayList<String>();
		if (serviceType != null) {
			wheres.add(CCachedThumbs.SERVICE_TYPE + " = ?");
			args.add(serviceType);
			if (serviceAccount != null) {
				wheres.add(CCachedThumbs.SERVICE_ACCOUNT + " = ?");
				args.add(serviceAccount);
			}
		}
		
		mDb.delete(CCachedThumbs.$TABLE,
				wheres.isEmpty() ? null : TextUtils.join(" AND ", wheres),
				args.isEmpty() ? null : args.toArray(new String[0]));
	}
	
	@Override
	public void clearLargeThumbnailCache(String serviceType, String serviceAccount) {
		clearFileCache(serviceType, serviceAccount,
				new Deleter() {
					@Override
					public void delete(String id) {
						File cache = toLargeThumbFile(id);
						cache.delete();
					}
		});
	}
	
	@Override
	public void clearMediaContentCache(String serviceType, String serviceAccount) {
		clearFileCache(serviceType, serviceAccount,
				new Deleter() {
					@Override
					public void delete(String id) {
						File cache = toContentFile(id);
						cache.delete();
					}
		});
	}
	
	private interface Deleter {
		void delete(String id);
	}
	
	private void clearFileCache(String serviceType, String serviceAccount, Deleter deleter) {
		ArrayList<String> wheres = new ArrayList<String>();
		ArrayList<String> args = new ArrayList<String>();
		if (serviceType != null) {
			wheres.add(CCachedFiles.SERVICE_TYPE + " = ?");
			args.add(serviceType);
			if (serviceAccount != null) {
				wheres.add(CCachedFiles.SERVICE_ACCOUNT + " = ?");
				args.add(serviceAccount);
			}
		}
		
		Cursor c = mDb.query(CCachedFiles.$TABLE,
				new String[] {CCachedFiles._ID},
				wheres.isEmpty() ? null : TextUtils.join(" AND ", wheres),
				args.isEmpty() ? null : args.toArray(new String[0]),
				null, null, null);
		try {
			while (c.moveToNext()) {
				String id = c.getString(0);
				deleter.delete(id);
			}
		} finally {
			c.close();
		}
	}
	
	@Override
	public void clearMediaTree(String serviceType, String serviceAccount) {
		ArrayList<String> wheres = new ArrayList<String>();
		ArrayList<String> args = new ArrayList<String>();
		if (serviceType != null) {
			wheres.add(CServiceIdentifier.SERVICE_TYPE + " = ?");
			args.add(serviceType);
			if (serviceAccount != null) {
				wheres.add(CServiceIdentifier.SERVICE_ACCOUNT + " = ?");
				args.add(serviceAccount);
			}
		}
		
		String[] tables = {
				CCachedFiles.$TABLE,
				CCachedThumbs.$TABLE,
				CCachedMetadata.$TABLE,
				CCachedMedias.$TABLE,
				CCachedMediaDirs.$TABLE,
				CCachedVersions.$TABLE,
		};
		
		mDb.beginTransaction();
		try {
			for (String table : tables) {
				mDb.delete(table,
						TextUtils.join(" AND ", wheres),
						args.toArray(new String[0]));
			}
			
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}
	
	public File getContentCacheDir() {
		return mContentRootDir;
	}
}
