package jp.co.johospace.jsphoto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.onlineservice.OnlineMediaServiceClient.AuthorizationHandler;
import jp.co.johospace.jsphoto.onlineservice.OnlineMediaServiceClient.InteractionCallback;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.PicasaClient;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.AlbumEntry;
import jp.co.johospace.jsphoto.util.IOIterator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Picasa同期対象
 */
public class PicasaSyncTargetActivity extends AbstractActivity implements View.OnClickListener, AuthorizationHandler {
	private static final String tag = PicasaSyncTargetActivity.class.getName();
	
	public static final String EXTRA_ACCOUNT =
			PicasaSyncTargetActivity.class.getName().concat(".EXTRA_ACCOUNT");
	
	public static final String EXTRA_LOCAL_FOLDER =
			PicasaSyncTargetActivity.class.getName().concat(".EXTRA_LOCAL_FOLDER");
	public static final String EXTRA_PICASA_ALBUM =
			PicasaSyncTargetActivity.class.getName().concat(".EXTRA_PICASA_ALBUM");
	
	private static final int DIALOG_PROGRESS_AUTHZ = 1;
	private static final int DIALOG_PROGRESS_GET_ALBUM = 2;
	private static final int DIALOG_ALBUM_NAME = 3;
	private static final int DIALOG_PROGRESS_NEW_ALBUM = 4;
	
	private static final int REQUEST_FOLDER_CHOOSER = 1;
	
	private PicasaClient mClient;
	
	private TextView mTxtLocalFolder;
	private Button mBtnSelectLocal;
	private Spinner mSpnPicasaAlbum;
	private Button mBtnNewAlbum;
	private Button mBtnOk;
	private Button mBtnCancel;
	private void findViews() {
		mTxtLocalFolder = (TextView) findViewById(R.id.txt_local_folder);
		mBtnSelectLocal = (Button) findViewById(R.id.btn_select_local);
		mSpnPicasaAlbum = (Spinner) findViewById(R.id.spn_picasa_album);
		mBtnNewAlbum = (Button) findViewById(R.id.btn_new_album);
		mBtnOk = (Button) findViewById(R.id.btn_ok);
		mBtnCancel = (Button) findViewById(R.id.btn_cancel);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.picasa_sync_target);
		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes(
				(android.view.WindowManager.LayoutParams) params);
		findViews();
		
