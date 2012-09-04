package jp.co.johospace.jsphoto.service;

import java.io.IOException;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.onlineservice.DefaultMediaSynchronizer;
import jp.co.johospace.jsphoto.onlineservice.Media;
import jp.co.johospace.jsphoto.onlineservice.MediaDirectory;
import jp.co.johospace.jsphoto.onlineservice.MediaSynchronizer;
import jp.co.johospace.jsphoto.onlineservice.MediaSynchronizer.BehaviorInConflict;
import jp.co.johospace.jsphoto.onlineservice.OnlineMediaServiceClient;
import jp.co.johospace.jsphoto.onlineservice.OnlineMediaServiceClient.InteractionCallback;
import jp.co.johospace.jsphoto.onlineservice.jscloudia.JsCloudiaClient;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.PicasaClient;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.StopWatch;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.FileObserver;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.preference.PreferenceManager;

/**
 * メディア同期サービス
 */
public class MediaSyncService extends PriorableIntentService {
	private static final String tag = MediaSyncService.class.getSimpleName();
	
	/** キープレフィクス */
	protected static final String PREFIX = MediaSyncService.class.getName() + ".";
	
	/** アクション： メディア同期 */
	protected static final String ACTION_SYNC_MEDIA = PREFIX + "ACTION_SYNC_MEDIA";
	/** アクション： メディアインデックス送信 */
	protected static final String ACTION_SEND_MEDIA_INDEX = PREFIX + "ACTION_SEND_MEDIA_INDEX";
	/** アクション： ファイル監視開始 */
	protected static final String ACTION_START_OBSERVATION = PREFIX + "ACTION_START_OBSERVATION";
	/** アクション： ファイル監視停止 */
	protected static final String ACTION_STOP_OBSERVATION = PREFIX + "ACTION_STOP_OBSERVATION";
	
	/** カテゴリ： 自動実行 */
	protected static final String CATEGORY_AUTO_PROCESS = PREFIX + "CATEGORY_AUTO_PROCESS";
	
	/** OSファイル監視 */
	protected static FileObserver mObserver;
	
	/** コンストラクタ */
	public MediaSyncService() {
		super(MediaSyncService.class.getSimpleName(),
				Process.THREAD_PRIORITY_BACKGROUND);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
//		Log.i(tag, String.format("service started. %s", intent));		/*$debug$*/
		StopWatch sw = StopWatch.start();
		try {
			String action = intent.getAction();
			if (ACTION_SYNC_MEDIA.equals(action)) {
				doSyncMedia(intent);
			} else if (ACTION_SEND_MEDIA_INDEX.equals(action)) {
				doSendMediaIndex(intent);
			} else if (ACTION_START_OBSERVATION.equals(action)) {
				doStartObservation(intent);
			} else if (ACTION_STOP_OBSERVATION.equals(action)) {
				doStopObservation(intent);
			} else {
//				Log.w(tag, String.format("unknown action[%s]. %s", action, intent));		/*$debug$*/
			}
		} catch (Exception e) {
//			Log.e(tag, "failed to process sync service.", e);		/*$debug$*/
		} finally {
//			Log.i(tag, String.format("    service finished in %dmsec.", sw.elapsed()));		/*$debug$*/
		}
	}
	
	/**
	 * メディア同期を実行します。
	 * @param intent クライアントからのIntent
	 */
	protected void doSyncMedia(Intent intent) {
		// CPUとWiFiを起こす
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
		WakeLock wakeLock =
				pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
		wakeLock.acquire();
		try {
			WifiLock wifiLock =
					wm.createWifiLock(WifiManager.WIFI_MODE_FULL, tag);
			wifiLock.acquire();
			try {
				if (mObserver != null) {
					mObserver.stopWatching();
				}
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				
				boolean onlyOnWifi = pref.getBoolean("syncPicasaOnlyOnWifi", false);
				boolean auto = intent.hasCategory(CATEGORY_AUTO_PROCESS);
				Integer type;
				if (auto && onlyOnWifi) {
					type = ConnectivityManager.TYPE_WIFI;
				} else {
					type = null;
				}
				
				if (!IOUtil.isNetworkConnected(this, 15, type)) {
					if (auto && onlyOnWifi) {
						notificationIsSync(getString(R.string.notification_picasa_sync)
								, getString(R.string.notification_picasa_sync)
								, getString(R.string.notification_picasa_sync_wifi_is_not_connected));
					} else {
						// 通信回線エラー通知
						notificationIsSync(getString(R.string.notification_picasa_sync)
								, getString(R.string.notification_picasa_sync)
								, getString(R.string.notification_picasa_sync_no_connected));
					}
					
//					Log.e(tag, "network is unavailable. cancel sync.");		/*$debug$*/
					return;
				}
				
				// データベース
				final SQLiteDatabase db = OpenHelper.sync.getDatabase();
				// ローカルディレクトリ
				final String localDir = pref.getString("picasaSyncLocal", null);
				// リモートディレクトリ
				final String remoteDirectoryID = pref.getString("picasaSyncRemote", null);
				// アカウント
				final String account = pref.getString("picasaAccount", null);
				// シンクロナイザ
				final MediaSynchronizer synchronizer = new DefaultMediaSynchronizer(this);
				// クライアント
				final OnlineMediaServiceClient<? extends MediaDirectory, ? extends Media> client =
						new PicasaClient(this, account);
				
				// 再認可（トークン更新）
				final Object waitLock = new Object();
				final boolean[] success = new boolean[1];
				client.authorize(new OnlineMediaServiceClient.AuthorizationHandler() {
					
					@Override
					public void startInteraction(Intent intent, InteractionCallback callback) {
						success[0] = false;
						synchronized (waitLock) {
							waitLock.notifyAll();
						}
					}
					
					@Override
					public void authorizationFinished(String account, boolean authorized) {
						if (authorized) {
							success[0] = true;
						} else {
							success[0] = false;
//							Log.e(tag, "authorization failed.");		/*$debug$*/
						}
						synchronized (waitLock) {
							waitLock.notifyAll();
						}
					}
				}, true);
				
				synchronized (waitLock) {
					try {
						waitLock.wait();
					} catch (InterruptedException e) {
						;
					}
				}
				
				if (success[0]) {
					synchronizer.synchronize(
							db, client, localDir, remoteDirectoryID,
							BehaviorInConflict.TAKE_LATEST);
				} else {
					return;
				}
				
			} finally {
				wifiLock.release();
			}
			
		} catch (IOException e) {
//			Log.e(tag, "failed to synchronize media.", e);		/*$debug$*/
			// 同期失敗通知
			notificationIsSync(getString(R.string.notification_picasa_sync)
					, getString(R.string.notification_picasa_sync)
					, getString(R.string.notification_picasa_sync_failed));
			return;
		} finally {
			wakeLock.release();
			if (mObserver != null) {
				mObserver.startWatching();
			}
		}
		
		if (doSendMediaIndex(intent)) {
			// 同期成功通知
			notificationIsSync(getString(R.string.notification_picasa_sync)
					, getString(R.string.notification_picasa_sync)
					, getString(R.string.notification_picasa_sync_success));
			
		} else {
			// 同期失敗通知
			notificationIsSync(getString(R.string.notification_picasa_sync)
					, getString(R.string.notification_picasa_sync)
					, getString(R.string.notification_picasa_sync_failed));
			
		}
	}
	
