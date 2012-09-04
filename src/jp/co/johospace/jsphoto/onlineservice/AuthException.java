package jp.co.johospace.jsphoto.onlineservice;

/**
 * 認証・認可に関連する例外
 */
public class AuthException extends OnlineServiceAccessException {

	private static final long serialVersionUID = 509060860467833806L;

	public AuthException() {
		super();
	}

	public AuthException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public AuthException(String detailMessage) {
		super(detailMessage);
	}

	public AuthException(Throwable throwable) {
		super(throwable);
	}

}
