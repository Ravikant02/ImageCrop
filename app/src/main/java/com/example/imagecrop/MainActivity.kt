package com.example.imagecrop

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.imagecrop.ActivityLauncher.OnActivityResult
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnActivityResult {
    private var cropperFragment: CropperFragment? = null
    private val REQUEST_IMAGE_CAPTURE = 4
    private var mCurrentPhotoPath: String? = null
    private var cropFrameLayout: FrameLayout? = null
    private var resultImage: ImageView? = null
    private var btnDownload: Button? = null
    private var btnCamera: Button? = null
    private var btnUpload: Button? = null
    private val activityLauncher = ActivityLauncher.registerActivityForResult(this)
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    dispatchTakePictureIntent(this)
                }
            }

    /*private val permissionLauncher = registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions(),
            ActivityResultCallback<Map<String?, Boolean?>> { result: Map<String?, Boolean?> ->
                if (result[Manifest.permission.CAMERA] != null) {
                    if (Objects.requireNonNull(result[Manifest.permission.CAMERA]) == true) {
                        dispatchTakePictureIntent(this)

                        *//*if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                            dispatchTakePictureIntent(this);
                        }*//*
                    }
                } *//*else if (result.get(Manifest.permission.READ_EXTERNAL_STORAGE) != null){
                    if (Objects.requireNonNull(result.get(Manifest.permission.READ_EXTERNAL_STORAGE)).equals(true)){
                        if (isCameraSelected) dispatchTakePictureIntent(context);
                        else dispatchToGalleryIntent();
                    }else {
                        CommonHelper.shortSnackbarBuilder(context, context.getResources().getString(R.string.storage_permission_msg));
                    }
                }*//*
            })*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cropFrameLayout = findViewById(R.id.cropFrameLayout)
        resultImage = findViewById(R.id.imgResult)
        btnCamera = findViewById(R.id.btnOpenCamera)
        btnDownload = findViewById(R.id.btnDownload)
        btnUpload = findViewById(R.id.btnSelectImage)
        cropperFragment = CropperFragment()
        val context: Context = this
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.add(R.id.cropFrameLayout, cropperFragment!!).commit()
        btnCamera?.setOnClickListener(View.OnClickListener {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent(context)
            } else {
                //permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE))
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        })
        btnDownload?.setOnClickListener(View.OnClickListener {
            try {
                val output = cropperFragment!!.croppedBitmap
                if (output != null) {
                    cropFrameLayout?.visibility = View.GONE
                    btnCamera?.visibility = View.GONE
                    btnUpload?.visibility = View.GONE
                    btnDownload?.visibility = View.GONE
                    resultImage?.visibility = View.VISIBLE
                    resultImage?.setImageBitmap(output)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
    }

    private fun initCropper(context: Context, imageUri: Uri) {
        try {
            Glide.with(context)
                    .asBitmap()
                    .load(R.drawable.mask1)
                    .into(object : SimpleTarget<Bitmap?>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                            val sourceBitmap = getBitmapFromUri(context, imageUri)
                            if (sourceBitmap != null) {
                                try {
                                    cropFrameLayout!!.visibility = View.VISIBLE
                                    btnDownload!!.visibility = View.VISIBLE
                                    cropperFragment!!.setCropper(sourceBitmap, resource)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    })
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun dispatchTakePictureIntent(context: Context) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(context.packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile(context)
                mCurrentPhotoPath = photoFile.absolutePath
            } catch (ex: IOException) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                var photoURI: Uri? = null
                photoURI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(context, context.resources.getString(R.string.authorities, BuildConfig.APPLICATION_ID), photoFile)
                } else {
                    Uri.fromFile(photoFile)
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                activityLauncher.launch(intent) { result: ActivityResult -> onActivityResultCallback(REQUEST_IMAGE_CAPTURE, result.resultCode, result.data!!) }
            }
        }
    }

    override fun onActivityResultCallback(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (mCurrentPhotoPath != null) {
                initCropper(this, getUriFromPath(mCurrentPhotoPath!!))
            }
        }
    }

    companion object {
        private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
            return try {
                val `is` = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(`is`)
                assert(`is` != null)
                `is`!!.close()
                bitmap
            } catch (ex: Exception) {
                //Loggers.error(ex.getMessage());
                null
            }
        }

        @Throws(IOException::class)
        private fun createImageFile(context: Context): File {
            // Create an image file name
            @SuppressLint("SimpleDateFormat") val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imageFileName = "JPEG_" + timeStamp + "_"
            val storageDir = Objects.requireNonNull(context).getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            // Save a file: path for use with ACTION_VIEW intents
            // mCurrentPhotoPath = image.getAbsolutePath();
            return File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",  /* suffix */
                    storageDir /* directory */
            )
        }

        private fun getUriFromPath(path: String): Uri {
            return Uri.fromFile(File(path))
        }
    }
}