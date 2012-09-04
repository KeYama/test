package jp.co.johospace.jsphoto.ux.widget.memory.grid;

import android.graphics.Rect;

import java.util.ArrayList;

/**
 *
 * カテゴリ一覧
 *
 */
class CategoryList {
	ArrayList<Category> mCategoryList;

	void draw(Rect rect){
		int size = mCategoryList.size();

		for(int n = 0; n < size; n++){
			Category category = mCategoryList.get(n);
			category.draw(rect);
		}
	}

	void layout(){
		int height = 0;
		for(Category category: mCategoryList){
			category.layout(height);

			height += category.getHeight();
		}
	}
}
