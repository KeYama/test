package jp.co.johospace.jsphoto.v2.onlineservice.jsmedia;

import jp.co.johospace.jsphoto.R;
import android.content.Context;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.Key;

/**
 * メディアサーバURL
 */
public class JsMediaUrl extends GenericUrl {

	/** サービスタイプ */
	@Key
	public String type;
	
	/** アカウント */
	@Key
	public String account;
	
	/** バージョン（開始） */
	@Key
	public Long from;
	
	/** バージョン（終了） */
	@Key
	public Long to;
	
	/** ディレクトリID */
	@Key
	public String dirId;
	
	/** メディアID */
	@Key
	public String mediaId;
	
	public JsMediaUrl() {
		super();
	}

	public JsMediaUrl(String encodedUrl) {
		super(encodedUrl);
	}
	
	public static JsMediaUrl root(Context context) {
		String root = context.getString(R.string.jsmedia_root);
		String api = context.getString(R.string.jsmedia_api_path);
		return new JsMediaUrl(root + api);
	}
	
	public static JsMediaUrl authPrefs(Context context) {
		return root(context).appendPath("certify", "authPrefs/");
	}
	
	public static JsMediaUrl startIndexing(Context context) {
		return root(context).appendPath("indexing", "startIndexing/");
	}
	
	public static JsMediaUrl startRecreation(Context context) {
		return root(context).appendPath("indexing", "startRecreation/");
	}
	
	public static JsMediaUrl currentMediaVersion(Context context) {
		return root(context).appendPath("refer", "currentMediaVersion/");
	}
	
	public static JsMediaUrl albums(Context context) {
		return root(context).appendPath("refer", "albumList/");
	}
	
	public static JsMediaUrl mediaList(Context context) {
		return root(context).appendPath("refer", "mediaList/");
	}
	
	public static JsMediaUrl searchRelative(Context context) {
		return root(context).appendPath("search", "searchRelative/");
	}
	
	public static JsMediaUrl memories(Context context) {
		return root(context).appendPath("search", "memories/");
	}
	
	public static JsMediaUrl cameraPaths(Context context) {
		return root(context).appendPath("cameraPaths/");
	}
	
	public static JsMediaUrl localMedia(Context context) {
		return root(context).appendPath("sync", "localMediaUpdate/");
	}
	
	public static JsMediaUrl mediaMetadata(Context context) {
		return root(context).appendPath("refer", "mediaMetadata/");
	}
	
	public static JsMediaUrl thumbnail(Context context) {
		return root(context).appendPath("refer", "thumbnail/");
	}
	
	public static JsMediaUrl media(Context context) {
		return root(context).appendPath("refer", "media/");
	}
	
	public static JsMediaUrl mediaUpdate(Context context) {
		return root(context).appendPath("sync", "mediaUpdate/");
	}
	
	public static JsMediaUrl mediaDelete(Context context) {
		return root(context).appendPath("sync", "mediaDelete/");
	}
	
	public static JsMediaUrl searchWord(Context context) {
		return root(context).appendPath("search", "searchWord/");
	}
	
	public static JsMediaUrl setupSync(Context context) {
		return root(context).appendPath("sync", "setupSync/");
	}
	
	public static JsMediaUrl externalCredentials(Context context) {
		return root(context).appendPath("certify", "externalCredentials/");
	}
	
	public static JsMediaUrl createAccount(Context context) {
		JsMediaUrl url = root(context).appendPath("certify", "createAccount/");
		url.set("secret", "lsdfasKJH;ij;XK>+LIjhasdLUIH>LK");
		return url;
	}
	
	public JsMediaUrl appendPath(String... pathParts) {
		for (String path : pathParts) {
			appendRawPath("/" + path);
		}
		return this;
	}
}
