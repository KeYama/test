package jp.co.johospace.jsphoto.fullscreen;

public class CurveUtil {
	public static float Freguson_Coons(float t, float x0, float x1, float v0, float v1){
		float t3 = (2*x0 - 2 * x1 + v0 + v1) * t * t * t;
		float t2 = (-3*x0 + 3 * x1 -2 * v0 - v1) * t * t;
		float t1 = v0 * t;
		float t0 = x0;
		
		return t3 + t2 + t1 + t0;
	}
	
	public static float easeOutSin(float t){
		if(t > 0.5f){
			return (float)Math.sin((t-0.5)*2.0 * Math.PI / 2) * 0.5f + 0.5f;
		}else{
			return t;
		}
	}
}
