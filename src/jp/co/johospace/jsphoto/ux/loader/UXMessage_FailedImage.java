package jp.co.johospace.jsphoto.ux.loader;

import jp.co.johospace.jsphoto.ux.renderer.UXImageResource;
import jp.co.johospace.jsphoto.ux.thread.UXMessage;

/**
 * 読み込み失敗時に送られるメッセージ
 */
public class UXMessage_FailedImage extends UXMessage {

	public Object info;
	public UXImageResource resource;

	public UXMessage_FailedImage(Object argInfo, UXImageResource res){
		info = argInfo;
		resource = res;
	}
}
