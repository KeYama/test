package jp.co.johospace.jsphoto.fullscreen;

import android.graphics.Matrix;

/**
 * フレームスクリーン。
 * 描画位置の計算を行う。（行列を返すことで反映する）
 * 
 */
public class FrameScreen implements MessageDispatcher.Handler{
	private static final float K = 0.15f;
	
	/** 吸着状態。対象位置に吸いつくように移動する */
	private static final int STATE_ABSORB = 1;
	/** 移動状態。速度に応じてフレームスクリーンが移動する */
	private static final int STATE_FLING = 2;
	/** なにも入力されていない状態 */
	private static final int STATE_NEUTRAL = 0;
	/** 移動状態から吸着状態に遷移する速度の閾値 */
	private static final float MIN_VELOCITY = 0.3f;
	
	private Measure mMeasure;
	/** 表示位置の最大値。Measureによる仮想位置。仮想位置に関してはMeasure参照 */
	private float mMax;
	private Matrix mMatrix;
	/** 現在の仮想位置。仮想位置に関してはMeasure参照 */
	private float mVirtualPosition;
	
	/** 現在の速度 */
	private float mVelocity;
	/** STATE_ABSORBで使用する補間係数 */
	private float mT;
	/** STATE_ABSORBで使用する補間のための初期位置 */
	private float mInitX;
	/** STATE_ABSORBで使用する、補間のターゲット位置 */
	private float mTarget;
	/** 現在のフレーム位置 */
	private int mCurrent;
	
	private int mState;
	
	public FrameScreen(Measure measure, float position, float max){
		mMeasure = measure;
		mMax = max;
		mCurrent = (int)position;
		mMatrix = new Matrix();
		mVirtualPosition = position;
		mState = STATE_NEUTRAL;
	}
	
	/** 描画を行う */
	public Matrix getMatrix(){
		mMatrix.setTranslate(-mMeasure.toWorldX(mVirtualPosition), 0);
		return mMatrix;
	}
	
	/**
	 * スクリーンの仮想位置から、現在メインで表示しているImageFrameの位置を取得する
	 * 
	 */
	public int getCenterFrameNumber(){
		float d = mVirtualPosition - mCurrent;
		if(d <= -0.5f || 0.5f <= d){
			mCurrent = (int)Math.round(mVirtualPosition);
		}
		
		if(mCurrent >= (int)mMax)mCurrent = (int)mMax;
		if(mCurrent < 0)mCurrent = 0;
		return mCurrent;
	}
	
	/**
	 * 時間を進める
	 * 
	 * @return 次の更新が必要かどうか
	 */
	public boolean process(){
		float tmpPosition = (int)mVirtualPosition;
		mVelocity -= K * mVelocity;
		mVirtualPosition += mVelocity / 15.0f;

		// 一ページ以上はスクロールさせない
		if(tmpPosition!=(int)mVirtualPosition) {
			mVelocity = 0;
			if(mVirtualPosition > tmpPosition) {
				mVirtualPosition = tmpPosition + 1;
			} else {
				mVirtualPosition = tmpPosition;
			}
			mState = STATE_NEUTRAL;
			return false;
		}
		// コレ以上は行けない
		if(mVirtualPosition>mMax) {
			mVirtualPosition = mMax;
			mState = STATE_NEUTRAL;
			mVelocity = 0;
			return false;
		}
		// コレ以上は行けない
		if(mVirtualPosition<0) {
			mVirtualPosition = 0;
			mState = STATE_NEUTRAL;
			mVelocity = 0;
			return false;
		}
		
		if((float)Math.abs(mVelocity) < MIN_VELOCITY && mState == STATE_FLING){
			mT = 0;
			mTarget = getCenterFrameNumber();
			
			float d = mVirtualPosition - mTarget;
			//0.2程度の移動でも反応するようにする。
			final float D = 0.2f;
			if(mVelocity == 0){
				if(d < -D){
					mTarget--;
				}else if(d > D){
					mTarget++;
				}
			}else if(mVelocity < 0){
				if(d < -D){
					mTarget--;
				}
			}else if(mVelocity > 0){
				if(d > D){
					mTarget++;
				}
			}
			
			
			if(mTarget < 0) mTarget = 0;
			if(mTarget > mMax)mTarget = mMax;
			mInitX = mVirtualPosition;
			mVelocity = 0;
			mState = STATE_ABSORB;
		}
		
		if(mState == STATE_ABSORB){
			mT += 0.1f;
			float t = CurveUtil.easeOutSin(mT);
			mVirtualPosition = (1 - t) * mInitX + t * mTarget;
			if(mT > 1){
				mVirtualPosition = mTarget;
				mState = STATE_NEUTRAL;
				return false;
			}
			return true;
		}
		
		return mVelocity != 0 || mState != STATE_NEUTRAL;
	}

	@Override
	public boolean handleMessage(Object msg) {
		if(msg instanceof MessageChannel.Fling){
			onFling((MessageChannel.Fling)msg);
			return true;
		}else if(msg instanceof MessageChannel.Scroll){
			onScroll((MessageChannel.Scroll)msg);
			return true;
		}else if(msg instanceof MessageChannel.Down){
			mVelocity = 0;
			mState = STATE_NEUTRAL;
			return true;
		}else if(msg instanceof MessageChannel.Up){
			mState = STATE_FLING;
			return true;
		}
		return false;
	}
	
	private void onFling(MessageChannel.Fling msg){
		mVelocity = mMeasure.toVirtualX(-msg.vx);
		mState = STATE_FLING;
	}
	
	private void onScroll(MessageChannel.Scroll msg){
		mVelocity = 0;
		mState = STATE_NEUTRAL;
		mVirtualPosition += mMeasure.toVirtualX(msg.dx);
	}
	
	public boolean isNeutral(){
		return mState == STATE_NEUTRAL;
	}
}
