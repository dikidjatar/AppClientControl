package com.xeg911.appclient.data.repository

import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.data.remote.firebase.DeviceFirebaseDataSource
import com.xeg911.appclient.domain.repository.RuleRepository
import com.xeg911.shared.rules.AutomationRule
import com.xeg911.shared.rules.RuleState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val deviceIdentifier: DeviceIdentifier,
    private val dataSource: DeviceFirebaseDataSource,
) : RuleRepository {

    override fun observeDeviceRules(): Flow<List<AutomationRule>> = dataSource.observeDeviceRules()

    override suspend fun publishState(state: RuleState): Result<Unit> = runCatching {
        dataSource.writeRuleState(deviceIdentifier.resolveDeviceId(), state)
    }

    override suspend fun loadStates(): Result<Map<String, RuleState>> = runCatching {
        dataSource.readRuleStates(deviceIdentifier.resolveDeviceId())
    }
}
