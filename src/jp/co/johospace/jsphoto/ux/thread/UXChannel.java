package jp.co.johospace.jsphoto.ux.thread;

import java.util.ArrayList;

/**
 * スレッド間のメッセージ受け渡しに使用されるチャンネル
 */
public class UXChannel {
	private ArrayList<UXMessage> mMessages;
	private long mIdNumber;
	public static final long INVALID_ID = Long.MIN_VALUE;

	public UXChannel(){
		mMessages = new ArrayList<UXMessage>();
		mIdNumber = Long.MIN_VALUE + 1;
	}

	/**
	 * メッセージをチャンネルに投函する。受け取りスレッドが待ち状態なら起こす。
	 *
	 * @param message
	 * @param emergency
	 * @return
	 */
	public synchronized long postMessage(UXMessage message, boolean emergency){
		long id = mIdNumber++;
		if(id == INVALID_ID)id = ++mIdNumber;
		message.setId(id);
		if(emergency){
			mMessages.add(0, message);
		}else{
			mMessages.add(message);
		}

		notifyAll();

		return id;
	}

	/**
	 * 同一IDでポスト。
	 *
	 * @param message
	 * @param emergency
	 * @return
	 */
	public synchronized void repostMessage(UXMessage message, boolean emergency){
		if(emergency){
			mMessages.add(0, message);
		}else{
			mMessages.add(message);
		}

		notifyAll();
	}

	/**
	 * メッセージを通常投函する。
	 *
	 * @param message
	 * @return
	 */
	public long postMessage(UXMessage message){
		return postMessage(message, false);
	}

	/**
	 * メッセージを投函する。同じタイプのメッセージが既に存在する場合は無視。
	 *
	 * @param message
	 * @return 投函されたかどうか
	 */
	public synchronized boolean postSingleMessage(UXMessage message){
		int size = mMessages.size();
		for(int n = 0; n < size; ++n){
			if(mMessages.get(n).getClass().equals(message.getClass())){
				return false;
			}
		}

		postMessage(message, false);
		return true;
	}

	/**
	 * 指定されたIDのメッセージをキャンセルする。
	 *
	 * @param id
	 * @return キャンセルされたらtrue
	 */
	public synchronized boolean cancelMessage(long id){
		int size = mMessages.size();
		for(int n = 0; n < size; ++n){
			UXMessage msg = mMessages.get(n);
			if(msg.getId() == id){
				mMessages.remove(n);
				return true;
			}
		}

		return false;
	}

	/**
	 * メッセージがチャンネル内に存在するかどうか
	 *
	 * @return 存在するならtrue
	 */
	public synchronized boolean hasMessage(){
		return mMessages.size() != 0;
	}


	/**
	 * メッセージを受け取る。無いなら待つ
	 *
	 * @return 受け取ったメッセージ
	 */
	public synchronized UXMessage waitForMessage(){
		if(mMessages.size() != 0){
			return mMessages.remove(0);
		}else{
			while(true){
				try {
					wait();
				} catch (InterruptedException e) {}
				if(mMessages.size() != 0){
					return mMessages.remove(0);
				}
			}
		}
	}
}
