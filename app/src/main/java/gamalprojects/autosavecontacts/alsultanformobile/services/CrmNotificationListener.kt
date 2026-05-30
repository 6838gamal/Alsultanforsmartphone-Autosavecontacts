package gamalprojects.autosavecontacts.alsultanformobile.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import gamalprojects.autosavecontacts.alsultanformobile.data.manager.ContactSaver
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger

class CrmNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        AppLogger.s("متصل بخدمة الاستماع للإشعارات! مراقبة إشعارات الواتساب نشطة الآن.", "NotificationListener")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        AppLogger.w("تم إلغاء الاتصال بخدمة الاستماع للإشعارات.", "NotificationListener")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: ""
        // Monitor standard WhatsApp and WhatsApp Business
        if (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b") {
            try {
                val extras = sbn.notification.extras ?: return
                // Extract Title (Usually the sender name or phone number)
                val titleCharSequence = extras.getCharSequence(Notification.EXTRA_TITLE)
                val title = titleCharSequence?.toString()?.trim() ?: ""
                
                // Extract Text (The message body)
                val textCharSequence = extras.getCharSequence(Notification.EXTRA_TEXT)
                val messageText = textCharSequence?.toString()?.trim() ?: ""

                if (title.isBlank()) return

                AppLogger.d("تم رصد إشعار واتساب من $title بخصوص رسالة.", "NotificationListener")

                // If the Title itself contains numbers and fits standard phone patterns, 
                // it is highly likely an unsaved WhatsApp contact (since saved contacts show as text name e.g. "Ahmed")
                val cleanTitleNumeric = title.filter { it.isDigit() || it == '+' }
                
                // Unsaved phone numbers on WA usually show up as "+XXX XXXXXX" or "0XXXXX" (at least 7 numbers)
                if (cleanTitleNumeric.length >= 7 && (title.startsWith("+") || title.any { it.isDigit() })) {
                    AppLogger.i("رصد مرسل واتساب غير محفوظ برقم الهاتف: $title", "NotificationListener")
                    ContactSaver.processIncomingNumberAsync(applicationContext, title, "WhatsApp")
                } else {
                    // It is a text name (meaning the contact is either already in WA contacts, OR WA is just showing a nickname)
                    // If the text content starts with a phone number or there's a phone number context, we can process it as well.
                    // But usually, if WA notification title is a Name, they might already be saved or it's a known group.
                    AppLogger.d("المرسل '$title' يبدو أسماً نصياً. تفاصيل الرسالة تم تجاهل معالجتها كأرقام مجهولة.", "NotificationListener")
                }
            } catch (e: Exception) {
                AppLogger.e("خطأ في معالجة إشعار واتساب: ${e.message}", "NotificationListener")
            }
        }
    }
}
