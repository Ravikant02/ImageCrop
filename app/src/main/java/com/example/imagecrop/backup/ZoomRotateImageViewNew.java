package com.example.imagecrop.backup;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.view.ScaleGestureDetectorCompat;

import com.example.imagecrop.R.styleable;


public class ZoomRotateImageViewNew extends AppCompatImageView implements ScaleGestureDetector.OnScaleGestureListener {
    private static final float MIN_SCALE = 0.6F;
    private static final float MAX_SCALE = 8.0F;
    private final int RESET_DURATION = 200;
    private ScaleType startScaleType;
    public Matrix matrix = new Matrix();
    private Matrix startMatrix = new Matrix();
    public float[] matrixValues = new float[9];
    private float[] startValues = null;
    private float minScale = 0.6F;
    private float maxScale = 8.0F;
    private float calculatedMinScale = 0.6F;
    private float calculatedMaxScale = 8.0F;
    private final RectF bounds = new RectF();
    private boolean translatable;
    private boolean zoomable;
    private boolean doubleTapToZoom;
    private boolean restrictBounds;
    private boolean animateOnReset;
    private boolean autoCenter;
    private float doubleTapToZoomScaleFactor;
    private int autoResetMode;
    private PointF last = new PointF(0.0F, 0.0F);
    private float startScale = 1.0F;
    private float scaleBy = 1.0F;
    private float currentScaleFactor = 1.0F;
    private int previousPointerCount = 1;
    public int mPivotX;
    public int mPivotY;
    private float mLastAngle = 0.0F;
    private float mLastScaleFactor = 0.0F;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private boolean doubleTapDetected = false;
    private boolean singleTapDetected = false;
    private final GestureDetector.OnGestureListener gestureListener;

    class NamelessClass_1 extends GestureDetector.SimpleOnGestureListener {
        NamelessClass_1() {
        }

        public boolean onDoubleTapEvent(MotionEvent e) {
            if (e.getAction() == 1) {
                ZoomRotateImageViewNew.this.doubleTapDetected = true;
            }

            return false;
        }

        public boolean onSingleTapUp(MotionEvent e) {
            ZoomRotateImageViewNew.this.singleTapDetected = true;
            return false;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            ZoomRotateImageViewNew.this.singleTapDetected = false;
            return false;
        }

        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    public ZoomRotateImageViewNew(Context context) {
        super(context);
        this.gestureListener = new NamelessClass_1();
        this.init(context, (AttributeSet)null);
    }

    public ZoomRotateImageViewNew(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.gestureListener = new NamelessClass_1();
        this.init(context, attrs);
    }

    public ZoomRotateImageViewNew(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.gestureListener = new NamelessClass_1();
        this.init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.scaleDetector = new ScaleGestureDetector(context, this);
        this.gestureDetector = new GestureDetector(context, this.gestureListener);
        ScaleGestureDetectorCompat.setQuickScaleEnabled(this.scaleDetector, false);
        this.startScaleType = this.getScaleType();
        TypedArray values = context.obtainStyledAttributes(attrs, styleable.ZoomRotateImageView);
        this.zoomable = values.getBoolean(styleable.ZoomRotateImageView_zoomable, true);
        this.translatable = values.getBoolean(styleable.ZoomRotateImageView_translatable, true);
        this.animateOnReset = values.getBoolean(styleable.ZoomRotateImageView_animateOnReset, true);
        this.autoCenter = values.getBoolean(styleable.ZoomRotateImageView_autoCenter, true);
        this.restrictBounds = values.getBoolean(styleable.ZoomRotateImageView_restrictBounds, false);
        this.doubleTapToZoom = values.getBoolean(styleable.ZoomRotateImageView_doubleTapToZoom, true);
        this.minScale = values.getFloat(styleable.ZoomRotateImageView_minScale, 0.6F);
        this.maxScale = values.getFloat(styleable.ZoomRotateImageView_maxScale, 8.0F);
        this.doubleTapToZoomScaleFactor = values.getFloat(styleable.ZoomRotateImageView_doubleTapToZoomScaleFactor, 3.0F);
        this.autoResetMode = values.getInt(styleable.ZoomRotateImageView_autoResetMode, 0);
        this.verifyScaleRange();
        values.recycle();
    }

