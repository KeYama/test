package jp.co.johospace.jsphoto.accessor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.CMediaTagMaster;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * タグエディタ
 */
public class TagEditor {
	private TagEditor() {
	}

	/**
	 * タグのエントリを問い合わせます。
	 * 
	 * @param db
	 *            データベース
	 * @param targetMedia
	 *            タグ編集対象のメディアファイル
	 * @return タグのエントリ。全メディアで現存するすべてのタグのリスト。編集対象に共通するタグはチェック済み。
	 */
	public static List<TagEntry> queryTagEntries(SQLiteDatabase db,
			Collection<File> targetMedia) {
		HashMap<String, Integer> tagCounts = new HashMap<String, Integer>();
		for (File media : targetMedia) {
			Cursor c = db.query(CMediaMetadata.$TABLE,
					new String[] { CMediaMetadata.METADATA },
					CMediaMetadata.DIRPATH + " = ?" + " AND "
							+ CMediaMetadata.NAME + " = ?" + " AND "
							+ CMediaMetadata.METADATA_TYPE + " = ?",
					new String[] { media.getParent(), media.getName(),
							CMediaMetadata.TYPE_TAG }, null, null, null);
			try {
				while (c.moveToNext()) {
					String tag = c.getString(0);
					Integer count = tagCounts.get(tag);
					if (count == null) {
						count = 0;
					}
					tagCounts.put(tag, ++count);
				}
			} finally {
				c.close();
			}
		}

		ArrayList<TagEntry> entries = new ArrayList<TagEntry>();
		Cursor c = db.query(CMediaTagMaster.$TABLE,
				new String[] { CMediaTagMaster.NAME }, null, null, null, null,
				CMediaTagMaster.NAME + " Desc");
		try {
			while (c.moveToNext()) {
				String tag = c.getString(0);
				Integer count = tagCounts.get(tag);
				entries.add(new TagEntry(tag, count != null
						&& targetMedia.size() == count));
			}
		} finally {
			c.close();
		}

		return entries;
	}

