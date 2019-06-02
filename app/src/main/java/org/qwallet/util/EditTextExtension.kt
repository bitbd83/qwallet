package org.qwallet.util

import android.widget.EditText

fun EditText.hasText() = text?.isNotBlank() == true
