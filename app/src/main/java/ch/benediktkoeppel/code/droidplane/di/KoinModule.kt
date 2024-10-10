package ch.benediktkoeppel.code.droidplane.di

import ch.benediktkoeppel.code.droidplane.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    viewModel{
        MainViewModel()
    }
}

