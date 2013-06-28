package org.rockettrack.views;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.rockettrack.util.ColorGradientHelper;
import org.rockettrack.util.Unit;
import org.rockettrack.util.UnitConverter;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class CompassNaviView extends View 
{
	
	private final static String TAG = "CompassNaviView";

	// Various paints
	private Paint mNorthPaint;
	private Paint mSouthPaint;
	private Paint mTextValuePaint;
	private Paint mTextValuePaintGreen;
	private Paint mTextValuePaintYellow;
	private Paint mTextValuePaintRed;
	private Paint mArrowPaint;
	private Paint mArrowTextPaint;
	private Paint mCenterPaint;

	private CompassLayout mCompassLayout;
	
	private int DeltaYCompassValues = 18;
	private int DeltaYGpsInformation = 65;

	protected final Picture mStaticLayout = new Picture();
	private int mDisplayWidthInPixel;
	private int mDisplayHeightInPixel;
	private float mDisplayScaledDensity;
	
	private float mfltAzimuth = 0.0f;

	// GPS values
	private Location mCurrentLocation;
	private int mintSatellites = 0;
	private SimpleDateFormat mDateFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
	
	// Navigation target
	private NavigationTarget mTarget;

	// Units
	private UnitConverter mUnitConverter = new UnitConverter();
	private Unit mUnitDistance = Unit.meter;
	private Unit mUnitAltitude = Unit.meter;

	private Context mContext;

	/**
	 * 
	 * @param context
	 */
	public CompassNaviView(Context context)
	{
		this(context, null);
		
		this.initViewProperties();
	}
	
	/**
	 * 
	 * @param context
	 * @param attrs
	 */
	public CompassNaviView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
		
		this.initViewProperties();
	}
	
	/**
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public CompassNaviView(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);

		this.initViewProperties();
		
		this.mContext = context;
		this.mDisplayWidthInPixel = this.mContext.getResources().getDisplayMetrics().widthPixels;
		this.mDisplayHeightInPixel = this.mContext.getResources().getDisplayMetrics().heightPixels;
		this.mDisplayScaledDensity = this.mContext.getResources().getDisplayMetrics().scaledDensity;
		
		// String t = "Width: " + this.mDisplayWidthInPixel + " Height: " + this.mDisplayHeightInPixel + " Density: " + this.mDisplayScaledDensity;
		// Toast.makeText(context, t, Toast.LENGTH_LONG).show();
		
		this.mCompassLayout = new CompassLayout(this.mDisplayWidthInPixel, this.mDisplayHeightInPixel, this.mDisplayScaledDensity);

		DeltaYCompassValues = (int)(18 * this.mDisplayScaledDensity);
		DeltaYGpsInformation = (int)(65 * this.mDisplayScaledDensity);

		this.definePaints();
		this.createStaticLayout();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void initViewProperties()
	{
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Required to make overlays work properly!
	}
	/**
	 * 
	 */
	private void definePaints()
	{
		// Compass main paint (outer circle and lines)
		this.mArrowPaint = new Paint();
		this.mArrowPaint.setColor(Color.RED);
		this.mArrowPaint.setAntiAlias(true);
		this.mArrowPaint.setStyle(Style.FILL_AND_STROKE);
		this.mArrowPaint.setStrokeWidth(2.0f);
		
		this.mArrowTextPaint = new Paint();
		this.mArrowTextPaint.setColor(Color.RED);
		this.mArrowTextPaint.setAntiAlias(true);
		this.mArrowTextPaint.setTextSize(12.0f * this.mDisplayScaledDensity);
		this.mArrowTextPaint.setTextAlign(Align.CENTER);
		this.mArrowTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

		// Text value paint
		this.mTextValuePaint = new Paint();
		this.mTextValuePaint.setColor(Color.WHITE);
		this.mTextValuePaint.setAntiAlias(true);
		this.mTextValuePaint.setTextSize(14.0f * this.mDisplayScaledDensity);
		this.mTextValuePaint.setTextAlign(Align.LEFT);

		this.mTextValuePaintYellow = new Paint();
		this.mTextValuePaintYellow.setColor(Color.YELLOW);
		this.mTextValuePaintYellow.setAntiAlias(true);
		this.mTextValuePaintYellow.setTextSize(14.0f * this.mDisplayScaledDensity);
		this.mTextValuePaintYellow.setTextAlign(Align.LEFT);
		
		this.mTextValuePaintGreen = new Paint();
		this.mTextValuePaintGreen.setColor(Color.GREEN);
		this.mTextValuePaintGreen.setAntiAlias(true);
		this.mTextValuePaintGreen.setTextSize(14.0f * this.mDisplayScaledDensity);
		this.mTextValuePaintGreen.setTextAlign(Align.LEFT);
		
		this.mTextValuePaintRed = new Paint();
		this.mTextValuePaintRed.setColor(Color.RED);
		this.mTextValuePaintRed.setAntiAlias(true);
		this.mTextValuePaintRed.setTextSize(14.0f * this.mDisplayScaledDensity);
		this.mTextValuePaintRed.setTextAlign(Align.LEFT);

		// Paint design of north triangle
		this.mNorthPaint = new Paint();
		this.mNorthPaint.setColor(0xFF8B0000);
		this.mNorthPaint.setAntiAlias(true);
		this.mNorthPaint.setStyle(Style.FILL);

		// Paint design of south triangle
		this.mSouthPaint = new Paint();
		this.mSouthPaint.setColor(0xFF00008B);
		this.mSouthPaint.setAntiAlias(true);
		this.mSouthPaint.setStyle(Style.FILL);
		
		this.mCenterPaint = new Paint();
		this.mCenterPaint.setColor(Color.BLACK);
		this.mCenterPaint.setAntiAlias(true);
		this.mCenterPaint.setStyle(Style.FILL);
	}

	/**
	 * 
	 */
	private void createStaticLayout()
	{
		final int width = this.mDisplayWidthInPixel;
		
		final Canvas canvas = mStaticLayout.beginRecording(this.mDisplayWidthInPixel, this.mDisplayHeightInPixel);

		// Define paints used in this method only
		
		// Compass inner circle paint
		final Paint compassInnerPaint = new Paint();
		compassInnerPaint.setColor(Color.DKGRAY);
		compassInnerPaint.setAntiAlias(true);
		compassInnerPaint.setStyle(Style.STROKE);
		compassInnerPaint.setStrokeWidth(2.0f);

		// Text header paint
		final Paint textHeaderPaint = new Paint();
		textHeaderPaint.setColor(Color.LTGRAY);
		textHeaderPaint.setAntiAlias(true);
		textHeaderPaint.setTextSize(10.0f * this.mDisplayScaledDensity);
		textHeaderPaint.setTextAlign(Align.LEFT);

		// Minor coordinates paint (text)
		final Paint coordsMinorPaint = new Paint();
		coordsMinorPaint.setColor(Color.WHITE);
		coordsMinorPaint.setAntiAlias(true);
		coordsMinorPaint.setTextSize(10.0f * this.mDisplayScaledDensity);
		coordsMinorPaint.setTextAlign(Align.CENTER);

		// Main coordinates paint (text)
		final Paint coordsMainPaint = new Paint();
		coordsMainPaint.setColor(0xFFFFA500); // Orange
		coordsMainPaint.setAntiAlias(true);
		coordsMainPaint.setTextSize(12.0f * this.mDisplayScaledDensity);
		coordsMainPaint.setTextAlign(Align.CENTER);

		final Paint compassMainPaint = new Paint();
		compassMainPaint.setColor(Color.GRAY);
		compassMainPaint.setAntiAlias(true);
		compassMainPaint.setStyle(Style.STROKE);
		compassMainPaint.setStrokeWidth(2.0f);
		
		final Paint compassMainPaintOrange = new Paint();
		compassMainPaintOrange.setColor(0xFFFFA500);
		compassMainPaintOrange.setAntiAlias(true);
		compassMainPaintOrange.setStyle(Style.STROKE);
		compassMainPaintOrange.setStrokeWidth(2.0f);

		// Draw target header
		canvas.drawText("Target", this.mCompassLayout.FirstColumnX, this.mCompassLayout.TargetInformationTargetHeaderY, textHeaderPaint);
		canvas.drawText("Latitude", width / 2 - 35 * this.mDisplayScaledDensity, this.mCompassLayout.TargetInformationTargetHeaderY, textHeaderPaint);
		canvas.drawText("Longitude", width - 100 * this.mDisplayScaledDensity, this.mCompassLayout.TargetInformationTargetHeaderY, textHeaderPaint);

		canvas.drawText("Distance", this.mCompassLayout.FirstColumnX, this.mCompassLayout.TargetInformationBearingHeaderY, textHeaderPaint);
		canvas.drawText("Bearing", width - 50 * this.mDisplayScaledDensity, this.mCompassLayout.TargetInformationBearingHeaderY, textHeaderPaint);

		// Draw compass layout (circles)
		canvas.drawCircle(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, this.mCompassLayout.CompassRadius, compassMainPaint);
		canvas.drawCircle(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, this.mCompassLayout.CompassRadiusTwoThirds, compassInnerPaint);
		canvas.drawCircle(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, this.mCompassLayout.CompassRadiusOneThird, compassInnerPaint);

		// Draw compass lines N-S and W-E
		canvas.drawLine(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY - this.mCompassLayout.CompassRadiusTwoThirds + 5 * this.mDisplayScaledDensity, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY + this.mCompassLayout.CompassRadiusTwoThirds - 5 * this.mDisplayScaledDensity, compassMainPaint);
		canvas.drawLine(this.mCompassLayout.CompassCenterX - this.mCompassLayout.CompassRadiusTwoThirds + 5 * this.mDisplayScaledDensity, this.mCompassLayout.CompassCenterY, this.mCompassLayout.CompassCenterX + this.mCompassLayout.CompassRadiusTwoThirds - 5 * this.mDisplayScaledDensity, this.mCompassLayout.CompassCenterY, compassMainPaint);
		
		final int intRadiusOuterMainDegreeValue = this.mCompassLayout.CompassRadius - (int)(18 * this.mDisplayScaledDensity);
		final int intRadiusOuterMinorDegreeValues = this.mCompassLayout.CompassRadius - (int)(15 * this.mDisplayScaledDensity);
		final int intCompassLetterRadius = this.mCompassLayout.CompassRadiusTwoThirds + (int)(10 * this.mDisplayScaledDensity);
		final int intInnerMarkShortRadiusFrom = this.mCompassLayout.CompassRadiusOneThird - (int)(5 * this.mDisplayScaledDensity);
		final int intInnerMarkShortRadiusTo = this.mCompassLayout.CompassRadiusOneThird + (int)(5 * this.mDisplayScaledDensity);
		final int intMiddleMarkShortRadiusFrom = this.mCompassLayout.CompassRadiusTwoThirds - (int)(5 * this.mDisplayScaledDensity);
		final int intMiddleMarkShortRadiusTo = this.mCompassLayout.CompassRadiusTwoThirds + (int)(5 * this.mDisplayScaledDensity);
		final int intOuterMarkShortRadiusFrom = this.mCompassLayout.CompassRadius - (int)(5 * this.mDisplayScaledDensity);
		final int intOuterMarkShortRadiusTo = this.mCompassLayout.CompassRadius + (int)(5 * this.mDisplayScaledDensity);
		
		for (int i = 0; i < 360; i += 15)
		{
			// Separate styles are used for 0,90,180 and 270 degrees
			if ((i % 90) == 0)
			{
				// Draw main coordinate letters (N,S,E,W)
				this.drawRotatedTextOnCircle(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intCompassLetterRadius, i, this.getDirectionForDegrees(i), coordsMainPaint);
				// Draw minor coordinate letters (NE, SE, SW, NW)
				this.drawRotatedTextOnCircle(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intCompassLetterRadius, i + 45, this.getDirectionForDegrees(i + 45), coordsMinorPaint);
				// Draw degree value inside outer line
				this.drawRotatedTextOnCircle(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intRadiusOuterMainDegreeValue, i, Integer.toString(i), coordsMainPaint);
				// Draw short mark lines for N,S,E,W
				this.drawShortLine(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intInnerMarkShortRadiusFrom, intInnerMarkShortRadiusTo, i, compassMainPaintOrange);
				this.drawShortLine(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intMiddleMarkShortRadiusFrom, intMiddleMarkShortRadiusTo, i, compassMainPaintOrange);
				// Draw short marker lines for NE, NW, SE, SW
				this.drawShortLine(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intInnerMarkShortRadiusFrom, intInnerMarkShortRadiusTo, i + 45, compassMainPaint);
				this.drawShortLine(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intMiddleMarkShortRadiusFrom, intMiddleMarkShortRadiusTo, i + 45, compassMainPaint);
				// Draw short marker lines on degree scale (outer line)
				this.drawShortLine(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intOuterMarkShortRadiusFrom, intOuterMarkShortRadiusTo, i, compassMainPaintOrange);
				this.drawShortLine(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intOuterMarkShortRadiusFrom, intOuterMarkShortRadiusTo, i + 45, compassMainPaint);
			} 
			else
			{
				// Draw degree value (15, 30, etc.)
				this.drawRotatedTextOnCircle(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intRadiusOuterMinorDegreeValues, i, Integer.toString(i), coordsMinorPaint);
				// Draw short marks on degree scale (outer line)
				this.drawShortLine(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, intOuterMarkShortRadiusFrom, intOuterMarkShortRadiusTo, i, compassInnerPaint);
			}
		}
		
		// Draw compass value header
		int intY = width + DeltaYCompassValues;
		
		canvas.drawText("Heading", this.mCompassLayout.FirstColumnX, intY, textHeaderPaint);
		canvas.drawText("Orientation", width - 50 * this.mDisplayScaledDensity, intY, textHeaderPaint);
		
		// Draw GPS information
		intY = width + DeltaYGpsInformation;
		
		// First line
		canvas.drawText("Latitude", this.mCompassLayout.FirstColumnX, intY, textHeaderPaint);
		canvas.drawText("Longitude", width / 2 - 40 * this.mDisplayScaledDensity, intY, textHeaderPaint);
		canvas.drawText("Altitude", width - 80 * this.mDisplayScaledDensity, intY, textHeaderPaint);

		// Second line
		canvas.drawText("Satellites", this.mCompassLayout.FirstColumnX, intY + 38 * this.mDisplayScaledDensity, textHeaderPaint);
		canvas.drawText("Accuracy", width / 4 - 10 * this.mDisplayScaledDensity, intY + 38 * this.mDisplayScaledDensity, textHeaderPaint);
		canvas.drawText("Heading", width / 2 - 30 * this.mDisplayScaledDensity, intY + 38 * this.mDisplayScaledDensity, textHeaderPaint);
		canvas.drawText("Last Fix", width - 130 * this.mDisplayScaledDensity, intY + 38 * this.mDisplayScaledDensity, textHeaderPaint);
		
		mStaticLayout.endRecording();	
	}
	
	/**
	 * 
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		canvas.drawPicture(this.mStaticLayout);
		
		this.drawCompass(canvas);
		this.drawCompassValues(canvas, DeltaYCompassValues);
		this.drawTargetInformation(canvas);
		this.drawGpsInformation(canvas, DeltaYGpsInformation);
	}

	/**
	 * 
	 * @param canvas
	 */
	private void drawCompass(Canvas canvas)
	{
		canvas.save();
		canvas.rotate(-this.mfltAzimuth, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY);
		
		// Red triangle pointing north
		final Path pathNorthA = new Path();
		pathNorthA.moveTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY - this.mCompassLayout.CompassRadiusTwoThirds);
		pathNorthA.lineTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY);
		pathNorthA.lineTo(this.mCompassLayout.CompassCenterX - this.mCompassLayout.CompassTriangleWidth, this.mCompassLayout.CompassCenterY);
		pathNorthA.lineTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY - this.mCompassLayout.CompassRadiusTwoThirds);
		pathNorthA.close();
		this.mNorthPaint.setColor(0xCC8B0000);
		canvas.drawPath(pathNorthA, this.mNorthPaint);
		final Path pathNorthB = new Path();
		pathNorthB.moveTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY - this.mCompassLayout.CompassRadiusTwoThirds);
		pathNorthB.lineTo(this.mCompassLayout.CompassCenterX + this.mCompassLayout.CompassTriangleWidth, this.mCompassLayout.CompassCenterY);
		pathNorthB.lineTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY);
		pathNorthB.lineTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY - this.mCompassLayout.CompassRadiusTwoThirds);
		pathNorthB.close();
		this.mNorthPaint.setColor(0xCC610000);
		canvas.drawPath(pathNorthB, this.mNorthPaint);
		
		// Blue triangle pointing south
		final Path pathSouthA = new Path();
		pathSouthA.moveTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY + this.mCompassLayout.CompassRadiusTwoThirds);
		pathSouthA.lineTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY);
		pathSouthA.lineTo(this.mCompassLayout.CompassCenterX - this.mCompassLayout.CompassTriangleWidth, this.mCompassLayout.CompassCenterY);
		pathSouthA.lineTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY + this.mCompassLayout.CompassRadiusTwoThirds);
		pathSouthA.close();
		this.mSouthPaint.setColor(0xCC000061);
		canvas.drawPath(pathSouthA, this.mSouthPaint);
		final Path pathSouthB = new Path();
		pathSouthB.moveTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY + this.mCompassLayout.CompassRadiusTwoThirds);
		pathSouthB.lineTo(this.mCompassLayout.CompassCenterX + this.mCompassLayout.CompassTriangleWidth, this.mCompassLayout.CompassCenterY);
		pathSouthB.lineTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY);
		pathSouthB.lineTo(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY + this.mCompassLayout.CompassRadiusTwoThirds);
		pathSouthB.close();
		this.mSouthPaint.setColor(0xCC00008B);
		canvas.drawPath(pathSouthB, this.mSouthPaint);

		canvas.restore();

		// Draw a black circle in the middle
		canvas.drawCircle(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, 4 * this.mDisplayScaledDensity, this.mCenterPaint);

		// Draw arrow pointing to the destination
		if (this.mCurrentLocation != null && this.mTarget != null)
		{
			final float fltBearing = this.mCurrentLocation.bearingTo(this.mTarget);
			final float fltDistance = this.mCurrentLocation.distanceTo(this.mTarget);
			final float fltArrowDegrees = -this.mfltAzimuth + fltBearing;

			// Get color for the distance
			final int color = ColorGradientHelper.getRedGreenGradient(1000, 5, fltDistance, 0xff);
			
			// Set arrow color according to distance
			this.mArrowPaint.setColor(color);
			this.drawShortLine(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, this.mCompassLayout.CompassRadiusTwoThirds, this.mCompassLayout.CompassRadius - (int)(10 * this.mDisplayScaledDensity), fltArrowDegrees, this.mArrowPaint);

			// Create the tip of the arrow
			final Path pathArrow = new Path();
			final Point pointStart = this.calculatePointOnCircle(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, this.mCompassLayout.CompassRadius, fltArrowDegrees);
			pathArrow.moveTo(pointStart.x, pointStart.y);
			Point point = this.calculatePointOnCircle(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, this.mCompassLayout.CompassRadius - (int)(10 * this.mDisplayScaledDensity), fltArrowDegrees - 3);
			pathArrow.lineTo(point.x, point.y);
			point = this.calculatePointOnCircle(this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, this.mCompassLayout.CompassRadius - (int)(10 * this.mDisplayScaledDensity), fltArrowDegrees + 3);
			pathArrow.lineTo(point.x, point.y);
			pathArrow.lineTo(pointStart.x, pointStart.y);
			pathArrow.close();
			canvas.drawPath(pathArrow, this.mArrowPaint);
			
			// Draw distance on arrow
			final String strDistance = this.getDistance(fltDistance);
			this.mArrowTextPaint.setColor(color);
			this.drawRotatedTextOnCircle(canvas, this.mCompassLayout.CompassCenterX, this.mCompassLayout.CompassCenterY, this.mCompassLayout.CompassRadius + (int)(5 * this.mDisplayScaledDensity), fltArrowDegrees, strDistance, this.mArrowTextPaint);

			// Write distance and bearing information into Header section
			canvas.drawText(strDistance, this.mCompassLayout.FirstColumnX, this.mCompassLayout.TargetInformationBearingValueY, this.mTextValuePaint);
			canvas.drawText(String.format("%.0f¡", fltBearing), this.mDisplayWidthInPixel - 50 * this.mDisplayScaledDensity, this.mCompassLayout.TargetInformationBearingValueY, this.mTextValuePaint);
		}
	}

	/**
	 * 
	 * @param canvas
	 */
	private void drawCompassValues(Canvas canvas, int intDeltaY)
	{
		final int intWidth = this.getWidth();
		final int intY = intWidth;
		
		canvas.drawText(String.format("%.0f¡", this.mfltAzimuth), this.mCompassLayout.FirstColumnX, intY + intDeltaY + 16 * this.mDisplayScaledDensity, this.mTextValuePaint);
		canvas.drawText(this.getDirectionForDegrees(this.mfltAzimuth), intWidth - 50 * this.mDisplayScaledDensity, intY + intDeltaY + 16 * this.mDisplayScaledDensity, this.mTextValuePaint);
	}
	
	/**
	 * 
	 * @param canvas
	 */
	private void drawTargetInformation(Canvas canvas)
	{
		if ( this.mTarget == null ) {
			return;
		}
		final int intWidth = this.getWidth();		
		final CoordinateHelper coordinateHelper = new CoordinateHelper(this.mTarget.getLatitude(), this.mTarget.getLongitude());

		canvas.drawText(this.mTarget.getName(), this.mCompassLayout.FirstColumnX, this.mCompassLayout.TargetInformationTargetValueY, this.mTextValuePaint);
		canvas.drawText(coordinateHelper.getLatitudeString(), intWidth / 2 - 35 * this.mDisplayScaledDensity, this.mCompassLayout.TargetInformationTargetValueY, this.mTextValuePaint);
		canvas.drawText(coordinateHelper.getLongitudeString(), intWidth - 100 * this.mDisplayScaledDensity, this.mCompassLayout.TargetInformationTargetValueY, this.mTextValuePaint);
	}

	/**
	 * 
	 * @param canvas
	 * @param intDeltaY
	 */
	private void drawGpsInformation(Canvas canvas, int intDeltaY)
	{
		final int intWidth = this.getWidth();

		// First line
		if (this.mCurrentLocation != null)
		{
			final CoordinateHelper coordinateHelper = new CoordinateHelper(this.mCurrentLocation.getLatitude(), this.mCurrentLocation.getLongitude());
			canvas.drawText(coordinateHelper.getLatitudeString(), this.mCompassLayout.FirstColumnX, intWidth + intDeltaY + 16 * this.mDisplayScaledDensity, this.mTextValuePaint);
			canvas.drawText(coordinateHelper.getLongitudeString(), intWidth / 2 - 40 * this.mDisplayScaledDensity, intWidth + intDeltaY + 16 * this.mDisplayScaledDensity, this.mTextValuePaint);
			final String strAltitude = this.getAltitude(this.mCurrentLocation.getAltitude());
			canvas.drawText(strAltitude, intWidth - 80 * this.mDisplayScaledDensity, intWidth + intDeltaY + 16 * this.mDisplayScaledDensity, this.mTextValuePaint);
		}

		// Second line
		final Paint satellitesPaint = this.getSatellitePaint();
		canvas.drawText(Integer.toString(this.mintSatellites), 10 * this.mDisplayScaledDensity, intWidth + intDeltaY + 54 * this.mDisplayScaledDensity, satellitesPaint);					
		if (this.mCurrentLocation != null)
		{
			final Paint accuracyPaint = this.getAccuracyPaint();
			canvas.drawText(String.format("%3.0fm", this.mCurrentLocation.getAccuracy()), intWidth / 4 - 10 * this.mDisplayScaledDensity, intWidth + intDeltaY + 54 * this.mDisplayScaledDensity, accuracyPaint);
			canvas.drawText(String.format("%3.0f\u00B0", this.mCurrentLocation.getBearing()), intWidth / 2 - 30 * this.mDisplayScaledDensity, intWidth + intDeltaY + 54 * this.mDisplayScaledDensity, this.mTextValuePaint);
			final Calendar cal = Calendar.getInstance();
			cal.setTimeZone(TimeZone.getTimeZone("UTC"));
			cal.setTimeInMillis(this.mCurrentLocation.getTime());
			cal.setTimeZone(TimeZone.getDefault());
			canvas.drawText(mDateFormat.format(cal.getTime()), intWidth - 130 * this.mDisplayScaledDensity, intWidth + intDeltaY + 54 * this.mDisplayScaledDensity, this.mTextValuePaint);
		}
	}

	/**
	 * 
	 * @return
	 */
	private Paint getSatellitePaint()
	{
		if (this.mintSatellites < 4)
			return this.mTextValuePaintRed;
		else if (this.mintSatellites < 7)
			return this.mTextValuePaintYellow;
		else
			return this.mTextValuePaintGreen;
	}

	/**
	 * 
	 * @return
	 */
	private Paint getAccuracyPaint()
	{
		final float fltAccuracy = this.mCurrentLocation.getAccuracy();
		if (fltAccuracy <= 8)
			return this.mTextValuePaintGreen;
		else if (fltAccuracy <= 30)
			return this.mTextValuePaintYellow;
		else
			return this.mTextValuePaintRed;
	}

	/**
	 * 
	 * @param fltDegrees
	 * @return
	 */
    private String getDirectionForDegrees(float fltDegrees)
    {
        if ((fltDegrees >= 337.5) || (fltDegrees < 22.5))
            return "N";
        else if ((fltDegrees >= 22.5) && (fltDegrees < 67.5))
            return "NE";
        else if ((fltDegrees >= 67.5) && (fltDegrees < 112.5))
            return "E";
        else if ((fltDegrees >= 112.5) && (fltDegrees < 157.5))
            return "SE";
        else if ((fltDegrees >= 157.5) && (fltDegrees < 202.5))
            return "S";
        else if ((fltDegrees >= 202.5) && (fltDegrees < 247.5))
            return "SW";
        else if ((fltDegrees > 247.5) && (fltDegrees < 292.5))
            return "W";
        else if ((fltDegrees >= 292.5) && (fltDegrees < 337.5))
            return "NW";
        else
            return "N";     // this is some default value if bearing = NaN
    }

    /**
     * 
     * @param distanceInMeters
     * @return
     */
    private String getDistance(Float distanceInMeters)
    {
		String strDistance = "";
		switch (this.mUnitDistance)
		{
		case meter:
			if (distanceInMeters > 1000)
				strDistance = String.format("%.2fkm", distanceInMeters / 1000);
			else
				strDistance = String.format("%.1fm", distanceInMeters);
			break;
		case feet:
			final Float fltDistanceInFeet = this.mUnitConverter.convert(Unit.meter, Unit.feet, distanceInMeters);
			strDistance=String.format("%.1fft", fltDistanceInFeet);
			break;
		}
    	
		return strDistance;
    }
    
    /**
     * 
     * @param altitudeInMeters
     * @return
     */
    private String getAltitude(Double altitudeInMeters)
    {
		String strAltitude = "";
		switch (this.mUnitAltitude)
		{
		case meter:
			strAltitude = String.format("%4.0fm", altitudeInMeters);
			break;
		case feet:
			Double dblAltitudeInFeet = this.mUnitConverter.convert(Unit.meter, Unit.feet, altitudeInMeters);
			strAltitude=String.format("%5.0fft", dblAltitudeInFeet);
			break;
		}
    	
		return strAltitude;
    }
    
	/**
	 * 
	 * @param canvas
	 * @param intCenterX
	 * @param intCenterY
	 * @param intRadius
	 * @param fltDegrees
	 * @param strText
	 * @param paint
	 */
	private void drawRotatedTextOnCircle(Canvas canvas, int intCenterX, int intCenterY, int intRadius, float fltDegrees, String strText, Paint paint)
	{
		canvas.save();
		final Point point = this.calculatePointOnCircle(intCenterX, intCenterY, intRadius, fltDegrees);
		canvas.rotate(fltDegrees, point.x, point.y);
		canvas.drawText(strText, point.x, point.y, paint);
		canvas.restore();
	}

	/**
	 * 
	 * @param canvas
	 * @param intCenterX
	 * @param intCenterY
	 * @param intInnerRadius
	 * @param intOuterRadius
	 * @param fltDegrees
	 * @param paint
	 */
	private void drawShortLine(Canvas canvas, int intCenterX, int intCenterY, int intInnerRadius, int intOuterRadius, float fltDegrees, Paint paint)
	{
		final Point pointInner = this.calculatePointOnCircle(intCenterX, intCenterY, intInnerRadius, fltDegrees);
		final Point pointOuter = this.calculatePointOnCircle(intCenterX, intCenterY, intOuterRadius, fltDegrees);
		canvas.drawLine(pointInner.x, pointInner.y, pointOuter.x, pointOuter.y, paint);
	}
	
	/**
	 * 
	 * @param intCenterX
	 * @param intCenterY
	 * @param intRadius
	 * @param degrees
	 * @return
	 */
	private Point calculatePointOnCircle(int intCenterX, int intCenterY, int intRadius, float degrees)
	{
		// for trigonometry, 0 is pointing east, so subtract 90
		// compass degrees are the wrong way round
		final double dblRadians = Math.toRadians(-degrees + 90);
		
		final int intX = (int) (intRadius * Math.cos(dblRadians));
		final int intY = (int) (intRadius * Math.sin(dblRadians));

		return new Point(intCenterX + intX, intCenterY - intY);
	}

	/**
	 * 
	 * @param latitude
	 * @param longitude
	 * @param name
	 */
	public void setTarget(final NavigationTarget target)
	{
		this.mTarget = target;
		this.postInvalidate();
	}

	/**
	 * 
	 * @param unit
	 */
	public void setUnitForDistance(Unit unit)
	{
		this.mUnitDistance = unit;
	}

	/**
	 * 
	 * @param unit
	 */
	public void setUnitForAltitude(Unit unit)
	{
		this.mUnitAltitude = unit;	
	}
	
	/**
	 * 
	 * @param count
	 */
	public void setSatelliteCount(int count)
	{
		this.mintSatellites = count;
	}
	
	/**
	 * 
	 * @param azimuth
	 */
	public void setAzimuth(float azimuth)
	{
		this.mfltAzimuth = azimuth;
	}
	
	/**
	 * 
	 * @param location
	 */
	public void setLocation(Location location)
	{
		this.mCurrentLocation = location;
	}
}
