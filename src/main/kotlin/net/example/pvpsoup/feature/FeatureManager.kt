package net.example.pvpsoup.feature

import net.example.pvpsoup.feature.combat.AutoSoupFeature

object FeatureManager {
    private val features = mutableMapOf<String, Feature>()

    fun init() {
        register(AntiInvisibilityFeature)
        register(Esp2DFeature)
        register(ChamsFeature)
        register(AutoSoupFeature)
        register(ChatMuteFeature)
        register(AntiWitherFeature)
    }

    private fun register(feature: Feature) {
        features[feature.name.lowercase()] = feature
    }

    fun getFeature(name: String): Feature? = features[name.lowercase()]

    fun getAllFeatures(): Collection<Feature> = features.values

    fun onTick() {
        for (feature in features.values) {
            if (feature.isEnabled) {
                feature.onTick()
            }
        }
    }
}
