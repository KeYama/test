package jp.co.johospace.jsphoto.dialog;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * シークレットパスワードの設定ダイアログです
 */
public class SecretPasswordDialog extends AlertDialog implements OnClickListener {

	/** 第１パスワード入力欄 */
	private EditText mPasswordFirst;
	/** 第２パスワード入力欄 */
	private EditText mPasswordSecond;
	
	/** OKボタン */
	private Button mBtnOk;
	/** キャンセルボタン */
	private Button mBtnCancel;

	/** パスワード正常セットフラグ */
	public boolean mIsSetPassword = false;
	
	public SecretPasswordDialog(Context context) {
		super(context);
		init(context);
	}
	

	public void init(Context context) {
		// レイアウト設定
        LayoutInflater inflater = 
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.secret_password, null);
        setView(view);
        
        setTitle(getContext().getString(R.string.folder_title_setting_secret_password));
        
        mPasswordFirst = (EditText) view.findViewById(R.id.editPassword);
        mPasswordSecond = (EditText) view.findViewById(R.id.editPasswordSecond);
        
        mBtnOk = (Button) view.findViewById(R.id.btnOk);
        mBtnCancel = (Button) view.findViewById(R.id.btnCancel);
        
        //TODO テキストチェックは保留
//        mBtnOk.setEnabled(false);
        mBtnOk.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        
		// TextWatcherを設定
		TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// テキストが入力されている間、OKボタンを有効化
				if (!TextUtils.isEmpty(mPasswordFirst.getText().toString().trim()) && 
						!TextUtils.isEmpty(mPasswordSecond.getText().toString().trim())) {
					mBtnOk.setEnabled(true);
				} else {
					mBtnOk.setEnabled(false);
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable s) {}
		};
		
		//TODO テキストチェックは保留
//		mPasswordFirst.addTextChangedListener(textWatcher);
//		mPasswordSecond.addTextChangedListener(textWatcher);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnOk:
				checkPassword();
				break;
			case R.id.btnCancel:
				dismiss();
				break;
		}
	}
	
	/**
	 * 入力されたパスワードのチェックを行い、設定します
	 */
	public void checkPassword() {
		String first = mPasswordFirst.getText().toString().trim();
		String second = mPasswordSecond.getText().toString().trim();
		
		first = first.replaceAll("[　|\\s]+", "");
		second = second.replaceAll("[　|\\s]+", "");
		
		// 初期パスワード未入力
		if ("".equals(first)) {
			Toast.makeText(getContext(), getContext().getString(R.string.folder_message_setting_secret_first_nothing), Toast.LENGTH_SHORT).show();
		
		// 確認パスワード未入力
		} else if ("".equals(second)) {
			Toast.makeText(getContext(), getContext().getString(R.string.folder_message_setting_secret_second_nothing), Toast.LENGTH_SHORT).show();
		
		// パスワードが一致
		} else if (first.equals(second)) {
			PreferenceUtil.setPreferenceValue(getContext(), ApplicationDefine.KEY_SECRET_PASSWORD, first);
			mIsSetPassword = true;
			dismiss();
		
		// パスワードが不一致
		} else {
			Toast.makeText(getContext(), getContext().getString(R.string.folder_message_setting_secret_error_password), Toast.LENGTH_SHORT).show();
		}
		
	}
}
