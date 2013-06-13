package org.rockettrack;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

public class ConsoleOutputView extends ImageView {

	private Queue<String> lines = new ArrayBlockingQueue<String>(100);

	private TextView lineView;
	
	public ConsoleOutputView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		lineView = new TextView(context, attrs, defStyle);
	}

	public ConsoleOutputView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ConsoleOutputView(Context context) {
		this(context,null);
	}

	public void addLine(String s ) {
		if ( lines.offer(s)  == true ) {
			return;
		}
		lines.poll();
		lines.add(s);
	}

	@Override
	protected void onDraw(Canvas canvas) {

		Paint textPaint = lineView.getPaint();
		int baseline = Math.round(textPaint.getFontSpacing());
		
		int y=0;
		for( String s : lines.toArray( new String[0])) {
			canvas.drawText(s,0,y,textPaint);
			y+=baseline;
		}
	}

}
