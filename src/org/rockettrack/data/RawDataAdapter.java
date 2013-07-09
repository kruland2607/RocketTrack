package org.rockettrack.data;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;

public class RawDataAdapter extends BaseAdapter implements Adapter {

	private static String TAG = "RawDataAdapter";
	
	private Queue<String> lines = new ArrayBlockingQueue<String>(200);

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

	public void resizeLines( int newSize ) {
		lines = new ArrayBlockingQueue<String>(newSize);
	}

	public void addRawData( String data ) {
		if ( lines.offer(data)  == true ) {
			return;
		}
		lines.poll();
		lines.add(data);
		//Log.d(TAG,"raw data added");
		this.notifyDataSetChanged();
	}
	
	public String[] getRawData() {
		return lines.toArray( new String[0] );
	}
	
	public void clearRawData() {
		lines.clear();
		this.notifyDataSetChanged();
	}
	
}
