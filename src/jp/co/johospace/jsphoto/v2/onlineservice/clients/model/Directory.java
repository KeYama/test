package jp.co.johospace.jsphoto.v2.onlineservice.clients.model;

import java.util.List;

/**
 * メディアディレクトリ
 */
public class Directory {

	/** ディレクトリID */
	public String id;
	/** ディレクトリ名 */
	public String name;
	/** 更新日時 */
	public Long updatedTimestamp;
	/** バージョン */
	public Long version;
	/** メディアバージョン */
	public Long mediaVersion;
	/** メディア */
	public List<Media> media;
	/** メディア件数 */
	public int mediaCount;
	/** 削除データ */
	public Boolean deleted;
}
