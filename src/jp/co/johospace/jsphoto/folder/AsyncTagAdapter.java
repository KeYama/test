package jp.co.johospace.jsphoto.folder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.TagListActivity.TagEntry;
import jp.co.johospace.jsphoto.cache.CacheLoader;
import jp.co.johospace.jsphoto.cache.LocalCachedThumbnailLoader;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import jp.co.johospace.jsphoto.ux.view.UXAsyncImageView;
import jp.co.johospace.jsphoto.ux.view.UXViewLoader;
import android.content.Context;
import android.content.res.Configuration;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * タグ一覧画面のアダプタです
 */
public class AsyncTagAdapter extends BaseAdapter{

	protected Context mContext;
	
	/** 描画領域　長さ */
	private Integer mParentLength;
	
	/** 一行の幅 */
	private Integer mRowWidth;
	
	/** 一行の高さ */
	private Integer mRowHeight;
	
	/** フォルダ情報のリスト */
	private final List<TagEntry> mListEntry;
	
	/** ソート条件 */
	private Comparator<TagEntry> mComparator;
	
	/** ウィンドウマネージャ */
	private WindowManager wm; 
	
	/** ディスプレイ */
	private Display display; 
	
	/** 描画領域　レイアウト */
	LinearLayout.LayoutParams mLayoutParams;
	
	/** シークレット非表示リスナ */
	private OnClickListener mSecretHideListener;
	/** オーバーレイ　閉じるボタンのリスナ */
	private OnClickListener mCategoryHideListener;

	public AsyncTagAdapter(Context context, ArrayList<TagEntry> tagEntry){
		this(context, tagEntry, true);
	}
	
	public AsyncTagAdapter(Context context, List<TagEntry> entries, boolean showFolderPath){
		mContext = context;
		mListEntry = entries;
	}
	
	/**
	 * フォルダ情報一覧を追加
	 * @param pathCollection	フォルダ情報のコレクション
	 */
	public void addAll(Collection<TagEntry> pathCollection){
		mListEntry.addAll(pathCollection);
		notifyDataSetChanged();
	}
	
	/**
	 * フォルダ情報を追加
	 * @param fe	フォルダ情報
	 */
	public void addItem(TagEntry fe){
		mListEntry.add(fe);
	}

