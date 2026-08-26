package net.example.pvpsoup

import com.mojang.brigadier.CommandDispatcher
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

class PvPSoup : ClientModInitializer {

    companion object {
        var autoRefillEnabled = true
        var autoDropBowlsEnabled = true
        var autoEatEnabled = true

        lateinit var eatSoupKey: KeyMapping
        private var previousSlot = -1
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
            val player = client.player ?: return@EndTick
            val gameMode = client.gameMode ?: return@EndTick

            if (client.screen is InventoryScreen) {
                handleInventoryLogic(client)
            }

            if (autoDropBowlsEnabled && client.screen == null) {
                dropBowlsFromHotbar(client)
            }

            if (autoEatEnabled && eatSoupKey.consumeClick()) {
                val soupSlot = findSoupInHotbar(player)
                if (soupSlot != -1) {
                    previousSlot = player.inventory.selected
                    player.inventory.selected = soupSlot

                    gameMode.useItem(player, InteractionHand.MAIN_HAND)

                    player.inventory.selected = previousSlot
                }
            }
        })

        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerCommands(dispatcher)
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
                .then(literal("refill")
                    .then(argument("state", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                        .executes { ctx ->
                            autoRefillEnabled = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "state")
                            ctx.source.sendFeedback(Component.literal("§a[PvPSoup] Автопополнение: $autoRefillEnabled"))
                            1
                        }))
                .then(literal("dropbowls")
                    .then(argument("state", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                        .executes { ctx ->
                            autoDropBowlsEnabled = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "state")
                            ctx.source.sendFeedback(Component.literal("§a[PvPSoup] Авто-выброс мисок: $autoDropBowlsEnabled"))
                            1
                        }))
                .then(literal("autoeat")
                    .then(argument("state", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                        .executes { ctx ->
                            autoEatEnabled = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "state")
                            ctx.source.sendFeedback(Component.literal("§a[PvPSoup] Использование супов по бинду: $autoEatEnabled"))
                            1
                        }))
        )
    }

    private fun sendHelp(source: FabricClientCommandSource) {
        val helpText = """
            §e=== PvPSoup Help ===
            §b/pvpsoup help §7- Показать эту справку
            §b/pvpsoup refill <true|false> §7- Автопополнение хотбара супами
            §b/pvpsoup dropbowls <true|false> §7- Авто-выбрасывание пустых мисок
            §b/pvpsoup autoeat <true|false> §7- Включить/выключить быстрое поедание супа по бинду
            §7* Бинд меняется в Настройках Управления Minecraft (по умолчанию: R).
        """.trimIndent()
        source.sendFeedback(Component.literal(helpText))
    }
}