		mAdapter = new ArrayAdapter<AlbumItem>(this,
				android.R.layout.simple_spinner_item, android.R.id.text1);
		mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpnPicasaAlbum.setAdapter(mAdapter);
		mSpnPicasaAlbum.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				checkEntry();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				checkEntry();
			}
		});
		
		mBtnSelectLocal.setOnClickListener(this);
		mBtnNewAlbum.setOnClickListener(this);
		mBtnOk.setOnClickListener(this);
		mBtnCancel.setOnClickListener(this);
		
		if (getIntent().hasExtra(EXTRA_ACCOUNT)) {
			mClient = new PicasaClient(this,
					getIntent().getStringExtra(EXTRA_ACCOUNT));
		} else {
			finish();
			return;
		}
		
		if (getIntent().hasExtra(EXTRA_LOCAL_FOLDER)) {
			mTxtLocalFolder.setText(getIntent().getStringExtra(EXTRA_LOCAL_FOLDER));
		} else {
			mTxtLocalFolder.setText(null);
		}
		
		showDialog(DIALOG_PROGRESS_AUTHZ);
		mClient.authorize(this, true);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		checkEntry();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_select_local: {
			Intent intent = new Intent(this, SelectFolderActivity.class);
			intent.putExtra(SelectFolderActivity.PARAM_CHOOSER, true);
			String path = mTxtLocalFolder.getText().toString();
			if (TextUtils.isEmpty(path)) {
				path = Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_DCIM).getAbsolutePath();
			}
			intent.putExtra(SelectFolderActivity.PARAM_START_PATH, path);
			intent.putExtra(SelectFolderActivity.PARAM_TITLE,
					mBtnSelectLocal.getText().toString());
			intent.putExtra(SelectFolderActivity.PARAM_CREATE_DUMMY, false);
			startActivityForResult(intent, REQUEST_FOLDER_CHOOSER);
		}
		break;
		
		case R.id.btn_ok: {
			Intent result = new Intent();
			result.putExtra(EXTRA_ACCOUNT,
					mClient.getServiceAccount());
			result.putExtra(EXTRA_LOCAL_FOLDER,
					mTxtLocalFolder.getText().toString());
			result.putExtra(EXTRA_PICASA_ALBUM,
					getSelectedAlbumID());
			setResult(RESULT_OK, result);
			finish();
		}
		break;
		
		case R.id.btn_cancel: {
			finish();
		}
		
		case R.id.btn_new_album: {
			showDialog(DIALOG_ALBUM_NAME);
		}
		break;
		}
	}
	
	
	private class AlbumItem {
		final AlbumEntry mEntry;
		public AlbumItem(AlbumEntry entry) {
			super();
			mEntry = entry;
		}
		
		@Override
		public String toString() {
			return mEntry.getName();
		}
	}
	
	private ArrayAdapter<AlbumItem> mAdapter;
	
	private String getSelectedAlbumID() {
		AlbumItem item = (AlbumItem) mSpnPicasaAlbum.getSelectedItem();
		return item == null ? null : item.mEntry.getID();
	}
	
	private boolean selectAlbum(String albumId) {
		final int count = mAdapter.getCount();
		for (int pos = 0; pos < count; pos++) {
			AlbumItem item = mAdapter.getItem(pos);
			if (item != null && albumId.equals(item.mEntry.getID())) {
				mSpnPicasaAlbum.setSelection(pos);
				return true;
			}
		}
		return false;
	}
	
	private class LoadAlbumTask extends AsyncTask<Void, Void, List<AlbumEntry>> {
		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_PROGRESS_GET_ALBUM);
		}
		
		@Override
		protected List<AlbumEntry> doInBackground(Void... params) {
			try {
				ArrayList<AlbumEntry> albums = new ArrayList<AlbumEntry>();
				IOIterator<AlbumEntry> itr = mClient.iterateDirectory();
				try {
					while (itr.hasNext()) {
						albums.add(itr.next());
					}
				} finally {
					itr.terminate();
				}
				
				return albums;
			} catch (Exception e) {
//				Log.e(tag, "failed to get albums.", e);		/*$debug$*/
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<AlbumEntry> result) {
			dismissDialog(DIALOG_PROGRESS_GET_ALBUM);
			if (result == null) {
				;
			} else {
				mAdapter.clear();
				for (AlbumEntry entry : result) {
					mAdapter.add(new AlbumItem(entry));
				}
				mAdapter.notifyDataSetChanged();
				
				if (getIntent().getStringExtra(EXTRA_PICASA_ALBUM) != null) {
					selectAlbum(getIntent().getStringExtra(EXTRA_PICASA_ALBUM));
				} else {
					;
				}
			}
			checkEntry();
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DIALOG_PROGRESS_AUTHZ: {
			ProgressDialog d = new ProgressDialog(this);
			d.setCancelable(false);
			d.setTitle(R.string.picasa_sync_target_progress_auth);
			return d;
		}
		case DIALOG_PROGRESS_GET_ALBUM: {
			ProgressDialog d = new ProgressDialog(this);
			d.setCancelable(false);
			d.setTitle(R.string.picasa_sync_target_progress_get_album);
			return d;
		}
		case DIALOG_ALBUM_NAME:
			final EditText edtAlbumName = new EditText(this);
			edtAlbumName.setText(
					getString(R.string.picasa_sync_target_initial_album_name, Build.MODEL));
			return new AlertDialog.Builder(this)
				.setCancelable(true)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setView(edtAlbumName)
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (TextUtils.isEmpty(edtAlbumName.getText())) {
							;
						} else {
							new NewAlbumTask().execute(
									edtAlbumName.getText().toString());
						}
					}
				})
				.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.create();
		case DIALOG_PROGRESS_NEW_ALBUM: {
			ProgressDialog d = new ProgressDialog(this);
			d.setCancelable(false);
			d.setTitle(R.string.picasa_sync_target_progress_create_album);
			return d;
		}
		}
		return super.onCreateDialog(id, args);
	}

	@Override
	public void startInteraction(Intent intent, InteractionCallback callback) {
		finish();
	}

	@Override
	public void authorizationFinished(String account, boolean authorized) {
		if (authorized) {
			dismissDialog(DIALOG_PROGRESS_AUTHZ);
			new LoadAlbumTask().execute();
		} else {
			finish();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_FOLDER_CHOOSER:
			if (resultCode == RESULT_OK) {
				mTxtLocalFolder.setText(
						data.getStringExtra(SelectFolderActivity.RESULT_PATH));
			}
			checkEntry();
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private boolean checkEntry() {
		if (!TextUtils.isEmpty(mTxtLocalFolder.getText())
				&& !TextUtils.isEmpty(getSelectedAlbumID())) {
			mBtnOk.setEnabled(true);
			return true;
		} else {
			mBtnOk.setEnabled(false);
			return false;
		}
	}
	
	private class NewAlbumTask extends AsyncTask<String, Void, AlbumEntry> {
		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_PROGRESS_NEW_ALBUM);
		}
		
		@Override
		protected AlbumEntry doInBackground(String... params) {
			try {
				return mClient.insertAlbum(params[0]);
			} catch (IOException e) {
//				Log.e(tag, "failed to create album.", e);		/*$debug$*/
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(AlbumEntry result) {
			removeDialog(DIALOG_PROGRESS_NEW_ALBUM);
			removeDialog(DIALOG_ALBUM_NAME);
			if (result != null) {
				mAdapter.add(new AlbumItem(result));
				selectAlbum(result.gphotoId);
				checkEntry();
			} else {
				Toast.makeText(getApplicationContext(),
						R.string.picasa_sync_target_failed_to_create_album,
						Toast.LENGTH_LONG).show();
			}
		}
	}
}
