package com.xeg911.appcontrol.data.remote.fcm

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.xeg911.appcontrol.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the Firebase service account from res/raw/service_account.json and
 * exchanges it for short-lived OAuth2 access tokens for the FCM HTTP v1 API.
 */
@Singleton
class FcmCredentialsProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val serviceAccount: ServiceAccountCredentials by lazy {
        context.resources.openRawResource(R.raw.service_account).use { stream ->
            GoogleCredentials.fromStream(stream) as ServiceAccountCredentials
        }
    }

    private val scopedCredentials: GoogleCredentials by lazy {
        serviceAccount.createScoped(listOf(FCM_SCOPE))
    }

    val projectId: String get() = serviceAccount.projectId!!

    suspend fun accessToken(): String = withContext(Dispatchers.IO) {
        scopedCredentials.refreshIfExpired()
        scopedCredentials.accessToken!!.tokenValue
    }

    private companion object {
        const val FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    }
}
