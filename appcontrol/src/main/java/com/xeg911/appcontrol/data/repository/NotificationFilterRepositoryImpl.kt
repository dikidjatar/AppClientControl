package com.xeg911.appcontrol.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.xeg911.appcontrol.core.util.FirebaseNode
import com.xeg911.appcontrol.core.util.NotificationFilterField
import com.xeg911.appcontrol.core.util.getValueOrNull
import com.xeg911.appcontrol.core.util.observeFlow
import com.xeg911.appcontrol.domain.repository.NotificationFilterRepository
import com.xeg911.shared.data.model.notification.NotificationSourceDef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationFilterRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
) : NotificationFilterRepository {

    private val sourcesRef
        get() = database.reference
            .child(FirebaseNode.NODE_NOTIFICATION_FILTER_CONFIG)
            .child(NotificationFilterField.NODE_SOURCES)

    private fun sourceRef(id: String) = sourcesRef.child(id)

    override fun observeSources(): Flow<Result<List<NotificationSourceDef>>> =
        sourcesRef.observeFlow().map { result ->
            result.map { snapshot ->
                snapshot.children.mapNotNull { it.toSourceDef() }
                    .sortedWith(compareByDescending<NotificationSourceDef> { it.enabled }
                        .thenBy { it.label.lowercase() })
            }
        }

    override suspend fun upsertSource(source: NotificationSourceDef): Result<Unit> = runCatching {
        require(source.id.isNotBlank()) { "Source id is required" }
        sourceRef(source.id).setValue(source.toMap()).await()
    }

    override suspend fun setEnabled(sourceId: String, enabled: Boolean): Result<Unit> =
        runCatching {
            sourceRef(sourceId)
                .child(NotificationFilterField.FIELD_ENABLED)
                .setValue(enabled)
                .await()
        }

    override suspend fun deleteSource(sourceId: String): Result<Unit> = runCatching {
        sourceRef(sourceId).removeValue().await()
    }

    private fun DataSnapshot.toSourceDef(): NotificationSourceDef? {
        val id = key ?: return null
        val defaults = NotificationSourceDef()
        return NotificationSourceDef(
            id = id,
            label = child(NotificationFilterField.FIELD_LABEL).getValueOrNull<String>() ?: id,
            enabled = child(NotificationFilterField.FIELD_ENABLED).getValueOrNull<Boolean>()
                ?: defaults.enabled,
            headerEmoji = child(NotificationFilterField.FIELD_HEADER_EMOJI).getValueOrNull<String>()
                ?: defaults.headerEmoji,
            fromEmoji = child(NotificationFilterField.FIELD_FROM_EMOJI).getValueOrNull<String>()
                ?: defaults.fromEmoji,
            packages = child(NotificationFilterField.FIELD_PACKAGES).stringList(),
            usesDefaultSms = child(NotificationFilterField.FIELD_USES_DEFAULT_SMS)
                .getValueOrNull<Boolean>() ?: false,
            noisePatterns = child(NotificationFilterField.FIELD_NOISE_PATTERNS).stringList(),
        )
    }

    private fun DataSnapshot.stringList(): List<String> =
        (value as? List<*>)?.filterIsInstance<String>().orEmpty()

    private fun NotificationSourceDef.toMap(): Map<String, Any?> = mapOf(
        NotificationFilterField.FIELD_LABEL to label.trim().ifBlank { id },
        NotificationFilterField.FIELD_ENABLED to enabled,
        NotificationFilterField.FIELD_HEADER_EMOJI to headerEmoji,
        NotificationFilterField.FIELD_FROM_EMOJI to fromEmoji,
        NotificationFilterField.FIELD_PACKAGES to packages.map { it.trim() }
            .filter { it.isNotBlank() }.distinct(),
        NotificationFilterField.FIELD_USES_DEFAULT_SMS to usesDefaultSms,
        NotificationFilterField.FIELD_NOISE_PATTERNS to noisePatterns.map { it.trim() }
            .filter { it.isNotBlank() }.distinct(),
    )
}
