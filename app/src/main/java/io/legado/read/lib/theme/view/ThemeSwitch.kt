package io.legado.read.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import io.legado.read.lib.theme.accentColor
import io.legado.read.utils.applyTint

/**
 * @author Aidan Follestad (afollestad)
 */
class ThemeSwitch(context: Context, attrs: AttributeSet) : SwitchCompat(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }

    }

}
