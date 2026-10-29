package fr.julien.quievreux.droidplane2.core.di

import fr.julien.quievreux.droidplane2.core.log.Logger
import org.koin.dsl.module

expect fun createPlatformLogger(): Logger

val coreKoinModule = module {
    factory<Logger> {
        createPlatformLogger()
    }
}


