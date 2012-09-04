package jp.co.johospace.jsphoto.grid;

import java.io.File;

import jp.co.johospace.jsphoto.define.ApplicationDefine;
import jp.co.johospace.jsphoto.util.MediaUtil;
import android.graphics.Bitmap;
public class ExtUtil {
	private static final String[] EXT = 
		{".jpg", ".jpeg", ".3gp", ".mp4", ".m4a", ".gif", ".png", ".bmp" };
	private static String[] EXT_ALL;
	private static final String[] EXT_VIDEO = 
		{".3gp", ".mp4", ".m4a"};
	private static String[] EXT_VIDEO_ALL;
	
	static {
		EXT_ALL = new String[EXT.length*2];
		for(int n = 0; n < EXT.length; n++){
			EXT_ALL[n*2] = EXT[n];
			EXT_ALL[n*2+1] = EXT[n] + ".secret";
		}
		
		EXT_VIDEO_ALL = new String[EXT_VIDEO.length*2];
		for(int n = 0; n < EXT_VIDEO.length; n++){
			EXT_VIDEO_ALL[n*2] = EXT_VIDEO[n];
			EXT_VIDEO_ALL[n*2+1] = EXT_VIDEO[n] + ".secret";
		}
	}
	
	public static final String EXT_PNG = ".png";	
	public static final String EXT_JPG = ".jpg";
	public static final String EXT_JPEG = ".jpeg";
	
	
	public static boolean isVideo(String path){
//		String target = getExt(new File(path));
//		for(String ext: EXT_VIDEO_ALL){
//			if(target.endsWith(ext))return true;
//		}
//		return false;
		String mime = MediaUtil.getMimeTypeFromPath(path);
		return mime != null && mime.startsWith("video/");
	}
	
	/**
	 * 
	 * 拡張子を除いた純粋なファイル名にサフィックスを付加する(シークレット対応）
	 * 
	 * @param target
	 * @param suffix
	 * @return
	 */
	public static File appendPureName(File target, String suffix){
		String targetName = target.getName().toLowerCase();
		for(String ext: EXT_ALL){
			if(targetName.endsWith(ext)){
				String pureName = targetName.substring(0, targetName.length() - ext.length());
				String newName = pureName + suffix;
				return new File(target.getParent(), newName + ext);
			}
		}
		return new File(target.getAbsolutePath() + suffix);
	}
	
	
	/**
	 * 
	 * filename-suffix-xxx.ext 形式のあいているファイルパスを作成する(シークレット対応)
	 * 
	 * @param target
	 * @param suffix
	 * @return
	 */
	public static File createEmptyPath(File target, String suffix){
		File toSave = ExtUtil.appendPureName(target, suffix);
		
		for(int n = 0; toSave.exists(); ++n){
			toSave = ExtUtil.appendPureName(target, suffix + n);
		}
		
		return toSave;
	}
	
	/**
	 * 
	 * filename-suffix-xxx.ext 形式のあいているファイルパスを作成する(シークレット対応)
	 * 同ファイル名、シークレット拡張子のファイルも検査対象とする
	 * 
	 * @param target
	 * @param suffix
	 * @return
	 */
	public static File createEmptyPathConsideringSecret(File target, String suffix){
		File toSave = ExtUtil.appendPureName(target, suffix);
		
		File subCheckFile;
		
		// 通常・シークレットの両ファイル名でチェック
		if (ExtUtil.isSecret(toSave)) {
			subCheckFile = ExtUtil.unSecret(toSave);
		} else {
			subCheckFile = ExtUtil.toSecret(toSave);
		}
		
		for(int n = 0; (toSave.exists() || subCheckFile.exists()); ++n){
			toSave = ExtUtil.appendPureName(target, suffix + n);
			
			if (ExtUtil.isSecret(toSave)) {
				subCheckFile = ExtUtil.unSecret(toSave);
			} else {
				subCheckFile = ExtUtil.toSecret(toSave);
			}
		}
		
		return toSave;
	}
	
	
	/**
	 * 
	 * 拡張子を得る（シークレットの場合はシークレットを除去）
	 * 
	 * @param target
	 * @return
	 */
	public static final String getExt(File target){
		String name = target.getName().toLowerCase();
		for(int n = 0; n < EXT_ALL.length; ++n){
			if(name.endsWith(EXT_ALL[n])){
				if(n % 2 == 0){
					return EXT_ALL[n];
				}else{
					return EXT_ALL[n-1];
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * 拡張子を得る（シークレット除去なし）
	 * 
	 * @param target
	 * @return
	 */
	public static final String getExtWithSecret(File target){
		String name = target.getName().toLowerCase();
		for(int n = 0; n < EXT_ALL.length; ++n){
			
			if(name.endsWith(EXT_ALL[n])){
				return EXT_ALL[n];
			}
		}
		return null;
	}
	
	/**
	 * 拡張子を除いた純粋な名前を得る
	 * 
	 * @param target
	 * @return
	 */
	public static final String getPureName(File target){
		String name = target.getName().toLowerCase();
		for(int n = 0; n < EXT_ALL.length; ++n){
			if(name.endsWith(EXT_ALL[n])){
				return name.substring(0, name.length() - EXT_ALL[n].length());
			}
		}
		return null;		
	}
	
	
	
	
	/**
	 * 
	 * 拡張子から圧縮形式を得る(シークレット対応)
	 * 
	 * @param target
	 * @return
	 */
	public static Bitmap.CompressFormat getFormat(File target){
		String ext = getExt(target);
		
		if(EXT_PNG.equals(ext)){
			return Bitmap.CompressFormat.PNG;
		}else if(EXT_JPG.equals(ext) || EXT_JPEG.equals(ext)){
			return Bitmap.CompressFormat.JPEG;
		}
		return null;
	}
	
	/**
	 * 
	 * シークレットファイルのパスにする
	 * 
	 * @param target
	 */
	public static File toSecret(File target){
		String path = target.getAbsolutePath();
		if(path.toLowerCase().endsWith(ApplicationDefine.SECRET)) return target;
		return new File(path + ApplicationDefine.SECRET);
	}
	
	/**
	 * 
	 * シークレットファイルではないパスにする
	 * 
	 * @param target
	 */
	public static File unSecret(File target){
		String path = target.getAbsolutePath();
		if(!path.toLowerCase().endsWith(ApplicationDefine.SECRET)) return target;
		path = path.substring(0, path.length() - ApplicationDefine.SECRET.length());
		return new File(path);
	}
	
	
	public static boolean isSecret(File target){
		return target.getName().endsWith(ApplicationDefine.SECRET);
	}
	
	/**
	 * 
	 * MIMEタイプをえる
	 * 
	 * @param target
	 * @return
	 */
	public static String getMimeType(String target){
//		if(target.endsWith(EXT_JPEG) || target.endsWith(EXT_JPG)){
//			return "image/jpeg";
//		}else if(target.endsWith(EXT_PNG)){
//			return "image/png";
//		}
//		else return "image/*";
		return MediaUtil.getMimeTypeFromPath(target);
	}
}
