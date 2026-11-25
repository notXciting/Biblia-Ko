package com.example.biblia.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.biblia.R

/** The Settings sub-pages, in menu order. The name is the `settings/{page}` route argument. */
enum class SettingsPage(val title: Int, val icon: ImageVector) {
    READING(R.string.settings_page_reading, Icons.AutoMirrored.Outlined.MenuBook),
    APPEARANCE(R.string.settings_page_appearance, Icons.Outlined.Palette),
    LISTEN(R.string.settings_listen, Icons.Outlined.Headphones),
    APP(R.string.settings_page_app, Icons.Outlined.PhoneAndroid),
    BACKUP(R.string.backup_title, Icons.Outlined.SettingsBackupRestore),
    ABOUT(R.string.settings_about, Icons.Outlined.Info),
    ;

    companion object {
        fun of(name: String?): SettingsPage = entries.firstOrNull { it.name == name } ?: READING
    }
}
