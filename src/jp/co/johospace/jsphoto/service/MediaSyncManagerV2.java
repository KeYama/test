package jp.co.johospace.jsphoto.service;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import jp.co.johospace.jsphoto.PicasaPrefsActivity;
import jp.co.johospace.jsphoto.util.JsonUtil;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

/**
 * メディア同期のマネージャ
 */
public class MediaSyncManagerV2 {
	private MediaSyncManagerV2() {}
	private static final String tag = MediaSyncManagerV2.class.getSimpleName();
	
	private static final int RESCHEDULE_MINUTES = 10;
	
	/** ブロードキャストレシーバ */
	public static class Receiver extends BroadcastReceiver {
		@Override
		public void onReceive(final Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
					Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
//				System.out.println(intent);		/*$debug$*/
				new Thread() {
					@Override
					public void run() {
						if (PicasaPrefsActivity.isAutoSyncEnabled(context)) {
							scheduleRepeatingSyncMedia(context, null, false);
						}
						if(isLocalSyncAllowed(context)){
							Calendar cal = Calendar.getInstance();
							cal.add(Calendar.MINUTE, RESCHEDULE_MINUTES);
							scheduleSend(context, cal.getTimeInMillis(), null);
						}
					}
				}.start();
			} else if ("jp.co.johospace.jscloudia.intent.action.REQUEST_SYNC".equals(action)) {
				startSyncMedia(context, intent.getExtras());
			}
		}
	}

	/** Alarmリクエスト： メディア同期 */
	protected static final int REQUEST_REPEATING_MEDIA_SYNC = 1;
	/** Alarmリクエスト： 送信 */
	protected static final int REQUEST_REPEATING_SEND = 2;
	/** Alarmリクエスト： 送信 */
	protected static final int REQUEST_SEND = 3;
	
	
	/**
	 * バックグラウンドでメディア同期を開始します。
	 * @param context コンテキスト
	 * @param extras エクストラ
	 */
	public static void startSyncMedia(Context context, Bundle extras) {
		Intent intent =
				createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_SYNC_MEDIA);
		if (extras != null) {
			intent.putExtras(extras);
		}
		context.startService(intent);
	}
	
	/**
	 * 設定された間隔で繰り返しのメディア同期をスケジューリングします。
	 * @param context コンテキスト
	 * @param extras サービスへのExtra
	 * @param immediate 即時実行する場合true
	 */
	public static void scheduleRepeatingSyncMedia(Context context, Bundle extras, boolean immediate) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		long interval = Long.parseLong(pref.getString(PicasaPrefsActivity.SYNC_PICASA_INTERVAL, "-1"));
		if (0 < interval) {
			scheduleRepeatingSyncMedia(context, interval, extras, immediate);
		}
	}
	
	/**
	 * 繰り返しのメディア同期をスケジューリングします。
	 * @param context コンテキスト
	 * @param interval 繰り返し間隔(msec)
	 * @param extras サービスへのExtra
	 * @param immediate 即時実行する場合true
	 */
	public static void scheduleRepeatingSyncMedia(Context context, long interval, Bundle extras, boolean immediate) {
		Intent intent =
				createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_SYNC_MEDIA);
		intent.addCategory(MediaSyncServiceV2.CATEGORY_AUTO_PROCESS);
		if (extras != null) {
			intent.putExtras(extras);
		}
		
		PendingIntent operation =
				PendingIntent.getService(context,
						REQUEST_REPEATING_MEDIA_SYNC, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		long triggerAt =
				System.currentTimeMillis() + (immediate ? 0L : interval);
		
		AlarmManager manager =
				(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		manager.setRepeating(AlarmManager.RTC_WAKEUP,
				triggerAt, interval, operation);
//		Log.i(tag, String.format(		/*$debug$*/
//				"scheduled auto-sync of media." +		/*$debug$*/
//				" triggering at %1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS. repeating in interval %2$dmin.",		/*$debug$*/
//				triggerAt, interval / DateUtils.MINUTE_IN_MILLIS));		/*$debug$*/
		
		// 同期フォルダの監視を開始する
		startObservation(context, extras);
	}
	
	/**
	 * スケジュールされた繰り返しのメディア同期をキャンセルします。
	 * @param context コンテキスト
	 */
	public static void cancelRepeatingSyncMedia(Context context) {
		Intent intent = createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_SYNC_MEDIA);
		intent.addCategory(MediaSyncServiceV2.CATEGORY_AUTO_PROCESS);
		
		AlarmManager manager =
				(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent operation =
				PendingIntent.getService(context,
						REQUEST_REPEATING_MEDIA_SYNC, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		manager.cancel(operation);
//		Log.i(tag, "auto-sync of media was canceled.");		/*$debug$*/
		
		// 同期フォルダ監視を停止する
		stopObservation(context);
	}
	
	
	/**
	 * バックグラウンドで送信を開始します。
	 * @param context コンテキスト
	 * @param extras エクストラ
	 */
	public static void startSend(Context context, Bundle extras) {
		Intent intent =
				createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_SEND_MEDIA_INDEX);
		if (extras != null) {
			intent.putExtras(extras);
		}
		context.startService(intent);
	}
	
	/**
	 * メディアインデックス送信をスケジューリングします。
	 * @param context コンテキスト
	 * @param triggerAt 起動時刻(msec)
	 * @param interval 繰り返し間隔(msec)
	 * @param extras サービスへのExtra
	 */
	public static void scheduleRepeatingSend(Context context, long triggerAt, long interval, Bundle extras) {
		Intent intent =
				createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_SEND_MEDIA_INDEX);
		intent.addCategory(MediaSyncServiceV2.CATEGORY_AUTO_PROCESS);
		if (extras != null) {
			intent.putExtras(extras);
		}
		
		PendingIntent operation =
				PendingIntent.getService(context,
						REQUEST_REPEATING_SEND, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		AlarmManager manager =
				(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		manager.setRepeating(AlarmManager.RTC_WAKEUP, /*やはり起こす*/
				triggerAt, interval, operation);
//		Log.i(tag, String.format(		/*$debug$*/
//				"scheduled auto-send of device status." +		/*$debug$*/
//				" triggering at %1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS. repeating in interval %2$dmin.",		/*$debug$*/
//				triggerAt, interval / DateUtils.MINUTE_IN_MILLIS));		/*$debug$*/
	}
	
	/**
	 * スケジュールされたメディアインデックスの送信をキャンセルします。
	 * @param context コンテキスト
	 */
	public static void cancelRepeatingSend(Context context) {
		Intent intent = createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_SEND_MEDIA_INDEX);
		intent.addCategory(MediaSyncServiceV2.CATEGORY_AUTO_PROCESS);
		
		AlarmManager manager =
				(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent operation =
				PendingIntent.getService(context,
						REQUEST_REPEATING_SEND, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		manager.cancel(operation);
//		Log.i(tag, "auto-send of device status was canceled.");		/*$debug$*/
	}
	
	/**
	 * メディアインデックス送信をスケジューリングします。
	 * @param context コンテキスト
	 * @param triggerAt 起動時刻(msec)
	 * @param extras サービスへのExtra
	 */
	public static void scheduleSend(Context context, long triggerAt, Bundle extras) {
		if(!isLocalSyncAllowed(context)){
			//安全のため二重ガード
			return;
		}
		
		Intent intent =
				createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_SEND_MEDIA_INDEX);
		intent.addCategory(MediaSyncServiceV2.CATEGORY_AUTO_PROCESS);
		if (extras != null) {
			intent.putExtras(extras);
		}
		
		PendingIntent operation =
				PendingIntent.getService(context,
						REQUEST_SEND, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		AlarmManager manager =
				(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		manager.set(AlarmManager.RTC_WAKEUP, /*やはり起こす*/
				triggerAt, operation);
//		Log.i(tag, String.format(		/*$debug$*/
//				"scheduled auto-send of device status." +		/*$debug$*/
//				" triggering at %1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.",		/*$debug$*/
//				triggerAt));		/*$debug$*/
	}
	
	/**
	 * スケジュールされたメディアインデックスの送信をキャンセルします。
	 * @param context コンテキスト
	 */
	public static void cancelSend(Context context) {
		Intent intent = createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_SEND_MEDIA_INDEX);
		intent.addCategory(MediaSyncServiceV2.CATEGORY_AUTO_PROCESS);
		
		AlarmManager manager =
				(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent operation =
				PendingIntent.getService(context,
						REQUEST_SEND, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		manager.cancel(operation);
//		Log.i(tag, "auto-send of device status was canceled.");		/*$debug$*/
	}
	
	/**
	 * 同期フォルダの監視を開始します。
	 * @param context コンテキスト
	 * @param extras サービスへのExtra
	 */
	public static void startObservation(Context context, Bundle extras) {
		Intent intent = createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_START_OBSERVATION);
		if (extras != null) {
			intent.putExtras(extras);
		}
		context.startService(intent);
	}
	
	/**
	 * 同期フォルダの監視を停止します。
	 * @param context コンテキスト
	 */
	public static void stopObservation(Context context) {
		Intent intent = createSyncServiceIntent(context, MediaSyncServiceV2.ACTION_STOP_OBSERVATION);
		context.startService(intent);
	}
	
	/**
	 * メディア同期サービスを起動するIntentを生成します。
	 * @param context コンテキスト
	 * @param action アクション
	 * @return 記録サービスを起動するIntent
	 */
	static Intent createSyncServiceIntent(Context context, String action) {
		Intent intent = new Intent(context, MediaSyncServiceV2.class);
		intent.setAction(action);
		return intent;
	}
	
	private static final String KEY_2WAY_SETTING =
			MediaSyncManagerV2.class.getSimpleName() + "|2way";
	
	public static class SyncSetting {
		public String service;
		public String account;
		public String localDir;
		public Boolean onlyOnWifi;
		public Long interval;
		
		public Long lastUpdated;
	}
	
	private static final Type SETTINGS_TYPE =
			new TypeToken<Map<String, Map<String, SyncSetting>>>() {}.getType();
	
	public static synchronized Map<String, Map<String, SyncSetting>> loadSyncSettings(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		String json = pref.getString(KEY_2WAY_SETTING, null);
		if (TextUtils.isEmpty(json)) {
			return new HashMap<String, Map<String, SyncSetting>>();
		} else {
			return JsonUtil.fromJson(json, SETTINGS_TYPE);
		}
	}
	
	public static synchronized void saveSyncSetting(Context context, SyncSetting setting) {
		Map<String, Map<String, SyncSetting>> settings = loadSyncSettings(context);
		Map<String, SyncSetting> accounts = settings.get(setting.service);
		if (accounts == null) {
			accounts = new HashMap<String, SyncSetting>();
			settings.put(setting.service, accounts);
		}
		
		accounts.put(setting.account, setting);
		
		saveSyncSettings(context, settings);
	}
	
	private static final String NONE_STRING = new String();
	
	public static synchronized void deleteSyncSetting(Context context, String service) {
		deleteSyncSetting(context, service, NONE_STRING);
	}
	
	public static synchronized void deleteSyncSetting(Context context, String service, String account) {
		Map<String, Map<String, SyncSetting>> settings = loadSyncSettings(context);
		Map<String, SyncSetting> accounts = settings.get(service);
		if (accounts != null) {
			if (account == NONE_STRING) {
				accounts.clear();
			} else {
				accounts.remove(account);
			}
		}
		saveSyncSettings(context, settings);
	}

	protected static synchronized void saveSyncSettings(Context context,
			Map<String, Map<String, SyncSetting>> settings) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString(KEY_2WAY_SETTING, JsonUtil.toJson(settings, SETTINGS_TYPE)).commit();
	}
	
	
	
	
	private static final String KEY_LOCAL_SYNC =
			MediaSyncManagerV2.class.getSimpleName().concat(".KEY_LOCAL_SYNC");
	public static boolean isLocalSyncAllowed(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(KEY_LOCAL_SYNC, false);
	}
	public static void saveLocalSyncAllowed(Context context, boolean b) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.edit().putBoolean(KEY_LOCAL_SYNC, b).commit();
	}
}
