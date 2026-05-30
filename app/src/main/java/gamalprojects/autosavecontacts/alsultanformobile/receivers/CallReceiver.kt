package gamalprojects.autosavecontacts.alsultanformobile.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import gamalprojects.autosavecontacts.alsultanformobile.data.manager.ContactSaver
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger

class CallReceiver : BroadcastReceiver() {

    companion object {
        // Keeps track of ringing or outgoing number across state transitions
        private var lastIncomingNumber: String? = null
        private var isRinging = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            // Extract phone number from EXTRA_INCOMING_NUMBER
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            AppLogger.d("رصد حالة الاتصال الهاتفي: $state, الرقم المستقبل: ${incomingNumber ?: "غير معروف"}", "CallReceiver")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                isRinging = true
                if (!incomingNumber.isNullOrBlank()) {
                    lastIncomingNumber = incomingNumber
                    AppLogger.i("رنين هاتف وارد من الرقم: $incomingNumber", "CallReceiver")
                }
            } 
            else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                // Call is active (answered or outgoing)
                if (!incomingNumber.isNullOrBlank()) {
                    lastIncomingNumber = incomingNumber
                }
                AppLogger.d("الهاتف في حالة نشطة (إجراء مكالمة أو الرد).", "CallReceiver")
            } 
            else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended or missed
                val numberToProcess = lastIncomingNumber
                if (isRinging && !numberToProcess.isNullOrBlank()) {
                    AppLogger.s("انتهت المكالمة الواردة من الرقم: $numberToProcess. جاري التحقق لتسجيل جهة الاتصال ملقائياً...", "CallReceiver")
                    
                    // Trigger the autosave task
                    ContactSaver.processIncomingNumberAsync(context.applicationContext, numberToProcess, "Call")
                }
                // Reset state
                lastIncomingNumber = null
                isRinging = false
            }
        }
    }
}
