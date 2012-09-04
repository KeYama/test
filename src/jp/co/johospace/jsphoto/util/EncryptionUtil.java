package jp.co.johospace.jsphoto.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;


/**
 * 暗号化・複合ユーティリティ
 */
public class EncryptionUtil {
	private EncryptionUtil() {}
	
	/** UTF-8キャラクタセット */
	private static final Charset utf8 = Charset.forName("UTF-8");
	/** デフォルトのキー生成用バイト列 */
	private static final byte[] defaultKeyBytes = {
		0x30, 0x39, 0x32, 0x72, 0x6d, 0x40, 0x73, 0x64,
		0x65, 0x75, 0x3b, 0x33, 0x6f, 0x69, 0x40, 0x41,
	};
	
	/**
	 * 文字列を暗号化して返します。
	 * @param data 暗号化する文字列
	 * @return 暗号化された文字列
	 */
	public static String encrypt(String data) {
		return encrypt(defaultKeyBytes, data);
	}
	
	/**
	 * 文字列を暗号化して返します。
	 * @param 暗号化キー
	 * @param data 暗号化する文字列
	 * @return 暗号化された文字列
	 */
	public static String encrypt(byte[] keyBytes, String data) {
		try {
			// 鍵生成
			SecretKey key = new SecretKeySpec(keyBytes, "AES");
			
			// 暗号化
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, key);
			byte[] encrypted = c.doFinal(data.getBytes(utf8.name()));
			return Base64.encodeToString(encrypted, Base64.DEFAULT);
		} catch (Exception e) {
			throw new Error("Failed to encrypt.", e);
		}
	}
	
	/**
	 * 暗号化された文字列を復号します。
	 * @param encription 暗号化された文字列
	 * @return 復号された文字列
	 */
	public static String decrypt(String encription) {
		return decrypt(defaultKeyBytes, encription);
	}
	
	/**
	 * 暗号化された文字列を復号します。
	 * @param keyBytes 暗号化キー
	 * @param encription 暗号化された文字列
	 * @return 復号された文字列
	 */
	public static String decrypt(byte[] keyBytes, String encription) {
		try {
			byte[] decoded = Base64.decode(encription, Base64.DEFAULT);
			SecretKey key = new SecretKeySpec(keyBytes, "AES");
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, key);
			byte[] decrypted = c.doFinal(decoded);
			return new String(decrypted, utf8.name());
		} catch (Exception e) {
			throw new Error("Failed to decrypt.", e);
		}
	}
	
	/**
	 * 文字列データのハッシュを返します。
	 * @param data 文字列データ
	 * @return ハッシュ
	 */
	public static String digest(String data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA512");
			md.update(data.getBytes(utf8.name()));
			return Base64.encodeToString(md.digest(), Base64.DEFAULT);
		} catch (Exception e) {
			throw new Error("Failed to digest.", e);
		}
	}
}
