package jp.co.johospace.jsphoto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.LocalFolderActivity.FolderEntry;
import jp.co.johospace.jsphoto.accessor.TagEditor;
import jp.co.johospace.jsphoto.accessor.TagEditor.TagEntry;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.TagNameEditDialog;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.view.ViewPager.LayoutParams;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * タグ入力画面アクティビティです
 */
public class TagEditActivity extends AbstractActivity implements OnClickListener, OnItemClickListener, CMediaMetadata {

	/** タグ入力エディットテキスト */
	private EditText mTxtTag;
	/** 設定中のタグリストビュー */
	private ListView mLstTagEquipment;
	/** 選択した画像のパスリスト */
	private ArrayList<String> mPathList;
	
	/** 受け取ったパスのFile形式 */
	private ArrayList<File> mFileList;
	
	/** フォルダ情報リスト */
	private List<FolderEntry> mEntryLocal = new ArrayList<FolderEntry>();
	/** 長押しで選択したアイテム */
	private TagEditor.TagEntry mTagSelect;
	/** メニューインフォ(予備) */
	private AdapterContextMenuInfo mLastMenuInfo = null;
	
	/** 追加ボタン */
	private ImageButton mBtnAdd;

	/** OKボタン */
	private Button mBtnOk;
	/** キャンセルボタン */
	private Button mBtnCancel;
	
	/** DBアクセス */
	private static SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();
	/** カーソル */
	private Cursor mCursor;
	/** リスト表示用 */
	private ArrayList<TagEntry> mLstTag;
	/** タグリストアダプター */
	private TagListAdapter mLstAdapter;
	
	/** リクエストコード */
	private static final int REQUEST_HISTORY = 0;
	
	/** コンテキストメニュー 編集 */
	private static final int MENU_EDIT = 0;
	/** コンテキストメニュー 削除 */
	private static final int MENU_DELETE = 1;
	
	/** 入力可能最大文字数 */
	private static final int MAX_TAG_LENGTH = 128;
	
