package org.arkikeskus.launcher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.arkikeskus.launcher.model.AppItem

/**
 * Coil fetcher that resolves an [AppItem]'s launcher icon via [LauncherAppsSource]. The icon is
 * **rasterized to a Bitmap once** here rather than handed to Coil as a live Drawable: launcher icons
 * are usually AdaptiveIconDrawables that re-composite their layers on every draw, which makes a
 * scrolling grid stutter. A cached Bitmap is just a cheap blit per frame.
 */
class AppIconFetcher(
    private val data: AppItem,
    private val source: LauncherAppsSource,
) : Fetcher {
    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val drawable = source.loadIcon(data) ?: return@withContext null
        val size = ICON_PX
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val source: LauncherAppsSource) : Fetcher.Factory<AppItem> {
        override fun create(data: AppItem, options: Options, imageLoader: ImageLoader): Fetcher =
            AppIconFetcher(data, source)
    }

    private companion object {
        /** Rasterization size (px); crisp for icons up to ~56dp at xxxhdpi. */
        const val ICON_PX = 168
    }
}
