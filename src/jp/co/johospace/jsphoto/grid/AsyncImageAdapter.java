package jp.co.johospace.jsphoto.grid;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jp.co.johospace.jsphoto.cache.CacheLoader;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

public class AsyncImageAdapter extends BaseAdapter {
	
	protected Context mContext;
	private List<String> mPathList = new ArrayList<String>();
	private List<Boolean> mSelected = new ArrayList<Boolean>();
	private String mModifiedImage;
	
	private boolean mMultiMode = false;
	private boolean mAutoRotation = false;
	
	private final CacheLoader mLoader;
	public AsyncImageAdapter(Context context){
		mContext = context;
		mLoader = createLoader();
		
		mAutoRotation = PreferenceUtil.getBooleanPreferenceValue(context, ApplicationDefine.PREF_IMAGE_AUTO_ROTATION);
	}
	
	public void addAll(Collection<String> pathCollection){
		mPathList.addAll(pathCollection);
		for(int n = 0; n < pathCollection.size(); n++){
			mSelected.add(false);
		}
		notifyDataSetChanged();
	}
	
	public void setModifiedImagePath(String path){
		mModifiedImage = path;
	}
	
	public void selectItem(int loc){
		mSelected.set(loc, true);
		//notifyDataSetChanged();
	}
	
	public void unselectItem(int loc){
		mSelected.set(loc, false);
		//notifyDataSetChanged();
	}
	
	public boolean toggleItem(int loc){
		if(mSelected.get(loc)){
			unselectItem(loc);
			return false;
		}else{
			selectItem(loc);
			return true;
		}
	}
	
	public void addItem(String path){
		mPathList.add(path);
		mSelected.add(false);
		notifyDataSetChanged();
	}
	
	public void clear(){
		mPathList.clear();
		mSelected.clear();
		
		//設定が変更されているかもしれないので読みに行く
		mAutoRotation = PreferenceUtil.getBooleanPreferenceValue(mContext, ApplicationDefine.PREF_IMAGE_AUTO_ROTATION);
		notifyDataSetChanged();
	}
	
	public void setMultiMode(boolean flag){
		if(mMultiMode != flag){
			mMultiMode = flag;
			if(!flag){
				for(int n = 0; n < mSelected.size(); ++n){
					mSelected.set(n, false);
				}
			}
			notifyDataSetChanged();
		}
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		AsyncImageView view = null;
		String path = (String)getItem(position);
		
		if(convertView == null){
			view = new AsyncImageView(mContext);
			view.setLayoutParams(new GridView.LayoutParams(
					GridView.LayoutParams.MATCH_PARENT,
					GridView.LayoutParams.MATCH_PARENT
					));
			view.setLoader(mLoader);
		}else{
			view = (AsyncImageView)convertView;
			if(path.equals(mModifiedImage)){
				mModifiedImage = null;
				view.loadImage(null);
			}
		}
		
		loadImage(view, path);
		view.setCheckMode(mMultiMode);
		view.setGridMode(true);
		view.setAutoRotation(mAutoRotation);
		view.setCheck(mSelected.get(position) && mMultiMode);
		view.setFavorite(FavoriteUtil.isFavorite(new File(path)));
		
		return view;
	}
	
	protected void loadImage(AsyncImageView view, String path){
		view.loadImage(path);
	}
	
	public void dispose() {
		if (mLoader != null) {
			mLoader.dispose();
		}
	}

	protected CacheLoader createLoader() {
		CacheLoader loader = new CacheLoader();
		return loader;
	}
	
	public ArrayList<String> getSelectedList(){
		ArrayList<String> list = new ArrayList<String>();
		for(int n = 0; n < mPathList.size(); ++n){
			if(mSelected.get(n)){
				list.add(mPathList.get(n));
			}
		}
		
		return list;
	}
	
	public ArrayList<String> getArrayList(){
		return (ArrayList<String>)mPathList;
	}
	
	public void toggleAllSelect(){
		boolean allChecked = true;
		for(boolean checked: mSelected){
			allChecked = allChecked && checked;
		}
		if(allChecked){
			for(int n = 0; n < mSelected.size(); n++){
				mSelected.set(n, false);
			}
		}else{
			for(int n = 0; n < mSelected.size(); n++){
				mSelected.set(n, true);
			}
		}
		notifyDataSetChanged();
	}
	
	public boolean isSelectedAnyItem(){
		for(boolean selected: mSelected){
			if(selected)return true;
		}
		return false;
	}

	@Override
	public int getCount() {
		return mPathList.size();
	}

	@Override
	public Object getItem(int position) {
		return mPathList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
