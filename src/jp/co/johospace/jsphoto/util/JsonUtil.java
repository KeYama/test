package jp.co.johospace.jsphoto.util;

import java.lang.reflect.Type;

import com.google.gson.Gson;

/**
 * JSONユーティリティ
 */
public class JsonUtil {
	private JsonUtil() {}
	
	/** スレッドローカルGSON */
	private static final ThreadLocal<Gson> sGson = new ThreadLocal<Gson>() {
		@Override
		protected Gson initialValue() {
			return new Gson();
		}
	};
	
	/**
	 * スレッドセーフなGSONを返します。
	 * @return GSON
	 */
	public static Gson gson() {
		return sGson.get();
	}
	
	/**
	 * デシリアライズします。
	 * @param json JSON
	 * @param clazz ターゲットの型
	 * @return デシリアライズされたインスタンス
	 */
	public static <T> T fromJson(String json, Class<T> clazz) {
		return gson().fromJson(json, clazz);
	}
	
	/**
	 * デシリアライズします。
	 * @param json JSON
	 * @param type ターゲットの型
	 * @return デシリアライズされたインスタンス
	 */
	public static <T> T fromJson(String json, Type type) {
		return gson().fromJson(json, type);
	}
	
	/**
	 * シリアライズします。
	 * @param o ターゲット
	 * @return JSONシリアライズされた結果
	 */
	public static String toJson(Object o) {
		return gson().toJson(o);
	}
	
	/**
	 * シリアライズします。
	 * @param o ターゲット
	 * @param type ターゲットの型
	 * @return JSONシリアライズされた結果
	 */
	public static String toJson(Object o, Type type) {
		return gson().toJson(o, type);
	}
}
