package com.example.data

enum class ClapSensitivity(val thresholdMultiplier: Float, val label: String) {
    LOW(1.8f, "Low (Needs louder claps)"),
    MEDIUM(1.3f, "Medium (Recommended)"),
    HIGH(0.9f, "High (Sensitive to quieter claps)")
}

enum class AlarmSoundType(val title: String) {
    SIREN("Police Siren"),
    RADAR("Radar Beacon"),
    CHIME("Urgent Chime"),
    ALARM_BELL("Classic Alarm")
}

data class AppSettings(
    val voiceEnabled: Boolean = false,
    val clapEnabled: Boolean = true,
    val voicePhrase: String = "find my phone",
    val clapSensitivity: ClapSensitivity = ClapSensitivity.MEDIUM,
    val clapCount: Int = 2,
    val clapCooldownSeconds: Int = 4,
    val alarmVolume: Float = 0.95f,
    val vibrationEnabled: Boolean = true,
    val flashlightEnabled: Boolean = true,
    val alarmSoundType: AlarmSoundType = AlarmSoundType.SIREN,
    val appLockEnabled: Boolean = false,
    val appPin: String = "",
    val biometricEnabled: Boolean = false,
    val autoStartOnBoot: Boolean = false
)
