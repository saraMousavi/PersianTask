package ir.android.taskroom.utils.calender;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;

import ir.android.taskroom.R;


public class RadialTextsView extends View {
    private final static String TAG = "RadialTextsView";

    private final Paint mPaint = new Paint();
    private final Paint mSelectedPaint = new Paint();

    private boolean mDrawValuesReady;
    private boolean mIsInitialized;

    private int selection = -1;

    private String[] mTexts;
    private String[] mInnerTexts;
    private boolean mIs24HourMode;
    private boolean mHasInnerCircle;
    private float mCircleRadiusMultiplier;
    private float mAmPmCircleRadiusMultiplier;
    private float mNumbersRadiusMultiplier;
    private float mInnerNumbersRadiusMultiplier;
    private float mTextSizeMultiplier;
    private float mInnerTextSizeMultiplier;

    private int mXCenter;
    private int mYCenter;
    private float mCircleRadius;
    private boolean mTextGridValuesDirty;
    private float mTextSize;
    private float mInnerTextSize;
    private float[] mTextGridHeights;
    private float[] mTextGridWidths;
    private float[] mInnerTextGridHeights;
    private float[] mInnerTextGridWidths;

    private float mAnimationRadiusMultiplier;
    private float mTransitionMidRadiusMultiplier;
    private float mTransitionEndRadiusMultiplier;
    ObjectAnimator mDisappearAnimator;
    ObjectAnimator mReappearAnimator;
    private InvalidateUpdateListener mInvalidateUpdateListener;
    private Context context;
    private String fontName="DroidNaskh-Regular";

    public RadialTextsView(Context context, String fontName) {
        super(context);
        this.context = context;
        mIsInitialized = false;
        this.fontName = fontName;
    }

    public void initialize(Resources res, String[] texts, String[] innerTexts,
                           boolean is24HourMode, boolean disappearsOut, String fontName) {
        if (mIsInitialized) {
            Log.e(TAG, "This RadialTextsView may only be initialized once.");
            return;
        }
        this.fontName = fontName;

        // Set up the paint.
        int numbersTextColor = res.getColor(R.color.mdtp_numbers_text_color);
        mPaint.setColor(numbersTextColor);
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.CENTER);

        // Set up the selected paint
        int selectedTextColor = res.getColor(R.color.white);
        mSelectedPaint.setColor(selectedTextColor);
        mSelectedPaint.setAntiAlias(true);
        mSelectedPaint.setTypeface(TypefaceHelper.get(context, fontName));
        mSelectedPaint.setTextAlign(Paint.Align.CENTER);

        mTexts = texts;
        mInnerTexts = innerTexts;
        mIs24HourMode = is24HourMode;
        mHasInnerCircle = (innerTexts != null);

