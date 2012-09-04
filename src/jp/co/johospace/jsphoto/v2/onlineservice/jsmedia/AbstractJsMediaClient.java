package jp.co.johospace.jsphoto.v2.onlineservice.jsmedia;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.co.johospace.jsphoto.util.JsonUtil;
import android.content.Context;
import android.text.TextUtils;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.gson.reflect.TypeToken;

/**
 * メディアサーバにアクセスするクライアント基底
 */
public abstract class AbstractJsMediaClient {

	protected final Context mContext;
	protected final JsMediaAuth mAuth;
	protected static final HttpTransport mTransport = AndroidHttp.newCompatibleTransport();
	protected final HttpRequestFactory mFactory;
	
	protected AbstractJsMediaClient(Context context) {
		super();
		mContext = context;
		mAuth = new JsMediaAuth(context);
		mFactory = mAuth.createAuthorizedRequestFactory(mTransport);
	}
	
	public HttpResponse executeGet(JsMediaUrl url, int connectTimeout, int readTimeout, String etag) throws IOException {
		HttpRequest request = mFactory.buildGetRequest(url);
		request.setConnectTimeout(connectTimeout);
		request.setReadTimeout(readTimeout);
		if (etag != null) {
			request.getHeaders().setIfNoneMatch(etag);
		}
		return execute(request);
	}
	
	public HttpResponse executeGet(JsMediaUrl url) throws IOException {
		return executeGet(url, 20000, 20000, null);
	}
	
	protected HttpResponse execute(HttpRequest request) throws IOException {
//		System.out.println(request.getUrl());		/*$debug$*/
		Locale locale = Locale.getDefault();
		request.getHeaders().set("Accept-Language",
				String.format("%s-%s", locale.getLanguage(), locale.getCountry()));
//StopWatch sw = StopWatch.start();
//sw.lap();
		HttpResponse response;
		try {
			response = request.execute();
		} catch (HttpResponseException e) {
			response = e.getResponse();
			int status = response.getStatusCode();
			String message;
			try {
				message = String.format("%d %s - %s",
						status, response.getStatusMessage(), response.parseAsString());
			} catch (IOException ioe) {
//				ioe.printStackTrace();		/*$debug$*/
				message = ioe.getMessage();
			}
			throw e;
//			switch (status) {
//			case HttpURLConnection.HTTP_FORBIDDEN:
//				throw new AuthFailedException(message);
//			case HttpURLConnection.HTTP_INTERNAL_ERROR:
//				throw new InternalServerErrorException(message);
//			default:
//				throw new IOException(message);
//			}
		}
//Log.d("jsc", String.format("%dms,%sb", sw.lap(), response.getHeaders().getContentLength()));
		
		saveExtCredentials(response);
		return response;
	}
	
	private static String sExtCredentials;
	private static final Object sCredentialsLock = new Object();
	static void saveExtCredentials(HttpResponse response) throws IOException {
		List<?> header = (List<?>) response.getHeaders().get("X-JorlleExtAuth");
		if (header != null && !header.isEmpty()) {
			synchronized (sCredentialsLock) {
				sExtCredentials = URLDecoder.decode(header.get(0).toString(), "UTF-8");
//				System.out.println(String.format("///////////////////////// %s", sExtCredentials));		/*$debug$*/
			}
		}
	}
	
	public static Map<String, Map<String, String>> getExtCredentials() {
		HashMap<String, Map<String, String>> credentials =
				new HashMap<String, Map<String, String>>();
		
		String json;
		synchronized (sCredentialsLock) {
			json = sExtCredentials;
		}
		
		if (!TextUtils.isEmpty(json)) {
			Map<String, String> map = JsonUtil.fromJson(json,
					new TypeToken<Map<String, String>>() {}.getType());
			credentials.clear();
			for (String key : map.keySet()) {
				String credential = map.get(key);
				String[] splits = key.split("\t");
				String type = splits[0];
				String account = splits[1];
				
				if (!credentials.containsKey(type)) {
					credentials.put(type, new HashMap<String, String>());
				}
				credentials.get(type).put(account, credential);
			}
		}
		
		return credentials;
	}
	
	public static Map<String, String> getExtCredentials(String serviceType) {
		Map<String, String> credentials = getExtCredentials().get(serviceType);
		return credentials == null ? new HashMap<String, String>() : credentials;
	}
	
	public static String getExtCredential(String serviceType, String serviceAccount) {
		return getExtCredentials(serviceType).get(serviceAccount);
	}

}
