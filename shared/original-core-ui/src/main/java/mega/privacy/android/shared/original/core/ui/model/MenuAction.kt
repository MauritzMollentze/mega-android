package mega.privacy.android.shared.original.core.ui.model

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.menus.MenuActions


/**
 * Common interface to define actions for [MenuActions]
 * In general menu actions have an icon to be shown in the toolbar, so it's preferable
 * to use [MenuActionWithIcon] or any of it's inherited abstract classes.
 */
interface MenuAction {
    /**
     * Description for this action to be shown in long clicks or in drop down menu
     */
    @Composable
    fun getDescription(): String

    /**
     * similar to xml definition of menus, this sets the order of menu actions, lower will be showed first.
     */
    val orderInCategory: Int
        get() = 100

    /**
     * True if the action should be enabled, false otherwise.
     */
    val enabled: Boolean
        get() = true

    /**
     * Tag for testing purposes.
     */
    val testTag: String
}

/**
 * Common interface for all actions with an icon
 */
interface MenuActionWithIcon : MenuAction {
    /**
     * Painter for the icon of this action when it's shown in the toolbar
     */
    @Composable
    fun getIconPainter(): Painter

    /**
     * If icon should be highlighted
     */
    val highlightIcon: Boolean
        get() = false
}

/**
 * Utility abstract class for actions without icon
 * @param descriptionRes the string resource to be used to generate the description of this action
 * @param testTag tag for testing purposes.
 */
abstract class MenuActionWithoutIcon(
    @StringRes val descriptionRes: Int,
    override val testTag: String,
) : MenuAction {
    @Composable
    override fun getDescription() = stringResource(id = descriptionRes)
}

/**
 * Utility abstract class for actions with description and icon from resources
 * @param iconRes the drawable resource to be used to generate the icon painter
 * @param descriptionRes the string resource to be used to generate the description of this action
 * @param testTag tag for testing purposes.
 */
abstract class MenuActionString(
    @DrawableRes val iconRes: Int,
    @StringRes val descriptionRes: Int,
    override val testTag: String,
) : MenuActionWithIcon {
    @Composable
    override fun getDescription() = stringResource(id = descriptionRes)

    @Composable
    override fun getIconPainter() = painterResource(id = iconRes)
}

/**
 * Utility abstract class for actions with description and icon from resources using plurals
 * @param iconRes the drawable resource to be used to generate the icon painter
 * @param descriptionRes the plural resource to be used to generate the description of this action
 * @param amount the amount to be used in [descriptionRes] plural
 * @param testTag tag for testing purposes.
 */
abstract class MenuActionPlural(
    @DrawableRes val iconRes: Int,
    @PluralsRes val descriptionRes: Int,
    val amount: Int,
    override val testTag: String,
) : MenuActionWithIcon {
    @Composable
    override fun getDescription() = pluralStringResource(id = descriptionRes, amount, amount)

    @Composable
    override fun getIconPainter() = painterResource(id = iconRes)
}

/**
 * Utility abstract class for drop down actions
 */
abstract class MenuActionDropDown(
    override val testTag: String,
) : MenuActionWithIcon {

    @Composable
    override fun getDescription() =
        stringResource(id = R.string.label_more)

    @Composable
    override fun getIconPainter() =
        painterResource(id = iconPackR.drawable.ic_more_vertical_medium_regular_outline)
}
