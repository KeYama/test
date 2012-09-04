package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import jp.co.johospace.jsphoto.grid.ExtUtil;
import jp.co.johospace.jsphoto.grid.ImageShrinker;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public class ImageOpActivity extends Activity{
	
	public static final String RESIZE_SUFFIX = "-resized";
	
	public static final String INTENT_ROTATE = "rotate";
	public static final String INTENT_ROTATE_ORIENTATION = "rotateOrientation";
	
	public static final int 
		ROTATE_90 = 1,
		ROTATE_180 = 2,
		ROTATE_270 = 3;
	
	public static final String INTENT_TARGET_PATH = "targetPath";
	public static final String INTENT_RESIZE = "resize";
	public static final String INTENT_RESIZE_WIDTH = "resizeWidth";
	public static final String INTENT_RESIZE_HEIGHT = "resizeHeight";
	public static final String INTENT_TITLE = "title";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if(intent.getBooleanExtra(INTENT_RESIZE, false)){
			new ShrinkTask(intent.getStringExtra(INTENT_TARGET_PATH)).execute((Void)null);
		}else if(intent.getBooleanExtra(INTENT_ROTATE, false)){
			new RotateTask(intent.getIntExtra(INTENT_ROTATE_ORIENTATION, 0), intent.getStringExtra(INTENT_TARGET_PATH))
				.execute((Void)null);
		}else{
			finish();
		}
	}
	
	
	private abstract class BaseAsyncTask extends AsyncTask<Void, Integer, Void>{
		private ProgressDialog mDialog;
		private int mMaxOp;
		protected BitmapFactory.Options mBitmapOptions;
		private int mError = 0;
		
		public BaseAsyncTask(int maxOp){
			mMaxOp = maxOp;
			mBitmapOptions = new BitmapFactory.Options();
		}
		
		protected void setError(int err){
			mError = err;
		}

		@Override
		protected void onCancelled() {
			setResult(RESULT_CANCELED);
			mDialog.dismiss();
			finish();
			if(mError != 0)
				Toast.makeText(getApplicationContext(), getString(mError), Toast.LENGTH_SHORT).show();	
							
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Void result) {
			setResult(RESULT_OK);
			mDialog.dismiss();
			finish();
						
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			mDialog = new ProgressDialog(ImageOpActivity.this);
			mDialog.setMax(mMaxOp);
			mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			String title = getIntent().getStringExtra(INTENT_TITLE);
			if (title != null) mDialog.setTitle(title);
			
			mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					mBitmapOptions.requestCancelDecode();
					cancel(false);
				}
			});
			mDialog.show();
			
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			mDialog.setProgress(values[0]);
			
			super.onProgressUpdate(values);
		}
	}
	
	private class RotateTask extends BaseAsyncTask{
		private int mOrientation;
		private String mTarget;

		public RotateTask(int ori, String target) {
			super(1);
			mOrientation = ori;
			mTarget = target;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try{
				ExifInterface exif = new ExifInterface(mTarget);
				
				int[] rotation = {
						ExifInterface.ORIENTATION_NORMAL,
						ExifInterface.ORIENTATION_ROTATE_90,
						ExifInterface.ORIENTATION_ROTATE_180,
						ExifInterface.ORIENTATION_ROTATE_270,
						ExifInterface.ORIENTATION_NORMAL,
						ExifInterface.ORIENTATION_ROTATE_90,
						ExifInterface.ORIENTATION_ROTATE_180,
						ExifInterface.ORIENTATION_ROTATE_270,
				};
				
				int rotateNumber = 0;
				switch(mOrientation){
				case ROTATE_90:
					rotateNumber = 1;
					break;
				case ROTATE_180:
					rotateNumber = 2;
					break;
				case ROTATE_270:
					rotateNumber = 3;
					break;
				}
				
				int baseNumber = 0;
				switch(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)){
				case ExifInterface.ORIENTATION_ROTATE_90:
					baseNumber = 1;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					baseNumber = 2;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					baseNumber = 3;
					break;
				default:
					baseNumber = 0;
					break;
				}
				
				exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(rotation[rotateNumber+baseNumber]));
				exif.saveAttributes();
				//new File(mTarget).setLastModified(System.currentTimeMillis());
				return null;
			}catch(IOException e){
				setError(R.string.toast_failed_create_file);
				cancel(false);
				return null;
			}
			
			
