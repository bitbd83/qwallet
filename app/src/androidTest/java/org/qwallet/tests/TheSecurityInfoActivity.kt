package org.qwallet.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.qwallet.R
import org.qwallet.activities.SecurityInfoActivity

class TheSecurityInfoActivity {

    @get:Rule
    var rule = TruleskActivityRule(SecurityInfoActivity::class.java)

    @Test
    fun infoShows() {
        onView(withText(R.string.security_info)).check(matches(isDisplayed()))

        rule.screenShot("security_info")
    }

}