	/**
	 * メディアインデックスを送信します。
	 * @param intent クライアントからのIntent
	 */
	protected boolean doSendMediaIndex(Intent intent) {
		// CPUとWiFiを起こす
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
		WakeLock wakeLock =
				pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
		wakeLock.acquire();
		try {
			WifiLock wifiLock =
					wm.createWifiLock(WifiManager.WIFI_MODE_FULL, tag);
			wifiLock.acquire();
			try {
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				
				// クライアント
				JsCloudiaClient client = new JsCloudiaClient(this);
				// データベース
				SQLiteDatabase db = OpenHelper.sync.getDatabase();
				// アカウント
				String account = pref.getString("picasaAccount", null);
				client.sendDirties(db, PicasaClient.SERVICE_TYPE, account);
				
				return true;
				
			} finally {
				wifiLock.release();
			}
		} catch (IOException e) {
//			Log.e(tag, "failed to send media indexes.", e);		/*$debug$*/
			return false;
		} finally {
			wakeLock.release();
		}
	}
	
	protected void notificationIsSync(String tickerText, String title, String msg){
		// 通知
		NotificationManager manager =
				(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Intent intent = new Intent(Intent.ACTION_VIEW);
	    PendingIntent pendingIntent = PendingIntent.getService(MediaSyncService.this, 0, intent, 0);
		Notification notification = new Notification();
		notification.icon = R.drawable.icon;
		notification.tickerText = tickerText;
		notification.when = System.currentTimeMillis(); notification.setLatestEventInfo(
			 getApplicationContext(), title, msg, pendingIntent);
		manager.notify(1, notification);
	}
	
	@Override
	protected void doStopSelf(int startId) {
		synchronized (MediaSyncService.class) {
			if (mObserver == null) {
				super.doStopSelf(startId);
			} else {
				// ファイル監視中は生き残る
				;
			}
		}
	}
	
	/**
	 * 同期フォルダ監視を開始します。
	 * @param intent インテント
	 */
	protected void doStartObservation(Intent intent) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final String localDir = pref.getString("picasaSyncLocal", null);
		synchronized (MediaSyncService.class) {
			if (mObserver != null) {
				mObserver.stopWatching();
			}
			int mask = /*FileObserver.CREATE
					+*/ FileObserver.MODIFY
					+ FileObserver.DELETE
					+ FileObserver.MOVED_TO
					+ FileObserver.MOVED_FROM;
			mObserver = new FileObserver(localDir, mask) {
				Thread mWaiter;
				@Override
				public void onEvent(int event, String path) {
//					Log.d(tag, String.format("detect sync folder changed. path=%s, event=%d", path, event));		/*$debug$*/
					if (path != null) {
						synchronized (MediaSyncService.class) {
							if (mWaiter != null) {
								mWaiter.interrupt();
							}
							mWaiter = new Thread() {
								@Override
								public void run() {
									synchronized (MediaSyncService.class) {
										try {
											// 一定時間後続を待つ
											MediaSyncService.class.wait(2000L);
										} catch (InterruptedException e) {
//											Log.d(tag, "next detection came in my wait-time. i am cancelled.");		/*$debug$*/
											return;
										}
										
//										Log.d(tag, "nobody came in my wait-time. i will invoke!");		/*$debug$*/
										Intent intent = new Intent(MediaSyncService.this, MediaSyncService.class);
										intent.setAction(ACTION_SYNC_MEDIA);
										intent.addCategory(CATEGORY_AUTO_PROCESS);
										startService(intent);
									}
								}
							};
							mWaiter.start();
						}
					}
				}
			};
			mObserver.startWatching();
		}
	}
	
	/**
	 * 同期フォルダ監視を停止します。
	 * @param intent インテント
	 */
	protected void doStopObservation(Intent intent) {
		synchronized (MediaSyncService.class) {
			if (mObserver != null) {
				mObserver.stopWatching();
				mObserver = null;
			}
		}
	}
}
