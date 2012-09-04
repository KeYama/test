package jp.co.johospace.jsphoto.cache;

import java.io.File;

import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXLocalThumbnailLoader;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class LocalCachedThumbnailLoader extends UXLocalThumbnailLoader {
	private static SQLiteDatabase mDatabase = OpenHelper.cache.getDatabase();
	
	
	@Override
	public synchronized boolean loadCachedThumbnail(Object info, int sizeHint,
			UXImageInfo out) {
		File path = info instanceof File ? (File) info : new File((String)info);
		String folder = path.getParent();
		String name = path.getName();
		
		Cursor cache = mDatabase.query(
				CMediaIndex.$TABLE, 
				new String[]{CMediaIndex.THUMBNAIL_TIMESTAMP, CMediaIndex.THUMBNAIL,
						CMediaIndex.ORIENTATION}, 
				CMediaIndex.NAME + " = ? AND " + CMediaIndex.DIRPATH + " = ? AND " + CMediaIndex.SIZE + " = ?",
				new String[]{ name, folder, String.valueOf(sizeHint) }, null, null, null);

		try{
			if(cache.moveToFirst()){
				long lastModified = cache.getLong(0);
				if(path.lastModified() == lastModified){
					out.compressedImage = cache.getBlob(1);
					
					// Exif情報のチェック
					if (mIsReflectionExif) {
						out.orientation = cache.getInt(2);
					}
					
					return true;
					//return false;
				}else{
					mDatabase.delete(CMediaIndex.$TABLE,
							CMediaIndex.NAME + " = ? AND " + CMediaIndex.DIRPATH + " = ? AND " + CMediaIndex.SIZE + " = ?",
							new String[]{ name, folder, String.valueOf(sizeHint) });
					return false;
				}
			}else{
				return false;
			}
		}finally{
			cache.close();
		}
	}

	@Override
	public synchronized void updateCachedThumbnail(Object info, int sizeHint,
			UXImageInfo in) {
		File media = info instanceof File ? (File) info : new File((String)info);
		ContentValues values = new ContentValues();

		values.put(CMediaIndex.THUMBNAIL, in.compressedImage);
		values.put(CMediaIndex.NAME, media.getName());
		values.put(CMediaIndex.DIRPATH, media.getParent());
		values.put(CMediaIndex.THUMBNAIL_TIMESTAMP, media.lastModified());
		values.put(CMediaIndex.ORIENTATION, in.orientation);
		values.put(CMediaIndex.SIZE, sizeHint);
		try{
			mDatabase.insertOrThrow(CMediaIndex.$TABLE, null, values);
//			mDatabase.insertWithOnConflict(CMediaIndex.$TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		}catch(Exception e){
//			android.util.Log.e("dbg","insertion error: " + e.getMessage() + values.toString());		/*$debug$*/
		}
	}

}
