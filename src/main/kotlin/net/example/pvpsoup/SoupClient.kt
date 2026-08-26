package net.example.pvpsoup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.item.Items
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.ClickType
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.player.Player
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW
import java.io.File

enum class EatTrigger {
    KEY,
    HEALTH
}

data class ModConfig(
    var modEnabled: Boolean = true,
    var autoRefillEnabled: Boolean = true,
    var autoDropBowlsEnabled: Boolean = true,
    var autoEatEnabled: Boolean = true,
    var muteChatEnabled: Boolean = false,
    var killFeedFilterEnabled: Boolean = true,
    var eatTrigger: EatTrigger = EatTrigger.KEY,
    var healthThreshold: Float = 14.0f
)

class SoupClient : ClientModInitializer {

    companion object {
        var config = ModConfig()
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        private val configFile: File = FabricLoader.getInstance().configDir.resolve("soupclient.json").toFile()

        lateinit var eatSoupKey: KeyMapping
        private var lastEatTime = 0L

        fun saveConfig() {
            try {
                configFile.writeText(gson.toJson(config))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadConfig() {
            try {
                if (configFile.exists()) {
                    config = gson.fromJson(configFile.readText(), ModConfig::class.java) ?: ModConfig()
                } else {
                    saveConfig()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                config = ModConfig()
            }
        }
    }

    override fun onInitializeClient() {
        loadConfig()

        eatSoupKey = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "key.soupclient.eat",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.soupclient"
            )
        )

        // Smart Chat / Mute Filter
        ClientReceiveMessageEvents.ALLOW_CHAT.register { message, _, _, _, _ ->
            if (!config.modEnabled) return@register true

            val text = message.string

            if (config.killFeedFilterEnabled) {
                val isKillMessage = text.contains("был убит", ignoreCase = true) ||
                                    text.contains("не смог противостоять", ignoreCase = true) ||
                                    text.contains("серия из", ignoreCase = true)
                if (isKillMessage) return@register false
            }

            if (config.muteChatEnabled) return@register false

            true
        }

        ClientReceiveMessageEvents.ALLOW_GAME.register { _, _ ->
            !config.modEnabled || !config.muteChatEnabled
        }

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            if (!config.modEnabled) return@EndTick

            val player = client.player ?: return@EndTick

            if (client.screen is InventoryScreen) {
                handleInventoryLogic(client)
            }

            if (config.autoDropBowlsEnabled && client.screen == null) {
                dropBowlsFromHotbar(client)
            }

            if (config.autoEatEnabled && client.screen == null) {
                val shouldEat = when (config.eatTrigger) {
                    EatTrigger.KEY -> eatSoupKey.consumeClick()
                    EatTrigger.HEALTH -> player.health <= config.healthThreshold && player.health < player.maxHealth
                }

                if (shouldEat) {
                    tryEatSoup(client)
                }
            }
        })

        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun tryEatSoup(client: Minecraft) {
        val player = client.player ?: return
        val connection = client.connection ?: return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEatTime < 150) return

        val soupSlot = findSoupInHotbar(player)
        if (soupSlot != -1) {
            val previousSlot = player.inventory.selected

            player.inventory.selected = soupSlot
            client.gameMode?.useItem(player, InteractionHand.MAIN_HAND)
            connection.send(ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, player.yRot, player.xRot))
            player.inventory.selected = previousSlot

