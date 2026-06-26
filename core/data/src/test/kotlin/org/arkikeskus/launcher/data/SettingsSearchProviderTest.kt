package org.arkikeskus.launcher.data

import com.google.common.truth.Truth.assertThat
import org.arkikeskus.launcher.data.search.SettingItem
import org.arkikeskus.launcher.data.search.matchSettings
import org.junit.Test

class SettingsSearchProviderTest {
    private val items = listOf(
        SettingItem("wifi", "android.settings.WIFI_SETTINGS", "Wi-Fi", listOf("wifi", "wlan", "langaton")),
        SettingItem("bt", "android.settings.BLUETOOTH_SETTINGS", "Bluetooth", listOf("bt")),
    )

    @Test fun `matches label case-insensitively`() {
        val r = matchSettings(items, "wi-f")
        assertThat(r.map { it.key }).containsExactly("wifi")
    }

    @Test fun `matches an alias`() {
        assertThat(matchSettings(items, "wlan").map { it.key }).containsExactly("wifi")
        assertThat(matchSettings(items, "langaton").map { it.key }).containsExactly("wifi")
    }

    @Test fun `blank query returns nothing`() {
        assertThat(matchSettings(items, "  ")).isEmpty()
    }

    @Test fun `no match returns empty`() {
        assertThat(matchSettings(items, "zzz")).isEmpty()
    }
}
