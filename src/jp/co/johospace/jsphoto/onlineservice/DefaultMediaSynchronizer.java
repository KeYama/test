package jp.co.johospace.jsphoto.onlineservice;

import java.io.IOException;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.util.JsonUtil;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * デフォルトメディア同期
 */
public class DefaultMediaSynchronizer extends MediaSynchronizer {
	private static final String tag = DefaultMediaSynchronizer.class.getSimpleName();
	
	public DefaultMediaSynchronizer(Context context) {
		super(context);
	}
	
	@Override
	protected void doDownload(SQLiteDatabase db, OnlineMediaServiceClient<? extends MediaDirectory, ? extends Media> client,
			String localDir, String remoteDirectoryID, BehaviorInConflict behavior) throws IOException {
		d("*START DOWNLOAD PROCESS");
		final LocalSyncStoreAccessor localStore =
				new LocalSyncStoreAccessor(this, client.getServiceType(), client.getServiceAccount());
		/*
		 * リモートのメディアの情報を全件問い合わせる
		 */
		d("request list of media at dirId[%s]", remoteDirectoryID);
		IOIterator<? extends Media> remotes = client.iterateMedia(remoteDirectoryID);
		try {
			/*
			 * リモートの状態を基準に、同期の下り方向を行う
			 */
			while (remotes.hasNext()) {
				final Media remote = remotes.next();
				d("**START PROCESS mediaId[%s]", remote.getMediaID());
				if (JorlleApplication.debuggable()) {
					d("%s", JsonUtil.toJson(remote));
				}
				
				// リモートIDを条件にローカルのメディアを取得
				LocalMedia local =
						localStore.queryLocalMediaByMediaID(
								db, remote.getMediaID());
				
				db.beginTransaction();
				try {
					
					if (local == null) {
						d("local media is NOT found. download as new media.");
						
						/*
						 * リモートメディアのIDに合致するローカルメディアがない
						 *  -> リモートを採用
						 */
						local =  localStore.insertLocalMedia(db, localDir, remote,
								client.openContent(remote), STATUS_PROCESSED_TOOK_REMOTE);
						local.syncCache = client.serializeMedia(remote);
						
						d("    saved as %s/%s", local.dirpath, local.name);
						
					} else {
						d("local media is found. - %s/%s status(%d)", local.dirpath, local.name, local.syncStatus);
						
						switch (local.syncStatus) {
						case STATUS_NOT_PROCESSED:
						case STATUS_WAITING_DOWNLOAD:
							break;
						default:
							d("process NOT necessary.");
							continue;
						}
						
						local.syncCache = client.serializeMedia(remote);
						if (local.contentExists()) {
							d("local file exists.");
							
							/*
							 * リモート、ローカルともに結び付けられたメディアが存在する
							 */
							if (remote.isDirtyRemotely(local.sync)) {
								d("remote media is dirty.");
								/*
								 * リモートメディアに変更がある
								 */
								if (local.isDirty()) {
									d("local is also dirty. CONFLICT at behavior[%s]", behavior);
									/*
									 * リモート、ローカルともに変更が発生している（競合）
									 *  -> 指示された競合時の振る舞いに応じて処理
									 */
									switch (behavior) {
									case KEEP_CONFLICT:	// 競合を放置
										local.syncStatus = STATUS_CONFLICT_UNRESOLVED;
										break;
										
									case TAKE_LATEST:	// より新しい変更を採用
										if (remote.isNewerEqual(local.getTimestamp())) {
											d("latest content is REMOTE. download and overwirite local.");
											/*
											 * リモートのほうが新しい
											 *  -> リモートを採用してローカルを上書き
											 */
											local.metadata = remote.getMetadata();
											local.sync = remote.toMediaSync();
											localStore.updateLocalMedia(db, local, client.openContent(remote), false);
											local.syncStatus = STATUS_PROCESSED_TOOK_REMOTE;
											
										} else {
											d("latest content is LOCAL. wait upload process.");
											/*
											 * ローカルのほうが新しい
											 *  -> ローカルは変更せず、同期の上り方向を待つ
											 */
											local.syncStatus = STATUS_WAITING_UPLOAD;
										}
										break;
										
									case TAKE_LOCAL:	// 無条件にローカルを採用
										// ローカルは変更せず、同期の上り方向を待つ
										local.syncStatus = STATUS_WAITING_UPLOAD;
										break;
										
									case TAKE_REMOTE:	// 無条件にリモートを採用
										// リモートを採用してローカルを上書き
										local.metadata = remote.getMetadata();
										local.sync = remote.toMediaSync();
										localStore.updateLocalMedia(db, local, client.openContent(remote), false);
										local.syncStatus = STATUS_PROCESSED_TOOK_REMOTE;
										break;
										
									default:
										local.syncStatus = STATUS_NOT_PROCESSED;
									}
								} else {
									d("local is NOT dirty. download and overwirite local.");
									/*
									 * リモートメディアに変更があり、ローカルに変更がない
									 *  -> リモートを採用してローカルを上書き
									 */
									local.metadata = remote.getMetadata();
									local.sync = remote.toMediaSync();
									localStore.updateLocalMedia(db, local, client.openContent(remote), false);
									local.syncStatus = STATUS_PROCESSED_TOOK_REMOTE;
								}
							} else {
								d("remote media is NOT dirty.");
								if (local.isDirty()) {
									d("local is dirty. wait upload process.");
									/*
									 * リモートに変更がなく、ローカルに変更がある
									 *  -> ローカルは変更せず、同期の上り方向を待つ
									 */
									local.syncStatus = STATUS_WAITING_UPLOAD;
								} else {
									d("local is also NOT dirty. nothing to do.");
									/*
									 * リモート、ローカルともに変更がない
									 *  -> 更新の必要がない
									 */
									local.syncStatus = STATUS_PROCESSED_NOT_NECESSARY;
								}
							}
						} else {
							d("local is already deleted.");
							/*
							 * ローカルにリモートとのリンクは残っているが、ローカルメディアが削除されている
							 */
							if (remote.isDirtyRemotely(local.sync)) {
								d("remote media is dirty. download remote and avoid to loss remote updates.");
								/*
								 * ローカルで削除されているが、リモートで変更が発生している
								 *  -> ローカルの削除を取り消して、リモートの最新状態に置き換える
								 */
								local.metadata = remote.getMetadata();
								local.sync = remote.toMediaSync();
								localStore.updateLocalMedia(db, local, client.openContent(remote), false);
								local.syncStatus = STATUS_PROCESSED_TOOK_REMOTE;
							} else {
								d("remote media is NOT dirty. nothing to loss. mark to delete.");
								/*
								 * ローカルで削除されていて、リモートにも変化がない
								 *  -> 未処理で放置し、最後に削除する
								 */
								local.syncStatus = STATUS_NOT_PROCESSED;
							}
						}
					}
					
					// ローカル状態を更新
					local.syncTime = System.currentTimeMillis();
					localStore.updateLocalMedia(db, local,
							local.syncStatus != STATUS_WAITING_UPLOAD);
					
					db.setTransactionSuccessful();
					
				} catch (Exception e) {
//					Log.e(tag, String.format("failed to syncD - %s", remote.toMediaSync().toValues()), e);		/*$debug$*/
					if (local != null) {
						local.syncStatus = STATUS_FAILED;
					}
					
				} finally {
					db.endTransaction();
				}
				
				if (local != null && local.syncStatus == STATUS_FAILED) {
					local.syncTime = System.currentTimeMillis();
					localStore.updateLocalMedia(db, local, false);
				}
				
			}
		} finally {
			remotes.terminate();
			d("*FINISH DOWNLOAD PROCESS");
		}
	}

