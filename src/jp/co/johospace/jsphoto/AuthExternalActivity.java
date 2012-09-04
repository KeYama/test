package jp.co.johospace.jsphoto;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager.LayoutParams;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * 外部サービス認証アクティビティ
 */
public class AuthExternalActivity extends AbstractActivity {

	private WebView mWeb;
	private View mLoading;
	
	public static final String EXTRA_SERVICE =
			AuthExternalActivity.class.getSimpleName() + ".EXTRA_SERVICE";
	public static final String EXTRA_URL =
			AuthExternalActivity.class.getSimpleName() + ".EXTRA_URL";
	public static final String EXTRA_DEVID =
			AuthExternalActivity.class.getSimpleName() + ".EXTRA_DEVID";
	public static final String EXTRA_TOKEN =
			AuthExternalActivity.class.getSimpleName() + ".EXTRA_TOKEN";
	
	public static int RESULT_AUTH_FAILED = RESULT_FIRST_USER + 1;
	
	private static final int TOAST = 1;
	private static final int LOADING = 2;
	private static final int LOADED = 3;
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TOAST:
				break;
			case LOADING:
				setTitle(msg.obj.toString());
				mLoading.setVisibility(View.VISIBLE);
				break;
			case LOADED:
				mLoading.setVisibility(View.GONE);
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.auth_external);
		getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		
		final String url = getIntent().getStringExtra(EXTRA_URL);
		if (TextUtils.isEmpty(url)) {
			finish();
		}
		
		mWeb = (WebView) findViewById(R.id.webview);
		mLoading = findViewById(R.id.loading);
		
		mWeb.setVerticalScrollbarOverlay(true);
		mWeb.getSettings().setJavaScriptEnabled(true);
		mWeb.addJavascriptInterface(new JsInterface(), "jorlle");
		mWeb.setWebViewClient(new WebViewClient() {
			@Override
			public void onLoadResource(WebView view, String url) {
			}
			
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Message msg = mHandler.obtainMessage(LOADING);
				msg.obj = url;
				mHandler.sendMessage(msg);
			}
			
			@Override
			public void onPageFinished(WebView view, String url) {
				Message msg = mHandler.obtainMessage(LOADED);
				mHandler.sendMessage(msg);
			}
			
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				CookieSyncManager.createInstance(AuthExternalActivity.this);
				CookieManager.getInstance().removeAllCookie();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				mWeb.loadUrl(url);
			}
		}.execute();
	}
	
	class JsInterface {
		public void onAuthorized(String devid, String token) {
			Intent data = new Intent();
			data.putExtra(EXTRA_DEVID, devid);
			data.putExtra(EXTRA_TOKEN, token);
			data.putExtra(EXTRA_SERVICE, getIntent().getStringExtra(EXTRA_SERVICE));
			setResult(RESULT_OK, data);
			finish();
		}
		
		public void onAuthorizationFailed() {
			setResult(RESULT_AUTH_FAILED);
			Toast.makeText(AuthExternalActivity.this,
					R.string.error_failed_to_authorize, Toast.LENGTH_LONG).show();
			finish();
		}
	}
}