    private void verifyScaleRange() {
        if (this.minScale >= this.maxScale) {
            throw new IllegalStateException("minScale must be less than maxScale");
        } else if (this.minScale < 0.0F) {
            throw new IllegalStateException("minScale must be greater than 0");
        } else if (this.maxScale < 0.0F) {
            throw new IllegalStateException("maxScale must be greater than 0");
        } else {
            if (this.doubleTapToZoomScaleFactor > this.maxScale) {
                this.doubleTapToZoomScaleFactor = this.maxScale;
            }

            if (this.doubleTapToZoomScaleFactor < this.minScale) {
                this.doubleTapToZoomScaleFactor = this.minScale;
            }

        }
    }

    public void setScaleRange(float minScale, float maxScale) {
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.startValues = null;
        this.verifyScaleRange();
    }

    public boolean isTranslatable() {
        return this.translatable;
    }

    public void setTranslatable(boolean translatable) {
        this.translatable = translatable;
    }

    public boolean isZoomable() {
        return this.zoomable;
    }

    public void setZoomable(boolean zoomable) {
        this.zoomable = zoomable;
    }

    public boolean getRestrictBounds() {
        return this.restrictBounds;
    }

    public void setRestrictBounds(boolean restrictBounds) {
        this.restrictBounds = restrictBounds;
    }

    public boolean getAnimateOnReset() {
        return this.animateOnReset;
    }

    public void setAnimateOnReset(boolean animateOnReset) {
        this.animateOnReset = animateOnReset;
    }

    public int getAutoResetMode() {
        return this.autoResetMode;
    }

    public void setAutoResetMode(int autoReset) {
        this.autoResetMode = autoReset;
    }

    public boolean getAutoCenter() {
        return this.autoCenter;
    }

    public void setAutoCenter(boolean autoCenter) {
        this.autoCenter = autoCenter;
    }

    public boolean getDoubleTapToZoom() {
        return this.doubleTapToZoom;
    }

    public void setDoubleTapToZoom(boolean doubleTapToZoom) {
        this.doubleTapToZoom = doubleTapToZoom;
    }

    public float getDoubleTapToZoomScaleFactor() {
        return this.doubleTapToZoomScaleFactor;
    }

    public void setDoubleTapToZoomScaleFactor(float doubleTapToZoomScaleFactor) {
        this.doubleTapToZoomScaleFactor = doubleTapToZoomScaleFactor;
        this.verifyScaleRange();
    }

    public float getCurrentScaleFactor() {
        return this.currentScaleFactor;
    }

    public void setScaleType(@Nullable ScaleType scaleType) {
        if (scaleType != null) {
            super.setScaleType(scaleType);
            this.startScaleType = scaleType;
            this.startValues = null;
        }

    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            this.setScaleType(this.startScaleType);
        }

    }

