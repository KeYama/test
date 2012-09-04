package jp.co.johospace.jsphoto.dialog;

import jp.co.johospace.jsphoto.R;
import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SimpleEditDialog extends Dialog {
	
	/** タイトル */
	public TextView mTxtTitle;
	/** 説明文 */
	public TextView mTxtView;
	/** テキスト編集 */
	public EditText mTxtEdit;
	/** OKボタン */
	public Button mBtnOk;
	/** キャンセルボタン */
	public Button mBtnChansel;
	
	/** TextWatcher */
	public TextWatcher mTextWatcher;
	
	public SimpleEditDialog(Context context) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.simple_edit_dialog);
		
		// 初期処理
		init(context);
	}

	private void init(Context context) {
		
		// 各Viewを設定
		mTxtTitle = (TextView)findViewById(R.id.txtTitle);
		mTxtView = (TextView)findViewById(R.id.txtMessage);
		mTxtEdit = (EditText)findViewById(R.id.txtEditText);
		
		mBtnOk = (Button)findViewById(R.id.btnOk);
		mTextWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
				String text = mTxtEdit.getText().toString().trim();
				// 入力値が無い場合は、OKボタンは無効
				if (TextUtils.isEmpty(text)) {
					mBtnOk.setEnabled(false);
				} else {
					mBtnOk.setEnabled(true);
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable editable) {}
		};
		
//		mTxtEdit.addTextChangedListener(mTextWatcher);
			
		mBtnChansel = (Button)findViewById(R.id.btnCancel);
	}
}
