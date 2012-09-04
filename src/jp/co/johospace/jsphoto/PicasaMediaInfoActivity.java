package jp.co.johospace.jsphoto;

import jp.co.johospace.jsphoto.cache.PicasaCache;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.PhotoEntry;
import jp.co.johospace.jsphoto.util.JsonUtil;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Picasa参照 メディア情報
 */
public class PicasaMediaInfoActivity extends AbstractActivity {

	public static final String EXTRA_ACCOUNT =
			PicasaMediaInfoActivity.class.getSimpleName() + ".EXTRA_ACCOUNT";
	public static final String EXTRA_TAG =
			PicasaMediaInfoActivity.class.getSimpleName() + ".EXTRA_TAG";
	public static final String EXTRA_ENTRY =
			PicasaMediaInfoActivity.class.getSimpleName() + ".EXTRA_ENTRY";
	
	private TableLayout mTblInfo;
	private Button mBtnOk;
	
	private PhotoEntry mEntry;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.picasa_media_info);
		
		mTblInfo = (TableLayout) findViewById(R.id.tbl_information);
		mBtnOk = (Button) findViewById(R.id.btn_ok);
		
		String entry = getIntent().getStringExtra(EXTRA_ENTRY);
		if (!TextUtils.isEmpty(entry)) {
			mEntry = JsonUtil.fromJson(entry, PhotoEntry.class);
		} else {
			String account = getIntent().getStringExtra(EXTRA_ACCOUNT);
			String tag = getIntent().getStringExtra(EXTRA_TAG);
			PicasaCache cache = new PicasaCache(this, account);
			String[] key = PicasaCache.decodeTag(tag);
//			try {//TODO
//				mEntry = cache.loadMediaEntry(key[0], key[1]);
//			} catch (IOException e) {
//				finish();
//			}
		}
		
		mBtnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		showEntry();
	}
	
	private void showEntry() {
		addRow(getString(R.string.media_info_title), mEntry.title);
		addRow(getString(R.string.media_info_summary), mEntry.summary);
		addRow(getString(R.string.media_info_tag), mEntry.mediaGroup.keywords);
		addRow(getString(R.string.media_info_width), mEntry.width);
		addRow(getString(R.string.media_info_height), mEntry.height);
		addRow(getString(R.string.media_info_size), mEntry.size);
		addRow(getString(R.string.media_info_timestamp), mEntry.timestamp);
		
		if (mEntry.mediaGroup.content.type.startsWith("image/")) {
			addRow(getString(R.string.media_info_fstop), mEntry.exifTags.fstop);
			addRow(getString(R.string.media_info_make), mEntry.exifTags.make);
			addRow(getString(R.string.media_info_model), mEntry.exifTags.model);
			addRow(getString(R.string.media_info_exposure), mEntry.exifTags.exposure);
			addRow(getString(R.string.media_info_flash), mEntry.exifTags.flash);
			addRow(getString(R.string.media_info_focallength), mEntry.exifTags.focallength);
			addRow(getString(R.string.media_info_iso), mEntry.exifTags.iso);
			addRow(getString(R.string.media_info_time), mEntry.exifTags.time);
		}
	}
	
	private TableRow addRow(String name, Object value) {
		TableRow row = (TableRow) getLayoutInflater().inflate(
				R.layout.picasa_media_info_row, mTblInfo, false);
		((TextView) row.findViewById(R.id.txt_name)).setText(name);
		((TextView) row.findViewById(R.id.txt_value)).setText(value == null ? null : value.toString());
		mTblInfo.addView(row);
		return row;
	}
}
