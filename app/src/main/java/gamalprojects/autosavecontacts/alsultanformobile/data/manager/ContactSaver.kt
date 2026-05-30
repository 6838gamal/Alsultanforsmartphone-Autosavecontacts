package gamalprojects.autosavecontacts.alsultanformobile.data.manager

import android.content.Context
import gamalprojects.autosavecontacts.alsultanformobile.data.database.AppDatabase
import gamalprojects.autosavecontacts.alsultanformobile.data.database.CrmContactEntity
import gamalprojects.autosavecontacts.alsultanformobile.services.CrmForegroundService
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ContactSaver {
    private val mutex = Any()

    /**
     * Entry point to inspect, save, and log an incoming contact event.
     */
    fun processIncomingNumberAsync(context: Context, rawNumber: String, source: String) {
        val appScope = CoroutineScope(Dispatchers.IO)
        appScope.launch {
            processIncomingNumber(context, rawNumber, source)
        }
    }

    suspend fun processIncomingNumber(context: Context, rawNumber: String, source: String): Unit = withContext(Dispatchers.IO) {
        val cleanNumber = sanitizePhoneNumber(rawNumber)
        if (cleanNumber.isBlank() || cleanNumber.length < 5) {
            AppLogger.d("Skipped processing invalid phone format: '$rawNumber'", "ContactSaver")
            return@withContext
        }

        synchronized(mutex) {
            val db = AppDatabase.getDatabase(context)
            val dao = db.contactDao()

            try {
                // 1. Check system contacts
                val existsInSystem = ContactsManager.isContactExists(context, cleanNumber)

                // 2. Check local CRM DB
                val existingCrm = kotlinx.coroutines.runBlocking { dao.getContactByPhone(cleanNumber) }

                if (existsInSystem) {
                    if (existingCrm != null) {
                        val updated = existingCrm.copy(
                            communicationCount = existingCrm.communicationCount + 1,
                            lastActivityTimestamp = System.currentTimeMillis()
                        )
                        kotlinx.coroutines.runBlocking { dao.updateContact(updated) }
                        AppLogger.d("رقم جهة الاتصال $cleanNumber مكرر في النظام. تم تحديث العداد ومستوى النشاط في CRM.", "ContactSaver")
                    } else {
                        val newEntity = CrmContactEntity(
                            phoneNumber = cleanNumber,
                            displayName = "عميل محفوظ مسبقاً",
                            source = source,
                            addedTimestamp = System.currentTimeMillis(),
                            lastActivityTimestamp = System.currentTimeMillis(),
                            communicationCount = 1,
                            status = "Saved"
                        )
                        kotlinx.coroutines.runBlocking { dao.insertContact(newEntity) }
                        AppLogger.d("تم تسجيل رقم عميل محفوظ مسبقاً في النظام ($cleanNumber) بجدول CRM.", "ContactSaver")
                    }
                    return@synchronized
                }

                // Not in system contacts
                if (existingCrm != null) {
                    val updated = existingCrm.copy(
                        communicationCount = existingCrm.communicationCount + 1,
                        lastActivityTimestamp = System.currentTimeMillis()
                    )
                    kotlinx.coroutines.runBlocking { dao.updateContact(updated) }

                    // Sync back to system contacts (maybe it was deleted manually)
                    val savedToSystem = ContactsManager.saveContactToSystem(context, existingCrm.displayName, cleanNumber)
                    AppLogger.s("رقم CRM موجود $cleanNumber متصل عبر $source. تم التحقق من سلامة جهة الاتصال: $savedToSystem", "ContactSaver")
                    return@synchronized
                }

                // Create a completely new smart name client
                val currentCount = kotlinx.coroutines.runBlocking { dao.getContactCount() }
                val smartName = if (currentCount == 0) {
                    "عميل جديد"
                } else {
                    "عميل جديد $currentCount"
                }

                AppLogger.log("جاري حفظ جهة اتصال جديدة تلقائياً: $smartName ($cleanNumber)", "ContactSaver")

                // Write to system contacts
                val saveSuccess = ContactsManager.saveContactToSystem(context, smartName, cleanNumber)

                val newCrmContact = CrmContactEntity(
                    phoneNumber = cleanNumber,
                    displayName = smartName,
                    source = source,
                    addedTimestamp = System.currentTimeMillis(),
                    lastActivityTimestamp = System.currentTimeMillis(),
                    communicationCount = 1,
                    status = if (saveSuccess) "Saved" else "Failed"
                )

                kotlinx.coroutines.runBlocking { dao.insertContact(newCrmContact) }

                if (saveSuccess) {
                    AppLogger.s("تم الحفظ التلقائي للعميل بنجاح: $smartName ($cleanNumber) المصدر: $source", "ContactSaver")
                    CrmForegroundService.updateNotification(context)
                } else {
                    AppLogger.e("فشل حفظ الرقم $cleanNumber في جهات اتصال الهاتف. تم تسجيله محلياً في CRM.", "ContactSaver")
                }

            } catch (e: Exception) {
                AppLogger.e("خطأ أثناء معالجة الرقم التلقائي $cleanNumber: ${e.message}", "ContactSaver")
            }
        }
    }

    private fun sanitizePhoneNumber(number: String): String {
        val trimmed = number.trim()
        if (trimmed.startsWith("+")) {
            return "+" + trimmed.filter { it.isDigit() }
        }
        return trimmed.filter { it.isDigit() }
    }
}
