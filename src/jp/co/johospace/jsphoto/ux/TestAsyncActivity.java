package jp.co.johospace.jsphoto.ux;

import android.app.Activity;
import android.graphics.*;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.widget.LinearLayout;
import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import jp.co.johospace.jsphoto.ux.view.UXViewLoader;

import java.io.File;
import java.io.IOException;

/**
 *
 * UXAsyncImageViewのテスト
 *
 */
public class TestAsyncActivity extends Activity{
	public static int WIDTH = 200;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		UXViewLoader loader = new UXViewLoader(new TmpThumbnailLoader());
		UXAsyncImageView view = new UXAsyncImageView(this);
		view.setViewLoader(loader);

		File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
		File[] folders = dcim.listFiles();
		String path = null;
		for(File folder: folders){
			File[] images = folder.listFiles();
			if(images != null && images.length != 0){
				path = images[0].getAbsolutePath();
				break;
			}
		}

		view.loadImage(path, WIDTH);
		view.setBorder(true, Color.WHITE, 10);
		view.setLayoutParams(new LinearLayout.LayoutParams(500, 500));
		LinearLayout ll = new LinearLayout(this);
		ll.addView(view);

		setContentView(ll);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		UXViewLoader.dispose();
	}

	private class TmpThumbnailLoader implements UXThumbnailLoader {
		@Override
		public boolean loadCachedThumbnail(Object info, int widthHint, UXImageInfo out) {
			return false;
		}

		@Override
		public boolean loadThumbnail(Object info, int widthHint, UXImageInfo out) {
			String path = (String)info;
			byte[] thumbnail = null;
			Bitmap thumbnailBitmap = null;
			Bitmap ret = null;

			try {
				ExifInterface exif = new ExifInterface(path);
				thumbnail = exif.getThumbnail();
			} catch (IOException e) {
			}

			if(thumbnail == null){
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(path, opt);
				opt.inJustDecodeBounds = false;

				int divide = ((opt.outWidth < opt.outHeight)? opt.outHeight: opt.outWidth);
				opt.inSampleSize = divide / WIDTH;

				thumbnailBitmap =  BitmapFactory.decodeFile(path, opt);
			}else{
				thumbnailBitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
			}

			if(thumbnailBitmap == null){
				return false;
			}

			float heightRate = (float)thumbnailBitmap.getHeight() / (float)thumbnailBitmap.getWidth();
			int height = (int)(WIDTH * heightRate);

			ret = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.RGB_565);
			Canvas c = new Canvas(ret);
			c.drawBitmap(thumbnailBitmap, new Rect(0,0,thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight()),
					new Rect(0,0, WIDTH, height), new Paint());
			thumbnailBitmap.recycle();

			out.bitmap = ret;
			out.orientation = 270;
			return true;
		}

		@Override
		public void updateCachedThumbnail(Object info, int widthHint, UXImageInfo in) {
		}
	}
}
