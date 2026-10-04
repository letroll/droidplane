package fr.julien.quievreux.droidplane2.data.di

import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.NodeManagerContract
import fr.julien.quievreux.droidplane2.data.NodeUtils
import fr.julien.quievreux.droidplane2.data.NodeUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.XmlParseUtils
import fr.julien.quievreux.droidplane2.data.XmlParseUtilsDefaultImpl
import org.koin.dsl.bind
import org.koin.dsl.module

val dataKoinModule = module {
    single<NodeUtils> {
        NodeUtilsDefaultImpl()
    }

    single<XmlParseUtils> {
        XmlParseUtilsDefaultImpl(
            nodeUtils = get(),
            logger = get(),
        )
    }

    factory { params ->
        NodeManager(
            logger = get(),
            nodeUtils = get(),
            xmlParseUtils = get(),
            coroutineScope = params.get(),
        )
    } bind NodeManagerContract::class
}
