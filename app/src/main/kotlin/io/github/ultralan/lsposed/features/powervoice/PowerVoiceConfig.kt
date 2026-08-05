package io.github.ultralan.lsposed.features.powervoice

object PowerVoiceConfig {
    const val DOUBAO_PACKAGE = "com.larus.nova"
    const val DOUBAO_LAUNCHER_ACTIVITY = "com.larus.home.impl.alias.AliasActivity3"
    const val DOUBAO_REALTIME_CALL_ACTIVITY = "com.larus.voicecall.impl.ui.RealtimeCallActivity"

    const val EXTRA_TRIGGER_DOUBAO_VOICE_CALL =
        "io.github.ultralan.lsposed.powervoice.TRIGGER_DOUBAO_VOICE_CALL"
    const val EXTRA_TRIGGER_SOURCE = "io.github.ultralan.lsposed.powervoice.TRIGGER_SOURCE"
    const val TRIGGER_SOURCE_POWER_LONG_PRESS = "power_long_press"

    const val PREFS_NAME = "power_voice"
    const val PREF_KEY_LAST_VOICE_CALL_INTENT_URI = "last_voice_call_intent_uri"
    const val PREF_KEY_TARGET_PROVIDER_ID = "target_provider_id"
}