            lastEatTime = currentTime
        }
    }

    private fun handleInventoryLogic(client: Minecraft) {
        val player = client.player ?: return
        val gameMode = client.gameMode ?: return
        val container = player.containerMenu

        if (config.autoDropBowlsEnabled) {
            for (i in 9..44) {
                val stack = container.getSlot(i).item
                if (stack.`is`(Items.BOWL)) {
                    gameMode.handleInventoryMouseClick(container.containerId, i, 1, ClickType.THROW, player)
                }
            }
        }

        if (config.autoRefillEnabled) {
            for (hotbarSlot in 36..44) {
                val hotbarStack = container.getSlot(hotbarSlot).item
                if (hotbarStack.isEmpty) {
                    val soupSlot = (9..35).firstOrNull { slotIdx ->
                        val item = container.getSlot(slotIdx).item.item
                        item == Items.MUSHROOM_STEW || item == Items.RABBIT_STEW || item == Items.BEETROOT_SOUP || item == Items.SUSPICIOUS_STEW
                    } ?: break

                    gameMode.handleInventoryMouseClick(container.containerId, soupSlot, 0, ClickType.PICKUP, player)
                    gameMode.handleInventoryMouseClick(container.containerId, hotbarSlot, 0, ClickType.PICKUP, player)
                }
            }
        }
    }

    private fun dropBowlsFromHotbar(client: Minecraft) {
        val player = client.player ?: return

        for (i in 0..8) {
            if (player.inventory.getItem(i).`is`(Items.BOWL)) {
                val current = player.inventory.selected
                player.inventory.selected = i
                client.connection?.send(
                    ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS,
                        BlockPos.ZERO,
                        Direction.DOWN
                    )
                )
                player.inventory.selected = current
            }
        }
    }

    private fun findSoupInHotbar(player: Player): Int {
        for (i in 0..8) {
            val item = player.inventory.getItem(i).item
            if (item == Items.MUSHROOM_STEW || item == Items.RABBIT_STEW || item == Items.BEETROOT_SOUP || item == Items.SUSPICIOUS_STEW) {
                return i
            }
        }
        return -1
    }

    private fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            literal("soupclient")
                .executes { ctx ->
                    sendHelp(ctx.source)
                    1
                }
                .then(literal("help").executes { ctx ->
                    sendHelp(ctx.source)
                    1
                })
                .then(literal("toggle")
                    .then(argument("state", BoolArgumentType.bool())
                        .executes { ctx ->
                            config.modEnabled = BoolArgumentType.getBool(ctx, "state")
                            saveConfig()
                            ctx.source.sendFeedback(Component.literal("§a[SoupClient] Mod ${if (config.modEnabled) "ENABLED" else "DISABLED"}"))
                            1
                        }))
                .then(literal("refill")
                    .then(argument("state", BoolArgumentType.bool())
                        .executes { ctx ->
                            config.autoRefillEnabled = BoolArgumentType.getBool(ctx, "state")
                            saveConfig()
                            ctx.source.sendFeedback(Component.literal("§a[SoupClient] Auto-refill: ${config.autoRefillEnabled}"))
                            1
                        }))
                .then(literal("dropbowls")
                    .then(argument("state", BoolArgumentType.bool())
                        .executes { ctx ->
                            config.autoDropBowlsEnabled = BoolArgumentType.getBool(ctx, "state")
                            saveConfig()
                            ctx.source.sendFeedback(Component.literal("§a[SoupClient] Auto-drop bowls: ${config.autoDropBowlsEnabled}"))
                            1
                        }))
                .then(literal("autoeat")
                    .then(argument("state", BoolArgumentType.bool())
                        .executes { ctx ->
                            config.autoEatEnabled = BoolArgumentType.getBool(ctx, "state")
                            saveConfig()
                            ctx.source.sendFeedback(Component.literal("§a[SoupClient] Auto-eat: ${config.autoEatEnabled}"))
                            1
                        }))
                .then(literal("mutechat")
                    .then(argument("state", BoolArgumentType.bool())
                        .executes { ctx ->
                            config.muteChatEnabled = BoolArgumentType.getBool(ctx, "state")
                            saveConfig()
                            ctx.source.sendFeedback(Component.literal("§a[SoupClient] Mute server chat: ${config.muteChatEnabled}"))
                            1
                        }))
                .then(literal("killfeed")
                    .then(argument("state", BoolArgumentType.bool())
                        .executes { ctx ->
                            config.killFeedFilterEnabled = BoolArgumentType.getBool(ctx, "state")
                            saveConfig()
                            ctx.source.sendFeedback(Component.literal("§a[SoupClient] Hide killfeed: ${config.killFeedFilterEnabled}"))
                            1
                        }))
                .then(literal("trigger")
                    .then(argument("mode", StringArgumentType.word())
                        .executes { ctx ->
                            val modeStr = StringArgumentType.getString(ctx, "mode").uppercase()
                            try {
                                config.eatTrigger = EatTrigger.valueOf(modeStr)
                                saveConfig()
                                ctx.source.sendFeedback(Component.literal("§a[SoupClient] AutoEat trigger mode: ${config.eatTrigger}"))
                            } catch (e: Exception) {
                                ctx.source.sendFeedback(Component.literal("§c[SoupClient] Invalid mode! Use: KEY or HEALTH"))
                            }
                            1
                        }))
                .then(literal("health")
                    .then(argument("hearts", FloatArgumentType.floatArg(1.0f, 20.0f))
                        .executes { ctx ->
                            val hearts = FloatArgumentType.getFloat(ctx, "hearts")
                            config.healthThreshold = hearts * 2.0f
                            saveConfig()
                            ctx.source.sendFeedback(Component.literal("§a[SoupClient] Health threshold for AutoEat set to: $hearts hearts (${config.healthThreshold} HP)"))
                            1
                        }))
        )
    }

    private fun sendHelp(source: FabricClientCommandSource) {
        val helpText = """
            §e=== SoupClient Help ===
            §b/soupclient toggle <true|false> §7- Toggle the entire client on/off
            §b/soupclient refill <true|false> §7- Auto-refill hotbar with soup
            §b/soupclient dropbowls <true|false> §7- Auto-drop empty bowls
            §b/soupclient autoeat <true|false> §7- Enable/disable auto-eating soup
            §b/soupclient mutechat <true|false> §7- Mute/unmute all server chat
            §b/soupclient killfeed <true|false> §7- Hide killfeed spam only
            §b/soupclient trigger <key|health> §7- AutoEat trigger mode
            §b/soupclient health <hearts> §7- Health threshold for health mode
            §7* Keybinding can be changed in Minecraft Controls (default: R).
        """.trimIndent()
        source.sendFeedback(Component.literal(helpText))
    }
}
