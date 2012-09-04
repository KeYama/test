package jp.co.johospace.jsphoto.ux.renderer.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.co.johospace.jsphoto.ux.loader.UXMessage_FailedImage;
import jp.co.johospace.jsphoto.ux.loader.UXMessage_LoadImage;
import jp.co.johospace.jsphoto.ux.renderer.UXMessage_Invalidate;
import jp.co.johospace.jsphoto.ux.renderer.UXMessage_InvalidateCanvasResource;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderEngine;
import jp.co.johospace.jsphoto.ux.renderer.UXRenderer;
import jp.co.johospace.jsphoto.ux.renderer.UXResource;
import jp.co.johospace.jsphoto.ux.thread.UXChannel;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;
import jp.co.johospace.jsphoto.ux.thread.UXMessage_End;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLUtils;

public class GLRenderer implements CustomGLSurfaceView.Renderer{
	private UXGLRenderEngine mEngine;
	private UXRenderer mRenderer;
	private UXChannel mChannel;
	private GLCanvas mCanvas;
	private UXRenderEngine.ImageListener mImageListener;
	private GL10 mGL;
	private boolean mNeedRedraw;
	private int mFps;
	private boolean mDrawEnd = false;
	private float mTexW, mTexH;
	private float mScreenW, mScreenH;
	private static FloatBuffer mTexturBuffer, mRangeBuffer, m9ScaleTexbuffer, m9ScaleRangebuffer
	;
	private float[] mUv = new float[8];
	private float[] m9scaleUv = new float[44];
	private float[] mPositions = new float[12];
	private float[] m9scalePositions = new float[44];

	private int mColor;
	private boolean mReloadFlag;
	private long mReloadMS;
	private CustomGLSurfaceView mView;
	
	private My9scaleParam mMy9ScaleParam = new My9scaleParam();
	
	private ArrayList<UXMessage_LoadImage> mImgMessages = new ArrayList<UXMessage_LoadImage>();

	public GLRenderer(UXGLRenderEngine engin, UXRenderer renderer, CustomGLSurfaceView view){
		mEngine = engin;
		mRenderer = renderer;
		mChannel = new GLRendererChannel();
		mCanvas = new GLCanvas(this);
		mView = view;
	}

	private class GLRendererChannel extends UXChannel{

		@Override
		public synchronized long postMessage(UXMessage message,
				boolean emergency) {
			long id = super.postMessage(message, emergency);
			mView.requestRender();
			return id;
		}

		@Override
		public synchronized long postMessage(UXMessage message) {
			long id =  super.postMessage(message);
			mView.requestRender();
			return id;
		}

		@Override
		public synchronized boolean postSingleMessage(UXMessage message) {
			boolean flag = super.postSingleMessage(message);
			mView.requestRender();
			return flag;
		}

		@Override
		public synchronized boolean cancelMessage(long id) {
			return super.cancelMessage(id);
		}

		@Override
		public synchronized boolean hasMessage() {
			return super.hasMessage();
		}

		@Override
		public synchronized UXMessage waitForMessage() {
			return super.waitForMessage();
		}

	}

	private boolean mSkip = false;

	static class Message_SkipStart extends UXMessage{
	}

	static class Message_SkipEnd extends UXMessage{
	}

	static class Message_ChangeColor extends UXMessage{
		public int color;
		public Message_ChangeColor(int argColor){
			color = argColor;
		}
	}

	/**
	 * 9scaleを描画します。
	 * @param texName
	 * @param texWidth
	 * @param texHeight
	 * @param scale
	 * @param dst
	 * @param texRange
	 * @return
	 */
	public boolean draw9scale(int texName, int texWidth, int texHeight, Rect scale, Rect dst, Rect texRange){

		//アルファブレンドをonにする
		alphaBlendingSwitch(true);

		final int cornerWidht = (texRange.width() /2) - (scale.width() /2);
		final int cornerHeight = (texRange.height() /2) - (scale.height() /2);

		if(cornerWidht * 2 > dst.width() || cornerHeight * 2 > dst.height()){
			return false;
		}
		setTexture(texName, texWidth, texHeight);
		set9scaleArea(mGL, texRange, scale);
		draw9scaleRange(mGL, dst, texRange, scale);

		return true;
	}

