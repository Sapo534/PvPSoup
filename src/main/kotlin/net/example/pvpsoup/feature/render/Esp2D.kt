package net.example.pvpsoup.feature.render

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.example.pvpsoup.config.ConfigManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object Esp2DRenderer {

    fun onRenderWorld(context: WorldRenderContext) {
        if (!ConfigManager.isModuleEnabled("Esp2D")) return

        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        val player = client.player ?: return
        val cameraPos = context.camera()?.pos ?: return

        val matrixStack = context.matrixStack() ?: return
        val consumers = context.consumers() ?: return

        val tickDelta = context.tickCounter().getTickDelta(true)

        for (entity in world.entities) {
            if (entity !is LivingEntity || entity == player || !entity.isAlive) continue

            // Плавная интерполяция позиции сущности
            val x = entity.lastRenderX + (entity.x - entity.lastRenderX) * tickDelta - cameraPos.x
            val y = entity.lastRenderY + (entity.y - entity.lastRenderY) * tickDelta - cameraPos.y
            val z = entity.lastRenderZ + (entity.z - entity.lastRenderZ) * tickDelta - cameraPos.z

            val boundingBox = entity.boundingBox
            val renderBox = Box(
                boundingBox.minX - entity.x + x,
                boundingBox.minY - entity.y + y,
                boundingBox.minZ - entity.z + z,
                boundingBox.maxX - entity.x + x,
                boundingBox.maxY - entity.y + y,
                boundingBox.maxZ - entity.z + z
            )

            // Используем стандартный линийный буфер Fabric/Vanilla
            val buffer = consumers.getBuffer(RenderLayer.getLines())

            // Зеленый цвет прямоугольника (R, G, B, Alpha)
            WorldRenderer.drawBox(
                matrixStack,
                buffer,
                renderBox,
                0.0f, 1.0f, 0.0f, 1.0f
            )
        }
    }
}