	/**
	 * フォルダ情報リストをソート
	 * @param comparator	ソート条件
	 */
	public void sort(Comparator<TagEntry> comparator) {
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
	
	
	/**
	 * ソート条件をセット
	 * 
	 * @param comparator ソート条件
	 */
	public void setComparator(Comparator<TagEntry> comparator) {
		this.mComparator = comparator;
	}
	
	/**
	 * フォルダ情報リストを取得
	 * 
	 * @return　フォルダ情報リスト
	 */
	public List<TagEntry> getEntryList() {
		return mListEntry;
	}
	
	/**
	 * ソート条件を取得
	 * 
	 * @return ソート条件
	 */
	public Comparator<TagEntry> getComparator() {
		return mComparator;
	}
	
	/**
	 * リストの内容を更新
	 */
	public void update(TagEntry entry) {
		mListEntry.set(mListEntry.indexOf(entry), entry);
		notifyDataSetChanged();
	}
	
	/**
	 * シークレット非表示ボタンのリスナをセット
	 * 
	 * @param listener リスナ
	 */
	public void setSecretHide(OnClickListener listener) {
		mSecretHideListener = listener;
	}
	
	/**
	 * カテゴリの非表示リスナをセット
	 * 
	 * @param listener リスナ
	 */
	public void setCategoryHide(OnClickListener listener) {
		mCategoryHideListener = listener;
	}
	
	/**
	 * 一行のレイアウトクラス
	 */
	static class ViewHolder {
		
		// 画像ビュー
		UXAsyncImageView imageView1;
		UXAsyncImageView imageView2;
		UXAsyncImageView imageView3;
		UXAsyncImageView imageView4;
		UXAsyncImageView imageView5;
		UXAsyncImageView imageView6;
		UXAsyncImageView imageView7;
		UXAsyncImageView imageView8;
		UXAsyncImageView imageView9;
		UXAsyncImageView imageView10;
		
		// 一行レイアウト
		FrameLayout frmTag;
		
		// タグ名
		TextView tagName;
		
		// メディアファイル数
		TextView mediaCount;
		
		// シークレット非表示ボタンレイアウト
		FrameLayout frmSecret;
		
		// シークレット非表示テキスト
		TextView secretHide;
		
		// ０件時、オーバーレイ
		FrameLayout frmOverlay;
		
		// オーバーレイテキスト
		TextView txtOverlay;
		
		// オーバーレイ閉じるボタン
		ImageButton overlayClose;
		
		// メディアファイル数　アイコン
		ImageView countImage;
		
		// カテゴリー種別　アイコン
		ImageView tagImage;
		
		// 選択時ビュー
		View select;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		//TODO 画像一覧のソート処理を適応予定
		View view = convertView;

		// その他レイアウト
		TextView tagName;
		TextView mediaCount;
		TextView secretHide;
		TextView txtOverlay;
		FrameLayout frmTag;
		FrameLayout frmSecret;
		FrameLayout frmOverlay;
		
		ImageButton overlayClose;
		
		ImageView countImage;
		ImageView tagImage;
		
		View select;
		
		LinearLayout layout = null;
		
		// フォルダ情報取得
		TagEntry fe = (TagEntry)getItem(position);
		
		// レイアウト設定
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
			view = inflater.inflate(R.layout.tag_list_item, null);
			
			layout = (LinearLayout)view.findViewById(R.id.lytTagImageContainer);
			frmOverlay = (FrameLayout)view.findViewById(R.id.overlay);
			
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
			
			select = (View)view.findViewById(R.id.viewOver);

			LayoutParams paramSelect = select.getLayoutParams();
			LayoutParams paramOverlay = frmOverlay.getLayoutParams();
			
			// 一行の高さと幅を取得
			if (mRowHeight == null || mRowWidth == null) {
				setRowLength();
			}
			
			paramSelect.height = mRowHeight;
			paramOverlay.height = mRowHeight;
			
			paramSelect.width = mRowWidth;
			
			// 横幅を設定
			select.setLayoutParams(paramSelect);
			frmOverlay.setLayoutParams(paramOverlay);	
			
			
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
			// その他レイアウト
			frmTag = (FrameLayout) view.findViewById(R.id.frmTag);
			tagName = (TextView)view.findViewById(R.id.txtTagName);
			mediaCount = (TextView)view.findViewById(R.id.txtMediaCount);
			frmSecret = (FrameLayout)view.findViewById(R.id.frmSecret);
			secretHide = (TextView)view.findViewById(R.id.txtSecretHide);
			txtOverlay = (TextView)view.findViewById(R.id.overlay_txt);
			overlayClose = (ImageButton)view.findViewById(R.id.overlay_close);
			countImage = (ImageView)view.findViewById(R.id.imgMediaCount);
			tagImage = (ImageView)view.findViewById(R.id.imgTag);
			
			view.setTag(layout);
		} else {
			layout = (LinearLayout)view.getTag();
			// その他レイアウト
			frmTag = (FrameLayout) view.findViewById(R.id.frmTag);
			tagName = (TextView)view.findViewById(R.id.txtTagName);
			mediaCount = (TextView)view.findViewById(R.id.txtMediaCount);
			frmSecret = (FrameLayout)view.findViewById(R.id.frmSecret);
			frmOverlay = (FrameLayout)view.findViewById(R.id.overlay);
			secretHide = (TextView)view.findViewById(R.id.txtSecretHide);
			txtOverlay = (TextView)view.findViewById(R.id.overlay_txt);
			overlayClose = (ImageButton)view.findViewById(R.id.overlay_close);
			countImage = (ImageView)view.findViewById(R.id.imgMediaCount);
			tagImage = (ImageView)view.findViewById(R.id.imgTag);
			
			select = (View)view.findViewById(R.id.viewOver);
		}
		
		
		if (fe != null) {
			// clear
			for( int n = 0; n < layout.getChildCount(); ++n){
				//TODO ImageViewのサイズ × 2
				((UXAsyncImageView)layout.getChildAt(n)).loadImage(null, mParentLength * 2);
			}
			
			// 画像情報取得
//			if (fe.getImages().size() > 0) {
			if (fe.getPathList().size() > 0) {
				// 横並びにフォルダ内の画像を表示
				for (int i = 0; i < fe.getPathList().size() && i < layout.getChildCount(); i++) {
					File file = new File(fe.getPathList().get(i));
					//TODO ImageViewのサイズ × 2
					((UXAsyncImageView)layout.getChildAt(i)).loadImage(file.getAbsolutePath(), mParentLength * 2);
				}
			}
			
			// フォルダ名とフォルダ内画像件数を表示
			String folder = fe.getName();

			// フォルダ名
			tagName.setText(folder);
			
			// メディアファイル数
			mediaCount.setText(String.valueOf(fe.getMediaCount()));
			
			// メディアファイル数
			int count = fe.getMediaCount();
			
			// ファイル数アイコン セット
			if (count >= 1000) {
				mediaCount.setVisibility(View.GONE);
				countImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.file_over_black));
			} else {
				mediaCount.setVisibility(View.VISIBLE);
				mediaCount.setText(String.valueOf(count));
				countImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.file_black));
			}
			
			// 非表示ボタンの表示状態をセット
			if (fe.getTagCategory() != null && fe.getTagCategory().equals(ApplicationDefine.TAG_CATEGORY_SECRET)) {
				frmSecret.setVisibility(View.VISIBLE);
				
				if (mSecretHideListener != null) {
					secretHide.setOnClickListener(mSecretHideListener);
				}
				
			} else {
				frmSecret.setVisibility(View.GONE);
			}
			
			LinearLayout.LayoutParams tagParams = (android.widget.LinearLayout.LayoutParams) frmTag.getLayoutParams();
			
			if (fe.getTagCategory() != null && fe.getTagCategory().equals(ApplicationDefine.TAG_CATEGORY_FAVORITE)) {
				tagParams.setMargins(5, 11, 5, 5);
			} else {
				tagParams.setMargins(5, 5, 5, 5);
			}
			
			frmTag.setLayoutParams(tagParams);
			
			
			// カテゴリのオーバーレイ関連処理
			
			// 件数0の際のオーバーレイ
			if(frmOverlay != null){
				int mcnt = fe.getMediaCount();

				if(mcnt < 1){
					
					if(txtOverlay != null){
						
						String message;
						
						if (fe.getTagCategory().equals(ApplicationDefine.TAG_CATEGORY_TAG)) {
							message = view.getResources().getString(R.string.folder_message_no_tag_file);
						} else {
							message = folder + view.getResources().getString(R.string.folder_message_no_file);
						}
						
						txtOverlay.setText((CharSequence)message);
						frmOverlay.setVisibility(View.VISIBLE);
					}
				} else {
					frmOverlay.setVisibility(View.GONE);
				}
			}
			
			// オーバーレイの閉じるボタンにリスナをセット
			if (mCategoryHideListener != null) {
				overlayClose.setOnClickListener(mCategoryHideListener);
				overlayClose.setTag(fe.getTagCategory());
			}
			
			// タグアイコンを対応するものに変更
			if (fe.getTagCategory().equals(ApplicationDefine.TAG_CATEGORY_SECRET)) {
				tagImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.tag_secret));
			} else if (fe.getTagCategory().equals(ApplicationDefine.TAG_CATEGORY_FAVORITE)) {
				tagImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.tag_star));
			} else {
				tagImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.tag_a));
			}
		}
		return view;
	}


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
	
	
	/**
	 * 画面サイズに応じた、一行のサイズを計算します
	 */
	public void setRowLength() {
		
		float pxHeight;
		
		// dp → pxへの変換
		// TODO frameLayoutにてfill等が効かないため、暫定処理
		final float scale = mContext.getResources().getDisplayMetrics().density;  
		pxHeight = (int) (44 * scale + 0.5f);  

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
	}
	
	
}