	public void drawRect(int left, int top, int right, int bottom, int color) {
		if(Color.alpha(color) == 255){
			alphaBlendingSwitch(false);
		}else{
			alphaBlendingSwitch(true);
		}

		mGL.glColor4f(
				Color.red(color)/255.0f,
				Color.green(color)/255.0f,
				Color.blue(color)/255.0f,
				Color.alpha(color)/255.0f
		);
		checkError();


		mGL.glBindTexture(GL10.GL_TEXTURE_2D, 0);
		checkError();
		drawQuad(left, top, right, bottom);

		mGL.glColor4f(1,1,1,1);
		checkError();
	}

	/**
	 * 表示するテクスチャのエリアを設定します。
	 * @param gl
	 * @param texArea
	 * @param scale
	 */
	private void set9scaleArea(GL10 gl, Rect texArea, Rect scale){

		My9scaleParam param = mMy9ScaleParam;
		param.setArea(texArea, scale);

		//左上
		m9scaleUv[0] = param.left;
		m9scaleUv[1] = param.top;
		m9scaleUv[2] = param.left;
		m9scaleUv[3] = param.center.top;
		m9scaleUv[4] = param.center.left;
		m9scaleUv[5] = param.top;
		m9scaleUv[6] = param.center.left;
		m9scaleUv[7] = param.center.top;

		//中上
		m9scaleUv[8] = param.center.right;
		m9scaleUv[9] = param.top;
		m9scaleUv[10] = param.center.right;
		m9scaleUv[11] = param.center.top;
		m9scaleUv[12] = param.right;
		m9scaleUv[13] = param.top;
		m9scaleUv[14] = param.right;
		m9scaleUv[15] = param.center.top;

		//中右
		m9scaleUv[16] = param.right;
		m9scaleUv[17] = param.center.bottom;
		m9scaleUv[18] = param.center.right;
		m9scaleUv[19] = param.center.top;
		m9scaleUv[20] = param.center.right;
		m9scaleUv[21] = param.center.bottom;

		//中中
		m9scaleUv[22] = param.center.left;
		m9scaleUv[23] = param.center.top;
		m9scaleUv[24] = param.center.left;
		m9scaleUv[25] = param.center.bottom;

		//中左
		m9scaleUv[26] = param.left;
		m9scaleUv[27] = param.center.top;
		m9scaleUv[28] = param.left;
		m9scaleUv[29] = param.center.bottom;

		//下左
		m9scaleUv[30] = param.left;
		m9scaleUv[31] = param.bottom;
		m9scaleUv[32] = param.center.left;
		m9scaleUv[33] = param.center.bottom;
		m9scaleUv[34] = param.center.left;
		m9scaleUv[35] = param.bottom;


		//下中
		m9scaleUv[36] = param.center.right;
		m9scaleUv[37] = param.center.bottom;
		m9scaleUv[38] = param.center.right;
		m9scaleUv[39] = param.bottom;

		//下右
		m9scaleUv[40] = param.right;
		m9scaleUv[41] = param.center.bottom;
		m9scaleUv[42] = param.right;
		m9scaleUv[43] = param.bottom;

		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		checkError();
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, get9ScaleBuffer(m9scaleUv));
		checkError();
	}

	/**
	 * 9scaleを描画します。
	 * @param gl
	 * @param dst
	 * @param texArea
	 * @param scale
	 */
	private  void draw9scaleRange(GL10 gl,Rect dst, Rect texArea, Rect scale){// float x, float y, float w, float h){

		My9scaleParam param = mMy9ScaleParam;
		param.setDraw(dst, scale, texArea);

		//上左
		m9scalePositions[0] = param.left;
		m9scalePositions[1] = param.top;
		m9scalePositions[2] = param.left;
		m9scalePositions[3] = param.center.top;
		m9scalePositions[4] = param.center.left;
		m9scalePositions[5] = param.top;
		m9scalePositions[6] = param.center.left;
		m9scalePositions[7] = param.center.top;

		//上中
		m9scalePositions[8] = param.center.right;
		m9scalePositions[9] = param.top;
		m9scalePositions[10] = param.center.right;
		m9scalePositions[11] = param.center.top;

		//上右
		m9scalePositions[12] = param.right;
		m9scalePositions[13] = param.top;
		m9scalePositions[14] = param.right;
		m9scalePositions[15] = param.center.top;

		//中右
		m9scalePositions[16] =param.right ;
		m9scalePositions[17] = param.center.bottom;
		m9scalePositions[18] = param.center.right;
		m9scalePositions[19] = param.center.top;
		m9scalePositions[20] = param.right;
		m9scalePositions[21] = param.center.bottom;

		//中中
		m9scalePositions[22] = param.center.left;
		m9scalePositions[23] = param.center.top;
		m9scalePositions[24] = param.center.left;
		m9scalePositions[25] = param.center.bottom;

		//中左
		m9scalePositions[26] = param.left;
		m9scalePositions[27] = param.center.top;
		m9scalePositions[28] = param.left;
		m9scalePositions[29] = param.center.bottom;


		//下左
		m9scalePositions[30] = param.left;
		m9scalePositions[31] = param.bottom;
		m9scalePositions[32] = param.center.left;
		m9scalePositions[33] = param.center.bottom;
		m9scalePositions[34] = param.center.left;
		m9scalePositions[35] = param.bottom;

		//下中
		m9scalePositions[36] = param.center.right;
		m9scalePositions[37] = param.center.bottom;
		m9scalePositions[38] = param.center.right;
		m9scalePositions[39] = param.bottom;

		//下右
		m9scalePositions[40] = param.right;
		m9scalePositions[41] = param.center.bottom;
		m9scalePositions[42] = param.right;
		m9scalePositions[43] = param.bottom;

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		checkError();
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, get9ScaleRangeBuffer(m9scalePositions));
		checkError();
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 22);
		checkError();
	}

	/**
	 * 9scaleの座標データを管理します。
	 */
	private class My9scaleParam{
		public float left, top, right, bottom;
		public RectF center;
		public My9scaleParam(){
			center = new RectF();
		}

		public void setArea(Rect texArea, Rect scale){

			float cornerH = ((texArea.height() - scale.height()) / 2) / mTexH;
			float cornerW = ((texArea.width() - scale.width()) /2) / mTexW;
			left = texArea.left / mTexW;
			top = texArea.top / mTexH;
			right = texArea.right / mTexW;
			bottom = texArea.bottom / mTexH;

			center.top =  top + cornerH;
			center.bottom = center.top + (scale.height() / mTexH);
			center.left = left + cornerW;
			center.right = center.left + (scale.width() / mTexW);
		}

		public void setDraw(Rect dst, Rect scale, Rect texArea){
			float cornerH = ((((texArea.height() - scale.height()) /2) / mScreenH) * 2f);
			float cornerW = ((((texArea.width() - scale.width()) /2) / mScreenW) * 2f);

			left = (dst.left / mScreenW) * 2.0f - 1.0f;
			top = (dst.top / mScreenH) * 2.0f - 1.0f;
			right = left + ((dst.width() / mScreenW) * 2.0f);
			bottom = top + ((dst.height() / mScreenH) * 2.0f);

			center.left = left + cornerW;
			center.top = top + cornerH;
			center.right = right - cornerW;
			center.bottom = bottom - cornerH;

			top = -top;
			bottom = -bottom;
			center.top = -center.top;
			center.bottom = -center.bottom;
		}
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if(mDrawEnd)return;
		if(mSkip && !mChannel.hasMessage()){
			mView.requestRender();
			return;
		}

		if(mReloadFlag){
			mReloadFlag = false;
		}
		if(mReloadFlag){
			mView.requestRender();
			return;
		}

		while(true){
			UXMessage msg = null;
			if(mChannel.hasMessage()){
				msg = mChannel.waitForMessage();
			}

			if(msg instanceof UXMessage_End){
				mDrawEnd = true;
			}else if(msg instanceof Message_SkipStart){
				mSkip = true;
				break;
			}else if(msg instanceof Message_SkipEnd){
				mSkip = false;
			}else if (msg instanceof Message_ChangeColor){
				mColor = ((Message_ChangeColor)msg).color;

			}else if(msg instanceof UXMessage_Invalidate){
				mNeedRedraw = true;

			}else if(msg instanceof UXMessage_FailedImage){
				UXMessage_FailedImage msgFailed = (UXMessage_FailedImage)msg;
				if(mImageListener != null)
					mImageListener.onFailedLoadingImage(msgFailed.resource, msgFailed.info);

			}else if(msg instanceof UXMessage_LoadImage){
				UXMessage_LoadImage msgLoad = (UXMessage_LoadImage)msg;
				if(mImageListener != null)
					mImageListener.onLoadImage(msgLoad.resource, msgLoad.info);
				setImage(msgLoad);
				
//				mImgMessages.add(msgLoad);
				mEngine.invalidate();
			}else if(msg instanceof UXMessage_InvalidateCanvasResource){
				NewGLCanvasResource res = (NewGLCanvasResource)((UXMessage_InvalidateCanvasResource)msg).resource;
				res.redraw();
				mEngine.invalidate();
			}
			if(msg != null){
				msg.recycleMessage();
			}
			if(!mChannel.hasMessage()){
				
				//制限付きでアップロード
				int maxUpload = Math.min(3, mImgMessages.size());
				for(int n = 0; n < maxUpload; ++n){
					UXMessage_LoadImage msgLoad = mImgMessages.remove(0);
					if(mImageListener != null)
						mImageListener.onLoadImage(msgLoad.resource, msgLoad.info);
					setImage(msgLoad);
				}
				
				
				//if(mNeedRedraw){
					long nano = System.nanoTime();
					//描画
					mGL.glClearColor(
							Color.red(mColor)/255.0f,
							Color.green(mColor)/255.0f,
							Color.blue(mColor)/255.0f,
							Color.alpha(mColor)/255.0f
							);
					checkError();
					mGL.glClear(GL10.GL_COLOR_BUFFER_BIT);
					checkError();
					mRenderer.render(mEngine);
					long delta = System.nanoTime() - nano;
					mFps = (int)(1000L *
							1000L * 1000L / delta);
				//}
				break;
			}
		}
	}

	private boolean isAlphaBlending;
	/**
	 * アルファブレンドのon, offを管理します。
	 * @param on
	 */
	private void alphaBlendingSwitch(boolean on){

		if(on){
			if(!isAlphaBlending){
				//アルファブレンドを有効か
				mGL.glEnable(GL10.GL_BLEND);
				checkError();
				mGL.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
				checkError();

				isAlphaBlending = true;
			}
		}else{
			if(isAlphaBlending){
				//アルファブレンドを無効化
				mGL.glDisable(GL10.GL_BLEND);
				checkError();

				isAlphaBlending = false;
			}
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {

//		android.util.Log.e("dbg", "onSurface");

		//GLCanvasの初期化
		mCanvas.reCreate();

		gl.glViewport(0, 0, width, height);
		reloadResources();
		mReloadMS = System.currentTimeMillis();

		mScreenW = width;
		mScreenH = height;

		mChannel.postSingleMessage(UXMessage_Invalidate.create());
	}

	private void reloadResources() {
		//まずテクスチャ削除
		mEngine.clearTextureAtlas();

		ArrayList<UXResource> resources = mEngine.getResources();
		for(UXResource res : resources){
			if(res instanceof GLImageResource){
				GLImageResource imgRes = ((GLImageResource)res);
				imgRes.clearTexture();

			}else if(res instanceof GLCanvasResource){
				((GLCanvasResource) res).reCreate();
			}
		}
		//それから作成
		mEngine.reloadTextureAtlas();

		for(UXResource res : resources){
			if(res instanceof GLImageResource){
				GLImageResource imgRes = ((GLImageResource)res);
				imgRes.reloadImage();

			}else if(res instanceof GLCanvasResource){
				((GLCanvasResource) res).reCreate();
			}
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		mGL = gl;
		mRenderer.initialize(mEngine);
	}

	/**
	 * glに画像をバインドします。返り値としてテクスチャネームを返します。
	 */
	public GLTextureParam bindImage(Bitmap bitmap){

		int[] textures = new int[1];
		mGL.glGenTextures(1, textures, 0);
		checkError();
		mGL.glEnable(GL10.GL_TEXTURE_2D);
		checkError();

		mGL.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		checkError();

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		if(!checkExponentiation(bitmap)){
			//2のべき乗の値を取得
			width = getMultiplier(bitmap);
			height = width;
		}

		GLTextureParam texParam = new GLTextureParam();
		texParam.texName = textures[0];
		texParam.texWidth = width;
		texParam.texHeight = height;

		mGL.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 2);
		checkError();

		mGL.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGB, width, height,
				0, GL10.GL_RGB, GL10.GL_UNSIGNED_SHORT_5_6_5, null);
		checkError();
		//画像を追加
		GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, bitmap);
		checkError();

		mGL.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		checkError();
		mGL.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		checkError();
		return texParam;
	}

	/**
	 * 2のべき乗かどうかを確かめます。
	 * @param bitmap
	 * @return
	 */
	private boolean checkExponentiation(Bitmap bitmap){
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		return ((w &(w -1)) == 0) && ((h& (h -1)) == 0);
	}

	/**
	 * 画像より大きい2のべき乗の値を返します。
	 * @param bitmap
	 * @return
	 */
	private int getMultiplier(Bitmap bitmap){

		int bigSize;
		if(bitmap.getWidth() >= bitmap.getHeight())
			bigSize = bitmap.getWidth();
		else
			bigSize = bitmap.getHeight();

		int multipler = 1;
		while(true){
			multipler *= 2;
			if(multipler >= bigSize){
				break;
			}
		}
		return multipler;
	}

	/**
	 * glに画像を表示する
	 * @param texName
	 * @param texWidth
	 * @param texHeight
	 * @param src
	 * @param dst
	 * @param alpha
	 */
	public void draw(int texName, int texWidth, int texHeight, Rect src, Rect dst, float alpha, boolean isCanvasRes){

		if(isCanvasRes){
			alphaBlendingSwitch(true);
		}else{
			if(alpha == 1.0f)
				alphaBlendingSwitch(false);
			else
				alphaBlendingSwitch(true);
		}

		mGL.glColor4f(1.0f, 1.0f, 1.0f, alpha);
		checkError();

		setTexture(texName, texWidth, texHeight);
		setTextureArea(src.left, src.top, src.right, src.bottom);
		drawQuad(dst.left, dst.top, dst.right, dst.bottom);
	}

	public void draw(int texName, int texWidth, int texHeight,
			int x, int y ,int imageWidth, int imageHeight, int texX, int texY){

		//ここでヘッダーを表示している
		//GLCanvasResのみで使用するので、アルファブレンドはonにする。
		alphaBlendingSwitch(true);

		setTexture(texName, texWidth, texHeight);
		setTextureArea(texX, texY, texX + imageWidth, texY + imageHeight);
		drawQuad(x, y, x + imageWidth, y + imageHeight);
	}

	/**
	 * 画像を回転描画する。
	 *
	 */
	public void drawRotation(int texName, int texWidth, int texHeight, Rect src, Rect dst, float alpha, int rotation){

		if(alpha == 1.0f)
			alphaBlendingSwitch(false);
		else
			alphaBlendingSwitch(true);
		mGL.glColor4f(1.0f, 1.0f, 1.0f, alpha);
		checkError();

		rotation = getFixRotation(rotation);		
		
		setTexture(texName, texWidth, texHeight);
		setTextureArea(src.left, src.top, src.right, src.bottom);
		float imaWidth = dst.right - dst.left;
		float imaHeight = dst.bottom - dst.top;

		int screenWidth = 0;
		int screenHeight = 0;
		if(rotation == 90 || rotation == 270){
			screenHeight = (int) mScreenH;
			screenWidth = (int) mScreenW;
			mScreenH = screenWidth;
			mScreenW = screenHeight;
		}
		float xx = 2.0f / mScreenW;
		float xy = 2.0f / mScreenH;

		float left = xx * ((mScreenW / 2)  - (dst.left  + (imaWidth / 2)));
		float top = xy * ((mScreenH / 2) - (dst.top + (imaHeight /2)));

		float backLeft = 0;
		float backTop = 0;
		if(rotation == 90 || rotation == 270){
			backTop = xx * ((imaWidth /2) * -1 + ((mScreenW /2) - dst.top) + ((imaWidth / 2) -(imaHeight /2)));//yCelnterIdou + yTopIdou + yImageHanbun;
			backLeft = xy * ((imaHeight /2) + (dst.left -(mScreenH / 2)) + ((imaWidth / 2) - (imaHeight /2)));

		}else{
			backTop = top;
			backLeft = -left;
		}

		mGL.glPushMatrix();
		mGL.glMatrixMode(GL10.GL_MODELVIEW);

		mGL.glTranslatef(backLeft, backTop, 0.0f);
		mGL.glRotatef(rotation, 0.0f, 0.0f, 1.0f);
		mGL.glTranslatef(left, -top, 0.0f);

		drawQuad(dst.left, dst.top, dst.right, dst.bottom);
		mGL.glPopMatrix();
		if(rotation == 90 || rotation == 270){
			mScreenW = screenWidth;
			mScreenH = screenHeight;
		}
	}
	
	/**
	 * GLは反時計回りなので時計回りで表示される値を返す
	 */
	private int getFixRotation(int rotation){

		switch (rotation) {
		case 90:
			rotation = 270;
			break;
			
		case 180:
			rotation = 180;
			break;

		case 270:
			rotation = 90;
			break;
			
		case 360:
			rotation = 360;
			break;
		}
		return rotation;
	}

	/**
	 * テクスチャを描画します。
	 */
	private void drawQuad(float left, float top, float right, float bottom){

		left = left/ mScreenW * 2.0f -1.0f;
		right = right / mScreenW * 2.0f -1.0f;
		top = (top / mScreenH * 2.0f - 1.0f) * -1;
		bottom = (bottom / mScreenH * 2.0f -1.0f) * -1;

		mPositions[0] = left;
		mPositions[1] = top;
		mPositions[2] = 0.0f;
		mPositions[3] = left;
		mPositions[4] = bottom;
		mPositions[5] = 0.0f;
		mPositions[6] = right;
		mPositions[7] = top;
		mPositions[8] = 0.0f;
		mPositions[9] = right;
		mPositions[10] = bottom;
		mPositions[11] = 0.0f;

		FloatBuffer fb = getTexturtBuffer(mPositions);
		mGL.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		checkError();
		mGL.glVertexPointer(3, GL10.GL_FLOAT, 0, fb);
		checkError();
		mGL.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		checkError();
	}

	/**
	 * テクスチャの表示エリアを設定します。
	 */
	private void setTextureArea(float left, float top, float right, float bottom){

		left = left / mTexW;
		top = top / mTexH;
		right = right/ mTexW;
		bottom = bottom / mTexH;

		mUv[0] = left;
		mUv[1] = top;
		mUv[2] = left;
		mUv[3] = bottom;
		mUv[4] = right;
		mUv[5] = top;
		mUv[6] = right;
		mUv[7] = bottom;

		FloatBuffer fb = getRangeBuffer(mUv);
		mGL.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		checkError();
		mGL.glTexCoordPointer(2, GL10.GL_FLOAT, 0, fb);
		checkError();
	}

	/**
	 * 使用するテクスチャを設定します。
	 * @param texName
	 * @param texWidh
	 * @param texHeight
	 */
	private void setTexture(int texName, int texWidh, int texHeight){
		//有効にする
		mGL.glEnable(GL10.GL_TEXTURE_2D);
		checkError();
		//バインドします。
		mGL.glBindTexture(GL10.GL_TEXTURE_2D, texName);
		checkError();

		mTexH = texHeight;
		mTexW = texWidh;
	}

	/**
	 * テクスチャに画像を追加します。
	 */
	public void addImageToTexture(int texName, int xoffset, int yoffset, Bitmap bitmap){

		mGL.glEnable(GL10.GL_TEXTURE_2D);
		checkError();
		mGL.glBindTexture(GL10.GL_TEXTURE_2D, texName);
		checkError();

		mGL.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 2);
		checkError();
		GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, xoffset, yoffset, bitmap, GL10.GL_RGBA, GL10.GL_UNSIGNED_SHORT_4_4_4_4);
		checkError();

	}

	/**
	 * テクスチャを作成します。
	 * @param width
	 * @param height
	 * @return
	 */
	public int createTexture(int width, int height){
		int[] textures = new int[1];
		mGL.glGenTextures(1, textures, 0);
		checkError();

		mGL.glEnable(GL10.GL_TEXTURE_2D);
		checkError();

		mGL.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		checkError();

		mGL.glPixelStorei(GL10.GL_UNPACK_ALIGNMENT, 2);
		checkError();

		//テクスチャを作成します。
		mGL.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, width, height,
				0, GL10.GL_RGBA, GL10.GL_UNSIGNED_SHORT_4_4_4_4, null);
		checkError();

		mGL.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		checkError();
		mGL.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
		checkError();

		return textures[0];
	}
