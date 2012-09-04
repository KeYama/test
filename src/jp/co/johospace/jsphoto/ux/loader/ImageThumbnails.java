package jp.co.johospace.jsphoto.ux.loader;

import java.io.IOException;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Thumbnails;
import android.provider.MediaStore.MediaColumns;

/**
 * サムネイル生成に利用できるユーティリティクラス。
 * ファイルパスを指定してサムネイル画像を取得することができる。
 */
public class ImageThumbnails {

    /**
     * EXIFからサムネイル画像を取得する
     * @param path JPG画像へのパス
     * @param width サムネイルの幅
     * @param height サムネイルの高さ
     * @return EXIFから取り出したサムネイル画像、見つからない場合はnull。
     */
    public static Bitmap getThumbnailExif(String path, int width, int height){
    	
        if(path == null){
            return null;
        }
        ExifInterface ei;
		try {
			ei = new ExifInterface(path);
			if(ei.hasThumbnail()){
			    byte[] b = ei.getThumbnail();
			    Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
			    return extractThumbnail(bmp, width, height, 0);
			}else{
			    return null;
			}
		} catch (IOException e) {
//		    e.printStackTrace();		/*$debug$*/
		}
    	
		return null;
    }
    

    /**
     * MediaStoreが管理しているMINIサムネイルを元にサムネイル画像を生成する
     * @param path 元画像のファイルパス
     * @param width サムネイルの幅
     * @param height サムネイルの高さ
     * @param cr コンテントプロバイダにアクセスするための ContentResolver オブジェクト
     * @return 指定したサイズのサムネイルBitmap
     */
    public static Bitmap getThumbnailMiniKind(String path, int width, int height, ContentResolver cr){
    	
    	// 元画像のパスから画像IDを取得
    	long imageId = requestImageId(cr, path);
    	if(imageId <= 0){
    		return null;
    	}
    	
    	// 画像IDからサムネイルパスを取得
    	String thumbPath = requestMiniThumbnailPath(cr, imageId);
    	if(thumbPath == null){
    		return null;
    	}
    	
    	// MINIサムネイルの大きさは分からないので、縮小したBitmapからサムネイルを生成する。
    	return getThumbnailOriginal(thumbPath, width, height);
    }
    
	/**
	 * MesiaStore.Images.Media で管理されているIDを取得する。
	 * @param cr ContentResolver オブジェクト
	 * @param path IDを問い合わせる画像のパス
	 * @return MediaStore.Images.Media のID。取得できない場合は0。
	 */
	private static long requestImageId(ContentResolver cr, String path){
		
    	Cursor c = null;
    	long imageId = 0;
    	
    	try{
        	// 画像パスから MediaStore で管理されているIDを取得する
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = new String[]{ MediaColumns._ID };
            String selection = MediaColumns.DATA + "=?";
            String[] selectionArgs = new String[]{ path };
    		
            c = cr.query(uri, projection, selection, selectionArgs, null);
            if( (c != null) && c.moveToFirst() ){
                imageId = c.getLong(0);
            }
            
    	}catch(Exception e){
    		// 基本的に例外は出ないはずだが、一応備えておく。
    	}finally{
    		if(c != null){
    			c.close();
    			c = null;
    		}
    	}
    	
    	return imageId;
	}
	
	/**
	 * MediaStore.Images.Thumbnails で管理されている、MINIサムネイルのパスを取得する。
	 * @param cr ContentResolver オブジェクト
	 * @param imageId MediaStore.Images.Media のID
	 * @return MINIサムネイルのファイルパス。取得できない場合はnull。
	 */
	private static String requestMiniThumbnailPath(ContentResolver cr, long imageId){
		
    	Cursor c = null;
    	String thumbPath = null;
    	
    	try{
        	// サムネイルパスを取得する
            Uri uri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
            String[] projection = new String[]{ Thumbnails.DATA };
            String selection = Thumbnails.IMAGE_ID + "=?";
            String[] selectionArgs = new String[]{ String.valueOf(imageId) };
    		
            c = cr.query(uri, projection, selection, selectionArgs, null);
            if( (c != null) && c.moveToFirst() ){
            	thumbPath = c.getString(0);
            }
            
    	}catch(Exception e){
    		// 基本的に例外は出ないはずだが、一応備えておく。
    	}finally{
    		if(c != null){
    			c.close();
    			c = null;
    		}
    	}
    	
    	return thumbPath;
	}

    
    /**
     * オリジナル画像を元にサムネイル画像を生成する
     * @param path 元画像のファイルパス
     * @param width サムネイルの幅
     * @param height サムネイルの高さ
     * @return 指定したサイズのサムネイルBitmap
     */
	public static Bitmap getThumbnailOriginal(String path, int width, int height){
		
		// 画像サイズだけを読み込み、縮小したBitmapを取り出す。
		BitmapFactory.Options options = new BitmapFactory.Options();
		
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		// 読み込む縮尺を決定
		int scaleW = options.outWidth / width + 1;
		int scaleH = options.outHeight / height + 1;
		int scale = Math.max(scaleW, scaleH);
		
		// 1/2, 1/4, 1/8 ... のように2のべき乗のスケールで縮小する
		// ex). 3の場合は1/4になる。
		options.inSampleSize = scale;

		options.inJustDecodeBounds = false;
		Bitmap image = BitmapFactory.decodeFile(path, options);
		
		return extractThumbnail(image, width, height, 0);
	}
	
