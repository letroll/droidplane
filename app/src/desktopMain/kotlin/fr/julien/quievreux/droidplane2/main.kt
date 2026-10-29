package fr.julien.quievreux.droidplane2

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import fr.julien.quievreux.droidplane2.core.di.coreKoinModule
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import fr.julien.quievreux.droidplane2.data.di.dataKoinModule
import fr.julien.quievreux.droidplane2.di.appCommonKoinModule
import fr.julien.quievreux.droidplane2.di.appDesktopKoinModule
import fr.julien.quievreux.droidplane2.ui.DesktopDroidplaneApp
import org.koin.core.context.startKoin

fun main() {
    val koinApp = startKoin {
        modules(appCommonKoinModule, appDesktopKoinModule, coreKoinModule, dataKoinModule)
    }
    val viewModel = koinApp.koin.get<MainViewModel>()
    val recentFilesRepository = koinApp.koin.get<RecentFilesRepository>()
    val logger = koinApp.koin.get<Logger>()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "DroidPlane2",
            onPreviewKeyEvent = { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.key == Key.Z &&
                    (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)
                ) {
                    if (viewModel.canUndoDelete) {
                        viewModel.onUndoDelete()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        ) {
            DesktopDroidplaneApp(
                viewModel = viewModel,
                recentFilesRepository = recentFilesRepository,
                logger = logger,
                onExitApplication = ::exitApplication
            )
        }
    }
}
