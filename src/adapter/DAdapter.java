package adapter;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

public interface DAdapter {

	public View getView(int rowLocation, int columnLocation, View convertView,
			ViewGroup parent);

	public void registerDataSetObserver(DataSetObserver observer);

	public void unregisterDataSetObserver(DataSetObserver observer);

	public int getWidth(int columnLocation);

	public int getHeight(int rowLocation);

	public int rowCount();

	public int columnCount();

	public int itemTypeCount();
}
