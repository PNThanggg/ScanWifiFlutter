package com.pnt.flutter_wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel

    private var wifi: WifiManager? = null
    private var wifiScanReceiver: BroadcastReceiver? = null

    private var scanWifiEventChannel: ScanWifiEventChannel ?= null
    private var scanWifiMethodCallHandler: ScanWifiMethodCallHandler ?= null

    companion object {
        private const val CHANNEL_NAME = "wifi_scan"
        private const val EVENT_NAME = "wifi_scan/onScannedResultsAvailable"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        scanWifiEventChannel = ScanWifiEventChannel(
            wifi = wifi!!
        )

        scanWifiMethodCallHandler = ScanWifiMethodCallHandler(
            activity = activity,
            context = context,
            wifi = wifi!!
        )

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                    scanWifiEventChannel?.onScannedResultsAvailable()
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(scanWifiMethodCallHandler)

        eventChannel = EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            EVENT_NAME
        )
        eventChannel.setStreamHandler(scanWifiEventChannel)
    }

    override fun onDestroy() {
        super.onDestroy()

        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)

        wifi = null
        context.unregisterReceiver(wifiScanReceiver)
        wifiScanReceiver = null
    }
}
