package jp.co.johospace.jsphoto.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.HomeActivity.CategoryState;
import jp.co.johospace.jsphoto.accessor.TagMasterAccessor;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.app.AlertDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

/**
 * カテゴリ追加のダイアログです
 */
public class CategoryAddDialog extends AlertDialog implements android.view.View.OnClickListener{
	
	/** DBアクセス */
	private static SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();
	
	/** カテゴリ一覧アダプタ */
	private CategoryAdapter mAdapter;
	
	/** カテゴリ名、選択状態のペアを格納したリスト */
	private List<CategoryState> mListStatus;
	
	/** 更新用、カテゴリ選択状態マップ */
	private HashMap<String, Boolean> mPreferenceMap = new HashMap<String, Boolean>();
	
	/** 更新用、タグ選択状態マップ */
	private HashMap<String, Boolean> mTagMap = new HashMap<String, Boolean>();
	
	/** カテゴリ名リストビュー */
	private ListView mListCategory;
	
	/** OKボタン */
	private Button mBtnOk;
	
	/** キャンセル */
	private Button mBtnCancel;
	
	/**
	 * カテゴリ名リストのアダプタ
	 */
	private class CategoryAdapter extends ArrayAdapter<CategoryState> {

		private LayoutInflater mInflater;
		
		/** チェック状態マップ */
		public List<Boolean> mListChk;
		
		public CategoryAdapter(Context context, List<CategoryState> objects) {
			super(context, R.layout.category_item, objects);
			
			mInflater = getLayoutInflater();
			
			// チェック状態マップの初期化
			initChkMap(objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				v = mInflater.inflate(R.layout.category_item, parent, false);
			}
			
			// タグ名を取得
			final CategoryState state = getItem(position);
			final String name = state.name;
			final String categoryName = state.category;
			
			final boolean isTag = state.isTag;
			
			final int categoryPos = position;
			
			// タグ名設定
			TextView txtName = (TextView) v.findViewById(R.id.txtCategoryName);
			txtName.setText(name);
			
			// 選択チェック
			CheckBox chkTag = (CheckBox) v.findViewById(R.id.chkCategory);
			
			chkTag.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final CheckBox checkBox = (CheckBox)v;
					
					boolean checked = checkBox.isChecked();
					
					// チェック状態マップに登録
					mListChk.set(categoryPos, checked);
					
					// タグ、設定値で、それぞれのマップに状態を保存
					if (isTag) {
						mTagMap.put(categoryName, checked);
						
					} else {
						mPreferenceMap.put(categoryName, checked);
					}
					
				}
			});
			
			// チェック状態マップを元に、チェックボックスに値をセット
			chkTag.setChecked(mListChk.get(categoryPos));
			
			return v;
		}
		
		/**
		 * チェック状態マップを初期化します
		 * @param cursor	カーソル
		 */
		public void initChkMap(List<CategoryState> list) {

			mListChk = new ArrayList<Boolean>();
			
			for (CategoryState status : list) {
				mListChk.add(status.checked);
			}
		}
	}
	
	
	public CategoryAddDialog(Context context, List<CategoryState> categoryList) {
		super(context);
		mListStatus = categoryList;
		init(context);
	}
	
	/**
	 * 初期化処理
	 * 
	 * @param context コンテキスト
	 */
	public void init(Context context) {
		
		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
//		setTitle(context.getString(R.string.home_menu_category_setting));
		
		// レイアウト設定
        LayoutInflater inflater = 
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.category, null);
        setView(view);
        
        mListCategory = (ListView) view.findViewById(R.id.listCategory);
        mBtnOk = (Button) view.findViewById(R.id.btnAddCategory);
        mBtnOk.setOnClickListener(this);
        
        mBtnCancel = (Button) view.findViewById(R.id.btnCancel);
        mBtnCancel.setOnClickListener(this);
        
		mListCategory = (ListView) view.findViewById(R.id.listCategory);
		
		// アダプタ設定
		mAdapter = new CategoryAdapter(context, mListStatus);
		mListCategory.setAdapter(mAdapter);
	}
	

	/**
	 * チェックの変更があったカテゴリの、プリファレンス、DBを更新します
	 */
	public void changeVisibleState() {
		
		Set<String> keySet = mPreferenceMap.keySet();
		Iterator<String> keyIte = keySet.iterator();
        
		String tagName = null;
		
		// プリファレンス値の変更
        while(keyIte.hasNext()) {
            tagName = (String) keyIte.next();   
            PreferenceUtil.setBooleanPreferenceValue(getContext(), tagName, mPreferenceMap.get(tagName));
        }   
		
        keySet = mTagMap.keySet();
        keyIte = keySet.iterator();
        
        // DB値の更新
        while(keyIte.hasNext()) {
            tagName = (String) keyIte.next();   
            TagMasterAccessor.updateTagHide(mDatabase, tagName, mTagMap.get(tagName));
        }   
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
			// OKボタン
			case R.id.btnAddCategory:
				changeVisibleState();
				break;
				
			// キャンセルボタン
			case R.id.btnCancel:
				mAdapter.mListChk.clear();
				break;
		}
		
		dismiss();
	}
}
