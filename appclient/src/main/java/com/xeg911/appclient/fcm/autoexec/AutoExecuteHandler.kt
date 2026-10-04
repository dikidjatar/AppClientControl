package com.xeg911.appclient.fcm.autoexec

/**
 * One "maybe auto execute" rule evaluated for every incoming FCM message.
 *
 * To add a new rule: implement this interface (or extend [ActionAutoExecuteHandler])
 * and bind it with `@Binds @IntoSet` in [com.xeg911.appclient.di.AutoExecuteModule].
 */
interface AutoExecuteHandler {
    val id: String

    val order: Int get() = DEFAULT_ORDER

    fun shouldExecute(context: AutoExecuteContext): Boolean

    suspend fun execute(context: AutoExecuteContext)

    companion object {
        const val FIRST_ORDER = 0
        const val DEFAULT_ORDER = 100
    }
}
