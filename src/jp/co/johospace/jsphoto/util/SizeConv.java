package jp.co.johospace.jsphoto.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * サイズの単位変換を行います。
 */
public class SizeConv {

	private DisplayMetrics displayMetrics;
	private int unit;
	private float scale = 1;
	private static SizeConv instance;
	private static Object syncObject = new Object();
	
	public static SizeConv getInstance(Context context) {
		if(instance==null) {
			synchronized (syncObject) {
				if(instance==null) {
					instance = new SizeConv(context);
				}
			}
		}
		return instance;
	}
	
	/**
	 * コンストラクタ
	 * @param context
	 */
	public SizeConv(Context context) {
		this(TypedValue.COMPLEX_UNIT_DIP, context.getResources().getDisplayMetrics());
	}

	/**
	 * コンストラクタ
	 * @param unit
	 * @param displayMetrics
	 * @param isTextSizeScaling
	 */
	public SizeConv(int unit, DisplayMetrics displayMetrics) {
		this.unit = unit;
		this.displayMetrics = displayMetrics;
		this.scale = 1;
	}

	/**
	 * 画面の対角インチサイズを取得します。
	 */
	public float getDisplayInch() {
		float width = displayMetrics.widthPixels / displayMetrics.xdpi;
		float height = displayMetrics.heightPixels / displayMetrics.ydpi;
		return (float)Math.sqrt(((width * width) + (height * height)));
	}

	/**
	 * サイズを変換します
	 * @param size DIPサイズ
	 * @return 変換結果
	 */
	public float getSize(float size) {
		return TypedValue.applyDimension(unit, size, displayMetrics) * scale;
	}
}
