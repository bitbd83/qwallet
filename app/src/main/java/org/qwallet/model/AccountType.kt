package org.qwallet.model

import android.app.Activity
import androidx.annotation.DrawableRes
import org.qwallet.data.addressbook.AccountKeySpec

data class AccountType(
        val accountType: String?,
        val name: String,
        val action: String,
        val description: String,
        @DrawableRes val drawable: Int,
        @DrawableRes val actionDrawable: Int,
        val wrapsKey: Boolean = false,
        val callback: (context: Activity, inSpec: AccountKeySpec) -> Unit = { _, _ -> }
)