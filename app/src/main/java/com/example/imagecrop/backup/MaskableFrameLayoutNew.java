package com.example.imagecrop.backup;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.imagecrop.R.styleable;

public class MaskableFrameLayoutNew extends FrameLayout {
    private static final String TAG = "MaskableFrameLayout";
    private static final int MODE_ADD = 0;
    private static final int MODE_CLEAR = 1;
    private static final int MODE_DARKEN = 2;
    private static final int MODE_DST = 3;
    private static final int MODE_DST_ATOP = 4;
    private static final int MODE_DST_IN = 5;
    private static final int MODE_DST_OUT = 6;
    private static final int MODE_DST_OVER = 7;
    private static final int MODE_LIGHTEN = 8;
    private static final int MODE_MULTIPLY = 9;
    private static final int MODE_OVERLAY = 10;
    private static final int MODE_SCREEN = 11;
    private static final int MODE_SRC = 12;
    private static final int MODE_SRC_ATOP = 13;
    private static final int MODE_SRC_IN = 14;
    private static final int MODE_SRC_OUT = 15;
    private static final int MODE_SRC_OVER = 16;
    private static final int MODE_XOR = 17;
    private Handler mHandler;
    @Nullable
    private Drawable mDrawableMask = null;
    @Nullable
    private Bitmap mFinalMask = null;
    private Paint mPaint = null;
    private PorterDuffXfermode mPorterDuffXferMode = null;

    public MaskableFrameLayoutNew(Context context) {
        super(context);
    }

    public MaskableFrameLayoutNew(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.construct(context, attrs);
    }

    public MaskableFrameLayoutNew(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.construct(context, attrs);
    }

    private void construct(Context context, AttributeSet attrs) {
        this.mHandler = new Handler();
        this.setDrawingCacheEnabled(true);
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        this.mPaint = this.createPaint(false);
        Theme theme = context.getTheme();
        if (theme != null) {
            TypedArray a = theme.obtainStyledAttributes(attrs, styleable.MaskableView, 0, 0);

            try {
                this.initMask(this.loadMask(a));
                this.mPorterDuffXferMode = this.getModeFromInteger(a.getInteger(styleable.MaskableView_porterduffxfermode, 0));
                this.initMask(this.mDrawableMask);
                if (a.getBoolean(styleable.MaskableView_anti_aliasing, false)) {
                    this.mPaint = this.createPaint(true);
                }
            } finally {
                if (a != null) {
                    a.recycle();
                }

            }
        } else {
            this.log("Couldn't load theme, mask in xml won't be loaded.");
        }

        this.registerMeasure();
    }

    private Paint createPaint(boolean antiAliasing) {
        Paint output = new Paint(1);
        output.setAntiAlias(antiAliasing);
        output.setXfermode(this.mPorterDuffXferMode);
        return output;
    }

    @Nullable
    private Drawable loadMask(TypedArray a) {
        int drawableResId = a.getResourceId(styleable.MaskableView_mask, -1);
        return AppCompatResources.getDrawable(this.getContext(), drawableResId);
    }

    private void initMask(@Nullable Drawable input) {
        if (input != null) {
            this.mDrawableMask = input;
            if (this.mDrawableMask instanceof AnimationDrawable) {
                this.mDrawableMask.setCallback(this);
            }
        } else {
            this.log("Are you sure you don't want to provide a mask ?");
        }

    }

    @Nullable
    public Drawable getDrawableMask() {
        return this.mDrawableMask;
    }

    @Nullable
    private Bitmap makeBitmapMask(@Nullable Drawable drawable) {
        if (drawable != null) {
            if (this.getMeasuredWidth() > 0 && this.getMeasuredHeight() > 0) {
                int width = this.getMeasuredWidth();
                int height = this.getMeasuredHeight();
                Bitmap mask = Bitmap.createBitmap(width, height, Config.ARGB_8888);
                Canvas canvas = new Canvas(mask);
                canvas.drawColor(1140850688);
                drawable.setBounds(0, 0, width, height);
                drawable.draw(canvas);
                return mask;
            } else {
                this.log("Can't create a mask with height 0 or width 0. Or the layout has no children and is wrap content");
                return null;
            }
        } else {
            this.log("No bitmap mask loaded, view will NOT be masked !");
            return null;
        }
    }

    public void setMask(int drawableRes) {
        Resources res = this.getResources();
        if (res != null) {
            this.setMask(res.getDrawable(drawableRes));
        } else {
            this.log("Unable to load resources, mask will not be loaded as drawable");
        }

    }

