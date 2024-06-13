package test.mega.privacy.android.app.presentation.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistViewModel
import mega.privacy.android.app.presentation.videosection.VideoToPlaylistViewModel.Companion.ERROR_MESSAGE_REPEATED_TITLE
import mega.privacy.android.app.presentation.videosection.mapper.VideoPlaylistSetUiEntityMapper
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistSetUiEntity
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.videosection.CreateVideoPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistSetsUseCase
import nz.mega.sdk.MegaSet
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoToPlaylistViewModelTest {
    private lateinit var underTest: VideoToPlaylistViewModel

    private val getVideoPlaylistSetsUseCase = mock<GetVideoPlaylistSetsUseCase>()
    private val videoPlaylistSetUiEntityMapper = mock<VideoPlaylistSetUiEntityMapper>()
    private val createVideoPlaylistUseCase = mock<CreateVideoPlaylistUseCase>()
    private val getNextDefaultAlbumNameUseCase = mock<GetNextDefaultAlbumNameUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runBlocking { whenever(getVideoPlaylistSetsUseCase()).thenReturn(emptyList()) }
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideoToPlaylistViewModel(
            getVideoPlaylistSetsUseCase = getVideoPlaylistSetsUseCase,
            videoPlaylistSetUiEntityMapper = videoPlaylistSetUiEntityMapper,
            createVideoPlaylistUseCase = createVideoPlaylistUseCase,
            getNextDefaultAlbumNameUseCase = getNextDefaultAlbumNameUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getVideoPlaylistSetsUseCase,
            videoPlaylistSetUiEntityMapper,
            createVideoPlaylistUseCase,
            getNextDefaultAlbumNameUseCase
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the state is updated correctly when getVideoPlaylistSetsUseCase returns empty`() =
        runTest {
            whenever(getVideoPlaylistSetsUseCase()).thenReturn(emptyList())
            initUnderTest()
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.items).isEmpty()
                assertThat(actual.isLoading).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the state is updated correctly when when getVideoPlaylistSetsUseCase returns not empty`() =
        runTest {
            val expectedUserSets = (1..3L).map {
                createUserSet(id = it, name = "Playlist$it")
            }
            whenever(getVideoPlaylistSetsUseCase()).thenReturn(expectedUserSets)
            whenever(videoPlaylistSetUiEntityMapper(anyOrNull())).thenReturn(mock())

            initUnderTest()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.items).isNotEmpty()
                assertThat(actual.items.size).isEqualTo(3)
                assertThat(actual.isLoading).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun createUserSet(
        id: Long,
        name: String,
    ): UserSet = object : UserSet {
        override val id: Long = id
        override val name: String = name
        override val type: Int = MegaSet.SET_TYPE_PLAYLIST
        override val cover: Long? = null
        override val creationTime: Long = 0
        override val modificationTime: Long = 0
        override val isExported: Boolean = false
        override fun equals(other: Any?): Boolean {
            val otherSet = other as? UserSet ?: return false
            return id == otherSet.id
                    && name == otherSet.name
                    && cover == otherSet.cover
                    && modificationTime == otherSet.modificationTime
                    && isExported == otherSet.isExported
        }
    }

    @Test
    fun `test that the createVideoPlaylistPlaceholderTitle is correctly updated`() = runTest {
        val expectedTitle = "new playlist"
        whenever(
            getNextDefaultAlbumNameUseCase(
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(expectedTitle)

        initUnderTest()

        underTest.setPlaceholderTitle(expectedTitle)

        underTest.uiState.test {
            assertThat(awaitItem().createVideoPlaylistPlaceholderTitle).isEqualTo(expectedTitle)
        }
    }

    @Test
    fun `test that the isInputTitleValid is correctly updated`() = runTest {
        initUnderTest()

        underTest.uiState.test {
            assertThat(awaitItem().isInputTitleValid).isTrue()
            underTest.setNewPlaylistTitleValidity(false)
            assertThat(awaitItem().isInputTitleValid).isFalse()
            underTest.setNewPlaylistTitleValidity(true)
            assertThat(awaitItem().isInputTitleValid).isTrue()
        }
    }

    @Test
    fun `test that the showCreateVideoPlaylist is correctly updated`() = runTest {
        initUnderTest()

        underTest.uiState.test {
            assertThat(awaitItem().shouldCreateVideoPlaylist).isFalse()
            underTest.setShouldCreateVideoPlaylist(true)
            assertThat(awaitItem().shouldCreateVideoPlaylist).isTrue()
            underTest.setShouldCreateVideoPlaylist(false)
            assertThat(awaitItem().shouldCreateVideoPlaylist).isFalse()
        }
    }

    @Test
    fun `test that the correct state is returned when an error occurs in creating the video playlist`() =
        runTest {
            whenever(createVideoPlaylistUseCase(any())).thenAnswer { throw Exception() }

            initUnderTest()

            underTest.createNewPlaylist("video playlist title")
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.isVideoPlaylistCreatedSuccessfully).isFalse()
                assertThat(actual.isLoading).isFalse()
            }
        }

    @Test
    fun `test that create video playlist returns a video playlist with the right title`() =
        runTest {
            val expectedTitle = "video playlist title"
            val expectedVideoPlaylist = mock<VideoPlaylist> {
                on { title }.thenReturn(expectedTitle)
            }
            whenever(createVideoPlaylistUseCase(expectedTitle)).thenReturn(expectedVideoPlaylist)

            initUnderTest()

            underTest.createNewPlaylist(expectedTitle)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.isVideoPlaylistCreatedSuccessfully).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state is updated correctly when new video playlist title is invalid`() =
        runTest {
            val expectedTitle = "/video playlist title"
            val expectedVideoPlaylist = mock<VideoPlaylist> {
                on { title }.thenReturn(expectedTitle)
            }
            whenever(createVideoPlaylistUseCase(expectedTitle)).thenReturn(expectedVideoPlaylist)

            initUnderTest()

            underTest.createNewPlaylist(expectedTitle)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.isInputTitleValid).isFalse()
                assertThat(actual.createDialogErrorMessage).isEqualTo(R.string.invalid_characters_defined)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state is updated correctly when new video playlist title is existing`() =
        runTest {
            val expectedTitle = "video playlist title"
            val expectedUiEntity = mock<VideoPlaylistSetUiEntity> {
                on { title }.thenReturn(expectedTitle)
            }

            whenever(getVideoPlaylistSetsUseCase()).thenReturn(listOf(mock()))
            whenever(videoPlaylistSetUiEntityMapper(anyOrNull())).thenReturn(expectedUiEntity)

            initUnderTest()
            underTest.createNewPlaylist(expectedTitle)
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.isInputTitleValid).isFalse()
                assertThat(actual.createDialogErrorMessage).isEqualTo(ERROR_MESSAGE_REPEATED_TITLE)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that createVideoPlaylistUseCase is not invoked when createNewPlaylist is invoked and createVideoPlaylistJob is active`() =
        runTest {
            val expectedTitle = "video playlist title"
            val expectedVideoPlaylist = mock<VideoPlaylist> {
                on { title }.thenReturn(expectedTitle)
            }
            val mockJob = mock<Job>()
            whenever(mockJob.isActive).thenReturn(true)
            whenever(createVideoPlaylistUseCase(expectedTitle)).thenReturn(expectedVideoPlaylist)

            initUnderTest()

            val field = VideoToPlaylistViewModel::class.java.declaredFields
                .first { it.name == "createVideoPlaylistJob" }
            field.isAccessible = true
            field.set(underTest, mockJob)

            underTest.createNewPlaylist(expectedTitle)
            verifyNoInteractions(createVideoPlaylistUseCase(expectedTitle))
        }
}
