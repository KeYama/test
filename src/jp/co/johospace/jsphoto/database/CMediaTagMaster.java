package jp.co.johospace.jsphoto.database;

/**
 * 列定義 タグマスター
 */
public interface CMediaTagMaster {

	/** テーブル名 */
	String $TABLE = "media_tag_master";
	
	
	/** ID **/
	String _ID = "_id";
	/** タグ名 **/
	String NAME = "name";
	/** 表示フラグ **/
	String HIDE = "hide";
	/** フィルタリングフラグ **/
	String FILTER = "filter";
}
