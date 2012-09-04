package jp.co.johospace.jsphoto.onlineservice.jscloudia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.CMediaSync;
import jp.co.johospace.jsphoto.database.CMediaSyncToSend;
import jp.co.johospace.jsphoto.onlineservice.JsCloudiaWebAuthenticator;
import jp.co.johospace.jsphoto.onlineservice.LocalSyncStoreAccessor;
import jp.co.johospace.jsphoto.onlineservice.TerminatableIterator;
import jp.co.johospace.jsphoto.onlineservice.jscloudia.model.ReceiveProcessedMedia;
import jp.co.johospace.jsphoto.onlineservice.jscloudia.model.SendHeader;
import jp.co.johospace.jsphoto.onlineservice.jscloudia.model.SendMediaIndex;
import jp.co.johospace.jsphoto.onlineservice.jscloudia.model.SendMediaMetadata;
import jp.co.johospace.jsphoto.provider.JorlleProvider;
import jp.co.johospace.jsphoto.util.JsonUtil;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;

/**
 * クラウド管理サーバクライアント
 */
public class JsCloudiaClient extends ContextWrapper implements HttpRequestInitializer, HttpExecuteInterceptor {
	protected final String tag = getClass().getSimpleName();
	
	private static final HttpTransport mTransport =
			AndroidHttp.newCompatibleTransport();
	
	public JsCloudiaClient(Context context) {
		super(context);
	}
	
