package jp.co.johospace.jsphoto.accessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.util.MediaUtil;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * データアクセス　メディアメタデータ
 */
public class MediaMetaDataAccessor {
	
	/**
	 * メディアメタデータのファイル名を更新します
	 * 
	 * @param db	データベース
	 * @param dirPath	ディレクトリのパス
	 * @param fileName 変更ファイル名
	 * @return	true:更新成功	false:更新失敗
	 */
	public static boolean updateMetaDataName(SQLiteDatabase db, String dirPath, String oldName, String newName) {

		boolean result = false;
		
		// フォルダ名をキーに、ファイル名を書き換える
		String sql = "UPDATE " + CMediaMetadata.$TABLE + " SET " + CMediaMetadata.NAME + " = ? " + 
							"WHERE " + CMediaMetadata.DIRPATH + " = ? AND " + CMediaMetadata.NAME + " = ?";
		
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
	 * メタデータのパスを更新します
	 * 
	 * @param db	データベース
	 * @param oldPath	更新前のパス名
	 * @param newPath	更新後のパス名
	 * @return	true:更新成功	false:更新失敗
	 */
	public static boolean updateDirPath(SQLiteDatabase db, String oldPath, String newPath) {
		boolean result = false;
		
		// 変更対象のパスを直接書き換えるSQL文
		String sqlFirst = "UPDATE " + CMediaMetadata.$TABLE + " SET " + CMediaMetadata.DIRPATH + " = ? WHERE " + CMediaMetadata.DIRPATH + " = ?";
		
		// 変更対象の配下のパスを書き換えるSQL文
		String sqlSecond = "UPDATE " + CMediaMetadata.$TABLE + " SET " + 
								CMediaMetadata.DIRPATH + " = ? || SUBSTR (" + CMediaMetadata.DIRPATH + ", ?, LENGTH("+ CMediaMetadata.DIRPATH +") - ? ) " +
								"WHERE " + CMediaMetadata.DIRPATH + " LIKE ?";

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
	 * フォルダ配下のメタデータを全て削除します
	 * 
	 * @param db	データベース
	 * @param folderPath 	対象フォルダ
	 * @return true:削除成功	false:削除失敗
	 */
	public static boolean deleteMetaData(SQLiteDatabase db, String folderPath) {
		
		int resultMetadata = 0;
		
		// ディレクトリパスを元に検索、削除
		String selectionMeta = CMediaMetadata.DIRPATH + " = ? ";
		
		try {
			db.beginTransaction();
			
			// フォルダ名を含むメタデータを削除
			resultMetadata = db.delete(CMediaMetadata.$TABLE, selectionMeta, new String[] {folderPath});
			
			db.setTransactionSuccessful();

		} finally {
			db.endTransaction();
		}
		
		return resultMetadata > 0;
	}
	
	
	/**
	 * metaDataテーブルに登録されている非隠しフォルダ一覧を取得します(親フォルダ含む）
	 * 
	 * @param db			データベース
	 * @param type			メタデータタイプ
	 * @param isDispHidden	true:隠しフォルダ表示	false:隠しフォルダ非表示
	 * @return		非隠しフォルダリスト
	 */
	public static List<String> queryHiddenFolder(SQLiteDatabase db, String type, boolean isDispHidden) {
		
		final int INDEX_DIRPATH = 1;
		
		// フォルダ検索用
		Cursor cursor = null;
		String[] columns = {CMediaMetadata._ID, CMediaMetadata.DIRPATH};
		String selection = CMediaMetadata.METADATA_TYPE + " = ?";
		String[] selectionArgs = {type};
		
		List<String> dirList = new ArrayList<String>();
		
		try {
			// メタデータに登録されているディレクトリ一覧を取得
			cursor = db.query(CMediaMetadata.$TABLE, columns, selection, selectionArgs, CMediaMetadata.DIRPATH, null, null);
			
			while (cursor.moveToNext()) {
				
				String dirPath = cursor.getString(INDEX_DIRPATH);
				
				// 隠しフォルダの表示状態によって、取得フォルダを切り替える
				if (isDispHidden) {
//					dirList.add(dirPath);
				} else {
					// 親の階層に「.nomedia」が含まれたファイルが無いかも確認
					while(!dirPath.equals(File.separator)) {
						// 隠しだった場合、フォルダに追加
						if (MediaUtil.isContainsNomedia(new File(dirPath))) {
							dirList.add(dirPath);
						}
						dirPath = new File(dirPath).getParent();
					}
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return dirList;
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
		String[] columns = {CMediaMetadata.DIRPATH, CMediaMetadata.NAME};
		String groupBy = CMediaMetadata.DIRPATH + "," + CMediaMetadata.NAME;
		
		
		// 削除条件
		StringBuffer selection = new StringBuffer();
		
		// 削除条件値
		List<String> selectionArgs = new ArrayList<String>();
		
		// 一ファイル分の条件式
		String dirAndFile = " ( " + CMediaMetadata.DIRPATH + " = ? AND " + CMediaMetadata.NAME + " = ? ) ";
		
		boolean result = false;
		
		try {
			// 登録されているファイルを取得
			cursor = db.query(CMediaMetadata.$TABLE, columns, null, null, groupBy, null, null);
			
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
			

			// 削除処理
			if (!isFirst) {
				db.delete(CMediaMetadata.$TABLE, selection.toString(), selectionArgs.toArray(new String[1]));
			}
			
			result = true;
			
		} finally {

			if (cursor != null) {
				cursor.close();
			}
		}
		
		return result;
	}
}
