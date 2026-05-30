package gamalprojects.autosavecontacts.alsultanformobile.data.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import gamalprojects.autosavecontacts.alsultanformobile.data.database.CrmContactEntity
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Generates a UTF-8 BOM CSV string of all CRM contacts.
     */
    fun generateCsv(contacts: List<CrmContactEntity>): String {
        val bom = "\uFEFF" // UTF-8 Byte Order Mark so Excel opens Arabic correctly
        val csvBuilder = StringBuilder(bom)
        
        // CSV Headers (in Arabic as requested by user's theme)
        csvBuilder.append("الاسم,الرقم,المصدر,تاريخ الحفظ,عدد مرات التواصل,الحالة\n")
        
        for (c in contacts) {
            val phoneClean = c.phoneNumber.replace(",", " ")
            val nameClean = c.displayName.replace(",", " ")
            val sourceTranslated = when (c.source) {
                "Call" -> "مكالمة"
                "SMS" -> "رسالة قصيرة SMS"
                "WhatsApp" -> "واتساب"
                else -> c.source
            }
            val statusTranslated = when (c.status) {
                "Saved" -> "تم الحفظ"
                "Failed" -> "فشل الحفظ"
                else -> c.status
            }
            
            csvBuilder.append("$nameClean,$phoneClean,$sourceTranslated,${formatDate(c.addedTimestamp)},${c.communicationCount},$statusTranslated\n")
        }
        return csvBuilder.toString()
    }

    /**
     * Generates an HTML XLS file that Excel recognizes. This supports rich styling and Arabic natively.
     */
    fun generateXls(contacts: List<CrmContactEntity>): String {
        val xlsBuilder = StringBuilder()
        xlsBuilder.append("<html>")
        xlsBuilder.append("<head>")
        xlsBuilder.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">")
        xlsBuilder.append("<style>")
        xlsBuilder.append("table { border-collapse: collapse; width: 100%; font-family: sans-serif; direction: rtl; }")
        xlsBuilder.append("th, td { border: 1px solid #dddddd; text-align: right; padding: 10px; }")
        xlsBuilder.append("th { background-color: #f2f2f2; font-weight: bold; color: #333333; }")
        xlsBuilder.append("tr:nth-child(even) { background-color: #fafafa; }")
        xlsBuilder.append("</style>")
        xlsBuilder.append("</head>")
        xlsBuilder.append("<body>")
        xlsBuilder.append("<h2>قاعدة بيانات عملاء Alsultan CRM AutoSave</h2>")
        xlsBuilder.append("<table>")
        xlsBuilder.append("<tr><th>الاسم</th><th>الرقم</th><th>المصدر</th><th>تاريخ الحفظ</th><th>تاريخ آخر تواصل</th><th>مرات التواصل</th><th>حالة الحفظ</th></tr>")
        
        for (c in contacts) {
            val sourceTranslated = when (c.source) {
                "Call" -> "مكالمة 📞"
                "SMS" -> "رسالة SMS 💬"
                "WhatsApp" -> "واتساب 🟢"
                else -> c.source
            }
            val statusTranslated = when (c.status) {
                "Saved" -> "<span style=\"color:green;\">تم الحفظ بنجاح</span>"
                "Failed" -> "<span style=\"color:red;\">فشل الحفظ</span>"
                else -> c.status
            }
            xlsBuilder.append("<tr>")
            xlsBuilder.append("<td>${c.displayName}</td>")
            xlsBuilder.append("<td>${c.phoneNumber}</td>")
            xlsBuilder.append("<td>$sourceTranslated</td>")
            xlsBuilder.append("<td>${formatDate(c.addedTimestamp)}</td>")
            xlsBuilder.append("<td>${formatDate(c.lastActivityTimestamp)}</td>")
            xlsBuilder.append("<td>${c.communicationCount}</td>")
            xlsBuilder.append("<td>$statusTranslated</td>")
            xlsBuilder.append("</tr>")
        }
        xlsBuilder.append("</table>")
        xlsBuilder.append("</body>")
        xlsBuilder.append("</html>")
        
        return xlsBuilder.toString()
    }

    /**
     * Saves exported content to a temporary file and triggers a Share Intent.
     */
    fun shareExportedFile(context: Context, filename: String, content: String): Boolean {
        try {
            val exportDir = File(context.cacheDir, "crm_exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val file = File(exportDir, filename)
            file.writeText(content, Charsets.UTF_8)
            
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (filename.endsWith(".csv")) "text/csv" else "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تصدير جهات الاتصال CRM")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "مشاركة ملف التصدير"))
            AppLogger.s("Successfully shared export file: $filename", "ExportManager")
            return true
        } catch (e: Exception) {
            AppLogger.e("Failed to share export file: ${e.message}", "ExportManager")
            return false
        }
    }
}
