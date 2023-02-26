package io.legado.read.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import io.legado.read.lib.theme.accentColor
import io.legado.read.utils.applyTint

class ThemeProgressBar(context: Context, attrs: AttributeSet) : ProgressBar(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}