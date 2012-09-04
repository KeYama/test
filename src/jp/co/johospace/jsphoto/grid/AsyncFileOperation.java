package jp.co.johospace.jsphoto.grid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.util.MediaStoreOperation;
import jp.co.johospace.jsphoto.util.MediaUtil;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.widget.Toast;

public class AsyncFileOperation {
	private Context mContext;
	private ProgressDialog mDialog;
	private AsyncTask<Void, Integer, Void> mExecute;
	private OnCompleteListener mListener;
	
	public interface OnCompleteListener{
		public void onComplete();
		public void onCancel();
	}
	
	public AsyncFileOperation(Context context){
		mContext = context;
		
		mDialog = new ProgressDialog(context);
		mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mDialog.setCancelable(false);
		mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				if(mExecute != null){
					mExecute.cancel(false);
				}
				mDialog.dismiss();
			}
		});
	}
	
	public void setOnCompleteListener(OnCompleteListener listener){
		mListener = listener;
	}
	
	public void dispose(){
		if(mExecute != null)mExecute.cancel(false);
		mDialog.dismiss();
		mDialog = null;
		mListener = null;
	}
	
	public boolean copyToFolder(String targetFolder, Iterable<String> source, int size, boolean isNormal){
		if(mExecute != null){
			Toast.makeText(mContext, mContext.getResources().getString(R.string.image_context_message_unfinished), Toast.LENGTH_LONG).show();
			return false;
		}
		
		mDialog.setTitle("copy to folder");
		mDialog.setMax(size);
		mDialog.setProgress(0);
		mDialog.show();
		mExecute = new CopyToFolderAsyncTask(targetFolder, source, isNormal);
		mExecute.execute((Void)null);
		return true;
	}
	
	public boolean moveToFolder(String targetFolder, Iterable<String> source, int size){
		if(mExecute != null){
			Toast.makeText(mContext, mContext.getResources().getString(R.string.image_context_message_unfinished), Toast.LENGTH_LONG).show();
			return false;
		}
		
		mDialog.setTitle("move to folder");
		mDialog.setMax(size);
		mDialog.setProgress(0);
		mDialog.show();
		mExecute = new MoveToFolderAsyncTask(targetFolder, source);
		mExecute.execute((Void)null);
		return true;
	}
	
	/**
	 * 複数ファイルを削除
	 * @param source 削除対象のパスリスト
	 * @param size 削除対象件数
	 * @return
	 */
	public boolean deleteFiles(Iterable<String> source, int size){
		if(mExecute != null){
			Toast.makeText(mContext, mContext.getResources().getString(R.string.image_context_message_unfinished), Toast.LENGTH_LONG).show();
			return false;
		}
		
		mDialog.setTitle("delete...");
		mDialog.setMax(size);
		mDialog.setProgress(0);
		mDialog.show();
		mExecute = new DeleteFilesAsyncTask(source);
		mExecute.execute((Void)null);
		return true;
	}

	
	private abstract class BaseAsyncTask extends AsyncTask<Void, Integer, Void>{

		@Override
		protected void onCancelled() {
			if(mDialog != null)mDialog.dismiss();
			mExecute = null;
			if(mListener != null)mListener.onCancel();
			
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Void result) {
			if(mDialog != null)mDialog.dismiss();
			mExecute = null;
			if(mListener != null)mListener.onComplete();
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if(mDialog != null)mDialog.setProgress(values[0]);
			super.onProgressUpdate(values);
		}
		
		protected File generateNewName(File old){
			return ExtUtil.createEmptyPathConsideringSecret(old, "");
//			return ExtUtil.createEmptyPath(old, "");
		}
		
		protected boolean copyFile(File from, File to)throws IOException{
			
			boolean result = false;
			
			FileChannel in = null;
			FileChannel out = null;
			
			try{
				in = new FileInputStream(from).getChannel();
				out = new FileOutputStream(to).getChannel();
				in.transferTo(0, in.size(), out);
				
				result = true;
				
			}finally{
				try{
					if(in != null)in.close();
				}catch(IOException e2){}
				try{
					if(out != null)out.close();
				}catch(IOException e2){}
			}
			
			return result;
		}
		

	}
	
	private class MoveToFolderAsyncTask extends BaseAsyncTask{
		
		/** 結果 */
		boolean mResult;
		
		private void moveFile(File from, File to)throws IOException{
			
			if(from.renameTo(to)){
				remakeDb(from, to);
			}
		}
		
		/**
		 *　メタ情報とインデックス情報を書き換えます 
		 */
		private void remakeDb(File from, File to) {
			
			SQLiteDatabase db = OpenHelper.external.getDatabase();
			
			String[] oldPath = new String[]{ from.getParentFile().getAbsolutePath(), from.getName()};
			
			ContentValues meta = new ContentValues();
			meta.put(CMediaMetadata.DIRPATH, to.getParentFile().getAbsolutePath());
			meta.put(CMediaMetadata.NAME, to.getName());
			
			db.update(
				CMediaMetadata.$TABLE, 
				meta, 
				CMediaMetadata.DIRPATH + " = ? AND " + CMediaMetadata.NAME + " = ? ", 
				oldPath
			);
			
			ContentValues index = new ContentValues();
			index.put(CMediaIndex.DIRPATH, to.getParentFile().getAbsolutePath());
			index.put(CMediaIndex.NAME, to.getName());
			
			OpenHelper.cache.getDatabase().update(
				CMediaIndex.$TABLE,
				index,
				CMediaIndex.DIRPATH + " = ? AND " + CMediaIndex.NAME + " = ? ",
				oldPath
			);
			
		}
		
		private String mTargetFolder;
		private Iterable<String> mSource;
		
		public MoveToFolderAsyncTask(String targetFolder, Iterable<String> source){
			mTargetFolder = targetFolder;
			mSource = source;
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			
			mResult = true;
			
			File targetFolder = new File(mTargetFolder);
			
			int count = 1;
			
			for(String sourceName: mSource){
				File sourceFile = new File(sourceName);
				File targetFile = new File(targetFolder, sourceFile.getName());
				
				// 通常・シークレット状態の両ファイル名で判定
				File subTargetFile;
				
				if (ExtUtil.isSecret(targetFile)) {
					subTargetFile = ExtUtil.unSecret(targetFile);
				} else {
					subTargetFile = ExtUtil.toSecret(targetFile);
				}
				
//				if(targetFile.exists()){
				if(targetFile.exists() || subTargetFile.exists()){
					targetFile = generateNewName(targetFile);
				}
				try{
					
					// 移動が成功したら、DB更新
					if (copyFile(sourceFile, targetFile)) {
						
						remakeDb(sourceFile, targetFile);
						MediaUtil.scanMedia(mContext.getApplicationContext(), targetFile, false);
					}

					// copyFile が終了後、移動元のファイルを削除
					// （移動先へ画像自体が出来ていても移動元画像が削除されなければ失敗とみなす。）
					sourceFile.delete();
					MediaStoreOperation.deleteMediaStoreEntry(mContext, sourceFile);
				}catch(IOException e){
					mResult = false;
					cancel(false);
				}
				
				if(isCancelled()) {
					mResult = false;
					return null;
				}
				
				mDialog.setProgress(count);
				
				count++;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		
			if (mResult) {
				Toast.makeText(mContext, mContext.getResources().getString(R.string.image_context_move_success),Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(mContext, mContext.getResources().getString(R.string.image_context_move_failure),Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private class CopyToFolderAsyncTask extends BaseAsyncTask{
		
		private String mTargetFolder;
		private Iterable<String> mSource;
		
		/** 通常コピーフラグ */
		private boolean mIsNormal;
		
		/** 結果 */
		private boolean mResult;

		public CopyToFolderAsyncTask(String targetFolder, Iterable<String> source, boolean isNormal) {
			mTargetFolder = targetFolder;
			mSource = source;
			mIsNormal = isNormal;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			
			mResult = true;
			
			int count = 1;
			
			File targetFolder = new File(mTargetFolder);
			for(String sourceName: mSource){
				File sourceFile = new File(sourceName);
				File targetFile = new File(targetFolder, sourceFile.getName());
				
				// 通常・シークレット状態の両ファイル名で判定
				File subTargetFile;
				
				if (ExtUtil.isSecret(targetFile)) {
					subTargetFile = ExtUtil.unSecret(targetFile);
				} else {
					subTargetFile = ExtUtil.toSecret(targetFile);
				}
				
//				if(targetFile.exists()){
				if(targetFile.exists() || subTargetFile.exists()){
					targetFile = generateNewName(targetFile);
				}
				try{
					copyFile(sourceFile, targetFile);
					MediaUtil.scanMedia(mContext.getApplicationContext(), targetFile, false);
				}catch(IOException e){
					mResult = false;
					cancel(false);
				}
				
				if(isCancelled()) {
					mResult = false;
					return null;
				}
				
				mDialog.setProgress(count);
				
				count++;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			if (mResult) {
				
				if (mIsNormal) {
					Toast.makeText(mContext, mContext.getResources().getString(R.string.image_context_copy_success),Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mContext, mContext.getResources().getString(R.string.image_context_move_sync_success),Toast.LENGTH_SHORT).show();
				} 
			
			} else {
				Toast.makeText(mContext, mContext.getResources().getString(R.string.image_context_copy_failure),Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	
	private class DeleteFilesAsyncTask extends BaseAsyncTask{
		private Iterable<String> mSource;
		
		/** 結果 */
		private boolean mResult;
		
		public DeleteFilesAsyncTask(Iterable<String> source){
			mSource = source;
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			mResult = true;
			
			int count = 1;
			
			for(String sourceName: mSource){
				File f = new File(sourceName);
				
				SQLiteDatabase dbExternal = OpenHelper.external.getDatabase();
				SQLiteDatabase dbCache = OpenHelper.cache.getDatabase();
				
				String index = CMediaIndex.DIRPATH + " = ? AND " + CMediaIndex.NAME + " = ? ";
				String meta = CMediaMetadata.DIRPATH + " = ? AND " + CMediaMetadata.NAME + " = ? ";
				
				dbExternal.delete(CMediaMetadata.$TABLE, meta, new String[]{f.getParent(), f.getName()});
				dbCache.delete(CMediaIndex.$TABLE, index, new String[]{f.getParent(), f.getName()});
				
				if(!f.delete()) {
					mResult = false;
					cancel(false);
				}else{
					MediaStoreOperation.deleteMediaStoreEntry(mContext, f);
				}
					
				if(isCancelled()) {
					mResult = false;
					return null;
				}
				
				mDialog.setProgress(count);
				
				count++;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if (mResult) {
				Toast.makeText(mContext, mContext.getResources().getString(R.string.folder_message_delete_success), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(mContext, mContext.getResources().getString(R.string.folder_message_delete_failure), Toast.LENGTH_SHORT).show();
			}
		}
	}
}
