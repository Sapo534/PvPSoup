package net.example.pvpsoup.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object ConfigManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FabricLoader.getInstance().configDir.resolve("soupclient.json").toFile()

    @get:JvmStatic
    var config: SoupConfig = SoupConfig()
        private set

    @JvmStatic
    fun isModuleEnabled(name: String): Boolean {
        return config.enabledModules.getOrDefault(name, false)
    }

    fun load() {
        if (!configFile.exists()) {
            save()
            return
        }

        try {
            configFile.reader().use { reader ->
                config = gson.fromJson(reader, SoupConfig::class.java) ?: SoupConfig()
            }

            // Добавляем новые модули в старые конфиги автоматически
            val defaultModules = SoupConfig().enabledModules
            defaultModules.forEach { (key, value) ->
                config.enabledModules.putIfAbsent(key, value)
            }
            save()
        } catch (e: Exception) {
            e.printStackTrace()
            config = SoupConfig()
            save()
        }
    }

    fun save() {
        try {
            configFile.parentFile.mkdirs()
            configFile.writer().use { writer ->
                gson.toJson(config, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
