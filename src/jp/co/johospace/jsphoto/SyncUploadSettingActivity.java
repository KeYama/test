package jp.co.johospace.jsphoto;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

public class SyncUploadSettingActivity extends AbstractActivity implements OnClickListener {

	
	/** 今すぐボタン */
	private Button mNow;
	/** 充電中ボタン */
	private Button mLate;
	
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sync_upload_setting);
		
		mNow = (Button)findViewById(R.id.btnNow);
		mNow.setOnClickListener(this);
		
		mLate = (Button)findViewById(R.id.btnLate);
		mLate.setOnClickListener(this);
		
		
	}

	@Override
	public void onClick(View v) {
		
		if (v.getId() == R.id.btnNow) {
			finish();
		} else if (v.getId() == R.id.btnLate) {
			finish();
		}
		
	}
}
