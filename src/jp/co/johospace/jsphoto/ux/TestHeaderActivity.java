package jp.co.johospace.jsphoto.ux;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import jp.co.johospace.jsphoto.ux.loader.UXImageInfo;
import jp.co.johospace.jsphoto.ux.loader.UXLocalThumbnailLoader;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.widget.UXGridDataSource;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget;
import jp.co.johospace.jsphoto.ux.widget.UXHeaderWidget;
import jp.co.johospace.jsphoto.ux.widget.UXStackContainer;
import jp.co.johospace.jsphoto.ux.widget.UXStage;
import jp.co.johospace.jsphoto.ux.widget.UXUnit;
import android.R;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;

/**
 *
 * Gridのテストアクティビティ
 *
 */
public class TestHeaderActivity extends Activity {
	private UXStage mStage;
	private File[] mImages;

	private UXThumbnailLoader mLoader;

	private int WIDTH = 200;
	private UXGridWidget mGrid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
		File[] folders = dcim.listFiles();
		for(File folder: folders){
			mImages = folder.listFiles();
			if(mImages != null)break;
		}

//		mStage = new UXStage(this, UXStage.SOFT_RENDERER);
		mStage = new UXStage(this, UXStage.GL_RENDERER);
		mStage.setBackgroundColor(R.color.white);
//		mStage.setScrollbarResource(jp.co.johospace.jsphoto.ux.R.drawable.scrollbar);

		//TODO: ここで切り替え
//		mLoader = new TmpThumbnailLoader();
//		mLoader = new FastThumbnailLoader();
		mLoader = new UXLocalThumbnailLoader();

		createStage();

		setContentView(mStage.getView());
	}


	private void createStage(){
		mStage.lockStage(new Runnable() {
			@Override
			public void run() {
				mStage.clear();
				UXStackContainer stack = new UXStackContainer();
				stack.addTo(mStage);

				UXHeaderWidget header = new UXHeaderWidget("hogehooogehogehgeohgheoghehgoehoghoe", "fuga", 20, UXUnit.DP);
//				UXHeaderWidget header = new UXHeaderWidget("日本語", 50, UXUnit.DP);
				header.margin(10, UXUnit.DP).textColor(Color.WHITE).backgroundColor(Color.MAGENTA, true).addTo(stack);


				Bitmap tmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
				tmp.eraseColor(Color.MAGENTA);
				BitmapDrawable drawable = new BitmapDrawable(tmp);

				header.icon(drawable, 20, UXUnit.DP, 20, UXUnit.DP);

				mGrid = new UXGridWidget(WIDTH, mLoader);
				mGrid
						.dataSource(new MyDataSource())
						.padding(5, UXUnit.DP)
						.itemType(new UXGridWidget.ThumbnailGrid())
						.addTo(stack);

				UXHeaderWidget header2 = new UXHeaderWidget("test", "desu", 20, UXUnit.DP);
				header2.margin(10, UXUnit.DP).textColor(Color.WHITE).addTo(stack);

				mGrid.setOnCornerTapListener(new UXGridWidget.CornerTapListener() {
					@Override
					public void onTap(int itemNumber) {
//						android.util.Log.e("dbg", ""+itemNumber);		/*$debug$*/
					}
				}, 50, UXUnit.DP);
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
				createStage();
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