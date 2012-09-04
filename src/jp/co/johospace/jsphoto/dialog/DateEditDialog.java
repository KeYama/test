package jp.co.johospace.jsphoto.dialog;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.callback.IDateSet;
import jp.co.johospace.jsphoto.define.ApplicationDefine;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;

/**
 * 日付設定ダイアログです
 */
public class DateEditDialog extends AlertDialog implements OnClickListener, OnDateChangedListener{

	/** コールバック用インターフェース */
	IDateSet mDateSet;
	
	/** 年 */
	Integer mYear;
	/** 月 */
	Integer mMonth;
	/** 日 */
	Integer mDay;

	/** 日付設定 */
	DatePicker mDatePicker;
	
	/**
	 * コンストラクタ
	 * 
	 * @param context	コンテキスト
	 * @param dateSet	コールバックインターフェース
	 * @param year		年
	 * @param month	月
	 * @param day		日
	 */
	public DateEditDialog(Context context, IDateSet dateSet, int year, int month, int day) {
		super(context);
		
		mDateSet = dateSet;
		mYear = year;
		mMonth = month;
		mDay = day;
		
		init(context);
	}

	/**
	 * 初期化処理
	 * 
	 * @param context コンテキスト
	 */
	public void init(Context context) {
		
		// レイアウト設定
        LayoutInflater inflater = 
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.date_picker_dialog, null);
        setView(view);
        
		mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);
		mDatePicker.init(mYear, mMonth, mDay, this);
		
		
		setButton(getContext().getResources().getString(R.string.filtering_button_date_setting), this);
		setButton(getContext().getResources().getString(android.R.string.cancel), this);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		
		// 設定した値をコールバックする
        if (mDateSet != null) {
        	
        	mDatePicker.clearFocus();
        	
        	switch (which) {
        	// 設定
        	case ApplicationDefine.INDEX_DATE_SETTING: 	
	        	mDateSet.onDateSet(mDatePicker, mDatePicker.getYear(), 
	                    mDatePicker.getMonth(), mDatePicker.getDayOfMonth());
	        	break;
	        	
	        // キャンセル
        	case ApplicationDefine.INDEX_DATE_CANCEL:
	        	mDateSet.onDateSet(mDatePicker, null, null, null);
	        	break;
        	} 
        }
	}

	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
	}
}
