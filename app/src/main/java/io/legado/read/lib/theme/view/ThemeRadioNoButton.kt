package io.legado.read.lib.theme.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import io.legado.read.R
import io.legado.read.lib.theme.Selector
import io.legado.read.lib.theme.accentColor
import io.legado.read.lib.theme.bottomBackground
import io.legado.read.lib.theme.getPrimaryTextColor
import io.legado.read.utils.ColorUtils
import io.legado.read.utils.dpToPx
import io.legado.read.utils.getCompatColor

class ThemeRadioNoButton(context: Context, attrs: AttributeSet) :
    AppCompatRadioButton(context, attrs) {

    private val isBottomBackground: Boolean

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ThemeRadioNoButton)
        isBottomBackground =
            typedArray.getBoolean(R.styleable.ThemeRadioNoButton_isBottomBackground, false)
        typedArray.recycle()
        initTheme()
    }

    private fun initTheme() {
        when {
            isInEditMode -> Unit
            isBottomBackground -> {
                val accentColor = context.accentColor
                val isLight = ColorUtils.isColorLight(context.bottomBackground)
                val textColor = context.getPrimaryTextColor(isLight)
                val checkedTextColor = if (ColorUtils.isColorLight(accentColor)) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
                background = Selector.shapeBuild()
                    .setCornerRadius(2.dpToPx())
                    .setStrokeWidth(2.dpToPx())
                    .setCheckedBgColor(accentColor)
                    .setCheckedStrokeColor(accentColor)
                    .setDefaultStrokeColor(textColor)
                    .create()
                setTextColor(
                    Selector.colorBuild()
                        .setDefaultColor(textColor)
                        .setCheckedColor(checkedTextColor)
                        .create()
                )
            }
            else -> {
                val accentColor = context.accentColor
                val defaultTextColor = context.getCompatColor(R.color.primaryText)
                val checkedTextColor = if (ColorUtils.isColorLight(accentColor)) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
                background = Selector.shapeBuild()
                    .setCornerRadius(2.dpToPx())
                    .setStrokeWidth(2.dpToPx())
                    .setCheckedBgColor(accentColor)
                    .setCheckedStrokeColor(accentColor)
                    .setDefaultStrokeColor(defaultTextColor)
                    .create()
                setTextColor(
                    Selector.colorBuild()
                        .setDefaultColor(defaultTextColor)
                        .setCheckedColor(checkedTextColor)
                        .create()
                )
            }
        }

    }

}