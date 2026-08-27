package net.example.pvpsoup.feature.combat

import net.example.pvpsoup.config.ConfigManager
import net.example.pvpsoup.feature.Feature
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object AutoSoupFeature : Feature("AutoSoup", "Пакетный автосуп и мгновенный выброс мисок") {

    private var lastSoupTime: Long = 0L

    override fun onTick() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val networkHandler = client.networkHandler ?: return
        val now = System.currentTimeMillis()

        if (player.isDead) return

        // 1. Пополнение хотбара супами из инвентаря (когда открыт GUI инвентаря)
        if (client.currentScreen is InventoryScreen) {
            refillHotbar()
            return
        }

        // 2. Выбрасываем все миски из хотбара пакетом
        dropHotbarBowls()

        // 3. Проверка задержки перед выпиванием нового супа
        if (now - lastSoupTime < ConfigManager.config.autoSoupDelayMs) {
            return
        }

        // 4. Проверка уровня здоровья и использование супа
        val currentHealth = player.health + player.absorptionAmount
        if (currentHealth <= ConfigManager.config.autoSoupHealth) {
            val soupSlot = findSoupSlotInHotbar() ?: return
            val oldSlot = player.inventory.selectedSlot

            // Переключаемся на слот с супом
            if (soupSlot != oldSlot) {
                networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(soupSlot))
            }

            // Использование предмета
            networkHandler.sendPacket(PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, player.yaw, player.pitch))

            // Мгновенный выброс пустой миски прямо из руки через пакет действий
            networkHandler.sendPacket(
                PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
                    BlockPos.ORIGIN,
                    Direction.DOWN
                )
            )

            // Возвращаемся на исходный слот
            if (soupSlot != oldSlot) {
                networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(oldSlot))
            }

            lastSoupTime = now
        }
    }

    private fun dropHotbarBowls() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val networkHandler = client.networkHandler ?: return
        val oldSlot = player.inventory.selectedSlot

        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item == Items.BOWL) {
                if (i != oldSlot) {
                    networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(i))
                }

                // Выбрасываем миску из хотбара без задержек
                networkHandler.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                    )
                )

                if (i != oldSlot) {
                    networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(oldSlot))
                }
            }
        }
    }

    private fun refillHotbar() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val interactionManager = client.interactionManager ?: return

        for (hotbarSlot in 0..8) {
            if (player.inventory.getStack(hotbarSlot).isEmpty) {
                val invSoupSlot = findSoupInInventory() ?: break
                interactionManager.clickSlot(0, invSoupSlot, 0, SlotActionType.QUICK_MOVE, player)
                break
            }
        }
    }

    private fun findSoupSlotInHotbar(): Int? {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return null

        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (isSoup(stack.item)) {
                return i
            }
        }
        return null
    }

    private fun findSoupInInventory(): Int? {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return null

        for (i in 9..35) {
            val stack = player.inventory.getStack(i)
            if (isSoup(stack.item)) {
                return i
            }
        }
        return null
    }

    private fun isSoup(item: net.minecraft.item.Item): Boolean {
        return item == Items.MUSHROOM_STEW || item == Items.BEETROOT_SOUP || item == Items.RABBIT_STEW
    }
}
