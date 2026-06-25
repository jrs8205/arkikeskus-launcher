package org.arkikeskus.launcher.data

import coil3.key.Keyer
import coil3.request.Options
import org.arkikeskus.launcher.model.IconRequest

class AppIconKeyer : Keyer<IconRequest> {
    // The themed/dark flags are in the key so each variant caches separately and a settings/theme
    // change re-fetches instead of serving a stale bitmap.
    override fun key(data: IconRequest, options: Options): String =
        "appicon:${data.app.key}:t=${data.themed}:d=${data.dark}"
}
