package jp.co.johospace.jsphoto.v2.onlineservice.jsmedia;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import jp.co.johospace.jsphoto.service.MediaSyncManagerV2;
import jp.co.johospace.jsphoto.util.IOIterator;
import jp.co.johospace.jsphoto.util.JsonUtil;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ContentsNotModifiedException;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.JsMediaServerClient;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.RespondedContents;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.AuthPreference;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.CameraPath;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Directory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Media;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.Memory;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.RelatedMedia;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.model.SyncPreference;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

/**
 * メディアサーバクライアント
 */
class JsMediaClient extends AbstractJsMediaClient implements JsMediaServerClient {

	private static final String KEY_AUTH_PREFERENCES = "AuthPreferences";
	
	public JsMediaClient(Context context) {
		super(context);
	}
	
	@Override
	public List<AuthPreference> getAuthPreferences(boolean forceResuest) throws IOException {
		SharedPreferences pref =
				mContext.getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE);
		String json;
		if (forceResuest) {
			JsMediaUrl url = JsMediaUrl.authPrefs(mContext);
			HttpResponse response = executeGet(url);
			json = response.parseAsString();
		} else {
			json = pref.getString(KEY_AUTH_PREFERENCES, null);
		}
		
		if (json == null) {
			return null;
		} else {
			List<AuthPreference> prefs = JsonUtil.fromJson(json,
					new TypeToken<List<AuthPreference>>(){}.getType());
			for (AuthPreference authPref : prefs) {
				Map<String, String> credentials = getExtCredentials(authPref.service);
				authPref.accounts.addAll(credentials.keySet());
				
				if (authPref.accounts.isEmpty()) {
					if (ClientManager.isBidirectional(authPref.service)) {
						MediaSyncManagerV2.deleteSyncSetting(mContext, authPref.service);
					}
				}
			}
			pref.edit().putString(KEY_AUTH_PREFERENCES, JsonUtil.toJson(prefs)).commit();
			return prefs;
		}
	}

	@Override
	public void requestIndexing(String serviceType, String serviceAccount, boolean async) throws IOException {
		JsMediaUrl url = JsMediaUrl.startIndexing(mContext);
		url.type = serviceType;
		url.account = serviceAccount;
		url.set("async", async ? 1 : 0);
		if (async) {
			executeGet(url);
		} else {
			executeGet(url, 20000, 10 * 60 * 1000, null);
		}
	}
	
	@Override
	public void requestRecreateIndex() throws IOException {
		JsMediaUrl url = JsMediaUrl.startRecreation(mContext);
		HttpRequest request = mFactory.buildPostRequest(url, null);
		request.setReadTimeout(2 * 60 * 1000);
		request.execute();
	}

	@Override
	public Long getCurrentMediaVersion(String serviceType, String serviceAccount)
			throws IOException {
		JsMediaUrl url = JsMediaUrl.currentMediaVersion(mContext);
		url.type = serviceType;
		url.account = serviceAccount;
		HttpResponse response = executeGet(url);
		String content = response.parseAsString();
		return TextUtils.isEmpty(content) ? null : Long.parseLong(content);
	}

	@Override
	public List<Directory> getDirectories(String serviceType,
			String serviceAccount, Long from, Long to, int mediaLimit, boolean syncExt)
			throws IOException {
		JsMediaUrl url = JsMediaUrl.albums(mContext);
		url.type = serviceType;
		url.account = serviceAccount;
		url.from = from;
		url.to = to;
		url.set("media", mediaLimit);
		
		HttpResponse response = executeGet(url);
		if (response.getStatusCode() == HttpURLConnection.HTTP_NO_CONTENT) {
			return new ArrayList<Directory>();
		} else {
			Gson gson = new Gson();
			JsonReader reader = new JsonReader(
					new InputStreamReader(response.getContent(), Charset.forName("UTF-8")));
			return gson.fromJson(reader, new TypeToken<List<Directory>>() {}.getType());
//			String json = response.parseAsString();
//			return JsonUtil.fromJson(json,
//					new TypeToken<List<Directory>>() {}.getType());
		}
	}

	@Override
	public List<Media> getMediaList(String serviceType, String serviceAccount, String dirId,
			boolean includeMetadata, Long from, Long to) throws IOException {
		JsMediaUrl url = JsMediaUrl.mediaList(mContext);
		url.type = serviceType;
		url.account = serviceAccount;
		url.from = from;
		url.to = to;
		url.dirId = dirId;
		url.set("includeMetadata", includeMetadata);
		
		HttpResponse response = executeGet(url, 20000, 60000, null);
		if (response.getStatusCode() == HttpURLConnection.HTTP_NO_CONTENT) {
			return new ArrayList<Media>();
		} else {
			Gson gson = new Gson();
			JsonReader reader = new JsonReader(
					new InputStreamReader(response.getContent(), Charset.forName("UTF-8")));
			return gson.fromJson(reader, new TypeToken<List<Media>>() {}.getType());
//			return JsonUtil.fromJson(response.parseAsString(),
//					new TypeToken<List<Media>>() {}.getType());
		}
	}

	@Override
	public List<Media> searchMediaByKeyword(String keyword, boolean includeLinkage) throws IOException {
		JsMediaUrl url = JsMediaUrl.searchWord(mContext);
		url.set("keyword", keyword);
		url.set("includeLinkage", includeLinkage ? 1 : 0);
		
		HttpResponse response = executeGet(url);
//		BufferedReader reader = new BufferedReader(
//				new InputStreamReader(response.getContent(), Charset.forName("UTF-8")));
//		try {
//			ArrayList<Media> results = new ArrayList<Media>();
//			String json;
//			while ((json = reader.readLine()) != null) {
//				results.add(JsonUtil.fromJson(json, Media.class));
//			}
//			return results;
//		} finally {
//			reader.close();
//		}
//		String json = response.parseAsString();
//		System.out.println(json);
//		return JsonUtil.fromJson(json, new TypeToken<List<Media>>() {}.getType());
		Gson gson = new Gson();
		Reader reader = new InputStreamReader(response.getContent(), Charset.forName("UTF-8"));
		try {
			return gson.fromJson(reader, new TypeToken<List<Media>>() {}.getType());
		} finally {
			reader.close();
		}
	}

	@Override
	public RelatedMedia searchRelatedMedia(String serviceType,
			String serviceAccount, String mediaId) throws IOException {
		JsMediaUrl url = JsMediaUrl.searchRelative(mContext);
		url.type = serviceType;
		url.account = serviceAccount;
		url.mediaId = mediaId;
		
		HttpResponse response = executeGet(url);
//		String json = response.parseAsString();
//		return JsonUtil.fromJson(json, RelatedMedia.class);
		Gson gson = new Gson();
		Reader reader = new InputStreamReader(response.getContent(), Charset.forName("UTF-8"));
		try {
			return gson.fromJson(reader, RelatedMedia.class);
		} finally {
			reader.close();
		}
	}

	@Override
	public RespondedContents<IOIterator<Memory>> searchMemories(String etag) throws IOException, ContentsNotModifiedException {
		JsMediaUrl url = JsMediaUrl.memories(mContext);
		
		HttpResponse response;
		try {
			response = executeGet(url, 20000, 30000, etag);
		} catch (HttpResponseException e) {
			if (e.getResponse().getStatusCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
				throw new ContentsNotModifiedException();
			} else {
				throw e;
			}
		}
		
		final JsonReader reader = new JsonReader(
				new InputStreamReader(new BufferedInputStream(response.getContent()), Charset.forName("UTF-8")));
		
		IOIterator<Memory> contents = new IOIterator<Memory>() {

			final JsonParser mParser = new JsonParser();
			final Gson mGson = new Gson();
			
			{
				try {
					reader.beginArray();
				} catch (IOException e) {
					reader.close();
					throw e;
				} catch (RuntimeException e) {
					reader.close();
					throw e;
				}
			}
			
			@Override
			public Memory next() throws IOException, NoSuchElementException {
				if (reader.hasNext()) {
					JsonElement element = mParser.parse(reader);
					return mGson.fromJson(element, Memory.class);
				} else {
					throw new NoSuchElementException();
				}
			}

			@Override
			public boolean hasNext() throws IOException {
				return reader.hasNext();
			}

			@Override
			public void terminate() throws IOException {
				reader.close();
			}
		};
		
		return new RespondedContents<IOIterator<Memory>>(contents, response.getHeaders().getETag());
	}

	@Override
	public synchronized IOIterator<Media> updateLocalMediaIndices(
			IOIterator<Media> localMedia, long[] out_next) throws IOException {
		try {
			File dir = new File(mContext.getExternalCacheDir(), getClass().getSimpleName());
			dir.mkdirs();
			File temp = new File(dir, "updateLocalMediaIndices");
			PrintWriter writer = new PrintWriter(temp);
			try {
				try {
					while (localMedia.hasNext()) {
						Media local = localMedia.next();
						writer.println(JsonUtil.toJson(local));
					}
				} finally {
					writer.close();
				}
				
				JsMediaUrl url = JsMediaUrl.localMedia(mContext);
				InputStreamContent content =
						new InputStreamContent("application/json", new FileInputStream(temp));
				HttpRequest request = mFactory.buildPostRequest(url, content);
				request.setReadTimeout(0);
				
				final HttpResponse response = execute(request);
				List<?> header = (List<?>) response.getHeaders().get("X-JorlleNextSync");
				if (header != null && !header.isEmpty()) {
					String[] hhmm = header.get(0).toString().split(":");
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.DATE, 1);
					cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hhmm[0]));
					cal.set(Calendar.MINUTE, Integer.parseInt(hhmm[1]));
					out_next[0] = cal.getTimeInMillis();
				} else {
					out_next[0] = Long.MIN_VALUE;
				}
				return new IOIterator<Media>() {

					final BufferedReader mReader =
							new BufferedReader(new InputStreamReader(response.getContent()));
					Media readNext() throws IOException {
						String json = mReader.readLine();
						if (TextUtils.isEmpty(json)) {
							return null;
						} else {
							return JsonUtil.fromJson(json, Media.class);
						}
					}
					Media mNext = readNext();
					
					@Override
					public Media next() throws IOException,
							NoSuchElementException {
						try {
							return mNext;
						} finally {
							mNext = readNext();
						}
					}

					@Override
					public boolean hasNext() throws IOException {
						return mNext != null;
					}

					@Override
					public void terminate() throws IOException {
						mReader.close();
					}
				};
			} finally {
				temp.delete();
			}
			
		} finally {
			localMedia.terminate();
		}
	}

	@Override
	public List<CameraPath> getCameraPathList() throws IOException {
		JsMediaUrl url = JsMediaUrl.cameraPaths(mContext);
		
		HttpResponse response = executeGet(url);
//		return JsonUtil.fromJson(response.parseAsString(),
//				new TypeToken<List<CameraPath>>() {}.getType());
		Gson gson = new Gson();
		Reader reader = new InputStreamReader(response.getContent(), Charset.forName("UTF-8"));
		try {
			return gson.fromJson(reader, new TypeToken<List<CameraPath>>() {}.getType());
		} finally {
			reader.close();
		}
	}
	
	@Override
	public SyncPreference setupSync(String serviceType, String serviceAccount) throws IOException {
		JsMediaUrl url = JsMediaUrl.setupSync(mContext);
		url.type = serviceType;
		url.account = serviceAccount;
		
		HttpRequest request = mFactory.buildPostRequest(url, null);
		HttpResponse response = execute(request);
		return JsonUtil.fromJson(response.parseAsString(), SyncPreference.class);
	}
	
	@Override
	public Map<String, String> getExternalServiceCredentials(
			String serviceType, boolean forceRequest) throws IOException {
		if (forceRequest) {
			JsMediaUrl url = JsMediaUrl.externalCredentials(mContext);
			executeGet(url);
		}
		return getExtCredentials(serviceType);
	}

	@Override
	public Map<String, String> createAccount() throws IOException {
		JsMediaUrl url = JsMediaUrl.createAccount(mContext);
		HttpResponse response = executeGet(url);
		return JsonUtil.fromJson(response.parseAsString(),
				new TypeToken<Map<String, String>>() {}.getType());
	}

}
