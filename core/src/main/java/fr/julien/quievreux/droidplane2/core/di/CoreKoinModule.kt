package fr.julien.quievreux.droidplane2.core.di

import fr.julien.quievreux.droidplane2.core.log.AndLogger
import fr.julien.quievreux.droidplane2.core.log.Logger
import org.koin.dsl.module

val coreKoinModule = module {
    factory<Logger> {
        AndLogger()
    }
}

