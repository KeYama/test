package jp.co.johospace.jsphoto.v2.onlineservice.clients.model;

/**
 * スケジュール
 */
public class Schedule {

	/** サービスタイプ */
	public String service;
	/** 開始日時 */
	public Long dtstart;
	/** 終了日時 */
	public Long dtend;
	/** 終日 */
	public Boolean allday;
	/** タイトル */
	public String title;
	/** 場所 */
	public String location;
	/** 詳細 */
	public String detail;
}
