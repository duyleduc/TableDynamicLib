package tools;

import java.util.Stack;

import android.view.View;

public class DViewRecycle {

	private Stack<View> mViewRecycles;

	public DViewRecycle() {
		mViewRecycles = new Stack<View>();
	}

	public void addRecycleView(View pView) {
		mViewRecycles.push(pView);
	}

	public View getRecycleView() {
		if (mViewRecycles.size() == 0)
			return null;
		return mViewRecycles.pop();
	}
}
