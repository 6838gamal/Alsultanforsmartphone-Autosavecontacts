package gamalprojects.autosavecontacts.alsultanformobile.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import gamalprojects.autosavecontacts.alsultanformobile.MainActivity
import gamalprojects.autosavecontacts.alsultanformobile.data.database.AppDatabase
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CrmForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "autosave_crm_service_channel"
        private const val NOTIFICATION_ID = 45293
        
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        /**
         * Allows external classes (like ContactSaver) to tell the foreground service
         * to refresh its displayed notification stats.
         */
        fun updateNotification(context: Context) {
            if (_isServiceRunning.value) {
                val intent = Intent(context, CrmForegroundService::class.java).apply {
                    action = "UPDATE_STATS"
                }
                try {
                    context.startService(intent)
                } catch (e: Exception) {
                    AppLogger.e("Failed to trigger update notification intent: ${e.message}", "ForegroundService")
                }
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        _isServiceRunning.value = true
        createNotificationChannel()
        AppLogger.s("تم بدء تشغيل الخدمة المراقبة بالخلفية بنجاح CRM Foreground Service", "ForegroundService")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopForegroundService()
            return START_NOT_STICKY
        }

        // Build or Update standard persistent notification
        serviceScope.launch {
            val db = AppDatabase.getDatabase(this@CrmForegroundService)
            val count = db.contactDao().getContactCount()
            buildAndShowNotification(count)
        }

        return START_STICKY
    }

    private fun buildAndShowNotification(savedCount: Int) {
        val stopIntent = Intent(this, CrmForegroundService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "Alsultan CRM AutoSave نشط"
        val notificationContent = "تم حفظ: $savedCount جهة اتصال عميل جديد حتى الآن بالخلفية."

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(notificationContent)
            .setSmallIcon(android.R.drawable.sym_action_call) // default sys icon or similar
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف المراقبة", stopPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 14+ requires foregroundServiceType for services.
            // Using SPECIAL_USE as a safe fallback since we monitor notifications, call states and sms.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundService() {
        _isServiceRunning.value = false
        AppLogger.w("تم إيقاف الخدمة المراقبة بالخلفية يدويًا.", "ForegroundService")
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "مراقبة الاتصالات في الخلفية Autorecord Call/SMS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "خدمة خلفية مخصصة لحفظ أرقام الاتصالات والرسائل الواردة غير المحفوظة"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        _isServiceRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