//			BitmapFactory.Options opt = new BitmapFactory.Options();
//			opt.inScaled = false;
//			opt.inJustDecodeBounds = true;
//			BitmapFactory.decodeFile(mTarget, opt);
//			
//			if(opt.outWidth * opt.outHeight * 4 > 12 * 1024 * 1024){
//				//サイズオーバー
//				setError(R.string.toast_too_large);
//				cancel(false);
//				return null;
//			}
//			
//			opt.inJustDecodeBounds = false;
//			Bitmap src = BitmapFactory.decodeFile(mTarget, opt);
//			Bitmap dst = null;
//			
//			switch(mOrientation){
//			case ROTATE_90:{
//				Matrix m = new Matrix();
//				m.postRotate(90);
//				dst = Bitmap.createBitmap(src, 0, 0, opt.outWidth, opt.outHeight, m, false);
//				
//				break;
//			}
//				
//			case ROTATE_180:{
//				Matrix m = new Matrix();
//				m.postRotate(180);
//				dst = Bitmap.createBitmap(src, 0, 0, opt.outWidth, opt.outHeight, m, false);
//				
//				break;
//			}
//				
//			case ROTATE_270:{
//				Matrix m = new Matrix();
//				m.postRotate(270);
//				dst = Bitmap.createBitmap(src, 0, 0, opt.outWidth, opt.outHeight, m, false);
//				
//				break;
//			}
//			}//end switch
//			
//			FileOutputStream out = null;
//			try{
//				File f = File.createTempFile("jorlle", ".tmp", getExternalCacheDir());
//				File old = new File(mTarget);
//				out = new FileOutputStream(f);
//				long oldLastModified = old.lastModified();
//				if(!dst.compress(CompressFormat.JPEG, 90, out))throw new IOException("error");
//				try{
//					out.close();
//					out = null;
//				}catch(IOException e){}
//				old.delete();
//				f.renameTo(old);
//				//変更を検出できなくなるため、コメントアウト
//				//old.setLastModified(oldLastModified);
//			}catch(IOException e){
//				setError(R.string.toast_failed_create_file);
//				cancel(false);
//			}finally{
//				if(out != null){
//					try{
//						out.close();
//					}catch(IOException e){}
//				}
//				
//				src.recycle();
//				dst.recycle();
//			}
//			return null;
		}
		
	}
	
	private class ShrinkTask extends BaseAsyncTask{
		String mTarget;

		public ShrinkTask(String target) {
			super(1);
			mTarget = target;
		}

		@Override
		protected Void doInBackground(Void... params) {
			if(!resizeImage(mTarget))cancel(false);
			this.publishProgress(1);
			return null;
		}

		private boolean resizeImage(String path){
			ImageShrinker shrinker = new ImageShrinker(path);
			Intent intent = getIntent();
			int width = intent.getIntExtra(INTENT_RESIZE_WIDTH, 0);
			int height = intent.getIntExtra(INTENT_RESIZE_HEIGHT, 0);
			
			ImageShrinker.Size targetSize = null;
			for(ImageShrinker.Size size: shrinker.getAvailableSize()){
				if(size.width == width && size.height == height){
					targetSize = size;
					break;
				}
			}
			
			if(targetSize == null){
				return false;
			}
			
			Bitmap b = null;
			b = shrinker.shrink(targetSize, mBitmapOptions);
			if(b == null){
				setError(R.string.toast_too_large);
			}else{
				File f = new File(path);
				File resized = ExtUtil.createEmptyPath(f, RESIZE_SUFFIX);

				FileOutputStream out = null;
				try{
					out = new FileOutputStream(resized);
					if(!b.compress(Bitmap.CompressFormat.JPEG, 90, out))throw new IOException("failed");
					return true;
				}catch(IOException e){
					setError(R.string.toast_failed_create_file);
				}finally{
					b.recycle();
					try{
						out.close();
					}catch(IOException e){}
				}
			}
			return false;
		}
	}
}
