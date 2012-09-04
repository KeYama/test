package jp.co.johospace.jsphoto.v2.onlineservice.clients;

/**
 * 外部サービスタイプ
 */
public interface ServiceType {

	/** ジョルテ */
	String JORTE = "net.jorte";
	
	/** PicasaWebアルバム */
	String PICASA_WEB = "com.google.picasaweb";
	
	/** ツイッター */
	String TWITTER = "com.twitter";
	
	/** フェイスブック */
	String FACEBOOK = "com.facebook";
	
	/** Googleカレンダー */
	String GOOGLE_CALENDAR = "com.google.calendar";
	
	/** Googleプロフィール */
	String GOOGLE = "com.google.profile";
	
	/** メディア管理ローカル */
	String JORLLE_LOCAL = "jp.co.johospace.jsphoto/local";
}
