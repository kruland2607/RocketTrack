package org.rockettrack.views;

import java.util.List;

import org.rockettrack.RocketTrackState;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

public class ConsoleOutputView extends ImageView {

	private TextView lineView;
	private int numberOfLines;
	
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

	@Override
	protected void onDraw(Canvas canvas) {

		Paint textPaint = lineView.getPaint();
		int baseline = computeBaseLine();
		
		List<String> lines = RocketTrackState.getInstance().getRawDataAdapter().getRawData();
		int firstLine = lines.size() - numberOfLines;
		if ( firstLine < 0 ) {
			firstLine = 0;
		}
		int y= -1* Math.round( textPaint.getFontMetrics().top ) ;
		for( int i = firstLine; i< lines.size(); i++ ) {
			String s = lines.get(i);
			canvas.drawText(s,0,y,textPaint);
			y+=baseline;
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		numberOfLines = h / computeBaseLine() - 1;
	}

	private int computeBaseLine() {
		Paint textPaint = lineView.getPaint();
		int baseline = Math.round(textPaint.getFontSpacing());
		return baseline;
	}
}
