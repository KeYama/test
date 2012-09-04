package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.util.PreferenceUtil;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IntroActivity extends AbstractActivity implements OnClickListener{

	private static final int REQUEST_INTRO = 1;

	/** チュートリアル画像ID */
	private static final int PAGE_FIRST = 0;
	private static final int PAGE_SECOND = 1;
	private static final int PAGE_THIRD = 2;
	
	/** レイアウト */
	ImageView mImage;
	
	ImageView mBtnBack;
	ImageView mBtnNext;
	ImageView mBtnFirst;
	ImageView mBtnSecond;
	ImageView mBtnThird;
	
	TextView mTitle;
	TextView mMesage;
	
	View mViewBack;
	View mViewNext;
	
	LinearLayout mLytInfo;
	FrameLayout mFrameInfo;
	
	/** 選択中のチュートリアル番号 */
	int mSelectPage = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		init();

		// 初回起動時のみ、承諾画面を開く
		showConsentActivity();

	}

	/**
	 * 画面の初期化
	 */
	private void init() {
		
		setContentView(R.layout.intro);
		
//		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//		Display display = wm.getDefaultDisplay();
//		
//		double reduction;
//		double reductionImage;
//		
//		Configuration config = getResources().getConfiguration();
//		
//		if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
//			reductionImage = 0.5;
//			reduction = 0.085;
//		} else {
//			reductionImage = 1;
//			reduction = 0.05;
//		}
		
		mImage = (ImageView) findViewById(R.id.ivTutorial);
//		LayoutParams layoutImage = mImage.getLayoutParams();
//		layoutImage.height = (int) (display.getHeight() * reductionImage);
//		layoutImage.width = display.getWidth();
//		mImage.setLayoutParams(layoutImage);
		
//		mFrameInfo = (FrameLayout) findViewById(R.id.frmInfo);
//		LayoutParams layoutFrame = mFrameInfo.getLayoutParams();
//		layoutFrame.height = (int) (display.getHeight() * reductionImage);
//		layoutFrame.width = display.getWidth();
//		mFrameInfo.setLayoutParams(layoutFrame);
		
		mLytInfo = (LinearLayout) findViewById(R.id.lytPageIcon);
//		LayoutParams layoutParams = mLytInfo.getLayoutParams();
//		layoutParams.height = (int) (display.getHeight() * reduction);
//		layoutParams.width = display.getWidth();
//		mLytInfo.setLayoutParams(layoutParams);

		mBtnBack = (ImageView) findViewById(R.id.imageBack);
		mBtnNext = (ImageView) findViewById(R.id.imageNext);
		mBtnFirst = (ImageView) findViewById(R.id.imagePageFirst);
		mBtnSecond = (ImageView) findViewById(R.id.imagePageSecond);
		mBtnThird = (ImageView) findViewById(R.id.imagePageThird);
		
		mTitle = (TextView) findViewById(R.id.text_tutorial_title);
		mMesage = (TextView) findViewById(R.id.text_tutorial_message);
		
		
		
		mViewBack = findViewById(R.id.viewBack);
		mViewNext = findViewById(R.id.viewNext);
		
		mBtnBack.setOnClickListener(this);
		mBtnNext.setOnClickListener(this);
		mBtnFirst.setOnClickListener(this);
		mBtnSecond.setOnClickListener(this);
		mBtnThird.setOnClickListener(this);
		
		showInfoPage(PAGE_FIRST);
		
		findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * 承諾画面を開く
	 */
	private void showConsentActivity(){
		//初回起動時、承諾画面を表示する
		boolean notFirst = PreferenceUtil.getBooleanPreferenceValue(this, ApplicationDefine.KEY_NOT_FIRST_TIME);
		if(!notFirst){
			Intent intent = new Intent(IntroActivity.this, IntroTermsOfServiceActivity.class);
			startActivityForResult(intent, REQUEST_INTRO);
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//			builder.setTitle(getResources().getString(R.string.intro_title_terms_of_service));
//			builder.setMessage(getResources().getString(R.string.intro_title_terms_of_service));
//			builder.setPositiveButton(getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int id) {
//					IntroActivity.this.finish();
//				}
//			});
//			builder.setNegativeButton("了承", new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int id) {
//					// プリファレンスをセット
//					PreferenceUtil.setBooleanPreferenceValue(IntroActivity.this, ApplicationDefine.KEY_NOT_FIRST_TIME, true);
//				}
//			});
//			builder.setOnCancelListener(new DialogInterface.OnCancelListener(){
//				public void onCancel(DialogInterface dialog){
//					IntroActivity.this.finish();
//				}
//			});
//			AlertDialog alert = builder.create();
//			alert.show();
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
		case REQUEST_INTRO:
			if(resultCode == RESULT_OK){
				// プリファレンスをセット
				PreferenceUtil.setBooleanPreferenceValue(IntroActivity.this, ApplicationDefine.KEY_NOT_FIRST_TIME, true);
			} else {
				IntroActivity.this.finish();
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		
		// バックボタン
		case R.id.imageBack:
			
			mSelectPage = mSelectPage - 1;
			if (mSelectPage < 0) mSelectPage = PAGE_FIRST; 
			
			showInfoPage(mSelectPage);
			break;
		
		// ネクストボタン
		case R.id.imageNext:
			
			mSelectPage = mSelectPage + 1;
			if (mSelectPage > 2) mSelectPage = PAGE_THIRD; 
			
			showInfoPage(mSelectPage);
			
			break;
			
		// 1番目の画像
		case R.id.imagePageFirst:
			showInfoPage(PAGE_FIRST);
			break;
			
		// 2番目の画像
		case R.id.imagePageSecond:
			showInfoPage(PAGE_SECOND);
			break;
			
		// 3番目の画像
		case R.id.imagePageThird:
			showInfoPage(PAGE_THIRD);
			break;
			
		default:
			showInfoPage(PAGE_FIRST);
			break;
		}
	}
	
	/**
	 * 番号に応じたチュートリアル画像とテキストを表示します
	 * 
	 * @param pageNumber	表示画面番号
	 */
	private void showInfoPage(int pageNumber) {
		
		Drawable image;
		String title;
		String message;
		
		switch (pageNumber) {
		
		// 1番目
		case PAGE_FIRST:
			mSelectPage = PAGE_FIRST;
			image = getResources().getDrawable(R.drawable.info_image_first);
			title = getString(R.string.intro_label_title_first);
			message = getString(R.string.intro_label_message_first);
			mBtnBack.setVisibility(View.GONE);
			mBtnNext.setVisibility(View.VISIBLE);
			mViewBack.setVisibility(View.VISIBLE);
			mViewNext.setVisibility(View.GONE);
			
			mBtnFirst.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_on));
			mBtnSecond.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_off));
			mBtnThird.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_off));
			
			break;
			
		// 2番目
		case PAGE_SECOND:
			mSelectPage = PAGE_SECOND;
			image = getResources().getDrawable(R.drawable.info_image_second);
			title = getString(R.string.intro_label_title_second);
			message = getString(R.string.intro_label_message_second);
			mBtnBack.setVisibility(View.VISIBLE);
			mBtnNext.setVisibility(View.VISIBLE);
			mViewBack.setVisibility(View.GONE);
			mViewNext.setVisibility(View.GONE);
			
			mBtnFirst.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_off));
			mBtnSecond.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_on));
			mBtnThird.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_off));
			
			break;
			
		// 3番目
		case PAGE_THIRD:
			mSelectPage = PAGE_THIRD;
			image = getResources().getDrawable(R.drawable.info_image_third);
			title = getString(R.string.intro_label_title_third);
			message = getString(R.string.intro_label_message_third);
			mBtnBack.setVisibility(View.VISIBLE);
			mBtnNext.setVisibility(View.GONE);
			mViewBack.setVisibility(View.GONE);
			mViewNext.setVisibility(View.VISIBLE);
			
			mBtnFirst.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_off));
			mBtnSecond.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_off));
			mBtnThird.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_on));
			
			break;
			
		default:
			image = getResources().getDrawable(R.drawable.tutorial_search);
			title = getString(R.string.intro_label_title_first);
			message = getString(R.string.intro_label_message_first);
			mBtnBack.setVisibility(View.VISIBLE);
			mBtnNext.setVisibility(View.GONE);
			
			mBtnFirst.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_on));
			mBtnSecond.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_off));
			mBtnThird.setImageDrawable(getResources().getDrawable(R.drawable.info_indicator_off));
			break;
		}
		
		mImage.setImageDrawable(image);
