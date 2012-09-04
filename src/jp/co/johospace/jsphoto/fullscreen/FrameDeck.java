package jp.co.johospace.jsphoto.fullscreen;

import java.util.List;

import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoaderFactory;
import jp.co.johospace.jsphoto.fullscreen.loader.ImageLoaderRunner;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * 描画の大本になるクラス。
 * 
 * 
 */
public class FrameDeck implements MessageDispatcher.Handler{
	private ImageLoaderRunner mLoader;
	private ImageHolder mHolder;
	
	private int mWidth, mHeight;
	private SurfaceHolder mSurfaceHolder;
	private Measure mMeasure;
	private FrameHolder mFrameHolder;
	
	public FrameDeck(MessageDispatcher dispatcher, ImageLoaderFactory factory,
			List<?> tags, int startPosition){
		mLoader = new ImageLoaderRunner(dispatcher, factory);
		mHolder = new ImageHolder(mLoader, tags);
		mMeasure = new Measure();
		
		mFrameHolder = new FrameHolder(mMeasure, mHolder, startPosition);
		
		//接続
		dispatcher.addMessageHandler(this);
		dispatcher.addMessageHandler(mFrameHolder);
		dispatcher.addMessageHandler(mHolder);
	}
	
	/**
	 * 描画を行う
	 * 
	 * @return 次に呼び出すと更新があるならばtrue,なければfalse
	 */
	public boolean draw(){
		if(mSurfaceHolder == null)return false;
		boolean processed = process();
		
		Canvas c = mSurfaceHolder.lockCanvas();
		
		if(c != null){
			c.drawRGB(0, 0, 0);
			mFrameHolder.draw(c);
		
			mSurfaceHolder.unlockCanvasAndPost(c);
		}
		
		return processed;
	}
	
	/**
	 * 時間を進める
	 * 
	 * @return 次に更新があるかどうか
	 */
	private boolean process(){
		boolean processed = false;
		
		processed |= mFrameHolder.process();
		return processed;
	}
	
	/**
	 * 現在表示中のフレーム位置を返す
	 * 
	 * @return
	 */
	public int getCurrentFrame(){
		return mFrameHolder.getFrameScreen().getCenterFrameNumber();
	}

	@Override
	public boolean handleMessage(Object msg) {
		if(msg instanceof MessageChannel.SurfaceChanged){
			onSurfaceChanged((MessageChannel.SurfaceChanged)msg);
		}
		return false;
	}
	
	public void dispose(){
		mFrameHolder.dispose();
	}
	
	public boolean isFrameNeutral(){
		return mFrameHolder.getFrameScreen().isNeutral();
	}
	
	/**
	 * 
	 * サーフェイスビューのサーフェイスが変更されたときに呼ばれるハンドラ
	 * 
	 * @param msg
	 */
	private void onSurfaceChanged(MessageChannel.SurfaceChanged msg){
		mWidth = msg.width;
		mHeight = msg.height;
		mSurfaceHolder = msg.holder;
		mLoader.setScreenSize(mWidth, mHeight);
		mMeasure.setScreenSize(mWidth, mHeight);
	}
}
