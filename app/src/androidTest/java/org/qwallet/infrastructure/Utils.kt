package org.qwallet.infrastructure

import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.loadKoinModules
import org.qwallet.data.tokens.CurrentTokenProvider
import org.qwallet.data.tokens.Token

fun setCurrentToken(token: Token) {
    loadKoinModules(
            listOf(module(override = true) {
                single { CurrentTokenProvider(get()).apply {
                    setCurrent(token)
                } }
            })
    )
}