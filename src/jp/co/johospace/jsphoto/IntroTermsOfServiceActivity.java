package jp.co.johospace.jsphoto;
import jp.co.johospace.jsphoto.util.AppUtil;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class IntroTermsOfServiceActivity extends AbstractActivity implements OnCheckedChangeListener{

	/** メタデータ管理用 */
//	private CheckBox mCheckMetadata;

	/** メタデータとはポップアップ */
	//private PopupWindow mPopSearchSet;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.intro_dialog);
		
//		mCheckMetadata = (CheckBox)findViewById(R.id.chkMetadata);
//		mCheckMetadata.setOnCheckedChangeListener(this);
		
		// テキストをセット
		TextView ca = (TextView)findViewById(R.id.txt_terms_agreement);
		ca.setText(AppUtil.getTextFromAssetFile(getAssets(), "terms/" + 
				AppUtil.getTermsAgreementTextFileName(this)));
		
		Button btnOk = (Button)findViewById(R.id.btnExecute);
		btnOk.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
//				if(mCheckMetadata.isChecked()) {
//					MediaSyncManagerV2.saveLocalSyncAllowed(getApplicationContext(), true);
//					MediaSyncManagerV2.startSend(getApplicationContext(), null);
//				}
				setResult(RESULT_OK);
				finish();
			}
		});

		Button btnCancel = (Button)findViewById(R.id.btnCancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		/*
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
			    mPopSearchSet.showAtLocation(v, Gravity.TOP, 0, 50);
			}
		});
			    */
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		/*
		if (mPopSearchSet != null && mPopSearchSet.isShowing()) {
			mPopSearchSet.dismiss();
		}
		*/
		
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}
}