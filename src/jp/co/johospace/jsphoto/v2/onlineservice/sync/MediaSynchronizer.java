package jp.co.johospace.jsphoto.v2.onlineservice.sync;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.util.TerminatableIterator;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.SyncPreference;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.db.LocalSyncStoreAccessor;
import jp.co.johospace.jsphoto.v2.onlineservice.sync.model.LocalMedia;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import com.google.api.client.http.HttpResponseException;

/**
 * メディア同期
 */
public class MediaSynchronizer extends ContextWrapper {
	private static final String tag = MediaSynchronizer.class.getSimpleName();
	
	protected final JsMediaServerClient mJsMedia;
	protected final SQLiteDatabase mDb;
	public MediaSynchronizer(Context context) {
		super(context);
		mJsMedia = ClientManager.getJsMediaServerClient(context);
		mDb = OpenHelper.sync.getDatabase();
	}
	
	private static final int STATUS_ERROR = -1;
	private static final int STATUS_NOT_PROCESSED = 0;
	private static final int STATUS_TAKE_LOCAL = 1;
	private static final int STATUS_LOCAL_NOT_DIRTY = 2;
	private static final int STATUS_TAKE_REMOTE = 3;
	
	public void synchronize(String serviceType, String serviceAccount, String localDir, NetworkTransferListener listener) throws IOException {
		if (mCancelRequested) {
			d(">>>>>>> sync canceled.");
			return;
		}
		
		// ローカルストア
		LocalSyncStoreAccessor localStore =
				new LocalSyncStoreAccessor(this, serviceType, serviceAccount);
		// 外部サービスクライアント
		ExternalServiceClient client =
				ClientManager.getExternalServiceClient(this, serviceType);
		
		// 同期設定を取得
		SyncPreference pref;
		try {
			pref = mJsMedia.setupSync(serviceType, serviceAccount);
		} catch (Exception e) {
			pref = mJsMedia.setupSync(serviceType, serviceAccount);
		}
		
		// 外部サービスcredentialを取得
		client.setAuthCredentials(
				mJsMedia.getExternalServiceCredentials(serviceType, false));

		mDb.beginTransaction();
		try {
			// ローカルストアをクリア
			localStore.clearSyncStatus(mDb, localDir, STATUS_NOT_PROCESSED);
			// メタデータの汚れを反映
			localStore.applyMetadataDirty(mDb, localDir);
			mDb.setTransactionSuccessful();
		} finally {
			mDb.endTransaction();
		}
		
		/*
		 * ローカルの差分を送信
		 */
		ArrayList<Pair<String, String>> fails = new ArrayList<Pair<String, String>>();
		// 新規・更新分
		TerminatableIterator<LocalMedia> itrLocal =
				localStore.queryLocalMediaAt(mDb, localDir);
		try {
			while (itrLocal.hasNext()) {
				if (mCancelRequested) {
					d(">>>>>>> sync canceled.");
					return;
				}
				
				LocalMedia local = itrLocal.next();
				mDb.beginTransaction();
				try {
					if (local.sync == null) {
						// ローカル新規
						d("* LOCAL NEW - %s", local.name);
						if (listener != null) {
							listener.onUpload(false);
						}
						Media remote = client.insertMedia(serviceAccount,
								pref.protectedDirectory.id, local.openContent(), local.name, local.metadata);
						d("  remote - %s", remote.mediaId);
						local.syncStatus = STATUS_TAKE_LOCAL;
						try{
							localStore.bindRemoteMedia(mDb, local, remote);
						}catch(IOException e){
							//補償を実行
							//失敗すればファイルが複製されるが、厳密に実装すると
							//二相コミットが必要になりコスト的に現実的ではない。
							client.deleteMedia(serviceAccount, remote.mediaId);
							throw e;
						}catch(RuntimeException e){
							client.deleteMedia(serviceAccount, remote.mediaId);
							throw e;	
						}
					} else {
						if (local.isDirty()) {
							// ローカル変更
							d("* LOCAL MOD - %s", local.name);
							if (listener != null) {
								listener.onUpload(false);
							}
							try {
								local.sync = client.updateMedia(serviceAccount,
										local.sync.mediaId, local.openContent(), local.sync.fileName, local.metadata);
								d("  remote - %s", local.sync.mediaId);
								local.syncStatus = STATUS_TAKE_LOCAL;
								localStore.updateLocalMedia(mDb, local, true, true);
							} catch (HttpResponseException e) {
								if (e.getResponse().getStatusCode() == HttpURLConnection.HTTP_PRECON_FAILED) {
									d("  REMOTE IS NEWER THAN LOCAL.");
									local.syncStatus = STATUS_TAKE_REMOTE;
									localStore.updateLocalMedia(mDb, local, true, false);
								} else {
									throw e;
								}
							}
						} else {
							// ローカルに変更なし
							d("* LOCAL KEEP - %s", local.name);
							local.syncStatus = STATUS_LOCAL_NOT_DIRTY;
							localStore.updateLocalMedia(mDb, local, true, false);
						}
					}
					
					mDb.setTransactionSuccessful();
				} catch (Exception e) {
					fails.add(new Pair<String, String>(local.dirpath, local.name));
					d("  FAILED ", e);
					;
				} finally {
					mDb.endTransaction();
				}
			}
		} finally {
			itrLocal.terminate();
		}
		
		// 削除分
		itrLocal = localStore.queryLocalMediaAt(mDb, localDir, STATUS_NOT_PROCESSED);
		try {
			while (itrLocal.hasNext()){
				LocalMedia local = itrLocal.next();
				if (fails.contains(new Pair<String, String>(local.dirpath, local.name))) {
					continue;
				}
				mDb.beginTransaction();
				try {
					d("* LOCAL DEL - %s", local.name);
					if (listener != null) {
						listener.onUpload(true);
					}
					
					try {
						client.deleteMedia(serviceAccount, local.sync.mediaId);
						localStore.deleteLocalMedia(mDb, local.dirpath, local.name);
						mDb.setTransactionSuccessful();
					} catch (HttpResponseException e) {
						if (e.getResponse().getStatusCode() == HttpURLConnection.HTTP_PRECON_FAILED) {
							d("    REMOTE IS NEWER THAN LOCAL.");
							mDb.setTransactionSuccessful();
						} else {
							throw e;
						}
					}
				} catch (Exception e) {
					d("  FAILED ", e);
					;
				} finally {
					mDb.endTransaction();
				}
			}
		} finally {
			itrLocal.terminate();
		}
		
		
		// 前回のバージョンを取得
		Long prevVersion = localStore.getSyncedVersion(mDb);
		d("* PREV VER - %s", prevVersion);
		
		// 現在のリモートバージョンを取得
		Long currentVersion =
				mJsMedia.getCurrentMediaVersion(serviceType, serviceAccount);
		d("* CURRENT VER - %s", currentVersion);
		
		// リモートの差分を取得
		Long from =
				prevVersion == null ? null : prevVersion + 1;
		List<Media> remoteUpdates =
				mJsMedia.getMediaList(serviceType, serviceAccount,
						pref.protectedDirectory.id, true, from, currentVersion);
		
		// ローカルを更新
		boolean canceled = false;
		Long processedVersion = null;
		String[] contentType = new String[1];
		for (Media remote : remoteUpdates) {
			if (mCancelRequested) {
				d(">>>>>>> sync canceled.");
				canceled = true;
				break;
			}
			
			mDb.beginTransaction();
			try {
				if (remote.deleted) {
					// リモートで削除
					d("* REMOTE DEL - %s", remote.mediaId);
					if (listener != null) {
						listener.onDownload(true);
					}
					localStore.deleteLocalMedia(mDb, remote.mediaId);
				} else {
					LocalMedia local = localStore.getLocalMedia(mDb, remote.mediaId);
					if (local == null) {
						// リモートで新規
						d("* REMOTE NEW - %s", remote.mediaId);
						if (listener != null) {
							listener.onDownload(false);
						}
						InputStream content = client.getMediaContent(remote, contentType);
						localStore.insertLocalMedia(mDb, localDir, remote, content, STATUS_TAKE_REMOTE);
					} else {
						// リモートで更新
						d("* REMOTE MOD - %s", remote.mediaId);
						if (!remote.version.equals(local.sync.version)) {
							if (listener != null) {
								listener.onDownload(false);
							}
							local.sync = remote;
							local.metadata = remote.metadata;
							InputStream content = client.getMediaContent(remote, contentType);
							localStore.updateLocalMedia(mDb, local, content, true);
						} else {
							d("  ignore. this is one that i did before.");
						}
					}
				}
				
				mDb.setTransactionSuccessful();
				processedVersion = remote.version;
			} catch (Exception e) {
				d("  FAILED ", e);
				;
			} finally {
				mDb.endTransaction();
			}
		}
		
		// リモートバージョンを保存
		if (!canceled) {
			processedVersion = currentVersion;
		}
		if (processedVersion != null) {
			localStore.saveSyncedVersion(mDb, processedVersion);
		}
	}
	
