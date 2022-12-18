package com.example.imagecrop

import android.content.Context
import android.widget.FrameLayout
import android.graphics.drawable.Drawable
import android.content.res.Resources.Theme
import android.content.res.TypedArray
import android.graphics.*
import com.example.imagecrop.R
import androidx.appcompat.content.res.AppCompatResources
import android.graphics.drawable.AnimationDrawable
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.os.Build.VERSION
import android.os.Handler
import android.util.AttributeSet

class MaskableFrameLayout : FrameLayout {
    private var mHandler: Handler? = null
    var drawableMask: Drawable? = null
        private set
    private var mFinalMask: Bitmap? = null
    private var mPaint: Paint? = null
    private var mPorterDuffXferMode: PorterDuffXfermode? = null

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        construct(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        construct(context, attrs)
    }

    private fun construct(context: Context, attrs: AttributeSet?) {
        mHandler = Handler()
        this.isDrawingCacheEnabled = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        mPaint = createPaint(false)
        val theme = context.theme
        if (theme != null) {
            val a = theme.obtainStyledAttributes(attrs, R.styleable.MaskableView, 0, 0)
            try {
                initMask(loadMask(a))
                mPorterDuffXferMode = getModeFromInteger(a.getInteger(R.styleable.MaskableView_porterduffxfermode, 0))
                initMask(drawableMask)
                if (a.getBoolean(R.styleable.MaskableView_anti_aliasing, false)) {
                    mPaint = createPaint(true)
                }
            } finally {
                if (a != null) {
                    a.recycle()
                }
            }
        } else {
            log("Couldn't load theme, mask in xml won't be loaded.")
        }
        registerMeasure()
    }

    private fun createPaint(antiAliasing: Boolean): Paint {
        val output = Paint(1)
        output.isAntiAlias = antiAliasing
        output.xfermode = mPorterDuffXferMode
        return output
    }

    private fun loadMask(a: TypedArray?): Drawable? {
        val drawableResId = a!!.getResourceId(R.styleable.MaskableView_mask, -1)
        return AppCompatResources.getDrawable(this.context, drawableResId)
    }

    private fun initMask(input: Drawable?) {
        if (input != null) {
            drawableMask = input
            if (drawableMask is AnimationDrawable) {
                drawableMask?.callback = this
            }
        } else {
            log("Are you sure you don't want to provide a mask ?")
        }
    }

