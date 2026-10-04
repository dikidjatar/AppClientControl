package com.xeg911.appclient.core.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.xeg911.shared.data.model.DeviceContact
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads all contacts from the device's address book.
 */
@Singleton
class ContactProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun collect(): List<DeviceContact> {
        val phoneNumbersMap = collectPhoneNumbers()
        return collectContacts(phoneNumbersMap)
    }

    private fun collectPhoneNumbers(): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null, null, null
        )?.use { cursor ->
            val contactIdIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            )
            val numberIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            while (cursor.moveToNext()) {
                val contactId = cursor.getString(contactIdIndex) ?: continue
                val number = cursor.getString(numberIndex) ?: continue
                result.getOrPut(contactId) { mutableListOf() }.add(number)
            }
        }

        return result
    }

    private fun collectContacts(phoneNumbersMap: Map<String, List<String>>): List<DeviceContact> {
        val contacts = mutableListOf<DeviceContact>()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )
        val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"

        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null, null, sortOrder
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            )
            while (cursor.moveToNext()) {
                val contactId = cursor.getString(idIndex) ?: continue
                val displayName = cursor.getString(nameIndex) ?: ""

                contacts.add(
                    DeviceContact(
                        contactId = contactId,
                        displayName = displayName,
                        phoneNumbers = phoneNumbersMap[contactId].orEmpty(),
                        sortKey = displayName.trim().lowercase()
                    )
                )
            }
        }

        return contacts
    }
}