    public void setMask(@Nullable Drawable input) {
        this.initMask(input);
        this.swapBitmapMask(this.makeBitmapMask(this.mDrawableMask));
        this.invalidate();
    }

    public void setPorterDuffXferMode(Mode mode) {
        this.mPorterDuffXferMode = new PorterDuffXfermode(mode);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.setSize(w, h);
    }

    private void setSize(int width, int height) {
        if (width > 0 && height > 0) {
            if (this.mDrawableMask != null) {
                this.swapBitmapMask(this.makeBitmapMask(this.mDrawableMask));
            }
        } else {
            this.log("Width and height must be higher than 0");
        }

    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mFinalMask != null && this.mPaint != null) {
            this.mPaint.setXfermode(this.mPorterDuffXferMode);
            canvas.drawBitmap(this.mFinalMask, 0.0F, 0.0F, this.mPaint);
            this.mPaint.setXfermode((Xfermode)null);
        } else {
            this.log("Mask or paint is null ...");
        }

    }

    private void registerMeasure() {
        final ViewTreeObserver treeObserver = this.getViewTreeObserver();
        if (treeObserver != null && treeObserver.isAlive()) {
            treeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    ViewTreeObserver aliveObserver = treeObserver;
                    if (!aliveObserver.isAlive()) {
                        aliveObserver = MaskableFrameLayoutNew.this.getViewTreeObserver();
                    }

                    if (aliveObserver != null) {
                        if (VERSION.SDK_INT >= 16) {
                            aliveObserver.removeOnGlobalLayoutListener(this);
                        } else {
                            aliveObserver.removeGlobalOnLayoutListener(this);
                        }
                    } else {
                        MaskableFrameLayoutNew.this.log("GlobalLayoutListener not removed as ViewTreeObserver is not valid");
                    }

                    MaskableFrameLayoutNew.this.swapBitmapMask(MaskableFrameLayoutNew.this.makeBitmapMask(MaskableFrameLayoutNew.this.mDrawableMask));
                }
            });
        }

    }

    private void log(String message) {
    }

    public void invalidateDrawable(Drawable dr) {
        if (dr != null) {
            this.initMask(dr);
            this.swapBitmapMask(this.makeBitmapMask(dr));
            this.invalidate();
        }

    }

    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (who != null && what != null) {
            this.mHandler.postAtTime(what, when);
        }

    }

    public void unscheduleDrawable(Drawable who, Runnable what) {
        if (who != null && what != null) {
            this.mHandler.removeCallbacks(what);
        }

    }

    private void swapBitmapMask(@Nullable Bitmap newMask) {
        if (newMask != null) {
            if (this.mFinalMask != null && !this.mFinalMask.isRecycled()) {
                this.mFinalMask.recycle();
            }

            this.mFinalMask = newMask;
        }

    }

    private PorterDuffXfermode getModeFromInteger(int index) {
        Mode mode = null;
        switch(index) {
            case 0:
                if (VERSION.SDK_INT >= 11) {
                    mode = Mode.ADD;
                } else {
                    this.log("MODE_ADD is not supported on api lvl " + VERSION.SDK_INT);
                }
            case 1:
                mode = Mode.CLEAR;
                break;
            case 2:
                mode = Mode.DARKEN;
                break;
            case 3:
                mode = Mode.DST;
                break;
            case 4:
                mode = Mode.DST_ATOP;
                break;
            case 5:
                mode = Mode.DST_IN;
                break;
            case 6:
                mode = Mode.DST_OUT;
                break;
            case 7:
                mode = Mode.DST_OVER;
                break;
            case 8:
                mode = Mode.LIGHTEN;
                break;
            case 9:
                mode = Mode.MULTIPLY;
                break;
            case 10:
                if (VERSION.SDK_INT >= 11) {
                    mode = Mode.OVERLAY;
                } else {
                    this.log("MODE_OVERLAY is not supported on api lvl " + VERSION.SDK_INT);
                }
            case 11:
                mode = Mode.SCREEN;
                break;
            case 12:
                mode = Mode.SRC;
                break;
            case 13:
                mode = Mode.SRC_ATOP;
                break;
            case 14:
                mode = Mode.SRC_IN;
                break;
            case 15:
                mode = Mode.SRC_OUT;
                break;
            case 16:
                mode = Mode.SRC_OVER;
                break;
            case 17:
                mode = Mode.XOR;
                break;
            default:
                mode = Mode.DST_IN;
        }

        this.log("Mode is " + mode.toString());
        return new PorterDuffXfermode(mode);
    }
}