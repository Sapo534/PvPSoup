package net.example.pvpsoup.util

import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object ChatUtils {
    private const val PREFIX = "§8[§cSoupClient§8] §f"

    fun info(message: String) {
        sendMessage(Text.literal("$PREFIX$message"))
    }

    fun success(message: String) {
        sendMessage(Text.literal("$PREFIX§a$message"))
    }

    fun error(message: String) {
        sendMessage(Text.literal("$PREFIX§c$message"))
    }

    private fun sendMessage(text: Text) {
        val client = MinecraftClient.getInstance()
        client.player?.sendMessage(text, false)
    }
}
