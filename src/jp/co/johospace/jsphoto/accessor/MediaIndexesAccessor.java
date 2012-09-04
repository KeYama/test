package jp.co.johospace.jsphoto.accessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * データアクセス　インデックス
 */
public class MediaIndexesAccessor {

	/**
	 * メディアインデックスのファイル名を更新します
	 * 
	 * @param db	データベース
	 * @param dirPath	ディレクトリのパス
	 * @param fileName 変更ファイル名
	 * @return	true:更新成功	false:更新失敗
	 */
	public static boolean updateIndexesName(SQLiteDatabase db, String dirPath, String oldName, String newName) {

		boolean result = false;
		
		// フォルダ名をキーに、ファイル名を書き換える
		String sql = "UPDATE " + CMediaIndex.$TABLE + " SET " + CMediaIndex.NAME + " = ? " + 
							"WHERE " + CMediaIndex.DIRPATH + " = ? AND " + CMediaIndex.NAME + " = ?";

		Object[] firstArgs = {newName, dirPath, oldName};

		try {
			db.beginTransaction();
			
			// 更新処理
			db.execSQL(sql, firstArgs);

			db.setTransactionSuccessful();
			
			result = true;
			
		} catch (Exception e) {
//			e.printStackTrace();		/*$debug$*/
		} finally {
			db.endTransaction();
		}
		
		return result;
	}
	
	
	/**
	 * メディアインデックスのパスを更新します
	 * 
	 * @param db	データベース
	 * @param oldPath	更新前のパス名
	 * @param newPath	更新後のパス名
	 * @return	true:更新成功	false:更新失敗
	 */
	public static boolean updateIndexesPath(SQLiteDatabase db, String oldPath, String newPath) {

		boolean result = false;
		
		// 変更対象のパスを直接書き換えるSQL文
		String sqlFirst = "UPDATE " + CMediaIndex.$TABLE + " SET " + CMediaIndex.DIRPATH + " = ? WHERE " + CMediaIndex.DIRPATH + " = ?";
		
		// 変更対象の配下のパスを書き換えるSQL文
		String sqlSecond = "UPDATE " + CMediaIndex.$TABLE + " SET " + 
								CMediaIndex.DIRPATH + " = ? || SUBSTR (" + CMediaIndex.DIRPATH + ", ?, LENGTH("+ CMediaIndex.DIRPATH +") - ? ) " +
								"WHERE " + CMediaIndex.DIRPATH + " LIKE ?";

		Object[] firstArgs = {newPath, oldPath};
		Object[] secondArgs = {newPath, oldPath.length() + 1, oldPath.length(), oldPath + "/%"};
		
		try {
			db.beginTransaction();
			
			// 更新処理
			db.execSQL(sqlFirst, firstArgs);
			db.execSQL(sqlSecond, secondArgs);

			db.setTransactionSuccessful();
			
			result = true;
			
		} catch (Exception e) {
//			e.printStackTrace();		/*$debug$*/
		} finally {
			db.endTransaction();
		}
		
		return result;
	}
	
	/**
	 * フォルダ配下のメディアインデックスのデータを全て削除します
	 * 
	 * @param db	データベース
	 * @param folderPath 	対象フォルダ
	 * @return true:削除成功	false:削除失敗
	 */
	public static boolean deleteIndexData(SQLiteDatabase db, String folderPath) {
		
		int resultIndexes = 0;
		
		// ディレクトリパスを元に削除
		String selectionIndex = CMediaIndex.DIRPATH + " = ? ";
		
		try {
			db.beginTransaction();
			
			// フォルダ名を含むデータを削除
			resultIndexes = db.delete(CMediaIndex.$TABLE, selectionIndex, new String[] {folderPath});
			
			db.setTransactionSuccessful();

		} finally {
			db.endTransaction();
		}
		
		return resultIndexes >=0;
	}
	
