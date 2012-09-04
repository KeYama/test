package jp.co.johospace.jsphoto.onlineservice;

import java.net.URLEncoder;

import jp.co.johospace.jsphoto.R;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

/**
 * クラウド管理Webの認証
 */
public class JsCloudiaWebAuthenticator extends ContextWrapper implements Authorizer {
	private static final String tag = JsCloudiaWebAuthenticator.class.getSimpleName();

	private static final String AUTH_PARAM_CALLBACK = "return_url";
	private static final String AUTH_PARAM_COOKIE = "cookie";
	private static final String AUTH_RETURN_TOKEN = "GTOKEN";
	private static final String AUTH_RETURN_ACCOUNT = "GACCOUNT";

	private static final String KEY_ACCOUNT = String.format("%s|%s",
			JsCloudiaWebAuthenticator.class.getName(), "a");
	private static final String KEY_TOKEN = String.format("%s|%s",
			JsCloudiaWebAuthenticator.class.getName(), "t");

	private final String mCallbackURI;

	public JsCloudiaWebAuthenticator(Context context, String callbackURI) {
		super(context);
		mCallbackURI = callbackURI;
	}

	@Override
	public void authorize(final AuthorizationHandler authHandler) {
		new Thread() {
			@Override
			public void run() {
				try {
					doAuthorize(authHandler);
				} catch (Exception e) {
//					Log.e(tag, "failed to authorize.", e);		/*$debug$*/
					authHandler.authorizationFinished(false);
				}
				doAuthorize(authHandler);
			}
		}.start();
	}

	protected void doAuthorize(final AuthorizationHandler authHandler) {
		// サーバの認証URLを表示
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(
				"%s?%s=%s&%s=%s", getString(R.string.jscloudia_auth_url),
				AUTH_PARAM_CALLBACK, URLEncoder.encode(mCallbackURI),
				AUTH_PARAM_COOKIE, "no")));
		authHandler.startInteraction(intent, new InteractionCallback() {
			@Override
			public void onInteractionResult(int resultCode, Intent data) {
				try {
					if (data.getData().toString().startsWith(mCallbackURI)) {
						if (resultCode == Activity.RESULT_OK) {
							String token = data.getData().getQueryParameter(
									AUTH_RETURN_TOKEN);
							String account = data.getData().getQueryParameter(
									AUTH_RETURN_ACCOUNT);
							saveCredentials(getBaseContext(), account, token);

							authHandler.authorizationFinished(true);
						} else {
							authHandler.authorizationFinished(false);
						}
					}

				} catch (Exception e) {
//					Log.e(tag, "failed to authorize.", e);		/*$debug$*/
					authHandler.authorizationFinished(false);
				}
			}
		});
	}

	public static void saveCredentials(Context context, String account,
			String token) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		pref.edit().putString(KEY_ACCOUNT, account).putString(KEY_TOKEN, token)
				.commit();
	}

	public static String getAccount(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return pref.getString(KEY_ACCOUNT, null);
	}

	public static String getToken(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return pref.getString(KEY_TOKEN, null);
	}

}
