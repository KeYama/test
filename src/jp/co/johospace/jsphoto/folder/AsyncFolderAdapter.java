package jp.co.johospace.jsphoto.folder;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.co.johospace.jsphoto.LocalFolderActivity.FolderEntry;
import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.cache.CacheLoader;
import jp.co.johospace.jsphoto.grid.AsyncImageView;
import android.content.Context;
import android.content.res.Configuration;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * フォルダ一覧画面（フォルダグリッド形式）のアダプタです
 */
public class AsyncFolderAdapter extends BaseAdapter {

	protected Context mContext;

	/** 描画領域　長さ */
	private Integer mParentLength;

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
	LinearLayout.LayoutParams mChildLayout;

	/** 画像同時表示数 */
	private static final int IMAGE_GRID_COUNT = 4;

	/** 画像表示位置　インデックス */
	public static final int
		INDEX_FIRST = 0,
		INDEX_SECOND = 1,
		INDEX_THIRD = 2,
		INDEX_FOURTH = 3;

	private final CacheLoader mLoader;
	public AsyncFolderAdapter(Context context, List<FolderEntry> entries){
		mContext = context;
		mLoader = createLoader();
		mListEntry = entries;
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
	//		this.comparator = comparator;
			sort();
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
	public void setComparator(Comparator<FolderEntry> comparator) {
		this.mComparator = comparator;
	}

	/**
	 * フォルダ情報リストを取得
	 *
	 * @return フォルダ情報リスト
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
	static class ViewHolder {
		// レイアウト
		LinearLayout imgLayout;

		// 各画像ビュー
		AsyncImageView firstImage;
		AsyncImageView secondImage;
		AsyncImageView thirdImage;
		AsyncImageView fourthImage;

		// フォルダ名
		TextView folderName;
	}



	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View view = convertView;
		ViewHolder holder;

		// レイアウト設定
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.folder_grid_item, null);

			holder = new ViewHolder();

			// ビュー格納レイアウト
			holder.imgLayout = (LinearLayout) view.findViewById(R.id.lytImgLayout);

			// 各画像ビュー
			holder.firstImage = (AsyncImageView) view.findViewById(R.id.imgFirst);
			holder.firstImage.setLoader(mLoader);
			holder.secondImage = (AsyncImageView) view.findViewById(R.id.imgSecond);
			holder.secondImage.setLoader(mLoader);
			holder.thirdImage = (AsyncImageView) view.findViewById(R.id.imgThird);
			holder.thirdImage.setLoader(mLoader);
			holder.fourthImage = (AsyncImageView) view.findViewById(R.id.imgFourth);
			holder.fourthImage.setLoader(mLoader);

			// フォルダ名
			holder.folderName = (TextView) view.findViewById(R.id.txtFolderName);

			// レイアウト値が設定されていなければ、作成
			if (mParentLength == null || mChildLayout == null) {
				setDisplayLength(holder.imgLayout);
			}

			// ビューのレイアウト設定
			holder.imgLayout.setLayoutParams(mLayoutParams);
			holder.firstImage.setLayoutParams(mChildLayout);
			holder.secondImage.setLayoutParams(mChildLayout);
			holder.thirdImage.setLayoutParams(mChildLayout);
			holder.fourthImage.setLayoutParams(mChildLayout);

			view.setTag(holder);

		} else {
			holder = (ViewHolder) view.getTag();
		}


		// フォルダ情報取得
		FolderEntry fe = (FolderEntry)getItem(position);

		if (fe != null) {

			String folderPath = fe.getName();

			List<String> imgList = fe.getImages();

			// グリッド表示数と画像件数の差分算出
//			int count = IMAGE_GRID_COUNT - imgList.size();

//			// グリッド表示数に満たない場合、残りを空白で埋める
//			for (int i = 0; i < count; i++) {
//				imgList.add(null);
//			}

//			holder.firstImage.loadImage(imgList.get(0));
//			holder.secondImage.loadImage(imgList.get(1));
//			holder.thirdImage.loadImage(imgList.get(2));
//			holder.fourthImage.loadImage(imgList.get(3));

			AsyncImageView imgView = null;

			// 画像を結合
			for (int i = 0; i < IMAGE_GRID_COUNT; i++) {

				// 画像の表示位置を決定
				switch(i) {
					// 左上
					case INDEX_FIRST:
						imgView = holder.firstImage;
						break;

					// 右上
					case INDEX_SECOND:
						imgView = holder.secondImage;
						break;

					// 左下
					case INDEX_THIRD:
						imgView = holder.thirdImage;
						break;

					// 右下
					case INDEX_FOURTH:
						imgView = holder.fourthImage;
						break;
				}

				if (i > imgList.size() - 1) {
					imgView.setVisibility(View.GONE);

				} else {
					imgView.loadImage(imgList.get(i));
					imgView.setVisibility(View.VISIBLE);
				}
			}

			// フォルダ名を表示
			holder.folderName.setText(folderPath);
		}

		return view;
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

		int margin = 0;

		// 画像サイズから画像表示幅の計算
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			mParentLength = display.getWidth() / 3;
			margin = mParentLength / 20;
		} else {
			mParentLength = display.getWidth() / 2;
			margin = mParentLength / 15;
		}

		// 余白の差分をとった画像表示幅
		mParentLength = mParentLength - margin;
//		mParentLength = mParentLength - (mParentLength / 20);

		// 合成後の画像サイズ
		mLayoutParams = new LinearLayout.LayoutParams(mParentLength, mParentLength);

		// 子の画像サイズ
		mChildLayout = new LinearLayout.LayoutParams(mParentLength / 2, mParentLength / 2);
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
		mLoader.dispose();
	}
}
