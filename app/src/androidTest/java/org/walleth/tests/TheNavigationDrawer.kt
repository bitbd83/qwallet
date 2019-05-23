package org.walleth.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.matcher.ViewMatchers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.ligi.trulesk.TruleskActivityRule
import org.mockito.Mockito.`when`
import org.walleth.R
import org.walleth.activities.MainActivity
import org.walleth.data.addressbook.AddressBookEntry
import org.walleth.infrastructure.TestApp

class TheNavigationDrawer {

    @get:Rule
    var rule = TruleskActivityRule(MainActivity::class.java, false)

    @Test
    fun navigationDrawerIsUsuallyNotShown() {
        `when`(TestApp.mySettings.onboardingDone).thenReturn(true)

        rule.launchActivity()
        onView(withId(R.id.navigationView)).check(matches(not(isDisplayed())))
    }

    @Test
    fun navigationDrawerOpensWhenRequested() {
        `when`(TestApp.mySettings.onboardingDone).thenReturn(true)

        rule.launchActivity()
        onView(withId(R.id.drawer_layout)).perform(open())
        onView(withId(R.id.navigationView)).check(matches(isDisplayed()))
    }

    @Test
    fun testNameIsDisplayedCorrectly() {
        `when`(TestApp.mySettings.onboardingDone).thenReturn(true)

        GlobalScope.async(Dispatchers.Main) {
            async(Dispatchers.Default) {
                TestApp.testDatabase.addressBook.upsert(AddressBookEntry(name = "espresso ligi", address = TestApp.currentAddressProvider.getCurrentNeverNull()))
            }.await()
        }

        rule.launchActivity()
        onView(withId(R.id.drawer_layout)).perform(open())


        onView(withId(R.id.accountName)).check(matches(withText("espresso ligi")))

        rule.screenShot("drawer_opened")
    }


}
