package jp.co.johospace.jsphoto.ux.renderer.gl;

import java.util.HashMap;
import java.util.Random;

import jp.co.johospace.jsphoto.ux.renderer.gl.GLCanvasResource.CanResParam;
import android.graphics.Bitmap;
import android.util.Log;

public class GLCanvas {
	private static final int NOT_VALID = Integer.MIN_VALUE;
	private static final int TEXTURE_HEIGHT = 1024;
	private static final int TEXTURE_WIDTH = 1024;

	private HashMap<Integer, TextureParam> mTextureMap = new HashMap<Integer, GLCanvas.TextureParam>();	//key テクスチャネーム
	private GLRenderer mRenderer;
	private int mIdConut;
	public GLCanvas(GLRenderer renderer){

		mTextureMap = new HashMap<Integer, GLCanvas.TextureParam>();
		mRenderer = renderer;
	}

	/**
	 * テクスチャを作成する
	 */
	public TextureParam createTexture(){

		//テクスチャを作成
		TextureParam texparam = new TextureParam();
		texparam.texName = mRenderer.createTexture(TEXTURE_WIDTH, TEXTURE_HEIGHT);
		return texparam;
	}

	/**
	 * Bitmapをテクスチャに追加する
	 * @param bitmap
	 */
	public void drawImageToTexture(CanResParam setparam, Bitmap bitmap){

		TextureID id = null;
		//使用できるテクスチャがあるか確かめる。
		for(TextureParam param : mTextureMap.values()){
			if((id = getAvailableTexTure(bitmap.getHeight(), param)) == null)
				continue;
		}
		int yoffset = 0;
		int texName = 0;
		TextureParam param = null;

		if(id == null){
			//テクスチャを作成
			param = createTexture();
			mTextureMap.put(param.texName, param);
			texName = param.texName;
		}else{
			param = mTextureMap.get(id.texName);
			yoffset = id.yoffset;
			texName = id.texName;
		}

		mRenderer.addImageToTexture(texName, 0, yoffset, bitmap);
		setparam.setParam(texName, ++mIdConut, TEXTURE_WIDTH, TEXTURE_HEIGHT,  0, yoffset);

		TextureElement element = new TextureElement();
		element.id = mIdConut;
		element.startY = yoffset;
 		element.endY = yoffset + bitmap.getHeight();
		param.idMap.put(element.id, element);

		if(id == null)
			mTextureMap.put(texName, param);
	}

	/**
	 * 使用できるテクスチャの位置を取得する。
	 * ないときはnullを返す
	 */
	private TextureID getAvailableTexTure(int height, TextureParam param){

		int startY = 0;
		int endY = height;
		for(int i=0; i <= TEXTURE_HEIGHT; i++ ){

			int checkNo = checkElement(startY, endY, param.idMap);
			if(checkNo == -1){

				TextureID textureID = new TextureID();
				textureID.texName = param.texName;
				textureID.yoffset = startY;
				return textureID;
			}else if(checkNo == -2){
				//画像が入りきるテクスチャが無いのでnullを返す
				return null;
			}else{
				//topを使用中画像のbottomに設定
				startY = checkNo;
				endY = startY + height;
			}
		}
		return null;
	}

	/**
	 * テクスチャに画像の収まる場所があるか確かめる
	 * @param startY
	 * @param endY
	 * @param elements
	 * @return -1 無使用中, -2 テクスチャに収まらない, 他範囲内に存在する画像のbottom
	 */
	private int checkElement(int startY, int endY, HashMap<Integer, TextureElement> elements){

		int maxBottom = 0;
		//１つずつ確かめていく
		for(TextureElement element : elements.values()){
			if(element.isValid){

				//使用中画像のmaxBottomを求める
				if(element.endY > maxBottom) maxBottom = element.endY;

				if(element.endY == startY)
					continue;

				//登録しようとしている場所に画像があるか確かめる。
				if(element.startY <= startY && element.endY >= startY){

					if(element.startY <= endY && element.endY >= startY){
						return element.endY;
					}
				}
			}
		}

		//範囲が無使用中
		if(endY > TEXTURE_HEIGHT)
			return -2;
		else
			return -1;
	}

	/**
	 * キャンバスとリソースの関連付けを削除。
	 * テクスチャが使用されていない場合はテクスチャを削除する。
	 * @param resouceID
	 */
	public void release(int texName, int resourceID){

		mTextureMap.remove(texName);
		mRenderer.deleteTexture(texName);
	}

	/**
	 * レンダラーでテクスチャが解放されたときに呼び出します。
	 * インスタンスを初期状態に戻します。
	 */
	public void reCreate(){
		if(mTextureMap.size() != 0)
			mTextureMap = new HashMap<Integer, GLCanvas.TextureParam>();
		mIdConut = 0;
	}

	//これはpublic で見える場所に移動する
	private class TextureParam{
		public HashMap<Integer, TextureElement> idMap;	//key 一意の数値
		public int texName;	//テクスチャネーム

		public TextureParam(){
			idMap = new HashMap<Integer, GLCanvas.TextureElement>();
		}
	}

	private class TextureElement{
		public int id;
		public boolean isValid;
		public int startY;
		public int endY;
		public TextureElement(){
			isValid = true;
		}
	}

	private class TextureID{
		public int texName;
		public int yoffset;
	}
}