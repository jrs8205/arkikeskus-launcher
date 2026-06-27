package org.arkikeskus.launcher.data.di

import javax.inject.Qualifier

/** Marks the process-lifetime [kotlinx.coroutines.CoroutineScope] for fire-and-forget work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
