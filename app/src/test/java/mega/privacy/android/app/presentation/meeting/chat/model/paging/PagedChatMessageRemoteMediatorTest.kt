package mega.privacy.android.app.presentation.meeting.chat.model.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.paging.FetchMessagePageResponse
import mega.privacy.android.domain.usecase.chat.message.paging.ClearChatMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.FetchMessagePageUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.SaveChatMessagesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalPagingApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PagedChatMessageRemoteMediatorTest {
    private lateinit var underTest: PagedChatMessageRemoteMediator

    private val chatId = 123L
    private val messageFlow: MutableStateFlow<ChatMessage?> = MutableStateFlow(null)
    private val fetchMessages = mock<FetchMessagePageUseCase>()
    private val saveChatMessagesUseCase = mock<SaveChatMessagesUseCase>()
    private val clearChatMessagesUseCase = mock<ClearChatMessagesUseCase>()

    val pageSize = 10
    private val state = PagingState<Int, TypedMessage>(
        emptyList(),
        null,
        PagingConfig(10),
        10
    )

    @BeforeAll
    internal fun setUp() {
        underTest =
            PagedChatMessageRemoteMediator(
                chatId = chatId,
                fetchMessages = fetchMessages,
                saveMessages = saveChatMessagesUseCase,
                coroutineScope = mock(),
                clearChatMessagesUseCase = clearChatMessagesUseCase,
            )
    }

    @AfterEach
    internal fun tearDown() {
        messageFlow.value = null
        Mockito.reset(
            fetchMessages,
            saveChatMessagesUseCase,
            clearChatMessagesUseCase
        )
    }

    @Test
    internal fun `test that end of pagination is true if chat has no more messages`() =
        runTest {

            fetchMessages.stub {
                onBlocking {
                    invoke(
                        any(),
                        any()
                    )
                }.thenReturn(
                    FetchMessagePageResponse(
                        chatId = 1L,
                        messages = emptyList(),
                        loadResponse = ChatHistoryLoadStatus.NONE
                    )
                )
            }
            val result = underTest.load(LoadType.PREPEND, state)

            assertThat(result).isInstanceOf(RemoteMediator.MediatorResult.Success::class.java)
            assertThat((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
                .isTrue()
        }

    @Test
    internal fun `test fetched response is saved`() = runTest {
        val messages = mutableListOf<ChatMessage>().apply {
            repeat(pageSize * 3) {
                add(mock())
            }
        }
        val response = mock<FetchMessagePageResponse> {
            on { this.messages }.thenReturn(messages)
            on { this.chatId }.thenReturn(chatId)
        }
        fetchMessages.stub {
            onBlocking {
                invoke(
                    any(),
                    any()
                )
            }.thenReturn(response)
        }

        underTest.load(LoadType.APPEND, state)

        verify(saveChatMessagesUseCase).invoke(response.chatId, response.messages)
    }

    @Test
    internal fun `test that error response is returned if an error is thrown`() = runTest {
        val exception = Exception("This is the issue")
        fetchMessages.stub {
            onBlocking {
                invoke(
                    any(),
                    any()
                )
            }.thenAnswer { throw exception }
        }

        val result = underTest.load(LoadType.APPEND, state)

        assertThat(result).isInstanceOf(RemoteMediator.MediatorResult.Error::class.java)
        assertThat((result as RemoteMediator.MediatorResult.Error).throwable)
            .isEqualTo(exception)
    }

    @Test
    internal fun `test that message database is cleared when load type is refresh`() = runTest {
        underTest.load(LoadType.REFRESH, state)

        verify(clearChatMessagesUseCase).invoke(chatId, false)
    }

    @Test
    internal fun `test that end of pagination reached is only sent when load result is none`() =
        runTest {
            val response = mock<FetchMessagePageResponse> {
                on { loadResponse }.thenReturn(
                    ChatHistoryLoadStatus.REMOTE,
                    ChatHistoryLoadStatus.NONE
                )

            }
            fetchMessages.stub {
                onBlocking {
                    invoke(
                        any(),
                        any()
                    )
                }.thenReturn(response)
            }

            val firstResult = underTest.load(LoadType.REFRESH, state)
            assertThat((firstResult as? RemoteMediator.MediatorResult.Success)?.endOfPaginationReached).isTrue()
        }

    @Test
    internal fun `test that timeout cancel exception returns when fetch message throw exception`() =
        runTest {
            val exception = mock<TimeoutCancellationException>()
            whenever(fetchMessages(any(), any())).thenAnswer { throw exception }

            val result = underTest.load(LoadType.APPEND, state)

            assertThat(result).isInstanceOf(RemoteMediator.MediatorResult.Error::class.java)
            assertThat((result as RemoteMediator.MediatorResult.Error).throwable)
                .isInstanceOf(TimeoutCancellationException::class.java)
        }
}