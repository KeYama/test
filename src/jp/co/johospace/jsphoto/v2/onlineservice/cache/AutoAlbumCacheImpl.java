package jp.co.johospace.jsphoto.v2.onlineservice.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.util.RowHandler;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedAutoAlbum;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedAutoAlbumCategories;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedAutoAlbumDividers;
import jp.co.johospace.jsphoto.v2.onlineservice.cache.db.CCachedAutoAlbumMedias;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ContentsNotModifiedException;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.RespondedContents;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Memory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.MemoryDivider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * オートアルバムキャッシュ 実装
 */
public class AutoAlbumCacheImpl implements AutoAlbumCache {

	private final SQLiteDatabase mDb = OpenHelper.cache.getDatabase();
	private final Context mContext;
	private final JsMediaServerClient mJsMedia;
	public AutoAlbumCacheImpl(Context context, JsMediaServerClient jsMedia) {
		super();
		mContext = context;
		mJsMedia = jsMedia;
	}
	
	@Override
	public List<CategoryInfo> getCategories(int mediaLimit) {
//StopWatch sw = StopWatch.start();/*$debug$*/
//sw.lap();/*$debug$*/
		{
			Cursor c = mDb.query(CCachedAutoAlbum.$TABLE,
					new String[] {CCachedAutoAlbum.LAST_UPDATED},
					null, null, null, null, null);
			try {
				if (!c.moveToFirst()) {
					return null;
				}
			} finally {
				c.close();
			}
		}
		
		Random random = new Random();
		ArrayList<CategoryInfo> categories = new ArrayList<CategoryInfo>();
		Cursor cCategory = mDb.query(
				CCachedAutoAlbumCategories.$TABLE,
				new String[] {
						CCachedAutoAlbumCategories.CATEGORY_NAME,
						CCachedAutoAlbumCategories.DIVIDER_COUNT,
						CCachedAutoAlbumCategories.MEDIA_COUNT,
				},
				null, null, null, null,
				CCachedAutoAlbumCategories.ORDER_SEQ);
		try {
			while (cCategory.moveToNext()) {
				CategoryInfo category = new CategoryInfo();
				category.categoryName = cCategory.getString(0);
				category.headerCount = cCategory.getInt(1);
				category.mediaCount = cCategory.getInt(2);
				category.mediaInfos = new ArrayList<Media>();
				categories.add(category);
				
				int from = random.nextInt(category.mediaCount) + 1;
				int to = Math.min(from + mediaLimit - 1, category.mediaCount);
				int count = to - from + 1;
				if (count < mediaLimit) {
					from = Math.max(1, from - (mediaLimit - count));
				}
				Cursor cMedia = mDb.query(CCachedAutoAlbumMedias.$TABLE,
						null,
						CCachedAutoAlbumMedias.CATEGORY_NAME + " = ?" +
								" AND " + CCachedAutoAlbumMedias.RANDOM_ORDER + " BETWEEN ? AND ?",
						new String[] {category.categoryName, String.valueOf(from), String.valueOf(to)},
						null, null,
						CCachedAutoAlbumMedias.RANDOM_ORDER);
				try {
					RowHandler<Media> handler = createMediaRowHandler(cMedia);
					while (cMedia.moveToNext()) {
						Media media = new Media();
						handler.populateCurrentRow(cMedia, media);
						category.mediaInfos.add(media);
					}
				} finally {
					cMedia.close();
				}
			}
		} finally {
			cCategory.close();
		}
		
//System.out.println(String.format("--------> getCategories : %dmsec", sw.lap()));/*$debug$*/
		return categories;
	}

