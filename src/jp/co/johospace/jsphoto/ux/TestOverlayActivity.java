package jp.co.johospace.jsphoto.ux;

/**
 * Overlay表示のサンプルコード
 */

import java.io.File;

import jp.co.johospace.jsphoto.ux.loader.UXLocalThumbnailLoader;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.widget.UXGridDataSource;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget;
import jp.co.johospace.jsphoto.ux.widget.UXStage;
import jp.co.johospace.jsphoto.ux.widget.UXUnit;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;

/**
 *
 * Gridのテストアクティビティ
 *
 */
public class TestOverlayActivity extends Activity {
	private UXStage mStage;
	private File[] mImages;

	private UXThumbnailLoader mLoader;

	private int WIDTH = 200;
	private UXGridWidget mGrid;
	private int mOverlayId;

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
		mStage.setBackgroundColor(Color.rgb(20, 20, 20));
//		mStage.setScrollbarResource(R.drawable.scrollbar);

		//TODO: ここで切り替え
//		mLoader = new TmpThumbnailLoader();
//		mLoader = new FastThumbnailLoader();
		mLoader = new UXLocalThumbnailLoader();

		mStage.lockStage(new Runnable() {
			@Override
			public void run() {
				mGrid = new UXGridWidget(WIDTH, mLoader);

				mStage.setBackgroundColor(Color.MAGENTA);

				Bitmap tmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
				tmp.eraseColor(Color.MAGENTA);
				BitmapDrawable drawable = new BitmapDrawable(tmp);

				Bitmap tmp2 = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
				tmp2.eraseColor(Color.GREEN);
				BitmapDrawable drawable2 = new BitmapDrawable(tmp2);

				UXGridWidget.OverlayGrid factory = new UXGridWidget.OverlayGrid(new MyOverlayDataSource());
				UXGridWidget.OverlayItem[] items = new UXGridWidget.OverlayItem[2];
				items[0] = new UXGridWidget.OverlayItem(
						drawable, //表示するDrawable
						UXGridWidget.OverlayGrid.POSITION_LEFT_BOTTOM,//表示位置
						25, UXUnit.DP, //幅
						25, UXUnit.DP, //高さ
						5, UXUnit.DP //マージン
						);

				items[1] = new UXGridWidget.OverlayItem(
						drawable2, //表示するDrawable
						UXGridWidget.OverlayGrid.POSITION_LEFT_BOTTOM,//表示位置
						25, UXUnit.DP, //幅
						25, UXUnit.DP, //高さ
						5, UXUnit.DP //マージン
				);

				factory.setRibbonWidth(30, UXUnit.DP);

				mGrid.setOnCornerTapListener(new UXGridWidget.CornerTapListener() {
					@Override
					public void onTap(int itemNumber) {
//						android.util.Log.e("dbg", "corner: " + itemNumber);		/*$debug$*/
					}
				}, 50, UXUnit.DP);

				mOverlayId = factory.addOverlay(items);



				mGrid
						.dataSource(new MyDataSource())
						.padding(5, UXUnit.DP)
						.itemType(factory)
						.addTo(mStage);
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

	public class MyOverlayDataSource implements UXGridWidget.OverlayDataSource{
		@Override
		public int getOverlayNumber(int itemPosition, int overlayId) {
			if(overlayId == mOverlayId){
				return (Math.random()>0.5)? 0: 1;
//				return 1;
			}

			return -1;
		}
	}

	public class MyDataSource implements UXGridDataSource {
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

}
