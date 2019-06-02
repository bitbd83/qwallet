package org.qwallet.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.qwallet.R
import org.qwallet.activities.SelectTokenActivity

class TheSelectTokensActivity {

    @get:Rule
    var rule = TruleskActivityRule(SelectTokenActivity::class.java)

    @Test
    fun listShows() {

        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))

        rule.screenShot("token_select")
    }

}