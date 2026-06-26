package org.arkikeskus.launcher.ui

import androidx.annotation.DrawableRes

/**
 * Shared vector-drawable icons (Material paths) used across the launcher's popups and rows, exposed
 * as resource ids so feature modules reference them without reaching into another module's R class.
 */
object LauncherIcons {
    @DrawableRes val Info: Int = R.drawable.ic_info
    @DrawableRes val Edit: Int = R.drawable.ic_edit
    @DrawableRes val Close: Int = R.drawable.ic_close
    @DrawableRes val Add: Int = R.drawable.ic_add
    @DrawableRes val Delete: Int = R.drawable.ic_delete
    @DrawableRes val VisibilityOff: Int = R.drawable.ic_visibility_off
    @DrawableRes val Call: Int = R.drawable.ic_call
    @DrawableRes val Message: Int = R.drawable.ic_message
    @DrawableRes val ChevronRight: Int = R.drawable.ic_chevron_right
    @DrawableRes val Remove: Int = R.drawable.ic_remove
}
