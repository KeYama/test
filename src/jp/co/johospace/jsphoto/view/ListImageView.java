package jp.co.johospace.jsphoto.view;

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
 * フォルダ画像表示ビュー（リスト形式）です
 */
public class ListImageView extends View {
	
	/** 表示画像 */
	private Bitmap mNewBitmap;
	
	/** 画像作成用ペイント*/
	private Paint mPaint = new Paint();
	
	/** 画像作成用キャンバス*/
	private Canvas mCanvas = new Canvas();
	
	/** 描画用イメージデータ */
	ImageData im = new ImageData();
	
	public ListImageView(Context context) {
		super(context);
	}
	
	public ListImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	/**
	 * サムネイル画像を結合し、フォルダグリッドに表示する
	 * @param imageList メディアファイルのパスリスト
	 */
	public boolean setImage(String imagePath, int parentLength, LayoutParams layoutParams) {
		
		int parentWidth = parentLength;
		int parentHeight = parentLength;
		
		// 描画領域サイズの決定
		setLayoutParams(layoutParams);
		
		// 最終的に表示される画像
		mNewBitmap = Bitmap.createBitmap(parentWidth, parentHeight, Bitmap.Config.ARGB_8888);
		mCanvas.setBitmap(mNewBitmap);
		
		
		int index = imagePath.lastIndexOf("/");
		
		// ファイルが格納されているフォルダ名と、ファイル名を取得
		String parent = imagePath.substring(0, index);
		String name = imagePath.substring(index + 1, imagePath.length());
				
		try {
			// パスを元に、サムネイルを取得
			ImageCache.getImageCache(parent, name, im);
		} catch (Exception e) {
			//TODO サムネイルが取得できない場合は、次の描画へ
//			Log.v("listImageView","作成失敗：" + imagePath);
//			e.printStackTrace();
			return false;
		}
		
		Bitmap bitmap = null;
		
		// サムネイルをリサイズ
		if (im != null) {
			bitmap = BitmapFactory.decodeByteArray(im.data, 0, im.size);
			bitmap = ImageUtil.resizeBitmap(bitmap, parentWidth, parentHeight);
		}

		// キャンバスに描画
		mCanvas.drawBitmap(bitmap, 0, 0, mPaint);
		
		bitmap.recycle();
		bitmap = null;
		
		return true;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(mNewBitmap, 0, 0, mPaint);
	}
}
