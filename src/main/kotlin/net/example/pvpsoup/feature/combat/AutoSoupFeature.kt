package net.example.pvpsoup.feature.combat

import net.example.pvpsoup.config.ConfigManager
import net.example.pvpsoup.feature.Feature
import net.minecraft.client.MinecraftClient
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand

object AutoSoupFeature : Feature("AutoSoup", "Автоматически ест суп при низком здоровье") {

    override fun onTick() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        if (player.isDead) return

        // Явное получение Float через методы Fabric/Yarn
        val currentHealth = player.health + player.absorptionAmount
        val targetHealth = ConfigManager.config.autoSoupHealth

        if (currentHealth <= targetHealth) {
            val soupSlot = findSoupSlot() ?: return
            val oldSlot = player.inventory.selectedSlot

            if (soupSlot != oldSlot) {
                client.networkHandler?.sendPacket(UpdateSelectedSlotC2SPacket(soupSlot))
            }

            client.networkHandler?.sendPacket(PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, player.yaw, player.pitch))

            if (soupSlot != oldSlot) {
                client.networkHandler?.sendPacket(UpdateSelectedSlotC2SPacket(oldSlot))
            }
        }
    }

    private fun findSoupSlot(): Int? {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return null

        for (i in 0..8) {
            val stack = player.inventory.getStack(i)
            if (stack.item == Items.MUSHROOM_STEW || stack.item == Items.BEETROOT_SOUP || stack.item == Items.RABBIT_STEW) {
                return i
            }
        }
        return null
    }
}
