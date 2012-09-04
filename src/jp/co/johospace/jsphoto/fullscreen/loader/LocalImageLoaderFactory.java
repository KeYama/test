package jp.co.johospace.jsphoto.fullscreen.loader;

public class LocalImageLoaderFactory implements ImageLoaderFactory {

	@Override
	public ImageLoader create() {
		return new LocalImageLoader();
	}

}
