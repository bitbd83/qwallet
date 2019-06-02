package org.qwallet.tests

import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.qwallet.activities.ExportKeyActivity

class TheExportKeyActivity {

    @get:Rule
    var rule = TruleskActivityRule(ExportKeyActivity::class.java)

    @Test
    fun exportShows() {
        rule.screenShot("export")
    }

}
