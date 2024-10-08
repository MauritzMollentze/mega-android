package mega.privacy.android.feature.devicecenter.domain.usecase

import mega.privacy.android.domain.usecase.backup.GetBackupInfoUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdAndNameMapUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceNode
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import javax.inject.Inject

/**
 * Use Case that retrieves all of the User's Backup Devices
 *
 * @property deviceCenterRepository [DeviceCenterRepository]
 * @property getBackupInfoUseCase [GetBackupInfoUseCase]
 * @property getDeviceIdAndNameMapUseCase [GetDeviceIdAndNameMapUseCase]
 * @property getDeviceIdUseCase [GetDeviceIdUseCase]
 */
class GetDevicesUseCase @Inject constructor(
    private val deviceCenterRepository: DeviceCenterRepository,
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val getDeviceIdAndNameMapUseCase: GetDeviceIdAndNameMapUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
) {
    /**
     * Invocation function
     *
     * @return The User's Backup Devices
     */
    suspend operator fun invoke(): List<DeviceNode> = deviceCenterRepository.getDevices(
        currentDeviceId = getDeviceIdUseCase().orEmpty(),
        backupInfoList = getBackupInfoUseCase(),
        deviceIdAndNameMap = getDeviceIdAndNameMapUseCase(),
    )
}