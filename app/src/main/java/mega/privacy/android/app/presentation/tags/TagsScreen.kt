package mega.privacy.android.app.presentation.tags

import mega.privacy.android.core.R as CoreR
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericDescriptionTextField
import mega.privacy.android.shared.original.core.ui.controls.textfields.transformations.PrefixTransformation
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.mobile.analytics.event.NodeInfoTagsAddedEvent
import mega.privacy.mobile.analytics.event.NodeInfoTagsRemovedEvent

/**
 * Tags screen composable.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TagsScreen(
    addNodeTag: (String) -> Unit,
    consumeInfoMessage: () -> Unit,
    validateTagName: (String) -> Unit,
    addOrRemoveTag: (String) -> Unit,
    onBackPressed: () -> Unit,
    consumeMaxTagsError: () -> Unit,
    uiState: TagsUiState,
) {
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current
    MegaScaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .semantics {
                testTagsAsResourceId = true
            },
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(tag = TAGS_SCREEN_APP_BAR),
                appBarType = AppBarType.BACK_NAVIGATION,
                title = stringResource(id = sharedR.string.add_tags_page_title_label),
                elevation = 0.dp,
                onNavigationPressed = { onBackPressed() },
            )
        },
    ) { paddingValues ->
        EventEffect(event = uiState.informationMessage, onConsumed = consumeInfoMessage) { info ->
            scaffoldState.snackbarHostState.showSnackbar(info.getInfo(context))
        }
        EventEffect(event = uiState.showMaxTagsError, onConsumed = { consumeMaxTagsError() }) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = context.getString(
                    sharedR.string.add_tags_error_max_tags,
                    TagsActivity.MAX_TAGS_PER_NODE
                )
            )
        }
        TagsContent(
            modifier = Modifier
                .padding(paddingValues)
                .testTag(TAGS_SCREEN_CONTENTS_LABEL),
            addNodeTag = addNodeTag,
            validateTagName = validateTagName,
            addOrRemoveTag = addOrRemoveTag,
            uiState = uiState,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsContent(
    addNodeTag: (String) -> Unit,
    validateTagName: (String) -> Unit,
    addOrRemoveTag: (String) -> Unit,
    uiState: TagsUiState,
    modifier: Modifier = Modifier,
) {

    fun addTag() {
        if (uiState.searchText.isNotBlank() && uiState.isError.not()) {
            addNodeTag(uiState.searchText)
            Analytics.tracker.trackEvent(NodeInfoTagsAddedEvent)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        MegaText(
            modifier = Modifier.testTag(TAGS_SCREEN_ADD_TAGS_LABEL),
            text = stringResource(id = sharedR.string.add_tags_text_label_tag),
            textColor = if (uiState.isError) TextColor.Error else TextColor.Accent,
            style = MaterialTheme.typography.caption,
        )
        // Tags content
        GenericDescriptionTextField(
            modifier = Modifier
                .padding(bottom = 4.dp),
            visualTransformation = PrefixTransformation("#"),
            value = uiState.searchText,
            imeAction = ImeAction.Done,
            supportingText = uiState.message,
            keyboardActions = KeyboardActions(
                onDone = { addTag() }
            ),
            showError = uiState.isError,
            onValueChange = {
                validateTagName(it)
            },
            showUnderline = true,
        )

        if (
            uiState.searchText.isNotBlank() &&
            uiState.isError.not() &&
            uiState.tags.contains(uiState.searchText).not()
        ) {
            TextMegaButton(
                modifier = Modifier.testTag(TAGS_SCREEN_ADD_TAGS_BUTTON),
                contentPadding = PaddingValues(vertical = 8.dp),
                text = stringResource(
                    id = sharedR.string.add_tags_button_label_add,
                    uiState.searchText
                ),
                onClick = ::addTag,
                textAlign = TextAlign.Start
            )
        }

        if (uiState.tags.isNotEmpty()) {
            MegaText(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .testTag(TAGS_SCREEN_EXISTING_TAGS_LABEL),
                text = stringResource(id = sharedR.string.add_tags_label_existing_tags),
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2,
            )
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.tags.forEach { tag ->
                val isSelected = uiState.nodeTags.contains(tag)
                MegaChip(
                    modifier = Modifier.testTag(TAGS_SCREEN_TAG_CHIP),
                    selected = isSelected,
                    text = "#$tag",
                    enabled = true,
                    onClick = {
                        addOrRemoveTag(tag)
                        Analytics.tracker.trackEvent(NodeInfoTagsRemovedEvent)
                    },
                    leadingIcon = if (isSelected) CoreR.drawable.ic_filter_selected else null,
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun TagsScreenPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TagsScreen(
            addNodeTag = {},
            consumeInfoMessage = {},
            validateTagName = { it.isNotEmpty() },
            addOrRemoveTag = {},
            onBackPressed = {},
            consumeMaxTagsError = {},
            uiState = TagsUiState(
                tags = persistentListOf("tag1", "tag2"),
                nodeTags = persistentListOf("tag1")
            ),
        )
    }
}

internal const val TAGS_SCREEN_APP_BAR = "tags_screen:tags_app_bar"
internal const val TAGS_SCREEN_CONTENTS_LABEL = "tags_screen:contents_label_tag"
internal const val TAGS_SCREEN_ADD_TAGS_LABEL = "tags_screen:add_tags_text_label_tag"
internal const val TAGS_SCREEN_ADD_TAGS_BUTTON = "tags_screen:add_tags_button"
internal const val TAGS_SCREEN_EXISTING_TAGS_LABEL = "tags_screen:existing_tags_label"
internal const val TAGS_SCREEN_TAG_CHIP = "tags_screen:tag_chip"
