package com.example.biblia.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.biblia.domain.model.AppIcon

/**
 * Switches the launcher icon by enabling exactly one manifest activity-alias
 * (IconSunrise / IconNight / IconSky, all targeting MainActivity) and
 * disabling the others. DONT_KILL_APP keeps the running app alive; the
 * launcher may take a few seconds to refresh its icon.
 */
object AppIconManager {

    private fun component(context: Context, icon: AppIcon) =
        ComponentName(context.packageName, "$BASE.${icon.aliasName}")

    /** Applies [icon] if the system state differs (cheap no-op otherwise). */
    fun apply(context: Context, icon: AppIcon) {
        if (current(context) == icon) return
        val pm = context.packageManager
        // Enable the new entry FIRST so there is never a moment without a launcher entry.
        setEnabled(pm, component(context, icon), true)
        AppIcon.entries.filter { it != icon }.forEach { setEnabled(pm, component(context, it), false) }
    }

    /** The alias currently enabled (manifest default: SUNRISE). */
    fun current(context: Context): AppIcon {
        val pm = context.packageManager
        return AppIcon.entries.firstOrNull { icon ->
            when (pm.getComponentEnabledSetting(component(context, icon))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.SUNRISE
                else -> false
            }
        } ?: AppIcon.SUNRISE
    }

    private fun setEnabled(pm: PackageManager, name: ComponentName, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (pm.getComponentEnabledSetting(name) != state) {
            pm.setComponentEnabledSetting(name, state, PackageManager.DONT_KILL_APP)
        }
    }

    /** Aliases are declared relative to the namespace (".IconSunrise"). */
    private const val BASE = "com.example.biblia"
}
