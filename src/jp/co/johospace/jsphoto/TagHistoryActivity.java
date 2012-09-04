package jp.co.johospace.jsphoto;

import java.util.ArrayList;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager.LayoutParams;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

/**
 * タグ履歴アクティビティです
 */
public class TagHistoryActivity extends FragmentActivity implements OnClickListener, CMediaMetadata {

	/** 履歴リストビュー */
	private ListView _lstHistory;
	/** 履歴リスト */
	private ArrayList<String> mLstHistory;
	/** 履歴追加リスト */
	private ArrayList<String> mLstResult;
	/** カーソル */
	private Cursor mCursor;
	
	/** OKボタン */
	private Button _btnOk;
	/** キャンセルボタン */
	private Button _btnCancel;
	
	private static SQLiteDatabase mDataBase = OpenHelper.external.getDatabase();;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.tag_history);
		setTitle(getString(R.string.tag_title_add_history));
		
        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.FILL_PARENT;
        getWindow().setAttributes(params);
		
		init();
	}

	/**
	 * 初期処理
	 * 
	 */
	private void init() {
		
		// 各ビューを設定
		_lstHistory = (ListView)findViewById(R.id.lstHistory);
		
		// 重複を排除する為、_ID列は全て0とする
		String sql = 
				"select distinct 0 AS _id, METADATA " +
						"from media_metadata " +
						"where METADATA_TYPE = '" + ApplicationDefine.MIME_TAG + "' " +
						"order by UPDATE_TIMESTAMP DESC ";
		mCursor = mDataBase.rawQuery(sql, null);
		
		mLstHistory = new ArrayList<String>();
		while (mCursor.moveToNext()) {
			mLstHistory.add(mCursor.getString(mCursor.getColumnIndex(METADATA)));
		}
		
		mLstResult = new ArrayList<String>();
		
		TagListAdapter adapter = new TagListAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, mLstHistory);
		_lstHistory.setAdapter(adapter);

		_btnOk = (Button)findViewById(R.id.btnOk);
		_btnOk.setOnClickListener(this);
		_btnCancel = (Button)findViewById(R.id.btnCancel);
		_btnCancel.setOnClickListener(this);
		
	}

	@Override
	public void onClick(View view) {
			
		// OKボタン押下
		if (view.getId() == R.id.btnOk) {
			// チェックに対応したリストを返却する
			Intent resultIntent = new Intent();
			resultIntent.putStringArrayListExtra(ApplicationDefine.INTENT_RESULT, mLstResult);
			setResult(RESULT_OK, resultIntent);
			finish();
			
		// キャンセルボタン押下
		} else if (view.getId() == R.id.btnCancel) {
			setResult(RESULT_CANCELED);
			finish();
		}
	}
	
	/**
	 * タスクリストのアダプター
	 */
	private class TagListAdapter extends ArrayAdapter<String> {
		private LayoutInflater mInflater;
		private ArrayList<String> mList;
		private ArrayList<Boolean> mChecked;
		
		// コンストラクタ
		public TagListAdapter(Context context, int resourceId, ArrayList<String> lstHistory) {
			super(context, resourceId, lstHistory);
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mList = lstHistory;
			
			mChecked = new ArrayList<Boolean>();
			// リストのチェックボックス状態を初期化
			for (int i = 0; i < lstHistory.size(); i++) {
				mChecked.add(false);
			}
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup viewGroup) {
			View view = convertView;
			if (view == null) {
				view = mInflater.inflate(R.layout.tag_history_item, null);
			}
			
			// 履歴タグ名
			TextView tagName = (TextView)view.findViewById(R.id.txtHistory);
			tagName.setText(mList.get(position));
			
			// チェックボックス設定(View再利用時を考慮)
			CheckBox chkBox = (CheckBox) view.findViewById(R.id.chkHistory);
			final int p = position;
			chkBox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					CheckBox check = (CheckBox)v.findViewById(R.id.chkHistory);
					if (check.isChecked()) {
						mChecked.set(p, true);
						mLstResult.add(mList.get(p));
					} else {
						mChecked.set(p, false);
						mLstResult.remove(mLstResult.indexOf(mList.get(p)));
					}
				}
			});
			chkBox.setChecked(mChecked.get(position));
			return view;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mCursor != null) {
			mCursor.close();
		}
	}	
}
