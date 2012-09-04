package jp.co.johospace.jsphoto.ux.renderer.soft;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import jp.co.johospace.jsphoto.ux.loader.UXMessage_FailedImage;
import jp.co.johospace.jsphoto.ux.loader.UXMessage_LoadImage;
import jp.co.johospace.jsphoto.ux.renderer.*;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;
import jp.co.johospace.jsphoto.ux.thread.UXMessage_End;

/**
 * ソフトウェアレンダラのスレッド
 */
class SoftRenderThread extends Thread{
	private UXRenderer mRenderer;
	private UXChannel mChannel;
	private UXSoftRenderEngine mEngine;
	private int mFps;
	private UXRenderEngine.ImageListener mImageListener;

	public static class Message_ChangeBackgroundColor extends UXMessage{
		public int color;
		public Message_ChangeBackgroundColor(int argColor){
			color = argColor;
		}
	}

	private static final Paint mRectPaint = new Paint();

	public void drawRect(int left, int top, int right, int bottom, int color){
		Canvas c = mEngine.getSystemCanvas();
		mRectPaint.setColor(color);
		c.drawRect(left, top, right, bottom, mRectPaint);
	}

	public SoftRenderThread(UXSoftRenderEngine engine, UXRenderer renderer){
		mEngine = engine;
		mRenderer = renderer;
		mChannel = new UXChannel();
	}

	public UXChannel getChannel(){
		return mChannel;
	}

	public int getFps(){
		return mFps;
	}

	public void setImageListener(UXRenderEngine.ImageListener listener){
		mImageListener = listener;
	}

	@Override
	public void run(){
		UXMessage msg = null;
		boolean needRedraw = false;

		mRenderer.initialize(mEngine);

		while(true){
			if(!mChannel.hasMessage() && needRedraw){
				SoftCanvas canvas = mEngine.getCanvas();

				long nano = System.nanoTime();
				if(canvas.begin()){
					mRenderer.render(mEngine);
					canvas.end();

					needRedraw = false;
					long delta = System.nanoTime() - nano;
					mFps = (int)(1000L * 1000L * 1000L / delta);
				}
			}

			msg = mChannel.waitForMessage();

			if(msg instanceof UXMessage_End){
				break;

			}else if(msg instanceof UXMessage_Invalidate){
				needRedraw = true;

			}else if(msg instanceof UXMessage_FailedImage){
				UXMessage_FailedImage msgFailed = (UXMessage_FailedImage)msg;
				if(mImageListener != null)
					mImageListener.onFailedLoadingImage(msgFailed.resource, msgFailed.info);

			}else if(msg instanceof UXMessage_LoadImage){
				UXMessage_LoadImage msgLoad = (UXMessage_LoadImage)msg;
				if(mImageListener != null)
					mImageListener.onLoadImage(msgLoad.resource, msgLoad.info);
				setImage(msgLoad);

			}else if(msg instanceof UXMessage_InvalidateCanvasResource){
				SoftCanvasResource res = (SoftCanvasResource)((UXMessage_InvalidateCanvasResource)msg).resource;
				res.redraw();
			}else if(msg instanceof Message_ChangeBackgroundColor){
				Message_ChangeBackgroundColor msgBg = (Message_ChangeBackgroundColor)msg;
				mEngine.getCanvas().setBackgroundColor(msgBg.color);
			}

			if(msg != null)
				msg.recycleMessage();
		}
		if(msg != null)
			msg.recycleMessage();
	}

	private void setImage(UXMessage_LoadImage msg){
		msg.resource.setImage(msg.info, msg.optBitmap, msg.compressedImage, msg.orientation);
	}
}
