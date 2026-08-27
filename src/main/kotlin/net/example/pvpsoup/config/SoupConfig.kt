package net.example.pvpsoup.config

import net.example.pvpsoup.util.ColorMode

data class SoupConfig(
    val enabledModules: MutableMap<String, Boolean> = mutableMapOf(
        "AntiInvisibility" to true,
        "Chams" to true,
        "Esp2D" to false,
        "AutoSoup" to false,
        "ChatMute" to false,
        "AntiWither" to true
    ),
    var autoSoupHealth: Float = 13.0f,
    var autoSoupDelayMs: Long = 220L,
    var chamsMode: ColorMode = ColorMode.TRANS,
    var chamsAlpha: Float = 0.6f,
    var chamsStaticRgb: Int = 0x00FF88
)
