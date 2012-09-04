package jp.co.johospace.jsphoto.grid;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.co.johospace.jsphoto.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FolderPathListView extends ListView{
	List<File> mFiles = new ArrayList<File>();

	public FolderPathListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public File getFile(int pos){
		return mFiles.get(pos);
	}
	
	public boolean setPath(File pathFile){
		if(!pathFile.isDirectory()){
			return false;
		}
		
		File[] files = pathFile.listFiles();
		if(files == null){
			return false;
		}
		
		List<String> list = new ArrayList<String>();
		mFiles.clear();
		for(File f: files){
			if(f.isDirectory()){
//				list.add(f.getName());
				mFiles.add(f);
			}
		}

		Collections.sort(mFiles, new NameAscender());
		
		for (File directory : mFiles) {
			list.add(directory.getName());
		}
		
		ArrayAdapter<String> adapter = 
				new FolderPathListViewAdapter(
						getContext(), 
						R.layout.folder_item, 
						list
					);
		setAdapter(adapter);
		
		return true;
	}
	
	/**
	 * 名前昇順
	 */
	public static class NameAscender implements Comparator<File>{

		@Override
		public int compare(File lhs, File rhs) {
			return lhs.getName().compareToIgnoreCase(rhs.getName());
		}
	}
}
