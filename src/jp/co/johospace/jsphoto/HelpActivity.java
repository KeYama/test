package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.util.AppUtil;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpActivity extends AbstractActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		
		// 初期化
		init();
	}
	
	/**
	 * 初期化
	 */
	private void init() {
		WebView help = (WebView)findViewById(R.id.webHtmlView);
		help.loadDataWithBaseURL("file:///android_asset/html/", AppUtil.getTextFromAsset(getAssets(), "help/" + AppUtil.getHelpHtmlName(this)), "text/html", "utf-8", null);
	}
}
