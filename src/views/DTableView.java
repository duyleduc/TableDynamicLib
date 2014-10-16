package views;

import java.util.ArrayList;
import java.util.List;

import tools.DViewRecycle;
import adapter.DAdapter;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.example.tabledatabaselib.R;

public class DTableView extends ViewGroup {

	//@formatter:off
	private List<View> 				mFirstRowViews;
	private List<List<View>> 		mTableViews;
	
	private DAdapter 				mAdapter;
	private DDataTableObserver      mTableObserver;
	private DViewRecycle			mRecyclesView;
	
	// Touch's event
	private int 					mCurrentX;
	private int 					mCurrentY;
	
	// Touch's animation
	private int 					mTouchSlop; // touch slop when moving view
	private int 					mMimVelocity;
	private int 					mMaxVelicity;
	private VelocityTracker 		mVelocityTracker;

	// Table's dimension
	private int 					mHeight; 
	private int 					mWidth;
	private int 					mScrollX;
	private int 					mFirstColumn;
	private int						mFirstRow;
	private int 					mColumnCount;
	private int 					mRowCount;
	
	// table's childrens (row or width dim)
	private int[]					mHeights;
	private int[]					mWidths;
	
	
	private boolean					mNeedRelayout;
	//@formatter:on
	public DTableView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mFirstRowViews = new ArrayList<View>();
		mTableViews = new ArrayList<List<View>>();
		mFirstColumn = 0;
		mFirstRow = 1;
		initConfig();
		initTouchParams();
	}

	private void initConfig() {
		ViewConfiguration tConfig = ViewConfiguration.get(getContext());
		mTouchSlop = tConfig.getScaledTouchSlop();
		mMimVelocity = tConfig.getScaledMinimumFlingVelocity();
		mMaxVelicity = tConfig.getScaledMaximumFlingVelocity();
		mNeedRelayout = false;
	}

	private void initTouchParams() {
		mScrollX = 0;
	}

	/**
	 * Position all children within this layout
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		if (mNeedRelayout || changed) {

			Log.e("order", "onLayout");
			mNeedRelayout = false;
			resetTable();
			if (mAdapter != null) {
				// Recalculate layout's dimensions
				mWidth = r - l;
				mHeight = b - t;

				int top, right, left, bottom;

				right = Math.min(mWidth, sumDims(mWidths));
				bottom = Math.min(mHeight, sumDims(mHeights));

				// add first row
				left = 0 - mScrollX;
				for (int i = 0; left < mWidth && i < mColumnCount; i++) {
					right = left + mWidths[i];
					View tView = makeAndSetupView(0, i, left, 0, right,
							mHeights[0]);
					mFirstRowViews.add(tView);
					left = right;
				}

				// add another views
				top = mHeights[0];
				left = mWidths[0] - mScrollX;

				bottom = mHeights[0];

				for (int i = 1; bottom < mHeight && i < mRowCount; i++) {
					bottom = top + mHeights[i];
					List<View> tViews = new ArrayList<View>();
					left = 0;
					for (int j = 0; left < mWidth && j < mColumnCount; j++) {
						right = left + mWidths[j];
						View tView = makeAndSetupView(i, j, left, top, right,
								bottom);
						tViews.add(tView);
						left = right;
					}
					top = bottom;
					mTableViews.add(tViews);
				}
			}
		}
	}

	private View makeAndSetupView(int row, int column, int l, int t, int r,
			int b) {
		int w = r - l;
		int h = b - t;
		View rView = createView(row, column, w, h);
		rView.layout(l, t, r, b);
		return rView;
	}

	private View createView(int row, int column, int w, int h) {

		View tRecycledView = null;

		tRecycledView = mRecyclesView.getRecycleView();
		View rView = mAdapter.getView(row, column, tRecycledView, this);
		rView.setTag(R.id.id_view_row, row);
		rView.setTag(R.id.id_view_column, column);
		rView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(h, MeasureSpec.UNSPECIFIED));
		addViewToTable(row, column, rView);
		return rView;
	}

	private void addViewToTable(int row, int column, View pView) {
		addView(pView);
	}

	private int sumDims(int[] arrays) {
		int tSum = 0;
		if (arrays != null) {
			for (int tVal : arrays) {
				tSum += tVal;
			}
		}
		return tSum;
	}

	private int sumDimsFor(int from, int to, int[] arrays) {
		int sum = 0;
		for (int i = from; i < to; i++) {
			sum += arrays[i];
		}
		return sum;
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		return super.drawChild(canvas, child, drawingTime);
	}

	/**
	 * 
	 * Ask all children to measure themselves and compute the measurement of
	 * this layout based on the children.
	 * 
	 * @param widthMeasureSpec
	 * @param heightMeasureSpec
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);

		int w, h;

		if (mAdapter != null) {
			mColumnCount = mAdapter.columnCount();
			mRowCount = mAdapter.rowCount();

			// calculate each column's width
			mWidths = new int[mColumnCount];
			for (int i = 0; i < mColumnCount; i++) {
				mWidths[i] = mAdapter.getWidth(i);
			}

			// calculate each row's height
			mHeights = new int[mRowCount];
			for (int i = 0; i < mRowCount; i++) {
				mHeights[i] = mAdapter.getHeight(i);
			}

			// calculate total width

			// http://stackoverflow.com/questions/12266899/onmeasure-custom-view-explanation
			// for more infos about measurespec
			if (widthMode == MeasureSpec.AT_MOST) { // wrap_content or
													// match_parent
				w = Math.min(width, sumDims(mWidths));
			} else if (widthMode == MeasureSpec.UNSPECIFIED) { // wrap_content
				w = sumDims(mWidths);
			} else { // match_parent
				w = width;
				int widths = sumDims(mWidths);

				if (widths < w) {
					float factorWidth = w / widths;

					for (int i = 0; i < mColumnCount; i++) {
						mWidths[i] *= factorWidth;
					}
				}

			}

			// calculate total height
			if (heightMode == MeasureSpec.AT_MOST) {
				h = Math.min(height, sumDims(mHeights));
			} else if (heightMode == MeasureSpec.UNSPECIFIED) {
				h = sumDims(mHeights);
			} else {
				h = height;
				int heights = sumDims(mHeights);
				if (heights < h) {
					float factorHeight = h / heights;
					for (int i = 0; i < mRowCount; i++) {
						mHeights[i] *= factorHeight;
					}
				}
			}
		} else {
			if (widthMode == MeasureSpec.AT_MOST
					|| widthMode == MeasureSpec.UNSPECIFIED) {
				w = 0;
				h = 0;
			} else {
				w = width;
				h = height;
			}
		}
		setMeasuredDimension(w, h);
	}

	/**
	 * When set an adapter, it must re-initialize all attributes
	 * 
	 * @param pAdapter
	 *            Table adapter
	 */
	public void setAdapter(DAdapter pAdapter) {
		mAdapter = pAdapter;
		mTableObserver = new DDataTableObserver();
		mAdapter.registerDataSetObserver(mTableObserver);
		mRecyclesView = new DViewRecycle();
		mTableObserver.onChanged();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		boolean intercept = false;

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mCurrentX = (int) ev.getX();
			mCurrentY = (int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			int tXTranslation = Math.abs(mCurrentX - (int) ev.getX());
			int tYTranslation = Math.abs(mCurrentY - (int) ev.getY());

			if (tXTranslation > mTouchSlop || tYTranslation > mTouchSlop) {
				intercept = true;
			}
		default:
			break;
		}

		return intercept;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mCurrentX = (int) event.getX();
			mCurrentY = (int) event.getY();
			break;
		case MotionEvent.ACTION_MOVE:

			int tXTranslation = mCurrentX - (int) event.getX();
			int tYTranslation = mCurrentY - (int) event.getY();
			// mCurrentX = (int) event.getX();
			// mCurrentY = (int) event.getY();
			Log.e("value of translations", tXTranslation + ":" + tYTranslation);
			scrollBy(tXTranslation, tYTranslation);
			break;
		case MotionEvent.ACTION_UP:
			break;
		default:
			break;
		}

		return true;
	}

	private void resetTable() {
		mFirstRowViews.clear();
		mTableViews.clear();

		removeAllViews();
	}

	@Override
	public void scrollBy(int x, int y) {

		boolean shouldRepositionView = true;
		if (mNeedRelayout)
			return;
		mScrollX += x;
		recalculateScrollValues();
		if (mScrollX == 0) {
			// do nothing
		} else if (mScrollX < 0) { // swipe to right
			if (mFirstColumn != 0) {
				while (!mFirstRowViews.isEmpty()
						&& getFilledWidth()
								- mWidths[mFirstColumn + mFirstRowViews.size()
										- 1] >= mWidth) {
					removeColumnRight();
				}

				while (mScrollX < 0 && mFirstColumn != 0) {
					addColumnLeft();
					mFirstColumn--;
					mScrollX += mWidths[mFirstColumn];
				}
			}
		} else { // swipe to left
			if (mScrollX > mTouchSlop) {

				while (mFirstColumn < mColumnCount
						&& mWidths[mFirstColumn] < mScrollX) {
					mScrollX -= mWidths[mFirstColumn];
					if (!mFirstRowViews.isEmpty() && getFilledWidth() >= mWidth) {
						mFirstColumn++;
						removeColumnLeft();
					}
				}
				while (getFilledWidth() < mWidth
						&& mFirstColumn + mFirstRowViews.size() < mColumnCount) {
					addColumnRight();
				}
				if (mFirstColumn + mFirstRowViews.size() >= mColumnCount) {
					mScrollX = 0;
				}
			}
		}
		if (shouldRepositionView)
			repositionViews();
	}

	private void recalculateScrollValues() {
		mScrollX = desiredScrollValueOf(mScrollX, mWidth, mWidths, 0,
				mFirstColumn);
	}

	private int desiredScrollValueOf(int scrollCurrent, int viewSize,
			int[] dims, int fromLocation, int toLocation) {
		if (scrollCurrent == 0) {
		} else if (scrollCurrent < 0) {
			scrollCurrent = Math.max(scrollCurrent,
					-sumDimsFor(fromLocation, toLocation, dims));
		} else {
			scrollCurrent = Math.min(
					scrollCurrent,
					Math.max(0, sumDimsFor(fromLocation, dims.length - 1, dims)
							- viewSize));
		}
		return scrollCurrent;
	}

	@Override
	public void scrollTo(int x, int y) {
		if (mNeedRelayout) {
			mScrollX = x;
			mFirstColumn = 0;
			mFirstRow = 0;
		} else {
			scrollBy(x - sumDimsFor(0, mFirstColumn, mWidths) - mScrollX, y
					- sumDimsFor(0, mFirstRow, mHeights));
		}
	}

	private void addColumnLeft() {
		View tView = createView(0, mFirstColumn, mWidths[mFirstColumn],
				mHeights[0]);
		mFirstRowViews.add(0, tView);
		int i = mFirstRow;
		for (List<View> list : mTableViews) {
			tView = createView(i, mFirstColumn, mWidths[mFirstColumn],
					mHeights[i]);
			i++;
			list.add(0, tView);
		}
	}

	private void removeColumnRight() {
		int columntToDelete = mFirstRowViews.size() - 1;
		removeColumnAt(columntToDelete);
	}

	private void addColumnRight() {
		int column = mFirstColumn + mFirstRowViews.size();
		View tView = createView(0, column, mWidths[column], mHeights[0]);
		mFirstRowViews.add(tView);

		int i = mFirstRow;
		for (List<View> list : mTableViews) {
			tView = createView(i, column, mWidths[column], mHeights[i]);
			list.add(tView);
			i++;
		}
	}

	private int getFilledWidth() {
		int sum = 0;
		for (int i = mFirstColumn; i < mFirstColumn + mFirstRowViews.size(); i++) {
			sum += mWidths[i];
		}
		return sum;
	}

	private void repositionViews() {
		int top, left, bottom, right;
		left = 0 - mScrollX;
		int index = mFirstColumn;

		for (View tView : mFirstRowViews) {
			right = left + mWidths[index];
			index++;
			tView.layout(left, 0, right, mHeights[0]);
			left = right;
		}

		int i = mFirstRow;
		top = mHeights[0];
		for (List<View> list : mTableViews) {

			left = 0 - mScrollX;
			index = mFirstColumn;

			bottom = top + mHeights[i];
			for (View tView : list) {
				right = left + mWidths[index];
				index++;
				tView.layout(left, top, right, bottom);
				left = right;
			}
			i++;
			top = bottom;
		}

		invalidate();
	}

	private void removeColumnLeft() {
		removeColumnAt(0);
	}

	private void removeColumnAt(int location) {
		View tView = mFirstRowViews.remove(location);
		removeView(tView);
		for (List<View> list : mTableViews) {
			tView = list.remove(location);
			removeView(tView);
		}
	}

	@Override
	public void removeView(View view) {
		super.removeView(view);
		mRecyclesView.addRecycleView(view);
	}

	private class DDataTableObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			mNeedRelayout = true;
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			// doing nothing here
		}
	}

}