	private boolean mCancelRequested;
	public void requestCancel() {
		d(">>>>>>> cancel requested.");
		mCancelRequested = true;
	}
	
	/** 同期プロセスでのネットワーク転送をリスンします */
	public static interface NetworkTransferListener {
		/**
		 * これからアップロードします。
		 * @param delete 削除要求をアップロードする場合true
		 */
		void onUpload(boolean delete);
		
		/**
		 * これからダウンロードします。
		 * @param delete リモートからの削除要求を処理する場合true
		 */
		void onDownload(boolean delete);
	}
	
	
	
	
	
	protected void d(String format, Object... args) {
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			try {		/*$debug$*/
				Log.d(tag, String.format(format, args));		/*$debug$*/
			} catch (RuntimeException e) {		/*$debug$*/
				Log.d(tag, "format:" + format + " args:" + Arrays.toString(args));		/*$debug$*/
			}		/*$debug$*/
		}		/*$debug$*/
	}
	
	protected void d(String format, Throwable t, Object... args) {
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			try {		/*$debug$*/
				Log.d(tag, String.format(format, args), t);		/*$debug$*/
			} catch (RuntimeException e) {		/*$debug$*/
				e.printStackTrace();		/*$debug$*/
				Log.d(tag, "format:" + format + " args:" + Arrays.toString(args), t);		/*$debug$*/
			}		/*$debug$*/
		}		/*$debug$*/
	}
}
