package fr.julien.quievreux.droidplane2.di

import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import fr.julien.quievreux.droidplane2.data.RecentFilesRepositoryImpl
import fr.julien.quievreux.droidplane2.core.extensions.default
import fr.julien.quievreux.droidplane2.ui.view.MindMapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appKoinModule = module {
    factory<CoroutineScope> {
        Dispatchers.default()
    }

    viewModel{
        MainViewModel(
            logger = get(),
            recentFilesRepository = get(),
        )
    }

    viewModel{
        MindMapViewModel(
            logger = get(),
        )
    }

    single<RecentFilesRepository> {
        RecentFilesRepositoryImpl(
            androidContext().getSharedPreferences("droidplane_recent_files", android.content.Context.MODE_PRIVATE)
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
                logger = get(),
                nodeUtils = get(),
                xmlParseUtils = get(),
                coroutineScope = parametersHolder.get(),
            )
        }
    }
}