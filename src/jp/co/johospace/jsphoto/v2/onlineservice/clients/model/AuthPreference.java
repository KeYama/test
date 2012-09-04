package jp.co.johospace.jsphoto.v2.onlineservice.clients.model;

import java.util.HashSet;
import java.util.Set;

import android.text.TextUtils;

/**
 * 認証・認可設定
 */
public class AuthPreference {

	/** サービスタイプ */
	public String service;
	/** 認可URL */
	public String authUrl;
	/** 認可解除URL */
	public String clearUrl;
	/** 利用有無 */
	public Boolean active;
	/** 有効期限切れ */
	public Boolean expired;
	
	/** 利用アカウント */
	public Set<String> accounts = new HashSet<String>();
	
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null || !(o instanceof AuthPreference)) {
			return false;
		}
		
		AuthPreference other = (AuthPreference) o;
		if (!TextUtils.equals(service, other.service)
				|| !TextUtils.equals(authUrl, other.authUrl)
				|| !TextUtils.equals(clearUrl, other.clearUrl)) {
			return false;
		}
		
		return active == other.active && expired == other.expired;
	}
}
