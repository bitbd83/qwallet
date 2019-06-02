package org.qwallet.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.qwallet.R
import org.qwallet.data.EXTRA_KEY_ADDRESS
import org.qwallet.data.addressbook.AddressBookEntry

open class AccountPickActivity : BaseAddressBookActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.subtitle = getString(R.string.address_book_subtitle)
    }

    override fun onAddressClick(addressEntry: AddressBookEntry) {
        setResult(Activity.RESULT_OK, Intent().apply { putExtra(EXTRA_KEY_ADDRESS, addressEntry.address.hex) })
        finish()
    }

}
