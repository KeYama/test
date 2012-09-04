package jp.co.johospace.jsphoto.v2.onlineservice.clients;

/**
 * レスポンスデータ
 */
public class RespondedContents<C> {

	public final C contents;
	public final String etag;
	
	public RespondedContents(C contents, String etag) {
		super();
		this.contents = contents;
		this.etag = etag;
	}
	
}
