package jp.co.johospace.jsphoto.onlineservice.jscloudia.model;

import java.util.List;

/**
 * メディアインデックス
 */
public class SendMediaIndex {
    /** サービスタイプ : メディアデータの実体をストアしているオンラインサービスを識別する。 */
    public String serviceType;

    /** サービスアカウント : メディアをストアするオンラインサービスのアカウント。 */
    public String serviceAccount;

    /** メディアID : サービスタイプ内でメディアを一意に識別する。 */
    public String mediaId;

    /** ディレクトリID */
    public String directoryId;

    /** メディアURI : メディアを一意に指すURI。 */
    public String mediaUri;

    /** ファイル名 */
    public String fileName;

	/** 制作日 */
	public Long productionDate;

    /** mediaMetadataList関連プロパティ */
    public List<SendMediaMetadata> mediaMetadataList;
    
    /** 操作 */
    public String operation;
}
