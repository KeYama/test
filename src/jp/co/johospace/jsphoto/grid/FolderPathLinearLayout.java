package jp.co.johospace.jsphoto.grid;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FolderPathLinearLayout extends LinearLayout {
	
	private List<File> mPathList;
	private OnPathChangedListener mListener;

	public static interface OnPathChangedListener{
		public void onPathChanged(File pathFile);
	}
	
	public FolderPathLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setOnPathChangedListener(OnPathChangedListener listener){
		mListener = listener;
	}
	
	public void refreshPath(){
		setPath(getPathFile());
		mListener.onPathChanged(getPathFile());
	}
	
	public void setPath(File pathFile){
		if(pathFile == null)throw new NullPointerException();
		
		if(!pathFile.isDirectory()){
			pathFile = pathFile.getParentFile();
		}else{
			pathFile = new File(pathFile, "");
		}
		
		mPathList = createPathList(pathFile);
		setTextView(mPathList);
	}
	
	public String getPath(){
		return getPathFile().getAbsolutePath();
	}
	
	public File getPathFile(){
		return mPathList.get(mPathList.size() - 1);
	}
	
	private List<File> createPathList(File pathFile){
		ArrayList<File> list = new ArrayList<File>();
		
		for(File path = pathFile; path != null; path = path.getParentFile()){
			if(path.getAbsolutePath().equals("/"))break;
			list.add(0, path);
		}
		
		return list;
	}
	
	private void setTextView(List<File> fileList){
		removeAllViews();
		for(int n = 0; n < fileList.size()-1; ++n){
			createAndAddTextView(fileList.get(n), true);
		}
		
		createAndAddTextView(fileList.get(fileList.size()-1), false);
	}
	
	private void createAndAddTextView(final File f, boolean link){
		
		FolderPathButtonView pathView = new FolderPathButtonView(getContext());
		
		if(link){
			pathView.setLinkText(f.getName());
			
			pathView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					setPath(f);
	
					if(mListener != null){
						mListener.onPathChanged(f);
					}
				}
			});
		}else{
//			pathView.setTextColor(Color.GREEN);
			pathView.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_dir_enable));
			pathView.setText(f.getName());
			pathView.setEnabled(false);
		}
		
		
//		addView(new FolderPathTextView(getContext()));
		TextView text = new TextView(getContext());
		
		text.setText(">");
		addView(text);
		addView(pathView);

	}
}
