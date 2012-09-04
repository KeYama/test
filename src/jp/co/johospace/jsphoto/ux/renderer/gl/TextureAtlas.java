package jp.co.johospace.jsphoto.ux.renderer.gl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.opengl.GLES11;
import android.opengl.GLU;
import jp.co.johospace.jsphoto.ux.renderer.UXCanvasResource;

import javax.microedition.khronos.opengles.GL11;
import java.util.ArrayList;

/**
 *
 * テクスチャ作成クラス
 *
 */
class TextureAtlas {
	private static final int TEXTURE_WIDTH = 1024;
	private static final int TEXTURE_HEIGHT = 1024;
	private static final int TEXTURE_PLATE_COUNT = 4;

	private static final Rect tmpRect = new Rect();


	private UXGLRenderEngine mEngine;

	private ArrayList<TexturePlate> mPlates = new ArrayList<TexturePlate>();
	ArrayList<Fragment> mActiveFragments = new ArrayList<Fragment>();

	public TextureAtlas(UXGLRenderEngine engine){
		mEngine = engine;
	}

	private class TexturePlate{
		private int mCurrentX;
		private int mCurrentY;
		private int mHighestY;
		private int mTextureName;
		private ArrayList<Fragment> mFragments = new ArrayList<Fragment>();

		public TexturePlate(UXGLRenderEngine engine){
			mTextureName = engine.getRenderer().createTexture(TEXTURE_WIDTH, TEXTURE_HEIGHT);
		}

		public void deleteTexture(UXGLRenderEngine engine){
			engine.getRenderer().deleteTexture(mTextureName);
		}

		public void addActiveFragments(ArrayList<Fragment> list){
			for(Fragment frag: mFragments){
				if(frag.isValid()){
					list.add(frag);
				}
			}
		}

		public Fragment createFragment(UXCanvasResource.CanvasRenderer renderer, int width, int height){
			Fragment fragment = new Fragment(renderer, mTextureName, new Rect(0,0, width, height));
			if(addFragment(fragment)){
				return fragment;
			}else{
				return null;
			}
		}

		public boolean addFragment(Fragment fragment){
			int height = fragment.getHeight();
			int width = fragment.getWidth();

			while(true){
				int emptyX = TEXTURE_WIDTH - mCurrentX;
				int emptyY = TEXTURE_HEIGHT - mCurrentY;

				//まったく空きがない
				if(emptyY < height)return false;

				if(emptyX < width){
					//次のベースラインへ
					mCurrentX = 0;
					mCurrentY += mHighestY;
					mHighestY = 0;
					continue;
				}

				//空き発見、確保処理
				Rect r = new Rect(0, 0, width, height);
				r.offset(mCurrentX, mCurrentY);
				fragment.setTextureArea(mTextureName, r);

				//幅更新
				mCurrentX += width;
				//必要ならば高さ更新
				if(mHighestY < height){
					mHighestY = height;
				}

				mFragments.add(fragment);
//				android.util.Log.e("dbg", r.toString() + " " + mTextureName);
				return true;
			}
		}
	}

	class Fragment{
		private Rect mTextureArea;
		private int mTextureName;
		private UXCanvasResource.CanvasRenderer mRenderer;
		private boolean isValid = true;

		public Fragment(UXCanvasResource.CanvasRenderer renderer, int textureName, Rect area){
			mTextureName = textureName;
			mRenderer = renderer;
			mTextureArea = area;
		}

		public void setTextureArea(int textureName, Rect area){
			mTextureName = textureName;
			mTextureArea = area;
		}

		public boolean isValid(){
			return isValid;
		}

		public int getWidth(){
			return mTextureArea.width();
		}

		public int getHeight(){
			return mTextureArea.height();
		}

		public boolean draw(UXGLRenderEngine engine, int x, int y){
//			mEngine.getRenderer().draw(mTextureName, TEXTURE_WIDTH, TEXTURE_HEIGHT,
//					0, 0, 1024, 1024,
//					0, 0);

			mEngine.getRenderer().draw(mTextureName, TEXTURE_WIDTH, TEXTURE_HEIGHT,
					x, y, mTextureArea.width(), mTextureArea.height(),
					mTextureArea.left, mTextureArea.top);
			return true;
		}

		public boolean draw(UXGLRenderEngine engine, Rect src, Rect dst, float alpha){
			tmpRect.set(src);
			tmpRect.offset(mTextureArea.left, mTextureArea.top);

			engine.getRenderer().draw(mTextureName,
					TEXTURE_WIDTH, TEXTURE_HEIGHT,
					tmpRect, dst, alpha, true);

			return true;
		}

		public boolean draw9scale(UXGLRenderEngine engine, Rect scale, Rect dst){
			return mEngine.getRenderer().draw9scale(
					mTextureName,
					TEXTURE_WIDTH,  TEXTURE_HEIGHT,
					scale, dst, mTextureArea
			);
		}

		public void setRenderer(UXCanvasResource.CanvasRenderer renderer){
			mRenderer = renderer;
		}

		public void dispose(){
			isValid = false;
		}

		public void redraw(UXGLRenderEngine engine){
			Bitmap b = Bitmap.createBitmap(mTextureArea.width(), mTextureArea.height(), Bitmap.Config.ARGB_4444);
			Canvas c = new Canvas(b);
			mRenderer.draw(c);

			engine.getRenderer().addImageToTexture(mTextureName, mTextureArea.left, mTextureArea.top, b);

			b.recycle();
		}

	}

	public Fragment createFragment(UXCanvasResource.CanvasRenderer renderer, int width, int height){
		if(width > TEXTURE_WIDTH || height > TEXTURE_HEIGHT){
			throw new RuntimeException("too large texture");
		}

		if(mPlates.size() == 0){
			addTexture();
		}

		Fragment fragment = null;
		boolean repacked = false;

		while(true){
			TexturePlate current = mPlates.get(mPlates.size()-1);
			fragment = current.createFragment(renderer, width, height);

			if(fragment == null){
				if(mPlates.size() >= TEXTURE_PLATE_COUNT){
					if(!repacked){
						repacked = true;
						repack();
					}else{
						return null;
					}
				}
				addTexture();
			}else{
				break;
			}
		}

		return fragment;
	}

	private void addTexture(){
		mPlates.add(new TexturePlate(mEngine));
	}

	public void clearOnSurfaceChanged(){
		ArrayList<Fragment> activeFragments = mActiveFragments;
		for(TexturePlate plate: mPlates){
			plate.addActiveFragments(activeFragments);
			plate.deleteTexture(mEngine);
		}

		mPlates.clear();
	}

	public void repack(){
		ArrayList<Fragment> activeFragments = mActiveFragments;
		for(TexturePlate plate: mPlates){
			plate.addActiveFragments(activeFragments);
			plate.deleteTexture(mEngine);
		}
//		android.util.Log.e("dbg", ""+mPlates.size());

		mPlates.clear();
		if(activeFragments.size() == 0)return;

		addTexture();

		for(Fragment frag: activeFragments){
			TexturePlate plate = mPlates.get(mPlates.size() - 1);

			if(!plate.addFragment(frag)){
				addTexture();
				plate = mPlates.get(mPlates.size() - 1);
				if(!plate.addFragment(frag) || mPlates.size() > TEXTURE_PLATE_COUNT){
					throw new RuntimeException("repack failed");
				}
			}else{
				frag.redraw(mEngine);
			}
		}

		mActiveFragments.clear();
	}
}
