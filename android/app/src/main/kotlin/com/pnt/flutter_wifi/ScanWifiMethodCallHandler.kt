package com.pnt.flutter_wifi

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import kotlin.random.Random

class ScanWifiMethodCallHandler(
    private val activity: Activity,
    private val context: Context,
    private val wifi: WifiManager,
) : MethodCallHandler {
    companion object {
        /** Error Codes */
        private const val ERROR_INVALID_ARGS = "InvalidArgs"
        private const val ERROR_NULL_ACTIVITY = "NullActivity"

        /** CanStartScan codes */
        private const val CAN_START_SCAN_YES = 1
        private const val CAN_START_SCAN_NO_LOC_PERM_REQUIRED = 2
        private const val CAN_START_SCAN_NO_LOC_PERM_DENIED = 3
        private const val CAN_START_SCAN_NO_LOC_PERM_UPGRADE_ACCURACY = 4
        private const val CAN_START_SCAN_NO_LOC_DISABLED = 5

        /** CanGetScannedResults codes */
        private const val CAN_GET_RESULTS_YES = 1
        private const val CAN_GET_RESULTS_NO_LOC_PERM_REQUIRED = 2
        private const val CAN_GET_RESULTS_NO_LOC_PERM_DENIED = 3
        private const val CAN_GET_RESULTS_NO_LOC_PERM_UPGRADE_ACCURACY = 4
        private const val CAN_GET_RESULTS_NO_LOC_DISABLED = 5

        /** Magic codes */
        private const val ASK_FOR_LOC_PERM = -1

        private val requestPermissionCookie =
            mutableMapOf<Int, (grantResults: IntArray) -> Boolean>()
        private val locationPermissionCoarse = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        private val locationPermissionFine = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        private val locationPermissionBoth = locationPermissionCoarse + locationPermissionFine

        private enum class AskLocPermResult {
            GRANTED, UPGRADE_TO_FINE, DENIED, ERROR_NO_ACTIVITY
        }
    }

    private val logTag = javaClass.simpleName

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "canStartScan" -> {
                val askPermission = call.argument<Boolean>("askPermissions") ?: return result.error(
                    ERROR_INVALID_ARGS,
                    "askPermissions argument is null",
                    null
                )

                when (val canCode = canStartScan(askPermission)) {
                    ASK_FOR_LOC_PERM -> askForLocationPermission { askResult ->
                        when (askResult) {
                            AskLocPermResult.GRANTED -> {
                                result.success(canStartScan(askPermission = false))
                            }
                            AskLocPermResult.UPGRADE_TO_FINE -> {
                                result.success(CAN_START_SCAN_NO_LOC_PERM_UPGRADE_ACCURACY)
                            }
                            AskLocPermResult.DENIED -> {
                                result.success(CAN_START_SCAN_NO_LOC_PERM_DENIED)
                            }
                            AskLocPermResult.ERROR_NO_ACTIVITY -> {
                                result.error(
                                    ERROR_NULL_ACTIVITY,
                                    "Cannot ask for location permission.",
                                    "Looks like called from non-Activity."
                                )
                            }
                        }
                    }
                    else -> result.success(canCode)
                }
            }

            "startScan" -> result.success(startScan())

            "canGetScannedResults" -> {
                val askPermission = call.argument<Boolean>("askPermissions") ?: return result.error(
                    ERROR_INVALID_ARGS,
                    "askPermissions argument is null",
                    null
                )
                when (val canCode = canGetScannedResults(askPermission)) {
                    ASK_FOR_LOC_PERM -> askForLocationPermission { askResult ->
                        when (askResult) {
                            AskLocPermResult.GRANTED -> {
                                result.success(canGetScannedResults(askPermission = false))
                            }
                            AskLocPermResult.UPGRADE_TO_FINE -> {
                                result.success(CAN_GET_RESULTS_NO_LOC_PERM_UPGRADE_ACCURACY)
                            }
                            AskLocPermResult.DENIED -> {
                                result.success(CAN_GET_RESULTS_NO_LOC_PERM_DENIED)
                            }
                            AskLocPermResult.ERROR_NO_ACTIVITY -> {
                                result.error(
                                    ERROR_NULL_ACTIVITY,
                                    "Cannot ask for location permission.",
                                    "Looks like called from non-Activity."
                                )
                            }
                        }
                    }
                    else -> result.success(canCode)
                }
            }

            "getScannedResults" -> result.success(getScannedResults())

            else -> result.notImplemented()
        }
    }

    private fun startScan(): Boolean = wifi.startScan()

    private fun canStartScan(askPermission: Boolean): Int {
        val hasLocPerm = hasLocationPermission()
        val isLocEnabled = isLocationEnabled()
        return when {
            // for SDK < P[28] : Not in guide, should not require any additional permissions
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> CAN_START_SCAN_YES
            // for SDK >= Q[29]: CHANGE_WIFI_STATE & ACCESS_x_LOCATION & "Location enabled"
            hasLocPerm && isLocEnabled -> CAN_START_SCAN_YES
            hasLocPerm -> CAN_START_SCAN_NO_LOC_DISABLED
            askPermission -> ASK_FOR_LOC_PERM
            else -> CAN_START_SCAN_NO_LOC_PERM_REQUIRED
        }
    }

    private fun hasLocationPermission(): Boolean {
        val permissions = when {
            requiresFineLocation() -> locationPermissionFine
            else -> locationPermissionBoth
        }
        return permissions.any { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiresFineLocation(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.Q

    private fun isLocationEnabled(): Boolean =
        isLocationEnabled(
            context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        )

    private fun askForLocationPermission(callback: (AskLocPermResult) -> Unit) {
        // make permissions
        val requiresFine = requiresFineLocation()
        // - for SDK > R[30] - cannot only ask for FINE
        val requiresFineButAskBoth = requiresFine && Build.VERSION.SDK_INT > Build.VERSION_CODES.R
        val permissions = when {
            requiresFineButAskBoth -> locationPermissionBoth
            requiresFine -> locationPermissionFine
            else -> locationPermissionCoarse
        }
        // request permission - add result-handler in requestPermissionCookie
        val permissionCode = 6567800 + Random.Default.nextInt(100)
        requestPermissionCookie[permissionCode] = { grantArray ->
            // invoke callback with proper askResult
            Log.d(logTag, "permissionResultCallback: args($grantArray)")
            callback.invoke(
                when {
                    // GRANTED: if all granted
                    grantArray.all { it == PackageManager.PERMISSION_GRANTED } -> {
                        AskLocPermResult.GRANTED
                    }

                    // UPGRADE_TO_FINE: if requiresFineButAskBoth and COARSE granted
                    requiresFineButAskBoth && grantArray.first() == PackageManager.PERMISSION_GRANTED -> {
                        AskLocPermResult.UPGRADE_TO_FINE
                    }

                    else -> AskLocPermResult.DENIED
                }
            )
            true
        }
        ActivityCompat.requestPermissions(activity, permissions, permissionCode)
    }

    private fun canGetScannedResults(askPermission: Boolean): Int {
        // check all prerequisite conditions
        // ACCESS_WIFI_STATE & ACCESS_x_LOCATION & "Location enabled"
        val hasLocPerm = hasLocationPermission()
        val isLocEnabled = isLocationEnabled()
        return when {
            hasLocPerm && isLocEnabled -> CAN_GET_RESULTS_YES
            hasLocPerm -> CAN_GET_RESULTS_NO_LOC_DISABLED
            askPermission -> ASK_FOR_LOC_PERM
            else -> CAN_GET_RESULTS_NO_LOC_PERM_REQUIRED
        }
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
}