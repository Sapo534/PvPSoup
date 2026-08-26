package net.example.pvpsoup

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
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.item.Items
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.ClickType
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.player.Player
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW

enum class EatTrigger {
    KEY,    // Eat on key press
    HEALTH  // Automatically eat based on health level
}

class PvPSoup : ClientModInitializer {

    companion object {
        var modEnabled = true               // Global toggle switch
        var autoRefillEnabled = true
        var autoDropBowlsEnabled = true
        var autoEatEnabled = true
        var eatTrigger = EatTrigger.KEY      // Trigger mode (KEY or HEALTH)
        var healthThreshold = 14.0f          // Health threshold in HP (14.0f = 7 hearts)

        lateinit var eatSoupKey: KeyMapping
    }

    override fun onInitializeClient() {
        eatSoupKey = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "key.pvpsoup.eat",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.pvpsoup"
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            if (!modEnabled) return@EndTick

            val player = client.player ?: return@EndTick

            if (client.screen is InventoryScreen) {
                handleInventoryLogic(client)
            }

            if (autoDropBowlsEnabled && client.screen == null) {
                dropBowlsFromHotbar(client)
            }

            if (autoEatEnabled && client.screen == null) {
                val shouldEat = when (eatTrigger) {
                    EatTrigger.KEY -> eatSoupKey.consumeClick()
                    EatTrigger.HEALTH -> player.health <= healthThreshold && player.health < player.maxHealth
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
        val gameMode = client.gameMode ?: return

        val soupSlot = findSoupInHotbar(player)
        if (soupSlot != -1) {
            val previousSlot = player.inventory.selected

            // Switch to soup
            player.inventory.selected = soupSlot

            // Use item on client + send network packet
            gameMode.useItem(player, InteractionHand.MAIN_HAND)

            // Restore previous slot
            player.inventory.selected = previousSlot
        }
    }

    private fun handleInventoryLogic(client: Minecraft) {
        val player = client.player ?: return
        val gameMode = client.gameMode ?: return
        val container = player.containerMenu

        if (autoDropBowlsEnabled) {
            for (i in 9..44) {
                val stack = container.getSlot(i).item
                if (stack.`is`(Items.BOWL)) {
                    gameMode.handleInventoryMouseClick(container.containerId, i, 1, ClickType.THROW, player)
                }
            }
        }

        if (autoRefillEnabled) {
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
            literal("pvpsoup")
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
                            modEnabled = BoolArgumentType.getBool(ctx, "state")
                            ctx.source.sendFeedback(Component.literal("§a[PvPSoup] Mod ${if (modEnabled) "ENABLED" else "DISABLED"}"))
                            1
                        }))
                .then(literal("refill")
                    .then(argument("state", BoolArgumentType.bool())
                        .executes { ctx ->
                            autoRefillEnabled = BoolArgumentType.getBool(ctx, "state")
                            ctx.source.sendFeedback(Component.literal("§a[PvPSoup] Auto-refill: $autoRefillEnabled"))
                            1
                        }))
                .then(literal("dropbowls")
                    .then(argument("state", BoolArgumentType.bool())
                        .executes { ctx ->
                            autoDropBowlsEnabled = BoolArgumentType.getBool(ctx, "state")
                            ctx.source.sendFeedback(Component.literal("§a[PvPSoup] Auto-drop bowls: $autoDropBowlsEnabled"))
                            1
                        }))
                .then(literal("autoeat")
                    .then(argument("state", BoolArgumentType.bool())
                        .executes { ctx ->
                            autoEatEnabled = BoolArgumentType.getBool(ctx, "state")
                            ctx.source.sendFeedback(Component.literal("§a[PvPSoup] Auto-eat: $autoEatEnabled"))
                            1
                        }))
                .then(literal("trigger")
                    .then(argument("mode", StringArgumentType.word())
                        .executes { ctx ->
                            val modeStr = StringArgumentType.getString(ctx, "mode").uppercase()
                            try {
                                eatTrigger = EatTrigger.valueOf(modeStr)
                                ctx.source.sendFeedback(Component.literal("§a[PvPSoup] AutoEat trigger mode: $eatTrigger"))
                            } catch (e: Exception) {
                                ctx.source.sendFeedback(Component.literal("§c[PvPSoup] Invalid mode! Use: KEY or HEALTH"))
                            }
                            1
                        }))
                .then(literal("health")
                    .then(argument("hearts", FloatArgumentType.floatArg(1.0f, 20.0f))
                        .executes { ctx ->
                            val hearts = FloatArgumentType.getFloat(ctx, "hearts")
                            healthThreshold = hearts * 2.0f // Convert hearts to HP (1 heart = 2 HP)
                            ctx.source.sendFeedback(Component.literal("§a[PvPSoup] Health threshold for AutoEat set to: $hearts hearts ($healthThreshold HP)"))
                            1
                        }))
        )
    }

    private fun sendHelp(source: FabricClientCommandSource) {
        val helpText = """
            §e=== PvPSoup Help ===
            §b/pvpsoup toggle <true|false> §7- Toggle the entire mod on/off
            §b/pvpsoup refill <true|false> §7- Auto-refill hotbar with soup
            §b/pvpsoup dropbowls <true|false> §7- Auto-drop empty bowls
            §b/pvpsoup autoeat <true|false> §7- Enable/disable auto-eating soup
            §b/pvpsoup trigger <key|health> §7- AutoEat trigger mode (key press or low health)
            §b/pvpsoup health <hearts> §7- Health threshold for health mode (e.g., 7.0)
            §7* Keybinding can be changed in Minecraft Controls settings (default: R).
        """.trimIndent()
        source.sendFeedback(Component.literal(helpText))
    }
}
