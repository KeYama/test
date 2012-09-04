package jp.co.johospace.jsphoto.dialog;

import jp.co.johospace.jsphoto.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class SimpleMessageDialog extends Dialog {

	private Button mBtnClose;
	private TextView mTxtMesg;

	private String mMessage;
	
	private View.OnClickListener mOnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			dismiss();
		}
	};
	
	public SimpleMessageDialog(Context context) {
		super(context);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		super.onCreate(savedInstanceState);
		
		init();
	}

	@Override
	protected void onStart() {
		super.onStart();

		mTxtMesg.setText(mMessage);
	}

	public void setMessage(String message) {
		mMessage = message;
	}

	private void init() {
		setContentView(R.layout.simple_message_dialog);
		
		mBtnClose = (Button) findViewById(R.id.btnClose);
		mTxtMesg = (TextView) findViewById(R.id.txt_message);
		
		mBtnClose.setOnClickListener(mOnClick);
	}
	
}
