package jp.co.johospace.jsphoto.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StatFs;
import android.text.format.DateUtils;
import android.util.Pair;

/**
 * 入出力ユーティリティ
 */
public class IOUtil {
	private IOUtil() {}
	
	/**
	 * 入出ストリーム間でバイトをコピーします。
	 * <p>入力の終端に達するまで読みながら出力に排出します。
	 * <p>入出力ともにストリームはこの操作によって閉じられることはありません。
	 * @param src 入力
	 * @param dest 出力
	 * @return コピーされたバイト数
	 * @throws IOException 入出力例外発生時
	 */
	public static long copy(InputStream src, OutputStream dest) throws IOException {
		final byte[] buffer = new byte[2048];
		
		long total = 0L;
		int read;
		while ((read = src.read(buffer)) != -1) {
			dest.write(buffer, 0, read);
			total += read;
		}
		
		return total;
	}
	
	/**
	 * ファイルをコピーします。
	 * @param src コピー元ファイル
	 * @param dest コピー先ファイル
	 * @return コピーされたバイト数
	 * @throws IOException 入出力例外発生時
	 */
	public static long copy(File src, File dest) throws IOException {
		FileInputStream in = new FileInputStream(src);
		try {
			FileOutputStream out = new FileOutputStream(dest);
			try {
				return copy(in, out);
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
	}
	
	/**
	 * ファイルを列挙します。
	 * <p>渡されたファイルがディレクトリの場合、ディレクトリ階層を再帰的にスキャンしてファイルを列挙します。
	 * @param file ファイル
	 * @param filter フィルタ
	 * @return ファイルの列挙。
	 */
	public static List<File> listFiles(File file, FileFilter filter) {
		ArrayList<File> files = new ArrayList<File>();
		listFiles(files, file, filter);
		return files;
	}
	
	/**
	 * ファイルを列挙します。
	 * <p>渡されたファイルがディレクトリの場合、ディレクトリ階層を再帰的にスキャンしてファイルを列挙します。
	 * @param file ファイル
	 * @return ファイルの列挙。
	 */
	public static List<File> listFiles(File file) {
		return listFiles(file, ACCEPTALL);
	}
	
	private static void listFiles(List<File> files, File file, FileFilter filter) {
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				listFiles(files, child, filter);
			}
		} else {
			if (filter.accept(file)) {
				files.add(file);
			}
		}
	}
	
	private static final FileFilter ACCEPTALL = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return true;
		}
	};
	
	/**
	 * 利用中のサイズを取得します
	 * @param dir マウントポイントのディレクトリ？
	 * @return Pair<Long, Long>　firstにトータルサイズ、secondに利用可能イズ
	 */
	public static Pair<Long, Long> getMediaSize(File dir) {
		String path = dir.getPath();
		StatFs statFs = new StatFs(path);
		
		long availableBlocks = statFs.getAvailableBlocks();
		long blockCount = statFs.getBlockCount();
		long blockSize = statFs.getBlockSize();
		
		return new Pair<Long, Long>(blockCount * blockSize, availableBlocks
				* blockSize);
	}
	
	/**
	 * 入力ストリームからデータを文字列として読み込みます。
	 * @param in 入力ストリーム
	 * @param charset キャラクタセット
	 * @return 入力ストリームが供給するバイトをキャラクタセットにエンコードした文字列
	 * @throws IOException 入出力例外発生時
	 */
	public static String readString(InputStream in, Charset charset) throws IOException {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			copy(in, bytes);
			return new String(bytes.toByteArray(), charset.name());
			
		} finally {
			in.close();
		}
	}
	
	/**
	 * ファイルからデータを文字列として読み込みます。
	 * @param file ファイル
	 * @param charset キャラクタセット
	 * @return 入力ストリームが供給するバイトをキャラクタセットにエンコードした文字列
	 * @throws IOException 入出力例外発生時
	 */
	public static String readString(File file, Charset charset) throws IOException {
		return readString(new FileInputStream(file), charset);
	}
	
	/**
	 * 引数のパスから、親となるフォルダ名、またはファイル名を取得します
	 * 
	 * @param path			元となるパス
	 * @param getParent	true:親フォルダ名を取得 false:ファイル名を取得
	 * @return
	 */
	public static String getPath(String path, boolean getParent) {
		
		int index = path.lastIndexOf("/");
		
		if (getParent) {
			return path.substring(0, index);
		} else {
			return path.substring(index + 1, path.length());
		}
	}
	
	/**
	 * フォルダ名を変更します
	 * @param newName	新しいフォルダ名
	 * @param folder	変更するフォルダ
	 * @return	String 変更後のフォルダパス
	 */
	public static String changeFolderName(String newName, File folder) {
		
		String newPath = folder.getParent() + "/" + newName.trim();
		
		File newFolder = new File(newPath);
		
		// 変更後のフォルダが既に存在する場合は、処理中断
		if (newFolder.exists()) {
			return null;
		}
		
		if (folder.renameTo(newFolder)) {
			return newFolder.getAbsolutePath();
		} else {
			return null;
		}
	}
	
	/**
	 * フォルダ・ファイルを削除します
	 * 
	 * @param f 削除対象のフォルダ・ファイル
	 * @return	true:削除成功 false:削除失敗
	 */
	public static boolean deleteFile(File f) {
		
		// フォルダ・ファイルが存在しない
		if (!f.exists()) {
			return false;
		}
		
		// ファイルならば削除
		if (f.isFile()) {
			return f.delete();
		}
		
		// フォルダならば、配下のファイル・フォルダを全て削除
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			int length = files.length;
			
			// 再帰呼び出しで、フォルダ内部の削除を実行
			for (int i = 0; i < length; i++) {
				deleteFile(files[i]);
			}
			
			return f.delete();
		}
		
		return false;
	}
	
	/**
	 * ネットワーク接続が確立されていて通信できる状態かどうかを返します。
	 * @param context コンテキスト
	 * @param waitLimitSec 最大の待ち時間（秒）
	 * @param type 特定のネットワークタイプのみ調べる場合、そのタイプ。すべてのネットワークを対象とする場合null。
	 * @return 確立されている場合true
	 */
	public static boolean isNetworkConnected(Context context, int waitLimitSec, Integer type) {
		ConnectivityManager manager =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		final int wait = waitLimitSec + 1;
		for (int trial = 0; trial < wait; trial++) {
			if (0 < trial) {
//				Log.d(IOUtil.class.getSimpleName(),		/*$debug$*/
//						String.format("isNetworkConnected retry %d", trial));		/*$debug$*/
			}
			
			NetworkInfo[] infos;
			if (type == null) {
				infos = manager.getAllNetworkInfo();
			} else {
				infos = new NetworkInfo[] {manager.getNetworkInfo(type)};
			}
			
			if (infos != null) {
				for (NetworkInfo info : infos) {
					if (info != null) {
						if (info.isConnected()) {
							return true;
						}
					}
				}
			}
			
			if (trial + 1 < wait) {
				try {
					Thread.sleep(DateUtils.SECOND_IN_MILLIS);
				} catch (InterruptedException e) {
					;
				}
			}
		}
		
		return false;
	}
}