	@Override
	protected void doUpload(SQLiteDatabase db, OnlineMediaServiceClient<? extends MediaDirectory, ? extends Media> client,
			String localDir, String remoteDirectoryID, BehaviorInConflict behavior) throws IOException {
		d("*START UPLOAD PROCESS");
		final LocalSyncStoreAccessor localStore =
				new LocalSyncStoreAccessor(this, client.getServiceType(), client.getServiceAccount());
		
		d("list up local media at %s.", localDir);
		/*
		 * ローカルメディアの情報を全件問い合わせる
		 */
		TerminatableIterator<LocalMedia> locals =
				localStore.queryLocalMediaAt(db, localDir);
		try {
			/*
			 * ローカルの状態をもとに、同期の上り方向を行う
			 */
			while (locals.hasNext()) {
				final LocalMedia local = locals.next();
				d("**START PROCESS [%s/%s]", local.dirpath, local.name);
				
				db.beginTransaction();
				try {
					if (local.sync == null) {
						d("no link with remote. start upload.");
						/*
						 * ローカルメディアにリモートとのリンクがない
						 *  -> ローカルを採用
						 */
						Media remote = client.insertMedia(remoteDirectoryID, local.name,
								local.openContent(), local.metadata);
						if (remote == null) {
//							Log.e(tag, "upload seems to FAIL. returned null entry.");		/*$debug$*/
							continue;
						}
						d("uploaded as mediaId[%s]", remote.getMediaID());
//						remote = client.getMedia(remote.toMediaSync());
						local.syncStatus = STATUS_PROCESSED_TOOK_LOCAL;
						local.syncCache = client.serializeMedia(remote);
						localStore.bindRemoteMedia(db, local, remote);
						
					} else {
						d("local has link with remote. status(%d)", local.syncStatus);
						switch (local.syncStatus) {
						case STATUS_NOT_PROCESSED:
						case STATUS_WAITING_UPLOAD:
							break;
						default:
							d("process NOT necessary.");
							continue;
						}
						
						Media remote;
						if (local.syncCache != null) {
							d("cache of remote data found. reuse.");
							remote = client.deserializeMedia(local.syncCache);
						} else {
//							remote = client.getMedia(local.sync);
//							if (remote != null) {
//								local.syncCache = client.serializeMedia(remote);
//							}
							// なぜか　Picasa　は、アルバムを移動したメディアはIDを指定すると返ってくる。
							remote = null;
							d("cache NOT found. handle as remote delete.");
						}
						
						if (remote == null) {
							d("linked remote media is deleted.");
							/*
							 * リモートで削除されている
							 */
							if (local.isDirty()) {
								d("local is dirty. upload local to avoid to loss local modification.");
								/*
								 * リモートで削除されているが、ローカルで変更されている
								 *  -> ローカルを採用して、リモートに復活
								 */
								remote = client.insertMedia(remoteDirectoryID, local.name,
										local.openContent(), local.metadata);
//								remote = client.getMedia(remote.toMediaSync());
								local.sync = remote.toMediaSync();
								local.syncStatus = STATUS_PROCESSED_TOOK_LOCAL;
							} else {
								d("local is NOT dirty. mark to delete.");
								/*
								 * リモートで削除されていて、ローカルでの変更もない
								 *  -> 未処理で放置し、最後に削除する
								 */
								local.syncStatus = STATUS_NOT_PROCESSED;
							}
						} else {
							d("linked remote data exists.");
							/*
							 * ローカルにもリモートにも存在する
							 */
							if (local.isDirty()) {
								d("local is dirty.");
								/*
								 * ローカルで変更されている
								 */
								if (remote.isDirtyRemotely(local.sync)) {
									d("remote is also dirty. CONFLICT at behavior[%s]", behavior);
									/*
									 * リモート、ローカルともに変更が発生している（競合）
									 *  -> 指示された競合時の振る舞いに応じて処理
									 */
									switch (behavior) {
									case KEEP_CONFLICT:	// 競合を放置
										local.syncStatus = STATUS_CONFLICT_UNRESOLVED;
										break;
										
									case TAKE_LATEST:	// より新しいほうを採用
										if (remote.isNewerEqual(local.getTimestamp())) {
											d("latest content is REMOTE. wait download process.");
											/*
											 * リモートのほうが新しい
											 *  -> ローカルは採用せず、同期の下り方向を待つ
											 */
											local.syncStatus = STATUS_WAITING_DOWNLOAD;
										} else {
											d("latest content is LOCAL. upload local and overwrite remote.");
											/*
											 * ローカルのほうが新しい
											 *  -> ローカルを採用して、リモートを上書き更新
											 */
											remote = client.updateMedia(local.sync,
													local.openContent(), local.metadata);
//											remote = client.getMedia(remote.toMediaSync());
											local.sync = remote.toMediaSync();
											local.syncStatus = STATUS_PROCESSED_TOOK_LOCAL;
										}
										break;
										
									case TAKE_LOCAL:	// 無条件にローカルを採用
										remote = client.updateMedia(local.sync,
												local.openContent(), local.metadata);
//										remote = client.getMedia(remote.toMediaSync());
										local.sync = remote.toMediaSync();
										local.syncStatus = STATUS_PROCESSED_TOOK_LOCAL;
										break;
										
									case TAKE_REMOTE:	// 無条件にリモートを採用
										local.syncStatus = STATUS_WAITING_DOWNLOAD;
										break;
										
									default:
										local.syncStatus = STATUS_NOT_PROCESSED;
									}
								} else {
									d("remote is NOT dirty. upload local and overwrite remote.");
									/*
									 * ローカルで変更されていて、リモートでは変化がない
									 *  -> ローカルを採用して、リモートを上書き更新
									 */
									remote = client.updateMedia(local.sync,
											local.openContent(), local.metadata);
//									remote = client.getMedia(remote.toMediaSync());
									local.sync = remote.toMediaSync();
									local.syncStatus = STATUS_PROCESSED_TOOK_LOCAL;
								}
							} else {
								d("local is NOT dirty.");
								if (remote.isDirtyRemotely(local.sync)) {
									d("remote is dirty. wait download process.");
									/*
									 * ローカルで変更はないが、リモートで変化がある
									 *  -> 同期の下り方向を待つ
									 */
									local.syncStatus = STATUS_WAITING_DOWNLOAD;
								} else {
									d("remote is also NOT dirty. nothing to do.");
									/*
									 * ローカル、リモートともに変化がない
									 *  -> 処理の必要なし
									 */
									local.syncStatus = STATUS_PROCESSED_NOT_NECESSARY;
								}
							}
						}
					}
					
					// ローカル状態を更新
					local.syncTime = System.currentTimeMillis();
					localStore.updateLocalMedia(db, local,
							local.syncStatus != STATUS_WAITING_DOWNLOAD);
					
					db.setTransactionSuccessful();
				} catch (Exception e) {
//					Log.e(tag, String.format("failed to syncU - %s/%s", local.dirpath, local.name), e);		/*$debug$*/
					local.syncStatus = STATUS_FAILED;
					
				} finally {
					db.endTransaction();
				}
				
				if (local.syncStatus == STATUS_FAILED) {
					local.syncTime = System.currentTimeMillis();
					localStore.updateLocalMedia(db, local, false);
				}
				
			}
		} finally {
			locals.terminate();
			d("*FINISH UPLOAD PROCESS");
		}
	}
}
