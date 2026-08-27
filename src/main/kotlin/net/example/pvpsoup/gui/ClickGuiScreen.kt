package net.example.pvpsoup.gui

import net.example.pvpsoup.config.ConfigManager
import net.example.pvpsoup.feature.FeatureManager
import net.example.pvpsoup.util.ColorMode
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class ClickGuiScreen : Screen(Text.literal("SoupClient ClickGUI")) {

    private val COLOR_HEADER = 0xFF4A2E18.toInt()
    private val COLOR_PANEL = 0xFF5C3A21.toInt()
    private val COLOR_BUTTON_OFF = 0xFF3D2515.toInt()
    private val COLOR_BUTTON_ON = 0xFFB88655.toInt()
    private val COLOR_HOVER = 0xFF8B5A2B.toInt()
    private val COLOR_TEXT = 0xFFA28263.toInt()

    private var panelX = 100
    private var panelY = 50
    private val panelWidth = 200
    private val buttonHeight = 32

    private var isDragging = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    override fun render(drawContext: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(drawContext, mouseX, mouseY, delta)

        if (isDragging) {
            panelX = mouseX - dragOffsetX
            panelY = mouseY - dragOffsetY
        }

        drawContext.fill(0, 0, this.width, this.height, 0x60000000.toInt())

        val features = FeatureManager.getAllFeatures().toList()
        val totalHeight = 30 + features.size * buttonHeight

        // Шапка
        drawContext.fill(panelX, panelY, panelX + panelWidth, panelY + 28, COLOR_HEADER)
        drawContext.drawText(
            textRenderer,
            "§lSoupClient §7| Click [LMB/RMB]",
            panelX + 10,
            panelY + 10,
            0xFFFFFFFF.toInt(),
            true
        )

        // Панель
        drawContext.fill(panelX, panelY + 28, panelX + panelWidth, panelY + totalHeight, COLOR_PANEL)

        var currentY = panelY + 32
        for (feature in features) {
            val isHovered = mouseX >= panelX + 5 && mouseX <= panelX + panelWidth - 5 &&
                    mouseY >= currentY && mouseY <= currentY + buttonHeight - 4

            val buttonColor = when {
                feature.isEnabled -> COLOR_BUTTON_ON
                isHovered -> COLOR_HOVER
                else -> COLOR_BUTTON_OFF
            }

            drawContext.fill(
                panelX + 5,
                currentY,
                panelX + panelWidth - 5,
                currentY + buttonHeight - 4,
                buttonColor
            )

            val statusText = if (feature.isEnabled) "§f[ВКЛ]" else "§7[ВЫКЛ]"
            val textColor = if (feature.isEnabled) 0xFFFFFFFF.toInt() else COLOR_TEXT

            // Название модуля
            drawContext.drawText(
                textRenderer,
                "${feature.name} $statusText",
                panelX + 10,
                currentY + 4,
                textColor,
                true
            )

            // Дополнительные параметры модуля (подстрока)
            val subText = when (feature.name) {
                "Chams" -> "§7Mode: §e${ConfigManager.config.chamsMode} §7| Alpha: §e${(ConfigManager.config.chamsAlpha * 100).toInt()}%"
                "AutoSoup" -> "§7Health: §e${ConfigManager.config.autoSoupHealth}HP §7| Delay: §e${ConfigManager.config.autoSoupDelayMs}ms"
                else -> "§7${feature.description}"
            }

            drawContext.drawText(
                textRenderer,
                subText,
                panelX + 10,
                currentY + 16,
                0xFFBBBBBB.toInt(),
                false
            )

            currentY += buttonHeight
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
            mouseY >= panelY && mouseY <= panelY + 28 && button == 0
        ) {
            isDragging = true
            dragOffsetX = (mouseX - panelX).toInt()
            dragOffsetY = (mouseY - panelY).toInt()
            return true
        }

        val features = FeatureManager.getAllFeatures().toList()
        var currentY = panelY + 32

        for (feature in features) {
            if (mouseX >= panelX + 5 && mouseX <= panelX + panelWidth - 5 &&
                mouseY >= currentY && mouseY <= currentY + buttonHeight - 4
            ) {
                if (button == 0) { // ЛКМ — Переключить ВКЛ/ВЫКЛ
                    feature.toggle()
                } else if (button == 1) { // ПКМ — Переключить настройки
                    when (feature.name) {
                        "Chams" -> cycleChamsMode()
                        "AutoSoup" -> cycleAutoSoupHealth()
                    }
                }
                return true
            }
            currentY += buttonHeight
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun cycleChamsMode() {
        val modes = ColorMode.entries
        val nextOrdinal = (ConfigManager.config.chamsMode.ordinal + 1) % modes.size
        ConfigManager.config.chamsMode = modes[nextOrdinal]
        ConfigManager.save()
    }

    private fun cycleAutoSoupHealth() {
        ConfigManager.config.autoSoupHealth = when (ConfigManager.config.autoSoupHealth) {
            10.0f -> 13.0f
            13.0f -> 16.0f
            16.0f -> 18.0f
            else -> 10.0f
        }
        ConfigManager.save()
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) isDragging = false
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun shouldPause(): Boolean = false
}
