package gamalprojects.autosavecontacts.alsultanformobile.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crm_contacts")
data class CrmContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val displayName: String,
    val source: String, // "Call", "SMS", "WhatsApp"
    val addedTimestamp: Long,
    val lastActivityTimestamp: Long,
    val communicationCount: Int = 1,
    val status: String = "Saved" // "Saved", "Failed"
)
