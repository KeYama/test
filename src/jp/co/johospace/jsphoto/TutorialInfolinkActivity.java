package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

/**
 * インフォリンクチュートリアル画面アクティビティです
 */
public class TutorialInfolinkActivity extends AbstractActivity implements OnClickListener {

	public static final String KEY_NOT_FIRST_SEARCH_TIME = "notFirstSearchTime";
	
	private static final int REQUEST_ONLINE_SETUP = 1;
	private static final int REQUEST_ONLINE_SETUP_SCHEDULE = 2;
	
	private ImageButton mBtnClose;
	private Button mBtnOnline;
	private Button mBtnAutoalbum;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.tutorial_infolink);

		mBtnClose = (ImageButton)findViewById(R.id.btnClose);
		mBtnClose.setOnClickListener(this);

		mBtnOnline = (Button)findViewById(R.id.btnOnline);
		mBtnOnline.setOnClickListener(this);

		mBtnAutoalbum = (Button)findViewById(R.id.btnAutoalbum);
		mBtnAutoalbum.setOnClickListener(this);

	}

	@Override
	public void onClick(View view) {

		// 閉じるボタン
		if (view.getId() == R.id.btnClose){

			// 自画面を閉じてオンラインタブへ切替
			setResult(ApplicationDefine.RESULT_TUTORIALINFOLINK_SEARCH);

			//次回からこの画面を表示しないように登録
			PreferenceUtil.setBooleanPreferenceValue(this, KEY_NOT_FIRST_SEARCH_TIME, false);

			finish();

		// オンラインボタン
		} else if (view.getId() == R.id.btnOnline){

			// メディアサービスの設定
//			Intent intent = new Intent(this, OnlineSetupActivity.class);
//			intent.putExtra(OnlineSetupActivity.EXTRA_BACK_MODE, OnlineSetupActivity.BACK_MODE_FINISH);
//			intent.putExtra(OnlineSetupActivity.EXTRA_FORCE_REQUEST, true);
			Intent intent = OnlineSetupActivity.createFullScreenIntent(this);
			startActivityForResult(intent, REQUEST_ONLINE_SETUP);
		// オートアルバムボタン
		} else if (view.getId() == R.id.btnAutoalbum){

			// 	スケジューラサービスの設定
//			Intent intent = new Intent(this, OnlineSchedulerSetupActivity.class);
//			intent.putExtra(OnlineSchedulerSetupActivity.EXTRA_BACK_MODE, OnlineSchedulerSetupActivity.BACK_MODE_FINISH);
//			intent.putExtra(OnlineSchedulerSetupActivity.EXTRA_FORCE_REQUEST, true);
			Intent intent = OnlineSchedulerSetupActivity.createFullScreenIntent(this);
			startActivityForResult(intent, REQUEST_ONLINE_SETUP_SCHEDULE);

		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if(keyCode == KeyEvent.KEYCODE_BACK){
			//次回からこの画面を表示しないように登録
			PreferenceUtil.setBooleanPreferenceValue(this, KEY_NOT_FIRST_SEARCH_TIME, false);
		}
		return super.onKeyDown(keyCode, event);
	}
}
