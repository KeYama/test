package jp.co.johospace.jsphoto.folder;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.johospace.jsphoto.LocalFolderActivity.FolderEntry;
import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.cache.CacheLoader;
import jp.co.johospace.jsphoto.cache.LocalCachedThumbnailLoader;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.service.MediaSyncManagerV2.SyncSetting;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import jp.co.johospace.jsphoto.ux.view.UXViewLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * フォルダ一覧画面（リスト形式）のアダプタです
 */
public class AsyncListAdapter extends BaseAdapter {

	protected Context mContext;

	/** 描画領域　長さ */
	private Integer mParentLength;

	/** 一行の幅 */
	private Integer mRowWidth;

	/** 一行の高さ */
	private Integer mRowHeight;

	/** フォルダ情報のリスト */
	private final List<FolderEntry> mListEntry;

	/** ソート条件 */
	private Comparator<FolderEntry> mComparator;

	/** ウィンドウマネージャ */
	private WindowManager wm;

	/** ディスプレイ */
	private Display display;

	/** 描画領域　レイアウト */
	LinearLayout.LayoutParams mLayoutParams;

	/** DCIMフォルダのパス **/
	private String mDcimPath;
	
	private final Map<String, String> mSyncPaths;

	public AsyncListAdapter(Context context, List<FolderEntry> entries){
		this(context, entries, true);
	}

	public AsyncListAdapter(Context context, List<FolderEntry> entries, boolean showFolderPath){
		mContext = context;
//		mShowFolderPath = showFolderPath;
//		mLoader = createLoader();
		mListEntry = entries;
		mSyncPaths = new HashMap<String, String>();
		Map<String, Map<String, SyncSetting>> settings =
				MediaSyncManagerV2.loadSyncSettings(mContext.getApplicationContext());
		for (String service : settings.keySet()) {
			Map<String, SyncSetting> accounts = settings.get(service);
			for (String account : accounts.keySet()) {
				SyncSetting setting = accounts.get(account);
				mSyncPaths.put(setting.localDir, service);
			}
		}
	}

	/**
	 * フォルダ情報一覧を追加
	 * @param pathCollection	フォルダ情報のコレクション
	 */
	public void addAll(Collection<FolderEntry> pathCollection){
		mListEntry.addAll(pathCollection);
		notifyDataSetChanged();
	}

	/**
	 * フォルダ情報一覧を、指定した位置に追加
	 *
	 * @param pathCollection	フォルダ情報のコレクション
	 * @param position	追加位置
	 */
	public void addAllPosition(Collection<FolderEntry> pathCollection, int position) {
		mListEntry.addAll(position, pathCollection);
		notifyDataSetChanged();
	}

	/**
	 * フォルダ情報を追加
	 * @param fe	フォルダ情報
	 */
	public void addItem(FolderEntry fe){
		mListEntry.add(fe);
//		sort();
//		notifyDataSetChanged();
	}

	/**
	 * フォルダ情報リストをソート
	 * @param comparator	ソート条件
	 */
	public void sort(Comparator<FolderEntry> comparator) {
		if (comparator != null) {
			setComparator(comparator);
			Collections.sort(mListEntry, comparator);
		}

		notifyDataSetChanged();
	}

	/**
	 * リストソート処理
	 */
	public void sort() {
		if (mComparator == null) return;

		Collections.sort(mListEntry, mComparator);
	}

	/**
	 * フォルダ情報をすべて消去
	 */
	public void clearItem() {
		mListEntry.clear();
	}

//	/**
//	 * フォルダ情報リストをセット
//	 *
//	 * @param fe　フォルダ情報リスト
//	 */
//	public void setEntryList(List<FolderEntry> fe) {
//		mListEntry = fe;
//		notifyDataSetChanged();
//	}

	/**
	 * ソート条件をセット
	 *
	 * @param comparator ソート条件
	 */
	public void setComparator(Comparator<FolderEntry> comparator) {
		this.mComparator = comparator;
	}