	/**
	 * 編集対象メディアファイルのタグを更新します。
	 * 
	 * @param db
	 *            データベース
	 * @param targetMedia
	 *            編集対象のメディアファイル
	 * @param newTags
	 *            新しいタグの状態。
	 */
	public static void updateTags(SQLiteDatabase db,
			Collection<File> targetMedia, Collection<TagEntry> newTags) {
		ArrayList<TagEntry> checked = new ArrayList<TagEntry>();
		for (TagEntry entry : newTags) {
			if (entry.checked) {
				checked.add(entry);
			}
		}

		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			for (File media : targetMedia) {
				db.delete(CMediaMetadata.$TABLE, CMediaMetadata.DIRPATH
						+ " = ?" + " AND " + CMediaMetadata.NAME + " = ?"
						+ " AND " + CMediaMetadata.METADATA_TYPE + " = ?",
						new String[] { media.getParent(), media.getName(),
								CMediaMetadata.TYPE_TAG });
				for (TagEntry entry : checked) {
					values.clear();
					values.put(CMediaMetadata.DIRPATH, media.getParent());
					values.put(CMediaMetadata.NAME, media.getName());
					values.put(CMediaMetadata.METADATA_TYPE,
							CMediaMetadata.TYPE_TAG);
					values.put(CMediaMetadata.METADATA, entry.tag);
					values.put(CMediaMetadata.UPDATE_TIMESTAMP,
							System.currentTimeMillis());
					db.insertOrThrow(CMediaMetadata.$TABLE, null, values);
				}
			}

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * 渡されたタグを全メディアから除去します。
	 * 
	 * @param db
	 *            データベース
	 * @param tag
	 *            除去するタグ
	 */
	public static void removeAll(SQLiteDatabase db, String tag) {
		db.beginTransaction();
		try {
			db.delete(CMediaMetadata.$TABLE, CMediaMetadata.METADATA_TYPE
					+ " = ?" + " AND " + CMediaMetadata.METADATA + " = ?",
					new String[] { CMediaMetadata.TYPE_TAG, tag });

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * 渡されたタグで全メディアのタグを置き換えます。
	 * 
	 * @param db
	 *            データベース
	 * @param oldTag
	 *            古いタグ
	 * @param newTag
	 *            新しいタグ
	 */
	public static void replaceAll(SQLiteDatabase db, String oldTag,
			String newTag) {
		db.beginTransaction();
		try {
			Cursor c = db.query(CMediaMetadata.$TABLE, new String[] {
					CMediaMetadata.DIRPATH, CMediaMetadata.NAME },
					CMediaMetadata.METADATA + " = ?" + " AND "
							+ CMediaMetadata.METADATA_TYPE + " = ?",
					new String[] { oldTag, CMediaMetadata.TYPE_TAG }, null,
					null, null);
			try {
				ContentValues values = new ContentValues();
				while (c.moveToNext()) {
					String dirpath = c.getString(0);
					String name = c.getString(1);
					db.delete(CMediaMetadata.$TABLE, CMediaMetadata.DIRPATH
							+ " = ?" + " AND " + CMediaMetadata.NAME + " = ?"
							+ " AND " + CMediaMetadata.METADATA_TYPE + " = ?"
							+ " AND " + CMediaMetadata.METADATA + " = ?",
							new String[] { dirpath, name,
									CMediaMetadata.TYPE_TAG, oldTag });

					values.clear();
					values.put(CMediaMetadata.DIRPATH, dirpath);
					values.put(CMediaMetadata.NAME, name);
					values.put(CMediaMetadata.METADATA_TYPE,
							CMediaMetadata.TYPE_TAG);
					values.put(CMediaMetadata.METADATA, newTag);
					values.put(CMediaMetadata.UPDATE_TIMESTAMP,
							System.currentTimeMillis());
					db.insertOrThrow(CMediaMetadata.$TABLE, null, values);
				}
			} finally {
				c.close();
			}

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/** タグエントリ */
	public static class TagEntry {
		public TagEntry() {
			super();
		}

		public TagEntry(String tag, boolean checked) {
			super();
			this.tag = tag;
			this.checked = checked;
		}

		/** タグ */
		public String tag;
		/** チェック */
		public boolean checked;
	}

	/**
	 * 渡されたメディアファイルが、タグをすべて持っているかどうかを返します。
	 * 
	 * @param db
	 *            データベース
	 * @param file
	 *            対象のメディアファイル
	 * @param tags
	 *            タグ
	 * @return すべて持っている場合true
	 */
	public static boolean hasAllTags(SQLiteDatabase db, File file,
			String... tags) {
		String dirpath = file.getParent();
		String name = file.getName();

		ArrayList<String> current = new ArrayList<String>();
		Cursor c = db.query(CMediaMetadata.$TABLE,
				new String[] { CMediaMetadata.METADATA },
				CMediaMetadata.DIRPATH + " = ?" + " AND " + CMediaMetadata.NAME
						+ " = ?" + " AND " + CMediaMetadata.METADATA_TYPE
						+ " = ?", new String[] { dirpath, name,
						CMediaMetadata.TYPE_TAG }, null, null, null);
		try {
			while (c.moveToNext()) {
				current.add(c.getString(0));
			}
		} finally {
			c.close();
		}

		return current.containsAll(Arrays.asList(tags));
	}

	/**
	 * まだ存在しないタグを登録します。
	 * 
	 * @param db
	 *            データベース
	 * @param file
	 *            対象のメディアファイル
	 * @param tags
	 *            タグ
	 * @return 登録件数
	 */
	public static int insertIfNotExists(SQLiteDatabase db, File file,
			String... tags) {
		String dirpath = file.getParent();
		String name = file.getName();
		ContentValues values = new ContentValues();
		int inserted = 0;

		db.beginTransaction();
		try {
			for (String tag : tags) {
				Cursor c = db.query(CMediaMetadata.$TABLE,
						new String[] { CMediaMetadata.METADATA },
						CMediaMetadata.DIRPATH + " = ?" + " AND "
								+ CMediaMetadata.NAME + " = ?" + " AND "
								+ CMediaMetadata.METADATA_TYPE + " = ?"
								+ " AND " + CMediaMetadata.METADATA + " = ?",
						new String[] { dirpath, name, CMediaMetadata.TYPE_TAG,
								tag }, null, null, null);
				try {
					if (!c.moveToNext()) {
						values.put(CMediaMetadata.DIRPATH, dirpath);
						values.put(CMediaMetadata.NAME, name);
						values.put(CMediaMetadata.METADATA_TYPE,
								CMediaMetadata.TYPE_TAG);
						values.put(CMediaMetadata.METADATA, tag);
						values.put(CMediaMetadata.UPDATE_TIMESTAMP,
								System.currentTimeMillis());
						db.insertOrThrow(CMediaMetadata.$TABLE, null, values);
						inserted++;
					}
				} finally {
					c.close();
				}
			}

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

		return inserted;
	}

	/**
	 * タグの一覧を取得します。
	 * 
	 * @param db
	 *            データベース
	 * @return タグ一覧
	 */
	public static ArrayList<String> selectAllTags(SQLiteDatabase db) {

		ArrayList<String> tagList = new ArrayList<String>();

		Cursor c = db.query(CMediaMetadata.$TABLE,
				new String[] { CMediaMetadata.METADATA },
				CMediaMetadata.METADATA_TYPE + " = ? ",
				new String[] { CMediaMetadata.TYPE_TAG }, null, null, null);

		while (c.moveToNext()) {
			tagList.add(c.getString(0));
		}

		return tagList;

	}

}
