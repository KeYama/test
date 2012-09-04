package jp.co.johospace.jsphoto.v2.onlineservice.jsmedia;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;

/**
 * メディアサーバ認証
 */
public class JsMediaAuth implements HttpRequestInitializer, HttpExecuteInterceptor {

	private static final HttpTransport mDefaultTransport = AndroidHttp.newCompatibleTransport();
	private final Context mContext;
	private final SharedPreferences mPref;
	private static final String KEY_DEVID = "devid";
	private static final String KEY_TOKEN = "token";
	public JsMediaAuth(Context context) {
		super();
		mContext = context.getApplicationContext();
		mPref = mContext.getSharedPreferences(
				getClass().getSimpleName(), Context.MODE_PRIVATE);
	}
	
	public void saveCredential(Credential credential) {
		saveCredential(credential.deviceId, credential.token);
	}
	
	public void saveCredential(String deviceId, String token) {
		mPref.edit()
			.putString(KEY_DEVID, deviceId)
			.putString(KEY_TOKEN, token).commit();
	}
	
	public Credential loadCredential() {
		String deviceId = mPref.getString(KEY_DEVID, null);
		String token = mPref.getString(KEY_TOKEN, null);
		if (deviceId != null && token != null) {
			return new Credential(deviceId, token);
		} else {
			return null;
		}
	}
	
	@Override
	public void intercept(HttpRequest request) throws IOException {
		Credential credential = loadCredential();
		if (credential != null) {
//			System.out.println(String.format("///////////////////// deviceId=%s, token=%s", credential.deviceId, credential.token));		/*$debug$*/
//			System.out.println(String.format("///////////////////// Authorization: %s", credential.generateAuthorizationHeader()));		/*$debug$*/
			request.getHeaders().setAuthorization(
					credential.generateAuthorizationHeader());
		}
	}

	@Override
	public void initialize(HttpRequest request) throws IOException {
		request.setInterceptor(this);
	}
	
	public HttpRequestFactory createAuthorizedRequestFactory() {
		return createAuthorizedRequestFactory(mDefaultTransport);
	}
	
	public HttpRequestFactory createAuthorizedRequestFactory(HttpTransport transport) {
		return transport.createRequestFactory(this);
	}
	
	public static class Credential {
		public final String deviceId;
		public final String token;
		public Credential(String deviceId, String token) {
			super();
			this.deviceId = Preconditions.checkNotNull(deviceId);
			this.token = Preconditions.checkNotNull(token);
		}
		
		public String generateAuthorizationHeader() {
			return String.format(
					"JorlleAuth devid=%s token=%s",
					deviceId, token);
		}
	}
}