	/**
	 * フォルダ情報リストを取得
	 *
	 * @return　フォルダ情報リスト
	 */
	public List<FolderEntry> getEntryList() {
		return mListEntry;
	}

	/**
	 * ソート条件を取得
	 *
	 * @return ソート条件
	 */
	public Comparator<FolderEntry> getComparator() {
		return mComparator;
	}

	/**
	 * 一行のレイアウトクラス
	 */
	public static class ViewHolder {

		// 画像ビュー
		public UXAsyncImageView imageView1;
		public UXAsyncImageView imageView2;
		public UXAsyncImageView imageView3;
		public UXAsyncImageView imageView4;
		public UXAsyncImageView imageView5;
		public UXAsyncImageView imageView6;
		public UXAsyncImageView imageView7;
		public UXAsyncImageView imageView8;
		public UXAsyncImageView imageView9;
		public UXAsyncImageView imageView10;

		// 日付
//		TextView folderDate;

		// フォルダ名
		public TextView folderName;

		// メディアファイル数
		public TextView mediaCount;

		// 同期フォルダアイコン
		public ImageView syncImage = null;

		// メディアファイル数　アイコン
		public ImageView countImage;

		// 選択時ビュー
		public View select;

		// フォルダパス
//		TextView folderPath;
		
		// レイアウト
		public LinearLayout layout;
		
		// 一行のレイアウト
		public FrameLayout frmFolder;
		
		public FolderEntry fe;
		
		public Button whatIsSync;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {


		//TODO 画像一覧のソート処理を適応予定
		View view = convertView;

		// その他レイアウト
		//TextView folderName;
		//TextView mediaCount;

		//ImageView syncImage;
		//ImageView countImage;

		LinearLayout layout = null;

		//View select;

		// フォルダ情報取得
		FolderEntry fe = (FolderEntry)getItem(position);
		ViewHolder holder = null;

		// レイアウト設定
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.folder_list_item, null);

			layout = (LinearLayout)view.findViewById(R.id.lytImageContainer);

			// Exif情報取得フラグをセットし、ローダーを作成
			LocalCachedThumbnailLoader thumbnailLoader = new LocalCachedThumbnailLoader();
			boolean isReflectionExif = PreferenceUtil.getBooleanPreferenceValue(mContext, ApplicationDefine.PREF_IMAGE_AUTO_ROTATION, true);
			thumbnailLoader.setReflectionExif(isReflectionExif);

			// 画像表示ローダ
			UXViewLoader loader = new UXViewLoader(thumbnailLoader);

			// 画像レイアウト設定
			UXAsyncImageView imageView1 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView2 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView3 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView4 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView5 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView6 = new UXAsyncImageView(mContext);

			imageView1.setBorder(true, mContext.getResources().getColor(R.color.deepgray), 1.5f);
			imageView2.setBorder(true, mContext.getResources().getColor(R.color.deepgray), 1.5f);
			imageView3.setBorder(true, mContext.getResources().getColor(R.color.deepgray), 1.5f);
			imageView4.setBorder(true, mContext.getResources().getColor(R.color.deepgray), 1.5f);
			imageView5.setBorder(true, mContext.getResources().getColor(R.color.deepgray), 1.5f);
			imageView6.setBorder(true, mContext.getResources().getColor(R.color.deepgray), 1.5f);

			imageView1.setAutoRotation(true);
			imageView2.setAutoRotation(true);
			imageView3.setAutoRotation(true);
			imageView4.setAutoRotation(true);
			imageView5.setAutoRotation(true);
			imageView6.setAutoRotation(true);

			imageView1.setViewLoader(loader);
			imageView2.setViewLoader(loader);
			imageView3.setViewLoader(loader);
			imageView4.setViewLoader(loader);
			imageView5.setViewLoader(loader);
			imageView6.setViewLoader(loader);

