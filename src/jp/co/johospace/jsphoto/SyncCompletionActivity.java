package jp.co.johospace.jsphoto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class SyncCompletionActivity extends AbstractActivity implements OnClickListener {


	/** カメラ */
	private TextView mCamera;
	/** カメラ起動コード */
	private int CAMERA_CODE = 2;
	/** インスタンスURI */
	private Uri mImageUri;
	/** 閉じるボタン */
	private Button mClose;

    final static String TAG = "Jorlle";
    final static String APPLICATION_NAME = "JorlleFolder";
    final static Uri IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    final static String PATH = Environment.getExternalStorageDirectory().toString() + "/" + APPLICATION_NAME;

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// ヘッダなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sync_comp);

		mCamera = (TextView)findViewById(R.id.camera);
		mCamera.setOnClickListener(this);

		mClose = (Button)findViewById(R.id.btnClose);
		mClose.setOnClickListener(this);

		// テキストリンクを追加
		TextView txtSendto = (TextView)findViewById(R.id.sendto);
		MovementMethod movementMethod = LinkMovementMethod.getInstance();
		txtSendto.setMovementMethod(movementMethod);
		CharSequence charSequence = Html.fromHtml(getHtmlImageUrl());
		txtSendto.setText(charSequence);           // HTMLとしてテキストをセット

	}

	/**
	 *  画像が保管されているURLを1
	 */
	private String getHtmlImageUrl(){
		String html = "";

		// リンクを作成
		String urlString = getResources().getString(R.string.online_message_synccomp_url);               // URL
//		String addressString = getResources().getString(R.string.online_message_synccomp_address);       // メール送り先アドレス
		// メール送り先アドレスを取得(自分のアカウント)
//		ArrayList<String> accountName = new ArrayList<String>();
//		Account[] accounts = AccountManager.get(this).getAccounts();
//		for (Account account : accounts) {
//			if (account.type.equals("com.google")) {
//				accountName.add(account.name);
//			}
//		}
		
		String subjectString = getResources().getString(R.string.online_message_synccomp_url);       // メール件名
		String bodyString = getResources().getString(R.string.online_message_synccomp_body_pre)
								+ urlString
								+ getResources().getString(R.string.online_message_synccomp_body_suf);   // メール本文
		String linkString = "mailto:"
//								+ accountName
								+ ""
								+ "?subject="
								+ ""
								+ "&body="
								+ bodyString;                                                            // リンク

		// テキストを作成
		String msgPrefix = getResources().getString(R.string.online_message_synccomp_msg2link_pre);
		String msgSuffix = getResources().getString(R.string.online_message_synccomp_msg2link_suf);
		String msgLink = getResources().getString(R.string.online_message_synccomp_msg2link);
		html = msgPrefix
					+ "<a href=\"" + linkString + "\">"
					+ msgLink
					+ "</a>"
					+ msgSuffix;                                                                         // HTMLテキスト

		return html;
	}

	@Override
	public void onClick(View v) {

		if (v.getId() == R.id.btnClose) {
			finish();
		}

		// リンク押下時にカメラ起動 TODO
//		if (v.getId() == R.id.camera) {
//
//		    String filename = System.currentTimeMillis() + ".jpg";
//
//		    ContentValues values = new ContentValues();
//		    values.put(MediaStore.Images.Media.TITLE, filename);
//		    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
//
//		    mImageUri = getContentResolver().insert(
//		            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//
//		    Intent intent = new Intent();
//		    intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
//		    intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
//		    startActivityForResult(intent, CAMERA_CODE);
//
//		}

	}

	/*
	 * 撮影した結果のデータを受け取り、指定したパスへ保存し直す
	 *
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == CAMERA_CODE) {
			// URIからBitmapデータを取り出す
	        Bundle bundle = data.getExtras();
	        Bitmap bm = (Bitmap)bundle.getParcelable("data");

	        ContentResolver cr = getContentResolver();
	        addImageAsApplication(cr, bm);

	    }
	}

	/**
	 * ファイル名を作成します
	 * @param dateTaken
	 * @return
	 */
	private static String createName(long dateTaken) {
        return DateFormat.format("yyyy-MM-dd_kk.mm.ss", dateTaken).toString();
	}


	/**
	 * カメラ画像を指定したパスへ保存します
	 * @param cr
	 * @param bitmap
	 * @return
	 */
	public static Uri addImageAsApplication(ContentResolver cr, Bitmap bitmap) {
	    long dateTaken = System.currentTimeMillis();
	    String name = createName(dateTaken) + ".jpg";
	    return addImageAsApplication(cr, name, dateTaken, PATH, name, bitmap, null);
	}



	/**
	 * カメラ画像を指定したパスへ保存します
	 * @param cr
	 * @param name
	 * @param dateTaken
	 * @param directory
	 * @param filename
	 * @param source
	 * @param jpegData
	 * @return
	 */
	public static Uri addImageAsApplication(ContentResolver cr, String name,
	        long dateTaken, String directory,
	        String filename, Bitmap source, byte[] jpegData) {

	    OutputStream outputStream = null;
	    String filePath = directory + "/" + filename;
	    try {
	        File dir = new File(directory);
	        if (!dir.exists()) {
	            dir.mkdirs();
//	            Log.d(TAG, dir.toString() + " create");		/*$debug$*/
	        }
	        File file = new File(directory, filename);
	        if (file.createNewFile()) {
	            outputStream = new FileOutputStream(file);
	            if (source != null) {
	                source.compress(CompressFormat.JPEG, 75, outputStream);
	            } else {
	                outputStream.write(jpegData);
	            }
	        }

	    } catch (FileNotFoundException ex) {
//	        Log.w(TAG, ex);		/*$debug$*/
	        return null;
	    } catch (IOException ex) {
//	        Log.w(TAG, ex);		/*$debug$*/
	        return null;
	    } finally {
	        if (outputStream != null) {
	            try {
	                outputStream.close();
	            } catch (Throwable t) {
	            }
	        }
	    }

	    ContentValues values = new ContentValues(7);
	    values.put(Images.Media.TITLE, name);
	    values.put(Images.Media.DISPLAY_NAME, filename);
	    values.put(Images.Media.DATE_TAKEN, dateTaken);
	    values.put(Images.Media.MIME_TYPE, "image/jpeg");
	    values.put(Images.Media.DATA, filePath);
	    return cr.insert(IMAGE_URI, values);
	}
}
