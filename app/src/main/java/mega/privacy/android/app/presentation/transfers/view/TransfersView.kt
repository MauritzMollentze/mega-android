package mega.privacy.android.app.presentation.transfers.view

import android.content.res.Configuration
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.model.TransfersUiState
import mega.privacy.android.app.presentation.transfers.model.TransfersViewModel
import mega.privacy.android.app.presentation.transfers.view.inprogress.InProgressTransfersView
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.tab.Tabs
import mega.privacy.android.shared.original.core.ui.controls.tab.TextCell
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun TransfersView(
    tabIndexToSelect: Int = IN_PROGRESS_TAB_INDEX,
    viewModel: TransfersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TransfersView(
        selectedTabIndex = tabIndexToSelect,
        uiState = uiState,
        onPlayPauseTransfer = viewModel::playOrPauseTransfer
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TransfersView(
    selectedTabIndex: Int,
    uiState: TransfersUiState,
    onPlayPauseTransfer: (Int) -> Unit,
) = with(uiState) {
    val scaffoldState = rememberScaffoldState()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    MegaScaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .semantics { testTagsAsResourceId = true },
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = stringResource(id = R.string.section_transfers),
                onNavigationPressed = { onBackPressedDispatcher?.onBackPressed() },
                elevation = 0.dp,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            Tabs(
                cells = persistentListOf(
                    TextCell(
                        text = stringResource(id = R.string.title_tab_in_progress_transfers),
                        tag = TEST_TAG_IN_PROGRESS_TAB,
                    ) {
                        InProgressTransfersView(
                            inProgressTransfers = inProgressTransfers,
                            isOverQuota = isOverQuota,
                            areTransfersPaused = areTransfersPaused,
                            onPlayPauseClicked = onPlayPauseTransfer,
                        )
                    },
                    TextCell(
                        text = stringResource(id = R.string.title_tab_completed_transfers),
                        tag = TEST_TAG_COMPLETED_TAB,
                    ) { CompletedTransfersView() }
                ),
                selectedIndex = selectedTabIndex,
            )
        }
    }
}

@Composable
internal fun CompletedTransfersView() {

}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkTransfersViewPreview")
@Composable
private fun TransfersViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TransfersView()
    }
}

/**
 * Tag for the in progress tab
 */
const val TEST_TAG_IN_PROGRESS_TAB = "transfers_view:tab_in_progress"

/**
 * Index for the in progress tab
 */
const val IN_PROGRESS_TAB_INDEX = 0

/**
 * Tag for the completed tab
 */
const val TEST_TAG_COMPLETED_TAB = "transfers_view:tab_completed"

/**
 * Index for the completed tab
 */
const val COMPLETED_TAB_INDEX = 1