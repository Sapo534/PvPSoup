package net.example.pvpsoup.mixin.render;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.example.pvpsoup.event.SoundPlayCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(SoundInstance sound, CallbackInfo ci) {
        if (sound != null && sound.getId() != null) {
            boolean cancel = SoundPlayCallback.EVENT.invoker().onSoundPlay(sound);
            if (cancel) {
                ci.cancel();
            }
        }
    }
}
