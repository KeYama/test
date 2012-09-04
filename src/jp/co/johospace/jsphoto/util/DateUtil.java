package jp.co.johospace.jsphoto.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.text.format.Time;

/**
 * 日付関連ユーティリティ
 */
public class DateUtil {


	
	/**
	 * ミリ秒を元に、日付文字列を作成します
	 * 
	 * @param millis	ミリ秒
	 * @return		日付文字列
	 */
	public static String getDateString(long millis) {
		String text = null;
		
		Locale loc = Locale.getDefault();
		
		// タイムゾーンを取得
		TimeZone time = TimeZone.getDefault();
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(millis);
		
		// 日本語
		if (loc.equals(Locale.JAPAN)) {
		
			// 年月日　時分秒フォーマット
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			sdf.setTimeZone(time);
			
			text = sdf.format(new Date(millis));
		
		// それ以外の言語
		} else {
			
			/** 時分秒フォーマット */
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			sdf.setTimeZone(time);
			
			text = String.format(Locale.ENGLISH, "%1$tb", cal) + "/" + 
								   cal.get(Calendar.DAY_OF_MONTH) + "/" + 
								   cal.get(Calendar.YEAR) + " " + 
								   sdf.format(new Date(millis));
		}
		
		return text;
	}
	
	
	
	/**
	 * 年月日を元に、日付文字列を作成します
	 * 
	 * @param year		年
	 * @param month	月
	 * @param day		日
	 * @return		日付文字列
	 */
	public static String getDateString(int year, int month, int day) {
		String text = null;
		
		Locale loc = Locale.getDefault();
		
		// 日本語
		if (loc.equals(Locale.JAPAN)) {
			text = String.valueOf(year) + "/" + String.valueOf(month + 1) + "/" + String.valueOf(day);
		
		// それ以外の言語
		} else {
			Calendar cal = Calendar.getInstance();
			cal.set(year, month, day);
			text = String.format(Locale.ENGLISH, "%1$tb", cal) + "/" + 
								   cal.get(Calendar.DAY_OF_MONTH) + "/" + 
								   cal.get(Calendar.YEAR);
		}
		
		return text;
	}

	/**
	 * 書式化された日付をパースします
	 * @param date 日付
	 * @param formatIn 日付の書式 例 yyyyMMdd
	 * @param out パースした結果
	 * @throws ParseException
	 */
	public static void parse(String date, String formatIn, Time out) throws ParseException {
		int position = 0;
		try {
			position = formatIn.indexOf("yyyy");
			if (position >= 0) {
				out.year = Integer.parseInt(date.substring(position, position + 4));
			}
			
			position = formatIn.indexOf("MM");
			if (position > 0) {
				out.month = Integer.parseInt(date.substring(position, position + 2)) - 1;
			}
			
			position = formatIn.indexOf("dd");
			if (position > 0) {
				out.monthDay = Integer.parseInt(date.substring(position, position + 2));
			}
		} catch (NumberFormatException e) {
			throw new ParseException("format is miss match", position);
		}
	}
	
}
