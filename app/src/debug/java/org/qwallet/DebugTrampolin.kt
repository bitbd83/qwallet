package org.qwallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.ligi.kaxt.startActivityFromClass
import org.qwallet.activities.StartupActivity

class DebugTrampolin : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivityFromClass(StartupActivity::class.java)
        finish()
    }
}
