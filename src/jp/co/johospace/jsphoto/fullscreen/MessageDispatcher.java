package jp.co.johospace.jsphoto.fullscreen;

import java.util.ArrayList;
import java.util.List;

public class MessageDispatcher {
	MessageChannel mChannel;
	List<MessageDispatcher.Handler> mHandlerList = new ArrayList<MessageDispatcher.Handler>();
	
	public static interface Handler{
		public boolean handleMessage(Object msg);
	}
	
	public MessageDispatcher(){
		mChannel = new MessageChannel();
	}
	
	public void addMessageHandler(MessageDispatcher.Handler handler){
		mHandlerList.add(handler);
	}
	
	public void removeMessageHandler(MessageDispatcher.Handler handler){
		mHandlerList.remove(handler);
	}
	
	public void putMessage(Object message){
		mChannel.putMessage(message);
	}
	
	public void waitMessage(){
		mChannel.waitMessage();
	}
	
	/**
	 * 
	 * @return 処理を続けるかどうか
	 */
	public boolean handleMessage(){
		Object message = mChannel.takeMessage();
		while(message != null){
			if(message instanceof MessageChannel.Exit){
				return false;
			}
			
			boolean handled = false;
			for(MessageDispatcher.Handler handler: mHandlerList){
				if(handler.handleMessage(message)){
					handled = true;
					break;
				}
			}
			
			if(!handled){
				((MessageChannel.Message)message).dispose();
			}
			
			message = mChannel.takeMessage();
		}
		
		return true;
	}
}
