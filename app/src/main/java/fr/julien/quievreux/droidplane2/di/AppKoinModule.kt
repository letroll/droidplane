package fr.julien.quievreux.droidplane2.di

import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.core.extensions.default
import fr.julien.quievreux.droidplane2.ui.view.MindMapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appKoinModule = module {
    factory<CoroutineScope> {
        Dispatchers.default()
    }

    viewModel{
        MainViewModel(
            logger = get(),
        )
    }

    viewModel{
        MindMapViewModel(
            logger = get(),
        )
    }

//    single { params ->
//        SearchManager(
//            nodesSource = params[0],
//            scope = get(),
//            logger = get(),
//            fetchText = params[1]
//        )
//    }

    scope<MainViewModel> {
        scoped { parametersHolder ->
            NodeManager(
                coroutineScope = get(),
                logger = get(),
                nodeUtils = get(),
                xmlParseUtils = get(),
            )
        }
    }
}