	/* Options used internally. */
	private static final int OPTIONS_NONE = 0x0;
	private static final int OPTIONS_SCALE_UP = 0x1;
	public static final int OPTIONS_RECYCLE_INPUT = 0x2;
	
    /**
     * Creates a centered bitmap of the desired size.
     *
     * @param source original bitmap source
     * @param width targeted width
     * @param height targeted height
     * @param options options used during thumbnail extraction
     */
    public static Bitmap extractThumbnail(
            Bitmap source, int width, int height, int options) {
        if (source == null) {
            return null;
        }

        float scale;
        if (source.getWidth() < source.getHeight()) {
            scale = width / (float) source.getWidth();
        } else {
            scale = height / (float) source.getHeight();
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        Bitmap thumbnail = transform(matrix, source, width, height,
                OPTIONS_SCALE_UP | options);
        return thumbnail;
    }
	
    /**
     * Transform source Bitmap to targeted width and height.
     */
    private static Bitmap transform(Matrix scaler,
            Bitmap source,
            int targetWidth,
            int targetHeight,
            int options) {
        boolean scaleUp = (options & OPTIONS_SCALE_UP) != 0;
        boolean recycle = (options & OPTIONS_RECYCLE_INPUT) != 0;

        int deltaX = source.getWidth() - targetWidth;
        int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            /*
            * In this case the bitmap is smaller, at least in one dimension,
            * than the target.  Transform it by placing as much of the image
            * as possible into the target and leaving the top/bottom or
            * left/right (or both) black.
            */
            Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight,
            Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b2);

            int deltaXHalf = Math.max(0, deltaX / 2);
            int deltaYHalf = Math.max(0, deltaY / 2);
            Rect src = new Rect(
            deltaXHalf,
            deltaYHalf,
            deltaXHalf + Math.min(targetWidth, source.getWidth()),
            deltaYHalf + Math.min(targetHeight, source.getHeight()));
            int dstX = (targetWidth  - src.width())  / 2;
            int dstY = (targetHeight - src.height()) / 2;
            Rect dst = new Rect(
                    dstX,
                    dstY,
                    targetWidth - dstX,
                    targetHeight - dstY);
            c.drawBitmap(source, src, dst, null);
            if (recycle) {
                source.recycle();
            }
            c.setBitmap(null);
            return b2;
        }
        float bitmapWidthF = source.getWidth();
        float bitmapHeightF = source.getHeight();

        float bitmapAspect = bitmapWidthF / bitmapHeightF;
        float viewAspect   = (float) targetWidth / targetHeight;

        if (bitmapAspect > viewAspect) {
            float scale = targetHeight / bitmapHeightF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        } else {
            float scale = targetWidth / bitmapWidthF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        }

        Bitmap b1;
        if (scaler != null) {
            // this is used for minithumb and crop, so we want to filter here.
            b1 = Bitmap.createBitmap(source, 0, 0,
            source.getWidth(), source.getHeight(), scaler, true);
        } else {
            b1 = source;
        }

        if (recycle && b1 != source) {
            source.recycle();
        }

        int dx1 = Math.max(0, b1.getWidth() - targetWidth);
        int dy1 = Math.max(0, b1.getHeight() - targetHeight);

        Bitmap b2 = Bitmap.createBitmap(
                b1,
                dx1 / 2,
                dy1 / 2,
                targetWidth,
                targetHeight);

        if (b2 != b1) {
            if (recycle || b1 != source) {
                b1.recycle();
            }
        }

        return b2;
    }
    	
}
