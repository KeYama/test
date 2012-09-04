package jp.co.johospace.jsphoto.onlineservice;

/**
 * オンラインサービスアクセスに関する例外
 */
public class OnlineServiceAccessException extends Exception {

	private static final long serialVersionUID = -8947356532475682593L;

	public OnlineServiceAccessException() {
		super();
	}

	public OnlineServiceAccessException(String detailMessage,
			Throwable throwable) {
		super(detailMessage, throwable);
	}

	public OnlineServiceAccessException(String detailMessage) {
		super(detailMessage);
	}

	public OnlineServiceAccessException(Throwable throwable) {
		super(throwable);
	}

}
