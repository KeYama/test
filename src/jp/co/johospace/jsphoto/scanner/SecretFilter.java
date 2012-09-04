package jp.co.johospace.jsphoto.scanner;

import java.io.File;

public class SecretFilter implements JorlleMediaFilter {

	@Override
	public boolean filter(File file) {
		if(file.getName().endsWith(".secret")){
			return true;
		}
		return false;
	}

}
