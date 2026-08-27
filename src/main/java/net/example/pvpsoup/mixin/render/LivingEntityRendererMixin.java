package net.example.pvpsoup.mixin.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.example.pvpsoup.config.ConfigManager;
import net.example.pvpsoup.util.ColorUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow protected M model;

    @Inject(
        method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRenderModelAfter(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        if (livingEntity instanceof AbstractClientPlayerEntity && ConfigManager.isModuleEnabled("Chams")) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) livingEntity;
            MinecraftClient client = MinecraftClient.getInstance();

            // Игнорируем собственного игрока от 1-го лица
            if (player == client.player && client.options.getPerspective().isFirstPerson()) {
                return;
            }

            // Текстура скина игрока
            Identifier playerTexture = player.getSkinTextures().texture();

            // Расчет ARGB цвета (Trans, LGBT, Rainbow и т.д.)
            int argbColor = ColorUtils.getColorArgb(
                ConfigManager.getConfig().getChamsMode(),
                new Color(ConfigManager.getConfig().getChamsStaticRgb()),
                (float) (player.getY() / 128.0),
                ConfigManager.getConfig().getChamsAlpha()
            );

            // Безопасно открываем второй буфер с ванильным слоем контура сквозь стены
            RenderLayer outlineLayer = RenderLayer.getOutline(playerTexture);
            VertexConsumer buffer = vertexConsumerProvider.getBuffer(outlineLayer);

            // Отрисовываем цветной силуэт Chams поверх правильной матрицы
            this.model.render(matrixStack, buffer, light, OverlayTexture.DEFAULT_UV, argbColor);
        }
    }
}
