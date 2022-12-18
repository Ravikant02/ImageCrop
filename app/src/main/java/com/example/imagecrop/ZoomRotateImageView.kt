package com.example.imagecrop

import android.animation.Animator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ScaleGestureDetectorCompat

class ZoomRotateImageView : AppCompatImageView, OnScaleGestureListener {
    private val RESET_DURATION = 200
    private var startScaleType: ScaleType? = null
    //private var matrix = Matrix()
    private var startMatrix = Matrix()
    var matrixValues = FloatArray(9)
    private var startValues: FloatArray? = null
    private var minScale = 0.6f
    private var maxScale = 8.0f
    private var calculatedMinScale = 0.6f
    private var calculatedMaxScale = 8.0f
    private val bounds = RectF()
    var isTranslatable = false
    var isZoomable = false
    var doubleTapToZoom = false
    var restrictBounds = false
    var animateOnReset = false
    var autoCenter = false
    private var doubleTapToZoomScaleFactor = 0f
    var autoResetMode = 0
    private val last = PointF(0.0f, 0.0f)
    private var startScale = 1.0f
    private var scaleBy = 1.0f
    var currentScaleFactor = 1.0f
        private set
    private var previousPointerCount = 1
    var mPivotX = 0
    var mPivotY = 0
    private var mLastAngle = 0.0f
    private var mLastScaleFactor = 0.0f
    private var scaleDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var doubleTapDetected = false
    private var singleTapDetected = false
    private val gestureListener: GestureDetector.OnGestureListener

    internal inner class NamelessClass_1 : SimpleOnGestureListener() {
        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            if (e.action == 1) {
                doubleTapDetected = true
            }
            return false
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            singleTapDetected = true
            return false
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            singleTapDetected = false
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }

    constructor(context: Context) : super(context) {
        gestureListener = NamelessClass_1()
        init(context, null as AttributeSet?)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        gestureListener = NamelessClass_1()
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        gestureListener = NamelessClass_1()
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        scaleDetector = ScaleGestureDetector(context, this)
        gestureDetector = GestureDetector(context, gestureListener)
        ScaleGestureDetectorCompat.setQuickScaleEnabled(scaleDetector, false)
        startScaleType = this.scaleType
        val values = context.obtainStyledAttributes(attrs, R.styleable.ZoomRotateImageView)
        isZoomable = values.getBoolean(R.styleable.ZoomRotateImageView_zoomable, true)
        isTranslatable = values.getBoolean(R.styleable.ZoomRotateImageView_translatable, true)
        animateOnReset = values.getBoolean(R.styleable.ZoomRotateImageView_animateOnReset, true)
        autoCenter = values.getBoolean(R.styleable.ZoomRotateImageView_autoCenter, true)
        restrictBounds = values.getBoolean(R.styleable.ZoomRotateImageView_restrictBounds, false)
        doubleTapToZoom = values.getBoolean(R.styleable.ZoomRotateImageView_doubleTapToZoom, true)
        minScale = values.getFloat(R.styleable.ZoomRotateImageView_minScale, 0.6f)
        maxScale = values.getFloat(R.styleable.ZoomRotateImageView_maxScale, 8.0f)
        doubleTapToZoomScaleFactor = values.getFloat(R.styleable.ZoomRotateImageView_doubleTapToZoomScaleFactor, 3.0f)
        autoResetMode = values.getInt(R.styleable.ZoomRotateImageView_autoResetMode, 0)
        verifyScaleRange()
        values.recycle()
    }

    private fun verifyScaleRange() {
        check(minScale < maxScale) { "minScale must be less than maxScale" }
        check(minScale >= 0.0f) { "minScale must be greater than 0" }
    }

    fun setScaleRange(minScale: Float, maxScale: Float) {
        this.minScale = minScale
        this.maxScale = maxScale
        startValues = null
        verifyScaleRange()
    }

    fun getDoubleTapToZoomScaleFactor(): Float {
        return doubleTapToZoomScaleFactor
    }

