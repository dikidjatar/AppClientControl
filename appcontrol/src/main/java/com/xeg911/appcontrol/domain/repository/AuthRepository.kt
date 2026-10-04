package com.xeg911.appcontrol.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    fun signOut()
    fun isSignedIn(): Boolean

    fun observeSignedIn(): Flow<Boolean>
}