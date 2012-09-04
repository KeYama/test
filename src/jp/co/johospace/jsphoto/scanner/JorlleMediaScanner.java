package jp.co.johospace.jsphoto.scanner;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.util.MediaUtil;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class JorlleMediaScanner {
	public static String DOT_NOMEDIA = ".nomedia";
	
	/**
	 * メディアやフォルダが見つかったときに呼ばれるリスナ
	 * 
	 */
	public static interface OnFoundListener{
		/**
		 * メディアが見つかった
		 * 
		 * @param file
		 */
		public void onFound(File file);
		
		/**
		 * メディアが入ったフォルダの中身を列挙開始時に呼ばれる
		 * 
		 * @param folder
		 */
		public void onStartFolder(File folder);
		
		/**
		 * 現在のフォルダのメディア列挙終了時に呼ばれる
		 * 
		 * @param folder
		 * @param size
		 */
		public void onEndFolder(File folder, int size);
		
		/**
		 * 検索終了
		 * 
		 */
		public void onComplete();
	}
	
	/**
	 * 日付新しい順
	 * 
	 */
	public static class DateAscender implements Comparator<File>{
		//TODO
		@Override
		public int compare(File lhs, File rhs) {

			long d = rhs.lastModified() - lhs.lastModified();
			if(d > 0)return 1;
			if(d < 0)return -1;
			
			// 同時刻に作成されたものは、名前の昇順にて比較
			return lhs.getName().compareTo(rhs.getName());
//			return 0;
		}
	}
	
	/**
	 * 日付古い順
	 * 
	 */
	public static class DateDescender implements Comparator<File>{
		//TODO
		@Override
		public int compare(File lhs, File rhs) {
			long d = lhs.lastModified() - rhs.lastModified();
			if(d > 0)return 1;
			if(d < 0)return -1;
			
			// 同時刻に作成されたものは、名前の昇順にて比較
			return lhs.getName().compareTo(rhs.getName());
//			return 0;
		}
	}

	/**
	 * 名前昇順
	 * 
	 */
	public static class NameAscender implements Comparator<File>{

		@Override
		public int compare(File lhs, File rhs) {
			return lhs.getName().compareTo(rhs.getName());
		}
		
	}
	
	/**
	 * 名前降順
	 * 
	 */
	public static class NameDescender implements Comparator<File>{

		@Override
		public int compare(File lhs, File rhs) {
			return rhs.getName().compareTo(lhs.getName());
		}
		
	}

	
	private final InternalHandler mHandler;
	
	private boolean mScanNomedia;
	private boolean mScanSecret;
	private boolean mScanSubfolder;
	private File mBaseFolder;
	private OnFoundListener mListener;
	private AsyncTask<Void, File, Void> mTask;
	private Comparator<File> mComparator;
	private int mMaxCallback;
//	private static final String[] EXT = 
//		{".jpg", ".jpeg", ".3gp", ".mp4", ".m4a", ".gif", ".png", ".bmp" };
//	private static String[] EXT_SECRET;
	
	private List<JorlleMediaFilter> mFilters;
	private DateFilter mDateFilter;
	private DBFilter mDBFilter;
	
	public JorlleMediaScanner newWithSameSetting(){
		JorlleMediaScanner r = new JorlleMediaScanner();
		r.mScanNomedia = mScanNomedia;
		r.mScanSecret = mScanSecret;
		r.mScanSubfolder = mScanSubfolder;
		r.mBaseFolder = mBaseFolder;
		r.mComparator = mComparator;
		r.mMaxCallback = mMaxCallback;
	
		r.mFilters = mFilters;
		r.mDateFilter = mDateFilter;
		r.mDBFilter = mDBFilter;
		
		return r;
	}
	
	public JorlleMediaScanner(){
		this(null);
	}
	
	public JorlleMediaScanner(Looper callbackLooper){
		mComparator = new DateAscender();
		mScanNomedia = false;
		mScanSecret = false;
		mScanSubfolder = false;
		mMaxCallback = Integer.MAX_VALUE;
		mBaseFolder = Environment.getExternalStorageDirectory();
//		if(EXT_SECRET == null){
//			EXT_SECRET = new String[EXT.length];
//			for(int n = 0; n < EXT.length; n++){
//				EXT_SECRET[n] = EXT[n] + ApplicationDefine.SECRET;
//			}
//		}
		
		if (callbackLooper == null) {
			mHandler = new InternalHandler();
		} else {
			mHandler = new InternalHandler(callbackLooper);
		}
	}
	
	/**
	 * 破棄。Activity終了時に呼ぶ事。
	 * 
	 */
	public void dispose(){
		if(mTask != null)mTask.cancel(false);
		mListener = null;
	}
	
	/**
	 * メディアファイルのソートアルゴリズム
	 * 
	 * @param comp
	 * @return
	 */
	public JorlleMediaScanner sort(Comparator<File> comp){
		if(mTask != null)return null;
		mComparator = comp;
		
		return this;
	}
	
	/**
	 * １フォルダ内でonFoundが呼ばれる最大回数
	 * 
	 * @param count
	 * @return
	 */
	public JorlleMediaScanner maxCallback(int count){
		if(mTask != null)return null;
		mMaxCallback = count;
		
		return this;
	}
	
	/**
	 * 検索タグを追加する。タグはOR
	 * 
	 * @param tag
	 * @return
	 */
	public JorlleMediaScanner addTag(String tag){
		if(mTask != null) return null;
		if(mDBFilter==null){
			if(mFilters==null)mFilters = new ArrayList<JorlleMediaFilter>();
			mDBFilter = new DBFilter();
			mFilters.add(mDBFilter);
		}

		mDBFilter.addTag(tag);
		return this;
	}
	
	/**
	 * お気に入りを検索する
	 * 
	 * @param tag
	 * @return
	 */
	public JorlleMediaScanner favorite(){
		if(mTask != null) return null;
		if(mDBFilter==null){
			if(mFilters==null)mFilters = new ArrayList<JorlleMediaFilter>();
			mDBFilter = new DBFilter();
			mFilters.add(mDBFilter);
		}
		
		mDBFilter.filterFavorite(true);
		return this;
	}
	
	
	/**
	 * この日付よりも後の日付をターゲットにして検索する
	 * endTimeと同時に指定した場合はandを取って検索
	 * 
	 * @param date
	 * @return
	 */
	public JorlleMediaScanner startTime(Calendar date){
		if(mTask != null)return null;
		if(mDateFilter==null){
			if(mFilters==null)mFilters = new ArrayList<JorlleMediaFilter>();
			mDateFilter = new DateFilter();
			mFilters.add(mDateFilter);
		}
		mDateFilter.setBegin(date);
		return this;
	}
	
	/**
	 * この日付よりも前の日付をターゲットにして検索する。
	 * startTimeと同時に指定した場合はandを取って検索
	 * 
	 * @param date
	 * @return
	 */
	public JorlleMediaScanner endTime(Calendar date){
		if(mTask != null)return null;
		if(mDateFilter==null){
			if(mFilters==null)mFilters = new ArrayList<JorlleMediaFilter>();
			mDateFilter = new DateFilter();
			mFilters.add(mDateFilter);
		}
		
		mDateFilter.setEnd(date);
		return this;
	}
	
	/**
	 * シークレットファイルだけを対象にして検索する
	 * 
	 * @return
	 */
	public JorlleMediaScanner scanOnlySecret(){
		if(mTask != null)return null;
		if(mFilters == null){
			mFilters = new ArrayList<JorlleMediaFilter>();
		}
		mFilters.add(new SecretFilter());
		mScanSecret = true;
		return this;
	}
	
	/**
	 * .nomediaを考慮するか否か
	 * メソッドチェイン
	 * 
	 * @param flag
	 * @return
	 */
	public JorlleMediaScanner scanNomedia(boolean flag){
		if(mTask != null)return null;
		mScanNomedia = flag;
		return this;
	}
	
	/**
	 * シークレットを考慮するか否か
	 * メソッドチェイン
	 * 
	 * @param flag
	 * @return
	 */
	public JorlleMediaScanner scanSecret(boolean flag){
		if(mTask != null)return null;
		mScanSecret = flag;
		return this;
	}
	
	/**
	 * サブフォルダの検索を行うか否か
	 * メソッドチェイン
	 * 
	 * @param flag
	 * @return
	 */
	public JorlleMediaScanner scanSubfolder(boolean flag) {
		if(mTask != null)return null;
		mScanSubfolder = flag;
		return this;
	}
	
	/**
	 * スキャンのベースフォルダ指定
	 * メソッドチェイン
	 * 
	 * @param folder
	 * @return
	 */
	public JorlleMediaScanner baseFolder(File folder){
		if(mTask != null)return null;
		if(!folder.isDirectory()){
			throw new IllegalArgumentException();
		}
		mBaseFolder = folder;
		return this;
	}
	
	/**
	 * スキャンのベースフォルダ指定（文字列版）
	 * メソッドチェイン
	 * 
	 * @param path
	 * @return
	 */
	public JorlleMediaScanner baseFolder(String path){
		return baseFolder(new File(path));
	}
	
	/**
	 * メディアファイルの列挙を開始
	 * 
	 * @param listener
	 * @return
	 */
	public boolean findMedia(OnFoundListener listener){
		if(mTask != null)return false;
		
		mListener = listener;
		mTask = new FindMediaTask();
		mTask.execute((Void)null);
		return true;
	}
	
	/**
	 * アイテムが見つかったとき、リスナを呼び出すメソッド。
	 * 
	 * @param file
	 */
	private void onFound(final File file){
		Message msg = Message.obtain();
		msg.arg1 = InternalHandler.ON_FOUND;
		msg.obj = file;
		
		mHandler.sendMessage(msg);
	}

	/**
	 * 列挙開始時に、リスナを呼び出すメソッド
	 * 
	 * @param file
	 */
	private void onStartFolder(final File file){
		Message msg = Message.obtain();
		msg.arg1 = InternalHandler.ON_START;
		msg.obj = file;
		mHandler.sendMessage(msg);
	}
	
	/**
	 * 列挙終了時に、リスナを呼び出すメソッド。
	 * 
	 * @param file
	 */
	private void onEndFolder(final File file, int size){
		Message msg = Message.obtain();
		msg.arg1 = InternalHandler.ON_END;
		msg.arg2 = size;
		msg.obj = file;
		mHandler.sendMessage(msg);
	}
	
	/**
	 * アイテム列挙が完了したとき、リスナを呼び出すメソッド
	 * 
	 */
	private void onComplete(){
		if(mListener != null)mListener.onComplete();
	}
	
	public static boolean isMedia(String name) {
		// MIMEタイプが image/xxxx と　video/xxxx　をメディアとして採用する
		String mime = MediaUtil.getMimeTypeFromPath(name);
		return mime != null && (mime.startsWith("image") || mime.startsWith("video"));
	}
	
	public static final FilenameFilter MEDIA_FILENAME_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String filename) {
			return isMedia(filename);
		}
	};
	
	public static final FileFilter MEDIA_FILE_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isFile() && isMedia(pathname.getName());
		}
	};
	
	/**
	 * 指定ファイルネームがメディアファイルか否か
	 * 
	 * @param name
	 * @return
	 */
	private boolean isMedia0(String name){
		// .nomedia を表示しない状態でのドットファイルは表示しない
		if(!mScanNomedia && ".".equals(name.substring(0, 1))) {
			return false;
		}

		if(mScanSecret){
//			int length = EXT.length;
//			for(int n = 0; n < length; ++n){
//				if(name.toLowerCase().endsWith(EXT[n]))return true;
//				if(name.toLowerCase().endsWith(EXT_SECRET[n]))return true;
//			}
			if (name.endsWith(ApplicationDefine.SECRET)) {
				name = name.substring(0,
						name.length() - ApplicationDefine.SECRET.length());
			}
			
		}else{
//			int length = EXT.length;
//
//			for(int n = 0; n < length; ++n){
//				if(name.toLowerCase().endsWith(EXT[n]))return true;
//			}
		}
		
//		return false;
		return isMedia(name);
	}
	
	/**
	 * 処理をUIスレッドに戻すハンドラ
	 */
	private class InternalHandler extends Handler{
		public static final int ON_START = 1;
		public static final int ON_END = 2;
		public static final int ON_FOUND = 3;

		public InternalHandler() {
			super();
		}
		
		public InternalHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			if(mListener != null){
				switch(msg.arg1){
				case ON_START:
					mListener.onStartFolder((File)msg.obj);
					break;
					
				case ON_END:
					mListener.onEndFolder((File)msg.obj, msg.arg2);
					break;
					
				case ON_FOUND:
					mListener.onFound((File)msg.obj);
				}
			}
		}
	}
	
	private class FindMediaTask extends AsyncTask<Void, File, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			enumMedia(mBaseFolder);

			return null;
		}
		
		public void enumMedia(File base){
			// 隠しフォルダを表示する設定だった場合「.」がついたフォルダも表示する
			if(!mScanNomedia && ".".equals(base.getName().substring(0, 1))){
				return;
			}
			
			File[] files = base.listFiles();
			
			if(files == null)return;
			List<File> mediaFiles = new ArrayList<File>();
			List<File> folders = new ArrayList<File>();
			boolean nomedia = false;
			for(File f: files){
				if (f.isDirectory()) {
					folders.add(f);
				} else {
					String name = f.getName();
					if(!mScanNomedia && name.equals(DOT_NOMEDIA)){

						// 隠しフォルダは、配下のフォルダ・ファイルも全て非表示に
						nomedia = true;
						//既に列挙してるかもしれないので、クリア
						folders.clear();
						break;
					}else if(isMedia0(name)){
						mediaFiles.add(f);
					}
				}
			}

			mediaFiles = filterMedia(mediaFiles);
			
			if(!nomedia && (mediaFiles.size() != 0)){
				Collections.sort(mediaFiles, mComparator);
				onStartFolder(base);
				
				int count = mMaxCallback;
				for(File f: mediaFiles){
					count--;
					
					if(count < 0)break;
					
					onFound(f);
					if(isCancelled())return;
				}

				onEndFolder(base, mediaFiles.size());
			}
			
			if(isCancelled())return;
			
			//GCを効かす
			mediaFiles = null;
			files = null;
			
			// サブフォルダを再帰検索
			if (mScanSubfolder) {
				for(File f: folders){
					enumMedia(f);
				}
			}
		}
		
		private List<File> filterMedia(List<File> files){
			if(mFilters == null)return files;
			
			List<File> result = new ArrayList<File>();
			for(File f: files){
				boolean accept = true;
				for(JorlleMediaFilter filter: mFilters){
					if(!filter.filter(f)){
						accept = false;
						break;
					}
				}
				if(accept){
					result.add(f);
				}
			}
			
			return result;
		}

		@Override
		protected void onCancelled() {
			onComplete();
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Void result) {
			onComplete();
			
			super.onPostExecute(result);
		}
		
	}
	
	/**
	 * 実行中のスキャンの完了を待ちます。
	 */
	public void join() {
		if (mTask != null) {
			try {
				mTask.get();
			} catch (InterruptedException e) {
				;
			} catch (ExecutionException e) {
				;
			}
		}
	}
}
