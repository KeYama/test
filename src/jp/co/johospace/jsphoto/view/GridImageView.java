package jp.co.johospace.jsphoto.view;

import java.util.List;

import jp.co.johospace.jsphoto.cache.ImageCache;
import jp.co.johospace.jsphoto.cache.ImageCache.ImageData;
import jp.co.johospace.jsphoto.util.ImageUtil;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;

/**
 * フォルダ画像表示ビュー（フォルダグリッド形式）です
 */
public class GridImageView extends View {

	/** 表示画像 */
	private Bitmap mNewBitmap;
	
	/** 画像作成用ペイント*/
	private Paint mPaint = new Paint();
	
	/** 画像作成用キャンバス*/
	private Canvas mCanvas = new Canvas();
	
	/** 描画用イメージデータ */
	private ImageData im = new ImageData();
	
	/** 画像結合数 */
	private static final int IMAGE_GRID_COUNT = 4;
	
	/** 画像結合　インデックス */
	public static final int
		INDEX_FIRST = 0,
		INDEX_SECOND = 1,
		INDEX_THIRD = 2,
		INDEX_FOURTH = 3;
	
	public GridImageView(Context context) {
		super(context);
	}
	
	public GridImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	/**
	 * サムネイル画像を結合し、フォルダグリッドに表示する
	 * @param imageList メディアファイルのパスリスト
	 * @param parentLength 画像の縦横幅
	 */
	public boolean setImage(List<String> imageList, int parentLength, LayoutParams layoutParams) {
		
		int imageListSize = imageList.size();
		
		// 縦横のサイズ
		int parentWidth = parentLength;
		int parentHeight = parentLength;
		
		// 描画領域サイズの設定
		setLayoutParams(layoutParams);
		
		int childWidth = parentWidth / 2;
		int childHeight = parentHeight / 2;
		
		// 最終的に表示される画像
		mNewBitmap = Bitmap.createBitmap(parentWidth, parentHeight, Bitmap.Config.ARGB_8888);
		mCanvas.setBitmap(mNewBitmap);
		
		int x = 0;
		int y = 0;
		
		int imageCount = 0;
		
		// 画像を結合
		for (int i = 0; i < IMAGE_GRID_COUNT; i++) {
			
			Bitmap bitmap = null;
			
			// 結合する画像が存在しない場合は、空白の画像を設定
			if (i > imageListSize - 1) {
				bitmap = Bitmap.createBitmap(childWidth, childHeight, Bitmap.Config.ARGB_8888); 
			} else {
				
				String imagePath = imageList.get(i);
				int index = imagePath.lastIndexOf("/");
				
				// ファイルが格納されているフォルダ名と、ファイル名を取得
				String parent = imagePath.substring(0, index);
				String name = imagePath.substring(index + 1, imagePath.length());
				
				imagePath = null;
				
				try {
					// パスを元に、サムネイルを取得
					ImageCache.getImageCache(parent, name, im);
				} catch (Exception e) {
					
					//TODO サムネイルが取得できない場合は、次の描画へ
//					Log.v("gridImageView","作成失敗：" + imagePath);
//					e.printStackTrace();		/*$debug$*/
					continue;
				}
				
				// サムネイルをリサイズ
				if (im != null) {
					bitmap = BitmapFactory.decodeByteArray(im.data, 0, im.size);
					bitmap = ImageUtil.resizeBitmap(bitmap, childWidth, childHeight);
				}
				
				imageCount++;
				
			}
			// 縦・横の値初期化
			x = 0;
			y = 0;
			
			// 画像の合成位置を決定
			switch(i) {
				// 左上
				case INDEX_FIRST:
					break;
				
				// 右上
				case INDEX_SECOND:
					x = childWidth;
					break;
				
				// 左下
				case INDEX_THIRD:
					y = childHeight;
					break;
				
				// 右下
				case INDEX_FOURTH:
					x = childWidth;
					y = childHeight;
					break;
			}
			
			// キャンバスに描画
			mCanvas.drawBitmap(bitmap, x, y, mPaint);
			
			bitmap.recycle();
			bitmap = null;
		}
		
		if (imageCount > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(mNewBitmap, 0 , 0, mPaint);
	}
}
