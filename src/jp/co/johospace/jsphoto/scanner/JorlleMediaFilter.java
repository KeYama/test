package jp.co.johospace.jsphoto.scanner;

import java.io.File;

public interface JorlleMediaFilter {
	/**
	 * accept => true, reject => false
	 * 
	 * @param file
	 * @return
	 */
	public boolean filter(File file);
}
