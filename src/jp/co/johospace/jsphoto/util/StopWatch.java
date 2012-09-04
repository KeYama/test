package jp.co.johospace.jsphoto.util;

import android.os.SystemClock;

/**
 * ストップウォッチ
 */
public class StopWatch {

	/** 開始時刻 */
	public final long start;
	/** 最終のラップ時間 */
	public long latestLap = -1L;
	/** 相対計測用 */
	private final long real;
	/** 相対計測用 */
	private long lapReal;
	
	/** コンストラクタ */
	private StopWatch() {
		super();
		start = System.currentTimeMillis();
		real = SystemClock.elapsedRealtime();
		lapReal = real;
	}
	
	/**
	 * 開始します。
	 * @return ストップウォッチ
	 */
	public static StopWatch start() {
		return new StopWatch();
	}
	
	/**
	 * 開始からの経過時間を返します。
	 * @return 開始からの経過時間
	 */
	public long elapsed() {
		return SystemClock.elapsedRealtime() - real;
	}
	
	/**
	 * ラップを計測します。
	 * @return 経過時間
	 */
	public long lap() {
		long current = SystemClock.elapsedRealtime();
		latestLap = current - lapReal;
		lapReal = current;
		return latestLap;
	}
}
