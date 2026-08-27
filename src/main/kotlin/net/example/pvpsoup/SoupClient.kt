package net.example.pvpsoup

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.example.pvpsoup.command.SoupCommand
import net.example.pvpsoup.config.ConfigManager
import net.example.pvpsoup.event.SoundPlayCallback
import net.example.pvpsoup.feature.FeatureManager
import net.example.pvpsoup.feature.render.Esp2DRenderer
import net.example.pvpsoup.gui.ClickGuiScreen
import org.lwjgl.glfw.GLFW

class SoupClient : ClientModInitializer {

    companion object {
        lateinit var guiKeyBinding: KeyBinding
    }

    override fun onInitializeClient() {
        ConfigManager.load()
        FeatureManager.init()
        SoupCommand.register()

        // 1. Фильтрация входящего чата сервера (ChatMute)
        ClientReceiveMessageEvents.ALLOW_CHAT.register { _, _, _, _, _ ->
            !ConfigManager.isModuleEnabled("ChatMute")
        }

        ClientReceiveMessageEvents.ALLOW_GAME.register { _, _ ->
            !ConfigManager.isModuleEnabled("ChatMute")
        }

        // 2. Блокировка звука спавна Визера (AntiWither)
        SoundPlayCallback.EVENT.register { sound ->
            if (ConfigManager.isModuleEnabled("AntiWither")) {
                val soundId = sound.id.toString()
                if (soundId.contains("wither.spawn")) {
                    return@register true // Отменяем воспроизведение звука
                }
            }
            false
        }

        // 3. Горячая клавиша ClickGUI (Right Shift)
        guiKeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.soupclient.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.soupclient.title"
            )
        )

        // 4. Рендеринг ESP
        WorldRenderEvents.LAST.register { context ->
            Esp2DRenderer.onRenderWorld(context)
        }

        // 5. Тики и клавиши
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.player != null && client.world != null) {
                FeatureManager.onTick()

                while (guiKeyBinding.wasPressed()) {
                    client.setScreen(ClickGuiScreen())
                }
            }
        }
    }
}