    fun setDoubleTapToZoomScaleFactor(doubleTapToZoomScaleFactor: Float) {
        this.doubleTapToZoomScaleFactor = doubleTapToZoomScaleFactor
        verifyScaleRange()
    }

    override fun setScaleType(scaleType: ScaleType?) {
        if (scaleType != null) {
            super.setScaleType(scaleType)
            startScaleType = scaleType
            startValues = null
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            this.scaleType = startScaleType
        }
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        this.scaleType = startScaleType
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        this.scaleType = startScaleType
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        this.scaleType = startScaleType
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        this.scaleType = startScaleType
    }

    private fun updateBounds(values: FloatArray) {
        if (this.drawable != null) {
            bounds[values[2], values[5], this.drawable.intrinsicWidth.toFloat() * values[0] + values[2]] = this.drawable.intrinsicHeight.toFloat() * values[4] + values[5]
        }
    }

    private val currentDisplayedWidth: Float
        private get() = if (this.drawable != null) this.drawable.intrinsicWidth.toFloat() * matrixValues[0] else 0.0f
    private val currentDisplayedHeight: Float
        private get() = if (this.drawable != null) this.drawable.intrinsicHeight.toFloat() * matrixValues[4] else 0.0f

    private fun setStartValues() {
        startValues = FloatArray(9)
        startMatrix = Matrix(this.imageMatrix)
        startMatrix.getValues(startValues)
        calculatedMinScale = minScale * startValues!![0]
        calculatedMaxScale = maxScale * startValues!![0]
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (this.isClickable || !this.isEnabled || !isZoomable && !isTranslatable) {
            super.onTouchEvent(event)
        } else {
            if (this.scaleType != ScaleType.MATRIX) {
                super.setScaleType(ScaleType.MATRIX)
            }
            if (startValues == null) {
                setStartValues()
            }
            matrix.set(this.imageMatrix)
            matrix.getValues(matrixValues)
            updateBounds(matrixValues)
            scaleDetector!!.onTouchEvent(event)
            gestureDetector!!.onTouchEvent(event)
            if (doubleTapToZoom && doubleTapDetected) {
                doubleTapDetected = false
                singleTapDetected = false
                if (matrixValues[0] != startValues!![0]) {
                    reset()
                } else {
                    val zoomMatrix = Matrix(matrix)
                    zoomMatrix.postScale(doubleTapToZoomScaleFactor, doubleTapToZoomScaleFactor, scaleDetector!!.focusX, scaleDetector!!.focusY)
                    animateScaleAndTranslationToMatrix(zoomMatrix, 200)
                }
                true
            } else {
                if (!singleTapDetected) {
                    if (event.actionMasked != 0 && event.pointerCount == previousPointerCount) {
                        if (event.actionMasked == 2) {
                            val focusx = scaleDetector!!.focusX
                            val focusy = scaleDetector!!.focusY
                            if (isTranslatable) {
                                val xdistance = getXDistance(focusx, last.x)
                                val ydistance = getYDistance(focusy, last.y)
                                matrix.postTranslate(xdistance, ydistance)
                            }
                            if (isZoomable) {
                                matrix.postScale(scaleBy, scaleBy, focusx, focusy)
                                currentScaleFactor = matrixValues[0] / startValues!![0]
                            }
                            this.imageMatrix = matrix
                            last[focusx] = focusy
                        }
                    } else {
                        last[scaleDetector!!.focusX] = scaleDetector!!.focusY
                    }
                    if (event.actionMasked == 1) {
                        scaleBy = 1.0f
                    }
                }
                previousPointerCount = event.pointerCount
                true
            }
        }
    }

    private fun resetImage() {
        when (autoResetMode) {
            0 -> if (matrixValues[0] <= startValues!![0]) {
                reset()
            } else {
                center()
            }
            1 -> if (matrixValues[0] >= startValues!![0]) {
                reset()
            } else {
                center()
            }
            2 -> reset()
            3 -> center()
        }
    }

    private fun center() {
        if (autoCenter) {
            animateTranslationX()
            animateTranslationY()
        }
    }

    @JvmOverloads
    fun reset(animate: Boolean = animateOnReset) {
    }

