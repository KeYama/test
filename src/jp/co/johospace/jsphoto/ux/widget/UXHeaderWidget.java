package jp.co.johospace.jsphoto.ux.widget;

import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * ヘッダウィジェット
 */
public class UXHeaderWidget extends UXWidget {
	private static final String DOT = "...";
	private Paint mPaint = new Paint();
	private String mText;
	private String mRightText;

	private float mTextSize;
	private int mTextSizeUnit;
	private float mTextSizePx;
	private int mTextHeight;

	private UXCanvasResource mCanvasResource;
	private UXCanvasResource mRightCanvasResource;

	private UXCanvasResource mIcon;
	private float mIconWidth;
	private float mIconHeight;
	private int mIconWidthUnit;
	private int mIconHeightUnit;
	private float mIconWidthPx;
	private float mIconHeightPx;

	private Drawable mIconDrawable;

	private int mBackgroundColor;
	private boolean mBackgroundFlag;

	private float mMargin;
	private float mMarginPx;
	private int mMarginUnit;

	public UXHeaderWidget(String text, String rightText, float size, int textSizeUnit){
		mText = text;
		mRightText = rightText;
		mTextSize = size;
		mTextSizeUnit = textSizeUnit;
	}

	public UXHeaderWidget backgroundColor(int color, boolean flag){
		mBackgroundColor = color;
		mBackgroundFlag = flag;

		return this;
	}

	public UXHeaderWidget margin(float margin, int unit){
		mMargin = margin;
		mMarginUnit = unit;
		return this;
	}

	public UXHeaderWidget icon(Drawable icon, float iconWidth, int iconWidthUnit, float iconHeight, int iconHeightUnit){
		mIconDrawable = icon;
		mIconWidth = iconWidth;
		mIconHeight = iconHeight;
		mIconWidthUnit = iconWidthUnit;
		mIconHeightUnit = iconHeightUnit;
		return this;
	}

	public UXHeaderWidget textColor(int color){
		mPaint.setColor(color);

		return this;
	}

	public void initializeResource(UXRenderEngine engine){

		if(mCanvasResource != null){
			return;
		}

		mCanvasResource = engine.createCanvasResource((int)mPaint.measureText(mText), mTextHeight,
				new UXCanvasResource.CanvasRenderer() {
					@Override
					public void draw(Canvas canvas) {
						mPaint.setTextSize(mTextSizePx);
						Paint.FontMetrics m = mPaint.getFontMetrics();

						canvas.drawText(mText, 0, -m.top, mPaint);
					}
				}, false);

		mCanvasResource.invalidate();

		if(mRightText != null && mRightCanvasResource == null){

			mRightCanvasResource = engine.createCanvasResource((int)mPaint.measureText(mRightText), mTextHeight,
					new UXCanvasResource.CanvasRenderer() {
						@Override
						public void draw(Canvas canvas) {
							mPaint.setTextSize(mTextSizePx);
							Paint.FontMetrics m = mPaint.getFontMetrics();

							canvas.drawText(mRightText, 0, -m.top, mPaint);
						}
					}, false);

			mRightCanvasResource.invalidate();
		}

		if(mIconDrawable != null){
			mIcon = engine.createCanvasResource((int)mIconWidthPx, (int)mIconHeightPx,
					new UXCanvasResource.CanvasRenderer() {
						@Override
						public void draw(Canvas canvas) {
							mIconDrawable.setBounds(new Rect(0, 0, (int)mIconWidthPx, (int)mIconHeightPx));
							mIconDrawable.draw(canvas);
						}
					}
				, false);
		}
	}

	@Override
	public void layout(UXStage stage) {
		mMarginPx = computeUnit(stage, mMargin, ORIENTATION_X, mMarginUnit);

		mTextSizePx = computeUnit(stage, mTextSize, ORIENTATION_Y, mTextSizeUnit);

		mPaint.setTextSize(mTextSizePx);
		
		mText = limitText(stage, mText, mPaint);
		
		Paint.FontMetrics metrics = mPaint.getFontMetrics();

		width(stage.getScreenWidth(), UXUnit.PX);
		height(mTextSizePx+metrics.bottom - metrics.top, UXUnit.PX);

		mTextHeight = (int)(metrics.bottom - metrics.top);

		mIconWidthPx = computeUnit(stage, mIconWidth, ORIENTATION_X, mIconWidthUnit);
		mIconHeightPx = computeUnit(stage, mIconHeight, ORIENTATION_Y, mIconHeightUnit);

		super.layout(stage);
	}
	
	private String limitText(UXStage stage, String text, Paint paint){
		int width = (int)(stage.getDisplayWidth() * 0.7f);
		int length = 0;
		
		for(;length < text.length() && width > paint.measureText(text.substring(0, length)+DOT); ++length);
		if(length == text.length())return text;
		if(length != 0)length--;
		
		return text.substring(0, length) + DOT;
	}

	@Override
	protected boolean protectedDraw(UXRenderEngine engine, RectF viewPort) {
		if(!checkVisible(viewPort))return false;

		initializeResource(engine);

		float drawX = getAbsX() + mMarginPx;
		float drawY = getAbsY() + mMarginPx;

		if(mBackgroundFlag){
			engine.drawRect(
					(int)mMarginPx/2,
					(int)(getAbsY() - viewPort.top + mMarginPx/2),
					(int)(engine.getWidth() - mMarginPx/2),
					(int)(getAbsY() - viewPort.top + mMarginPx*1.5 + mTextHeight),
					mBackgroundColor
				);
		}

		if(mIcon != null){
			float iconY = getAbsY() + (mMarginPx*2+mTextHeight)/2 - mIconHeightPx/2 - viewPort.top;
			mIcon.draw((int)drawX, (int)iconY);
			drawX += mIconWidthPx + mMarginPx;
		}

		mCanvasResource.draw(
				(int)(drawX - viewPort.left),
				(int)(drawY - viewPort.top));

		if(mRightCanvasResource != null){
			mRightCanvasResource.draw(
					(int)(engine.getWidth() - mMarginPx - mRightCanvasResource.getWidth()),
					(int)(drawY - viewPort.top)
			);
		}

		return true;
	}


	@Override
	public void deactivate() {
		super.deactivate();
		//それほど量が多くないと思われるのでキャッシュしておく
		//mCanvasResource.purgeMemory();
		//mRightCanvasResource.purgeMemory();
		//if(mIcon != null)mIcon.purgeMemory();
	}
}
