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

package jp.co.johospace.jsphoto.onlineservice.picasa.api;

import java.net.URLEncoder;

import android.text.TextUtils;

import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.util.Key;

public class PicasaUrl extends GoogleUrl {

	/** Whether to pretty print HTTP requests and responses. */
	private static final boolean PRETTY_PRINT = false;

	public static final String ROOT_URL = "https://picasaweb.google.com/data/";

	@Key("max-results")
	public Integer maxResults;

	@Key
	public String kinds;
	
	@Key("q")
	public String query;
	
	@Key
	public String imgmax;
	
	@Key
	public String thumbsize;

	public PicasaUrl(String url) {
		super(url);
		prettyprint = PRETTY_PRINT;
	}

	/**
	 * Constructs a new Picasa Web Albums URL based on the given relative path.
	 * 
	 * @param relativePath
	 *            encoded path relative to the {@link #ROOT_URL}
	 * @return new Picasa URL
	 */
	public static PicasaUrl relativeToRoot(String relativePath) {
		return new PicasaUrl(ROOT_URL + relativePath);
	}
	
	/**
	 * ユーザベースのFeed URLを生成します。
	 * @param userID ユーザID
	 * @return URL
	 */
	public static PicasaUrl feedBasedUser(String userID) {
		return new PicasaUrl(ROOT_URL
				+ join("/", "feed", "api", "user", userID));
	}
	
	/**
	 * コンタクトベースのFeed URLを生成します。
	 * @param userID ユーザID
	 * @return URL
	 */
	public static PicasaUrl feedBasedContacts(String userID) {
		return new PicasaUrl(ROOT_URL
				+ join("/", "feed", "api", "user", userID, "contacts"));
	}
	
	/**
	 * アルバムベースのFeed URLを生成します。
	 * @param userID ユーザID
	 * @param albumID アルバムID
	 * @return URL
	 */
	public static PicasaUrl feedBasedAlbum(String userID, String albumID) {
		String url = ROOT_URL
				+ join("/", "feed", "api", "user", userID, "albumid", albumID);
		return new PicasaUrl(url);
	}
	
	/**
	 * フォトベースのFeed URLを生成します。
	 * @param userID ユーザID
	 * @param albumID アルバムID
	 * @param photoID フォトID
	 * @return URL
	 */
	public static PicasaUrl feedBasedPhoto(String userID, String albumID, String photoID) {
		return new PicasaUrl(ROOT_URL
				+ join("/", "feed", "api", "user", userID, "albumid", albumID, "photoid", photoID));
	}
	
	/**
	 * コミュニティ検索のFeed URLを生成します。
	 * @param query 検索語句
	 * @return URL
	 */
	public static PicasaUrl feedOfCommunitySearch(String query) {
		PicasaUrl url = new PicasaUrl(ROOT_URL
				+ join("/", "feed", "api", "all"));
		url.kinds = "photo";
		url.query = URLEncoder.encode(query);
		return url;
	}
	
	/**
	 * おすすめフォトのFeed URLを生成します。
	 * @return URL
	 */
	public static PicasaUrl feedOfFeaturedPhotos() {
		return new PicasaUrl(ROOT_URL
				+ join("/", "feed", "api", "featured"));
	}
	
	
	/**
	 * アルバムエントリのURLを生成します。
	 * @param userID ユーザID
	 * @param albumID アルバムID
	 * @return URL
	 */
	public static PicasaUrl entryOfAlbum(String userID, String albumID) {
		return new PicasaUrl(ROOT_URL
				+ join("/", "entry", "api", "user", userID, "albumid", albumID));
	}
	
	/**
	 * フォトエントリのURLを生成します。
	 * @param userID ユーザID
	 * @param albumID アルバムID
	 * @param photoID フォトID
	 * @return URL
	 */
	public static PicasaUrl entryOfPhoto(String userID, String albumID, String photoID) {
		return new PicasaUrl(ROOT_URL
				+ join("/", "entry", "api", "user", userID, "albumid", albumID, "photoid", photoID));
	}
	
	private static String join(CharSequence delimiter, Object... tokens) {
		return TextUtils.join(delimiter, tokens);
	}
}
