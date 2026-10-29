package fr.julien.quievreux.droidplane2.di

import android.content.Context
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import fr.julien.quievreux.droidplane2.data.RecentFilesRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appAndroidKoinModule = module {
    single<RecentFilesRepository> {
        RecentFilesRepositoryImpl(
            androidContext().getSharedPreferences("droidplane_recent_files", Context.MODE_PRIVATE)
        )
    }
}

val appKoinModule = listOf(appCommonKoinModule, appAndroidKoinModule)
