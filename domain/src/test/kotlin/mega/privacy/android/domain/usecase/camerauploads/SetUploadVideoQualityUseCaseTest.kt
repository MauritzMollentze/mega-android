package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetUploadVideoQualityUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetUploadVideoQualityUseCaseTest {

    private lateinit var underTest: SetUploadVideoQualityUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetUploadVideoQualityUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @ParameterizedTest(name = "test that {0} is set")
    @EnumSource(VideoQuality::class)
    fun `test that a new video quality is set when uploading videos`(videoQuality: VideoQuality) =
        runTest {
            underTest(videoQuality)

            verify(cameraUploadsRepository).setUploadVideoQuality(videoQuality)
        }
}