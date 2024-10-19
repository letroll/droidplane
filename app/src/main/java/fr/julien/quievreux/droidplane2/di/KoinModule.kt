package fr.julien.quievreux.droidplane2.di

import fr.julien.quievreux.droidplane2.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    viewModel{
        MainViewModel()
    }
}
