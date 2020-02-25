package com.developergunda.android_activity_transition.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.widget.Toast
import com.developergunda.android_activity_transition.R
import com.developergunda.android_activity_transition.notification.service.NotificationChannelService
import com.google.android.gms.location.*
import io.flutter.Log
import io.flutter.plugin.common.EventChannel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


private const val ACTION_TRANSITION_RECEIVER = "action_transition_receiver"

class TransitionService : Service(), EventChannel.StreamHandler {

    private var receiver: TransitionReceiver? = null
    private lateinit var transitionsPendingIntent: PendingIntent
    private var activityTrackingEnabled = false
    private var eventChannel: EventChannel? = null

    private var eventSink: EventChannel.EventSink? = null

    override fun onBind(intent: Intent): IBinder = LocalBinder()

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        this.eventSink = events
        this.eventSink?.let { startTransitionActivity() }
    }

    override fun onCancel(arguments: Any?) {
        this.eventSink = null
    }

    //Should be done onBound condition in Activities to get the actual flutter engine which will be used for communicating
    fun registerEventChannel(eventChannel: EventChannel) {
        this.eventChannel = eventChannel
        eventChannel?.let {
            it.setStreamHandler(this)
        }
    }

    fun deRegisterFlutterEngine() {
        eventChannel = null
    }

    fun startTransitionActivity() {
        startForegroundNotification()
        registerTransitionReceiver()
        registerTransitionActivity()
    }

    fun stopTransitionActivity() {
        unregisterTransitionReceiver()
        unregisterTransitionActivity()
        stopForeground(true)
        stopSelf()
    }

    private fun registerTransitionReceiver() {
        receiver = TransitionReceiver(eventSink)
        registerReceiver(receiver, IntentFilter(ACTION_TRANSITION_RECEIVER))
    }

    private fun unregisterTransitionReceiver() {
        receiver?.let {
            unregisterReceiver(receiver)
            receiver = null
        }
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NotificationChannelService.createChannel(this)
            val builder: Notification.Builder =
                    Notification.Builder(this, "sriharsha")
                            .setContentText("Nothing yet")
                            .setContentTitle("Activity Transition service")
                            .setSmallIcon(R.drawable.launch_background)
            startForeground(143, builder.build())
        }
    }

    private fun registerTransitionActivity() {
        transitionsPendingIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_TRANSITION_RECEIVER), 0)

        val request = ActivityTransitionRequest(getTransitionList())
        val task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, transitionsPendingIntent)
        task.addOnSuccessListener {
            activityTrackingEnabled = true
            Log.e("Sri", "Transitions Api was successfully registered.")
            showToast("Transitions Api was successfully registered.")
        }
        task.addOnFailureListener { e ->
            Log.e("Sri", "Transitions Api could NOT be registered: $e")
            showToast("Transitions Api could NOT be registered: $e")
        }
    }

    private fun unregisterTransitionActivity() {
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(transitionsPendingIntent)
                .addOnSuccessListener {
                    activityTrackingEnabled = false
                    Log.e("Sri", "Transitions successfully unregistered.")
                    showToast("Transitions successfully unregistered.")
                }
                .addOnFailureListener { e ->
                    Log.e("Sri", "Transitions Api could NOT be unregistered: $e")
                    showToast("Transitions Api could NOT be unregistered: $e")
                }
    }

    private fun getTransitionList(): ArrayList<ActivityTransition> {
        val activityTransitionList = ArrayList<ActivityTransition>()
        activityTransitionList.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build())
        activityTransitionList.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build())
        activityTransitionList.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build())
        activityTransitionList.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build())
        activityTransitionList.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build())
        activityTransitionList.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build())
        activityTransitionList.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build())
        activityTransitionList.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build())
        return activityTransitionList
    }

    private fun showToast(message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post(Runnable {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        })
    }

    inner class LocalBinder : Binder() {
        fun getInstance(): TransitionService = this@TransitionService
    }

    class TransitionReceiver(private val eventSink: EventChannel.EventSink?) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)
                for (event in result!!.transitionEvents) {
                    val info = "Transition: " + toActivityString(event.activityType) +
                            " (" + toTransitionType(event.transitionType) + ")" + "   " +
                            SimpleDateFormat("hh:mm:ss", Locale.US).format(Date())
                    updateNotification(context, info.toString())
                    val handler = Handler(Looper.getMainLooper())
                    handler.post(Runnable {
                        Toast.makeText(context, info.toString(), Toast.LENGTH_LONG).show()
                        eventSink?.success(info.toString())
                    })
                }
            }
        }

        private fun toActivityString(activity: Int): String? {
            return when (activity) {
                DetectedActivity.STILL -> "STILL"
                DetectedActivity.WALKING -> "WALKING"
                DetectedActivity.IN_VEHICLE -> "VEHICLE"
                DetectedActivity.RUNNING -> "RUNNING"
                else -> "UNKNOWN"
            }
        }

        private fun toTransitionType(transitionType: Int): String? {
            return when (transitionType) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
                else -> "UNKNOWN"
            }
        }

        private fun updateNotification(context: Context, info: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val builder: Notification.Builder =
                        Notification.Builder(context, "sriharsha")
                                .setContentText(info)
                                .setContentTitle("Current Location")
                                .setSmallIcon(R.drawable.launch_background)
                notificationManager.notify(143, builder.build())
            }
        }
    }
}
