package jp.co.johospace.jsphoto.grid;

import java.io.File;
import java.util.Date;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * 
 * お気に入り操作ユーティリティ
 * 
 */
public class FavoriteUtil {
	/**
	 * 対象ファイルをfavoriteに設定
	 * 
	 * @param file
	 */
	public static void addFavorite(File file){
		SQLiteDatabase db = OpenHelper.external.getDatabase();
		
		ContentValues values = new ContentValues();
		values.put(CMediaMetadata.DIRPATH, file.getParent());
		values.put(CMediaMetadata.NAME, file.getName());
		values.put(CMediaMetadata.METADATA_TYPE, ApplicationDefine.MIME_FAVORITE);
		values.put(CMediaMetadata.METADATA, 1);
		values.put(CMediaMetadata.UPDATE_TIMESTAMP, new Date().getTime());
		
		if(-1 == db.insert(CMediaMetadata.$TABLE, null, values))
			throw new RuntimeException("failed insertion");
	}
	
	/**
	 * 対象ファイルをお気に入りから除外
	 * 
	 * @param file
	 */
	public static void removeFavorite(File file){
		SQLiteDatabase db = OpenHelper.external.getDatabase();
		
		db.delete(
				CMediaMetadata.$TABLE,
				CMediaMetadata.DIRPATH + " = ? and " + 
				CMediaMetadata.NAME  + " = ? and " + 
				CMediaMetadata.METADATA_TYPE + " = ?",
				new String[]{
						file.getParent(), 
						file.getName(), 
						ApplicationDefine.MIME_FAVORITE
						}
				);
	}
	
	/**
	 * 対象ファイルがお気に入りかどうか
	 * 
	 * @param file
	 * @return
	 */
	public static boolean isFavorite(File file){
		SQLiteDatabase db = OpenHelper.external.getDatabase();
		
		Cursor c = db.query(
				CMediaMetadata.$TABLE,
				new String[]{CMediaMetadata.METADATA}, 
				CMediaMetadata.DIRPATH + "= ? and " +
				CMediaMetadata.NAME + "= ? and " + 
				CMediaMetadata.METADATA_TYPE + " = ?", 
				new String[]{file.getParent(), file.getName(), ApplicationDefine.MIME_FAVORITE},
				null, null, null);
		try{
			if(c.moveToFirst()){
				do{
					String value = c.getString(c.getColumnIndex(CMediaMetadata.METADATA));
					if(value.equals("1")){
						return true;
					}
				}while(c.moveToNext());	
			}
			return false;
		}finally{
			c.close();
		}
	}
}