    public void setImageResource(int resId) {
        super.setImageResource(resId);
        this.setScaleType(this.startScaleType);
    }

    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        this.setScaleType(this.startScaleType);
    }

    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        this.setScaleType(this.startScaleType);
    }

    public void setImageURI(@Nullable Uri uri) {
        super.setImageURI(uri);
        this.setScaleType(this.startScaleType);
    }

    private void updateBounds(float[] values) {
        if (this.getDrawable() != null) {
            this.bounds.set(values[2], values[5], (float)this.getDrawable().getIntrinsicWidth() * values[0] + values[2], (float)this.getDrawable().getIntrinsicHeight() * values[4] + values[5]);
        }

    }

    private float getCurrentDisplayedWidth() {
        return this.getDrawable() != null ? (float)this.getDrawable().getIntrinsicWidth() * this.matrixValues[0] : 0.0F;
    }

    private float getCurrentDisplayedHeight() {
        return this.getDrawable() != null ? (float)this.getDrawable().getIntrinsicHeight() * this.matrixValues[4] : 0.0F;
    }

    private void setStartValues() {
        this.startValues = new float[9];
        this.startMatrix = new Matrix(this.getImageMatrix());
        this.startMatrix.getValues(this.startValues);
        this.calculatedMinScale = this.minScale * this.startValues[0];
        this.calculatedMaxScale = this.maxScale * this.startValues[0];
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.isClickable() || !this.isEnabled() || !this.zoomable && !this.translatable) {
            return super.onTouchEvent(event);
        } else {
            if (this.getScaleType() != ScaleType.MATRIX) {
                super.setScaleType(ScaleType.MATRIX);
            }

            if (this.startValues == null) {
                this.setStartValues();
            }

            this.matrix.set(this.getImageMatrix());
            this.matrix.getValues(this.matrixValues);
            this.updateBounds(this.matrixValues);
            this.scaleDetector.onTouchEvent(event);
            this.gestureDetector.onTouchEvent(event);
            if (this.doubleTapToZoom && this.doubleTapDetected) {
                this.doubleTapDetected = false;
                this.singleTapDetected = false;
                if (this.matrixValues[0] != this.startValues[0]) {
                    this.reset();
                } else {
                    Matrix zoomMatrix = new Matrix(this.matrix);
                    zoomMatrix.postScale(this.doubleTapToZoomScaleFactor, this.doubleTapToZoomScaleFactor, this.scaleDetector.getFocusX(), this.scaleDetector.getFocusY());
                    this.animateScaleAndTranslationToMatrix(zoomMatrix, 200);
                }

                return true;
            } else {
                if (!this.singleTapDetected) {
                    if (event.getActionMasked() != 0 && event.getPointerCount() == this.previousPointerCount) {
                        if (event.getActionMasked() == 2) {
                            float focusx = this.scaleDetector.getFocusX();
                            float focusy = this.scaleDetector.getFocusY();
                            if (this.translatable) {
                                float xdistance = this.getXDistance(focusx, this.last.x);
                                float ydistance = this.getYDistance(focusy, this.last.y);
                                this.matrix.postTranslate(xdistance, ydistance);
                            }

                            if (this.zoomable) {
                                this.matrix.postScale(this.scaleBy, this.scaleBy, focusx, focusy);
                                this.currentScaleFactor = this.matrixValues[0] / this.startValues[0];
                            }

                            this.setImageMatrix(this.matrix);
                            this.last.set(focusx, focusy);
                        }
                    } else {
                        this.last.set(this.scaleDetector.getFocusX(), this.scaleDetector.getFocusY());
                    }

                    if (event.getActionMasked() == 1) {
                        this.scaleBy = 1.0F;
                    }
                }

                this.previousPointerCount = event.getPointerCount();
                return true;
            }
        }
    }

    private void resetImage() {
        switch(this.autoResetMode) {
            case 0:
                if (this.matrixValues[0] <= this.startValues[0]) {
                    this.reset();
                } else {
                    this.center();
                }
                break;
            case 1:
                if (this.matrixValues[0] >= this.startValues[0]) {
                    this.reset();
                } else {
                    this.center();
                }
                break;
            case 2:
                this.reset();
                break;
            case 3:
                this.center();
        }

    }

    private void center() {
        if (this.autoCenter) {
            this.animateTranslationX();
            this.animateTranslationY();
        }

    }

    public void reset() {
        this.reset(this.animateOnReset);
    }

    public void reset(boolean animate) {
    }

    private void animateToStartMatrix() {
        this.animateScaleAndTranslationToMatrix(this.startMatrix, 200);
    }

    private void animateScaleAndTranslationToMatrix(final Matrix targetMatrix, int duration) {
        float[] targetValues = new float[9];
        targetMatrix.getValues(targetValues);
        final Matrix beginMatrix = new Matrix(this.getImageMatrix());
        beginMatrix.getValues(this.matrixValues);
        final float xsdiff = targetValues[0] - this.matrixValues[0];
        final float ysdiff = targetValues[4] - this.matrixValues[4];
        final float xtdiff = targetValues[2] - this.matrixValues[2];
        final float ytdiff = targetValues[5] - this.matrixValues[5];
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0F, 1.0F});
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            final Matrix activeMatrix = new Matrix(ZoomRotateImageViewNew.this.getImageMatrix());
            final float[] values = new float[9];

            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (Float)animation.getAnimatedValue();
                this.activeMatrix.set(beginMatrix);
                this.activeMatrix.getValues(this.values);
                this.values[2] += xtdiff * val;
                this.values[5] += ytdiff * val;
                this.values[0] += xsdiff * val;
                this.values[4] += ysdiff * val;
                this.activeMatrix.setValues(this.values);
                ZoomRotateImageViewNew.this.setImageMatrix(this.activeMatrix);
            }
        });
        anim.addListener(new SimpleAnimatorListener() {
            public void onAnimationEnd(Animator animation) {
                ZoomRotateImageViewNew.this.setImageMatrix(targetMatrix);
            }
        });
        anim.setDuration((long)duration);
        anim.start();
    }

    private void animateTranslationX() {
        if (this.getCurrentDisplayedWidth() > (float)this.getWidth()) {
            if (this.bounds.left > 0.0F) {
                this.animateMatrixIndex(2, 0.0F);
            } else if (this.bounds.right < (float)this.getWidth()) {
                this.animateMatrixIndex(2, this.bounds.left + (float)this.getWidth() - this.bounds.right);
            }
        } else if (this.bounds.left < 0.0F) {
            this.animateMatrixIndex(2, 0.0F);
        } else if (this.bounds.right > (float)this.getWidth()) {
            this.animateMatrixIndex(2, this.bounds.left + (float)this.getWidth() - this.bounds.right);
        }

    }

    private void animateTranslationY() {
        if (this.getCurrentDisplayedHeight() > (float)this.getHeight()) {
            if (this.bounds.top > 0.0F) {
                this.animateMatrixIndex(5, 0.0F);
            } else if (this.bounds.bottom < (float)this.getHeight()) {
                this.animateMatrixIndex(5, this.bounds.top + (float)this.getHeight() - this.bounds.bottom);
            }
        } else if (this.bounds.top < 0.0F) {
            this.animateMatrixIndex(5, 0.0F);
        } else if (this.bounds.bottom > (float)this.getHeight()) {
            this.animateMatrixIndex(5, this.bounds.top + (float)this.getHeight() - this.bounds.bottom);
        }

    }

    private void animateMatrixIndex(final int index, float to) {
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.matrixValues[index], to});
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            final float[] values = new float[9];
            Matrix current = new Matrix();

            public void onAnimationUpdate(ValueAnimator animation) {
                this.current.set(ZoomRotateImageViewNew.this.getImageMatrix());
                this.current.getValues(this.values);
                this.values[index] = (Float)animation.getAnimatedValue();
                this.current.setValues(this.values);
                ZoomRotateImageViewNew.this.setImageMatrix(this.current);
            }
        });
        animator.setDuration(200L);
        animator.start();
    }

    private float getXDistance(float toX, float fromX) {
        float xdistance = toX - fromX;
        if (this.restrictBounds) {
            xdistance = this.getRestrictedXDistance(xdistance);
        }

        if (this.bounds.right + xdistance < 0.0F) {
            xdistance = -this.bounds.right;
        } else if (this.bounds.left + xdistance > (float)this.getWidth()) {
            xdistance = (float)this.getWidth() - this.bounds.left;
        }

        return xdistance;
    }

    private float getRestrictedXDistance(float xdistance) {
        float restrictedXDistance = xdistance;
        if (this.getCurrentDisplayedWidth() >= (float)this.getWidth()) {
            if (this.bounds.left <= 0.0F && this.bounds.left + xdistance > 0.0F && !this.scaleDetector.isInProgress()) {
                restrictedXDistance = -this.bounds.left;
            } else if (this.bounds.right >= (float)this.getWidth() && this.bounds.right + xdistance < (float)this.getWidth() && !this.scaleDetector.isInProgress()) {
                restrictedXDistance = (float)this.getWidth() - this.bounds.right;
            }
        } else if (!this.scaleDetector.isInProgress()) {
            if (this.bounds.left >= 0.0F && this.bounds.left + xdistance < 0.0F) {
                restrictedXDistance = -this.bounds.left;
            } else if (this.bounds.right <= (float)this.getWidth() && this.bounds.right + xdistance > (float)this.getWidth()) {
                restrictedXDistance = (float)this.getWidth() - this.bounds.right;
            }
        }

        return restrictedXDistance;
    }

    private float getYDistance(float toY, float fromY) {
        float ydistance = toY - fromY;
        if (this.restrictBounds) {
            ydistance = this.getRestrictedYDistance(ydistance);
        }

        if (this.bounds.bottom + ydistance < 0.0F) {
            ydistance = -this.bounds.bottom;
        } else if (this.bounds.top + ydistance > (float)this.getHeight()) {
            ydistance = (float)this.getHeight() - this.bounds.top;
        }

        return ydistance;
    }

    private float getRestrictedYDistance(float ydistance) {
        float restrictedYDistance = ydistance;
        if (this.getCurrentDisplayedHeight() >= (float)this.getHeight()) {
            if (this.bounds.top <= 0.0F && this.bounds.top + ydistance > 0.0F && !this.scaleDetector.isInProgress()) {
                restrictedYDistance = -this.bounds.top;
            } else if (this.bounds.bottom >= (float)this.getHeight() && this.bounds.bottom + ydistance < (float)this.getHeight() && !this.scaleDetector.isInProgress()) {
                restrictedYDistance = (float)this.getHeight() - this.bounds.bottom;
            }
        } else if (!this.scaleDetector.isInProgress()) {
            if (this.bounds.top >= 0.0F && this.bounds.top + ydistance < 0.0F) {
                restrictedYDistance = -this.bounds.top;
            } else if (this.bounds.bottom <= (float)this.getHeight() && this.bounds.bottom + ydistance > (float)this.getHeight()) {
                restrictedYDistance = (float)this.getHeight() - this.bounds.bottom;
            }
        }

        return restrictedYDistance;
    }

    public boolean onScale(ScaleGestureDetector detector) {
        this.scaleBy = this.startScale * detector.getScaleFactor() / this.matrixValues[0];
        float projectedScale = this.scaleBy * this.matrixValues[0];
        if (projectedScale < this.calculatedMinScale) {
            this.scaleBy = this.calculatedMinScale / this.matrixValues[0];
        } else if (projectedScale > this.calculatedMaxScale) {
            this.scaleBy = this.calculatedMaxScale / this.matrixValues[0];
        }

        return false;
    }

    public boolean onScaleBegin(ScaleGestureDetector detector) {
        this.startScale = this.matrixValues[0];
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector) {
        this.scaleBy = 1.0F;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mPivotX = w / 2;
        this.mPivotY = h / 2;
    }

    public void rotate(float degrees) {
        if (this.startValues == null) {
            this.setStartValues();
        }

        this.matrix.set(this.getImageMatrix());
        this.matrix.getValues(this.matrixValues);
        this.updateBounds(this.matrixValues);
        this.matrix.postRotate(degrees - this.mLastAngle, (float)this.mPivotX, (float)this.mPivotY);
        this.setImageMatrix(this.matrix);
        this.mLastAngle = degrees;
    }

    public void zoom(float scaleFactor) {
        if (this.startValues == null) {
            this.setStartValues();
        }

        this.matrix.set(this.getImageMatrix());
        this.matrix.getValues(this.matrixValues);
        this.updateBounds(this.matrixValues);
        this.matrix.postScale(scaleFactor, scaleFactor, (float)this.mPivotX, (float)this.mPivotY);
        this.setImageMatrix(this.matrix);
        this.mLastScaleFactor = scaleFactor;
    }

    private float getScaleX(float scaleFactor) {
        float sx = this.startScale * scaleFactor / this.matrixValues[0];
        float projectedScale = sx * this.matrixValues[0];
        return projectedScale;
    }

    public void resetAll() {
        this.animateToStartMatrix();
    }

    private class SimpleAnimatorListener implements Animator.AnimatorListener {
        private SimpleAnimatorListener() {
        }

        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
        }

        public void onAnimationCancel(Animator animation) {
        }

        public void onAnimationRepeat(Animator animation) {
        }
    }
}
