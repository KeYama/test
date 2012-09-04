package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.CMediaSync;
import jp.co.johospace.jsphoto.provider.JorlleProvider;
import jp.co.johospace.jsphoto.provider.JorlleSyncProvider;
import android.app.Application;
import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;

/**
 * アプリケーション
 */
public class JorlleApplication extends Application {

	private static JorlleApplication sInstance;
	
	public static JorlleApplication instance() {
		return sInstance;
	}
	
	private static boolean debuggable;
	public static boolean debuggable() {
		return debuggable;
	}

	public JorlleApplication() {
		sInstance = this;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		ApplicationInfo app = getApplicationInfo();
		if (app == null) {
			debuggable = false;
		} else {
			debuggable = (app.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE;
		}
		
		ContentResolver cr = getContentResolver();
		// JorlleProviderに移動
//		try {
//			OpenHelper.external.initialize();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		cr.getType(JorlleProvider.getUriFor(getApplicationContext(), new String[] {CMediaMetadata.$TABLE}));

		// JorlleSyncProviderに移動
//		try {
//			OpenHelper.sync.initialize();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		cr.getType(JorlleSyncProvider.getUriFor(getApplicationContext(), CMediaSync.$TABLE));
	}
}
