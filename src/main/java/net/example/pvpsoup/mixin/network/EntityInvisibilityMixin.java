package net.example.pvpsoup.mixin.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.example.pvpsoup.config.ConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityInvisibilityMixin {

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvisibleTo(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (ConfigManager.isModuleEnabled("AntiInvisibility")) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void onIsInvisible(CallbackInfoReturnable<Boolean> cir) {
        if (ConfigManager.isModuleEnabled("AntiInvisibility")) {
            cir.setReturnValue(false);
        }
    }
}
