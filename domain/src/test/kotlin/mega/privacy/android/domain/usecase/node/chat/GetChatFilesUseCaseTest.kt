package mega.privacy.android.domain.usecase.node.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatFilesUseCaseTest {
    private lateinit var underTest: GetChatFilesUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val addChatFileTypeUseCase = mock<AddChatFileTypeUseCase>()

    private val chatId = 11L
    private val messageId = 11L

    @BeforeAll
    fun setUp() {
        underTest = GetChatFilesUseCase(nodeRepository, addChatFileTypeUseCase)
    }

    @BeforeEach
    fun resetMocks() = reset(
        nodeRepository,
        addChatFileTypeUseCase,
    )

    @Test
    fun `test that empty list is returned when there is no file node`() = runTest {
        whenever(nodeRepository.getNodesFromChatMessage(chatId, messageId))
            .thenReturn(emptyList())
        assertThat(underTest(chatId, messageId)).isEmpty()
    }

    @Test
    fun `test that typed node returned by addChatFileTypeUseCase is returned`() = runTest {
        val fileNode = mock<FileNode>()
        val expected = listOf(mock<ChatDefaultFile>())
        whenever(nodeRepository.getNodesFromChatMessage(chatId, messageId))
            .thenReturn(listOf(fileNode))
        whenever(addChatFileTypeUseCase(fileNode, chatId, messageId))
            .thenReturn(expected.first())
        val actual = underTest(chatId, messageId)
        assertThat(actual).isEqualTo(expected)
    }
}