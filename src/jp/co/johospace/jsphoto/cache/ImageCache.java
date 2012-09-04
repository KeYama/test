package jp.co.johospace.jsphoto.cache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.database.CMediaIndex;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.util.MediaUtil;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore.Video.Thumbnails;

public class ImageCache implements CMediaIndex {
	private static int mCacheImageSize = 150;
	private static SQLiteDatabase mDatabase = OpenHelper.cache.getDatabase();
	
	/**
	 * データベースをクローズする。アプリケーション終了時に呼び出す事
	 */
	public static void dispose(){
	}
	
	/**
	 * イメージの生データ
	 */
	public static class ImageData{
		public byte[] data;
		public int size;
	}
	
	/**
	 * キャッシュの画像の幅、高さの最大値をセット
	 * 
	 * @param size
	 */
	public static void setCacheSize(int size){
		mCacheImageSize = size;
	}
		
	/**
	 * イメージキャッシュを得る
	 * 無いなら作成
	 * 
	 * @param path
	 * @param data
	 * @throws IOException
	 */
	public static void getImageCache(String folder, String name, ImageData data) throws IOException {
		
		File pathFile = new File(folder, name);
		
		Cursor cache = mDatabase.query(
				$TABLE, 
				new String[]{THUMBNAIL_TIMESTAMP, THUMBNAIL}, 
				NAME + " = ? AND " + DIRPATH + " = ?",
				new String[]{ name, folder }, null, null, null);
		try {
			if(cache.moveToFirst()){
				long lastModified = cache.getLong(0);
				if(pathFile.lastModified() == lastModified){
					data.data = cache.getBlob(1);
				}else{
					data.data = updateCache(pathFile);
				}
			}else{
				data.data = createCache(pathFile);
			}
			if (data.data != null) {
				data.size = data.data.length;
			}
		} finally {
			cache.close();
		}
	}
	
	/**
	 * キャッシュデータを作成
	 */
	public static byte[] createCache(File media) throws IOException{
		
		Bitmap bitmap = createThumbnail(media);
		
		if (bitmap == null) return null;
		
		ByteArrayOutputStream out = null;
		try{
			out = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
		}finally{
			if (bitmap != null) {
				bitmap.recycle();
			}
		}
		byte[] rawData = out.toByteArray();
		ContentValues values = new ContentValues();
		values.put(THUMBNAIL, rawData);
		values.put(NAME, media.getName());
		values.put(DIRPATH, media.getParent());
		values.put(THUMBNAIL_TIMESTAMP, media.lastModified());
		mDatabase.insertOrThrow($TABLE, null, values);
		
		return rawData;
	}
	
	public static byte[] updateCache(File media) throws IOException {
		Bitmap bitmap = createThumbnail(media);
		
		ByteArrayOutputStream out = null;
		try{
			out = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
		}finally{
			if(bitmap != null)bitmap.recycle();
		}
		byte[] rawData = out.toByteArray();
		ContentValues values = new ContentValues();
		values.put(THUMBNAIL, rawData);
		values.put(THUMBNAIL_TIMESTAMP, media.lastModified());
		mDatabase.update($TABLE, values,
				DIRPATH + " = ? AND " + NAME + " = ?",
				new String[] {media.getParent(), media.getName()});
		
		return rawData;
	}

	public static Bitmap createThumbnail(File media)
			throws IOException {
		BitmapFactory.Options opt = new BitmapFactory.Options();
		String path = media.getAbsolutePath();
		
		opt.inJustDecodeBounds = true;
		opt.inScaled = false;
		
		BitmapFactory.decodeFile(path, opt);
		
		opt.inScaled = true;
		opt.inJustDecodeBounds = false;
		opt.inPurgeable = true;
		
		int divide = ((opt.outWidth < opt.outHeight)? opt.outHeight: opt.outWidth);
		
		if (divide == 0) throw new IOException("decode error");  
		
//		opt.inTargetDensity = 1000 * mCacheImageSize / ((opt.outWidth < opt.outHeight)? opt.outHeight: opt.outWidth);
//		opt.inTargetDensity = 1000 * mCacheImageSize / divide;
//		
//		if(opt.inTargetDensity != 0 )opt.inSampleSize = 1000 / opt.inTargetDensity;
//		if(opt.inSampleSize != 0) opt.inDensity = 1000 / opt.inSampleSize;
		
		//TODO inSampleSize == 0 になり、エラーとなる場合があるため、ダミー値を設定
		opt.inSampleSize = divide / mCacheImageSize;
		if(opt.inSampleSize == 0)opt.inSampleSize = 1;
		
		opt.inTargetDensity = mCacheImageSize * opt.inSampleSize;
		opt.inDensity = divide;
		
		
		
		Bitmap bitmap;
		
		//TODO 動画ファイルはThumbnailUtilsを使用
		String check = path.toLowerCase();
		String mime = MediaUtil.getMimeTypeFromPath(check);
//		if (check.contains(".mp4") || check.contains(".3gp") || check.contains(".m4a")) {
		if (mime != null && mime.startsWith("video/")) {
			bitmap = ThumbnailUtils.createVideoThumbnail(path, Thumbnails.MINI_KIND);
			bitmap = editVideoThumbnail(bitmap);
		} else {
			bitmap = BitmapFactory.decodeFile(path, opt);
		}
		
//		Bitmap bitmap = BitmapFactory.decodeFile(path, opt);
		
		if(bitmap == null)throw new IOException("decode error");
		return bitmap;
	}
	
	public static Bitmap editVideoThumbnail(Bitmap bitmap)throws IOException{
		if(bitmap == null)throw new IOException("decode error");
		Canvas c = new Canvas(bitmap);

		Drawable overlay = JorlleApplication.instance().getApplicationContext().getResources()
				.getDrawable(jp.co.johospace.jsphoto.R.drawable.ic_all_select);
		overlay.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
		overlay.draw(c);
		
		return bitmap;
	}
}
