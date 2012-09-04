package jp.co.johospace.jsphoto.provider;

import java.io.File;
import java.io.IOException;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.database.CMediaSync;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.util.IOUtil;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * メディア管理 同期プロバイダ
 */
public class JorlleSyncProvider extends ContentProvider {

	/** MIMEタイプ： メディア同期 */
	public static final String TYPE_MEDIASYNC =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.jp.co.johospace.jsphoto." + CMediaSync.$TABLE;
	
	private static final int MEDIASYNC = 1;
	public static final String PATH_MEDIASYNC = CMediaSync.$TABLE;
	
	private static UriMatcher sMatcher;
	
	@Override
	public final void attachInfo(Context context, ProviderInfo info) {
		super.attachInfo(context, info);
		if (sMatcher == null) {
			UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
			matcher.addURI(info.authority, PATH_MEDIASYNC, MEDIASYNC);
			sMatcher = matcher;
		}
	}

	@Override
	public String getType(Uri uri) {
		int match = sMatcher.match(uri);
		switch (match) {
		case MEDIASYNC:
			return TYPE_MEDIASYNC;
		}
		
		return null;
	}
	
	@Override
	public boolean onCreate() {
		upgradeDatabaseFromBeta();
		OpenHelper.sync.initialize();
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int match = sMatcher.match(uri);
		switch (match) {
		case MEDIASYNC: {
			return OpenHelper.sync.getDatabase().query(
					CMediaSync.$TABLE, projection, selection, selectionArgs, null, null, sortOrder);
		}
		}
		
		return null;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}
	
	public static Uri getUriFor(Context context, String pathSegment) {
		String authority = context.getString(R.string.authority_sync_provider);
		return Uri.parse(String.format("content://%s/%s",
				authority, pathSegment));
	}

	private void upgradeDatabaseFromBeta() {
		// 旧DBファイル
		File dir = new File(getContext().getExternalCacheDir(), "database");
		File oldDbFile = new File(dir, "external-sync.db");
		// 新DBファイル
		File newDbFile = OpenHelper.sync.getDatabaseFile();
		
		if (oldDbFile.exists()
				&& !newDbFile.exists()) {

			// 旧から新の場所へDBファイルを丸ごと移動する
			try {
				IOUtil.copy(oldDbFile, newDbFile);
			} catch (IOException e) {
				Log.e(JorlleSyncProvider.class.getSimpleName(),
						"couldn't save data in beta time.", e);
				newDbFile.delete();
			}
			
			oldDbFile.delete();
		}
	}
}
