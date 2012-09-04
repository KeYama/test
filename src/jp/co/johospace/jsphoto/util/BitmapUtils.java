package jp.co.johospace.jsphoto.util;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapUtils {

	// TODO 圧縮クオリティを調整しながら決定すること。
    private static final int COMPRESS_JPEG_QUALITY = 50;
    
    public static byte[] compressBitmap(Bitmap bitmap) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,
                COMPRESS_JPEG_QUALITY, os);
        return os.toByteArray();
    }
    
    /**
     * Bitmapを縮小する。
     * ただし、Bitmapの縦横比率は変更しない。
     * 
     * @param src
     * @param width 縮小後の幅(縦横比率は変更しないので、必ず指定した値になるとは限らない)
     * @param height 縮小後の高さ(縦横比率は変更しないので、必ず指定した値になるとは限らない)
     * @return
     */
    public static Bitmap cutdownBitmap(Bitmap src, int width, int height){
    	if(src == null){
    		return null;
    	}

    	int srcWidth = src.getWidth();
    	int srcHeight = src.getHeight();
    	if((srcWidth < width) || (srcHeight < height)){
    		return src;
    	}
    	
    	// 大きい方のスケールを採用する
    	float scaleWidth = (float)width / (float)srcWidth;
    	float scaleHeight = (float)height / (float)srcHeight;
    	float scale = Math.max(scaleWidth, scaleHeight);
    	
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		Bitmap scaledBitmap = Bitmap.createBitmap(src, 0, 0,
				srcWidth, srcHeight, matrix, true);
		
		return scaledBitmap;
    }
}
