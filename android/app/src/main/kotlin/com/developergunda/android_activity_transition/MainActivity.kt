package com.developergunda.android_activity_transition

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import com.developergunda.android_activity_transition.services.TransitionService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugins.GeneratedPluginRegistrant


class MainActivity : FlutterActivity() {
    private lateinit var connection: ServiceConnection

    private var transitionService: TransitionService? = null

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
    }

    override fun onStart() {
        super.onStart()
        if (doesNotRequirePermission()) {
            bindService(transitionServiceHandler)
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    1234)
        }
    }


    override fun onStop() {
        super.onStop()
        transitionService?.let {
            unbindService(connection)
        }
    }


    private fun bindService(handler: TransitionServiceHandler) {

        connection = object : ServiceConnection {
            override fun onServiceDisconnected(p0: ComponentName?) {
                transitionService = null
            }

            override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
                val binder = service as TransitionService.LocalBinder
                transitionService = binder.getInstance()
                handler.onBound()
            }
        }

        bindService(
                Intent(this, TransitionService::class.java),
                connection,
                Context.BIND_AUTO_CREATE
        )
    }

    private val transitionServiceHandler = object : TransitionServiceHandler {
        override fun onBound() {
            val intent = Intent(this@MainActivity, TransitionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else startService(intent)

            transitionService?.registerEventChannel(EventChannel(flutterEngine?.dartExecutor?.binaryMessenger, "sri"))
        }
    }

    private val runningQOrLater: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun doesNotRequirePermission(): Boolean {
        return if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            true
        }
    }

    interface TransitionServiceHandler {
        fun onBound()
    }
}
