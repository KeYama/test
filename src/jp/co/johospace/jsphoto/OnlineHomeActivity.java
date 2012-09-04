package jp.co.johospace.jsphoto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jp.co.johospace.jsphoto.managed.NavigatableActivity;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ExternalServiceClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import android.os.Bundle;
import android.widget.TextView;

/**
 * オンラインホーム
 */
public class OnlineHomeActivity extends NavigatableActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TextView v = new TextView(this);
		v.setText(getClass().getName());
		setContentView(v);
	}
	
	private class BaseEntry {
		ExternalServiceClient client;
		int icon;
		String serviceName;
		long lastUpdated;
		int fileCount;
		
		InputStream loadThumbnail(Media media) throws IOException {
			return client.getThumbnail(media, 100);
		}
	}
	
	private class AlbumEntry extends BaseEntry {
		List<Directory> albums;
	}
	
	private class FlatEntry extends BaseEntry {
		List<Media> medias;
	}
}