	@Override
	public Memory getMemory(String categoryName) {
//StopWatch sw = StopWatch.start();/*$debug$*/
//sw.lap();/*$debug$*/
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT ");
		sql.append("   d." + CCachedAutoAlbumDividers.CATEGORY_NAME + ", ");
		sql.append("   d." + CCachedAutoAlbumDividers._ID + " divider_id, ");
		sql.append("   d." + CCachedAutoAlbumDividers.IS_EVENT + ", ");
		sql.append("   d." + CCachedAutoAlbumDividers.MONTH_DATE + ", ");
		sql.append("   d." + CCachedAutoAlbumDividers.TITLE + ", ");
		sql.append("   m.* ");
		sql.append(" FROM ");
		sql.append("   " + CCachedAutoAlbumDividers.$TABLE + "   d, ");
		sql.append("   " + CCachedAutoAlbumMedias.$TABLE + "     m ");
		sql.append(" WHERE ");
		sql.append("       d." + CCachedAutoAlbumDividers.CATEGORY_NAME + " = ? ");
		sql.append("   AND m." + CCachedAutoAlbumMedias.CATEGORY_NAME + " = ? ");
		sql.append("   AND d." + CCachedAutoAlbumDividers._ID + " = m." + CCachedAutoAlbumMedias.DIVIDER_ID + " ");
		sql.append(" ORDER BY ");
		sql.append("   d." + CCachedAutoAlbumDividers.ORDER_SEQ + ", ");
		sql.append("   m." + CCachedAutoAlbumMedias.ORDER_SEQ + " ");
		
		Memory memory = null;
		
		Cursor c = mDb.rawQuery(sql.toString(), new String[] {categoryName, categoryName});
		try {
			final RowHandler<Media> handler = createMediaRowHandler(c);
			Long prevDividerId = null;
			MemoryDivider currentDivider = null;
			while (c.moveToNext()) {
				Long dividerId = c.getLong(1);
				
				if (memory == null) {
					memory = new Memory();
					memory.keyword = categoryName;
					memory.dividers = new ArrayList<MemoryDivider>();
				}
				
				if (currentDivider == null
						|| !dividerId.equals(prevDividerId)) {
					boolean isEvent = c.getInt(2) == 1;
					String date = c.getString(3);
					String title = c.getString(4);
					
					currentDivider = new MemoryDivider();
					currentDivider.isEvent = isEvent;
					currentDivider.date = date;
					currentDivider.title = title;
					currentDivider.media = new ArrayList<Media>();
					memory.dividers.add(currentDivider);
				}
				
				Media media = new Media();
				handler.populateCurrentRow(c, media);
				currentDivider.media.add(media);
				
				prevDividerId = dividerId;
			}
		} finally {
			c.close();
		}
		
//System.out.println(String.format("--------> getMemory : %dmsec", sw.lap()));/*$debug$*/
		return memory;
	}

