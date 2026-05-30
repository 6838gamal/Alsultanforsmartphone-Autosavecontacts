package gamalprojects.autosavecontacts.alsultanformobile.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM crm_contacts ORDER BY lastActivityTimestamp DESC")
    fun getAllContactsFlow(): Flow<List<CrmContactEntity>>

    @Query("SELECT * FROM crm_contacts WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getContactByPhone(phoneNumber: String): CrmContactEntity?

    @Query("SELECT COUNT(*) FROM crm_contacts")
    suspend fun getContactCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: CrmContactEntity): Long

    @Update
    suspend fun updateContact(contact: CrmContactEntity)

    @Delete
    suspend fun deleteContact(contact: CrmContactEntity)

    @Query("DELETE FROM crm_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Int)

    @Query("DELETE FROM crm_contacts")
    suspend fun deleteAllContacts()

    @Query("SELECT COUNT(*) FROM crm_contacts")
    fun getContactCountFlow(): Flow<Int>
}
