package jp.co.johospace.jsphoto.service;

import java.io.IOException;
import java.util.Calendar;
import java.util.Map;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.StopWatch;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.LocalSynchronizer;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.MediaSynchronizer;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.FileObserver;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;

/**
 * メディア同期サービス
 */
public class MediaSyncServiceV2 extends PriorableIntentService {
	private static final String tag = MediaSyncServiceV2.class.getSimpleName();
	
	/** キープレフィクス */
	protected static final String PREFIX = MediaSyncServiceV2.class.getName() + ".";
	
	/** アクション： メディア同期 */
	protected static final String ACTION_SYNC_MEDIA = PREFIX + "ACTION_SYNC_MEDIA";
	/** アクション： メディアインデックス送信 */
	protected static final String ACTION_SEND_MEDIA_INDEX = PREFIX + "ACTION_SEND_MEDIA_INDEX";
	/** アクション： ファイル監視開始 */
	protected static final String ACTION_START_OBSERVATION = PREFIX + "ACTION_START_OBSERVATION";
	/** アクション： ファイル監視停止 */
	protected static final String ACTION_STOP_OBSERVATION = PREFIX + "ACTION_STOP_OBSERVATION";
	/** アクション： 同期中止 */
	protected static final String ACTION_CANCEL_SYNC_MEDIA = PREFIX + "ACTION_CANCEL_SYNC_MEDIA";
	
	/** カテゴリ： 自動実行 */
	protected static final String CATEGORY_AUTO_PROCESS = PREFIX + "CATEGORY_AUTO_PROCESS";
	
	/** Extra： 全件再送信 */
	public static final String EXTRA_RESEND_ALL = PREFIX + "EXTRA_RESEND_ALL";
	
	/** OSファイル監視 */
	protected static FileObserver mObserver;
	
