package com.xeg911.appclient.permission

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.shared.data.model.PermissionSnapshot
import com.xeg911.shared.data.model.PermissionStatus
import com.xeg911.shared.data.model.PermissionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionChange(
    val name: String,
    val simpleName: String,
    val type: PermissionType,
    val granted: Boolean,
)

/**
 * Observes the real permission state of the app.
 */
@Singleton
class PermissionMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val checker: PermissionChecker,
    @param:ApplicationScope private val scope: CoroutineScope,
) : DefaultLifecycleObserver {

    private val started = AtomicBoolean(false)
    private val refreshMutex = Mutex()
    private var foregroundPollJob: Job? = null

    private val _state = MutableStateFlow(checker.snapshot())
    val state: StateFlow<PermissionSnapshot> = _state.asStateFlow()

    private val _changes = MutableSharedFlow<List<PermissionChange>>(extraBufferCapacity = 16)
    val changes: SharedFlow<List<PermissionChange>> = _changes.asSharedFlow()

    private val listenerObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@PermissionMonitor)
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(ENABLED_NOTIFICATION_LISTENERS), false, listenerObserver,
            )
        }.onFailure { Log.w(TAG, "Cannot observe notification listener setting", it) }
        refresh()
    }

    fun isGranted(permission: AppPermission): Boolean = checker.isGranted(permission)

    fun refresh() {
        scope.launch { refreshNow() }
    }

    suspend fun refreshNow(): PermissionSnapshot = refreshMutex.withLock {
        val previous = _state.value
        val current = withContext(Dispatchers.Default) { checker.snapshot() }
        val diff = diff(previous.items, current.items)
        if (diff.isNotEmpty() || previous.totalCount != current.totalCount) {
            _state.value = current
            if (diff.isNotEmpty()) {
                Log.i(
                    TAG,
                    "Permission change: ${diff.joinToString { "${it.simpleName}=${it.granted}" }}"
                )
                _changes.emit(diff)
            }
        }
        current
    }

    override fun onStart(owner: LifecycleOwner) {
        refresh()
        foregroundPollJob?.cancel()
        foregroundPollJob = scope.launch {
            while (isActive) {
                delay(FOREGROUND_POLL_MS)
                refreshNow()
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) = refresh()

    override fun onStop(owner: LifecycleOwner) {
        foregroundPollJob?.cancel()
        foregroundPollJob = null
    }

    private fun diff(
        old: Map<String, PermissionStatus>,
        new: Map<String, PermissionStatus>,
    ): List<PermissionChange> = new.values
        .filter { item -> old[item.simpleName]?.granted != item.granted }
        .map { PermissionChange(it.name, it.simpleName, it.type, it.granted) }

    private companion object {
        const val TAG = "PermissionMonitor"
        const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
        const val FOREGROUND_POLL_MS = 5_000L
    }
}
