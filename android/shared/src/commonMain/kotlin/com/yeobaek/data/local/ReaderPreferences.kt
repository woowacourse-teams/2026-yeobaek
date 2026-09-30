package com.yeobaek.data.local

import com.russhwolf.settings.Settings

class ReaderPreferences(
    private val settings: Settings,
) {
    fun saveFontSize(fontSize: Int) {
        settings.putInt(FONT_SIZE, fontSize)
    }

    fun getFontSize(): Int? = settings.getIntOrNull(FONT_SIZE)

    companion object {
        private const val FONT_SIZE = "readerFontSize"
    }
}
