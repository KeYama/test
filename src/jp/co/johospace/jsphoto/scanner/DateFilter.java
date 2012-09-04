package jp.co.johospace.jsphoto.scanner;

import java.io.File;
import java.util.Calendar;

public class DateFilter implements JorlleMediaFilter {
	private Long mBegin;
	private Long mEnd;
	
	public DateFilter(){
		
	}
	
	public void setBegin(Calendar cal){
		mBegin = cal.getTimeInMillis();
	}
	
	public void setEnd(Calendar cal){
		cal.add(Calendar.DAY_OF_MONTH, 1);
		cal.add(Calendar.SECOND, -1);
		mEnd = cal.getTimeInMillis();
	}

	@Override
	public boolean filter(File file) {
		
		long lastModified = file.lastModified();
		

		
		if(mBegin != null){
			if(lastModified < mBegin) return false;
		}
		if(mEnd != null){
			if(mEnd <= lastModified) return false;
		}
		
		return true;
	}

}
