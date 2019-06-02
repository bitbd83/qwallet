package org.qwallet.activities

import android.app.Activity
import android.graphics.Paint
import android.text.TextPaint
import android.util.TypedValue
import android.view.MotionEvent
import com.github.amlcurran.showcaseview.OnShowcaseEventListener
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import org.ligi.tracedroid.TraceDroid
import org.ligi.tracedroid.sending.TraceDroidEmailSender
import org.qwallet.R
import org.qwallet.data.config.Settings
import org.qwallet.viewmodels.TransactionListViewModel

class OnboardingController(val activity: Activity,
                           private val viewModel: TransactionListViewModel,
                           val settings: Settings) {

    private val showcaseView by lazy {
        ShowcaseView.Builder(activity)
                .setTarget(ViewTarget(R.id.receive_button, activity))
                .setContentText(R.string.onboard_showcase_message)
                .setContentTextPaint(contentPaint)
                .build()
    }

    private val contentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = activity.resources.getDimension(R.dimen.abc_text_size_title_material)
        val typedValue = TypedValue()
        val arr = activity.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.textColorPrimary))
        val primaryColor = arr.getColor(0, -1)
        color = primaryColor
        arr.recycle()
    }

    fun install() {
        if (!settings.onboardingDone) {
            viewModel.isOnboardingVisible.value = true

            showcaseView.setOnShowcaseEventListener(object : OnShowcaseEventListener {
                override fun onShowcaseViewShow(p0: ShowcaseView?) {}
                override fun onShowcaseViewHide(p0: ShowcaseView?) {}
                override fun onShowcaseViewDidHide(p0: ShowcaseView?) {
                    dismiss()
                }

                override fun onShowcaseViewTouchBlocked(p0: MotionEvent?) {}
            })

            showcaseView.show()

            settings.onboardingDone = true
        } else {
            if (TraceDroid.getStackTraceFiles().isNotEmpty()) {
                TraceDroidEmailSender.sendStackTraces("info@qpay.group", activity)
            }
        }

    }

    fun dismiss() {
        if (viewModel.isOnboardingVisible.value == true) {
            viewModel.isOnboardingVisible.value = false
            showcaseView.hide()
        }
    }
}
