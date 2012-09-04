package jp.co.johospace.jsphoto.view;

import java.io.IOException;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.accessor.TagMasterAccessor;
import jp.co.johospace.jsphoto.cache.ImageCache;
import jp.co.johospace.jsphoto.cache.ImageCache.ImageData;
import jp.co.johospace.jsphoto.database.OpenHelper;
import jp.co.johospace.jsphoto.util.ImageUtil;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * カテゴリのビューです
 */
public class CategoryView extends LinearLayout implements OnClickListener {

	/** DBアクセス */
	private static SQLiteDatabase mDatabase = OpenHelper.external.getDatabase();
	
	/** サムネイル画像 */
	private ImageView mImage;
	/** ヘッダ名 */
	private TextView mTxtHeader;
	/** タイトルテキスト */
	private TextView mTxtTop;
	/** 詳細テキスト */
	private TextView mTxtData;
	/** 画像 */
	private ImageView mImageGone;
	/** カテゴリ詳細レイアウト */
	private LinearLayout mLytCategoryData;
	
	/** カテゴリ名 */
	public String mCategoryName;
	/** 表示名 */
	public String mDisplayName;
	/** 表示状態 */
	public boolean mVisivility = true;
	
	/** タグ判定フラグ */
	public boolean mIsTag = false;
	
	
	public CategoryView(Context context, LayoutParams layoutParam) {
		super(context);
		init(context, layoutParam);
	}
	
	@Override
	public int getId() {
		return super.getId();
	}
	
	/**
	 * 初期化処理
	 */
	public void init(Context context,  LayoutParams layoutParam) {
		
		LayoutInflater inflater = LayoutInflater.from(context);
		
		View view = inflater.inflate(R.layout.home_item, null);
		view.setLayoutParams(layoutParam);
		
		addView(view);
		
		mImage = (ImageView) view.findViewById(R.id.imgCategory);
		
		mTxtHeader = (TextView) view.findViewById(R.id.txtCategory);
		mTxtTop = (TextView) view.findViewById(R.id.txtCategoryTop);
		mTxtData = (TextView) view.findViewById(R.id.txtCategoryData);
		
		mLytCategoryData = (LinearLayout) view.findViewById(R.id.lytCategoryData);
		
		mImageGone = (ImageView) view.findViewById(R.id.imgDeleteCategory);
		mImageGone.setOnClickListener(this);
	}
	
	/**
	 * 画像パスを元にサムネイルを取得し、ビューに表示します
	 * 
	 * @param folder	フォルダパス
	 * @param name		ファイル名
	 * @param length	描画領域の長さ
	 */
	public void setImage (String folder, String name, int length) {
		ImageData im = new ImageData();
		
		try {
			// キャッシュを取得
			ImageCache.getImageCache(folder, name, im);
		} catch (IOException e) {
//			e.printStackTrace();		/*$debug$*/
		}
		
		Bitmap bitmap;
		
		// サムネイルをリサイズ
		if (im != null && im.data != null) {
			bitmap = BitmapFactory.decodeByteArray(im.data, 0, im.size);
			setImage(bitmap, length);
		}
	}
	
	public void setImage(Bitmap bitmap, int length) {
		bitmap = ImageUtil.resizeBitmap(bitmap, length, length);
		
		mImage.setImageBitmap(bitmap);
	}
	
	/**
	 * ダミー画像を作成し、ビューに表示します
	 * 
	 * @param length	描画領域の長さ
	 */
	public void setDummyImage(int length) {
		Bitmap bitmap = Bitmap.createBitmap(length, length, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(Color.LTGRAY);
		mImage.setImageBitmap(bitmap);
	}
	
	/**
	 * リソースIDを元に、画像を表示します。
	 * 
	 * @param imageId	画像ID
	 * @param length	描画領域の長さ
	 */
	public void setImage(int imageId, int length) {
		LayoutParams params = new LayoutParams(length, length);
		params.setMargins(5, 5, 5, 5);
		mImage.setLayoutParams(params);
		mImage.setImageResource(imageId);
	}
	
	/**
	 * ヘッダ名をセットします
	 */
	public void setHeaderName(String headerName) {
		mTxtHeader.setText(headerName);
		mDisplayName = headerName;
	}
	
	/**
	 * タグ名をセットします
	 */
	public void setTextTop(String text) {
		mTxtTop.setText(text);
	}
	
	/**
	 * 詳細テキストをセットします
	 */
	public void setTextData(String text) {
		mTxtData.setText(text);
	}
	
	/**
	 * カテゴリ名をセットします
	 */
	public void setCategoryName(String name) {
		mCategoryName = name;
	}
	
	/**
	 * 表示状態をセットします
	 */
	public void setStatusVisible(boolean visible) {
		mVisivility = visible;
		
		int visivility = visible ? View.VISIBLE : View.GONE;
		
		setVisibility(visivility);
	}
	
	/**
	 * タグかどうかのフラグをセットします
	 */
	public void setIsTag(boolean isTag) {
		mIsTag = isTag;
	}

	/**
	 * カテゴリ詳細レイアウトにクリックリスナをセットします
	 */
	public void setCategoryClickListener(OnClickListener clickListener) {
		mLytCategoryData.setOnClickListener(clickListener);
	}
	
	@Override
	public void onClick(View v) {
	
		// カテゴリ全体を非表示に
		setVisibility(View.GONE);
		mVisivility = false;
		
		if (mIsTag) {
			TagMasterAccessor.updateTagHide(mDatabase, mCategoryName, mVisivility);
		} else {
			PreferenceUtil.setBooleanPreferenceValue(getContext(), mCategoryName, mVisivility);
		}
		
		Toast.makeText(getContext(), getResources().getString(R.string.home_message_close_category), Toast.LENGTH_LONG).show();
	}
}
