package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class SyncConfirmActivity extends AbstractActivity implements OnClickListener {

	
	public static final String EXTRA_SERVICE =
			SyncConfirmActivity.class.getName() + ".EXTRA_SERVICE";
	public static final String EXTRA_AUTH_URL =
			SyncConfirmActivity.class.getName() + ".EXTRA_AUTH_URL";
	
	/** キャンセルボタン */
	private Button mCancel;
	/** OKボタン */
	private Button mOk;
	/** 以後聞かない */
	private CheckBox mNoNeedConfirm;
	
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sync_confirm);
		
		mCancel = (Button)findViewById(R.id.btnCancel);
		mCancel.setOnClickListener(this);
		
		mOk = (Button)findViewById(R.id.btnOk);
		mOk.setOnClickListener(this);
		
		((TextView) findViewById(R.id.txt_message)).setText(
				getString(R.string.online_format_sync_confirm,
						ClientManager.getServiceName(this, getIntent().getStringExtra(EXTRA_SERVICE))));
		
		mNoNeedConfirm = (CheckBox) findViewById(R.id.chk_no_need_confirm);
	}

	@Override
	public void onClick(View v) {
		
		if (v.getId() == R.id.btnCancel) {
			finish();
		} else if (v.getId() == R.id.btnOk) {
			Intent data = new Intent();
			data.putExtra(EXTRA_SERVICE,
					getIntent().getStringExtra(EXTRA_SERVICE));
			data.putExtra(EXTRA_AUTH_URL,
					getIntent().getStringExtra(EXTRA_AUTH_URL));
			setResult(RESULT_OK, data);
			finish();
		}
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		saveNeedConfirm(this, !mNoNeedConfirm.isChecked());
	}
	
	private static final String KEY_NEED_CONFIRM =
			SyncConfirmActivity.class.getName() + ".needConfirm";
	
	public static boolean needConfirm(Context context) {
		return false;
//		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
//				KEY_NEED_CONFIRM, true);
	}
	
	public static void saveNeedConfirm(Context context, boolean needConfirm) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_NEED_CONFIRM, needConfirm).commit();
	}
}
