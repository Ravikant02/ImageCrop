package com.example.imagecrop

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

class ActivityLauncher private constructor(caller: ActivityResultCaller,
                                           contract: ActivityResultContract<Intent, ActivityResult>,
                                           private var activityResultCallback: ActivityResultCallback<ActivityResult>?) {
    private val launcher: ActivityResultLauncher<Intent>

    init {
        launcher = caller.registerForActivityResult(contract) { result: ActivityResult -> onActivityResult(result) }
    }

    fun launch(intent: Intent, activityResultCallback: ActivityResultCallback<ActivityResult>?) {
        if (activityResultCallback != null) {
            this.activityResultCallback = activityResultCallback
        }
        launcher.launch(intent)
    }

    private fun onActivityResult(result: ActivityResult) {
        if (activityResultCallback != null) activityResultCallback!!.onActivityResult(result)
    }

    interface OnActivityResult {
        fun onActivityResultCallback(requestCode: Int, resultCode: Int, data: Intent?)
    }

    companion object {
        fun registerActivityForResult(
                caller: ActivityResultCaller): ActivityLauncher {
            return ActivityLauncher(caller, ActivityResultContracts.StartActivityForResult(), null)
        }
    }
}