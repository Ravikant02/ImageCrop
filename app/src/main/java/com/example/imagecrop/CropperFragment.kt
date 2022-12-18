package com.example.imagecrop

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment

class CropperFragment : Fragment() {
    private var maskableFrameLayout: MaskableFrameLayout? = null
    private val finalImageView: ImageView? = null
    private val reloadButton: Button? = null
    private val downloadButton: Button? = null
    private val toggled = false
    private var initialTouchTrigger = false
    private var rotationBar: SeekBar? = null
    private var zoomBar: SeekBar? = null
    private var rotateBtn: Button? = null
    private var zoomBtn: Button? = null
    private var resetBtn: Button? = null
    private var previewImageView: ZoomRotateImageView? = null
    var sourceBitmap: Bitmap? = null
    var maskBitmap: Bitmap? = null
    private var prevScale = 0.0f
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_cropper, container, false)
        maskableFrameLayout = view.findViewById<View>(R.id.mask_layout) as MaskableFrameLayout
        previewImageView = view.findViewById<View>(R.id.preview_iv) as ZoomRotateImageView
        rotateBtn = view.findViewById<View>(R.id.rotateBtn) as Button
        rotationBar = view.findViewById<View>(R.id.rotationBar) as SeekBar
        zoomBar = view.findViewById<View>(R.id.zoomBar) as SeekBar
        resetBtn = view.findViewById<View>(R.id.resetBtn) as Button
        zoomBtn = view.findViewById<View>(R.id.zoomBtn) as Button
        rotationBar!!.progress = 50
        zoomBar!!.progress = 5
        val context: Context? = this.activity
        rotationBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (!initialTouchTrigger) {
                    previewImageView!!.scaleType = ImageView.ScaleType.MATRIX
                    initialTouchTrigger = true
                }
                val angle = (180 * (seekBar.progress - 50)).toFloat() / 50.0f
                previewImageView!!.rotate(angle)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        zoomBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (!initialTouchTrigger) {
                    previewImageView!!.scaleType = ImageView.ScaleType.MATRIX
                    initialTouchTrigger = true
                }
                val percent = seekBar.progress.toFloat() / 10
                val scaleFactor = 1.0f + (percent - prevScale)
                prevScale = percent
                previewImageView!!.zoom(scaleFactor)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        resetBtn!!.setOnClickListener {
            previewImageView!!.resetAll()
            zoomBar!!.progress = 5
            rotationBar!!.progress = 50
        }
        try {
            if (sourceBitmap != null && maskBitmap != null) {
                setCropper(sourceBitmap, maskBitmap)
            }
        } catch (var7: Exception) {
        }
        return view
    }

    @Throws(Exception::class)
    fun setCropper(sourceBitmap: Bitmap?, maskBitmap: Bitmap?) {
        try {
            if (maskBitmap == null || sourceBitmap == null) {
                if (rotationBar != null) {
                    rotationBar!!.visibility = View.GONE
                }
                if (rotateBtn != null) {
                    rotateBtn!!.visibility = View.GONE
                }
                if (rotateBtn != null) {
                    rotateBtn!!.visibility = View.GONE
                }
                if (rotationBar != null) {
                    rotationBar!!.visibility = View.GONE
                }
                if (resetBtn != null) {
                    resetBtn!!.visibility = View.GONE
                }
                if (zoomBtn != null) {
                    zoomBtn!!.visibility = View.GONE
                }
                if (zoomBar != null) {
                    zoomBar!!.visibility = View.GONE
                }
                throw Exception("Source or mask file can not be null")
            }
        } catch (var4: Exception) {
            throw var4
        }
        this.sourceBitmap = sourceBitmap
        this.maskBitmap = maskBitmap
        if (sourceBitmap != null && maskBitmap != null) {
            if (previewImageView != null && maskableFrameLayout != null) {
                previewImageView!!.setImageBitmap(sourceBitmap)
                val maskDrawable: Drawable = BitmapDrawable(this.resources, maskBitmap)
                maskableFrameLayout!!.setMask(maskDrawable)
            }
            if (rotateBtn != null) {
                rotateBtn!!.visibility = View.VISIBLE
            }
            if (rotationBar != null) {
                rotationBar!!.visibility = View.VISIBLE
            }
            if (resetBtn != null) {
                resetBtn!!.visibility = View.VISIBLE
            }
            if (zoomBtn != null) {
                zoomBtn!!.visibility = View.VISIBLE
            }
            if (zoomBar != null) {
                zoomBar!!.visibility = View.VISIBLE
            }
        } else {
            if (rotateBtn != null) {
                rotateBtn!!.visibility = View.GONE
            }
            if (rotationBar != null) {
                rotationBar!!.visibility = View.GONE
            }
            if (resetBtn != null) {
                resetBtn!!.visibility = View.GONE
            }
            if (zoomBtn != null) {
                zoomBtn!!.visibility = View.GONE
            }
            if (zoomBar != null) {
                zoomBar!!.visibility = View.GONE
            }
        }
    }

    @get:Throws(Exception::class)
    val croppedBitmap: Bitmap
        get() = getCroppedBitmap(sourceBitmap, maskBitmap, previewImageView!!.imageMatrix)

    @Throws(Exception::class)
    private fun getCroppedBitmap(src: Bitmap?, maskBitmap: Bitmap?, reqMatrix: Matrix): Bitmap {
        try {
            if (maskBitmap == null || src == null) {
                throw Exception("Source or mask file can not be null")
            }
        } catch (var18: Exception) {
            throw var18
        }
        val sd = getDeviceDensity(this.activity)
        val sw = src.width
        val sh = src.height
        val mw = maskBitmap.width
        val mh = maskBitmap.height
        val mx = (sw - mw) / 2
        val my = (sh - mh) / 2
        val output = Bitmap.createBitmap(mw, mh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(1)
        paint.color = -65536
        canvas.drawBitmap(maskBitmap, 0.0f, 0.0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val matrix = Matrix()
        matrix.set(reqMatrix)
        val values = FloatArray(9)
        matrix.getValues(values)
        val dx = sd / 2.5f
        for (i in View.VISIBLE until values.size - 1) {
            values[i] /= dx
            Log.d("Output", "Matrix " + values[i])
        }
        matrix.setValues(values)
        canvas.drawBitmap(src, matrix, paint)
        return output
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val `is` = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(`is`)
            assert(`is` != null)
            `is`!!.close()
            bitmap
        } catch (var5: Exception) {
            null
        }
    }

    companion object {
        fun getDeviceDensity(context: Context?): Float {
            val metrics = context!!.resources.displayMetrics
            return metrics.density
        }
    }
}