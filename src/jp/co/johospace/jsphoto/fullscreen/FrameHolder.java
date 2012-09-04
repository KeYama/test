package jp.co.johospace.jsphoto.fullscreen;

import java.io.File;
import java.io.IOException;

import jp.co.johospace.jsphoto.fullscreen.loader.TagHint;
import jp.co.johospace.jsphoto.grid.ExtUtil;
import jp.co.johospace.jsphoto.util.MediaUtil;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.text.TextUtils;

/**
 * ImageFrmaeを保持するクラス
 * 
 */
public class FrameHolder implements MessageDispatcher.Handler{
	/** 物差し。ディスプレイの大きさ等の情報を測る */
	private Measure mMeasure;
	/** 実際のBitmapを保持するオブジェクト */
	private ImageHolder mHolder;
	/* フレームの表示を制御するオブジェクト */
	private FrameScreen mScreen;
	/** 描画に使うマトリクス */
	Matrix mMatrix;
	/** イメージフレームを保持。内容は必要になってから作成 */
	ImageFrame[] mFrames;
	
	public FrameHolder(Measure measure, ImageHolder holder, int initialPosition){
		mMeasure = measure;
		mHolder = holder;
		mScreen = new FrameScreen(measure, initialPosition, holder.size() - 1);
		mFrames = new ImageFrame[holder.size()];
		mMatrix = new Matrix();
	}
	
	/***
	 * 
	 * フレームスクリーンを返す
	 * 
	 * @return
	 */
	public FrameScreen getFrameScreen(){
		return mScreen;
	}
	
	/**
	 * 時間を進める
	 * 
	 * @return　次に更新が必要か否か
	 */
	public boolean process(){
		boolean processed = false;
		
		processed = getFrame(mScreen.getCenterFrameNumber()).process();
		if(!processed)processed |= mScreen.process();
		return processed;
	}
	
	/**
	 * 現在内容を描画する
	 * 
	 * @param c
	 */
	public void draw(Canvas c){
		mMatrix.set(mScreen.getMatrix());
		int p = mScreen.getCenterFrameNumber();
		if(p < 1){
			getFrame(0).draw(c, mMatrix);
			if(mFrames.length > 1)
				getFrame(1).draw(c, mMatrix);
		}else if(p >= mHolder.size() - 1){
			int s = mHolder.size();
			getFrame(s-1).draw(c, mMatrix);
			getFrame(s-2).draw(c, mMatrix);			
		}else{
			getFrame(p-1).draw(c, mMatrix);
			getFrame(p).draw(c, mMatrix);
			getFrame(p+1).draw(c, mMatrix);			
		}
	}
	
	/**
	 * 対象位置のイメージフレームを得る。遅延作成
	 * 
	 * @param at
	 * @return
	 */
	private ImageFrame getFrame(int at){
		if(mFrames[at] == null){
			//JPEGなら方向を得る
			int orientation = ExifInterface.ORIENTATION_NORMAL;
			Object tag = mHolder.getTag(at);
			String path;
			if (tag instanceof TagHint) {
				path = ((TagHint) tag).getFilePath();
			} else {
				path = tag.toString();
			}
			
			if (tag instanceof String || !TextUtils.isEmpty(path)) {
				File file = new File(path);

				String checkString;
				
				// シークレットだった場合、.secretを排除したパスを作成
				if (ExtUtil.isSecret(file)) {
					checkString = ExtUtil.unSecret(new File(path)).getPath();
				} else {
					checkString = path;
				}
				
//				String mimeType = MediaUtil.getMimeTypeFromPath(path);
				String mimeType = MediaUtil.getMimeTypeFromPath(checkString);
				
				if(mimeType != null && mimeType.equals("image/jpeg")){
					try{
						ExifInterface exif = new ExifInterface(path);
						orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
					}catch(IOException e){}
				}
			}
			
			mFrames[at] = new ImageFrame(mMeasure, mHolder, at, at, orientation);
		}
		
		return mFrames[at];
	}
	
	
	/**
	 * 破棄する
	 * 
	 */
	public void dispose(){
		for(ImageFrame frame: mFrames){
			if(frame != null)frame.dispose();
		}
	}

	@Override
	public boolean handleMessage(Object msg) {
		//現在のイメージフレームに問い合わせ、なにもないようであれば自身のスクリーンにメッセージを依頼する
		boolean handled = getFrame(mScreen.getCenterFrameNumber()).getScreen().handleMessage(msg);
		if(!handled)handled = mScreen.handleMessage(msg);
		return handled;
	}
}
