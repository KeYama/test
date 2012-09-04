package jp.co.johospace.jsphoto.onlineservice.picasa.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import jp.co.johospace.jsphoto.JorlleApplication;
import jp.co.johospace.jsphoto.database.CMediaMetadata;
import jp.co.johospace.jsphoto.onlineservice.Media;
import jp.co.johospace.jsphoto.onlineservice.MediaMetadata;
import jp.co.johospace.jsphoto.onlineservice.MediaSync;
import jp.co.johospace.jsphoto.onlineservice.OnlineMediaServiceClient;
import jp.co.johospace.jsphoto.onlineservice.gdata.api.GDataXmlClient;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.AlbumEntry;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.AlbumFeed;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.Entry;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.Feed;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.LiteAlbumFeed;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.LitePhotoEntry;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.MediaGroup;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.PhotoEntry;
import jp.co.johospace.jsphoto.onlineservice.picasa.api.model.UserFeed;
import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.util.MediaUtil;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.MultipartRelatedContent;
import com.google.api.client.http.xml.atom.AtomContent;
import com.google.api.client.xml.XmlNamespaceDictionary;

/**
 * Picasaクライアント
 */
public class PicasaClient
		implements OnlineMediaServiceClient<AlbumEntry, PhotoEntry>, HttpRequestInitializer, HttpExecuteInterceptor {
	private static final String tag = PicasaClient.class.getSimpleName();
	
	/** サポートされる画像タイプ */
	private static final Set<String> sSupportedImageTypes =
			Collections.unmodifiableSet(new TreeSet<String>(
					Arrays.asList("image/bmp", "image/gif", "image/jpeg", "image/png")));
	
	/** サポートされる動画タイプ */
	private static final Set<String> sSupportedVideoTypes =
			Collections.unmodifiableSet(new TreeSet<String>(
					Arrays.asList("video/3gpp", "video/avi", "video/quicktime",
							"video/mp4", "video/mpeg", "video/mpeg4",
							"video/msvideo", "video/x-ms-asf", "video/x-ms-wmv", "video/x-msvideo")));
	
	/** コンテキスト */
	private final Context mContext;
	/** アカウント */
	private final String mAccount;
	/** トークン */
	private String mAuthToken;
	/** HTTPトランスポート */
	private final HttpTransport transport = AndroidHttp.newCompatibleTransport();
	/** 名前空間 */
	private static final XmlNamespaceDictionary DICTIONARY = new XmlNamespaceDictionary()
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
	/** 内部クライアント */
	private final InternalPicasaClient mClient =
			new InternalPicasaClient(transport.createRequestFactory(this));;

	private class InternalPicasaClient extends GDataXmlClient {

		public InternalPicasaClient(HttpRequestFactory requestFactory) {
			super("2", requestFactory, DICTIONARY);
		}
		
		@Override
		protected HttpResponse execute(HttpRequest request) throws IOException {
			try {
				return super.execute(request);
			} catch (HttpResponseException e) {
				d("try reauthorize.", e);
				if (e.getResponse().getStatusCode() == HttpStatusCodes.STATUS_CODE_FORBIDDEN
						|| e.getResponse().getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
					if (reauthorize(true)) {
						d("reauthorized.");
						return super.execute(request);
					} else {
						throw e;
					}
				} else {
					throw e;
				}
			}
		}

		public void executeDelete(Entry entry) throws IOException {
			PicasaUrl url = new PicasaUrl(entry.getEditLink());
			super.executeDelete(url, entry.etag);
		}

		<T> T executeGet(PicasaUrl url, Class<T> parseAsType)
				throws IOException {
			return super.executeGet(url, parseAsType);
		}

		<T> T executePost(PicasaUrl url, T content) throws IOException {
			return super.executePost(url, content instanceof Feed, content);
		}

		public <T extends Entry> T executeInsert(PicasaUrl url, T entry)
				throws IOException {
			return executePost(url, entry);
		}

		public PhotoEntry executeInsertPhotoEntryWithMetadata(PhotoEntry photo,
				PicasaUrl albumFeedUrl, AbstractInputStreamContent content)
				throws IOException {
			HttpRequest request = getRequestFactory().buildPostRequest(
					albumFeedUrl, null);
			AtomContent atomContent = AtomContent.forEntry(DICTIONARY, photo);
			new MultipartRelatedContent(atomContent, content)
					.forRequest(request);
			return execute(request).parseAs(PhotoEntry.class);
		}

		public PhotoEntry executeUpdatePhotoEntryWithMetadata(PhotoEntry photo,
				AbstractInputStreamContent content) throws IOException {
			/* 本当はPATCHでPartialUpdateしたいが、受け付けてくれない */
			HttpRequest request = getRequestFactory().buildPutRequest(
					new GenericUrl(photo.getEditMediaLink()), null);
			AtomContent atomContent = AtomContent.forEntry(DICTIONARY, photo);
			new MultipartRelatedContent(atomContent, content)
					.forRequest(request);
			request.getHeaders().setIfMatch("*");
			request.getHeaders().setETag(null);
			return execute(request).parseAs(PhotoEntry.class);
		}

		public InputStream openContent(GenericUrl url) throws IOException {
			HttpRequest request = getRequestFactory().buildGetRequest(url);
			HttpResponse response = execute(request);
			return response.getContent();
		}
	}

	public PicasaClient(Context context, String account) {
		super();
		mContext = context;
		mAccount = account;
		reauthorize(false);
	}
	
	final Handler mHandler = new Handler(Looper.getMainLooper());
	
	@Override
	public void authorize(final AuthorizationHandler authHandler, final boolean expired) {
		new Thread(new Runnable() {
			final AccountManager manager = AccountManager.get(mContext);
			final Account account = new Account(getServiceAccount(), "com.google");
			@Override
			public void run() {
				try {
					// アカウントマネージャ経由でトークンを取得
					final Bundle result = manager.getAuthToken(account,
							"lh2", false, null, null).getResult();
					
					if (result.containsKey(AccountManager.KEY_INTENT)) {
						// アカウントマネージャがユーザとの対話を求めている
						final Intent intent = result.getParcelable(AccountManager.KEY_INTENT);
						int flags = intent.getFlags();
						flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
						intent.setFlags(flags);
						
						// 対話を開始する
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								authHandler.startInteraction(intent, new InteractionCallback() {
									@Override
									public void onInteractionResult(int resultCode, Intent data) {
										if (resultCode == Activity.RESULT_OK) {
											authorize(authHandler, false);
										} else {
											mHandler.post(new Runnable() {
												@Override
												public void run() {
													authHandler.authorizationFinished(mAccount, false);
												}
											});
										}
									}
								});
							}
						});
					} else if (result.containsKey(AccountManager.KEY_AUTHTOKEN)) {
						// トークンが取れた
						mAuthToken = result.getString(AccountManager.KEY_AUTHTOKEN);
						if (expired) {
							manager.invalidateAuthToken("com.google", mAuthToken);
							authorize(authHandler, false);
							return;
						}
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								authHandler.authorizationFinished(mAccount, true);
							}
						});
						
					} else {
						// とれない
//						Log.e(tag, result.getString(AccountManager.KEY_AUTH_FAILED_MESSAGE));		/*$debug$*/
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								authHandler.authorizationFinished(mAccount, false);
							}
						});
					}
				} catch (Exception e) {
//					Log.e(tag, "authorization failed.", e);		/*$debug$*/
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							authHandler.authorizationFinished(mAccount, false);
						}
					});
				}
			}
		}).start();
	}
	
	public boolean reauthorize(boolean invalidate) {
		try {
			AccountManager manager = AccountManager.get(mContext);
			Account account = new Account(getServiceAccount(), "com.google");
			Bundle result = manager.getAuthToken(account,
					"lh2", false, null, null).getResult();
			if (result.containsKey(AccountManager.KEY_AUTHTOKEN)) {
				mAuthToken = result.getString(AccountManager.KEY_AUTHTOKEN);
				if (invalidate) {
					manager.invalidateAuthToken("com.google", mAuthToken);
					result = manager.getAuthToken(account,
							"lh2", true, null, null).getResult();
					if (result.containsKey(AccountManager.KEY_AUTHTOKEN)) {
						mAuthToken = result.getString(AccountManager.KEY_AUTHTOKEN);
						return true;
					} else {
						return false;
					}
				} else {
					return true;
				}
			} else {
				return false;
			}
		} catch (Exception e) {
//			Log.e(tag, "authorization failed.", e);		/*$debug$*/
			return false;
		}
	}

	public static final String SERVICE_TYPE = "picasaweb.google.com";
	@Override
	public String getServiceType() {
		return SERVICE_TYPE;
	}

	@Override
	public String getServiceAccount() {
		return mAccount;
	}

	public EntryIterator<UserFeed, AlbumEntry> listDirectories(int maxResults) throws IOException {
		PicasaUrl url = PicasaUrl.feedBasedUser(getUserID());
		url.kinds = "album";
		url.maxResults = maxResults;
		return new EntryIterator<UserFeed, AlbumEntry>(url, UserFeed.class) {
			
			@Override
			protected String getNext(UserFeed feed) {
				return feed.getNextLink();
			}
			
			@Override
			protected AlbumEntry getElementAt(UserFeed feed, int pos) {
				return feed.albums.get(pos);
			}
			
			@Override
			protected int countElements(UserFeed feed) {
				if (feed == null || feed.albums == null) {
					return 0;
				} else {
					return feed.albums.size();
				}
			}
		};
	}
	
	@Override
	public EntryIterator<UserFeed, AlbumEntry> iterateDirectory()
			throws IOException {
		return listDirectories(10);
	}

	@Override
	public EntryIterator<AlbumFeed, PhotoEntry> iterateMedia(String directoryID) throws IOException {
		return listMedia(directoryID, 10, null, "d");
	}
	
	public EntryIterator<AlbumFeed, PhotoEntry> listMedia(
			String directoryID, int maxResults, String[] thumbSizes, String imgMax)
			throws IOException {
		PicasaUrl url = toListMediaUrl(directoryID, maxResults, thumbSizes,
				imgMax);
		
		return new EntryIterator<AlbumFeed, PhotoEntry>(url, AlbumFeed.class) {
			
			@Override
			protected String getNext(AlbumFeed feed) {
				return feed.getNextLink();
			}
			
			@Override
			protected PhotoEntry getElementAt(AlbumFeed feed, int pos) {
				return feed.photos.get(pos);
			}
			
			@Override
			protected int countElements(AlbumFeed feed) {
				if (feed == null || feed.photos == null) {
					return 0;
				} else {
					return feed.photos.size();
				}
			}
		};
	}
	
	public EntryIterator<LiteAlbumFeed, LitePhotoEntry> listMediaLite(
			String directoryID, int maxResults, String[] thumbSizes, String imgMax)
			throws IOException {
		PicasaUrl url = toListMediaUrl(directoryID, maxResults, thumbSizes,
				imgMax);
		
		return new EntryIterator<LiteAlbumFeed, LitePhotoEntry>(url, LiteAlbumFeed.class) {
			
			@Override
			protected String getNext(LiteAlbumFeed feed) {
				return feed.getNextLink();
			}
			
			@Override
			protected LitePhotoEntry getElementAt(LiteAlbumFeed feed, int pos) {
				return feed.photos.get(pos);
			}
			
			@Override
			protected int countElements(LiteAlbumFeed feed) {
				if (feed == null || feed.photos == null) {
					return 0;
				} else {
					return feed.photos.size();
				}
			}
		};
	}

	protected PicasaUrl toListMediaUrl(String directoryID, int maxResults,
			String[] thumbSizes, String imgMax) {
		PicasaUrl url = PicasaUrl.feedBasedAlbum(getUserID(), directoryID);
		url.kinds = "photo";
		if (0 < maxResults) {
			url.maxResults = maxResults;
		}
		if (thumbSizes != null && 0 < thumbSizes.length) {
			url.thumbsize = TextUtils.join(",", thumbSizes);
		}
		if (!TextUtils.isEmpty(imgMax)) {
			url.imgmax = imgMax;
		}
		return url;
	}

	@Override
	public PhotoEntry getMedia(MediaSync sync) throws IOException {
		return getMedia(sync.directoryID, sync.syncData1);
	}
	
	public PhotoEntry getMedia(String albumId, String photoId) throws IOException {
		PicasaUrl url =
				PicasaUrl.entryOfPhoto(getUserID(), albumId, photoId);
		url.imgmax = "d";
		return mClient.executeGet(url, PhotoEntry.class);
	}
	
	@Override
	public InputStream openContent(Media media) throws IOException {
		PhotoEntry entry = (PhotoEntry) media;
		return openContent(entry.mediaGroup.content.url);
	}
	
	public InputStream openContent(String url) throws IOException {
		return mClient.openContent(new GenericUrl(url));
	}

	@Override
	public PhotoEntry insertMedia(String directoryID, String name, InputStream content, Collection<MediaMetadata> metadata)
			throws IOException {
		String mimeType = MediaUtil.getMimeTypeFromPath(name);
		if (mimeType != null
				&& (sSupportedImageTypes.contains(mimeType) || sSupportedVideoTypes.contains(mimeType))) {
			PicasaUrl url = PicasaUrl.feedBasedAlbum(getUserID(), directoryID);
			
			// メディア本体
			InputStreamContent c = new InputStreamContent(mimeType, content);
			
			// メタデータ
			ArrayList<String> tags = new ArrayList<String>();
			for (MediaMetadata m : metadata) {
				if (CMediaMetadata.TYPE_TAG.equals(m.type)) {
					tags.add(m.value);
				}
			}
			String keywords = TextUtils.join(", ", tags);
			
			PhotoEntry newEntry = new PhotoEntry();
			newEntry.title = name;
			newEntry.mediaGroup = new MediaGroup();
			newEntry.mediaGroup.keywords = keywords;
			
			PhotoEntry entry = mClient.executeInsertPhotoEntryWithMetadata(newEntry, url, c);
			if (JorlleApplication.debuggable()) {
				d("new media inserted at %d - %s", System.currentTimeMillis(), JsonUtil.toJson(entry));
			}
			return entry;
			
		} else {
			return null;
		}
	}

	@Override
	public PhotoEntry updateMedia(MediaSync sync, InputStream content, Collection<MediaMetadata> metadata)
			throws IOException {
		PhotoEntry original =
				JsonUtil.fromJson(new String(sync.syncData5), PhotoEntry.class);
		PhotoEntry newEntry =
				JsonUtil.fromJson(new String(sync.syncData5), PhotoEntry.class);
		
		// メディア本体
		InputStreamContent c = new InputStreamContent(original.mediaGroup.content.type, content);
		
		// メタデータ
		ArrayList<String> tags = new ArrayList<String>();
		for (MediaMetadata m : metadata) {
			if (CMediaMetadata.TYPE_TAG.equals(m.type)) {
				tags.add(m.value);
			}
		}
		String keywords = TextUtils.join(", ", tags);
		
		if (newEntry.mediaGroup == null) {
			newEntry.mediaGroup = new MediaGroup();
		}
		newEntry.mediaGroup.keywords = keywords;
		
		return mClient.executeUpdatePhotoEntryWithMetadata(newEntry, c);
	}

	@Override
	public void deleteMedia(MediaSync sync) throws IOException {
		PhotoEntry entry = JsonUtil.fromJson(sync.syncData5, PhotoEntry.class);
		mClient.executeDelete(entry);
	}

	@Override
	public void initialize(HttpRequest request) throws IOException {
		request.setInterceptor(this);
	}

	@Override
	public void intercept(HttpRequest request) throws IOException {
		if (mAuthToken != null) {
			request.getHeaders().setAuthorization(GoogleHeaders.getGoogleLoginValue(mAuthToken));		
		}
	}
	
	/** FeedをもとにEntryをイテレーションします */
	public abstract class EntryIterator<F extends Feed, E extends Object> implements IOIterator<E> {

		final Class<F> mFeedType;
		EntryIterator(PicasaUrl url, Class<F> feedType) throws IOException {
			super();
			mFeedType = feedType;
			mFeed = mClient.executeGet(url, mFeedType);
			prepareNext();
		}
		
		F mFeed;
		int mPos;
		E mNext;
		
		void prepareNext() throws IOException {
			int count = countElements(mFeed);
			if (count == 0) {
				mNext = null;
			} else if (mPos < count) {
				mNext = getElementAt(mFeed, mPos++);
			} else {
				String nextUrl = getNext(mFeed);
				if (TextUtils.isEmpty(nextUrl)) {
					mNext = null;
				} else {
					mFeed = mClient.executeGet(new PicasaUrl(nextUrl), mFeedType);
					mPos = 0;
					prepareNext();
				}
			}
		}
		
		@Override
		public boolean hasNext() throws IOException {
			return mNext != null;
		}

		@Override
		public E next() throws IOException {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			
			try {
				return mNext;
			} finally {
				prepareNext();
			}
		}

		@Override
		public void terminate() throws IOException {
		}
		
		
		protected abstract int countElements(F feed);
		
		protected abstract E getElementAt(F feed, int pos);
		
		protected abstract String getNext(F feed);
		
		public String getEtag() {
			return mFeed.etag;
		}
	}
	
	/**
	 * PicasaWebアルバムFeedのユーザIDを返します。
	 * @return ユーザID
	 */
	public String getUserID() {
		int index = mAccount.indexOf('@');
		if (0 <= index) {
			return mAccount.substring(0, index);
		} else {
			return mAccount;
		}
	}

	@Override
	public byte[] serializeMedia(Media media) {
		if (media instanceof PhotoEntry) {
			return JsonUtil.toJson(media).getBytes();
		} else {
			return null;
		}
	}

	@Override
	public PhotoEntry deserializeMedia(byte[] data)
			throws UnsupportedOperationException {
		String json = new String(data);
		return JsonUtil.fromJson(json, PhotoEntry.class);
	}
	
	public AlbumEntry insertAlbum(String name) throws IOException {
		AlbumEntry album = new AlbumEntry();
		album.title = name;
		album.access = "private";
		PicasaUrl url = PicasaUrl.feedBasedUser(getUserID());
		return mClient.executeInsert(url, album);
	}
	
	public AlbumEntry insertAlbum(String name, String access) throws IOException {
		AlbumEntry album = new AlbumEntry();
		album.title = name;
		album.access = access;
		PicasaUrl url = PicasaUrl.feedBasedUser(getUserID());
		return mClient.executeInsert(url, album);
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
