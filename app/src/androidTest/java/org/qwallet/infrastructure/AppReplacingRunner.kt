package org.qwallet.infrastructure

import org.ligi.trulesk.AppReplacingRunnerBase

class AppReplacingRunner : AppReplacingRunnerBase() {

    override fun testAppClass() = TestApp::class.java

}
