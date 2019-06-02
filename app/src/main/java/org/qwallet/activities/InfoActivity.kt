package org.qwallet.activities

import android.os.Bundle
import android.text.method.LinkMovementMethod
import kotlinx.android.synthetic.main.activity_info.*
import org.ligi.compat.HtmlCompat
import org.qwallet.BuildConfig
import org.qwallet.R

class InfoActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_info)

        supportActionBar?.subtitle = getString(R.string.info_activity_subtitle, BuildConfig.VERSION_NAME)

        intro_text.text = HtmlCompat.fromHtml(getString(R.string.info_text))
        intro_text.movementMethod = LinkMovementMethod()

    }

}
