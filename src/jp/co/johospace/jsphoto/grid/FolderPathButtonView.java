package jp.co.johospace.jsphoto.grid;

import jp.co.johospace.jsphoto.R;
import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.widget.Button;
import android.widget.LinearLayout;

public class FolderPathButtonView extends Button {

	public FolderPathButtonView(Context context) {
		super(context);
		
		LinearLayout.LayoutParams params = 
				new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.WRAP_CONTENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
		
		float density = context.getResources().getDisplayMetrics().density;
		int margin = (int)(density * 5);
		params.setMargins(margin, 0, margin, 0);
		setBackgroundResource(R.drawable.button_drawable_dir);
		setLayoutParams(params);
		setEllipsize(TruncateAt.END);
		setTextSize(16);
		setText(">");
	}
	
	public void setLinkText(String text){
		setText(text);
	}

}
