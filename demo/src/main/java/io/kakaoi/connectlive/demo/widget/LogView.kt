package io.kakaoi.connectlive.demo.widget

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.text.color
import androidx.core.view.isVisible
import io.kakaoi.connectlive.demo.databinding.ViewLogBinding

open class LogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val binding = ViewLogBinding.inflate(LayoutInflater.from(context), this)

    private val colorMap = mapOf(
        Log.VERBOSE to Color.DKGRAY,
        Log.DEBUG to Color.LTGRAY,
        Log.INFO to Color.CYAN,
        Log.WARN to Color.MAGENTA,
        Log.ERROR to Color.RED,
        Log.ASSERT to Color.WHITE
    )

    init {
        binding.closeLog.setOnClickListener(::onClickClosed)
    }

    fun log(priority: Int, tag: String, message: String) {
        binding.logs.text = SpannableStringBuilder(binding.logs.text).apply {
            if (isNotEmpty())
                append('\n')

            val textColor = colorMap.getOrDefault(priority, Color.GRAY)
            color(textColor) { append("$tag: $message") }
        }.takeLast(MAX_LINES)
    }

    protected open fun onClickClosed(v: View) {
        isVisible = false
    }

    companion object {
        private const val MAX_LINES = 1000
    }
}