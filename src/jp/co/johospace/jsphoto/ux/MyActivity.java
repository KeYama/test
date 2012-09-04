package jp.co.johospace.jsphoto.ux;

import android.app.Activity;
import android.graphics.*;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.widget.*;

import java.io.File;
import java.io.IOException;

public class MyActivity extends Activity
{
	public static final int WIDTH = 200;
	UXStage mStage;
	TmpThumbnailLoader mLoader = new TmpThumbnailLoader();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

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

		mStage = new UXStage(this, UXStage.SOFT_RENDERER);
		final int STACK = 1;
		final int CONTAINER = 2;
		final String thumbnail = path;

		mStage.lockStage(new Runnable() {
			@Override
			public void run() {
				new UXStackContainer()
						.id(STACK)
						.addTo(mStage);

				new UXContainer()
						.divide(5, 5)
						.width(1, UXUnit.SCREEN_WIDTH)
						.height(0.5f, UXUnit.SCREEN_WIDTH)
						.id(CONTAINER)
						.addTo(mStage, STACK);

				new UXThumbnailWidget(thumbnail, 100, mLoader)
						.width(1, UXUnit.GRID)
						.height(1, UXUnit.GRID)
						.position(1, 2, UXUnit.GRID)
						.addTo(mStage, CONTAINER);

				new UXThumbnailWidget(thumbnail, 100, mLoader)
						.width(2, UXUnit.GRID)
						.height(2, UXUnit.GRID)
						.position(3, 4, UXUnit.GRID)
						.addTo(mStage, CONTAINER);

			}
		});

		setContentView(mStage.getView());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mStage.dispose();
	}


	private class TmpThumbnailLoader implements UXThumbnailLoader{
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
			return true;
		}

		@Override
		public void updateCachedThumbnail(Object info, int widthHint, UXImageInfo in) {
		}
	}
}
