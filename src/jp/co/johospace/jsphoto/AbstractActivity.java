package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.util.DefaultExceptionHandler;
import jp.co.johospace.jsphoto.util.ExceptionHandler;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * アクティビティの基底です。
 */
public class AbstractActivity extends FragmentActivity implements ExceptionHandler {

	private ExceptionHandler mExceptionHandler;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		mExceptionHandler = new DefaultExceptionHandler(this);
	}

	@Override
	public boolean handleException(Throwable t, boolean assertTo) {
		return mExceptionHandler.handleException(t, assertTo);
	}

	@Override
	public boolean assertException(NetErrors error, Throwable t,
			boolean assertTo) {
		return mExceptionHandler.assertException(error, t, assertTo);
	}

}
