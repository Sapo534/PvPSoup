package net.example.pvpsoup.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.sound.SoundInstance;

@FunctionalInterface
public interface SoundPlayCallback {

    Event<SoundPlayCallback> EVENT = EventFactory.createArrayBacked(SoundPlayCallback.class, listeners -> sound -> {
        for (SoundPlayCallback listener : listeners) {
            if (listener.onSoundPlay(sound)) {
                return true;
            }
        }
        return false;
    });

    boolean onSoundPlay(SoundInstance sound);
}
