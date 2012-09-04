package jp.co.johospace.jsphoto.grid;

import java.util.ArrayList;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExifView extends LinearLayout {
	
	public ExifView(Context context, String path) {
		super(context);
		
		ExifContent content = createExifContent();
		content.load(path);
		setupView(content);
	}
	
	
	public ExifView(Context context, ArrayList<String> pathList) {
		super(context);
		
		ExifContent content = createExifContent();
		content.loadExifContent(pathList);
		setupView(content);
	}
	
	private void setupView(ExifContent content){
		setOrientation(LinearLayout.VERTICAL);
		int width = (int)(100 * getContext().getResources().getDisplayMetrics().density);
		
		for(int n = 0; n < content.getNumContent(); n++){
			LinearLayout llContent = new LinearLayout(getContext());
			llContent.setOrientation(LinearLayout.HORIZONTAL);
			
			TextView tvKey = new TextView(getContext());
			tvKey.setText(content.getKey(n));
			tvKey.setLayoutParams(new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT));
			
			TextView tvValue = new TextView(getContext());
			tvValue.setText(content.getValue(n));
			tvValue.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, 
					LinearLayout.LayoutParams.WRAP_CONTENT));
			
			llContent.addView(tvKey);
			llContent.addView(tvValue);
			
			addView(llContent);
		}
	}

	
	private ExifContent createExifContent(){
		return new ExifContent8();
	}
}
