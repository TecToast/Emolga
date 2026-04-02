package de.tectoast.emolga.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Module
class PlatformModule {
    @Single
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Single
    fun defaultClock(): Clock = Clock.System
}
