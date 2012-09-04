package jp.co.johospace.jsphoto.onlineservice.jscloudia.model;


/**
 * 処理されたメディア
 */
public class ReceiveProcessedMedia {

    /** サービスタイプ : メディアデータの実体をストアしているオンラインサービスを識別する。 */
    public String serviceType;

    /** サービスアカウント : メディアをストアするオンラインサービスのアカウント。 */
    public String serviceAccount;

    /** メディアID : サービスタイプ内でメディアを一意に識別する。 */
    public String mediaId;
}
