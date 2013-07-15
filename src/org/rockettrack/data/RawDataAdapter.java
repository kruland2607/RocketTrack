package org.rockettrack.data;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;

public class RawDataAdapter extends BaseAdapter implements Adapter {

	private static String TAG = "RawDataAdapter";
	
	private List<String> lines = new ArrayList<String>(200);

	@Override
	public int getCount() {
		return lines.size();
	}

	@Override
	public boolean isEmpty() {
		return lines.size() == 0;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addRawData( String data ) {
		lines.add(data);
		//Log.d(TAG,"raw data added");
		this.notifyDataSetChanged();
	}
	
	public List<String> getRawData() {
		return lines;
	}
	
	public void clearRawData() {
		lines.clear();
		this.notifyDataSetChanged();
	}
	
}
