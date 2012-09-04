package jp.co.johospace.jsphoto.v2.onlineservice.clients;

import java.lang.reflect.Constructor;

import jp.co.johospace.jsphoto.R;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * クライアントマネージャ
 */
public class ClientManager {
	private ClientManager() {
	}
	
	public static JsMediaServerClient getJsMediaServerClient(Context context) {
		return newImplementation(
				"jp.co.johospace.jsphoto.v2.onlineservice.jsmedia.JsMediaClient", context);
	}
	
	public static ExternalServiceClient getExternalServiceClient(Context context, String serviceType) {
		Class<? extends ExternalServiceClient> type;
		if (serviceType.equals(ServiceType.FACEBOOK)) {
			type = FacebookClient.class;
		} else if (serviceType.equals(ServiceType.PICASA_WEB)) {
			type = PicasaClient.class;
		} else if (serviceType.equals(ServiceType.TWITTER)) {
			type = TwitterClient.class;
		} else {
			return null;
		}
		
		return getExternalServiceClient(context, type);
	}
	
	public static <T extends ExternalServiceClient> T getExternalServiceClient(Context context, Class<T> type) {
		if (type == PicasaClient.class) {
			return newImplementation(
					"jp.co.johospace.jsphoto.v2.onlineservice.picasa.PicasaClientImpl", context);
		} else if (type == TwitterClient.class) {
			return newImplementation(
					"jp.co.johospace.jsphoto.v2.onlineservice.twitter.TwitterClientImpl", context);
		} else if (type == FacebookClient.class) {
			return newImplementation(
					"jp.co.johospace.jsphoto.v2.onlineservice.facebook.FacebookClientImpl", context);
		}
		
		return null;
	}
	
	static <T> T newImplementation(String name, Context context) {
		try {
			@SuppressWarnings("unchecked")
			Class<T> clazz = (Class<T>) Class.forName(name);
			Constructor<T> c = clazz.getConstructor(Context.class);
			return (T) c.newInstance(context);
		} catch (Exception e) {
//			Log.e(ClientManager.class.getSimpleName(),		/*$debug$*/
//					"failed to create implementation instance.", e);		/*$debug$*/
			return null;
		}
	}
	
	public static boolean isBidirectional(String serviceType) {
		return ServiceType.PICASA_WEB.equals(serviceType);
	}
	
	public static boolean hasDirectory(String serviceType) {
		return !ServiceType.TWITTER.equals(serviceType);
	}
	
	public static boolean hasMedia(String serviceType) {
		return ServiceType.PICASA_WEB.equals(serviceType)
				|| ServiceType.TWITTER.equals(serviceType)
				|| ServiceType.FACEBOOK.equals(serviceType);
	}
	
	public static boolean isScheduler(String serviceType) {
		return ServiceType.GOOGLE_CALENDAR.equals(serviceType)
				|| ServiceType.JORTE.equals(serviceType);
	}
	
	public static String getServiceName(Context context, String serviceType) {
		if (ServiceType.PICASA_WEB.equals(serviceType)) {
			return context.getString(R.string.service_picasa);
		} else if (ServiceType.FACEBOOK.equals(serviceType)) {
			return context.getString(R.string.service_facebook);
		} else if (ServiceType.TWITTER.equals(serviceType)) {
			return context.getString(R.string.service_twitter);
		} else if (ServiceType.JORTE.equals(serviceType)) {
			return context.getString(R.string.service_jorte);
		} else if (ServiceType.GOOGLE_CALENDAR.equals(serviceType)) {
			return context.getString(R.string.service_gcal);
		} else {
			throw new IllegalArgumentException(String.format("what is %s?", serviceType));
		}
	}
	
	public static int getIconResource(Context context, String serviceType) {
		if (ServiceType.PICASA_WEB.equals(serviceType)) {
			return R.drawable.ic_online_picasa;
		} else if (ServiceType.FACEBOOK.equals(serviceType)) {
			return R.drawable.ic_online_fb;
		} else if (ServiceType.TWITTER.equals(serviceType)) {
			return R.drawable.ic_online_twitter;
		} else if (ServiceType.JORTE.equals(serviceType)) {
			return R.drawable.ic_online_jorte;
		} else if (ServiceType.GOOGLE_CALENDAR.equals(serviceType)) {
			return R.drawable.ic_online_gcal;
		} else {
			throw new IllegalArgumentException(String.format("what is %s?", serviceType));
		}
	}
	
	/**
	 * メディアをブラウザで表示するためのインテントを取得します
	 * @param context コンテキスト
	 * @param serviceType サービスタイプ
	 * @param serviceAccount サービスアカウント
	 * @param mediaId メディアID
	 * @return Intent:{"act"="android.intent.action.VIEW", "cat"="android.intent.category.BROWSABLE", "data"=$media_uri]}
	 */
	public static Intent getShareIntent(Context context, String serviceType, String serviceAccount, String mediaId) {
		if (ServiceType.PICASA_WEB.equals(serviceType)) {
			return null;
		} else if (ServiceType.FACEBOOK.equals(serviceType)) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addCategory(Intent.CATEGORY_BROWSABLE);
			
			intent.setData(Uri.parse(String.format("http://www.facebook.com/photo.php?fbid=%s", mediaId)));
			return intent;
		} else if (ServiceType.TWITTER.equals(serviceType)) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addCategory(Intent.CATEGORY_BROWSABLE);
			
			// mediaId = id + "," + url
			String[] idSeg = mediaId.split(",");
			intent.setData(Uri.parse(String.format("http://twitter.com/%s/status/%s", serviceAccount, idSeg[0])));
			return intent;
		} else if (ServiceType.JORTE.equals(serviceType)) {
			return null;
		} else if (ServiceType.GOOGLE_CALENDAR.equals(serviceType)) {
			return null;
		} else {
			throw new IllegalArgumentException(String.format("what is %s?", serviceType));
		}
	}
	
	/**
	 * 外部サービスの表示順を返します。
	 * @param serviceType サービスタイプ
	 * @return
	 */
	public static int getDisplayOrder(String serviceType) {
		if (ServiceType.PICASA_WEB.equals(serviceType)) {
			return 1;
		} else if (ServiceType.TWITTER.equals(serviceType)) {
			return 2;
		} else if (ServiceType.FACEBOOK.equals(serviceType)) {
			return 3;
		} else if (ServiceType.GOOGLE_CALENDAR.equals(serviceType)) {
			return 4;
		} else if (ServiceType.JORTE.equals(serviceType)) {
			return 5;
		} else {
			throw new IllegalArgumentException(String.format("what is %s?", serviceType));
		}
	}
}
