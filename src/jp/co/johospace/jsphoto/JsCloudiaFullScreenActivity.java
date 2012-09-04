package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.johospace.jsphoto.database.CMediaSync;
import jp.co.johospace.jsphoto.fullscreen.ImageSurfaceView;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.PicasaClient;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.PhotoEntry;
import jp.co.johospace.jsphoto.provider.JorlleSyncProvider;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.JsonUtil;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;

/**
 * クラウド管理アプリからの画像表示
 */
public class JsCloudiaFullScreenActivity extends FullScreenActivity {
	
	private static final Pattern mUriPattern =
			Pattern.compile(".+/user/(.+)/albumid/(.+)/photoid/([^/\\?]+).*");
	
	private PhotoEntry mEntry;
	private File mTempFile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (findViewById(R.id.iconHome) != null) {
			findViewById(R.id.iconHome).setVisibility(View.INVISIBLE);
		}
		
		Intent intent = getIntent();
		final Uri uri = intent.getData();
		if (uri != null) {
			
			new AsyncTask<Void, Void, File>() {

				@Override
				protected void onPreExecute() {
					showDialog(DIAROG_PROGRESS_VIEW_SYNCED_MEDIA);
				}
				
				@Override
				protected File doInBackground(Void... params) {
					try {
						// エントリのIDを分解
						String userId;
						String albumId;
						String photoId;
						Matcher matcher = mUriPattern.matcher(uri.toString());
						if (matcher.matches() && matcher.groupCount() == 3) {
							userId = matcher.group(1);
							albumId = matcher.group(2);
							photoId = matcher.group(3);
						} else {
							return null;
						}
						
						// FIXME メディアのIDからリアルなGoogleアカウントを得る
						// メディアのプライバシー設定によっては、メディアIDから実際のGoogleアカウントは得られないようだ
						// インデクシングデータにアカウントを含める必要がある
						HashSet<String> accounts = new HashSet<String>();
						Uri syncMediaUri = JorlleSyncProvider.getUriFor(
								getApplicationContext(), JorlleSyncProvider.PATH_MEDIASYNC);
						Cursor c = getContentResolver().query(syncMediaUri,
								new String[] {CMediaSync.SERVICE_ACCOUNT},
								CMediaSync.SERVICE_TYPE + " = ?" +
										" AND " + CMediaSync.MEDIA_ID + " = ?",
								new String[] {PicasaClient.SERVICE_TYPE, uri.toString()}, null);
						try {
							if (c.moveToFirst()) {
//								userId = c.getString(0);
								accounts.add(c.getString(0));
//							} else {
//								return null;
							}
						} finally {
							c.close();
						}
						
						// FIXME 現時点では端末設定のアカウントで可能な限り表示する
						AccountManager manager = AccountManager.get(getApplicationContext());
						Account[] devices = manager.getAccountsByType("com.google");
						for (Account a : devices) {
							accounts.add(a.name);
						}
						
						PicasaClient client = null;
						for (Iterator<String> itr = accounts.iterator(); itr.hasNext() && mEntry == null;) {
							String account = itr.next();
							client = new PicasaClient(getApplicationContext(), account/*userId*//* + "@gmail.com"*/);
							
							// エントリをダウンロード
							try {
								mEntry = client.getMedia(albumId, photoId);
							} catch (HttpResponseException e) {
								if (e.getResponse().getStatusCode() == HttpStatusCodes.STATUS_CODE_FORBIDDEN/*普通はForbidden*/
										|| e.getResponse().getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND/*プライバシー設定によってはNot Foundになるようだ*/) {
									// 再認可
									if (!client.reauthorize(true)) {
//										throw e;
//										e.printStackTrace();		/*$debug$*/
										continue;
									} else {
										try {
											mEntry = client.getMedia(albumId, photoId);
										} catch (HttpResponseException e2nd) {
											if (e2nd.getResponse().getStatusCode() == HttpStatusCodes.STATUS_CODE_FORBIDDEN
													|| e2nd.getResponse().getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
//												e2nd.printStackTrace();		/*$debug$*/
												continue;
											} else {
												throw e2nd;
											}
										}
									}
								} else {
									throw e;
								}
							}
						}
						if (mEntry == null) {
							return null;
						}
						
						// 本体ダウンロード一時ファイル
						mTempFile = new File(getExternalCacheDir(), getClass().getSimpleName());
						mTempFile.mkdirs();
						mTempFile = new File(mTempFile, "viewsynced");
						mTempFile.delete();
						
						// 本体ダウンロード
						InputStream in = client.openContent(mEntry.mediaGroup.content.url);
						IOUtil.copy(in, new FileOutputStream(mTempFile));
						return mTempFile;
						
//						String account = PicasaPrefsActivity.getPicasaAccount(this);
//						if (TextUtils.isEmpty(account)) {
//							finish();
//							return;
//						}
//						SQLiteDatabase db = OpenHelper.getDatabase();
//						LocalSyncStoreAccessor localStore =
//								new LocalSyncStoreAccessor(this, PicasaClient.SERVICE_TYPE, account);
//						LocalMedia media = localStore.queryLocalMediaByMediaID(db, uri.toString());
//						if (media != null) {
//							File path = new File(media.dirpath, media.name);
//							ImageLoaderFactory factory = createImageLoaderFactory();
//							setContentView(new ImageSurfaceView(
//									FullScreenActivity.this, factory,
//									Arrays.asList(new String[] {path.getAbsolutePath()}), 0));
//						}
					} catch (Exception e) {
//						e.printStackTrace();		/*$debug$*/
						return null;
					}
				}
				
				@Override
				protected void onPostExecute(File result) {
					removeDialog(DIAROG_PROGRESS_VIEW_SYNCED_MEDIA);
					if (result != null) {
						// 表示
						mTags = Arrays.asList(new String[] {result.getAbsolutePath()});
						mTagPos = 0;
						recreateSurfaceView(0);
					} else {
						Toast.makeText(getApplicationContext(),
								R.string.fullscreen_view_synced_failed, Toast.LENGTH_LONG).show();
						finish();
					}
				}
				
			}.execute();
		} else {
			Toast.makeText(getApplicationContext(),
					R.string.fullscreen_view_synced_failed, Toast.LENGTH_LONG).show();
			finish();
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mTempFile != null) {
			mTempFile.delete();
		}
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0, MENU_INFO, Menu.NONE, R.string.image_context_info);
		return true;
	}
	
	@Override
	protected void recreateSurfaceView(int pos) {
		if (mEntry != null) {
			FrameLayout frame = (FrameLayout)findViewById(R.id.flFullscreen);
			frame.removeAllViews();
			if(mSurfaceView != null)mSurfaceView.dispose();
			
			mTagPos = pos;
			
			frame.addView(
				mSurfaceView = new ImageSurfaceView(this, mFactory, mTags, mTagPos)
			);
			
			mSurfaceView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(mShowTop){
						hideTopbar();
						mHandler.removeCallbacksAndMessages(null);
					}else{
						showTopbar();
						mHandler.postDelayed(new Runnable() {
							
							@Override
							public void run() {
								hideTopbar();
							}
						}, TIME_TO_HIDE);
					}
				}
			});
			
			// 画面遷移時、最初にトップバーを表示
			showTopbar();
			mHandler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					hideTopbar();
				}
			}, TIME_TO_HIDE);
		}
	}
	
	@Override
	protected void showInfo() {
		String json = JsonUtil.toJson(mEntry);
		Intent intent = new Intent(this, PicasaMediaInfoActivity.class);
		intent.putExtra(PicasaMediaInfoActivity.EXTRA_ENTRY, json);
		startActivity(intent);
	}
	
	@Override
	protected Long getCurrentDate() {
		return null;
	}

}
