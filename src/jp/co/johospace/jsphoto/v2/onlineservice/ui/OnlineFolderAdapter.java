package jp.co.johospace.jsphoto.v2.onlineservice.ui;

import java.util.List;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.folder.AsyncListAdapter.ViewHolder;
import jp.co.johospace.jsphoto.ux.loader.UXThumbnailLoader;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import jp.co.johospace.jsphoto.ux.view.UXViewLoader;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import android.content.Context;
import android.content.res.Configuration;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * オンラインフォルダ一覧アダプタ
 */
public class OnlineFolderAdapter extends BaseAdapter {

	private final Context mContext;
	private List<Directory> mDirectories;
	private final UXThumbnailLoader mLoader;
	private final WindowManager mWindowManager; 
	private final Display mDisplay;
	private final int mBorderColor;
	private Integer mImageSize;
	
	/** 一行の幅 */
	private Integer mRowWidth;
	/** 一行の高さ */
	private Integer mRowHeight;
	/** 描画領域　長さ */
	private Integer mParentLength;

	public OnlineFolderAdapter(Context context, UXThumbnailLoader loader, List<Directory> directories) {
		super();
		mContext = context;
		mDirectories = directories;
		mLoader = loader;
		mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		mDisplay = mWindowManager.getDefaultDisplay();
		mBorderColor = context.getResources().getColor(R.color.black);
	}
	
	@Override
	public int getCount() {
		return mDirectories.size();
	}

	@Override
	public Object getItem(int position) {
		return mDirectories.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View view = convertView;

		/*
		// その他レイアウト
		TextView folderName;
		TextView mediaCount;
		
		ImageView countImage;
		*/

		LinearLayout layout = null;
		
		// フォルダ情報取得
		Directory dir = (Directory) getItem(position);
		
		ViewHolder holder = null;
		
		// レイアウト設定
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
			view = inflater.inflate(R.layout.online_folder_list_item, null);
			
			layout = (LinearLayout)view.findViewById(R.id.lytImageContainer);

			// 画像表示ローダ
			UXViewLoader loader = new UXViewLoader(mLoader);

			// 画像レイアウト設定
			UXAsyncImageView imageView1 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView2 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView3 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView4 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView5 = new UXAsyncImageView(mContext);
			UXAsyncImageView imageView6 = new UXAsyncImageView(mContext);

			imageView1.setViewLoader(loader);
			imageView2.setViewLoader(loader);
			imageView3.setViewLoader(loader);
			imageView4.setViewLoader(loader);
			imageView5.setViewLoader(loader);
			imageView6.setViewLoader(loader);

			// 画像のサイズ指定
			setDisplayLength();
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mImageSize, mImageSize);
			imageView1.setLayoutParams(params);
			imageView2.setLayoutParams(params);
			imageView3.setLayoutParams(params);
			imageView4.setLayoutParams(params);
			imageView5.setLayoutParams(params);
			imageView6.setLayoutParams(params);
			
			imageView1.setBorder(true, mBorderColor, 1.5f);
			imageView2.setBorder(true, mBorderColor, 1.5f);
			imageView3.setBorder(true, mBorderColor, 1.5f);
			imageView4.setBorder(true, mBorderColor, 1.5f);
			imageView5.setBorder(true, mBorderColor, 1.5f);
			imageView6.setBorder(true, mBorderColor, 1.5f);

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

				imageView7.setViewLoader(loader);
				imageView8.setViewLoader(loader);
				imageView9.setViewLoader(loader);
				imageView10.setViewLoader(loader);

				imageView7.setLayoutParams(params);
				imageView8.setLayoutParams(params);
				imageView9.setLayoutParams(params);
				imageView10.setLayoutParams(params);

				imageView7.setBorder(true, mBorderColor, 1.5f);
				imageView8.setBorder(true, mBorderColor, 1.5f);
				imageView9.setBorder(true, mBorderColor, 1.5f);
				imageView10.setBorder(true, mBorderColor, 1.5f);

				layout.addView(imageView7);
				layout.addView(imageView8);
				layout.addView(imageView9);
				layout.addView(imageView10);
			} else {

			}
			// その他レイアウト
//			folderName = (TextView)view.findViewById(R.id.txtFolderName);
//			mediaCount = (TextView)view.findViewById(R.id.txtMediaCount);
//			countImage = (ImageView)view.findViewById(R.id.imgMediaCount);
			
			holder = new ViewHolder();
			holder.layout = layout;
			// その他レイアウト
			holder.folderName = (TextView)view.findViewById(R.id.txtFolderName);
			holder.mediaCount = (TextView)view.findViewById(R.id.txtMediaCount);
			holder.countImage = (ImageView)view.findViewById(R.id.imgMediaCount);

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
			
			//view.setTag(layout);
		} else {
			holder = (ViewHolder)view.getTag();
			layout = holder.layout;
			/*
			layout = (LinearLayout)view.getTag();
			// その他レイアウト
			folderName = (TextView)view.findViewById(R.id.txtFolderName);
			mediaCount = (TextView)view.findViewById(R.id.txtMediaCount);
			countImage = (ImageView)view.findViewById(R.id.imgMediaCount);
			*/
		}

		
		if (dir != null) {
			// サムネイルロード
			final int C = layout.getChildCount();
			for( int i = 0; i < C; ++i){
				UXAsyncImageView image = (UXAsyncImageView) layout.getChildAt(i);
				if (dir.media != null && i < dir.media.size()) {
					//TODO ImageViewのサイズ × 2
					image.loadImage(dir.media.get(i), mImageSize * 2);
				} else {
					image.loadImage(null, 0);
				}
			}
			
			// フォルダ名
			holder.folderName.setText(dir.name);

			// メディアファイル数
			int count = dir.mediaCount;
			
			// ファイル数アイコン　セット
			if (count >= 1000) {
				holder.mediaCount.setVisibility(View.GONE);
				holder.countImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.file_over_black));
			} else {
				holder.mediaCount.setVisibility(View.VISIBLE);
				holder.mediaCount.setText(String.valueOf(count));
				holder.countImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.file_black));
			}
			
		}
		return view;
	}
	
	/**
	 * 画面サイズに応じた、一行のサイズを計算します
	 */
	public void setRowLength() {

		float pxHeight;

		// dp → pxへの変換
		// TODO frameLayoutにてfill等が効かないため、暫定処理
		final float scale = mContext.getResources().getDisplayMetrics().density;
		pxHeight = (int) (50 * scale + 0.5f);

		mRowHeight = (int) (mParentLength + pxHeight);

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
	
	public void setDisplayLength() {
		WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Configuration config = mContext.getResources().getConfiguration();
		// 画面の向きによって、描画サイズの計算式を変更
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mImageSize = mDisplay.getWidth() / 10 - 10;
			mParentLength = display.getWidth() / 10 - 10;
		} else {
			mImageSize = mDisplay.getWidth() / 6 - 10;
			mParentLength = display.getWidth() / 6 - 10;
		}
	}

	/** メモリを解放 **/
	public void dispose(){
		UXAsyncImageView.dispose();
	}
}
