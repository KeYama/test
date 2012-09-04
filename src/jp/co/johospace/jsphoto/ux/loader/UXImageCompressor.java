package jp.co.johospace.jsphoto.ux.loader;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * イメージ圧縮クラス
 */
public class UXImageCompressor {

	/**
	 * 圧縮、加工を実行する
	 *
	 * @param inInfo 圧縮元画像。所有権はUXImageCompressorが持つので、不要ならば解放する事。
	 * @param outInfo 出力情報
	 */
	public void compress(UXImageInfo inInfo, int widthHint, UXImageInfo outInfo){
		//デフォルト実装
		outInfo.compressedImage = inInfo.compressedImage;
		outInfo.bitmap = inInfo.bitmap;
		outInfo.orientation = inInfo.orientation;
		if(inInfo.compressedImage != null){
			return;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		inInfo.bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

		byte[] compressed = out.toByteArray();
		try {
			out.close();
		} catch (IOException e) {
		}

		outInfo.compressedImage = compressed;
		outInfo.bitmap = inInfo.bitmap;
	}
}
