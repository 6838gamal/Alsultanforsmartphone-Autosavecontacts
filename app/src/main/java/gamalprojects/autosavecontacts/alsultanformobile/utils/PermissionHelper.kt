package gamalprojects.autosavecontacts.alsultanformobile.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import gamalprojects.autosavecontacts.alsultanformobile.services.CrmAccessibilityService
import gamalprojects.autosavecontacts.alsultanformobile.services.CrmNotificationListener

object PermissionHelper {

    /**
     * Checks list of raw Android manifest permissions.
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if standard runtime permissions are granted.
     */
    fun hasContactsPermissions(context: Context): Boolean {
        return hasPermission(context, android.Manifest.permission.READ_CONTACTS) &&
                hasPermission(context, android.Manifest.permission.WRITE_CONTACTS)
    }

    fun hasSmsPermissions(context: Context): Boolean {
        return hasPermission(context, android.Manifest.permission.RECEIVE_SMS) &&
                hasPermission(context, android.Manifest.permission.READ_SMS)
    }

    fun hasCallsPermissions(context: Context): Boolean {
        return hasPermission(context, android.Manifest.permission.READ_CALL_LOG) &&
                hasPermission(context, android.Manifest.permission.READ_PHONE_STATE)
    }

    fun hasNotificationPostPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    /**
     * Check if our Notification Listener Service is permitted by the system.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = android.content.ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == context.packageName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Action intent to open the notification access settings.
     */
    fun getNotificationListenerSettingsIntent(): Intent {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        } else {
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
        }
        return Intent(action)
    }

    /**
     * Check if Accessibility Service is enabled in System.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = "${context.packageName}/${CrmAccessibilityService::class.java.canonicalName}"
        val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return flat?.contains(expectedComponentName) == true
    }

    /**
     * Action intent to open Accessibility settings.
     */
    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    /**
     * Check if App is exempt from Battery Optimization (Doze Mode).
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    /**
     * Action intent to request ignoring battery optimization.
     */
    fun getRequestBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
