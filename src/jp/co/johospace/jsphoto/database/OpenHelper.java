package jp.co.johospace.jsphoto.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import jp.co.johospace.jsphoto.JorlleApplication;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQuery;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * データベースのオープンヘルパー
 */
public abstract class OpenHelper {

	public static final OpenHelper external = new OpenHelper() {
		
		@Override
		protected String getName() {
			return "external";
		}

		@Override
		protected int getVersion() {
			return 1;
		}
		
	};

	public static final OpenHelper sync = new OpenHelper() {

		@Override
		protected String getName() {
			return "external-sync";
		}

		@Override
		protected int getVersion() {
			return 1;
		}
		
	};

	public static final OpenHelper cache = new OpenHelper() {

		@Override
		protected File getDir() {
			File externalDir = JorlleApplication.instance().getExternalCacheDir();
			File dir = new File(externalDir, "database");
			return dir;
		}
		
		@Override
		protected String getName() {
			return "cache";
		}

		@Override
		protected int getVersion() {
			return 2;
		}
		
	};
	
	private static final Object LOCK = new Object();
	
	/** カーソルファクトリ */
	public static final CursorFactory DEFAULT_FACTORY = new CursorFactory() {
		@Override
		public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery,
				String editTable, SQLiteQuery query) {
			return new SQLiteCursor(db, masterQuery, editTable, query);
		}
	};
	
	/** データベースの格納ディレクトリを返します。 */
	protected File getDir() {
		File dir = new File(Environment.getExternalStorageDirectory(), ".jsphoto");
		dir = new File(dir, "database");
		return dir;
	}
	
	/** データベース名を返します。 */
	protected abstract String getName();
	
	/** データベースバージョンを返します。 */
	protected abstract int getVersion();
	
	/** データベースのファイルを返します。 */
	public File getDatabaseFile() {
		File dir = getDir();
		dir.mkdirs();		
		return new File(dir, String.format("%s.db", getName()));
	}
	
	/** データベース */
	private SQLiteDatabase sDb;
	private boolean initialized = false;
	
	/**
	 * データベースを初期化します。
	 */
	public void initialize() {
		synchronized(LOCK) {
			if (initialized) return;
			
			SQLiteDatabase db = openOrCreateDatabase();
			
			if (db == null) return; 
			
			int currentVersion = db.getVersion();
			
			if (currentVersion != getVersion()) {
				db.beginTransaction();
				try {
					if (currentVersion == 0) {
						onCreate(db);
					} else {
						onUpgrade(db, currentVersion, getVersion());
					}
					
					db.setVersion(getVersion());
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			}
			
			sDb = db;
			initialized = true;
		}
	}
	
	/** 外部データベースを返します。 */
	public SQLiteDatabase getDatabase() {
		synchronized(LOCK) {
			// SDチェック
			if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
				return sDb;
			}

			if (!initialized) {
//				throw new Error("database is not initialized.");
				initialize();
			}
			
			if (!sDb.isOpen()) {
				sDb = openOrCreateDatabase();
			}
			return sDb;
		}
	}
	
	private SQLiteDatabase openOrCreateDatabase() {
		JorlleApplication application = JorlleApplication.instance();
		if (application == null || application.getExternalCacheDir() == null) {
			return null;
		}
		
		application.getExternalCacheDir().mkdirs();
		File installed = new File(application.getExternalCacheDir(),
				String.format(".installed.%s.%s", OpenHelper.class.getSimpleName(), getName()));
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(JorlleApplication.instance());
		String key = String.format("%s|%s", OpenHelper.class.getSimpleName(), getName());
		
		File file = getDatabaseFile();
		
		if (installed.exists()
				&& !pref.getBoolean(key, false)) {
			/*
			 * キャッシュディレクトリ内のマーカーが存在するのに、内部データがない
			 *   → データの消去が行われた
			 */
			if (file.delete()) {
				Log.d(OpenHelper.class.getSimpleName(),
						String.format("detabase deleted - %s", getName()));
			} else {
				Log.e(OpenHelper.class.getSimpleName(),
						String.format("failed to delete detabase - %s", getName()));
			}
		}
		
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, DEFAULT_FACTORY);
		
		pref.edit().putBoolean(key, true).commit();
		if (!installed.exists()) {
			try {
				installed.createNewFile();
			} catch (IOException e) {
				;
			}
		}
		
		return db;
	}
	
	/** データベースが新規作成されました。 */
	private void onCreate(SQLiteDatabase db) {
		DDL ddl = getCreateDDL();
		try {
			String sql;
			while ((sql = ddl.nextSql()) != null) {
				db.execSQL(sql);
			}
		} finally {
			ddl.terminate();
		}
	}
	
	/** データベースバージョンが異なります。 */
	private void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		DDL ddl = getUpgradeDDL(oldVersion, newVersion);
		try {
			String sql;
			while ((sql = ddl.nextSql()) != null) {
				db.execSQL(sql);
			}
		} finally {
			ddl.terminate();
		}
	}
	
	/**
	 * 外部リソースのストリームから読み込むDDLをあらわします
	 */
	static class DDL {
		final Reader in;
		final String delimiter;
		DDL(Reader in, String delimiter) {
			super();
			this.in = in;
			this.delimiter = delimiter;
		}
		
		public String nextSql() {
			try {
				StringBuffer sql = new StringBuffer();
				int c;
				char[] buf = new char[delimiter.length()];
				while ((c = in.read()) != -1) {
					if (delimiter.charAt(0) == c) {
						buf[0] = (char) c;
						in.mark(buf.length - 1);
						in.read(buf, 1, buf.length - 1);
						in.reset();
						if (delimiter.equals(new String(buf))) {
							in.skip(buf.length - 1);
							break;
						} else {
							sql.append((char) c);
						}
					} else {
						sql.append((char) c);
					}
				}
				if (c == -1) {
					in.close();
					return null;
				} else {
					return sql.toString();
				}
			} catch (IOException e) {
				throw new Error(e);
			}
		}
		
		public void terminate() {
			try {
				in.close();
			} catch (IOException e) {
				;
			}
		}
	}
	
	/** DDLファイルの文字エンコーディング */
	private static final Charset ENCODING = Charset.forName("UTF-8");
	
	/**
	 * データベース作成時のDDLを返します
	 * @return データベース作成時のDDL
	 */
	private DDL getCreateDDL() {
		final String resource = String.format("ddl/onCreate.%s.ddl", getName());
		InputStream in;
		try {
			in = JorlleApplication.instance().getResources().getAssets().open(resource, AssetManager.ACCESS_STREAMING);
		} catch (IOException e) {
			throw new Error(
					String.format("Cannot open DDL file on create database. - assets/%s", resource), e);
		}
		BufferedReader reader =
			new BufferedReader(new InputStreamReader(in, ENCODING), 1024);
		return new DDL(reader, ";--");
	}
	
	/**
	 * データベースアップグレード時のDDLを返します
	 * @param oldVersion 古いバージョン
	 * @param newVersion 新しいバージョン
	 * @return データベースアップグレード時のDDL
	 */
	private DDL getUpgradeDDL(int oldVersion, int newVersion) {
		final String resource =
			String.format("ddl/onUpgrade.%s.%d.%d.ddl", getName(), oldVersion, newVersion);
		InputStream in;
		try {
			in = JorlleApplication.instance().getResources().getAssets().open(resource, AssetManager.ACCESS_STREAMING);
		} catch (IOException e) {
			throw new Error(
					String.format("Cannot open DDL file on upgrade database(ver.%d -> ver.%d). - assets/%s",
							oldVersion, newVersion, resource), e);
		}
		BufferedReader reader =
			new BufferedReader(new InputStreamReader(in, ENCODING), 1024);
		return new DDL(reader, ";--");
	}
	
	public static SQLiteDatabase createDatabase(Context context, File path, String ddlResource, int version) throws IOException {
		if (path.exists()) {
			path.delete();
		}
		
		SQLiteDatabase db =
				SQLiteDatabase.openOrCreateDatabase(path, DEFAULT_FACTORY);
		db.beginTransaction();
		try {
			InputStream in = context.getResources().getAssets().open(ddlResource);
			BufferedReader reader =
					new BufferedReader(new InputStreamReader(in, ENCODING), 1024);
			DDL ddl = new DDL(reader, ";--");
			try {
				String sql;
				while ((sql = ddl.nextSql()) != null) {
					db.execSQL(sql);
				}
			} finally {
				ddl.terminate();
			}
			
			db.setVersion(version);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		return db;
	}
}
