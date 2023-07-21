package com.pnt.flutter_wifi

import android.annotation.SuppressLint
import android.net.wifi.WifiManager
import android.os.Build
import io.flutter.plugin.common.EventChannel

class ScanWifiEventChannel(
    private val wifi: WifiManager
) : EventChannel.StreamHandler {
    private var _eventSink: EventChannel.EventSink? = null

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        _eventSink = events
        onScannedResultsAvailable()
    }

    fun onScannedResultsAvailable() {
        _eventSink?.success(getScannedResults())
    }

    @SuppressLint("MissingPermission")
    private fun getScannedResults(): List<Map<String, Any?>> = wifi.scanResults.map { ap ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mapOf(
                "ssid" to ap.wifiSsid,
                "bssid" to ap.BSSID,
                "capabilities" to ap.capabilities,
                "frequency" to ap.frequency,
                "level" to ap.level,
                "timestamp" to ap.timestamp,
                "standard" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ap.wifiStandard else null,
                "centerFrequency0" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.centerFreq0 else null,
                "centerFrequency1" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.centerFreq1 else null,
                "channelWidth" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.channelWidth else null,
                "isPasspoint" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.isPasspointNetwork else null,
                "operatorFriendlyName" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.operatorFriendlyName else null,
                "venueName" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.venueName else null,
                "is80211mcResponder" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.is80211mcResponder else null
            )
        } else {
            mapOf(
                "ssid" to "",
                "bssid" to ap.BSSID,
                "capabilities" to ap.capabilities,
                "frequency" to ap.frequency,
                "level" to ap.level,
                "timestamp" to ap.timestamp,
                "standard" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ap.wifiStandard else null,
                "centerFrequency0" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.centerFreq0 else null,
                "centerFrequency1" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.centerFreq1 else null,
                "channelWidth" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.channelWidth else null,
                "isPasspoint" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.isPasspointNetwork else null,
                "operatorFriendlyName" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.operatorFriendlyName else null,
                "venueName" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.venueName else null,
                "is80211mcResponder" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ap.is80211mcResponder else null
            )
        }
    }

    override fun onCancel(arguments: Any?) {
        _eventSink?.endOfStream()
        _eventSink = null
    }
}