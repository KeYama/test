package jp.co.johospace.jsphoto;

import java.util.ArrayList;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * オートアルバムチュートリアル画面アクティビティです
 */
public class TutorialAutoalbumActivity extends AbstractActivity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 初期処理
		init();

	}

	/**
	 * 初期処理
	 */
	private void init(){

		setContentView(R.layout.tutorial_autoalbum);

		// 同期アプリケーションのリストを初期化
		initSyncapkList();

	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
		// 初期処理
		init();
    }

	@Override
	public void onClick(View view) {

//		if (view.getId() == R.id.viewOver){
//			// Layoutリストの行クリック
//
//			// ※　Tag にて選択されたサービスを取得
//			Toast.makeText(this, ((View)view.findViewById(R.id.viewOver)).getTag().toString(), Toast.LENGTH_SHORT).show();
//
//		}

	}

	/**
	 * 同期アプリケーションのリストを初期化
	 */
	private void initSyncapkList() {

		// 同期アプリケーションの Layout を取得
		LinearLayout layout = (LinearLayout)findViewById(R.id.laySyncapk);

		// 同期アプリケーションを取得
		ArrayList<SyncapkItem> listApk = new ArrayList<SyncapkItem>();

		listApk.add(new SyncapkItem(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_online_jorte), "Jorte Calendar"));
		listApk.add(new SyncapkItem(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_autoalbum_google), "Google Calendar"));

		int rowno = 0;
		// Layout に同期アプリケーションを追加
		for (SyncapkItem item : listApk) {

			// 行を作成
			View view = getLayoutInflater().inflate(R.layout.tutorial_syncapk_row, null);

			// 値をセット
			((ImageView)view.findViewById(R.id.ivApk)).setImageBitmap(item.icon);
			((TextView)view.findViewById(R.id.tvApkName)).setText(item.name);
			((ImageView)view.findViewById(R.id.ivAddRevoke)).setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.button_plus));

//			((View)view.findViewById(R.id.viewOver)).setTag(item.name);
//			((View)view.findViewById(R.id.viewOver)).setOnClickListener(this);

			// 行を追加
			layout.addView(view);

			rowno ++;

		}

	}

	//要素のクラス
	public class SyncapkItem {
	    public Bitmap icon;
	    public String name;

	    private SyncapkItem(Bitmap aIcon, String aName) {
	    	icon = aIcon;
	    	name = aName;
	    }
	}

}