	/** コンストラクタ */
	public MediaSyncServiceV2() {
		super(MediaSyncServiceV2.class.getSimpleName(),
				Process.THREAD_PRIORITY_BACKGROUND);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		if (intent == null) {
			intent = new Intent(ACTION_START_OBSERVATION);
//			Log.d(tag, "service \"re\"starting");		/*$debug$*/
		}
		
		if (ACTION_CANCEL_SYNC_MEDIA.equals(intent.getAction())) {
			// キャンセル要求はキューイングしない
			MediaSynchronizer synchronizer = mCurrentSynchronizer;
			if (synchronizer != null) {
				synchronizer.requestCancel();
				notifyCanceling();
				postTask(new Runnable() {
					@Override
					public void run() {
						removeNotification();
					}
				});
			}
		} else {
			super.onStart(intent, startId);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
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
	
	private MediaSynchronizer mCurrentSynchronizer;
	
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
				boolean auto = intent.hasCategory(CATEGORY_AUTO_PROCESS);
				
				if (mObserver != null) {
					mObserver.stopWatching();
				}
				try {
					// 同期実行
					mCurrentSynchronizer = new MediaSynchronizer(this);
					ClientManager.getJsMediaServerClient(this).getAuthPreferences(true);
					Map<String, Map<String, SyncSetting>> settings =
							MediaSyncManagerV2.loadSyncSettings(this);
					for (final String service : settings.keySet()) {
						Map<String, SyncSetting> accounts = settings.get(service);
						if (accounts.isEmpty()) {
							MediaSyncManagerV2.deleteSyncSetting(this, service);
						}
						for (String account : accounts.keySet()) {
							SyncSetting setting = accounts.get(account);
							try {
								
								boolean onlyOnWifi = setting.onlyOnWifi != null && setting.onlyOnWifi;
								Integer type;
								if (auto && onlyOnWifi) {
									type = ConnectivityManager.TYPE_WIFI;
								} else {
									type = null;
								}
								
								if (!IOUtil.isNetworkConnected(this, 15, type)) {
//									Log.e(tag, "network is unavailable. cancel sync.");		/*$debug$*/
									return;
								}
								
								MediaSynchronizer.NetworkTransferListener listener = null;
								if (auto) {
									listener = new MediaSynchronizer.NetworkTransferListener() {
										
										boolean mNotified;
										
										@Override
										public void onUpload(boolean delete) {
											if (!mNotified) {
												notifySynchronizing(
														ClientManager.getServiceName(MediaSyncServiceV2.this, service));
												mNotified = true;
											}
										}
										
										@Override
										public void onDownload(boolean delete) {
											if (!mNotified) {
												notifySynchronizing(
														ClientManager.getServiceName(MediaSyncServiceV2.this, service));
												mNotified = true;
											}
										}
									};
								}
								
								mCurrentSynchronizer.synchronize(
										setting.service, setting.account, setting.localDir, listener);
								
							} catch (IOException e) {
//								e.printStackTrace();		/*$debug$*/
								continue;
							} finally {
								setting.lastUpdated = System.currentTimeMillis();
								MediaSyncManagerV2.saveSyncSetting(this, setting);
							}
						}
					}
					
				} finally {
					if (auto) {
						removeNotification();
					}
				}
				
				
			} finally {
				wifiLock.release();
			}
			
		} catch (IOException e) {
//			e.printStackTrace();		/*$debug$*/
		
		} finally {
			mCurrentSynchronizer = null;
			if (mObserver != null) {
				mObserver.startWatching();
			}
			wakeLock.release();
		}
	}
	
	/**
	 * メディアインデックスを送信します。
	 * @param intent クライアントからのIntent
	 */
	protected boolean doSendMediaIndex(Intent intent) {
		// 失敗した時のために1日後をスケジューリングしておく
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		MediaSyncManagerV2.scheduleSend(this, cal.getTimeInMillis(), null);
		
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
				
				LocalSynchronizer synchronizer = new LocalSynchronizer(this);
				long next = synchronizer.sendLocalIndexes(
						intent.getBooleanExtra(EXTRA_RESEND_ALL, false));
				if (next != Long.MIN_VALUE) {
					// サーバから返された時刻をもとに再スケジューリング
					MediaSyncManagerV2.scheduleSend(this, next, null);
				}
				
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
	
	@Override
	protected void doStopSelf(int startId) {
		synchronized (MediaSyncServiceV2.class) {
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
		Map<String, Map<String, SyncSetting>> settings =
				MediaSyncManagerV2.loadSyncSettings(this);
		SyncSetting setting = null;
		for (String service : settings.keySet()) {
			Map<String, SyncSetting> accounts = settings.get(service);
			for (String account : accounts.keySet()) {
				setting = accounts.get(account);
			}
		}
		
		if (setting != null) {
			synchronized (MediaSyncServiceV2.class) {
				if (mObserver != null) {
					mObserver.stopWatching();
				}
				int mask = /*FileObserver.CREATE
						+*/ FileObserver.MODIFY
						+ FileObserver.DELETE
						+ FileObserver.MOVED_TO
						+ FileObserver.MOVED_FROM;
				mObserver = new FileObserver(setting.localDir, mask) {
					Thread mWaiter;
					@Override
					public void onEvent(int event, String path) {
//						Log.d(tag, String.format("detect sync folder changed. path=%s, event=%d", path, event));		/*$debug$*/
						if (path != null) {
							synchronized (MediaSyncServiceV2.class) {
								if (mWaiter != null) {
									mWaiter.interrupt();
								}
								mWaiter = new Thread() {
									@Override
									public void run() {
										synchronized (MediaSyncServiceV2.class) {
											try {
												// 一定時間後続を待つ
												MediaSyncServiceV2.class.wait(2000L);
											} catch (InterruptedException e) {
//												Log.d(tag, "next detection came in my wait-time. i am cancelled.");		/*$debug$*/
												return;
											}
											
//											Log.d(tag, "nobody came in my wait-time. i will invoke!");		/*$debug$*/
											Intent intent = new Intent(MediaSyncServiceV2.this, MediaSyncServiceV2.class);
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
	}
	
	/**
	 * 同期フォルダ監視を停止します。
	 * @param intent インテント
	 */
	protected void doStopObservation(Intent intent) {
		synchronized (MediaSyncServiceV2.class) {
			if (mObserver != null) {
				mObserver.stopWatching();
				mObserver = null;
			}
		}
	}
	
	
	private static final int NOTIFICATION_SYNCHRONIZING = 1;
	
	protected void notifySynchronizing(String serviceName) {
		NotificationManager manager =
				(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		Intent cancel = new Intent(this, MediaSyncServiceV2.class);
		cancel.setAction(ACTION_CANCEL_SYNC_MEDIA);
		PendingIntent intent = PendingIntent.getService(this, 0, cancel, 0);
		
		Notification notification = new Notification();
		notification.icon = R.drawable.icon;
		notification.tickerText = getString(R.string.notification_ticker_synchronizing);
		notification.when = System.currentTimeMillis();
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		notification.setLatestEventInfo(this,
				getString(R.string.notification_title_synchronizing),
				getString(R.string.notification_text_synchronizing, serviceName),
				intent);
		
		manager.notify(NOTIFICATION_SYNCHRONIZING, notification);
	}
	
	protected void notifyCanceling() {
		NotificationManager manager =
				(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		Notification notification = new Notification();
		notification.icon = R.drawable.icon;
		notification.tickerText = null;
		notification.when = System.currentTimeMillis();
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		PendingIntent intent = PendingIntent.getService(this, 0, new Intent(), 0);
		
		notification.setLatestEventInfo(this,
				getString(R.string.notification_title_canceling_sync),
				getString(R.string.notification_text_canceling_sync),
				intent);
		
		manager.notify(NOTIFICATION_SYNCHRONIZING, notification);
	}
	
	protected void removeNotification() {
		NotificationManager manager =
				(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.cancel(NOTIFICATION_SYNCHRONIZING);
	}
}
