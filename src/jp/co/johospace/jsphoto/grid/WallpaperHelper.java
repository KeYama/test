package jp.co.johospace.jsphoto.grid;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

public class WallpaperHelper {
	private File mTmpFile;
	
	public void onActivityResult(Activity activity, int resultCode){
		if(resultCode == Activity.RESULT_OK){
			setWallpaper(activity, mTmpFile.getAbsolutePath());
		}
		mTmpFile.delete();

	}
	

	private void setWallpaper(Activity activity, String path){
		FileInputStream in = null;
		try{
			in = new FileInputStream(path);
			WallpaperManager manager = WallpaperManager.getInstance(activity);
			manager.setStream(in);
		}catch(IOException e){
		}finally{
			try{
				if(in != null)in.close();
			}catch(IOException e){}
		}
	}
	
	public static boolean checkCanCrop(Activity activity, String path){
		Cursor c = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				null,
				MediaStore.Images.ImageColumns.DATA + " = ?",
				new String[]{path}, null);

			if(c.moveToFirst()){
				c.close();
				return true;
			}
			c.close();
			return false;

	}
	
	
	public void startCropActivity(Activity activity, String filePath, int requestCode){
		Cursor c = activity.getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
				null,
				MediaStore.Images.ImageColumns.DATA + " = ?",
				new String[]{filePath}, 
				null);
		if(c.moveToFirst()){
			String content = "content://media/external/images/media/" + 
					c.getInt(c.getColumnIndex(MediaStore.MediaColumns._ID));
			
            PackageManager pm = activity.getPackageManager();
			List<ApplicationInfo> infos = pm.getInstalledApplications(0);
			//使えるCropアプリケーションを探す。機種依存。
			String[] apps = {"com.android.gallery", "com.cooliris.media", "com.google.android.gallery3d"};
			String[] clss = {"com.android.camera.CropImage", "com.cooliris.media.CropImage", "com.android.gallery3d.app.CropImage"};
			
			int cls= -1;
			for (ApplicationInfo info : infos) {
				String pkg = info.packageName;
				if (apps[0].equals(pkg)) {
					cls = 0;
				}
				if (apps[1].equals(pkg)) {
					cls = 1;
				}
				if (apps[2].equals(pkg)) {
					cls = 2;
				}
			}
			try{
				if(cls >= 0){
					mTmpFile = File.createTempFile("jorlle", ".png");
					
					Intent i = new Intent();
					i.setClassName(apps[cls], clss[cls]);
					i.setData(Uri.parse(content));
					i.putExtra("crop", true);
					i.putExtra("outputX", 800);
					i.putExtra("outputY", 800);
					i.putExtra("aspactX", 800);
					i.putExtra("aspactY", 800);
					i.putExtra("scale", true);
					//i.putExtra("circleCrop", true);
					//i.putExtra("setWallpaper", true);
					i.putExtra("noFaceDetection", true);
					i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTmpFile));
					i.putExtra("outputFormat", Bitmap.CompressFormat.PNG);
					
					activity.startActivityForResult(i, requestCode);
				} else {
					// Xperia arcなど標準のギャラリーが入っていない場合
					mTmpFile = File.createTempFile("jorlle", ".png");
					Intent intent = new Intent("com.android.camera.action.CROP");
					// トリミングに渡す画像パス
					intent.setData(Uri.parse(content));
					// トリミング後の画像の幅
					intent.putExtra("outputX", 800);
					// トリミング後の画像の高さ
					intent.putExtra("outputY", 800);
					// トリミング後の画像のアスペクト比（X）
					intent.putExtra("aspectX", 800);
					// トリミング後の画像のアスペクト比（Y）
					intent.putExtra("aspectY", 800);
					// トリミング中の枠を拡大縮小させるか
					intent.putExtra("scale", true);
					intent.putExtra("noFaceDetection", true);
					intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTmpFile));
					intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG);
					activity.startActivityForResult(intent, requestCode);
				}
			}catch(Exception e){
				//TODO: toast error
			}
		}
	}

}
