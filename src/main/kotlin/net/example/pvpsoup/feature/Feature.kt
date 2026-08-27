package net.example.pvpsoup.feature

import net.example.pvpsoup.config.ConfigManager
import net.example.pvpsoup.util.ChatUtils

abstract class Feature(
    val name: String,
    val description: String
) {
    var isEnabled: Boolean
        get() = ConfigManager.config.enabledModules.getOrDefault(name, false)
        set(value) {
            ConfigManager.config.enabledModules[name] = value
            ConfigManager.save()
            if (value) onEnable() else onDisable()
        }

    fun toggle(): Boolean {
        isEnabled = !isEnabled
        if (isEnabled) {
            ChatUtils.success("Модуль $name §2включен")
        } else {
            ChatUtils.error("Модуль $name §4выключен")
        }
        return isEnabled
    }

    open fun onEnable() {}
    open fun onDisable() {}
    open fun onTick() {}
}