        // Calculate the radius for the main circle.
        if (is24HourMode) {
            mCircleRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.mdtp_circle_radius_multiplier_24HourMode));
        } else {
            mCircleRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.mdtp_circle_radius_multiplier));
            mAmPmCircleRadiusMultiplier =
                    Float.parseFloat(res.getString(R.string.mdtp_ampm_circle_radius_multiplier));
        }

        // Initialize the widths and heights of the grid, and calculate the values for the numbers.
        mTextGridHeights = new float[7];
        mTextGridWidths = new float[7];
        if (mHasInnerCircle) {
            mNumbersRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.mdtp_numbers_radius_multiplier_outer));
            mTextSizeMultiplier = Float.parseFloat(
                    res.getString(R.string.mdtp_text_size_multiplier_outer));
            mInnerNumbersRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.mdtp_numbers_radius_multiplier_inner));
            mInnerTextSizeMultiplier = Float.parseFloat(
                    res.getString(R.string.mdtp_text_size_multiplier_inner));

            mInnerTextGridHeights = new float[7];
            mInnerTextGridWidths = new float[7];
        } else {
            mNumbersRadiusMultiplier = Float.parseFloat(
                    res.getString(R.string.mdtp_numbers_radius_multiplier_normal));
            mTextSizeMultiplier = Float.parseFloat(
                    res.getString(R.string.mdtp_text_size_multiplier_normal));
        }

        mAnimationRadiusMultiplier = 1;
        mTransitionMidRadiusMultiplier = 1f + (0.05f * (disappearsOut ? -1 : 1));
        mTransitionEndRadiusMultiplier = 1f + (0.3f * (disappearsOut ? 1 : -1));
        mInvalidateUpdateListener = new InvalidateUpdateListener();

        mTextGridValuesDirty = true;
        mIsInitialized = true;
    }

    /* package */ void setTheme(Context context, boolean themeDark) {
        Resources res = context.getResources();
        int textColor;
        if (themeDark) {
            textColor = res.getColor(R.color.white);
        } else {
            textColor = res.getColor(R.color.mdtp_numbers_text_color);
        }
        mPaint.setColor(textColor);
    }

    /**
     * Set the value of the selected text. Depending on the theme this will be rendered differently
     *
     * @param selection The text which is currently selected
     */
    protected void setSelection(int selection) {
        this.selection = selection;
    }

    /**
     * Allows for smoother animation.
     */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    /**
     * Used by the animation to move the numbers in and out.
     */
    @SuppressWarnings("unused")
    public void setAnimationRadiusMultiplier(float animationRadiusMultiplier) {
        mAnimationRadiusMultiplier = animationRadiusMultiplier;
        mTextGridValuesDirty = true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int viewWidth = getWidth();
        if (viewWidth == 0 || !mIsInitialized) {
            return;
        }

        if (!mDrawValuesReady) {
            mXCenter = getWidth() / 2;
            mYCenter = getHeight() / 2;
            mCircleRadius = Math.min(mXCenter, mYCenter) * mCircleRadiusMultiplier;
            if (!mIs24HourMode) {
                // We'll need to draw the AM/PM circles, so the main circle will need to have
                // a slightly higher center. To keep the entire view centered vertically, we'll
                // have to push it up by half the radius of the AM/PM circles.
                float amPmCircleRadius = mCircleRadius * mAmPmCircleRadiusMultiplier;
                mYCenter -= amPmCircleRadius * 0.75;
            }

            mTextSize = mCircleRadius * mTextSizeMultiplier;
            if (mHasInnerCircle) {
                mInnerTextSize = mCircleRadius * mInnerTextSizeMultiplier;
            }

            // Because the text positions will be static, pre-render the animations.
            renderAnimations();

            mTextGridValuesDirty = true;
            mDrawValuesReady = true;
        }

        // Calculate the text positions, but only if they've changed since the last onDraw.
        if (mTextGridValuesDirty) {
            float numbersRadius =
                    mCircleRadius * mNumbersRadiusMultiplier * mAnimationRadiusMultiplier;

            // Calculate the positions for the 12 numbers in the main circle.
            calculateGridSizes(numbersRadius, mXCenter, mYCenter,
                    mTextSize, mTextGridHeights, mTextGridWidths);
            if (mHasInnerCircle) {
                // If we have an inner circle, calculate those positions too.
                float innerNumbersRadius =
                        mCircleRadius * mInnerNumbersRadiusMultiplier * mAnimationRadiusMultiplier;
                calculateGridSizes(innerNumbersRadius, mXCenter, mYCenter,
                        mInnerTextSize, mInnerTextGridHeights, mInnerTextGridWidths);
            }
            mTextGridValuesDirty = false;
        }

        // Draw the texts in the pre-calculated positions.
        drawTexts(canvas, mTextSize, TypefaceHelper.get(context, fontName), mTexts, mTextGridWidths, mTextGridHeights);
        if (mHasInnerCircle) {
            drawTexts(canvas, mInnerTextSize, TypefaceHelper.get(context, fontName), mInnerTexts,
                    mInnerTextGridWidths, mInnerTextGridHeights);
        }
    }

    /**
     * Using the trigonometric Unit Circle, calculate the positions that the text will need to be
     * drawn at based on the specified circle radius. Place the values in the textGridHeights and
     * textGridWidths parameters.
     */
    private void calculateGridSizes(float numbersRadius, float xCenter, float yCenter,
                                    float textSize, float[] textGridHeights, float[] textGridWidths) {
        /*
         * The numbers need to be drawn in a 7x7 grid, representing the points on the Unit Circle.
         */
        float offset1 = numbersRadius;
        // cos(30) = a / r => r * cos(30) = a => r * √3/2 = a
        float offset2 = numbersRadius * ((float) Math.sqrt(3)) / 2f;
        // sin(30) = o / r => r * sin(30) = o => r / 2 = a
        float offset3 = numbersRadius / 2f;
        mPaint.setTextSize(textSize);
        mSelectedPaint.setTextSize(textSize);
        // We'll need yTextBase to be slightly lower to account for the text's baseline.
        yCenter -= (mPaint.descent() + mPaint.ascent()) / 2;

        textGridHeights[0] = yCenter - offset1;
        textGridWidths[0] = xCenter - offset1;
        textGridHeights[1] = yCenter - offset2;
        textGridWidths[1] = xCenter - offset2;
        textGridHeights[2] = yCenter - offset3;
        textGridWidths[2] = xCenter - offset3;
        textGridHeights[3] = yCenter;
        textGridWidths[3] = xCenter;
        textGridHeights[4] = yCenter + offset3;
        textGridWidths[4] = xCenter + offset3;
        textGridHeights[5] = yCenter + offset2;
        textGridWidths[5] = xCenter + offset2;
        textGridHeights[6] = yCenter + offset1;
        textGridWidths[6] = xCenter + offset1;
    }

    /**
     * Draw the 12 text values at the positions specified by the textGrid parameters.
     */
    private void drawTexts(Canvas canvas, float textSize, Typeface typeface, String[] texts,
                           float[] textGridWidths, float[] textGridHeights) {
        mPaint.setTextSize(textSize);
        mPaint.setTypeface(typeface);
        LanguageUtils.getPersianNumbers(texts);
        canvas.drawText(texts[0], textGridWidths[3], textGridHeights[0], Integer.parseInt(texts[0]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[1], textGridWidths[4], textGridHeights[1], Integer.parseInt(texts[1]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[2], textGridWidths[5], textGridHeights[2], Integer.parseInt(texts[2]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[3], textGridWidths[6], textGridHeights[3], Integer.parseInt(texts[3]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[4], textGridWidths[5], textGridHeights[4], Integer.parseInt(texts[4]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[5], textGridWidths[4], textGridHeights[5], Integer.parseInt(texts[5]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[6], textGridWidths[3], textGridHeights[6], Integer.parseInt(texts[6]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[7], textGridWidths[2], textGridHeights[5], Integer.parseInt(texts[7]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[8], textGridWidths[1], textGridHeights[4], Integer.parseInt(texts[8]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[9], textGridWidths[0], textGridHeights[3], Integer.parseInt(texts[9]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[10], textGridWidths[1], textGridHeights[2], Integer.parseInt(texts[10]) == selection ? mSelectedPaint : mPaint);
        canvas.drawText(texts[11], textGridWidths[2], textGridHeights[1], Integer.parseInt(texts[11]) == selection ? mSelectedPaint : mPaint);
    }

    /**
     * Render the animations for appearing and disappearing.
     */
    private void renderAnimations() {
        Keyframe kf0, kf1, kf2, kf3;
        float midwayPoint = 0.2f;
        int duration = 500;

        // Set up animator for disappearing.
        kf0 = Keyframe.ofFloat(0f, 1);
        kf1 = Keyframe.ofFloat(midwayPoint, mTransitionMidRadiusMultiplier);
        kf2 = Keyframe.ofFloat(1f, mTransitionEndRadiusMultiplier);
        PropertyValuesHolder radiusDisappear = PropertyValuesHolder.ofKeyframe(
                "animationRadiusMultiplier", kf0, kf1, kf2);

        kf0 = Keyframe.ofFloat(0f, 1f);
        kf1 = Keyframe.ofFloat(1f, 0f);
        PropertyValuesHolder fadeOut = PropertyValuesHolder.ofKeyframe("alpha", kf0, kf1);

        mDisappearAnimator = ObjectAnimator.ofPropertyValuesHolder(
                this, radiusDisappear, fadeOut).setDuration(duration);
        mDisappearAnimator.addUpdateListener(mInvalidateUpdateListener);


        // Set up animator for reappearing.
        float delayMultiplier = 0.25f;
        float transitionDurationMultiplier = 1f;
        float totalDurationMultiplier = transitionDurationMultiplier + delayMultiplier;
        int totalDuration = (int) (duration * totalDurationMultiplier);
        float delayPoint = (delayMultiplier * duration) / totalDuration;
        midwayPoint = 1 - (midwayPoint * (1 - delayPoint));

        kf0 = Keyframe.ofFloat(0f, mTransitionEndRadiusMultiplier);
        kf1 = Keyframe.ofFloat(delayPoint, mTransitionEndRadiusMultiplier);
        kf2 = Keyframe.ofFloat(midwayPoint, mTransitionMidRadiusMultiplier);
        kf3 = Keyframe.ofFloat(1f, 1);
        PropertyValuesHolder radiusReappear = PropertyValuesHolder.ofKeyframe(
                "animationRadiusMultiplier", kf0, kf1, kf2, kf3);

        kf0 = Keyframe.ofFloat(0f, 0f);
        kf1 = Keyframe.ofFloat(delayPoint, 0f);
        kf2 = Keyframe.ofFloat(1f, 1f);
        PropertyValuesHolder fadeIn = PropertyValuesHolder.ofKeyframe("alpha", kf0, kf1, kf2);

        mReappearAnimator = ObjectAnimator.ofPropertyValuesHolder(
                this, radiusReappear, fadeIn).setDuration(totalDuration);
        mReappearAnimator.addUpdateListener(mInvalidateUpdateListener);
    }

    public ObjectAnimator getDisappearAnimator() {
        if (!mIsInitialized || !mDrawValuesReady || mDisappearAnimator == null) {
            Log.e(TAG, "RadialTextView was not ready for animation.");
            return null;
        }

        return mDisappearAnimator;
    }

    public ObjectAnimator getReappearAnimator() {
        if (!mIsInitialized || !mDrawValuesReady || mReappearAnimator == null) {
            Log.e(TAG, "RadialTextView was not ready for animation.");
            return null;
        }

        return mReappearAnimator;
    }

    private class InvalidateUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            RadialTextsView.this.invalidate();
        }
    }
}