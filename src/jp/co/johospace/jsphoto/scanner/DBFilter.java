package jp.co.johospace.jsphoto.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DBFilter implements JorlleMediaFilter {
	private SQLiteDatabase mDb;
	private List<String> mTags;
	private boolean mFavoriteFlag;
	
	public DBFilter(){
		mDb = OpenHelper.external.getDatabase();
		mTags = new ArrayList<String>();
	}
	
	public void addTag(String tag){
		mTags.add(tag);
	}
	
	public void filterFavorite(boolean flag){
		mFavoriteFlag = flag;
	}

	@Override
	public boolean filter(File file) {
		Cursor c = mDb.query(
				CMediaMetadata.$TABLE,
				new String[]{CMediaMetadata.METADATA, CMediaMetadata.METADATA_TYPE}, 
				CMediaMetadata.DIRPATH + "= ? and " +
				CMediaMetadata.NAME + "= ?", 
				new String[]{file.getParent(), file.getName()},
				null, null, null);
		
		try{
			if(c.moveToFirst()){
	
				boolean containsTag = false;
				boolean favorite = false;
				
				do{
					String type = c.getString(c.getColumnIndex(CMediaMetadata.METADATA_TYPE));
					String value = c.getString(c.getColumnIndex(CMediaMetadata.METADATA));
	
					if(type.equals(ApplicationDefine.MIME_TAG)){
						
						if(mTags.contains(value)){
							containsTag = true;
						}
					} else if(type.equals(ApplicationDefine.MIME_FAVORITE)){
						
						if(mFavoriteFlag && "1".equals(value)){
							favorite = true;
						}
					}
					
				}while(c.moveToNext());
				
				// タグ指定ではないならば、タグフラグはtrueに
				if (mTags.size() == 0) {
					containsTag = true;
				}
				
				// お気に入り指定ではないならば、お気に入りフラグはtrueに
				if (!mFavoriteFlag) {
					favorite = true;
				}
				
				return containsTag && favorite;
			}else{
				return false;
			}
		}finally{
			c.close();
		}
	}
}
