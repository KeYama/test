package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * タグチュートリアル画面アクティビティです
 */
public class TutorialTagActivity extends NavigatableActivity {

	/** オプションメニュー 設定 */
	private static final int MENU_ITEM_SETTING = 1;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.tutorial_tag);

		findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setTagDisplayState();
				
				onAllClearHistory();
				
				PreferenceUtil.setBooleanPreferenceValue(TutorialTagActivity.this, TagListActivity.KEY_NOT_FIRST_TAG_TIME, false);
				
				// タグ一覧に再遷移
				Intent intent = new Intent(TutorialTagActivity.this, TagListActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				goNextHistory("TagListActivity", intent);
			}

		});
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			
			setTagDisplayState();
			PreferenceUtil.setBooleanPreferenceValue(TutorialTagActivity.this, TagListActivity.KEY_NOT_FIRST_TAG_TIME, false);
			
			onAllClearHistory();
			
			// タグ一覧に再遷移
			Intent intent = new Intent(TutorialTagActivity.this, TagListActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			goNextHistory("TagListActivity", intent);
			
			return true;
		// メニューボタン押下
		} else if (keyCode == KeyEvent.KEYCODE_MENU) {
			openOptionsMenu();
			return true;
		}
	
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * タグ一覧画面　オーバーレイの表示状態を全表示にします
	 */
	private void setTagDisplayState() {
		PreferenceUtil.setBooleanPreferenceValue(this, ApplicationDefine.KEY_TAG_CATEGORY_FAVORITE, true);
		PreferenceUtil.setBooleanPreferenceValue(this, ApplicationDefine.KEY_TAG_CATEGORY_SECRET, true);
		PreferenceUtil.setBooleanPreferenceValue(this, ApplicationDefine.KEY_TAG_CATEGORY_TAG, true);
	}
	
	/*
	 * オプションメニュー作成
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		menu.add(0, MENU_ITEM_SETTING, 0, getResources().getString(R.string.menu_setting)).setIcon(R.drawable.ic_setting);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	/**
	 * メニュー選択イベント
	 * @param item
	 * @return true:成功、false:失敗
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			// 設定
			case MENU_ITEM_SETTING:
				Intent settingIntent = new Intent(TutorialTagActivity.this, JorllePrefsActivity.class);
				startActivity(settingIntent);
				break;
		}

		return true;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		View view = getWindow().getDecorView();
		changeActivity(view);
	}
}