			// 画像のサイズ指定
			if (mParentLength == null) {
				setDisplayLength();
			}

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mParentLength, mParentLength);
			imageView1.setLayoutParams(params);
			imageView2.setLayoutParams(params);
			imageView3.setLayoutParams(params);
			imageView4.setLayoutParams(params);
			imageView5.setLayoutParams(params);
			imageView6.setLayoutParams(params);

			layout.addView(imageView1);
			layout.addView(imageView2);
			layout.addView(imageView3);
			layout.addView(imageView4);
			layout.addView(imageView5);
			layout.addView(imageView6);

			int orientation = view.getResources().getConfiguration().orientation;
			// 縦横判断（true:横 else:縦）
			if(Configuration.ORIENTATION_LANDSCAPE == orientation){
				UXAsyncImageView imageView7 = new UXAsyncImageView(mContext);
				UXAsyncImageView imageView8 = new UXAsyncImageView(mContext);
				UXAsyncImageView imageView9 = new UXAsyncImageView(mContext);
				UXAsyncImageView imageView10 = new UXAsyncImageView(mContext);

				imageView7.setBorder(true, mContext.getResources().getColor(R.color.black), 1.5f);
				imageView8.setBorder(true, mContext.getResources().getColor(R.color.black), 1.5f);
				imageView9.setBorder(true, mContext.getResources().getColor(R.color.black), 1.5f);
				imageView10.setBorder(true, mContext.getResources().getColor(R.color.black), 1.5f);

				imageView7.setAutoRotation(true);
				imageView8.setAutoRotation(true);
				imageView9.setAutoRotation(true);
				imageView10.setAutoRotation(true);
				
				imageView7.setViewLoader(loader);
				imageView8.setViewLoader(loader);
				imageView9.setViewLoader(loader);
				imageView10.setViewLoader(loader);

				imageView7.setLayoutParams(params);
				imageView8.setLayoutParams(params);
				imageView9.setLayoutParams(params);
				imageView10.setLayoutParams(params);

				layout.addView(imageView7);
				layout.addView(imageView8);
				layout.addView(imageView9);
				layout.addView(imageView10);
			} else {

			}
			
			holder = new ViewHolder();
			holder.layout = layout;
			// その他レイアウト
			holder.frmFolder = (FrameLayout) view.findViewById(R.id.frmFolder);
			holder.folderName = (TextView)view.findViewById(R.id.txtFolderName);
			holder.mediaCount = (TextView)view.findViewById(R.id.txtMediaCount);
			holder.syncImage = (ImageView) view.findViewById(R.id.imgFolder);
			holder.countImage = (ImageView)view.findViewById(R.id.imgMediaCount);
			holder.whatIsSync = (Button) view.findViewById(R.id.btn_what_is_sync);

			holder.select = (View)view.findViewById(R.id.viewOver);

			if (mRowHeight == null || mRowWidth == null) {
				setRowLength();
			}

			if(holder.select!=null) {
				LayoutParams param = holder.select.getLayoutParams();
				param.height = mRowHeight;
				param.width = mRowWidth;
				holder.select.setLayoutParams(param);
			}
			holder.fe = null;

