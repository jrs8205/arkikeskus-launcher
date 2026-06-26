package org.arkikeskus.launcher.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.arkikeskus.launcher.data.search.SearchAggregator
import org.arkikeskus.launcher.data.search.SearchProvider
import org.arkikeskus.launcher.model.SearchResult
import org.arkikeskus.launcher.model.SearchResults
import org.junit.Test

class SearchAggregatorTest {

    private class FakeProvider(
        private val enabled: Boolean,
        private val results: List<SearchResult> = emptyList(),
        private val throws: Boolean = false,
    ) : SearchProvider {
        override suspend fun isEnabled() = enabled
        override suspend fun query(query: String): List<SearchResult> {
            if (throws) error("boom")
            return results
        }
    }

    @Test fun `blank query returns empty results without running providers`() = runTest {
        val agg = SearchAggregator(setOf(FakeProvider(true, throws = true)))
        assertThat(agg.search("   ")).isEqualTo(SearchResults())
    }

    @Test fun `groups results by type`() = runTest {
        val agg = SearchAggregator(
            setOf(
                FakeProvider(true, listOf(SearchResult.Calculation("1+1", "2"))),
                FakeProvider(true, listOf(SearchResult.Setting("Wi-Fi", "a", "wifi"))),
                FakeProvider(true, listOf(SearchResult.Contact("Liisa", "u", "1", null, 1L))),
            ),
        )
        val r = agg.search("x")
        assertThat(r.calc?.result).isEqualTo("2")
        assertThat(r.settings.map { it.key }).containsExactly("wifi")
        assertThat(r.contacts.map { it.name }).containsExactly("Liisa")
    }

    @Test fun `a throwing provider is ignored`() = runTest {
        val agg = SearchAggregator(
            setOf(
                FakeProvider(true, throws = true),
                FakeProvider(true, listOf(SearchResult.Setting("Wi-Fi", "a", "wifi"))),
            ),
        )
        assertThat(agg.search("x").settings.map { it.key }).containsExactly("wifi")
    }

    @Test fun `a disabled provider is skipped`() = runTest {
        val agg = SearchAggregator(
            setOf(FakeProvider(false, listOf(SearchResult.Setting("Wi-Fi", "a", "wifi")))),
        )
        assertThat(agg.search("x").settings).isEmpty()
    }
}
