/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package jp.co.johospace.jsphoto.onlineservice.picasa.api.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TimeZone;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.onlineservice.Media;
import jp.co.johospace.jsphoto.onlineservice.MediaMetadata;
import jp.co.johospace.jsphoto.onlineservice.MediaSync;
import jp.co.johospace.jsphoto.util.JsonUtil;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.google.api.client.util.Key;

public class PhotoEntry extends Entry implements Media {
	protected final String tag = getClass().getSimpleName();

	@Key
	public Category category = Category.newKind("photo");

	@Key
	public String id;
	
	@Key("gphoto:id")
	public String gphotoId;
	
	@Key("gphoto:albumid")
	public String gphotoAlbumId;
	
	@Key("gphoto:timestamp")
	public String timestamp;
	
	@Key("media:group")
	public MediaGroup mediaGroup;
	
	@Key("exif:tags")
	public ExifTags exifTags;
	
	@Key("gphoto:width")
	public int width;
	
	@Key("gphoto:height")
	public int height;
	
	@Key("gphoto:size")
	public long size;

	@Override
	public String getMediaID() {
		return id;
	}

	@Override
	public String getTitle() {
		return title;
	}
	
	public String getEditMediaLink() {
		return Link.find(links, "edit-media");
	}

	@Override
	public MediaSync toMediaSync() {
		MediaSync sync = new MediaSync();
		sync.mediaID = getMediaID();
		sync.directoryID = gphotoAlbumId;
		String uri = getSelfLink();
		if (!TextUtils.isEmpty(uri)) {
			int index = uri.lastIndexOf('?');
			if (0 <= index) {
				uri = uri.substring(0, index);
			}
		}
		sync.mediaURI = uri;
		sync.remoteVersion = etag;
		
		sync.syncData1 = gphotoId;
		sync.syncData5 = JsonUtil.toJson(this);
		if (TextUtils.isEmpty(timestamp)) {
			sync.productionDate = null;
		} else {
			// FIXME 暫定対応 - Picasaを通過したexifの撮影日がずれる。どうすればよいか。
			sync.productionDate = Long.valueOf(timestamp);
			if (!Build.MODEL.equals("Galaxy Nexus")) {
				sync.productionDate -= TimeZone.getDefault().getRawOffset();
			}
		}
		return sync;
	}

	@Override
	public boolean isDirtyRemotely(MediaSync sync) {
		boolean dirty = !etag.equals(sync.remoteVersion);
		d("given etag(%s) vs my etag(%s) : dirtyRemotely=%s", sync.remoteVersion, etag, dirty);
		return dirty;
	}

	@Override
	public boolean isNewerEqual(long millis) {
		Time time = new Time();
		time.parse3339(updated);
		long mine = time.toMillis(false);
		boolean newerEqual = millis <= mine;
		d("others(%d) vs mine(%d) -> i am newer equal: %s", millis, mine, newerEqual);
		return newerEqual;
	}
	
	@Override
	public Collection<MediaMetadata> getMetadata() throws IOException {
		ArrayList<MediaMetadata> tags = new ArrayList<MediaMetadata>();
		if (mediaGroup != null && !TextUtils.isEmpty(mediaGroup.keywords)) {
			String[] keywords = mediaGroup.keywords.split("\\s*,");
			for (String keyword : keywords) {
				MediaMetadata metadata = new MediaMetadata();
				metadata.type = CMediaMetadata.TYPE_TAG;
				metadata.value = keyword;
				tags.add(metadata);
			}
		}
		return tags;
	}
	
	protected void d(String format, Object... args) {		/*$debug$*/
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			Log.d(tag, String.format(format, args));		/*$debug$*/
		}		/*$debug$*/
	}		/*$debug$*/
	
	protected void d(String format, Throwable t, Object... args) {		/*$debug$*/
		if (JorlleApplication.debuggable()) {		/*$debug$*/
			Log.d(tag, String.format(format, args), t);		/*$debug$*/
		}		/*$debug$*/
	}		/*$debug$*/
}
