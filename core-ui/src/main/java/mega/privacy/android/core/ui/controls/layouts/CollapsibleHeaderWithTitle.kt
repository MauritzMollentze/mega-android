package mega.privacy.android.core.ui.controls.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.core.ui.controls.appbar.AppBarForCollapsibleHeader
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBarTitle
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme


/**
 * CollapsibleHeaderWithTitle
 *
 * Layout to add a header with collapsible title in [ScaffoldWithCollapsibleHeader] together with [AppBarForCollapsibleHeader]. Check ScaffoldWithCollapsibleHeader preview for an example
 * @param appBarType this will be used to set the correct paddings of the title, should be the same as the [AppBarForCollapsibleHeader]
 * @param title for the toolbar, it should be the same as the [AppBarForCollapsibleHeader]. Here, as the header has more space, it will take up to [titleMaxLines] lines.
 * @param titleMaxLines the maximum title lines when expanded. Default to 3
 * @param content any other content that will be show in the header.
 */
@Composable
fun CollapsibleHeaderWithTitle(
    appBarType: AppBarType,
    title: String,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 3,
    content: @Composable ConstraintLayoutScope.() -> Unit,
) = ConstraintLayout(
    modifier = modifier
        .fillMaxWidth()
) {
    val (titlePlaceholder, titleVisible) = createRefs()

    content()
    val statusBarHeight = WindowInsets.Companion.statusBars.getTop(LocalDensity.current).dp
    val appBarBottomGuideline = createGuidelineFromTop(statusBarHeight + APP_BAR_HEIGHT.dp)
    val titleDisplacement = LocalCollapsibleHeaderTitleTransition.current.offset
    //this is just a none visible text with 1 line to place the next text with first line in the center of the virtual toolbar (appBarBottomGuideline)
    MegaAppBarTitle(
        title = title,
        modifier = Modifier
            .alpha(0f)
            .constrainAs(titlePlaceholder) {
                start.linkTo(parent.start, if (appBarType == AppBarType.NONE) 16.dp else 72.dp)
                end.linkTo(parent.end, 8.dp)
                top.linkTo(parent.top, statusBarHeight)
                bottom.linkTo(appBarBottomGuideline)
                baseline
                height = Dimension.wrapContent
                width = Dimension.fillToConstraints
            }
    )
    MegaAppBarTitle(
        modifier = Modifier
            .fillMaxWidth()
            .constrainAs(titleVisible) {
                start.linkTo(titlePlaceholder.start)
                end.linkTo(titlePlaceholder.end)
                top.linkTo(titlePlaceholder.top, titleDisplacement)
                height = Dimension.wrapContent
                width = Dimension.fillToConstraints
            },
        title = title,
        maxLines = titleMaxLines,
    )
}

@CombinedThemePreviews
@Composable
private fun CollapsibleHeaderPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CollapsibleHeaderWithTitle(AppBarType.MENU, "Title") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MegaTheme.colors.background.inverse),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "preview", color = MegaTheme.colors.text.accent)
            }
        }
    }
}