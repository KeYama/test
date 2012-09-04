package jp.co.johospace.jsphoto.ux;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import jp.co.johospace.jsphoto.ux.loader.ImageThumbnails;
import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXLocalThumbnailLoader;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.widget.UXGridDataSource;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget;
import jp.co.johospace.jsphoto.ux.widget.UXStage;
import jp.co.johospace.jsphoto.ux.widget.UXUnit;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

/**
 *
 * Gridのテストアクティビティ
 *
 */
public class TestGridActivity extends Activity {
	private UXStage mStage;
	private File[] mImages;

	private UXThumbnailLoader mLoader;

	private int WIDTH = 200;
	private UXGridWidget mGrid;


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


	private void compress(Bitmap b){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		b.compress(Bitmap.CompressFormat.JPEG, 80, out);

		try{
			out.close();
		}catch(IOException e){}
		b.recycle();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
		File[] folders = dcim.listFiles();
		for(File folder: folders){
			mImages = folder.listFiles();
			if(mImages != null)break;
		}


		UXImageInfo info = new UXImageInfo();
		UXLocalThumbnailLoader loader = new UXLocalThumbnailLoader();
		File image = null;
		long nano = 0;


		nano = System.nanoTime();

		for(int n=0; n < 20; n++){
			image = mImages[n];
			compress(ImageThumbnails.getThumbnailOriginal(image.getAbsolutePath(), 200, 200));
		}

//		Log.e("dbg", ""+((double)(System.nanoTime()-nano))/(1000*1000*1000));		/*$debug$*/


		nano = System.nanoTime();

		for(int n=0; n < 20; n++){
			image = mImages[n];
			compress(decodeThumbnail(image.getAbsolutePath(), 200));

		}

//		Log.e("dbg", ""+((double)(System.nanoTime() -nano))/(1000*1000*1000));		/*$debug$*/



//		mStage = new UXStage(this, UXStage.SOFT_RENDERER);
		mStage = new UXStage(this, UXStage.GL_RENDERER);
		mStage.setBackgroundColor(Color.rgb(20,20,20));

		//TODO: ここで切り替え
//		mLoader = new TmpThumbnailLoader();
//		mLoader = new FastThumbnailLoader();
		mLoader = new UXLocalThumbnailLoader();

		mStage.lockStage(new Runnable() {
			@Override
			public void run() {
				mGrid = new UXGridWidget(WIDTH, mLoader);
				mGrid
					.dataSource(new MyDataSource())
					.padding(5, UXUnit.DP)
					.itemType(new UXGridWidget.ThumbnailGrid())
					.addTo(mStage);
			}
		});

		mGrid.setOnItemTapListener(new UXGridWidget.ItemTapListener() {
			@Override
			public void onTap(int itemNumber) {
//				android.util.Log.e("dbg", ""+itemNumber);		/*$debug$*/
			}
		});

		mGrid.setOnItemLongPressListener(new UXGridWidget.ItemLongPressListener() {
			@Override
			public void onLongPress(int itemNumber) {
//				android.util.Log.e("dbg", "long: "+itemNumber);		/*$debug$*/
				mGrid.invalidateData();
				mStage.invalidate();
			}
		});

		mStage.setScrollListener(new UXStage.ScrollListener(){
			@Override
			public void onScrollStart() {
//				android.util.Log.e("dbg", "sclstart");		/*$debug$*/
			}

			@Override
			public void onScrollEnd() {
//				android.util.Log.e("dbg", "sclend");		/*$debug$*/
			}
		});

		setContentView(mStage.getView());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mStage.dispose();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mStage.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mStage.onResume();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
			mStage.lockStage(new Runnable() {
				@Override
				public void run() {
					mGrid.column(5);
				}
			});
		}else{
			mStage.lockStage(new Runnable() {
				@Override
				public void run() {
					mGrid.column(3);
				}
			});
		}
	}

	public class MyDataSource implements UXGridDataSource{
		@Override
		public Object getOverlayInfo(int item, int iconNumber) {
			return null;
		}

		@Override
		public int getRotation(int item) {
			return 0;
		}

		@Override
		public Object getInfo(int item) {
			return mImages[item].getAbsolutePath();
		}

		@Override
		public int getItemCount() {
			return mImages.length;
		}
	}

	private class FastThumbnailLoader extends TmpThumbnailLoader{

		byte[][] mCompressed = new byte[3][];

		public FastThumbnailLoader(){
			UXImageInfo info = new UXImageInfo();

			for(int n = 0; n < 3; ++n){
				loadThumbnail(mImages[(int) (Math.random() * mImages.length)].getAbsolutePath(), WIDTH, info);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				info.bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
				mCompressed[n] = out.toByteArray();
				info.bitmap.recycle();
				info.bitmap = null;
			}
		}

		@Override
		public boolean loadCachedThumbnail(Object info, int widthHint, UXImageInfo out) {
			out.compressedImage =  mCompressed[(int)(Math.random()*mCompressed.length)];
			return true;
		}

		@Override
		public void updateCachedThumbnail(Object info, int widthHint, UXImageInfo in) {
		}
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
				if(opt.inSampleSize == 0)opt.inSampleSize = 1;

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