    private fun animateToStartMatrix() {
        animateScaleAndTranslationToMatrix(startMatrix, 200)
    }

    private fun animateScaleAndTranslationToMatrix(targetMatrix: Matrix, duration: Int) {
        val targetValues = FloatArray(9)
        targetMatrix.getValues(targetValues)
        val beginMatrix = Matrix(this.imageMatrix)
        beginMatrix.getValues(matrixValues)
        val xsdiff = targetValues[0] - matrixValues[0]
        val ysdiff = targetValues[4] - matrixValues[4]
        val xtdiff = targetValues[2] - matrixValues[2]
        val ytdiff = targetValues[5] - matrixValues[5]
        val anim = ValueAnimator.ofFloat(*floatArrayOf(0.0f, 1.0f))
        anim.addUpdateListener(object : AnimatorUpdateListener {
            val activeMatrix = Matrix(this@ZoomRotateImageView.imageMatrix)
            val values = FloatArray(9)
            override fun onAnimationUpdate(animation: ValueAnimator) {
                val `val` = animation.animatedValue as Float
                activeMatrix.set(beginMatrix)
                activeMatrix.getValues(values)
                values[2] += xtdiff * `val`
                values[5] += ytdiff * `val`
                values[0] += xsdiff * `val`
                values[4] += ysdiff * `val`
                activeMatrix.setValues(values)
                this@ZoomRotateImageView.imageMatrix = activeMatrix
            }
        })
        anim.addListener(object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                this@ZoomRotateImageView.imageMatrix = targetMatrix
            }
        })
        anim.duration = duration.toLong()
        anim.start()
    }

    private fun animateTranslationX() {
        if (currentDisplayedWidth > this.width.toFloat()) {
            if (bounds.left > 0.0f) {
                animateMatrixIndex(2, 0.0f)
            } else if (bounds.right < this.width.toFloat()) {
                animateMatrixIndex(2, bounds.left + this.width.toFloat() - bounds.right)
            }
        } else if (bounds.left < 0.0f) {
            animateMatrixIndex(2, 0.0f)
        } else if (bounds.right > this.width.toFloat()) {
            animateMatrixIndex(2, bounds.left + this.width.toFloat() - bounds.right)
        }
    }

    private fun animateTranslationY() {
        if (currentDisplayedHeight > this.height.toFloat()) {
            if (bounds.top > 0.0f) {
                animateMatrixIndex(5, 0.0f)
            } else if (bounds.bottom < this.height.toFloat()) {
                animateMatrixIndex(5, bounds.top + this.height.toFloat() - bounds.bottom)
            }
        } else if (bounds.top < 0.0f) {
            animateMatrixIndex(5, 0.0f)
        } else if (bounds.bottom > this.height.toFloat()) {
            animateMatrixIndex(5, bounds.top + this.height.toFloat() - bounds.bottom)
        }
    }

    private fun animateMatrixIndex(index: Int, to: Float) {
        val animator = ValueAnimator.ofFloat(*floatArrayOf(matrixValues[index], to))
        animator.addUpdateListener(object : AnimatorUpdateListener {
            val values = FloatArray(9)
            var current = Matrix()
            override fun onAnimationUpdate(animation: ValueAnimator) {
                current.set(this@ZoomRotateImageView.imageMatrix)
                current.getValues(values)
                values[index] = animation.animatedValue as Float
                current.setValues(values)
                this@ZoomRotateImageView.imageMatrix = current
            }
        })
        animator.duration = 200L
        animator.start()
    }

    private fun getXDistance(toX: Float, fromX: Float): Float {
        var xdistance = toX - fromX
        if (restrictBounds) {
            xdistance = getRestrictedXDistance(xdistance)
        }
        if (bounds.right + xdistance < 0.0f) {
            xdistance = -bounds.right
        } else if (bounds.left + xdistance > this.width.toFloat()) {
            xdistance = this.width.toFloat() - bounds.left
        }
        return xdistance
    }

    private fun getRestrictedXDistance(distance: Float): Float {
        var restrictedXDistance = distance
        if (currentDisplayedWidth >= this.width.toFloat()) {
            if (bounds.left <= 0.0f && bounds.left + distance > 0.0f && !scaleDetector!!.isInProgress) {
                restrictedXDistance = -bounds.left
            } else if (bounds.right >= this.width.toFloat() && bounds.right + distance < this.width.toFloat() && !scaleDetector!!.isInProgress) {
                restrictedXDistance = this.width.toFloat() - bounds.right
            }
        } else if (!scaleDetector!!.isInProgress) {
            if (bounds.left >= 0.0f && bounds.left + distance < 0.0f) {
                restrictedXDistance = -bounds.left
            } else if (bounds.right <= this.width.toFloat() && bounds.right + distance > this.width.toFloat()) {
                restrictedXDistance = this.width.toFloat() - bounds.right
            }
        }
        return restrictedXDistance
    }

    private fun getYDistance(toY: Float, fromY: Float): Float {
        var ydistance = toY - fromY
        if (restrictBounds) {
            ydistance = getRestrictedYDistance(ydistance)
        }
        if (bounds.bottom + ydistance < 0.0f) {
            ydistance = -bounds.bottom
        } else if (bounds.top + ydistance > this.height.toFloat()) {
            ydistance = this.height.toFloat() - bounds.top
        }
        return ydistance
    }

    private fun getRestrictedYDistance(distance: Float): Float {
        var restrictedYDistance = distance
        if (currentDisplayedHeight >= this.height.toFloat()) {
            if (bounds.top <= 0.0f && bounds.top + distance > 0.0f && !scaleDetector!!.isInProgress) {
                restrictedYDistance = -bounds.top
            } else if (bounds.bottom >= this.height.toFloat() && bounds.bottom + distance < this.height.toFloat() && !scaleDetector!!.isInProgress) {
                restrictedYDistance = this.height.toFloat() - bounds.bottom
            }
        } else if (!scaleDetector!!.isInProgress) {
            if (bounds.top >= 0.0f && bounds.top + distance < 0.0f) {
                restrictedYDistance = -bounds.top
            } else if (bounds.bottom <= this.height.toFloat() && bounds.bottom + distance > this.height.toFloat()) {
                restrictedYDistance = this.height.toFloat() - bounds.bottom
            }
        }
        return restrictedYDistance
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        scaleBy = startScale * detector.scaleFactor / matrixValues[0]
        val projectedScale = scaleBy * matrixValues[0]
        if (projectedScale < calculatedMinScale) {
            scaleBy = calculatedMinScale / matrixValues[0]
        } else if (projectedScale > calculatedMaxScale) {
            scaleBy = calculatedMaxScale / matrixValues[0]
        }
        return false
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        startScale = matrixValues[0]
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        scaleBy = 1.0f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mPivotX = w / 2
        mPivotY = h / 2
    }

    fun rotate(degrees: Float) {
        if (startValues == null) {
            setStartValues()
        }
        matrix.set(this.imageMatrix)
        matrix.getValues(matrixValues)
        updateBounds(matrixValues)
        matrix.postRotate(degrees - mLastAngle, mPivotX.toFloat(), mPivotY.toFloat())
        this.imageMatrix = matrix
        mLastAngle = degrees
    }

    fun zoom(scaleFactor: Float) {
        if (startValues == null) {
            setStartValues()
        }
        matrix.set(this.imageMatrix)
        matrix.getValues(matrixValues)
        updateBounds(matrixValues)
        matrix.postScale(scaleFactor, scaleFactor, mPivotX.toFloat(), mPivotY.toFloat())
        this.imageMatrix = matrix
        mLastScaleFactor = scaleFactor
    }

    private fun getScaleX(scaleFactor: Float): Float {
        val sx = startScale * scaleFactor / matrixValues[0]
        return sx * matrixValues[0]
    }

    fun resetAll() {
        animateToStartMatrix()
    }

    private open inner class SimpleAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {}
        override fun onAnimationEnd(animation: Animator) {}
        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
    }

    companion object {
        private const val MIN_SCALE = 0.6f
        private const val MAX_SCALE = 8.0f
    }
}