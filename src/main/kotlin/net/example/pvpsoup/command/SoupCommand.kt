package net.example.pvpsoup.command

import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.example.pvpsoup.config.ConfigManager
import net.example.pvpsoup.feature.FeatureManager
import net.example.pvpsoup.util.ChatUtils
import net.example.pvpsoup.util.ColorMode

object SoupCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            val featureSuggestions = SuggestionProvider<FabricClientCommandSource> { _, builder ->
                FeatureManager.getAllFeatures().forEach { builder.suggest(it.name) }
                builder.buildFuture()
            }

            val commandNode = ClientCommandManager.literal("soupclient")
                .then(ClientCommandManager.literal("list")
                    .executes {
                        ChatUtils.info("§lСписок модулей SoupClient:")
                        FeatureManager.getAllFeatures().forEach { feature ->
                            val status = if (feature.isEnabled) "§a[ВКЛ]" else "§c[ВЫКЛ]"
                            ChatUtils.info(" - §e${feature.name} $status §7- ${feature.description}")
                        }
                        1
                    }
                )
                .then(ClientCommandManager.literal("toggle")
                    .then(ClientCommandManager.argument("module", StringArgumentType.word())
                        .suggests(featureSuggestions)
                        .executes { context ->
                            val moduleName = StringArgumentType.getString(context, "module")
                            val feature = FeatureManager.getFeature(moduleName)
                            if (feature != null) {
                                feature.toggle()
                            } else {
                                ChatUtils.error("Модуль '$moduleName' не найден! Используйте /soupclient list")
                            }
                            1
                        }
                    )
                )
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.literal("autosoup")
                        .then(ClientCommandManager.argument("health", FloatArgumentType.floatArg(1.0f, 20.0f))
                            .executes { context ->
                                val health = FloatArgumentType.getFloat(context, "health")
                                ConfigManager.config.autoSoupHealth = health
                                ConfigManager.save()
                                ChatUtils.success("Порог здоровья AutoSoup установлен на $health HP (${health / 2} сердечек)")
                                1
                            }
                        )
                    )
                )
                .then(ClientCommandManager.literal("chams")
                    .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                        .suggests { _, builder ->
                            ColorMode.entries.forEach { builder.suggest(it.name.lowercase()) }
                            builder.buildFuture()
                        }
                        .executes { context ->
                            val modeStr = StringArgumentType.getString(context, "mode").uppercase()
                            try {
                                val mode = ColorMode.valueOf(modeStr)
                                ConfigManager.config.chamsMode = mode
                                ConfigManager.save()
                                ChatUtils.success("Режим силуэта Chams установлен на: §e$mode")
                            } catch (e: Exception) {
                                ChatUtils.error("Неизвестный режим. Доступно: static, rainbow, lgbt, trans, pan")
                            }
                            1
                        }
                    )
                )

            dispatcher.register(commandNode)
            dispatcher.register(ClientCommandManager.literal("soup").redirect(dispatcher.getRoot().getChild("soupclient")))
        }
    }
}
