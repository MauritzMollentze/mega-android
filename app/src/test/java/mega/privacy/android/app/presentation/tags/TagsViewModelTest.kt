package mega.privacy.android.app.presentation.tags

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.node.ManageNodeTagUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TagsViewModelTest {

    private val manageNodeTagUseCase = mock<ManageNodeTagUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val monitorNodeUpdatesById = mock<MonitorNodeUpdatesById>()
    private lateinit var stateHandle: SavedStateHandle
    private lateinit var underTest: TagsViewModel

    @BeforeEach
    fun resetMock() {
        stateHandle = SavedStateHandle(mapOf(TagsActivity.NODE_ID to 123L))
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        underTest = TagsViewModel(
            manageNodeTagUseCase = manageNodeTagUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            monitorNodeUpdatesById = monitorNodeUpdatesById,
            stateHandle = stateHandle
        )
    }

    @AfterEach
    fun clear() {
        reset(manageNodeTagUseCase, getNodeByIdUseCase, monitorNodeUpdatesById)
    }

    @Test
    fun `test that getNodeByHandle update uiState with nodeHandle and tags`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        val node = mock<TypedNode> {
            on { id } doReturn NodeId(123L)
            on { name } doReturn "tags"
            on { tags } doReturn listOf("tag1", "tag2")
        }
        whenever(getNodeByIdUseCase(NodeId(123L))).thenReturn(node)
        val nodeId = NodeId(123L)
        underTest.getNodeByHandle(nodeId)
        val uiState = underTest.uiState.value
        assertThat(uiState.tags).containsExactly("tag1", "tag2")
    }

    @Test
    fun `test that getNodeByHandle log error when getNodeByIdUseCase fails`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        whenever(getNodeByIdUseCase(NodeId(123L))).thenThrow(RuntimeException())
        val nodeHandle = NodeId(123L)
        underTest.getNodeByHandle(nodeHandle)
    }

    @Test
    fun `test that addNodeTag update node tags`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        whenever(manageNodeTagUseCase(NodeId(123L), newTag = "new tag")).thenReturn(Unit)
        underTest.addNodeTag("new tag")
        verify(manageNodeTagUseCase).invoke(NodeId(123L), newTag = "new tag")
    }

    @ParameterizedTest(name = "validateTagName should return {0} when tag is {1} and message is {2}")
    @MethodSource("validateTagNameProvider")
    fun `test that validateTagName should return true when tag is valid`(
        expected: Boolean,
        tag: String,
        message: String?,
    ) {
        val actual = underTest.validateTagName(tag)
        assertThat(actual).isEqualTo(expected)
        assertThat(underTest.uiState.value.message).isEqualTo(message)
    }

    private fun validateTagNameProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            false,
            "",
            "Use tags to help you find and organise your data. Try tagging by year, location, project, or subject."
        ),
        Arguments.of(
            false,
            " ",
            "Use tags to help you find and organise your data. Try tagging by year, location, project, or subject."
        ),
        Arguments.of(false, "tag with space", "Tags can only contain letters and numbers."),
        Arguments.of(
            false,
            "tag with special characters!@#",
            "Tags can only contain letters and numbers."
        ),
        Arguments.of(true, "tag", null),
        Arguments.of(true, "tag123", null),
        Arguments.of(
            false,
            "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz",
            "Tags can be up to 32 characters long."
        )
    )

    @Test
    fun `test monitorNodeUpdatesById updates node`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(
            flowOf(listOf(NodeChanges.Tags))
        )
        underTest.getNodeByHandle(NodeId(123L))
        verify(getNodeByIdUseCase, times(2)).invoke(NodeId(123L))
    }

    @Test
    fun `test that removeTag update node tags`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        whenever(manageNodeTagUseCase(NodeId(123L), oldTag = "old tag", newTag = null)).thenReturn(
            Unit
        )
        underTest.removeTag("old tag")
        verify(manageNodeTagUseCase).invoke(NodeId(123L), oldTag = "old tag", newTag = null)
    }
}