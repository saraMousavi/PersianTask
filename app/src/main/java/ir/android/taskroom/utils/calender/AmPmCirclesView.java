package ir.android.taskroom.utils.calender;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

import ir.android.taskroom.R;


public class AmPmCirclesView extends View {
    private static final String TAG = "AmPmCirclesView";

    // Alpha level for selected circle.
    private static final int SELECTED_ALPHA = Utils.SELECTED_ALPHA;
    private static final int SELECTED_ALPHA_THEME_DARK = Utils.SELECTED_ALPHA_THEME_DARK;

    private final Paint mPaint = new Paint();
    private int mSelectedAlpha;
    private int mTouchedColor;
    private int mUnselectedColor;
    private int mAmPmTextColor;
    private int mAmPmSelectedTextColor;
    private int mSelectedColor;
    private float mCircleRadiusMultiplier;
    private float mAmPmCircleRadiusMultiplier;
    private String mAmText;
    private String mPmText;
    private boolean mIsInitialized;

    private static final int AM = TimePickerDialogs.AM;
    private static final int PM = TimePickerDialogs.PM;

    private boolean mDrawValuesReady;
    private int mAmPmCircleRadius;
    private int mAmXCenter;
    private int mPmXCenter;
    private int mAmPmYCenter;
    private int mAmOrPm;
    private int mAmOrPmPressed;
    private Context context;
    private String fontName="DroidNaskh-Regular";

    public AmPmCirclesView(Context context, String fontName) {
        super(context);
        this.context = context;
        mIsInitialized = false;
        this.fontName = fontName;
    }

    public void initialize(Context context, int amOrPm, String fontName) {
        if (mIsInitialized) {
            Log.e(TAG, "AmPmCirclesView may only be initialized once.");
            return;
        }
        this.fontName = fontName;

        Resources res = context.getResources();
        mUnselectedColor = res.getColor(R.color.white);
        mSelectedColor = res.getColor(R.color.mdtp_accent_color);
        mTouchedColor = res.getColor(R.color.mdtp_accent_color_dark);
        mAmPmTextColor = res.getColor(R.color.mdtp_ampm_text_color);
        mAmPmSelectedTextColor = res.getColor(R.color.white);
        mSelectedAlpha = SELECTED_ALPHA;
        mPaint.setTypeface(TypefaceHelper.get(context, fontName));
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.CENTER);

        mCircleRadiusMultiplier =
                Float.parseFloat(res.getString(R.string.mdtp_circle_radius_multiplier));
        mAmPmCircleRadiusMultiplier =
                Float.parseFloat(res.getString(R.string.mdtp_ampm_circle_radius_multiplier));
        mAmText = getContext().getString(R.string.am_label);
        mPmText = getContext().getString(R.string.pm_label);

        setAmOrPm(amOrPm);
        mAmOrPmPressed = -1;

