package jp.co.johospace.jsphoto.managed;

import jp.co.johospace.jsphoto.TagListActivity;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class TagNavigationGroup extends NavigationGroup {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Intent topIntent = getIntent();
		String top = topIntent.getStringExtra(ApplicationDefine.TAB_TAG);
		if (top != null && top.equals(ApplicationDefine.TAB_TOP)) {
			mHistory.clear();
			
			// TagListActivityを最初に呼び出し管理する
			Intent intent = new Intent().setClass(this, TagListActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			
			View view = getLocalActivityManager()
					.startActivity("TabListActivity", intent)
					.getDecorView();
			replaceView(view);
		}
		
	}
	
}
