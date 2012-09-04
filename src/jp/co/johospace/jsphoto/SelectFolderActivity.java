package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jp.co.johospace.jsphoto.accessor.MediaIndexesAccessor;
import jp.co.johospace.jsphoto.accessor.MediaMetaDataAccessor;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.dialog.SimpleEditDialog;
import jp.co.johospace.jsphoto.grid.FolderPathLinearLayout;
import jp.co.johospace.jsphoto.grid.FolderPathListView;
import jp.co.johospace.jsphoto.util.IOUtil;
import jp.co.johospace.jsphoto.util.MediaStoreOperation;
import jp.co.johospace.jsphoto.util.MediaUtil;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * フォルダ選択画面アクティビティです
 */
public class SelectFolderActivity extends Activity implements
			FolderPathLinearLayout.OnPathChangedListener,
			AdapterView.OnItemClickListener
			{
	/** インテントID 開始パス */
	public static final String PARAM_START_PATH = "path";
	/** インテントID タイトル */
	public static final String PARAM_TITLE = "title";
	/** インテントID ファイルチューザ */
	public static final String PARAM_CHOOSER = "chooser";
	/** インテントID ダミーを作成する */
	public static final String PARAM_CREATE_DUMMY = "createDummy";

//	public static final String PARAM_COMMAND = "command";

	/** DBアクセス */
	private static SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();
	
	/** インテントID パス */
	public static final String RESULT_PATH = "path";

//	public static final int COMMAND_COPY = 0;
//	public static final int COMMAND_MOVE = 1;

	/** ダイアログID 新しいフォルダ */
	public static final int DIALOG_NEW_FOLDER = 1;
	/** ダミー画像サイズ */
	public static final int DUMMY_IMAGE_SIZE = 8;
	/** フォルダ名入力テキスト　初期文字列*/
	public static final String NEW_FOLDER_NAME = "new folder";

	/** 選択フォルダパス */
	private String mPath;
	/** 選択フォルダタイトル */
	private String mTitle;
	/** 前回選択フォルダ */
	private File mOldPathFile;
	/** 新しいパス追加ダイアログ　パス名入力テキスト */
	private EditText mNewFolder;
	/** パス一覧のレイアウト */
	private FolderPathLinearLayout llPath;
	/** パス一覧 */
	FolderPathListView mLvFolder;
	
	/** OKボタン */
	private Button mBtnOk;
	
	/** メニューインフォ */
	private AdapterContextMenuInfo mMenuInfo = null;
	/** フォルダ名変更EditText */
	private EditText mChangeText;
	/** フォルダ名変更対象パス */
	private String mTargetPath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.select_folder);

		Intent params = getIntent();
		if (getIntent().getBooleanExtra(PARAM_CHOOSER, false)){
//			((TextView)findViewById(R.id.txtTitle)).setText(R.string.folder_title_change_name);
			((TextView)findViewById(R.id.txtTitle)).setText(getIntent().getStringExtra(PARAM_TITLE));
		}else{
			((TextView)findViewById(R.id.txtTitle)).setText(R.string.folder_title_create);
		}

		if(params != null){
			mPath = params.getStringExtra(PARAM_START_PATH);
			
			mTitle = params.getStringExtra(PARAM_TITLE);
		}

		
		// パスがない、もしくは存在しないパスを指定していたら、SD直下を指定
		if(mPath == null) {
			mPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		} else {
			File checkFile = new File(mPath);
			
			if (!checkFile.exists()) mPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		
		if(mTitle == null)
			mTitle = "(no title)";

		llPath = ((FolderPathLinearLayout)findViewById(R.id.llPath));
		llPath.setPath(new File(mPath));
		llPath.setOnPathChangedListener(this);

		mLvFolder = ((FolderPathListView)findViewById(R.id.lvFolder));
		mLvFolder.setPath(new File(mPath));
		mLvFolder.setOnItemClickListener(this);
		// コンテキストメニューを登録
		registerForContextMenu(mLvFolder);
		mLvFolder.setBackgroundResource(R.drawable.list_view_border);

		setLayoutParam();

		mNewFolder = new EditText(this);
		mOldPathFile = new File(mPath);

		setupButtons();
	}


	@Override
	protected void onResume() {
		super.onResume();

		llPath.refreshPath();
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		FolderPathListView pathView = (FolderPathListView) parent;
		File pathFile = pathView.getFile(position);

		if(!pathView.setPath(pathFile)){
			pathView.setPath(mOldPathFile);
			((FolderPathLinearLayout)findViewById(R.id.llPath)).setPath(mOldPathFile);
			Toast.makeText(this, getString(R.string.toast_cannot_ls, pathFile.getName()), Toast.LENGTH_SHORT).show();
		}else{
			((FolderPathLinearLayout)findViewById(R.id.llPath)).setPath(pathFile);
			mOldPathFile = pathFile;
		}

		mPath = pathFile.getPath();
	}

	@Override
	public void onPathChanged(File pathFile) {
		((FolderPathListView)findViewById(R.id.lvFolder)).setPath(pathFile);
		mOldPathFile = pathFile;
		mPath = pathFile.getPath();
	}

	public void setupButtons(){

		// 移動先選択画面
		if (getIntent().getBooleanExtra(PARAM_CHOOSER, false)) {

//			((EditText)findViewById(R.id.txtFolderName)).setVisibility(View.GONE);
			((EditText)findViewById(R.id.txtFolderName)).setVisibility(View.GONE);
			((Button)findViewById(R.id.btnExecute)).setVisibility(View.VISIBLE);
			((Button)findViewById(R.id.btnExecute)).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
					intent.putExtra(RESULT_PATH, llPath.getPath());
					setResult(RESULT_OK, intent);
					finish();
				}
			});

		// 新規フォルダ作成画面
		} else {

			((Button)findViewById(R.id.btnNewFolder)).setVisibility(View.INVISIBLE);

			mNewFolder = (EditText)findViewById(R.id.txtFolderName);
			
			mBtnOk = (Button)findViewById(R.id.btnExecute);
			mBtnOk.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					try {
						// 新しいフォルダの追加
//						EditText txtName = (EditText)findViewById(R.id.txtFolderName);
						if (mNewFolder != null && mNewFolder.getText().toString().length() > 0) {
							newFolder(mNewFolder.getText().toString());
						}
					} catch (IOException e) {
//						e.printStackTrace();		/*$debug$*/
					}
				}


			});

			// TextWatcherを設定
			TextWatcher textWatcher = new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// テキストが入力されている間、OKボタンを有効化
					if (!TextUtils.isEmpty(mNewFolder.getText().toString().trim())) {
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
			
			mNewFolder.addTextChangedListener(textWatcher);
			mBtnOk.setEnabled(false);
		}

		// 新しいフォルダボタン
		((Button)findViewById(R.id.btnNewFolder)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(getApplicationContext(), SelectFolderActivity.class);
				intent.putExtra(SelectFolderActivity.PARAM_TITLE, getString(R.string.folder_title_create));
				intent.putExtra(SelectFolderActivity.PARAM_CHOOSER, false);
				intent.putExtra(SelectFolderActivity.PARAM_START_PATH, llPath.getPath());
				startActivity(intent);

			}
		});

		// キャンセルボタン
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// 画面切り替え時、リストのレイアウトを再セット
		setLayoutParam();
	}

	/**
	 * パスリストのレイアウトを再設定します
	 */
	public void setLayoutParam() {
		if (mLvFolder == null ) return;

		DisplayMetrics met = getResources().getDisplayMetrics();

		float height = met.heightPixels - 300 * met.density;

		mLvFolder.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				(int)height
			));
	}


	/**
	 * 新しいフォルダを作成し、ダミーの画像ファイルを作成します
	 *
	 * @param folderName	フォルダ名
	 * @throws IOException
	 */
	private void newFolder(String folderName) throws IOException{

		// フォルダ名をフォーマット
		String newName = deleteTopDot(folderName).replace("\n", "").trim();

		newName = newName.replaceAll("[　|\\s]+", "");
		
		File newFolder = new File(mPath, newName);

		// フォルダ作成
		// FIXME 255文字以内なのに、androidで見えなくなってしまう。暫定対応として100文字制限をかけます。
		if(newFolder.toString().length() > 100 || !newFolder.mkdir()){
			Toast.makeText(this, getResources().getString(R.string.folder_message_new_failure), Toast.LENGTH_LONG).show();
		} else {

			if (getIntent().getBooleanExtra(PARAM_CREATE_DUMMY, true)) {
				// 一覧に表示するため、ダミーの画像ファイルを作成したフォルダに登録
				File dummyImage = new File(newFolder.getPath(), ApplicationDefine.DUMMY_IMAGE_FILENAME);
				OutputStream os = new FileOutputStream(dummyImage);

				Bitmap bmp;
				Bitmap bmp2;

				try {
					int w = DUMMY_IMAGE_SIZE;
					int h = DUMMY_IMAGE_SIZE;

					// bmpはimmutable = 不変のため、コピーして画像操作
					bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

					// ダミー画像作成
					bmp2 = bmp.copy(bmp.getConfig(), true);
					bmp2.eraseColor(Color.DKGRAY);
					bmp2.compress(CompressFormat.PNG, 100, os);
					os.flush();

				} catch (FileNotFoundException e) {
//					e.printStackTrace();		/*$debug$*/

				} catch (IOException e) {
//					e.printStackTrace();		/*$debug$*/
				} finally {
					if (os != null) {
						os.close();
					}

					bmp = null;
					bmp2 = null;
				}
			}

			llPath.refreshPath();

			Toast.makeText(this, getResources().getString(R.string.folder_message_new_success), Toast.LENGTH_LONG).show();
			MediaUtil.scanMedia(getApplicationContext(), newFolder, false);
		}
	}

	/**
	 * 新しいフォルダ名　入力ダイアログを表示します
	 */
	public void showNewFolderDialog() {
		// 新しいフォルダダイアログ

		final EditText changeText = new EditText(this);
		changeText.setInputType(InputType.TYPE_CLASS_TEXT);
		changeText.setText("");
		changeText.setHint(R.string.folder_hint_new);
//		mNewFolder.setText(NEW_FOLDER_NAME);
		
		new AlertDialog.Builder(this)
			.setTitle(getResources().getString(R.string.folder_title_change_name))
			.setView(changeText)
			.setPositiveButton(android.R.string.cancel, null)
			.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						// 新しいフォルダの追加
						newFolder(changeText.getText().toString());
					} catch (IOException e) {
//						e.printStackTrace();		/*$debug$*/
					}
				}
			})
			.show();
	}

	/**
	 * 文字列の先頭の「.」を削除します
	 *
	 * @param text	文字列
	 * @return	フォーマット文字列
	 */
	public String deleteTopDot(String text) {

		int textSize = text.length();
		String result = text;

		// 先頭からチェックを行い、「.」があれば除去
		for (int i = 0; i < textSize; i++) {
			if (".".equals(result.substring(0, 1))) {
				result = text.substring(i + 1, text.length());
			} else {
				break;
			}
		}

		return result;
	}
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		mMenuInfo = (AdapterContextMenuInfo) menuInfo;

	    menu.setHeaderTitle(getString(R.string.folder_context_title));
	    // フォルダ名変更
	    menu.add(0, 0, 0, getString(R.string.folder_title_change_other_name)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				FolderPathListView list = (FolderPathListView)mLvFolder;
				mTargetPath = list.getFile(mMenuInfo.position).toString();
				// フォルダ名変更ダイアログを表示
				showChangeNameDialog();
				return false;
			}
		});
	    // フォルダ削除
	    menu.add(0, 1, 0, getString(R.string.folder_title_delete_folder)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				FolderPathListView list = (FolderPathListView)mLvFolder;
				mTargetPath = list.getFile(mMenuInfo.position).toString();
				showDeleteDialog();
				
				return false;
			}
		});
	    
	}

	
	/**
	 * 名前変更ダイアログを表示します
	 */
	public void showChangeNameDialog() {
		
//		mChangeText = new EditText(this);
//		mChangeText.setInputType(InputType.TYPE_CLASS_TEXT);
//		mChangeText.setText("");
//		mChangeText.setHint(R.string.folder_hint_new);
//		
//		new AlertDialog.Builder(SelectFolderActivity.this)
//			.setTitle(getResources().getString(R.string.folder_title_change_name))
//			.setIcon(android.R.drawable.ic_dialog_info)
//			.setView(mChangeText)
//			.setMessage(getResources().getString(R.string.folder_message_change_name))
//			.setPositiveButton(android.R.string.cancel, null)
//			.setNegativeButton(android.R.string.ok, )
//			.show();
		
		final SimpleEditDialog dialog = new SimpleEditDialog(SelectFolderActivity.this);
		dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		// 文字数制限(FAT-32は255文字まで、 255 - .jpeg.secret(12文字) = 243)
		int maxLength = 243;

		dialog.mTxtEdit.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxLength) });
		dialog.mTxtTitle.setText(getResources().getString(R.string.folder_title_change_other_name));
		dialog.mTxtView.setText(getResources().getString(R.string.folder_message_change_name));
		dialog.mTxtEdit.setHint(R.string.folder_hint_new);
		
		dialog.mBtnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				NameChangeTask task = new NameChangeTask(dialog.mTxtEdit.getText().toString());
				task.execute();
				dialog.dismiss();
			}
		});
		dialog.mBtnOk.setEnabled(false);
		dialog.mTxtEdit.addTextChangedListener(dialog.mTextWatcher);
		dialog.mBtnChansel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				dialog.dismiss();
			}
		});
		
		dialog.show();
		
	}
	
