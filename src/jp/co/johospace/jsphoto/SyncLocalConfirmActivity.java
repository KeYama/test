package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class SyncLocalConfirmActivity extends AbstractActivity implements OnClickListener {

	public static final String EXTRA_MESSAGE =
			SyncLocalConfirmActivity.class.getName() + ".EXTRA_MESSAGE";
	
	/** キャンセル */
	private Button mNo;
	/** キャンセル(再表示しない) */
	private Button mNever;
	/** OK */
	private Button mYes;
	
	/** ポップアップ */
	private PopupWindow mPopSearchSet;

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sync_local_confirm);
		
		mNo = (Button)findViewById(R.id.btnNo);
		mNo.setOnClickListener(this);
		
		mNever = (Button)findViewById(R.id.btnNever);
		mNever.setOnClickListener(this);
		
		mYes = (Button)findViewById(R.id.btnYes);
		mYes.setOnClickListener(this);
		
		((TextView) findViewById(R.id.txt_message)).setText(getIntent().getStringExtra(EXTRA_MESSAGE));

		Button btnMetadata = (Button)findViewById(R.id.btnMetadata);
		btnMetadata.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// ポップアップか何かで内容について通知
				// ポップアップにセット
			    mPopSearchSet = new PopupWindow(v);

			    LinearLayout layoutSet = (LinearLayout) getLayoutInflater().inflate(R.layout.what_is_metadata, null);
			    mPopSearchSet.setContentView(layoutSet);
			    
			    mPopSearchSet.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
			    mPopSearchSet.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
			    // ポップアップウィンドウ領域外タップ時は閉じる
			    mPopSearchSet.setBackgroundDrawable(new BitmapDrawable());
			    mPopSearchSet.setOutsideTouchable(true);
			    mPopSearchSet.showAtLocation(v, Gravity.TOP, 0, 0);
			}
		});
	}

	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.btnYes: {
			// チェックがついていれば以後確認しない
			MediaSyncManagerV2.saveLocalSyncAllowed(this, true);
			MediaSyncManagerV2.startSend(this, null);
			saveNeedConfirm(this, true);
			Toast.makeText(this, R.string.sync_local_confirm_message_available_soon, Toast.LENGTH_LONG).show();
			
			finish();
		}
		break;
		case R.id.btnNo: {
			finish();
		}
		break;
		case R.id.btnNever: {
			saveNeedConfirm(this, false);
			finish();
		}
		break;
		}
		
	}
	
	private static final String KEY_NEED_CONFIRM =
			SyncLocalConfirmActivity.class.getName() + ".needConfirm";
	
	public static boolean needConfirm(Context context) {
		boolean need = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				KEY_NEED_CONFIRM, true);
		boolean active = MediaSyncManagerV2.isLocalSyncAllowed(context);
		return need && !active;
	}
	
	public static void saveNeedConfirm(Context context, boolean needConfirm) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_NEED_CONFIRM, needConfirm).commit();
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mPopSearchSet != null && mPopSearchSet.isShowing()) {
			mPopSearchSet.dismiss();
		}
		
	}
}