	/** タグが変更されたら立つ */
	private boolean mDirty;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.tag_edit);
		setTitle(getString(R.string.tag_title_edit));
		
		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.FILL_PARENT;
        getWindow().setAttributes(params);
		
		// Intent取得
		Intent pathIntent = getIntent();
		
		String path = pathIntent.getStringExtra(ApplicationDefine.INTENT_PATH);
		mPathList = new ArrayList<String>();
		
		// 単一選択がされていない場合、複数選択
		if (path == null) {
			mPathList = pathIntent.getStringArrayListExtra(ApplicationDefine.INTENT_PATH_LIST);
		} else {
			mPathList.add(path);
		}
		
		// 画像が選択されていない場合
		if (mPathList == null || mPathList.size() == 0) {
			Toast.makeText(getApplicationContext(), getString(R.string.tag_message_no_image), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		// 初期表示処理
		init();
		
	}


	/**
	 * 初期処理
	 * 
	 */
	private void init() {
		
		// 各ビューを設定
		mTxtTag = (EditText)findViewById(R.id.txtTag);
		mLstTagEquipment = (ListView)findViewById(R.id.lstTagEquipment);
		mBtnAdd = (ImageButton)findViewById(R.id.btnAdd);
		mBtnAdd.setOnClickListener(this);
		
		mBtnOk = (Button)findViewById(R.id.btnOk);
		mBtnOk.setOnClickListener(this);
		// 初期表示は押下不可 TODO チェック状態が変更された場合も押下可能にすること
//		mBtnOk.setEnabled(false);
		
		mBtnCancel = (Button)findViewById(R.id.btnCancel);
		mBtnCancel.setOnClickListener(this);
		mLstTag = new ArrayList<TagEditor.TagEntry>();

		// TextWatcherを設定
		TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// テキストが入力されている間、OKボタンを有効化
				if (!TextUtils.isEmpty(mTxtTag.getText().toString().trim())) {
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
//		mTxtTag.addTextChangedListener(textWatcher);
		
		
		mFileList = new ArrayList<File>();
		
		// 受け取ったパス(String)を、File形式に変換
		for (String pathName : mPathList) {
			mFileList.add(new File(pathName));
		}
		
		// 表示するリスト
		mLstTag = (ArrayList<TagEntry>)TagEditor.queryTagEntries(mDatabase, mFileList);
		
		mLstAdapter = new TagListAdapter(getApplicationContext(), android.R.layout.simple_list_item_checked, mLstTag);
		mLstTagEquipment.setAdapter(mLstAdapter);
		mLstTagEquipment.setOnItemClickListener(this);
		// 長押時には編集、削除のダイアログを表示
		registerForContextMenu(mLstTagEquipment);
			
	}
	
	
	@Override
	public void onClick(View view) {
		
		// 追加ボタン押下
		if (view.getId() == R.id.btnAdd) {
			
			// 何も入力されていない場合は処理しない
			if (!TextUtils.isEmpty(mTxtTag.getText())) {
				String tag = mTxtTag.getText().toString();
				tag = tag.replaceAll("[　|\\s]+", "");
				addTag(tag);
			}
			// 入力エリアをクリア
			mTxtTag.setText(null);
			
		// OKボタン押下
		} else if (view.getId() == R.id.btnOk) {
			if (!TextUtils.isEmpty(mTxtTag.getText())) {
				String tag = mTxtTag.getText().toString();
				tag = tag.replaceAll("[　|\\s]+", "");
				addTag(tag);
			}
			// DB登録処理
			TagEditor.updateTags(mDatabase, mFileList, mLstTag);
			mDirty = true;
			setResult(RESULT_OK);
			finish();
			
		// キャンセルボタン押下
		} else if (view.getId() == R.id.btnCancel) {
			finish();
		}
	}
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		//履歴画面から戻ってきた場合
		if (requestCode == REQUEST_HISTORY) {
			// 追加する場合
			if (resultCode == RESULT_OK) {
				Bundle bundle = data.getExtras();
				
				ArrayList<String> resultList = bundle.getStringArrayList(ApplicationDefine.INTENT_RESULT);
				for (int i = 0; i < resultList.size(); i++) {
					String tag = resultList.get(i).replaceAll("[　|\\s]+", "");
					addTag(tag);
				}
			}
		}
	}
	
	
	/**
	 * タグをリストに追加します。
	 * @param tag
	 */
	private void addTag(String tag) {

		if (tag.length() > 0) {
			// リスト内に重複値がある場合はチェックを付ける
			for (int i = 0; i < mLstAdapter.getCount(); i++) {
				if (mLstAdapter.getItem(i).tag.equals(tag)) {
					mLstAdapter.getItem(i).checked = true;
					mLstAdapter.notifyDataSetChanged();
					return;
				}
			}
			
			// リストビューに入力値を設定
			TagEntry meta = new TagEntry();
			meta.tag = tag;
			meta.checked = true;
			mLstTag.add(0, meta);
			mLstAdapter.notifyDataSetChanged();
		}
	}


	
	/**
	 * タグ一覧のアダプタ
	 */
	private class TagListAdapter extends ArrayAdapter<TagEntry> {
		
		private LayoutInflater mInflater;
		private ArrayList<TagEntry> mList;
		
		// コンストラクタ
		public TagListAdapter(Context context, int resourceId, ArrayList<TagEntry> lstTag) {
			super(context, resourceId, lstTag);
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mList = lstTag;
			
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup viewGroup) {
			View view = convertView;
			if (view == null) {
				view = mInflater.inflate(R.layout.tag_history_item, null);
			}
			
			// 履歴タグ名
			TextView tagName = (TextView)view.findViewById(R.id.txtHistory);
			tagName.setText(mList.get(position).tag);
			
			// チェックボックス設定(View再利用時を考慮)
			CheckBox chkBox = (CheckBox) view.findViewById(R.id.chkHistory);
			final int p = position;
			chkBox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					CheckBox check = (CheckBox)v.findViewById(R.id.chkHistory);
					if (check.isChecked()) {
						mList.get(p).checked = true;
					} else {
						mList.get(p).checked = false;
					}
				}
			});
			chkBox.setChecked(mList.get(position).checked);
			
			return view;
		}
	}
	
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    
	    mLastMenuInfo = (AdapterContextMenuInfo) menuInfo;
	    
	    ListView listView = (ListView)v;
	    final TagEditor.TagEntry tagEntry = (TagEntry) listView.getItemAtPosition(mLastMenuInfo.position);
	    
	    //コンテキストメニューの設定
	    menu.setHeaderTitle(getString(R.string.tag_item_edit_title));
	    menu.add(0, MENU_EDIT, 0, getString(R.string.tag_label_edit)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem menuitem) {
				if (tagEntry != null) {
					TagNameEditDialog dialog = new TagNameEditDialog(TagEditActivity.this, tagEntry.tag);
					dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
					dialog.setOnDismissListener(new OnDismissListener() {
						
						@Override
						public void onDismiss(DialogInterface dialog) {
							// タグを取り直す
							TagNameEditDialog dismissDialog = (TagNameEditDialog)dialog;
							String afterTagName = dismissDialog.mAfterTagName;
							// タグ名が設定されている場合
							if (!TextUtils.isEmpty(afterTagName)) {
								// アダプタに設定されている要素を削除し、内容を置き換えて設定し直す
								mLstAdapter.remove(tagEntry);
								tagEntry.tag = afterTagName;
								mLstAdapter.add(tagEntry);
								mLstAdapter.notifyDataSetChanged();
							}
						}
					});
					dialog.show();
				}
				return false;
			}
		});
	    menu.add(0, MENU_DELETE, 0, getString(R.string.tag_label_delete)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem menuitem) {
		    	if (tagEntry != null) {
					 new AlertDialog.Builder(TagEditActivity.this)
					.setTitle(getString(R.string.tag_label_delete))
					.setMessage(getString(R.string.tag_label_delete_confirmation, tagEntry.tag))
					.setPositiveButton(android.R.string.cancel, null)
					.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							TagEditor.removeAll(mDatabase, tagEntry.tag);
							mDirty = true;
							mLstAdapter.remove(tagEntry);
							mLstAdapter.notifyDataSetChanged();
						}
					})
					.show();
		    		
		    	}
				return false;
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mDirty) {
			// 双方向同期
			Map<String, Map<String, SyncSetting>> settings =
					MediaSyncManagerV2.loadSyncSettings(this);
			for (String service : settings.keySet()) {
				Map<String, SyncSetting> accounts = settings.get(service);
				if (!accounts.isEmpty()) {
					Long interval = accounts.values().iterator().next().interval;
					if (interval == null || 0 < interval) {
						MediaSyncManagerV2.startSyncMedia(this, null);
					}
				}
			}
			
			// ローカルインデクシング
			if (MediaSyncManagerV2.isLocalSyncAllowed(this)) {
				MediaSyncManagerV2.startSend(this, null);
			}
		}
		
		if (mCursor != null) {
			mCursor.close();
		}
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {

		CheckBox chkBox = (CheckBox)view.findViewById(R.id.chkHistory);
		TagEntry item = mLstAdapter.getItem(position);
		
		if (item.checked) {
			item.checked = false;
			chkBox.setChecked(false);
		} else {
			item.checked = true;
			chkBox.setChecked(true);
		}
		
	}
	 
}