			//view.setTag(layout);
			view.setTag(holder);
		} else {
//			layout = (LinearLayout)view.getTag();
			holder = (ViewHolder)view.getTag();
			layout = holder.layout;
			/*
			// その他レイアウト
			folderName = (TextView)view.findViewById(R.id.txtFolderName);
			mediaCount = (TextView)view.findViewById(R.id.txtMediaCount);
			syncImage = (ImageView) view.findViewById(R.id.imgFolder);
			countImage = (ImageView)view.findViewById(R.id.imgMediaCount);

			select = (View)view.findViewById(R.id.viewOver);
			*/
		}

		
		if (fe != null) { // && holder.fe != fe) {
			/*
			// clear
			for( int n = 0; n < layout.getChildCount(); ++n){
				//TODO ImageViewのサイズ × 2
				((UXAsyncImageView)layout.getChildAt(n)).loadImage(null, mParentLength * 2);
			}

			// 画像情報取得
			if (fe.getImages().size() > 0) {
				// 横並びにフォルダ内の画像を表示
				for (int i = 0; i < fe.getImages().size() && i < layout.getChildCount(); i++) {
					File file = new File(fe.getImages().get(i));
					//TODO ImageViewのサイズ × 2
					((UXAsyncImageView)layout.getChildAt(i)).loadImage(file.getAbsolutePath(), mParentLength * 2);
				}
			}
			*/
			
			final int C = layout.getChildCount();
			int sz = fe.getImages().size();
			for( int i = 0; i < C; ++i){
				UXAsyncImageView image = (UXAsyncImageView) layout.getChildAt(i);
				if (i < sz) {
					File file = new File(fe.getImages().get(i));
					//TODO ImageViewのサイズ × 2
					image.loadImage(file.getAbsolutePath(), mParentLength * 2);
				} else {
					image.loadImage(null, 0);
				}
			}			

			// フォルダ名とフォルダ内画像件数を表示
			String folder = fe.getName();

			// フォルダ名
			holder.folderName.setText(folder);

			// 同期フォルダアイコンの表示
			if(holder.syncImage!=null) {
				String syncService = mSyncPaths.get(fe.getPath());
				if (syncService != null) {
					holder.syncImage.setImageResource(R.drawable.sync_folder);
					holder.whatIsSync.setVisibility(View.VISIBLE);
					holder.whatIsSync.setTag(new Object[] {syncService, holder.syncImage});
					holder.whatIsSync.setOnClickListener(mOnClickWhatIsSync);
				} else {
					holder.syncImage.setImageResource(R.drawable.folder_black_local);
					holder.whatIsSync.setVisibility(View.GONE);
					holder.whatIsSync.setTag(null);
					holder.whatIsSync.setOnClickListener(null);
				}
			}

			// メディアファイル数
			int count = fe.getMediaCount();

			// ファイル数アイコン　セット
			if (count >= 1000) {
				holder.mediaCount.setVisibility(View.GONE);
				holder.countImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.file_over_black));
			} else {
				holder.mediaCount.setVisibility(View.VISIBLE);
				holder.mediaCount.setText(String.valueOf(count));
				holder.countImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.file_black));
			}
			
			//DCIM下のフォルダは色を変える
			if(mDcimPath == null)
				mDcimPath= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();

			if(fe.getPath().indexOf(mDcimPath) >= 0){
				view.setBackgroundColor(Color.rgb(192, 192, 192));
			}else{
				view.setBackgroundColor(mContext.getResources().getColor(R.color.lightgray));
			}
			
			holder.fe = fe;
		}
		
		//Log.d("AsyncListAdapter",fe.getPath());
		
		LinearLayout.LayoutParams rowParams = (LinearLayout.LayoutParams) holder.frmFolder.getLayoutParams();
		
		if (position == 0) {
			rowParams.setMargins(5, 12, 5, 4);
		} else {
			rowParams.setMargins(5, 4, 5, 4);
		}
		
		holder.frmFolder.setLayoutParams(rowParams);
		
		return view;
	}
	
	private PopupWindow mPopupWhatIsSync;
	
	private final View.OnClickListener mOnClickWhatIsSync = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Object[] tags = (Object[]) v.getTag();
			if (v.getTag() != null) {
				String syncService = (String) tags[0];
				View anchor = (View) tags[1];
			    mPopupWhatIsSync = new PopupWindow(anchor);
			    
			    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    View popupView = inflater.inflate(R.layout.popup_plain_message, null);
			    popupView.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						mPopupWhatIsSync.dismiss();
						return true;
					}
				});
			    
			    mPopupWhatIsSync.setContentView(popupView);
			    mPopupWhatIsSync.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
			    mPopupWhatIsSync.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
			    mPopupWhatIsSync.setBackgroundDrawable(new BitmapDrawable());
			    mPopupWhatIsSync.setOutsideTouchable(true);
			    
			    TextView message = (TextView) popupView.findViewById(R.id.txt_message);
			    message.setText(mContext.getString(R.string.message_what_is_sync,
			    		ClientManager.getServiceName(mContext, syncService)));
			    
			    int H = wm.getDefaultDisplay().getHeight();
			    mPopupWhatIsSync.getContentView().measure(0, 0);
			    int h = mPopupWhatIsSync.getContentView().getMeasuredHeight();
			    int[] location = new int[2];
			    anchor.getLocationInWindow(location);
			    int x = location[0];
			    int y = location[1];
			    
			    int py;
			    Drawable background;
			    if (y < H - y) {
			    	// below
			    	py = y;
			    	background = mContext.getResources().getDrawable(R.drawable.popup_message_below);
			    } else {
			    	// above
			    	py = y - h;
			    	background = mContext.getResources().getDrawable(R.drawable.popup_message_above);
			    }
			    
			    popupView.setBackgroundDrawable(background);
			    mPopupWhatIsSync.showAtLocation(anchor, Gravity.TOP | Gravity.LEFT, x, py);
			    
