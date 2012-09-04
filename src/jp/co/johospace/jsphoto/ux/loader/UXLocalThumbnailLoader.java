package jp.co.johospace.jsphoto.ux.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import jp.co.johospace.jsphoto.JorlleApplication;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

/**
 * ローカルのサムネイルローダー
 */
public class UXLocalThumbnailLoader implements UXThumbnailLoader {

	/** Exif反映フラグ */
	protected boolean mIsReflectionExif = true;

	@Override
	public synchronized boolean loadCachedThumbnail(Object info, int sizeHint, UXImageInfo out) {
		return false;
	}

	@Override
	public boolean loadThumbnail(Object info, int sizeHint, UXImageInfo out) {
//		return createThumbnail(info, sizeHint, out);
		return createThumbnailTao(info, sizeHint, out);
	}
	
	private boolean createThumbnailTao(Object info, int sizeHint, UXImageInfo out){
		String path = info instanceof File ? ((File) info).getAbsolutePath() : (String)info;
		
		Bitmap thumb = null;
		
		thumb = ImageThumbnails.getThumbnailMiniKind(path, sizeHint, sizeHint, JorlleApplication.instance().getContentResolver());
		
        ExifInterface ei;
		try {
			ei = new ExifInterface(path);
			if (mIsReflectionExif) {
				switch(ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)){
				case ExifInterface.ORIENTATION_NORMAL:
					break;
				case ExifInterface.ORIENTATION_ROTATE_90:
					out.orientation = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					out.orientation = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					out.orientation = 270;
					break;
				}
			}
			
			if(thumb == null && ei.hasThumbnail()){
			    byte[] b = ei.getThumbnail();
			    Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
			    thumb = ImageThumbnails.extractThumbnail(bmp, sizeHint, sizeHint, 0);
			}
			
		} catch (IOException e) {
		}
		
		if(thumb == null){
			thumb = ImageThumbnails.getThumbnailOriginal(path, sizeHint, sizeHint);
		}
		if(thumb == null){
			thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
		}
		
		//565ではないなら変換
		if(thumb != null){
			if(thumb.getConfig() != Bitmap.Config.RGB_565){
				Bitmap tmp =
						Bitmap.createBitmap(thumb.getWidth(), thumb.getHeight(), Bitmap.Config.RGB_565);
				Canvas c = new Canvas(tmp);
				c.drawBitmap(thumb, 0, 0, new Paint());
				thumb.recycle();
				thumb = tmp;
			}
		}
		
		out.bitmap = thumb;
		
		if(thumb == null)return false;
		else return true;
	}

	private boolean createThumbnail(Object info, int sizeHint, UXImageInfo out) {
		String path = info instanceof File ? ((File) info).getAbsolutePath() : (String)info;
		byte[] thumbnail = null;
		Bitmap thumbnailBitmap = null;

		try{
			ExifInterface exif = new ExifInterface(path);
			thumbnail = exif.getThumbnail();
			
			if (mIsReflectionExif) {

				switch(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)){
				case ExifInterface.ORIENTATION_NORMAL:
					break;
				case ExifInterface.ORIENTATION_ROTATE_90:
					out.orientation = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					out.orientation = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					out.orientation = 270;
					break;
				}
			}
			
			
		}catch (IOException e){}

		if(thumbnail == null){
			thumbnailBitmap = decodeThumbnail(path, sizeHint);
			if(thumbnailBitmap == null)return false;

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
			thumbnail = stream.toByteArray();
			try {
				stream.close();
			} catch (IOException e) {}
		}else{
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inPreferredConfig = Bitmap.Config.RGB_565;

			thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length, opt);
		}

		if(thumbnailBitmap == null)return false;

		//565ではないなら変換
		if(thumbnailBitmap.getConfig() != Bitmap.Config.RGB_565){
			Bitmap tmp =
					Bitmap.createBitmap(thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight(), Bitmap.Config.RGB_565);
			Canvas c = new Canvas(tmp);
			c.drawBitmap(thumbnailBitmap, 0, 0, new Paint());
			thumbnailBitmap.recycle();
			thumbnailBitmap = tmp;
		}

		out.bitmap = thumbnailBitmap;
		out.compressedImage = thumbnail;

		return true;
	}

	/**
	 * 指定サイズにサムネイル作成
	 *
	 * @param path
	 * @param sizeHint
	 * @return
	 */
	private Bitmap decodeThumbnail(String path, int sizeHint){
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, opt);
		opt.inJustDecodeBounds = false;
		opt.inPreferredConfig = Bitmap.Config.RGB_565;

		int divide = ((opt.outWidth < opt.outHeight)? opt.outHeight: opt.outWidth);
		opt.inSampleSize = divide / sizeHint;
		if(opt.inSampleSize == 0)opt.inSampleSize = 1;
		opt.inSampleSize = toPow2(opt.inSampleSize);

		Bitmap ret = BitmapFactory.decodeFile(path, opt);
		if(ret == null){
			ret = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
		}

		return ret;
	}

	private int toPow2(int from){
		int n = 1;
		for(n = 1; n <= from; n *=2);
//		n /= 2;
		return n;
	}

	@Override
	public synchronized void updateCachedThumbnail(Object info, int sizeHint, UXImageInfo in) {
	}
	
	/**
	 * Exif反映フラグをセットします
	 * 
	 * @param reflectionExif	Exif反映フラグ
	 */
	public void setReflectionExif(boolean reflectionExif) {
		mIsReflectionExif = reflectionExif;
	}
}
