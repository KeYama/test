package jp.co.johospace.jsphoto.ux.widget;

/**
 *
 * addされたWidgetをそのまま積み上げるコンテナ
 * ワールド座標を変更する。
 *
 */
public class UXStackContainer extends UXContainer{
	@Override
	public void layout(UXStage stage) {
		super.layout(stage);

		int size = mChildren.size();
		float currentY = 0;

		for(int n = 0; n < size; ++n){
			UXWidget child = mChildren.get(n);

			child.worldPosition(0, currentY);
			currentY += child.getHeight() + child.getY();
		}

		mHeightPx = currentY;
	}
}
