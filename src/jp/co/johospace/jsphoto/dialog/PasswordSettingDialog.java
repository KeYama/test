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
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * シークレットパスワードの設定ダイアログです
 */
public class PasswordSettingDialog extends AlertDialog implements OnClickListener {
	
	/** 古いパスワード入力欄 */
	private EditText mOldPassword;
	/** 第１パスワード入力欄 */
	private EditText mPasswordFirst;
	/** 第２パスワード入力欄 */
	private EditText mPasswordSecond;
	
	/** パスワード設定コンテナ */
	private LinearLayout mOldPass;
	
	/** 設定パスワード */
	private String mPassword;
	
	/** OKボタン */
	private Button mBtnOk;
	/** キャンセルボタン */
	private Button mBtnCancel;

	/** パスワード正常セットフラグ */
	public boolean mIsSetPassword = false;
	
	public PasswordSettingDialog(Context context) {
		super(context);
		init(context);
	}
	

	public void init(Context context) {
		// レイアウト設定
        LayoutInflater inflater = 
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.setting_password, null);
        setView(view);
        
        setTitle(context.getString(R.string.pref_password_setting));
        
        // 旧パスワード入力コンテナ
        mOldPass = (LinearLayout)view.findViewById(R.id.lytOldPassword);
        // 各入力欄
        mOldPassword = (EditText) view.findViewById(R.id.editOldPassword);
        mPasswordFirst = (EditText) view.findViewById(R.id.editPassword);
        mPasswordSecond = (EditText) view.findViewById(R.id.editPasswordSecond);
        
        mBtnOk = (Button) view.findViewById(R.id.btnOk);
        mBtnCancel = (Button) view.findViewById(R.id.btnCancel);
        
        //TODO テキストチェックは保留
//        mBtnOk.setEnabled(false);
        mBtnOk.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        
        // パスワード設定状況を確認し、設定済みなら古いパスワードを表示
        mPassword = PreferenceUtil.getPreferenceValue(context, ApplicationDefine.KEY_SECRET_PASSWORD, null);
        if (mPassword == null) {
        	mOldPass.setVisibility(View.GONE);
        }
        
		// TextWatcherを設定
		TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// テキストが入力されている間、OKボタンを有効化
				
				boolean isOldPasswordEmpty;
				
				if (mOldPass.getVisibility() == View.VISIBLE) {
					isOldPasswordEmpty = TextUtils.isEmpty(mOldPassword.getText().toString().trim());
				} else {
					isOldPasswordEmpty = false;
				}
				
				if (!isOldPasswordEmpty && 
						!TextUtils.isEmpty(mPasswordFirst.getText().toString().trim()) && 
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
//		mOldPassword.addTextChangedListener(textWatcher);
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
		String old = mOldPassword.getText().toString().trim();
		String first = mPasswordFirst.getText().toString().trim();
		String second = mPasswordSecond.getText().toString().trim();
		
		first = first.replaceAll("[　|\\s]+", "");
		second = second.replaceAll("[　|\\s]+", "");
		
		// 旧パスワード入力欄が表示されている場合
		if (mOldPass.isShown()) {
			// パスワードを変更する場合(パスワード設定済み、旧パスワードが入力済み)
			if (mPassword != null && !TextUtils.isEmpty(old)) {
				
				// 旧パスワードと入力された内容を確認
				if (mPassword.equals(old)) {
				
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
				// 旧パスワード入力ミス
				} else {
					Toast.makeText(getContext(), getContext().getString(R.string.pref_message_different_old_password), Toast.LENGTH_SHORT).show();
				}
			// 旧パスワードが未入力
			} else if (TextUtils.isEmpty(old)) {
				Toast.makeText(getContext(), getContext().getString(R.string.pref_message_not_input_old_password), Toast.LENGTH_SHORT).show();
			}
			
		// 初回パスワード設定時
		} else {
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
}
