package jp.co.johospace.jsphoto;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager.LayoutParams;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

/**
 * 関連情報Howto画面アクティビティです
 */
public class InfoHowtoActivity  extends FragmentActivity implements OnClickListener{

	/** OKボタン */
	private Button _btnOk;
	/** キャンセルボタン */
	private Button _btnCancel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.info_howto);
		setTitle(getString(R.string.info_title_howto));

		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.WRAP_CONTENT;

        getWindow().setAttributes(params);

        // 初期処理
		init();
	}

	/**
	 * 初期処理
	 *
	 */
	private void init() {

		// 各ビューを設定
		_btnOk = (Button)findViewById(R.id.btnOk);
		_btnOk.setOnClickListener(this);

		_btnCancel = (Button)findViewById(R.id.btnCancel);
		//_btnCancel.setOnClickListener(this);
		_btnCancel.setVisibility(View.GONE); // 非表示にする

	}

	@Override
	public void onClick(View view) {

		// OKボタン押下
		if (view.getId() == R.id.btnOk) {

			setResult(RESULT_OK);
			finish();

		}
	}

}
