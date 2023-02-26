package io.legado.read.ui.widget.text

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.read.R
import io.legado.read.lib.theme.Selector
import io.legado.read.lib.theme.ThemeStore
import io.legado.read.utils.ColorUtils
import io.legado.read.utils.dpToPx
import io.legado.read.utils.getCompatColor

class AccentBgTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private var radius = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccentBgTextView)
        radius = typedArray.getDimensionPixelOffset(R.styleable.AccentBgTextView_radius, radius)
        typedArray.recycle()
        upBackground()
    }

    fun setRadius(radius: Int) {
        this.radius = radius.dpToPx()
        upBackground()
    }

    private fun upBackground() {
        val accentColor = if (isInEditMode) {
            context.getCompatColor(R.color.accent)
        } else {
            ThemeStore.accentColor(context)
        }
        background = Selector.shapeBuild()
            .setCornerRadius(radius)
            .setDefaultBgColor(accentColor)
            .setPressedBgColor(ColorUtils.darkenColor(accentColor))
            .create()
        setTextColor(
            if (ColorUtils.isColorLight(accentColor)) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        )
    }
}