//	List<Integer> mCount

	/**
	 * テクスチャを削除します。
	 * @param textureName
	 */
	public void deleteTexture(int textureName){
		mGL.glDeleteTextures(1, new int[] {textureName}, 0);
		
		checkError();
	}

	private void checkError(){
		int i = mGL.glGetError();
		if(i != 0){
//			throw new RuntimeException(GLU.gluErrorString(i));
		}
	}

	/**
	 * FloatBufferを返します。
	 */
	private FloatBuffer getTexturtBuffer(float[] positions){	
		return mTexturBuffer = getFloatBuffer(mTexturBuffer, positions);
	}

	private FloatBuffer getRangeBuffer(float[] uv){
		return mRangeBuffer = getFloatBuffer(mRangeBuffer, uv);
	}

	private FloatBuffer get9ScaleBuffer(float[] uv){
		return m9ScaleTexbuffer = getFloatBuffer(m9ScaleTexbuffer, uv);
	}

	private FloatBuffer get9ScaleRangeBuffer(float[] positoin){
		return m9ScaleRangebuffer = getFloatBuffer(m9ScaleRangebuffer, positoin);
	}

	private FloatBuffer getFloatBuffer(FloatBuffer fb, float[] arg){
		if(fb == null){
			ByteBuffer bb = ByteBuffer.allocateDirect(arg.length * 4);
			bb.order(ByteOrder.nativeOrder());
			fb = bb.asFloatBuffer();
		}else{
			fb.clear();
		}
		fb.put(arg);
		fb.position(0);

		return fb;
	}

	public GLCanvas getGLCanvas(){
		return mCanvas;
	}

	public int getFps(){
		return mFps;
	}

	public void setImageListener(UXRenderEngine.ImageListener listener){
		mImageListener = listener;
	}

	public UXChannel getChannel(){
		return mChannel;
	}

	private void setImage(UXMessage_LoadImage msg){
		msg.resource.setImage(msg.info, msg.optBitmap, msg.compressedImage, msg.orientation);
	}
}
