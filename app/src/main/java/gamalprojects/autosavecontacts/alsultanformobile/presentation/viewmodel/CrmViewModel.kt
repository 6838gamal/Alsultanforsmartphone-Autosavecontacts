package gamalprojects.autosavecontacts.alsultanformobile.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gamalprojects.autosavecontacts.alsultanformobile.data.database.AppDatabase
import gamalprojects.autosavecontacts.alsultanformobile.data.database.CrmContactEntity
import gamalprojects.autosavecontacts.alsultanformobile.data.manager.ContactsManager
import gamalprojects.autosavecontacts.alsultanformobile.data.manager.ExportManager
import gamalprojects.autosavecontacts.alsultanformobile.services.CrmForegroundService
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger
import gamalprojects.autosavecontacts.alsultanformobile.utils.LogLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CrmViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.contactDao()

    // 1. Reactive flow of saved contacts in local DB
    val contactsFlow: StateFlow<List<CrmContactEntity>> = dao.getAllContactsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Reactive flow of system log entries
    val logsFlow = AppLogger.logsFlow

    // 3. Reactive flow indicating whether the Foreground Service is active
    val isServiceRunningFlow = CrmForegroundService.isServiceRunning

    // Count flows
    val contactCountFlow: StateFlow<Int> = dao.getContactCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    init {
        AppLogger.d("تم تهيئة CrmViewModel.", "ViewModel")
    }

    /**
     * Start the CRM Foreground Service
     */
    fun startCrmService(context: Context) {
        val intent = Intent(context, CrmForegroundService::class.java)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            AppLogger.s("تم تشغيل خدمة التسجيل التلقائي للمكالمات والرسائل بنجاح.", "Presenter")
        } catch (e: Exception) {
            AppLogger.e("فشل تشغيل الخدمة بالخلفية: ${e.message}", "Presenter")
        }
    }

    /**
     * Stop the CRM Foreground Service
     */
    fun stopCrmService(context: Context) {
        val intent = Intent(context, CrmForegroundService::class.java).apply {
            action = "STOP_SERVICE"
        }
        try {
            context.startService(intent)
            AppLogger.w("تم إرسال طلب إيقاف الخدمة بالخلفية.", "Presenter")
        } catch (e: Exception) {
            AppLogger.e("فشل إيقاف الخدمة بالخلفية: ${e.message}", "Presenter")
        }
    }

    /**
     * Renames a contact's display name inside both the Local DB and System Contacts
     */
    fun updateContactName(contact: CrmContactEntity, newName: String, context: Context) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                // Update in Room DB
                val updated = contact.copy(displayName = newName)
                dao.updateContact(updated)

                // Update in system address book
                val systemUpdated = ContactsManager.updateSystemContactName(context, contact.phoneNumber, newName)
                if (systemUpdated) {
                    AppLogger.s("تم تعديل الاسم للعميل ${contact.phoneNumber} إلى '$newName' بنجاح.", "Presenter")
                } else {
                    AppLogger.w("تم التعديل بالـ CRM فقط؛ لم يتم العثور عليه في أرقام الهاتف.", "Presenter")
                }
            } catch (e: Exception) {
                AppLogger.e("خطأ أثناء تعديل الاسم للعميل ${contact.phoneNumber}: ${e.message}", "Presenter")
            }
        }
    }

    /**
     * Deletes contact log entity from Local DB and optionally system contacts
     */
    fun deleteContact(contact: CrmContactEntity, deleteFromSystem: Boolean, context: Context) {
        viewModelScope.launch {
            try {
                // Delete from Room DB
                dao.deleteContact(contact)

                // Optionally delete from system address book
                if (deleteFromSystem) {
                    val systemDeleted = ContactsManager.deleteSystemContactByNumber(context, contact.phoneNumber)
                    if (systemDeleted) {
                        AppLogger.s("تم حذف جهة اتصال العميل: ${contact.displayName} من نظام الهاتف وقاعدة البيانات.", "Presenter")
                    } else {
                        AppLogger.d("تم الحذف من قاعدة بيانات CRM فقط.", "Presenter")
                    }
                } else {
                    AppLogger.s("تم حذف سجل العميل ${contact.displayName} من قاعدة بيانات تطبيق CRM فقط.", "Presenter")
                }
                CrmForegroundService.updateNotification(context)
            } catch (e: Exception) {
                AppLogger.e("خطأ أثناء الحذف: ${e.message}", "Presenter")
            }
        }
    }

    /**
     * Performs a full wipe of database logs
     */
    fun clearAllContactsLog() {
        viewModelScope.launch {
            try {
                dao.deleteAllContacts() // We will add a query or custom deleteAllContacts in DAO later, but let's do delete on flow or clean up
                // We'll write the query in DAO to support complete purge
                AppLogger.w("تم مسح السجلات بالكامل من الـ CRM.", "Presenter")
            } catch (e: Exception) {
                AppLogger.e("فشل مسح السجلات: ${e.message}", "Presenter")
            }
        }
    }

    /**
     * Trigger export to CSV
     */
    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            val contacts = contactsFlow.value
            if (contacts.isEmpty()) {
                AppLogger.log("لا يوجد بيانات لتصديرها!", "Export", LogLevel.WARNING)
                return@launch
            }
            val csvContent = ExportManager.generateCsv(contacts)
            val filename = "Alsultan_CRM_Contacts_${System.currentTimeMillis()}.csv"
            ExportManager.shareExportedFile(context, filename, csvContent)
        }
    }

    /**
     * Trigger export to XLSX/XLS (HTML spreadsheet)
     */
    fun exportToXls(context: Context) {
        viewModelScope.launch {
            val contacts = contactsFlow.value
            if (contacts.isEmpty()) {
                AppLogger.log("لا يوجد بيانات لتصديرها!", "Export", LogLevel.WARNING)
                return@launch
            }
            val xlsContent = ExportManager.generateXls(contacts)
            val filename = "Alsultan_CRM_Contacts_${System.currentTimeMillis()}.xls"
            ExportManager.shareExportedFile(context, filename, xlsContent)
        }
    }

    /**
     * Clears local system notification logs
     */
    fun clearUiLogs() {
        AppLogger.clearLogs()
    }
}
