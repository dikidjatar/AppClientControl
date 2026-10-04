package com.xeg911.appclient.fcm.autoexec

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoExecuteCoordinator @Inject constructor(
    handlers: Set<@JvmSuppressWildcards AutoExecuteHandler>,
) {
    private val handlers = handlers.sortedBy { it.order }

    suspend fun executeAll(context: AutoExecuteContext) {
        handlers.forEach { handler ->
            val applies = runCatching { handler.shouldExecute(context) }
                .onFailure { Log.w(TAG, "${handler.id}: shouldExecute failed", it) }
                .getOrDefault(false)
            if (!applies) return@forEach
            runCatching { handler.execute(context) }
                .onSuccess {
                    Log.d(
                        TAG,
                        "${handler.id}: executed for ${context.payload.notificationId}"
                    )
                }
                .onFailure { Log.e(TAG, "${handler.id}: execute failed", it) }
        }
    }

    private companion object {
        const val TAG = "AutoExecuteCoordinator"
    }
}
