package jp.co.johospace.jsphoto.preference;

import java.util.ArrayList;

import jp.co.johospace.jsphoto.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

/**
 * Googleアカウントの選択Preference
 */
public class GoogleAccountListPreference extends ListPreference {

	private final String mNotSpecifiedLabel;
	private final String mNotSpecifiedValue;
	public GoogleAccountListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(
				attrs, R.styleable.GoogleAccountListPreference);
		mNotSpecifiedLabel = ta.getString(R.styleable.GoogleAccountListPreference_notSpecifiedLabel);
		mNotSpecifiedValue = ta.getString(R.styleable.GoogleAccountListPreference_notSpecifiedValue);
	}

	public GoogleAccountListPreference(Context context) {
		super(context);
		mNotSpecifiedLabel = null;
		mNotSpecifiedValue = null;
	}
	
	@Override
	public boolean shouldDisableDependents() {
		return TextUtils.isEmpty(getValue()) || super.shouldDisableDependents();
	}
	
	@Override
	public void setValue(String value) {
		if (callChangeListener(value)) {
			super.setValue(value);
			notifyDependencyChange(shouldDisableDependents());
			notifyChanged();
		}
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		
		ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
		ArrayList<CharSequence> values = new ArrayList<CharSequence>();
		
		if (mNotSpecifiedValue != null) {
			entries.add(mNotSpecifiedLabel);
			values.add(mNotSpecifiedValue);
		}
		
		AccountManager manager = AccountManager.get(getContext());
		Account[] accounts =
				manager.getAccountsByType("com.google");
		for (Account account : accounts) {
			entries.add(account.name);
			values.add(account.name);
		}
		
		setEntries(entries.toArray(new CharSequence[0]));
		setEntryValues(values.toArray(new CharSequence[0]));
	}

}
