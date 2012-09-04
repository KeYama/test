package jp.co.johospace.jsphoto.accessor;

import jp.co.johospace.jsphoto.provider.JorlleSyncProvider;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.db.CMediaSync;
import android.content.Context;
import android.database.Cursor;

/**
 * JorlleSyncProviderのアクセスクラス
 */
public final class SyncProviderAccessor {
	private SyncProviderAccessor() {}
	
	/**
	 * メディアが同期されているかどうかを取得します
	 * @param context コンテキスト
	 * @param media メディア
	 * @return
	 */
	public static boolean isSyncedMedia(Context context, Media media) {
		return isSyncedMedia(context, media.service, media.account, media.mediaId);
	}
	
	/**
	 * メディアが同期されているかどうかを取得します
	 * @param context コンテキスト
	 * @param serviceType サービスタイプ
	 * @param serviceAccount サービスアカウント
	 * @param mediaId メディアID
	 * @return
	 * @see ServiceType
	 */
	public static boolean isSyncedMedia(Context context, String serviceType, String serviceAccount, String mediaId) {
		Cursor c = context.getContentResolver().query(
				JorlleSyncProvider.getUriFor(context, CMediaSync.$TABLE),
				null,
				CMediaSync.SERVICE_TYPE + " = ?" +
						" AND " + CMediaSync.SERVICE_ACCOUNT + " = ?" +
						" AND " + CMediaSync.MEDIA_ID + " = ?",
				new String[] {serviceType, serviceAccount, mediaId}, null);
		try {
			return c.moveToFirst();
		} finally {
			c.close();
		}
	}
}
