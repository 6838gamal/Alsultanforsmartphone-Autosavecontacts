package gamalprojects.autosavecontacts.alsultanformobile.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import gamalprojects.autosavecontacts.alsultanformobile.services.CrmForegroundService
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AppLogger.s("تم تشغيل الجهاز! جاري تشغيل CRM Foreground Service للتسجيل التلقائي...", "BootReceiver")
            
            try {
                val serviceIntent = Intent(context, CrmForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                AppLogger.e("فشل بدء خدمة الخلفية بعد إعادة تشغيل الجهاز: ${e.message}", "BootReceiver")
            }
        }
    }
}
