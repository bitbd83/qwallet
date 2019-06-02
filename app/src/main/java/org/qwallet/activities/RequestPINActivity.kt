package org.qwallet.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import org.ligi.kaxtui.alert
import org.qwallet.R
import org.qwallet.data.EXTRA_KEY_PIN
import org.qwallet.ui.KEY_MAP_TOUCH_WITH_ZERO
import org.qwallet.ui.showPINDialog

fun Activity.showAccountPinDialog(@StringRes title: Int = R.string.please_enter_your_pin, onPIN: (pin: String?) -> Unit) {

    showPINDialog(
            labelButtons = true,
            pinPadMapping = KEY_MAP_TOUCH_WITH_ZERO,
            onPIN = onPIN,
            onCancel = {
                onPIN(null)
                finish()
            },
            title = title
    )
}

class RequestPINActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showAccountPinDialog { pin1 ->
            showAccountPinDialog(R.string.please_confirm_your_pin) { pin2 ->
                if (pin1 == pin2) {
                    setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_KEY_PIN, pin1))
                    finish()
                } else {
                    alert("PIN do not mach", title = "Error") {
                        finish()
                    }
                }
            }
        }
    }


}
