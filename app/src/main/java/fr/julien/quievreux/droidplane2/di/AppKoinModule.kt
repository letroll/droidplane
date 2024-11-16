package fr.julien.quievreux.droidplane2.di

import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.data.NodeManager
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appKoinModule = module {
    viewModel{
        MainViewModel(
        )
    }

    single { params ->
        NodeManager(
            coroutineScope = params.get()
        )
    }
}