	/**
	 * ファイルの存在をチェックし、正しい状態に更新します
	 * 
	 * @param db	データベース
	 */
	public static boolean checkDBFolder(SQLiteDatabase db) {
		
		final int INDEX_DIRPATH = 0;
		final int INDEX_NAME = 1;
		
		// 取得条件
		Cursor cursor = null;
		String[] columns = {CMediaIndex.DIRPATH, CMediaIndex.NAME};
		String groupBy = CMediaIndex.DIRPATH + "," + CMediaIndex.NAME;
		
		
		// 削除条件
		StringBuffer selection = new StringBuffer();
		
		// 削除条件値
		List<String> selectionArgs = new ArrayList<String>();
		
		// 一ファイル分の条件式
		String dirAndFile = " ( " + CMediaIndex.DIRPATH + " = ? AND " + CMediaIndex.NAME + " = ? ) ";
		
		boolean result = false;
		
		try {
			// 登録されているファイルを取得
			cursor = db.query(CMediaIndex.$TABLE, columns, null, null, groupBy, null, null);
			
			boolean isFirst = true;
			
			// ファイルを一件ずつチェック
			while (cursor.moveToNext()) {
				
				String dirPath = cursor.getString(INDEX_DIRPATH);
				String fileName = cursor.getString(INDEX_NAME);
				
				File file = new File(dirPath, fileName);
				
				// ファイルが存在しない（アプリ外で削除されている）ならば、テーブルから削除
				if (!file.exists()) {

					if (isFirst) {
						isFirst = false;
					} else {
						selection.append(" OR ");
					}
					
					selection.append(dirAndFile);
					selectionArgs.add(dirPath);
					selectionArgs.add(fileName);
				}
			}

			if (!isFirst) {
				db.delete(CMediaIndex.$TABLE, selection.toString(), selectionArgs.toArray(new String[1]));
			}
			
			result = true;
			
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		return result;
	}
	
	/**
	 * インデックスとメタデータのファイル名を、まとめて更新する
	 * 
	 * @param dbExternal	データベース
	 * @param dirPath	ディレクトリのパス
	 * @param fileName 変更ファイル名
	 * @return	true:更新成功	false:更新失敗
	 * @throws Exception 
	 */
	public static boolean updateMediaData(SQLiteDatabase dbExternal, SQLiteDatabase dbCache, String dirPath, String oldName, String newName) throws Exception {

		// SQL インデックス
		String sqlIndex = "UPDATE " + CMediaIndex.$TABLE + " SET " + CMediaIndex.NAME + " = ? " + 
							"WHERE " + CMediaIndex.DIRPATH + " = ? AND " + CMediaIndex.NAME + " = ?";

		// SQL メタデータ
		String sqlMeta = "UPDATE " + CMediaMetadata.$TABLE + " SET " + CMediaMetadata.NAME + " = ? " + 
							"WHERE " + CMediaMetadata.DIRPATH + " = ? AND " + CMediaMetadata.NAME + " = ?";
		
		// 更新項目
		Object[] firstArgs = {newName, dirPath, oldName};
		

		dbExternal.beginTransaction();
		try {
			
			// 更新処理
			dbExternal.execSQL(sqlMeta, firstArgs);
			
			dbExternal.setTransactionSuccessful();
			
		} finally {
			dbExternal.endTransaction();
		}

		dbCache.beginTransaction();
		try {
			
			// 更新処理
			dbCache.execSQL(sqlIndex, firstArgs);
			
			dbCache.setTransactionSuccessful();
			
		} finally {
			dbCache.endTransaction();
		}
		
		return true;
	}
	
	/**
	 * インデックスとメタデータの該当するデータを、すべて削除します
	 * 
	 * @param dbCache			データベース
	 * @param folderPath	フォルダパス
	 * @return				true:削除成功	false:削除失敗
	 */
	public static boolean deleteMediaData(SQLiteDatabase dbExternal, SQLiteDatabase dbCache, String folderPath) {
		
		int resultIndexes = 0;	
		int resultMetadata = 0;
	
		// 条件式　インデックス
		String selectionIndex = CMediaIndex.DIRPATH + " = ? ";
		
		// 条件式　メタデータ
		String selectionMeta = CMediaMetadata.DIRPATH + " = ? ";
		
		dbExternal.beginTransaction();
		try {
			
			// フォルダ名を含むデータを削除
			resultMetadata = dbExternal.delete(CMediaMetadata.$TABLE, selectionMeta, new String[] {folderPath});
			
			dbExternal.setTransactionSuccessful();

		} finally {
			dbExternal.endTransaction();
		}
		
		dbCache.beginTransaction();
		try {
			
			// フォルダ名を含むデータを削除
			resultIndexes = dbCache.delete(CMediaIndex.$TABLE, selectionIndex, new String[] {folderPath});
			
			dbCache.setTransactionSuccessful();

		} finally {
			dbCache.endTransaction();
		}
		
		return resultIndexes >=0 && resultMetadata >= 0;
	}
}