//		mImage.setBackgroundDrawable(image);
		mTitle.setText(title);
		mMesage.setText(message);
	}
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT || newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
		
			int selectPage = mSelectPage;
			
			init();
			
			showInfoPage(selectPage);
			
//			WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//			Display display = wm.getDefaultDisplay();
//			
//			double reduction;
//			double reductionImage;
//			
//			if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//				reductionImage = 0.5;
//				reduction = 0.085;
//				
//			} else {
//				reductionImage = 1;
//				reduction = 0.05;
//			}
//			
//			LayoutParams layoutImage = mImage.getLayoutParams();
//			layoutImage.height = (int) (display.getHeight() * reductionImage);
//			layoutImage.width = display.getWidth();
//			mImage.setLayoutParams(layoutImage);
//			
//			LayoutParams layoutFrame = mFrameInfo.getLayoutParams();
//			layoutFrame.height = (int) (display.getHeight() * reductionImage);
//			layoutFrame.width = display.getWidth();
//			mFrameInfo.setLayoutParams(layoutFrame);
//			
//			mLytInfo = (LinearLayout) findViewById(R.id.lytPageIcon);
//			LayoutParams layoutParams = mLytInfo.getLayoutParams();
//			layoutParams.height = (int) (display.getHeight() * reduction);
//			layoutParams.width = display.getWidth();
//			mLytInfo.setLayoutParams(layoutParams);
			
		}
	}
	
}
