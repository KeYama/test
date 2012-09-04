package jp.co.johospace.jsphoto.grid;

import java.util.List;

import jp.co.johospace.jsphoto.R;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FolderPathListViewAdapter extends ArrayAdapter<String> {
	private int mRes;

	public FolderPathListViewAdapter(Context context, int textViewResourceId,
			List<String> objects) {
		super(context, textViewResourceId, objects);
		mRes = textViewResourceId;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null){
			LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(mRes, null);
		}
		
		TextView view = (TextView)convertView.findViewById(R.id.tvFolder);
		view.setText(getItem(position));
		view.setTextColor(Color.rgb(32, 32, 32));
		
		return convertView;
	}
	
	

}
