package jp.co.johospace.jsphoto.dialog;

import java.text.SimpleDateFormat;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Schedule;
import android.app.Dialog;
import android.content.Context;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class DetailDialog extends Dialog implements android.view.View.OnClickListener {
	
	private Schedule mDetail;
	
	public DetailDialog(Context context, Schedule detail) {
		super(context);
		
		mDetail = detail;
		
		init(context);
	}

	private void init(Context context) {
		
		LayoutInflater inflater = LayoutInflater.from(context);
		
		// 詳細セット
		View lyt = inflater.inflate(R.layout.detail, null);
		((TextView)lyt.findViewById(R.id.txtTitle)).setText(mDetail.title);
		((TextView)lyt.findViewById(R.id.txtContent)).setText(mDetail.detail);
		((TextView)lyt.findViewById(R.id.txtPlace)).setText(mDetail.location);
		Button btn = (Button)lyt.findViewById(R.id.btnEnd);
		btn.setOnClickListener(this);
		
		// サービスのアイコン
		ImageView img = (ImageView) lyt.findViewById(R.id.imgTitle);
		img.setImageResource(ClientManager.getIconResource(getContext(), mDetail.service));
		
		/*
		if (sche.rrule != null) {
			EventRecurrence rec = new EventRecurrence();
			rec.parse(sche.rrule);
			((TextView)lyt.findViewById(R.id.txtRepeat)).setText(FormatUtil.toRepeatString(context, rec));
		}
		{// 状態
			String status = sche.imp == 1 ? context.getString(R.string.important) : "";
			
			if (status.length() > 0 && sche.compl == 1) {
				status += ", ";
				status += context.getString(R.string.completed);
			} else if (sche.compl == 1){
				status += context.getString(R.string.completed);
			}
			
			if (status.length() > 0 && sche.isHoliday == 1) {
				status += ", ";
				status += context.getString(R.string.holiday);
			} else if (sche.isHoliday == 1){
				status += context.getString(R.string.holiday);
			}
			
			((TextView)lyt.findViewById(R.id.txtStatus)).setText(status);
		}
		*/
		{// 開始・終了日時
			String fromFormat = "yyyyMMddHHmmssSSS";
			String toFormat = context.getString(R.string.detail_datetime_format);
			SimpleDateFormat sdf = new SimpleDateFormat(fromFormat);
			
			Time st = new Time();
			st.set(mDetail.dtstart);
			Time en = new Time();
			en.set(mDetail.dtend);
			if(st.hour==0 && st.minute==0 && en.hour==23 && en.minute==59) {
				toFormat = context.getString(R.string.detail_date_format);
			}
			
			try {// 開始
				if (mDetail.dtstart != null) {
					long startMillis = mDetail.dtstart;
					sdf.applyPattern(toFormat);
					String startText = sdf.format(startMillis);
					((TextView)lyt.findViewById(R.id.txtStartTime)).setText(startText);
				}
			} catch (Exception e){
//				e.printStackTrace();		/*$debug$*/
			}
			
			sdf.applyPattern(fromFormat);
			
			try {// 終了
				if (mDetail.dtend != null) {
					long endMillis = mDetail.dtend;
					sdf.applyPattern(toFormat);
					String endText = sdf.format(endMillis);
					((TextView)lyt.findViewById(R.id.txtEndTime)).setText(endText);
				}
			} catch (Exception e) {
//				e.printStackTrace();		/*$debug$*/
			}
		}
		// 表示
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(lyt);
	}

	@Override
	public void onClick(View v) {
		
		// 終了
		if (v.getId() == R.id.btnEnd) {
			dismiss();
		}
		
	}
}
