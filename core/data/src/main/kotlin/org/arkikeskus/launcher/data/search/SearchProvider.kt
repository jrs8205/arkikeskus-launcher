package org.arkikeskus.launcher.data.search

import org.arkikeskus.launcher.model.SearchResult

/** A single search source (apps, settings, contacts, calculator). */
interface SearchProvider {
    /** Whether this source should run given the current settings/permission state. */
    suspend fun isEnabled(): Boolean

    /** Matches for [query] (already trimmed and non-blank). Must not throw for normal input. */
    suspend fun query(query: String): List<SearchResult>
}