//	/** フォルダ名変更リスナー */
//	DialogInterface.OnClickListener mNameChangeListener = new DialogInterface.OnClickListener() {
//		
//		@Override
//		public void onClick(DialogInterface dialog, int which) {
//			
//			NameChangeTask task = new NameChangeTask();
//			task.execute();
//		}
//	};
	
	
	/**
	 * フォルダ名変更タスク
	 */
	public class NameChangeTask extends AsyncTask<String, Void, Boolean> {
		
		String mChangeName;
		
		String mErrorExist = null;
		
		ProgressDialog mProgress;
		
		public NameChangeTask(String name) {
			mChangeName = name;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			mProgress = new ProgressDialog(SelectFolderActivity.this);

			mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgress.setMessage(getString(R.string.folder_message_progress));
			mProgress.setCancelable(false);
			
			mProgress.show();
		}
		
		@Override
		protected Boolean doInBackground(String...file) {
			
			// フォルダ名変更
			String newName = deleteTopDot(mChangeName);
			
			newName = newName.replaceAll("[　|\\s]+", "").trim();
			
			File pastFile = new File(mTargetPath);
			
			// FIXME 255文字以内なのに、androidで見えなくなってしまう。暫定対応として100文字制限をかけます。
			if (newName.length() > 100) return false;
						
			
			String newFolderPath = IOUtil.changeFolderName(newName, pastFile);

			boolean result = false;
			if (newFolderPath == null) {

				String newPath = pastFile.getParent() + "/" + newName.trim();
				
				File newFile = new File(newPath);
				
				if (newFile.exists()) mErrorExist = getString(R.string.folder_message_change_existing);
				
			} else {
				
				// DBのデータを書き換える
				boolean metadataChange = MediaMetaDataAccessor.updateDirPath(mDatabase, mTargetPath, newFolderPath);
				boolean indexChange = MediaIndexesAccessor.updateIndexesPath(OpenHelper.cache.getDatabase(), mTargetPath, newFolderPath);
				
				MediaStoreOperation.scanAndDeleteMediaStoreEntry(getApplicationContext(), new File(mTargetPath), new File(newFolderPath), true);
				
				result = indexChange && metadataChange;
			}
			
			return result;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			mProgress.dismiss();
			
			String message;
			
			if (result) {
				message = getString(R.string.folder_message_change_name_success);
			} else {
				if (mErrorExist == null) {
					message = getString(R.string.folder_message_change_name_failure);
				} else {
					message = mErrorExist;
				}
			}
			
			// 終了メッセージ表示
			Toast.makeText(SelectFolderActivity.this, message, Toast.LENGTH_SHORT).show();
			mLvFolder.setPath(new File(mPath));
			
		}
	}
	
	/**
	 * 削除ダイアログを表示し、フォルダの削除を行います
	 */
	public void showDeleteDialog() {
		
		// メッセージ作成
		String message = getResources().getString(R.string.folder_message_delete);
		message = message.replace("[0]", mTargetPath);
		
		new AlertDialog.Builder(SelectFolderActivity.this)
		.setTitle(getResources().getString(R.string.folder_title_delete))
		.setIcon(android.R.drawable.ic_dialog_info)
		.setMessage(message)
		.setPositiveButton(getResources().getString(android.R.string.cancel), null)
		.setNegativeButton(getResources().getString(android.R.string.ok), mDeleteListener)
		.show();
	}

	
	/** フォルダ内メディアファイルの削除リスナー */
	DialogInterface.OnClickListener mDeleteListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			
			// メディアファイルを削除
			DeleteTask task = new DeleteTask(mTargetPath);
			task.execute();
		}
	};
	
	/**
	 * メディアファイル削除タスク
	 */
	public class DeleteTask extends AsyncTask<Void, Void, Boolean> {

		/** シークレット設定/解除フラグ */
		private String mBasePath;
		
		
		public DeleteTask(String basePath) {
			mBasePath = basePath;
		}
		
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			// 隠しフォルダ表示状態
			boolean containsDotFile = PreferenceUtil.getBooleanPreferenceValue(SelectFolderActivity.this, ApplicationDefine.PREF_HIDDEN_FOLDER_DISPLAY, false);

			// メディアファイルの削除
			return MediaUtil.deleteAllMedia(mDatabase, mBasePath, containsDotFile);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			String message;
			
			if (result) {
				message = getString(R.string.folder_message_delete_success);
				
				File base = new File(mBasePath);
				
				// メディアファイル以外のファイルも、フォルダ内に存在しなければ、フォルダそのものを削除
				if (base.listFiles().length == 0) {
					if(!base.delete()) {
						message = getString(R.string.folder_message_delete_folder);
					}
				}
				
			} else {
				message = getString(R.string.folder_message_delete_failure);
			}
			
			// 終了メッセージ表示
			Toast.makeText(SelectFolderActivity.this, message, Toast.LENGTH_SHORT).show();
			
			mLvFolder.setPath(new File(mPath));
		}
	}
	
	
}