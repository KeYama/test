package jp.co.johospace.jsphoto;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class NoMountActivity extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Toast.makeText(this, getResources().getString(R.string.no_sd_toast), Toast.LENGTH_LONG).show();
		setContentView(R.layout.no_sd);
	}
}