	@Override
	public void updateCache() throws IOException, ContentsNotModifiedException {
//StopWatch sw = StopWatch.start();/*$debug$*/
//sw.lap();/*$debug$*/
		String currentEtag;
		Cursor c = mDb.query(
				CCachedAutoAlbum.$TABLE,
				new String[] {CCachedAutoAlbum.CURRENT_ETAG},
				CCachedAutoAlbum.PK + " = ?",
				new String[] {CCachedAutoAlbum.PK_VALUE},
				null, null, null, "1");
		try {
			if (c.moveToNext()) {
				currentEtag = c.getString(0);
			} else {
				currentEtag = null;
			}
		} finally {
			c.close();
		}
		
		RespondedContents<IOIterator<Memory>> contents = mJsMedia.searchMemories(currentEtag);
		IOIterator<Memory> memories = contents.contents;
//System.out.println(String.format("--------> networking : %dmsec", sw.lap()));/*$debug$*/
		try {
			mDb.beginTransaction();
			try {
//sw.lap();/*$debug$*/
				doClearCache(mDb);
//System.out.println(String.format("--------> doClearCache : %dmsec", sw.lap()));/*$debug$*/
				
				ContentValues values = new ContentValues();
				int memorySeq = 0;
				while (memories.hasNext()) {
//sw.lap();/*$debug$*/
					Memory memory = memories.next();
//System.out.println(String.format("--------> memories.next() : %dmsec", sw.lap()));/*$debug$*/
					
//sw.lap();/*$debug$*/
					// カテゴリ保存
					values.clear();
					values.put(CCachedAutoAlbumCategories.CATEGORY_NAME, memory.keyword);
					values.put(CCachedAutoAlbumCategories.DIVIDER_COUNT, memory.dividers.size());
//					values.put(CCachedAutoAlbumCategories.MEDIA_COUNT, mediaSeq);
					values.put(CCachedAutoAlbumCategories.ORDER_SEQ, ++memorySeq);
					long categoryId =
							mDb.insertOrThrow(CCachedAutoAlbumCategories.$TABLE, null, values);
//System.out.println(String.format("--------> カテゴリ保存 : %dmsec", sw.lap()));/*$debug$*/
					
//sw.lap();/*$debug$*/
					// 区切り保存
					int dividerSeq = 0;
					int mediaSeq = 0;
					ArrayList<Media> medias = new ArrayList<Media>();
					for (MemoryDivider divider : memory.dividers) {
						values.clear();
						values.put(CCachedAutoAlbumDividers.CATEGORY_NAME, memory.keyword);
						values.put(CCachedAutoAlbumDividers.IS_EVENT, divider.isEvent ? 1 : 0);
						values.put(CCachedAutoAlbumDividers.MONTH_DATE, divider.date);
						values.put(CCachedAutoAlbumDividers.TITLE, divider.title);
						values.put(CCachedAutoAlbumDividers.ORDER_SEQ, ++dividerSeq);
						Long dividerId =
								mDb.insertOrThrow(CCachedAutoAlbumDividers.$TABLE, null, values);
						
						// メディアについて、サーバからの送信順を記憶し、区切りに紐づける
						for (Media media : divider.media) {
							media.orderSeq = ++mediaSeq;
							media.parentId = dividerId;
						}
						medias.addAll(divider.media);
					}
//System.out.println(String.format("--------> 区切り保存 : %dmsec", sw.lap()));/*$debug$*/
					
//sw.lap();/*$debug$*/
					// カテゴリ配下の全メディアをシャッフル
					Collections.shuffle(medias);
//System.out.println(String.format("--------> シャッフル : %dmsec", sw.lap()));/*$debug$*/
					
//sw.lap();/*$debug$*/
					// メディアを保存
					mediaSeq = 0;
					for (Media media : medias) {
						values.clear();
						populateMedia(media, values);
						values.put(CCachedAutoAlbumMedias.DIVIDER_ID, media.parentId);
						values.put(CCachedAutoAlbumMedias.ORDER_SEQ, media.orderSeq);
						values.put(CCachedAutoAlbumMedias.CATEGORY_NAME, memory.keyword);
						values.put(CCachedAutoAlbumMedias.RANDOM_ORDER, ++mediaSeq);
						mDb.insertOrThrow(CCachedAutoAlbumMedias.$TABLE, null, values);
					}
//System.out.println(String.format("--------> メディア保存 : %dmsec", sw.lap()));/*$debug$*/
					
//sw.lap();/*$debug$*/
					// メディア件数を更新
					values.clear();
					values.put(CCachedAutoAlbumCategories.MEDIA_COUNT, mediaSeq);
					mDb.update(CCachedAutoAlbumCategories.$TABLE, values,
							CCachedAutoAlbumCategories._ID + " = ?",
							new String[] {String.valueOf(categoryId)});
//System.out.println(String.format("--------> メディア件数更新 : %dmsec", sw.lap()));/*$debug$*/
				}
				
//sw.lap();/*$debug$*/
				// キャッシュ済みを記録
				values.clear();
				values.put(CCachedAutoAlbum.PK, CCachedAutoAlbum.PK_VALUE);
				values.put(CCachedAutoAlbum.LAST_UPDATED, System.currentTimeMillis());
				values.put(CCachedAutoAlbum.CURRENT_ETAG, contents.etag);
				mDb.replace(CCachedAutoAlbum.$TABLE, null, values);
//System.out.println(String.format("--------> メディア件数更新 : %dmsec", sw.lap()));/*$debug$*/
				
				mDb.setTransactionSuccessful();
			} finally {
//sw.lap();/*$debug$*/
				mDb.endTransaction();
//System.out.println(String.format("--------> コミット : %dmsec", sw.lap()));/*$debug$*/
			}
		} finally {
//sw.lap();/*$debug$*/
			memories.terminate();
//System.out.println(String.format("--------> terminate : %dmsec", sw.lap()));/*$debug$*/
		}
		
	}

