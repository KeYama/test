package jp.co.johospace.jsphoto.v2.onlineservice.twitter.resolvers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * メディアURLリゾルバマネージャ
 */
public class MediaUrlResolverManager {
	private MediaUrlResolverManager() {}
	
	private static final Map<String, MediaUrlResolver> sResolvers;
	static {
		HashMap<String, MediaUrlResolver> map = new HashMap<String, MediaUrlResolver>();
		MediaUrlResolver[] resolvers = {
				new TwitterResolver(),
				new TwitpicResolver(),
				new HatenaNeJpResolver(),
				new HatenaComResolver(),
				new MobyPictureResolver(),
				new MovapicResolver(),
				new YFrogResolver(),
		};
		
		for (MediaUrlResolver resolver : resolvers) {
			map.put(resolver.getStorageType(), resolver);
		}
		
		sResolvers = Collections.unmodifiableMap(map);
	}
	
	public static MediaUrlResolver getResolver(String type) {
		return sResolvers.get(type);
	}
}
