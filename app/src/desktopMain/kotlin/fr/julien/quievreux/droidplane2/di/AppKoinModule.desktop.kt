package fr.julien.quievreux.droidplane2.di

import fr.julien.quievreux.droidplane2.data.DesktopRecentFilesRepository
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import org.koin.dsl.module

val appDesktopKoinModule = module {
    single<RecentFilesRepository> {
        DesktopRecentFilesRepository()
    }
}