        mIsInitialized = true;
    }

    /* package */ void setTheme(Context context, boolean themeDark) {
        Resources res = context.getResources();
        if (themeDark) {
            mUnselectedColor = res.getColor(R.color.mdtp_circle_background_dark_theme);
            mSelectedColor = res.getColor(R.color.mdtp_red);
            mAmPmTextColor = res.getColor(R.color.white);
            mSelectedAlpha = SELECTED_ALPHA_THEME_DARK;
        } else {
            mUnselectedColor = res.getColor(R.color.white);
            mSelectedColor = res.getColor(R.color.mdtp_accent_color);
            mAmPmTextColor = res.getColor(R.color.mdtp_ampm_text_color);
            mSelectedAlpha = SELECTED_ALPHA;
        }
    }

    public void setAmOrPm(int amOrPm) {
        mAmOrPm = amOrPm;
    }

    public void setAmOrPmPressed(int amOrPmPressed) {
        mAmOrPmPressed = amOrPmPressed;
    }

    /**
     * Calculate whether the coordinates are touching the AM or PM circle.
     */
    public int getIsTouchingAmOrPm(float xCoord, float yCoord) {
        if (!mDrawValuesReady) {
            return -1;
        }

        int squaredYDistance = (int) ((yCoord - mAmPmYCenter) * (yCoord - mAmPmYCenter));

        int distanceToAmCenter =
                (int) Math.sqrt((xCoord - mAmXCenter) * (xCoord - mAmXCenter) + squaredYDistance);
        if (distanceToAmCenter <= mAmPmCircleRadius) {
            return AM;
        }

        int distanceToPmCenter =
                (int) Math.sqrt((xCoord - mPmXCenter) * (xCoord - mPmXCenter) + squaredYDistance);
        if (distanceToPmCenter <= mAmPmCircleRadius) {
            return PM;
        }

        // Neither was close enough.
        return -1;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int viewWidth = getWidth();
        if (viewWidth == 0 || !mIsInitialized) {
            return;
        }

        if (!mDrawValuesReady) {
            int layoutXCenter = getWidth() / 2;
            int layoutYCenter = getHeight() / 2;
            int circleRadius =
                    (int) (Math.min(layoutXCenter, layoutYCenter) * mCircleRadiusMultiplier);
            mAmPmCircleRadius = (int) (circleRadius * mAmPmCircleRadiusMultiplier);
            layoutYCenter += mAmPmCircleRadius * 0.75;
            int textSize = mAmPmCircleRadius * 3 / 4;
            mPaint.setTextSize(textSize);

            // Line up the vertical center of the AM/PM circles with the bottom of the main circle.
            mAmPmYCenter = layoutYCenter - mAmPmCircleRadius / 2 + circleRadius;
            // Line up the horizontal edges of the AM/PM circles with the horizontal edges
            // of the main circle.
            mAmXCenter = layoutXCenter - circleRadius + mAmPmCircleRadius;
            mPmXCenter = layoutXCenter + circleRadius - mAmPmCircleRadius;

            mDrawValuesReady = true;
        }

        // We'll need to draw either a lighter blue (for selection), a darker blue (for touching)
        // or white (for not selected).
        int amColor = mUnselectedColor;
        int amAlpha = 255;
        int amTextColor = mAmPmTextColor;
        int pmColor = mUnselectedColor;
        int pmAlpha = 255;
        int pmTextColor = mAmPmTextColor;

        if (mAmOrPm == AM) {
            amColor = mSelectedColor;
            amAlpha = mSelectedAlpha;
            amTextColor = mAmPmSelectedTextColor;
        } else if (mAmOrPm == PM) {
            pmColor = mSelectedColor;
            pmAlpha = mSelectedAlpha;
            pmTextColor = mAmPmSelectedTextColor;
        }
        if (mAmOrPmPressed == AM) {
            amColor = mTouchedColor;
            amAlpha = mSelectedAlpha;
        } else if (mAmOrPmPressed == PM) {
            pmColor = mTouchedColor;
            pmAlpha = mSelectedAlpha;
        }

        // Draw the two circles.
        mPaint.setColor(amColor);
        mPaint.setAlpha(amAlpha);
        canvas.drawCircle(mAmXCenter, mAmPmYCenter, mAmPmCircleRadius, mPaint);
        mPaint.setColor(pmColor);
        mPaint.setAlpha(pmAlpha);
        canvas.drawCircle(mPmXCenter, mAmPmYCenter, mAmPmCircleRadius, mPaint);

        // Draw the AM/PM texts on top.
        mPaint.setColor(amTextColor);
        int textYCenter = mAmPmYCenter - (int) (mPaint.descent() + mPaint.ascent()) / 2;
        canvas.drawText(mAmText, mAmXCenter, textYCenter, mPaint);
        mPaint.setColor(pmTextColor);
        canvas.drawText(mPmText, mPmXCenter, textYCenter, mPaint);
        mPaint.setTypeface(TypefaceHelper.get(context, fontName));
    }
}