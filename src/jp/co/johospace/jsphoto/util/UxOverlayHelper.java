package jp.co.johospace.jsphoto.util;

import jp.co.johospace.jsphoto.R;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget.OverlayDataSource;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget.OverlayGrid;
import jp.co.johospace.jsphoto.ux.widget.UXGridWidget.OverlayItem;
import jp.co.johospace.jsphoto.ux.widget.UXUnit;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ClientManager;
import jp.co.johospace.jsphoto.v2.onlineservice.clients.ServiceType;
import android.content.Context;
import android.content.res.Resources;

public class UxOverlayHelper extends OverlayGrid {

	private static final String[] SERVICE_TYPES = {
		ServiceType.FACEBOOK,
		ServiceType.JORLLE_LOCAL,
		ServiceType.PICASA_WEB,
		ServiceType.TWITTER,
	};
	
	private OverlayItem[] mItemIcons;
	private OverlayItem[] mItemTags;

	private int mItemTagId;

	public UxOverlayHelper(OverlayDataSource dataSource) {
		super(dataSource);
	}
	
	public void init(Context context) {
		initIconItems(context);
		initTagItems(context);
		setRibon(context);
	}

	public int getTagOverlayId() {
		return mItemTagId;
	}
	
	private void initIconItems(Context context) {
		Resources resources = context.getResources();
		final int n = SERVICE_TYPES.length;
		mItemIcons = new OverlayItem[n];
		for (int i = 0; i < n; i++) {
			mItemIcons[i] = factItem(context, resources, SERVICE_TYPES[i]);
		}
		addOverlay(mItemIcons);
	}

	private OverlayItem factItem(Context context, Resources resources, String serviceType) {
		if (ServiceType.JORLLE_LOCAL.equals(serviceType)) {
			return new UXGridWidget.OverlayItem(
					resources.getDrawable(R.drawable.icon02), //表示するDrawable
					UXGridWidget.OverlayGrid.POSITION_LEFT_BOTTOM,//表示位置
					25, UXUnit.DP, //幅
					25, UXUnit.DP, //高さ
					2, UXUnit.DP //マージン
					);
		} else {
			return new UXGridWidget.OverlayItem(
					resources.getDrawable(ClientManager.getIconResource(context, serviceType)), //表示するDrawable
					UXGridWidget.OverlayGrid.POSITION_LEFT_BOTTOM,//表示位置
					25, UXUnit.DP, //幅
					25, UXUnit.DP, //高さ
					2, UXUnit.DP //マージン
					);
		}
	}

	private void initTagItems(Context context) {
		mItemTags = new OverlayItem[] {
				new UXGridWidget.OverlayItem(
						context.getResources().getDrawable(R.drawable.img_tagedit), //表示するDrawable
						UXGridWidget.OverlayGrid.POSITION_RIGHT_TOP,//表示位置
						35, UXUnit.DP, //幅
						35, UXUnit.DP, //高さ
						0, UXUnit.DP //マージン
				),
		};
		mItemTagId = addOverlay(mItemTags);
	}

	private void setRibon(Context context) {
		setRibbonWidth(30, UXUnit.DP);
	}

	public int getOverlayNumber(String service, int overlayId) {
		if (overlayId == mItemTagId) {
			return 0;
		} else {
			final int n = SERVICE_TYPES.length;
			for (int i=0; i<n; i++) {
				if (SERVICE_TYPES[i].equals(service))
					return i;
			}
			return -1;
		}
	}

	public boolean isTagLayer(int overlayId) {
		return overlayId == mItemTagId;
	}
}
