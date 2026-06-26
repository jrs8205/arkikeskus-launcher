package org.arkikeskus.launcher.data

import com.google.common.truth.Truth.assertThat
import org.arkikeskus.launcher.model.SearchResult
import org.junit.Test

class SearchResultTest {
    @Test
    fun `result ids are stable and namespaced`() {
        assertThat(SearchResult.Setting("Wi-Fi", "android.settings.WIFI_SETTINGS", "wifi").id)
            .isEqualTo("setting:wifi")
        assertThat(SearchResult.Calculation("1+1", "2").id).isEqualTo("calc")
        assertThat(
            SearchResult.Contact("Liisa", "content://x/1", "123", null, 1L).id,
        ).isEqualTo("contact:1")
    }
}
