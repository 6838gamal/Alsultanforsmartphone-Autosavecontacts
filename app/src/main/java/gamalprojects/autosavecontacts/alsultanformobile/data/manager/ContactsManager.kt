package gamalprojects.autosavecontacts.alsultanformobile.data.manager

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import gamalprojects.autosavecontacts.alsultanformobile.utils.AppLogger

object ContactsManager {

    /**
     * Checks if a phone number already exists in Android Contacts.
     */
    fun isContactExists(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            AppLogger.w("READ_CONTACTS permission not granted. Cannot check system contacts.", "ContactsManager")
            return false
        }

        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    AppLogger.d("Phone $phoneNumber found in system contacts as: $name", "ContactsManager")
                    return true
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error querying system contacts for $phoneNumber: ${e.message}", "ContactsManager")
        }
        return false
    }

    /**
     * Writes a new mobile contact into the Android system address book.
     */
    fun saveContactToSystem(context: Context, displayName: String, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank() || displayName.isBlank()) return false
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            AppLogger.e("WRITE_CONTACTS permission not granted. Cannot save contact.", "ContactsManager")
            return false
        }

        try {
            val ops = ArrayList<ContentProviderOperation>()

            // Step 1: Insert Raw Contact
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build())

            // Step 2: Insert Name Info
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build())

            // Step 3: Insert Mobile Phone Info
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build())

            // Execute the batch operation
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            AppLogger.s("Successfully saved contact: $displayName ($phoneNumber) to system", "ContactsManager")
            return true
        } catch (e: Exception) {
            AppLogger.e("Failed to write contact directly to device storage: ${e.message}", "ContactsManager")
            return false
        }
    }

    /**
     * Updates an existing contact's display name inside Android System contacts
     */
    fun updateSystemContactName(context: Context, phoneNumber: String, newName: String): Boolean {
        val hasWrite = context.checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasRead = context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasWrite || !hasRead) return false

        try {
            // Find system contact raw ID or look up first
            var rawContactId: Long? = null
            val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID)
            val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%$phoneNumber%")
            context.contentResolver.query(phoneUri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    rawContactId = cursor.getLong(0)
                }
            }

            if (rawContactId != null) {
                val where = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                val args = arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)

                val ops = ArrayList<ContentProviderOperation>()
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(where, args)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
                    .build())
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                AppLogger.s("System contact name for $phoneNumber updated to '$newName'", "ContactsManager")
                return true
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to update system contact name: ${e.message}", "ContactsManager")
        }
        return false
    }

    /**
     * Deletes a contact from the Android Contacts system by their phone number.
     */
    fun deleteSystemContactByNumber(context: Context, phoneNumber: String): Boolean {
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return false

        try {
            val contactUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(contactUri, arrayOf(ContactsContract.PhoneLookup.LOOKUP_KEY), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val lookupKey = cursor.getString(0)
                    val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
                    context.contentResolver.delete(uri, null, null)
                    AppLogger.s("Deleted contact with phone $phoneNumber from system contacts.", "ContactsManager")
                    return true
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Exception trying to delete system contact: ${e.message}", "ContactsManager")
        }
        return false
    }
}
