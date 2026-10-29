package fr.julien.quievreux.droidplane2.core.di

import fr.julien.quievreux.droidplane2.core.log.AndLogger
import fr.julien.quievreux.droidplane2.core.log.Logger

actual fun createPlatformLogger(): Logger = AndLogger()
