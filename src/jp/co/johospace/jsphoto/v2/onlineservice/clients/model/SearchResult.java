package jp.co.johospace.jsphoto.v2.onlineservice.clients.model;

/**
 * 検索結果
 */
public class SearchResult {

	public Media media;
	
	public Sns sns;
	
	public static class Media {
		public String terminalId;
		public Integer type;
		public Long id;
		public String mediaData;
		public String summary;
		public Tags tags;
		public String title;
		public String ｃomment;
		public String time;
		public int fav;
		public String[] searchWords;
		public String[] noSearchWords;
		public String[] relId;
		
		public static class Tags {
			public String[] gen;
			public String[] date;
			public String[] place;
			public String[] person;
		}
	}
	
	public static class Sns {
		public String userName;
		public Integer type;
		public String id;
		public String summary;
		public String[] searchWords;
		public String[] noSearchWords;
		public String postTime;
		public String[] coordinates;
		public String takenTime;
		public String[] picURL;
		public String[] relId;
	}
}
