package adapter;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;

public abstract class DTableAdapter implements DAdapter {

	private DataSetObservable mDataObservable = new DataSetObservable();
	private Context mContext;

	public DTableAdapter(Context pContext) {
		mContext = pContext;
	}

	public Context getContext() {
		return mContext;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mDataObservable.registerObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataObservable.unregisterObserver(observer);
	}
}
