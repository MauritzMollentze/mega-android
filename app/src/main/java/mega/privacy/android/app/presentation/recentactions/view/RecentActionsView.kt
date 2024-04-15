package mega.privacy.android.app.presentation.recentactions.view

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsUiState
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * Composable for the recent actions screen
 *
 * @param uiState               [RecentActionsUiState]
 * @param onItemClick           Callback when an item is clicked
 * @param onMenuClick           Callback when the menu button is clicked
 * @param onShowActivityActionClick Callback when the show activity action is clicked
 * @param onScrollStateChanged  Callback when the scroll state changes
 */
@Composable
fun RecentActionsView(
    uiState: RecentActionsUiState,
    onItemClick: (RecentActionBucket) -> Unit,
    onMenuClick: (TypedFileNode) -> Unit,
    onShowActivityActionClick: () -> Unit,
    onScrollStateChanged: (isScrolling: Boolean) -> Unit,
) {
    if (uiState.isLoading) {
        RecentLoadingView()
    } else if (uiState.hideRecentActivity) {
        RecentActionsHiddenView(
            onShowActivityActionClick = onShowActivityActionClick
        )
    } else {
        if (uiState.groupedRecentActionItems.isEmpty()) {
            RecentActionsEmptyView()
        } else {
            RecentActionsListView(
                groupedRecentActions = uiState.groupedRecentActionItems,
                onItemClick = onItemClick,
                onMenuClick = onMenuClick,
                onScrollStateChanged = onScrollStateChanged
            )
        }
    }
}