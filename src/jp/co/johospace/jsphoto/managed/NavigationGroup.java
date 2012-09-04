package jp.co.johospace.jsphoto.managed;

import java.util.ArrayList;

import jp.co.johospace.jsphoto.util.DefaultExceptionHandler;
import jp.co.johospace.jsphoto.util.ExceptionHandler;
import android.app.Activity;
import android.app.ActivityGroup;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

public class NavigationGroup extends ActivityGroup implements ExceptionHandler {

	/** Viewの履歴を保持 */
	public final ArrayList<View> mHistory = new ArrayList<View>();

	private ExceptionHandler mExceptionHandler;
	
	public NavigationGroup() {
		super();
	}

	public NavigationGroup(boolean singleActivityMode) {
		super(singleActivityMode);
	}

	/**
	 * 同じ階層のActivityを入れ替えます
	 * @param v View
	 */
	public void changView(View v) {
		
		mHistory.remove(mHistory.size() - 1);
		mHistory.add(v);
		setContentView(v);
		
	}

	/**
	 * 新しい階層のActivityを追加します
	 * @param v View
	 */
	public void replaceView(View v) {
		
		mHistory.add(v);
		setContentView(v);
	}

	/**
	 * 一つ前のActivityを表示
	 */
	public void doBack() {
		
		// 履歴がある場合
		if (mHistory.size() > 1) {
			mHistory.remove(mHistory.size() - 1);
			setContentView(mHistory.get(mHistory.size() - 1));
			
		// 最上位ではアプリ終了
		} else {
			finish();
		}
		
	}
	
	/**
	 * トップ画面以下の階層を消去
	 * 
	 * @param con	コンテキスト
	 */
	public void doClean(Context con){
		
		// TOP以下の階層がある場合は削除
		if (mHistory.size() > 1) {
			//TODO 本来は、トップ以下の階層を全て削除する必要あり（現在は、NewGridActivityのみを削除）
			mHistory.remove(mHistory.size() - 1);

			//TODO dummyのviewをセットすることで、NewGridActivityのonDestroyを走らせる
			View v = new View(con);
			setContentView(v);
		}
	}
	
	/**
	 * 遷移履歴をすべて削除します
	 */
	public void doAllClear() {
		mHistory.clear();
	}
	
	/**
	 * 一つ前のActivityを更新して表示(再取得)
	 */
	public void doBackReflesh() {
		if (mHistory.size() > 1) {
			// カレントのActivityを削除
			mHistory.remove(mHistory.size() - 1);
			// 呼び出したいActivityのIntentを抜き出す
			View refleshView = mHistory.get(mHistory.size() - 1);
			Activity activity = (Activity) refleshView.getContext();
			Intent intent = activity.getIntent();
			refleshView = getLocalActivityManager().startActivity(activity.getLocalClassName(), intent).getDecorView();
			setContentView(refleshView);
		}
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mExceptionHandler = new DefaultExceptionHandler(this);
	}

	/* 
	 * NavigationGroupでイベントを取得
	 * 画面に表示されている子Viewへイベントを渡す
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Activity current = getLocalActivityManager().getCurrentActivity();
		if (current != null) {
			return current.onKeyDown(keyCode, event);
		} else {
			finish();
			return true;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Activity current = getLocalActivityManager().getCurrentActivity();
		if (current instanceof NavigatableActivity) {
			((NavigatableActivity) current).onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean handleException(Throwable t, boolean isUserAction) {
		return mExceptionHandler.handleException(t, isUserAction);
	}

	@Override
	public boolean assertException(NetErrors error, Throwable t,
			boolean isUserAction) {
		return mExceptionHandler.assertException(error, t, isUserAction);
	}
}