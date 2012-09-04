package jp.co.johospace.jsphoto.accessor;

import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.CMediaTagMaster;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * データアクセス　タグマスタ
 */
public class TagMasterAccessor {
	
	/**
	 * タグの表示状態を取得します
	 * 
	 * @param tagName	対象タグ名
	 * @return	true:表示	false:非表示
	 */
	public static boolean getTagHide(SQLiteDatabase db, String tagName) {
		Cursor cursor = null;
		boolean result = true;
		
		String[] columns = {CMediaTagMaster._ID, CMediaTagMaster.HIDE};
		String selection = CMediaTagMaster.NAME + " = ? ";
		String[] selectionArgs = {tagName};
		
		try {
			cursor = db.query(CMediaTagMaster.$TABLE, columns, selection, selectionArgs, null, null, null);
			
			if (cursor.moveToFirst()) {
				
				result = cursor.getInt(1) == 0;
			}
			
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		return result;
	}
	
	/**
	 * タグマスタの表示フラグを更新します
	 * 
	 * @param db	データベース
	 * @param name	タグ名
	 * @param hide	表示状態
	 */
	public static void updateTagHide(SQLiteDatabase db, String name, boolean hide) {
		
		int hideFlag = hide ? 0 : 1;
		
		// 更新値セット
		ContentValues cv = new ContentValues();
		cv.put(CMediaTagMaster.HIDE, hideFlag);
		
		String selection = CMediaTagMaster.NAME + " = ? ";
		String[] selectionArgs = {name};
		
		try {
			db.beginTransaction();
			
			db.update(CMediaTagMaster.$TABLE, cv, selection, selectionArgs);
			
			db.setTransactionSuccessful();
			
		} finally {
			db.endTransaction();
		}
	}
	
	/**
	 * タグマスタのフィルタリングフラグを更新します
	 * 
	 * @param db	データベース
	 * @param name	タグ名
	 * @param filtering	表示状態
	 */
	public static void updateTagFilter(SQLiteDatabase db, String name, boolean filtering) {
		
		int hideFlag = filtering ? 1 : 0;

		// 更新値セット
		ContentValues cv = new ContentValues();
		cv.put(CMediaTagMaster.FILTER, hideFlag);
		
		String selection = CMediaTagMaster.NAME + " = ? ";
		String[] selectionArgs = {name};
		
		try {
			db.beginTransaction();
			
			int i = db.update(CMediaTagMaster.$TABLE, cv, selection, selectionArgs);

			db.setTransactionSuccessful();
			
		} finally {
			db.endTransaction();
		}
	}
	
	/**
	 * タグマスタのフィルタリングフラグを初期化します
	 * 
	 * @param db	データベース
	 */
	public static void initFilter(SQLiteDatabase db) {
		ContentValues cv = new ContentValues();
		cv.put(CMediaTagMaster.FILTER, 0);
		
		try {
			db.beginTransaction();
			
			db.update(CMediaTagMaster.$TABLE, cv, null, null);
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * タグの一覧を取得します
	 * 
	 * @param db		データベース
	 * @param dispHidden	隠しフォルダ表示フラグ
	 * @param dispSecret	シークレット表示フラグ
	 * @return 検索結果
	 */
	public static Cursor queryTag(SQLiteDatabase db, boolean dispHidden, boolean dispSecret) {
		
		// metadataテーブルの取得条件
		String selectionText = "SELECT " + CMediaMetadata.METADATA + " FROM " + CMediaMetadata.$TABLE + 
								" WHERE " + CMediaMetadata.METADATA_TYPE + " = ? ";
		
		// masterの条件値
		List<String> selectionArgs = new ArrayList<String>();
		selectionArgs.add(ApplicationDefine.MIME_TAG);
		
		// フォルダ一覧
		List<String> dirList = new ArrayList<String>();
		
		// 隠しフォルダを表示しない場合
		if (!dispHidden) {
			
			// metadataに登録されている、隠しフォルダを取得
			dirList = MediaMetaDataAccessor.queryHiddenFolder(db, ApplicationDefine.MIME_TAG, false);
			
			boolean isFirst = true;
			
			int size = dirList.size();
			
			// フォルダの取得条件を設定
			if (size > 0) {
				
				selectionText += " AND " + CMediaMetadata.DIRPATH + " NOT LIKE ";
				
				for (int i = 0; i < size; i++) {
					// 合わせてwhere条件を作成
					if (isFirst) {
						selectionText += "?";
						isFirst = false;
					} else {
						selectionText += " AND " + CMediaMetadata.DIRPATH + " NOT LIKE ?";
					}
				}

				for (String dirpath : dirList) {
					selectionArgs.add(dirpath + "%");
				}
			}
		}

		String selectionTagName;
		
		// シークレットを表示
		if (dispSecret) {
			selectionTagName = selectionText;
			
		// シークレットを非表示
		} else {
			selectionTagName = selectionText + " AND " + CMediaMetadata.NAME + " NOT LIKE ? " ;
			selectionArgs.add("%" + ApplicationDefine.SECRET);
		}
		
		// metadataをタグ名でグループ化
		selectionText += " GROUP BY " + CMediaMetadata.METADATA;
		
		// metadataの検索条件を元に、masterテーブルの値を取得
		String selection = CMediaTagMaster.NAME + " IN ( " + selectionTagName + " ) ";
		Cursor cursor = db.query(CMediaTagMaster.$TABLE, 
										new String[] {CMediaTagMaster._ID, CMediaTagMaster.NAME, CMediaTagMaster.FILTER}, 
										selection, selectionArgs.toArray(new String[1]), null, null, null);
		return cursor;
	}
}
