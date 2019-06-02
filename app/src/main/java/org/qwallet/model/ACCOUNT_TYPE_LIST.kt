package org.qwallet.model

import android.app.Activity
import android.content.Context.NFC_SERVICE
import android.content.Intent
import android.nfc.NfcManager
import org.ligi.kaxtui.alert
import org.qwallet.R
import org.qwallet.activities.ImportKeyActivity
import org.qwallet.activities.RequestPINActivity
import org.qwallet.activities.RequestPasswordActivity
import org.qwallet.activities.nfc.NFCGetAddressActivity
import org.qwallet.activities.trezor.TrezorGetAddressActivity
import org.qwallet.data.*
import org.qwallet.data.addressbook.AccountKeySpec


val ACCOUNT_TYPE_LIST = listOf(

        AccountType(ACCOUNT_TYPE_BURNER,
                "Burner",
                "Create Burner",
                "Easy to get you started but weak security.",
                R.drawable.ic_whatshot_black_24dp,
                R.drawable.ic_key,
                wrapsKey = true,
                callback = { activity, inSpec -> activity.finishWithType(inSpec, ACCOUNT_TYPE_BURNER) }
        ),
        AccountType(
                ACCOUNT_TYPE_TREZOR,
                "TREZOR wallet",
                "Connect TREZOR",
                "Very reasonable security.",
                R.drawable.trezor_icon_black,
                R.drawable.trezor_icon_black
        ) { activity, _ ->
            activity.startActivityForResult(Intent(activity, TrezorGetAddressActivity::class.java), REQUEST_CODE_IMPORT)
        },
        AccountType(
                ACCOUNT_TYPE_IMPORT,
                "Imported Key",
                "Import Key",
                "Import a key - e.g. your backup",
                R.drawable.ic_import,
                R.drawable.ic_import) { activity, _ ->
            activity.startActivityForResult(Intent(activity, ImportKeyActivity::class.java), REQUEST_CODE_IMPORT)
        },
        AccountType(ACCOUNT_TYPE_WATCH_ONLY,
                "Watch only account",
                "Watch Only",
                "No transactions possible.",
                R.drawable.ic_watch,
                R.drawable.ic_watch,
                callback = { activity, inSpec -> activity.finishWithType(inSpec, ACCOUNT_TYPE_WATCH_ONLY) }),
        AccountType(ACCOUNT_TYPE_NFC,
                "NFC account",
                "Connect via NFC",
                "Contact-less connection.",
                R.drawable.ic_nfc_black,
                R.drawable.ic_nfc_black) { activity, _ ->
            val manager = activity.getSystemService(NFC_SERVICE) as NfcManager
            val adapter = manager.defaultAdapter
            if (adapter != null && adapter.isEnabled) {
                activity.startActivityForResult(Intent(activity, NFCGetAddressActivity::class.java), REQUEST_CODE_PICK_NFC)
            } else {
                activity.alert("NFC not available.")
            }

        },
        AccountType(ACCOUNT_TYPE_PIN_PROTECTED,
                "PIN protected",
                "Create Key with PIN",
                "More secure than a burner.",
                R.drawable.ic_fiber_pin_black_24dp,
                R.drawable.ic_fiber_pin_black_24dp,
                wrapsKey = true) { activity, _ ->
            activity.startActivityForResult(Intent(activity, RequestPINActivity::class.java), REQUEST_CODE_ENTER_PIN)
        },
        AccountType(ACCOUNT_TYPE_PASSWORD_PROTECTED,
                "password protected",
                "Create Key with password",
                "Similar to PIN.",
                R.drawable.ic_keyboard_black_24dp,
                R.drawable.ic_keyboard_black_24dp,
                wrapsKey = true) { activity, _ ->
            activity.startActivityForResult(Intent(activity, RequestPasswordActivity::class.java), REQUEST_CODE_ENTER_PASSWORD)
        }
)

private fun Activity.finishWithType(inSpec: AccountKeySpec, type: String) {
    setResult(Activity.RESULT_OK,
            Intent().putExtra(EXTRA_KEY_ACCOUNTSPEC, inSpec.copy(type = type))
    )
    finish()
}

val ACCOUNT_TYPE_MAP by lazy {
    mutableMapOf<String, AccountType>().apply {
        ACCOUNT_TYPE_LIST.forEach {
            it.accountType?.let { accountType -> put(accountType, it) }
        }
    }
}
