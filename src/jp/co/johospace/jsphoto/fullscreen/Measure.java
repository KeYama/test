package jp.co.johospace.jsphoto.fullscreen;

/**
 * 各種単位系変換クラス。
 * 
 */
public class Measure {
	float mScreenWidth;
	float mScreenHeight;
	public Measure(){
	}
	
	public float toWorldX(float virtualX){
		return mScreenWidth * virtualX;
	}
	
	public float toVirtualX(float screenX){
		return screenX / mScreenWidth;
	}
	
	public float getScreenWidth(){
		return mScreenWidth;
	}
	
	public float getScreenHeight(){
		return mScreenHeight;
	}
	
	public void setScreenSize(float width, float height){
		mScreenWidth = width;
		mScreenHeight = height;
	}
}
