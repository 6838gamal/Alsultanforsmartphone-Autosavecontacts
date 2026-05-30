package gamalprojects.autosavecontacts.alsultanformobile.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import gamalprojects.autosavecontacts.alsultanformobile.data.manager.ContactSaver
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger
import java.util.regex.Pattern

class CrmAccessibilityService : AccessibilityService() {

    private val phonePattern = Pattern.compile("(\\+?[0-9][\\s-]*){7,18}")

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppLogger.s("تم تشغيل خدمة إمكانية الوصول بنجاح Accessibility Service", "AccessibilityService")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Intercept WhatsApp, Phone calls, etc.
        val packageName = event.packageName?.toString() ?: ""
        
        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b" || packageName.contains("telephony") || packageName.contains("dialer")) {
            val sourceNode = event.source ?: return
            inspectNode(sourceNode, packageName)
        }
    }

    private fun inspectNode(node: AccessibilityNodeInfo?, packageName: String) {
        if (node == null) return

        // If node contains text, check if it matches a phone number
        val text = node.text?.toString()?.trim() ?: ""
        if (text.isNotBlank() && text.length >= 7) {
            val matcher = phonePattern.matcher(text)
            if (matcher.matches()) {
                val digitsOnly = text.filter { it.isDigit() || it == '+' }
                if (digitsOnly.length >= 7) {
                    AppLogger.d("تم التقاط رقم هاتف محتمل من شاشة $packageName: $text", "AccessibilityService")
                    
                    // Automatically process it as a potential background customer save context!
                    val source = if (packageName.contains("whatsapp")) "WhatsApp Space" else "Call Context"
                    ContactSaver.processIncomingNumberAsync(applicationContext, text, source)
                }
            }
        }

        // Recursively inspect child nodes
        for (i in 0 until node.childCount) {
            inspectNode(node.getChild(i), packageName)
        }
    }

    override fun onInterrupt() {
        AppLogger.w("تم إيقاف خدمة إمكانية الوصول مؤقتاً.", "AccessibilityService")
    }
}
