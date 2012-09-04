package jp.co.johospace.jsphoto.v2.onlineservice.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.provider.JorlleProvider;
import jp.co.johospace.jsphoto.util.RowHandler;
import jp.co.johospace.jsphoto.util.StringPair;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Metadata;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * プロバイダアクセサ
 */
public class ProviderAccessor {

	public static List<Metadata> queryMetadata(Context context, String dirpath, String name) {
		Cursor c = context.getContentResolver().query(
				JorlleProvider.getUriFor(context, new String[] {CMediaMetadata.$TABLE}),
				null,
				CMediaMetadata.DIRPATH + " = ?" +
						" AND " + CMediaMetadata.NAME + " = ?",
				new String[] {dirpath, name}, null);
		try {
			ArrayList<Metadata> metadata = new ArrayList<Metadata>();
			RowHandler<Metadata> handler = Metadata.createRowHandler(c);
			while (c.moveToNext()) {
				Metadata m = new Metadata();
				handler.populateCurrentRow(c, m);
				metadata.add(m);
			}
			return metadata;
		} finally {
			c.close();
		}
	}
	
	public static int replaceMetadata(Context context,
			String dirpath, String name, Collection<Metadata> metadatas, boolean silent) {
//		Map<String, List<ContentValues>> types = new HashMap<String, List<ContentValues>>();
//		for (Metadata metadata : metadatas) {
//			List<ContentValues> list = types.get(metadata.type);
//			if (list == null) {
//				list = new ArrayList<ContentValues>();
//				types.put(metadata.type, list);
//			}
//			ContentValues values = metadata.toValues();
//			values.put(CMediaMetadata.DIRPATH, dirpath);
//			values.put(CMediaMetadata.NAME, name);
//			values.put(CMediaMetadata.UPDATE_TIMESTAMP, System.currentTimeMillis());
//			list.add(values);
//		}
//		
//		int effected = 0;
//		for (String type : types.keySet()) {
//			List<ContentValues> list = types.get(type);
//			Uri uri = JorlleProvider.getUriFor(this,
//					new String[] {CMediaMetadata.$TABLE, dirpath, name, type},
//					new StringPair("silent", String.valueOf(silent)));
//			effected += getContentResolver().bulkInsert(uri, list.toArray(new ContentValues[0]));
//		}
//		
//		return effected;
		ArrayList<ContentValues> list = new ArrayList<ContentValues>();
		for (Metadata metadata : metadatas) {
			ContentValues values = metadata.toValues();
			values.put(CMediaMetadata.DIRPATH, dirpath);
			values.put(CMediaMetadata.NAME, name);
			values.put(CMediaMetadata.UPDATE_TIMESTAMP, System.currentTimeMillis());
			list.add(values);
		}
		Uri uri = JorlleProvider.getUriFor(context,
				new String[] {CMediaMetadata.$TABLE, dirpath, name, "*"},
				new StringPair("silent", String.valueOf(silent)));
		return context.getContentResolver().bulkInsert(uri, list.toArray(new ContentValues[0]));
	}
	
	public static int deleteMetadata(Context context, String dirpath, String name) {
		Uri uri = JorlleProvider.getUriFor(context,
				new String[] {CMediaMetadata.$TABLE});
		return context.getContentResolver().delete(uri,
				CMediaMetadata.DIRPATH + " = ?" +
						" AND " + CMediaMetadata.NAME + " = ?",
				new String[] {dirpath, name});
	}
}
