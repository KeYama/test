package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.dialog.SimpleMessageDialog;
import jp.co.johospace.jsphoto.view.TableRadioGroup;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;

public class SelectSyncFolderLiteActivity extends AbstractActivity implements OnClickListener {
	
	public static final String EXTRA_WIFI =
			SelectSyncFolderLiteActivity.class.getName() + ".EXTRA_WIFI";
	public static final String EXTRA_TARGET =
			SelectSyncFolderLiteActivity.class.getName() + ".EXTRA_TARGET";
	
	public static final int TARGET_ONLY_JORLLE = 1;
//	public static final int TARGET_ALL_CAMERA = 2;
	public static final int TARGET_NONE = 0;
	
	private static final int DIALOG_HELP1 = 1;

	
	/** OK */
	private Button mBtnOk;
	/** キャンセル */
	private Button mBtnCancel;
	/** ヘルプ */
	private Button mBtnHelp1;
//	/** ヘルプ2 */
//	private Button mBtnHelp2;
	/** ラジオグループ */
	private TableRadioGroup mRadios;
	/** Wifi */
	private CheckBox mWifi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.select_sync_folder_lite);
		
		// OKボタン
		mBtnOk = (Button)findViewById(R.id.btnOk);
		mBtnOk.setOnClickListener(this);
		
		// キャンセルボタン
		mBtnCancel = (Button)findViewById(R.id.btnCancel);
		mBtnCancel.setOnClickListener(this);
		
		// ヘルプボタン
		mBtnHelp1 = (Button) findViewById(R.id.btnHelp1);
		mBtnHelp1.setOnClickListener(this);
		
//		// ヘルプ2ボタン
//		mBtnHelp2 = (Button) findViewById(R.id.btnHelp2);
//		mBtnHelp2.setOnClickListener(this);
		
		// ラジオグループ
		mRadios = (TableRadioGroup) findViewById(R.id.radios);
		
		// Wifi
		mWifi = (CheckBox) findViewById(R.id.chkWiFi);
	}

	@Override
	public void onClick(View v) {
	
		// OKボタン押下時
		if (v.getId() == R.id.btnOk) {
			Intent data = new Intent();
			int target;
			switch (mRadios.getCheckedRadioButtonId()) {
			case R.id.rbUpSelf:
				target = TARGET_ONLY_JORLLE;
				break;
//			case R.id.rbUpAll:
//				target = TARGET_ALL_CAMERA;
//				break;
			default:
				target = TARGET_NONE;
			}
			data.putExtra(EXTRA_TARGET, target);
			data.putExtra(EXTRA_WIFI, mWifi.isChecked());
			setResult(RESULT_OK, data);
			finish();
			
		// キャンセルボタン押下時
		} else if (v.getId() == R.id.btnCancel) {
			finish();
		}
		// ヘルプボタン
		else if (v.getId() == R.id.btnHelp1) {
			showDialog(DIALOG_HELP1);
		}
//		// ヘルプボタン2
//		else if (v.getId() == R.id.btnHelp2) {
//			Toast.makeText(this, "help2", Toast.LENGTH_LONG).show();
//		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_HELP1:
			SimpleMessageDialog dialog = new SimpleMessageDialog(this);
			return dialog;
		}
		return super.onCreateDialog(id);
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_HELP1:
			((SimpleMessageDialog)dialog).setMessage(getString(R.string.message_select_sync_folder_help));
			return;
		}
		super.onPrepareDialog(id, dialog);
	}
}