    private fun makeBitmapMask(drawable: Drawable?): Bitmap? {
        return if (drawable != null) {
            if (this.measuredWidth > 0 && this.measuredHeight > 0) {
                val width = this.measuredWidth
                val height = this.measuredHeight
                val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(mask)
                canvas.drawColor(1140850688)
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas)
                mask
            } else {
                log("Can't create a mask with height 0 or width 0. Or the layout has no children and is wrap content")
                null
            }
        } else {
            log("No bitmap mask loaded, view will NOT be masked !")
            null
        }
    }

    fun setMask(drawableRes: Int) {
        val res = this.resources
        if (res != null) {
            this.setMask(res.getDrawable(drawableRes))
        } else {
            log("Unable to load resources, mask will not be loaded as drawable")
        }
    }

    fun setMask(input: Drawable?) {
        initMask(input)
        swapBitmapMask(makeBitmapMask(drawableMask))
        this.invalidate()
    }

    fun setPorterDuffXferMode(mode: PorterDuff.Mode?) {
        mPorterDuffXferMode = PorterDuffXfermode(mode)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setSize(w, h)
    }

    private fun setSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            if (drawableMask != null) {
                swapBitmapMask(makeBitmapMask(drawableMask))
            }
        } else {
            log("Width and height must be higher than 0")
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (mFinalMask != null && mPaint != null) {
            mPaint!!.xfermode = mPorterDuffXferMode
            canvas.drawBitmap(mFinalMask!!, 0.0f, 0.0f, mPaint)
            mPaint!!.xfermode = null as Xfermode?
        } else {
            log("Mask or paint is null ...")
        }
    }

    private fun registerMeasure() {
        val treeObserver = this.viewTreeObserver
        if (treeObserver != null && treeObserver.isAlive) {
            treeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    var aliveObserver = treeObserver
                    if (!aliveObserver.isAlive) {
                        aliveObserver = this@MaskableFrameLayout.viewTreeObserver
                    }
                    if (aliveObserver != null) {
                        if (VERSION.SDK_INT >= 16) {
                            aliveObserver.removeOnGlobalLayoutListener(this)
                        } else {
                            aliveObserver.removeGlobalOnLayoutListener(this)
                        }
                    } else {
                        log("GlobalLayoutListener not removed as ViewTreeObserver is not valid")
                    }
                    swapBitmapMask(makeBitmapMask(drawableMask))
                }
            })
        }
    }

    private fun log(message: String) {}
    override fun invalidateDrawable(dr: Drawable) {
        if (dr != null) {
            initMask(dr)
            swapBitmapMask(makeBitmapMask(dr))
            this.invalidate()
        }
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        if (who != null && what != null) {
            mHandler!!.postAtTime(what, `when`)
        }
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        if (who != null && what != null) {
            mHandler!!.removeCallbacks(what)
        }
    }

    private fun swapBitmapMask(newMask: Bitmap?) {
        if (newMask != null) {
            if (mFinalMask != null && !mFinalMask!!.isRecycled) {
                mFinalMask!!.recycle()
            }
            mFinalMask = newMask
        }
    }

    private fun getModeFromInteger(index: Int): PorterDuffXfermode {
        var mode: PorterDuff.Mode? = null
        when (index) {
            0 -> {
                if (VERSION.SDK_INT >= 11) {
                    mode = PorterDuff.Mode.ADD
                } else {
                    log("MODE_ADD is not supported on api lvl " + VERSION.SDK_INT)
                }
                mode = PorterDuff.Mode.CLEAR
            }
            1 -> mode = PorterDuff.Mode.CLEAR
            2 -> mode = PorterDuff.Mode.DARKEN
            3 -> mode = PorterDuff.Mode.DST
            4 -> mode = PorterDuff.Mode.DST_ATOP
            5 -> mode = PorterDuff.Mode.DST_IN
            6 -> mode = PorterDuff.Mode.DST_OUT
            7 -> mode = PorterDuff.Mode.DST_OVER
            8 -> mode = PorterDuff.Mode.LIGHTEN
            9 -> mode = PorterDuff.Mode.MULTIPLY
            10 -> {
                if (VERSION.SDK_INT >= 11) {
                    mode = PorterDuff.Mode.OVERLAY
                } else {
                    log("MODE_OVERLAY is not supported on api lvl " + VERSION.SDK_INT)
                }
                mode = PorterDuff.Mode.SCREEN
            }
            11 -> mode = PorterDuff.Mode.SCREEN
            12 -> mode = PorterDuff.Mode.SRC
            13 -> mode = PorterDuff.Mode.SRC_ATOP
            14 -> mode = PorterDuff.Mode.SRC_IN
            15 -> mode = PorterDuff.Mode.SRC_OUT
            16 -> mode = PorterDuff.Mode.SRC_OVER
            17 -> mode = PorterDuff.Mode.XOR
            else -> mode = PorterDuff.Mode.DST_IN
        }
        log("Mode is $mode")
        return PorterDuffXfermode(mode)
    }

    companion object {
        private const val TAG = "MaskableFrameLayout"
        private const val MODE_ADD = 0
        private const val MODE_CLEAR = 1
        private const val MODE_DARKEN = 2
        private const val MODE_DST = 3
        private const val MODE_DST_ATOP = 4
        private const val MODE_DST_IN = 5
        private const val MODE_DST_OUT = 6
        private const val MODE_DST_OVER = 7
        private const val MODE_LIGHTEN = 8
        private const val MODE_MULTIPLY = 9
        private const val MODE_OVERLAY = 10
        private const val MODE_SCREEN = 11
        private const val MODE_SRC = 12
        private const val MODE_SRC_ATOP = 13
        private const val MODE_SRC_IN = 14
        private const val MODE_SRC_OUT = 15
        private const val MODE_SRC_OVER = 16
        private const val MODE_XOR = 17
    }
}