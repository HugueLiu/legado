package io.legado.read.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.legado.read.lib.theme.Selector
import io.legado.read.lib.theme.ThemeStore
import io.legado.read.lib.theme.bottomBackground
import io.legado.read.lib.theme.getSecondaryTextColor
import io.legado.read.utils.ColorUtils

class ThemeBottomNavigationVIew(context: Context, attrs: AttributeSet) :
    BottomNavigationView(context, attrs) {

    init {
        val bgColor = context.bottomBackground
        setBackgroundColor(bgColor)
        val textIsDark = ColorUtils.isColorLight(bgColor)
        val textColor = context.getSecondaryTextColor(textIsDark)
        val colorStateList = Selector.colorBuild()
            .setDefaultColor(textColor)
            .setSelectedColor(ThemeStore.accentColor(context)).create()
        itemIconTintList = colorStateList
        itemTextColor = colorStateList
    }

}