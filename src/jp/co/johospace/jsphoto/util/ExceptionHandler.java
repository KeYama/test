package jp.co.johospace.jsphoto.util;

public interface ExceptionHandler {

	public enum NetErrors {
		Connection,
		ConnectionClosed,
		TimeoutConnection,
		TimeoutResponse,
		Unknown,
	}
	
	/**
	 * エラーをハンドリングします
	 * @param t 例外
	 * @param assertTo エラーをUI上に報告するかどうか
	 * @return 例外を処理した場合はtrue、それ以外はfalse
	 */
	boolean handleException(Throwable t, boolean assertTo);
	
	/**
	 * ネットワーク例外を報告します
	 * @param error エラーコード
	 * @param t 例外
	 * @param assertTo エラーをUI上に報告するかどうか
	 * @return 例外を報告した場合はtrue、それ以外はfalse
	 */
	boolean assertException(NetErrors error, Throwable t, boolean assertTo);
}
