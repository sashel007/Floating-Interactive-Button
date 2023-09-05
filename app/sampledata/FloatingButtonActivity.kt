package ru.ikar.floatingbutton_ikar

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.ikar.floatingbutton_ikar.composables.FloatingButton

class FloatingButtonActivity : ComponentActivity() {

    companion object {
        const val OVERLAY_PERMISSION_REQ_CODE = 1010
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // If permission not granted, prompt user
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            // If permission already granted, start service and finish activity
            val intentService = Intent(this, FloatingButtonService::class.java)
            startService(intentService)
            finish()
        }
    }

    // This is where you add the onActivityResult function
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                // Permission granted, start the service and finish activity.
                val intentService = Intent(this, FloatingButtonService::class.java)
                startService(intentService)
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}
