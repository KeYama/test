package jp.co.johospace.jsphoto.view;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.util.SizeConv;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressPopupWindow extends PopupWindow {

	private TextView mTxtMessage;
	private ProgressBar mProgress;
	
	private CharSequence mMessage;

	public ProgressPopupWindow(Context context) {
		super(context);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.popup_progress, null);
		
		mTxtMessage = (TextView) view.findViewById(R.id.txt_message);
		mProgress = (ProgressBar) view.findViewById(R.id.progress);
		setContentView(view);
		
		SizeConv sc = SizeConv.getInstance(context);
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		final float width = wm.getDefaultDisplay().getWidth();
		
		setWidth((int) sc.getSize(width * 0.6f));
		setHeight((int) SizeConv.getInstance(context).getSize(80f));
		setBackgroundDrawable(null);
	}
	
	public void setMessage(CharSequence message) {
		this.mMessage = message;
		
		if (mTxtMessage != null)
			mTxtMessage.setText(mMessage);
	}
	
	public void showProgress() {
		mProgress.setVisibility(View.VISIBLE);
		mTxtMessage.setGravity(Gravity.LEFT);
	}
	
	public void hideProgress() {
		mTxtMessage.setGravity(Gravity.CENTER_HORIZONTAL);
		mProgress.setVisibility(View.GONE);
	}
}