//			    System.out.println(String.format("H=%d, x=%d, y=%d, h=%d, py=%d", H, x, y, h, py));/*debug*/
			}
		}
	};
	
	/**
	 * 画面サイズに応じた描画サイズを計算します
	 *
	 * @param view	描画ビュー
	 */
	public void setDisplayLength() {

		wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		display = wm.getDefaultDisplay();

		Configuration config = mContext.getResources().getConfiguration();

		// 画面の向きによって、描画サイズの計算式を変更
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mParentLength = display.getWidth() / 10 - 10;
		} else {
			mParentLength = display.getWidth() / 6 - 10;
		}
	}







	protected CacheLoader createLoader() {
		CacheLoader loader = new CacheLoader();
		return loader;
	}

	/**
	 * 描画サイズを削除します
	 */
	public void clearLength() {
		this.mParentLength = null;
	}

	/**
	 * 画面サイズに応じた描画サイズを計算します
	 *
	 * @param view	描画ビュー
	 */
	public void setDisplayLength(View view) {

		wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		display = wm.getDefaultDisplay();

		Configuration config = view.getResources().getConfiguration();

		// 画像サイズの計算
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mParentLength = display.getWidth() / 7;
		} else {
			mParentLength = display.getWidth() / 5;
		}

		mLayoutParams = new LinearLayout.LayoutParams(mParentLength, mParentLength);
	}

	/**
	 * 画面サイズに応じた、一行のサイズを計算します
	 */
	public void setRowLength() {

		float pxHeight;

		// dp → pxへの変換
		// TODO frameLayoutにてfill等が効かないため、暫定処理
		final float scale = mContext.getResources().getDisplayMetrics().density;
		pxHeight = (int) (43 * scale + 0.5f);

		mRowHeight = (int) (mParentLength + pxHeight);


		wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		display = wm.getDefaultDisplay();

		Configuration config = mContext.getResources().getConfiguration();

		// TODO 縦同様、暫定処理　修正の必要あり
		float pxWidth = (int) (18 * scale + 0.5f);

		// 画面の向きによって、一行全体の横幅を取得
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mRowWidth = (int)(mParentLength * 10 + pxWidth);
		} else {
			mRowWidth = (int)(mParentLength * 6 + pxWidth);
		}
	}

	@Override
	public int getCount() {
		return mListEntry.size();
	}

	@Override
	public Object getItem(int position) {
		try {
			return mListEntry.get(position);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void dispose() {
		UXViewLoader.dispose();
		UXAsyncImageView.dispose();
		if (mPopupWhatIsSync != null && mPopupWhatIsSync.isShowing()) {
			mPopupWhatIsSync.dismiss();
		}
	}

}
