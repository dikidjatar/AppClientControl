package com.xeg911.appclient.core.device

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdentifier @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    private val mutex = Mutex()

    fun cachedDeviceId(): String? = firebaseAuth.currentUser?.uid

    suspend fun resolveDeviceId(): String = mutex.withLock {
        firebaseAuth.currentUser?.uid
            ?: firebaseAuth.signInAnonymously().await().user?.uid
            ?: throw IllegalStateException("Anonymous sign-in returned a null user")
    }
}
