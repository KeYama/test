package jp.co.johospace.jsphoto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.MediaGroup;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.PhotoEntry;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.google.api.client.http.xml.atom.AtomContent;
import com.google.api.client.xml.XmlNamespaceDictionary;

public class MockPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.mock_preferences);
		
		PhotoEntry newEntry = new PhotoEntry();
		newEntry.title = "たいじゃ";
		newEntry.mediaGroup = new MediaGroup();
		newEntry.mediaGroup.keywords = "たぐけ";
		
		XmlNamespaceDictionary DICTIONARY = new XmlNamespaceDictionary()
		.set("", "http://www.w3.org/2005/Atom")
		.set("exif", "http://schemas.google.com/photos/exif/2007")
		.set("gd", "http://schemas.google.com/g/2005")
		.set("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#")
		.set("georss", "http://www.georss.org/georss")
		.set("gml", "http://www.opengis.net/gml")
		.set("gphoto", "http://schemas.google.com/photos/2007")
		.set("media", "http://search.yahoo.com/mrss/")
		.set("openSearch", "http://a9.com/-/spec/opensearch/1.1/")
		.set("xml", "http://www.w3.org/XML/1998/namespace");
		
		AtomContent atomContent = AtomContent.forEntry(DICTIONARY, newEntry);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			atomContent.writeTo(out);
//			System.out.println(new String(out.toByteArray()));	/*$debug$*/
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
}
