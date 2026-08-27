package net.example.pvpsoup

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.example.pvpsoup.command.SoupCommand
import net.example.pvpsoup.config.ConfigManager
import net.example.pvpsoup.feature.FeatureManager
import net.example.pvpsoup.feature.render.Esp2DRenderer

class SoupClient : ClientModInitializer {

    override fun onInitializeClient() {
        ConfigManager.load()
        FeatureManager.init()
        SoupCommand.register()

        // Подписка на рендер мира
        WorldRenderEvents.LAST.register { context ->
            Esp2DRenderer.onRenderWorld(context)
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.player != null && client.world != null) {
                FeatureManager.onTick()
            }
        }
    }
}
