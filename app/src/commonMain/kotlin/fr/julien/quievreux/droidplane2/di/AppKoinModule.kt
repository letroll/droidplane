package fr.julien.quievreux.droidplane2.di

import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.core.extensions.default
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.update.GitHubUpdateCheckerServiceImpl
import fr.julien.quievreux.droidplane2.update.UpdateCheckerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appCommonKoinModule = module {
    factory<CoroutineScope> {
        Dispatchers.default()
    }

    single<UpdateCheckerService> {
        GitHubUpdateCheckerServiceImpl()
    }

    viewModel {
        MainViewModel(
            logger = get(),
            recentFilesRepository = get(),
            updateCheckerService = get(),
        )
    }

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
