package io.kakaoi.connectlive.demo.widget

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import io.kakaoi.connectlive.demo.databinding.ViewLogBinding
import io.kakaoi.connectlive.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

open class LogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), Logger {
    private val binding = ViewLogBinding.inflate(LayoutInflater.from(context), this)
    private val logs = MutableSharedFlow<CharSequence>(extraBufferCapacity = 16)
    private var isAggregatedVisible = false

    init {
        binding.closeLog.setOnClickListener(::onClickClosed)

        val textBuilder = SpannableStringBuilder()

        logs
            .onEach {
                synchronized(textBuilder) {
                    textBuilder.appendLine(it)
                    val overflowed = textBuilder.length - MAX_CHARS
                    if (overflowed > 0)
                        textBuilder.replace(0, overflowed, "")
                }
            }
            .flowOn(Dispatchers.IO)
            .filter { isAggregatedVisible }
            .conflate()
            .onEach {
                synchronized(textBuilder) {
                    binding.logs.text = textBuilder
                }
                delay(100)
            }
            .launchIn(MainScope())
    }

    override fun log(priority: Int, tag: String, message: String) {
        val record = buildSpannedString {
            val textColor = colorMap.getOrDefault(priority, Color.GRAY)
            color(textColor) {
                append(dateFormat.format(System.currentTimeMillis()))
                append('\t')
                append(tag)
                append(' ')
                append(message)
            }
        }

        logs.tryEmit(record)
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        isAggregatedVisible = isVisible
    }

    protected open fun onClickClosed(v: View) {
        isVisible = false
    }

    companion object {
        private const val MAX_CHARS = 10000

        private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT)

        private val colorMap = mapOf(
            Log.VERBOSE to Color.DKGRAY,
            Log.DEBUG to Color.LTGRAY,
            Log.INFO to Color.CYAN,
            Log.WARN to Color.MAGENTA,
            Log.ERROR to Color.RED,
            Log.ASSERT to Color.WHITE
        )
    }
}