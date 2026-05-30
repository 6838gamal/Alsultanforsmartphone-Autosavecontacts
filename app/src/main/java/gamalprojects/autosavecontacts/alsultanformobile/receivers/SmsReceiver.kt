package gamalprojects.autosavecontacts.alsultanformobile.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import gamalprojects.autosavecontacts.alsultanformobile.data.manager.ContactSaver
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isEmpty()) return

                // Extract sender from the first message segment
                val senderAddress = messages[0].originatingAddress ?: ""
                val body = messages[0].messageBody ?: ""

                if (senderAddress.isNotBlank()) {
                    AppLogger.i("رصد رسالة قصيرة SMS واردة من: $senderAddress. نص الرسالة: $body", "SmsReceiver")
                    
                    // Trigger the Contact Saver to inspect/save this number
                    ContactSaver.processIncomingNumberAsync(context.applicationContext, senderAddress, "SMS")
                }
            } catch (e: Exception) {
                AppLogger.e("خطأ أثناء استلام ومعالجة رسالة SMS: ${e.message}", "SmsReceiver")
            }
        }
    }
}
