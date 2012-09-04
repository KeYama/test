package jp.co.johospace.jsphoto.managed;

import jp.co.johospace.jsphoto.AbstractActivity;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;

public class NavigatableActivity extends AbstractActivity {

	
	/**
	 * タブ毎にActivity遷移を進めます
	 * @param intent Activity開始に利用するIntent
	 */
	public void goNextHistory(Intent intent) {
		String id = intent.getComponent().getShortClassName();
		goNextHistory(id, intent);
	}
	
	/**
	 * タブ毎にActivity遷移を進めます
	 * @param id ActivityのId
	 * @param intent Activity開始に利用するIntent
	 */
	public void goNextHistory(String id, Intent intent) {
		
		NavigationGroup parent = (NavigationGroup)getParent();
		View view = parent.getLocalActivityManager()
				.startActivity(id, intent)
				.getDecorView();
		parent.replaceView(view);
	}
	
	/**
	 * タブ毎にActivity遷移を戻します
	 * 戻った先のActivityを再取得します
	 */
	public void onBackRefleshHistory() {
		NavigationGroup parent = (NavigationGroup)getParent();
		parent.doBackReflesh();
	}
	
	
	/**
	 * タブ毎にActivity遷移を戻します
	 */
	public void onBackHistory() {
		
		NavigationGroup parent = (NavigationGroup)getParent();
		parent.doBack();
	}
	
	/**
	 * タブ毎に遷移情報を初期化します
	 * 
	 * @param con	コンテキスト
	 */
	public void onClearHistory(Context con) {
		NavigationGroup parent = (NavigationGroup)getParent();
		parent.doClean(con);	
	}
	
	/**
	 * タブ毎に遷移情報を全て初期化します
	 */
	public void onAllClearHistory() {
		NavigationGroup parent = (NavigationGroup)getParent();
		parent.doAllClear();
	}
	
	/**
	 * Activityを置き換えます
	 */
	public void changeActivity(View v) {
		
		NavigationGroup parent = (NavigationGroup)getParent();
		parent.changView(v);
	}
	
	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		NavigationGroup parent = (NavigationGroup) getParent();
		if (parent == null) {
			super.startActivityForResult(intent, requestCode);
		} else {
			parent.startActivityForResult(intent, requestCode);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBackHistory();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
