package jp.co.johospace.jsphoto.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;

/**
 * 画像ユーティリティ
 */
public class ImageUtil {

    /**
     * ビットマップをリサイズします
     * 
     * @param bmp	ビットマップ
     * @param w	横幅
     * @param h	立て幅
     * @return		リサイズ後の画像
     */
	public static Bitmap resizeBitmap(Bitmap bmp,int w,int h) {        
		Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);        
		Canvas canvas = new Canvas(result);        
		BitmapDrawable drawable=new BitmapDrawable(bmp);        
		drawable.setBounds(0, 0, w, h);        
		drawable.draw(canvas);        
		return result;    
	} 
	
}