	@Override
	public void clearCache() {
		mDb.beginTransaction();
		try {
			doClearCache(mDb);
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
	}
	
	private void doClearCache(SQLiteDatabase db) {
		db.delete(CCachedAutoAlbumMedias.$TABLE, null, null);
		db.delete(CCachedAutoAlbumDividers.$TABLE, null, null);
		db.delete(CCachedAutoAlbumCategories.$TABLE, null, null);
		db.delete(CCachedAutoAlbum.$TABLE, null, null);
	}
	
	private void populateMedia(Media media, ContentValues values) {
		values.put(CCachedAutoAlbumMedias.SERVICE_TYPE, media.service);
		values.put(CCachedAutoAlbumMedias.SERVICE_ACCOUNT, media.account);
		values.put(CCachedAutoAlbumMedias.MEDIA_ID, media.mediaId);
		values.put(CCachedAutoAlbumMedias.DIR_ID, media.directoryId);
		values.put(CCachedAutoAlbumMedias.MEDIA_URI, media.mediaUri);
		values.put(CCachedAutoAlbumMedias.VERSION, media.version == null ? 0 : media.version);
		values.put(CCachedAutoAlbumMedias.FILE_NAME, media.fileName);
		values.put(CCachedAutoAlbumMedias.THUMBNAIL_DATA, media.thumbnailData);
		values.put(CCachedAutoAlbumMedias.UPDATED, media.updatedTimestamp == null ? 0 : media.updatedTimestamp);
		values.put(CCachedAutoAlbumMedias.PRODUCTION_DATE, media.productionDate);
	}
	
	private RowHandler<Media> createMediaRowHandler(final Cursor c) {
		return new RowHandler<Media>() {
			final int INDEX_SERVICE_TYPE = c.getColumnIndex(CCachedAutoAlbumMedias.SERVICE_TYPE);
			final int INDEX_SERVICE_ACCOUNT = c.getColumnIndex(CCachedAutoAlbumMedias.SERVICE_ACCOUNT);
			final int INDEX_MEDIA_ID = c.getColumnIndex(CCachedAutoAlbumMedias.MEDIA_ID);
			final int INDEX_DIRECTORY_ID = c.getColumnIndex(CCachedAutoAlbumMedias.DIR_ID);
			final int INDEX_MEDIA_URI = c.getColumnIndex(CCachedAutoAlbumMedias.MEDIA_URI);
			final int INDEX_REMOTE_VERSION = c.getColumnIndex(CCachedAutoAlbumMedias.VERSION);
			final int INDEX_TITLE = c.getColumnIndex(CCachedAutoAlbumMedias.FILE_NAME);
			final int INDEX_THUMBNAIL_DATA = c.getColumnIndex(CCachedAutoAlbumMedias.THUMBNAIL_DATA);
			final int INDEX_REMOTE_TIMESTAMP = c.getColumnIndex(CCachedAutoAlbumMedias.UPDATED);
			final int INDEX_PRODUCTION_DATE = c.getColumnIndex(CCachedAutoAlbumMedias.PRODUCTION_DATE);
			
			@Override
			public void populateCurrentRow(Cursor c, Media row) {
				if (0 <= INDEX_SERVICE_TYPE) {
					row.service = c.getString(INDEX_SERVICE_TYPE);
				}
				if (0 <= INDEX_SERVICE_ACCOUNT) {
					row.account = c.getString(INDEX_SERVICE_ACCOUNT);
				}
				if (0 <= INDEX_MEDIA_ID) {
					row.mediaId = c.getString(INDEX_MEDIA_ID);
				}
				if (0 <= INDEX_DIRECTORY_ID) {
					row.directoryId = c.getString(INDEX_DIRECTORY_ID);
				}
				if (0 <= INDEX_MEDIA_URI) {
					row.mediaUri = c.getString(INDEX_MEDIA_URI);
				}
				if (0 <= INDEX_REMOTE_VERSION) {
					row.version = c.getLong(INDEX_REMOTE_VERSION);
				}
				if (0 <= INDEX_TITLE) {
					row.fileName = c.getString(INDEX_TITLE);
				}
				if (0 <= INDEX_THUMBNAIL_DATA) {
					row.thumbnailData = c.getString(INDEX_THUMBNAIL_DATA);
				}
				if (0 <= INDEX_REMOTE_TIMESTAMP) {
					row.updatedTimestamp = c.getLong(INDEX_REMOTE_VERSION);
				}
				if (0 <= INDEX_PRODUCTION_DATE) {
					if (c.isNull(INDEX_PRODUCTION_DATE)) {
						row.productionDate = null;
					} else {
						row.productionDate = c.getLong(INDEX_PRODUCTION_DATE);
					}
				}
			}
		};
	}

}
