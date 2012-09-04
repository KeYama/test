package jp.co.johospace.jsphoto.callback;

import android.widget.DatePicker;

/**
 * 日付ダイアログ　コールバック用インターフェース
 */
public interface IDateSet {
	void onDateSet(DatePicker view, Integer year, Integer monthOfYear, Integer dayOfMonth);
}
