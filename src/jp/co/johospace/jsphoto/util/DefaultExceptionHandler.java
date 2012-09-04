package jp.co.johospace.jsphoto.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLProtocolException;

import org.apache.http.conn.ConnectTimeoutException;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.google.api.client.http.HttpResponseException;

public class DefaultExceptionHandler implements ExceptionHandler {

	private static final int HANDLE_TOAST = 1;

	private class MyHandler extends Handler {
		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLE_TOAST:
				String text = (String) msg.obj;
				Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
				break;
			}
		}
	}

	private Context mContext;
	private Handler mHandler;

	public DefaultExceptionHandler(Context activity) {
		mContext = activity;
		mHandler = new MyHandler(mContext.getMainLooper());
	}

	@Override
	public boolean handleException(Throwable t, boolean assertTo) {
//		t.printStackTrace();		/*$debug$*/
		if (t instanceof IOException) {
			return assertException((IOException) t, assertTo);
		} else {
			Throwable cause = t.getCause();
			if (cause != null && cause instanceof IOException) {
				return assertException((IOException) cause, assertTo);
			}
		}

		return false;
	}

	private boolean assertException(IOException e, boolean assertTo) {
		if (e instanceof SocketTimeoutException) {
			return assertException(NetErrors.TimeoutResponse, e, assertTo);
		} else if (e instanceof ConnectTimeoutException) {
			return assertException(NetErrors.TimeoutConnection, e, assertTo);
		} else if (e instanceof ConnectException) {
			return assertException(NetErrors.Connection, e, assertTo);
		} else if (e instanceof SocketException) {
			return assertException(NetErrors.Unknown, e, assertTo);
		} else if (e instanceof HttpResponseException) {
			final int status = ((HttpResponseException) e).getResponse().getStatusCode();
			if (status < 0) {
				return assertException(NetErrors.ConnectionClosed, e, assertTo);
			} else {
				return assertException(NetErrors.Unknown, e, assertTo);
			}
		} else if (e instanceof SSLProtocolException) {
			return assertException(NetErrors.Unknown, e, assertTo);
		} else {
			e.printStackTrace(); /* $debug$ */
		}
		return false;
	}

	@Override
	public boolean assertException(NetErrors error, Throwable t,
			boolean assertTo) {
		if (!assertTo) {
//			t.printStackTrace();		/*$debug$*/
			return false;
		}

		Message msg = null;
		switch (error) {
		case ConnectionClosed:
			msg = mHandler.obtainMessage(HANDLE_TOAST, Toast.LENGTH_LONG, 0, mContext.getString(jp.co.johospace.jsphoto.R.string.message_error_net_connclose));
			break;
		case Connection:
			// fall through
		case TimeoutConnection:
			msg = mHandler.obtainMessage(HANDLE_TOAST, Toast.LENGTH_LONG, 0, mContext.getString(jp.co.johospace.jsphoto.R.string.message_error_net_timeout_conn));
			break;
		case TimeoutResponse:
			msg = mHandler.obtainMessage(HANDLE_TOAST, Toast.LENGTH_LONG, 0, mContext.getString(jp.co.johospace.jsphoto.R.string.message_error_net_timeout_sock));
			break;
		default:
			msg = mHandler.obtainMessage(HANDLE_TOAST, Toast.LENGTH_LONG, 0, mContext.getString(jp.co.johospace.jsphoto.R.string.message_error_net_unknown));
			break;
		}
		if (msg == null) {
//			t.printStackTrace();		/*$debug$*/
			return false;
		}

		mHandler.sendMessage(msg);
		return true;
	}

}
