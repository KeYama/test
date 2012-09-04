package jp.co.johospace.jsphoto.fullscreen;

import java.util.LinkedList;
import java.util.Queue;

import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.view.SurfaceHolder;


public class MessageChannel {
	Queue<Object> mQueue = new LinkedList<Object>();
	
	public MessageChannel(){
		
	}
	
	public static class Message{
		public void dispose(){}
	}
	
	public static class Exit extends Message{
	}
	
	public static class LoadBitmap extends Message{
		public Bitmap bitmap;
		public Object tag;
		public Object info;
		public boolean isFull;
		
		public LoadBitmap(Bitmap b, Object t, Object i, boolean full){
			bitmap = b;
			tag = t;
			info = i;
			isFull = full;
		}
		
		@Override
		public void dispose(){
			bitmap.recycle();
		}
	}
	
	public static class FailedLoadingBitmap extends Message{
		public Object tag;
		public Object info;
		
		public FailedLoadingBitmap(Object t, Object i){
			tag = t;
			info = i;
		}
	}
	
	public static class SurfaceChanged extends Message{
		public SurfaceHolder holder;
		public int width;
		public int height;
		
		public SurfaceChanged(SurfaceHolder s, int w, int h){
			holder = s;
			width = w;
			height = h;
		}
	}
	
	public static class Motion extends Message{
	}
	
	public static class Scroll extends Motion{
		public MotionEvent e1;
		public MotionEvent e2;
		public float dx;
		public float dy;
		public Scroll(MotionEvent _e1, MotionEvent _e2, float _dx, float _dy){
			e1 = _e1;
			e2 = _e2;
			dx = _dx;
			dy = _dy;
		}
	}
	
	public static class Fling extends Motion{
		public MotionEvent e1;
		public MotionEvent e2;
		public float vx;
		public float vy;
		public Fling(MotionEvent _e1, MotionEvent _e2, float _vx, float _vy){
			e1 = _e1;
			e2 = _e2;
			vx = _vx;
			vy = _vy;
		}
	}
	
	public static class Down extends Motion{
		public MotionEvent e;
		public Down(MotionEvent _e){
			e = _e;
		}
	}
	
	public static class Up extends Motion{
		public MotionEvent e;
		public Up(MotionEvent _e){
			e = _e;
		}
	}
	
	public static class DoubleTap extends Motion{
		public MotionEvent e;
		public DoubleTap(MotionEvent _e){
			e = _e;
		}
	}
	
	public static class Scaling extends Motion{
		public float scale;
		public Scaling(float _s){
			scale = _s;
		}
	}
	
	public static class ScaleStart extends Motion{
	}
	
	public static class ScaleEnd extends Motion{
	}
	
	public synchronized void putMessage(Object message){
		mQueue.add(message);
		notifyAll();
	}
	
	public synchronized Object takeMessage(){
		return mQueue.poll();
	}
	
	public synchronized void waitMessage(){
		if(mQueue.size() != 0)return;
		
		try {
			wait();
		} catch (InterruptedException e) {}
	}
}