	public void sendDirties(SQLiteDatabase db, String serviceType, String serviceAccount) throws IOException {
		d("*START SEND TO CLOUDIA.");
		File dir = getDir(JsCloudiaClient.class.getName(), MODE_PRIVATE);
		dir.mkdirs();
		File temporary = new File(dir, "senddata");
		try {
			// JSONシリアライズしたファイルリストのレコードを、テンポラリファイルに行区切りで詰め込む
			PrintWriter writer = new PrintWriter(temporary, "UTF-8");
			try {
				TerminatableIterator<SendMediaIndex> itr =
						queryDirties(db, serviceType, serviceAccount);
				try {
					if (!itr.hasNext()) {
						return;
					}
					
					while (itr.hasNext()) {
						String json = JsonUtil.toJson(itr.next());
						writer.println(json);
						d("** send data - %s", json);
					}
				} finally {
					itr.terminate();
				}
			} finally {
				writer.close();
			}
			
			// ファイルリストを詰め込んだファイルをコンテンツとする
			FileContent content = new FileContent("text/plane", temporary);
			
			// 送信
			GenericUrl url = new GenericUrl(getString(R.string.jscloudia_post_url));
			HttpRequest request =
					mTransport.createRequestFactory(this).buildPostRequest(url, content);
			HttpResponse response = request.execute();
			
			// サーバサイドで受理され正常処理されたものが返ってくる
			// それらは、送信済みとして端末のプールから削除する
			LocalSyncStoreAccessor accessor =
					new LocalSyncStoreAccessor(this, serviceType, serviceAccount);
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(response.getContent(), "UTF-8"));
			try {
				String json;
				while((json = reader.readLine()) != null) {
					if (0 < json.trim().length()) {
						ReceiveProcessedMedia processed =
								JsonUtil.fromJson(json, ReceiveProcessedMedia.class);
						accessor.deleteDirtiesToSend(db, processed.mediaId);
						d("** received and processed data - %s", json);
					}
				}
			} finally {
				reader.close();
			}
			
		} finally {
			temporary.delete();
			d("*FINISH SEND TO CLOUDIA.");
		}
	}
	
	private TerminatableIterator<SendMediaIndex> queryDirties(final SQLiteDatabase db,
			final String serviceType, final String serviceAccount) {
		// 変更された同期データをプールしたテーブルを元に差分を抽出する
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT  " + CMediaSyncToSend.$TABLE + ".*, ");
		sql.append("        " + CMediaSync.$TABLE + ".* ");
		sql.append("FROM    " + CMediaSyncToSend.$TABLE + " ");
		sql.append("          LEFT JOIN " + CMediaSync.$TABLE + " ON ( ");
		sql.append("                " + CMediaSyncToSend.SERVICE_TYPE_TO_SEND + "     = " + CMediaSync.SERVICE_TYPE + " ");
		sql.append("            AND " + CMediaSyncToSend.SERVICE_ACCOUNT_TO_SEND + "  = " + CMediaSync.SERVICE_ACCOUNT + " ");
		sql.append("            AND " + CMediaSyncToSend.MEDIA_ID_TO_SEND + "         = " + CMediaSync.MEDIA_ID + " ");
		sql.append("          ) ");
		sql.append("WHERE   " + CMediaSyncToSend.SERVICE_TYPE_TO_SEND + "     = ? ");
		sql.append("    AND " + CMediaSyncToSend.SERVICE_ACCOUNT_TO_SEND + "  = ? ");
		
		final Cursor c = db.rawQuery(sql.toString(),
				new String[] {serviceType, serviceAccount});
		return new TerminatableIterator<SendMediaIndex>() {

			final int INDEX_DIRPATH = c.getColumnIndex(CMediaSync.DIRPATH);
			final int INDEX_NAME = c.getColumnIndex(CMediaSync.NAME);
			final int INDEX_MEDIA_ID = c.getColumnIndex(CMediaSyncToSend.MEDIA_ID_TO_SEND);
			final int INDEX_DIRECTORY_ID = c.getColumnIndex(CMediaSync.DIRECTORY_ID);
			final int INDEX_MEDIA_URI = c.getColumnIndex(CMediaSync.MEDIA_URI);
			final int INDEX_PRODUCTION_DATE = c.getColumnIndex(CMediaSync.PRODUCTION_DATE);
			final int INDEX_OPERATION = c.getColumnIndex(CMediaSyncToSend.OPERATION_TO_SEND);
			
			{
				prepareNext();
			}
			
			SendMediaIndex mNext;
			void prepareNext() {
				if (c.moveToNext()) {
					mNext = new SendMediaIndex();
					mNext.serviceType = serviceType;
					mNext.serviceAccount = serviceAccount;
					mNext.mediaId = c.getString(INDEX_MEDIA_ID);
					mNext.directoryId = c.getString(INDEX_DIRECTORY_ID);
					mNext.mediaUri = c.getString(INDEX_MEDIA_URI);
					mNext.fileName = c.getString(INDEX_NAME);
					mNext.productionDate = c.getLong(INDEX_PRODUCTION_DATE);
					mNext.operation = c.getString(INDEX_OPERATION);
					mNext.mediaMetadataList = new ArrayList<SendMediaMetadata>();
					if (!"DEL".equals(mNext.operation)) {
//						Cursor cMetadata = db.query(CMediaMetadata.$TABLE,
//								new String[] {CMediaMetadata.METADATA_TYPE, CMediaMetadata.METADATA},
//								CMediaMetadata.DIRPATH + " = ?" +
//										" AND " + CMediaMetadata.NAME + " = ?",
//								new String[] {c.getString(INDEX_DIRPATH), c.getString(INDEX_NAME)}, null, null, null);
						Cursor cMetadata = getContentResolver().query(
								JorlleProvider.getUriFor(getApplicationContext(), new String[] {CMediaMetadata.$TABLE}),
								new String[] {CMediaMetadata.METADATA_TYPE, CMediaMetadata.METADATA},
								CMediaMetadata.DIRPATH + " = ?" +
										" AND " + CMediaMetadata.NAME + " = ?",
								new String[] {c.getString(INDEX_DIRPATH), c.getString(INDEX_NAME)}, null);
						try {
							while (cMetadata.moveToNext()) {
								SendMediaMetadata metadata = new SendMediaMetadata();
								metadata.metadataType = cMetadata.getString(0);
								metadata.metadata = cMetadata.getString(1);
								mNext.mediaMetadataList.add(metadata);
							}
						} finally {
							cMetadata.close();
						}
					}
				} else {
					mNext = null;
				}
			}
			
			@Override
			public boolean hasNext() {
				return mNext != null;
			}

			@Override
			public SendMediaIndex next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				
				try {
					return mNext;
				} finally {
					prepareNext();
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void terminate() throws IOException {
				c.close();
			}
		};
	}

	@Override
	public void intercept(HttpRequest request) throws IOException {
		SendHeader header = new SendHeader();
		header.token = JsCloudiaWebAuthenticator.getToken(this);
		request.getHeaders().set("JS-Header", JsonUtil.toJson(header));
	}

	@Override
	public void initialize(HttpRequest request) throws IOException {
		request.setInterceptor(this);
	}
	
	protected void d(String format, Object... args) {		/*$debug$*/
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			Log.d(tag, String.format(format, args));		/*$debug$*/
		}		/*$debug$*/
	}		/*$debug$*/
	
	protected void d(String format, Throwable t, Object... args) {		/*$debug$*/
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			Log.d(tag, String.format(format, args), t);		/*$debug$*/
		}		/*$debug$*/
	}		/*$debug$*/
}
