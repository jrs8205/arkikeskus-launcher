package org.arkikeskus.launcher.data.search

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.arkikeskus.launcher.data.SettingsRepository
import org.arkikeskus.launcher.model.SearchResult
import javax.inject.Inject

/** A raw contact row, decoupled from Android `Cursor` so the provider stays JVM-testable. */
data class RawContact(
    val name: String,
    val lookupUri: String,
    val number: String?,
    val photoUri: String?,
    val id: Long,
)

/** Reads contacts matching a query. Real impl backed by ContentResolver; faked in tests. */
interface ContactDataSource {
    suspend fun search(query: String, limit: Int): List<RawContact>
}

/** Permission gate seam (real impl checks the running app; faked in tests). */
fun interface PermissionChecker {
    fun has(permission: String): Boolean
}

/**
 * Searches contacts, gated by the [SettingsRepository.searchContacts] setting AND the READ_CONTACTS
 * runtime permission. Never throws for normal input; a revoked-permission ContentResolver failure is
 * swallowed by the [SearchAggregator]'s runCatching.
 */
class ContactSearchProvider @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val permissionChecker: PermissionChecker,
    private val dataSource: ContactDataSource,
) : SearchProvider {

    override suspend fun isEnabled(): Boolean =
        settingsRepository.settings.first().searchContacts &&
            permissionChecker.has(Manifest.permission.READ_CONTACTS)

    override suspend fun query(query: String): List<SearchResult> =
        dataSource.search(query, LIMIT).map {
            SearchResult.Contact(it.name, it.lookupUri, it.number, it.photoUri, it.id)
        }

    private companion object { const val LIMIT = 5 }
}

/** Real ContentResolver-backed contact search. Verified on-device (not unit-tested). */
class ContentResolverContactDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : ContactDataSource {

    override suspend fun search(query: String, limit: Int): List<RawContact> = withContext(Dispatchers.IO) {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(query))
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
        )
        val out = ArrayList<RawContact>()
        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val lookupCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val nameCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val photoCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            val hasPhoneCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            while (c.moveToNext() && out.size < limit) {
                val id = c.getLong(idCol)
                val lookup = c.getString(lookupCol)
                val name = c.getString(nameCol) ?: continue
                val photo = c.getString(photoCol)
                val number = if (c.getInt(hasPhoneCol) > 0) primaryNumber(id) else null
                val lookupUri = ContactsContract.Contacts.getLookupUri(id, lookup)?.toString()
                    ?: ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id).toString()
                out.add(RawContact(name, lookupUri, number, photo, id))
            }
        }
        out
    }

    private fun primaryNumber(contactId: Long): String? {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )
        return cursor?.use { if (it.moveToFirst()) it.getString(0) else null }
    }
}

/** Real permission checker. */
class AndroidPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : PermissionChecker {
    override fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}
