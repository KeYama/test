package jp.co.johospace.jsphoto.v2.onlineservice.clients.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 関連メディア
 */
public class RelatedMedia {

	/** 関連メディア */
	public List<Media> medias = new ArrayList<Media>();
	/** 関連スケジュール */
	public List<Schedule> schedules = new ArrayList<Schedule>();
	/** 関連SNSコンテンツ */
	public SnsContent snsContents;
	
	public boolean isEmpty() {
		return (medias == null || medias.isEmpty())
				&& (schedules == null || schedules.isEmpty())
				&& snsContents == null;
	}
}
