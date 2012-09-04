package jp.co.johospace.jsphoto.onlineservice.jscloudia.model;


/**
 * メディアメタデータ
 */
public class SendMediaMetadata {
    /** メタデータタイプ : レコードがあらわすメタデータの種類をMIMEタイプで識別します。 vnd.jp.co.johospace/media-tag：タグ vnd.jp.co.johospace/media-favorite：お気に入り */
    public String metadataType;

    /** メタデータ */
    public String metadata;
}
