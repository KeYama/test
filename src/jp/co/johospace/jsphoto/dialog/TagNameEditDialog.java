package jp.co.johospace.jsphoto.dialog;

import java.util.ArrayList;
import java.util.Map;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.accessor.TagEditor;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import android.app.Dialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * タグ名を編集するダイアログです
 */
public class TagNameEditDialog extends Dialog implements OnClickListener {

	/** OKボタン */
	private Button mBtnOk;
	/** キャンセルボタン */
	private Button mBtnCancel;
	/** 編集テキストボックス */
	private EditText mTxtTagName;

	/** 旧タグ名 */
	private String mOldName;
	/** データベース */
	private static SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();

	/** 変更後のタグ名 */
	public String mAfterTagName;

	/** TextWatcher */
	public TextWatcher mTextWatcher;
	
	
	public TagNameEditDialog(Context context, String oldName) {
		super(context);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		mOldName = oldName;

		setContentView(R.layout.tag_name_edit);
		
		init(context);
	}


	/**
	 * 初期設定
	 * @param context
	 */
	public void init(Context context) {
		// レイアウト設定
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
//        LayoutInflater inflater =
//                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View view = inflater.inflate(R.layout.tag_name_edit, null);
////        setView(view);
//        setContentView(view);

        // 各View設定
        TextView title = (TextView)findViewById(R.id.txtTitle);
        title.setText(context.getString(R.string.tag_label_name_edit));

        mBtnOk = (Button)findViewById(R.id.btnOk);
        mBtnOk.setOnClickListener(this);
//        mBtnOk.setEnabled(false);
        
        mBtnCancel = (Button)findViewById(R.id.btnCancel);
        mBtnCancel.setOnClickListener(this);

        mTxtTagName = (EditText)findViewById(R.id.txtTagEdit);
        mTxtTagName.setHint(mOldName);
        
		mTextWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
				String text = mTxtTagName.getText().toString().trim();
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
        
//        mTxtTagName.addTextChangedListener(mTextWatcher);

//        setIcon(0);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnOk:
				// タグ名置き換え処理
				replaceTagName();
				break;
			case R.id.btnCancel:
				dismiss();
				break;
		}
	}


	/**
	 * 旧タグ名から新タグ名へ置き換えます
	 */
	private void replaceTagName() {
		
		ArrayList<String> tags = TagEditor.selectAllTags(mDatabase);
		
		String newTagName = mTxtTagName.getText().toString();
		String tag = newTagName.replaceAll("[　|\\s]+", "");
		
		// 入力されている場合
		if (!TextUtils.isEmpty(tag)) {
			// 新旧比較し、差異がある場合のみ置き換え
			if (!mOldName.equals(tag)) {
				
				// ただし、重複していた場合は、Toastにて注意を促す
				for (String compTag : tags) {
					if (tag.equals(compTag)) {
						Toast.makeText(getContext(), getContext().getString(R.string.tag_label_error_message), Toast.LENGTH_SHORT).show();
						return;
					}
				}
				
				
				TagEditor.replaceAll(mDatabase, mOldName, tag);
				mAfterTagName = tag;
				
				// 双方向同期
				Map<String, Map<String, SyncSetting>> settings =
						MediaSyncManagerV2.loadSyncSettings(getContext());
				for (String service : settings.keySet()) {
					Map<String, SyncSetting> accounts = settings.get(service);
					if (!accounts.isEmpty()) {
						Long interval = accounts.values().iterator().next().interval;
						if (interval == null || 0 < interval) {
							MediaSyncManagerV2.startSyncMedia(getContext(), null);
						}
					}
				}
				
				// ローカルインデクシング
				if (MediaSyncManagerV2.isLocalSyncAllowed(getContext())) {
					MediaSyncManagerV2.startSend(getContext(), null);
				}
				
				dismiss();
			}
		}

	}
}
