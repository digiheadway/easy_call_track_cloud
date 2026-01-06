package com.clicktoearn.linkbox.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object MyAssets : Screen("my_assets", "My Assets", Icons.Default.Storage)
    object MyLinks : Screen("my_links", "My Links", Icons.Default.Link)
    object History : Screen("history", "History", Icons.Default.History)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object SharedContent : Screen("sharedContent/{token}?isNewInstall={isNewInstall}", "Shared Content", Icons.Default.Link) {
        fun createRoute(token: String, isNewInstall: Boolean = false) = "sharedContent/$token?isNewInstall=$isNewInstall"
    }
    object PageEditor : Screen("page_editor/{assetId}/{editMode}", "Page Editor", Icons.Default.Description) {
        fun createRoute(assetId: String, editMode: Boolean = false) = "page_editor/$assetId/$editMode"
    }
    object SharedFolder : Screen("shared_folder/{ownerId}/{folderId}/{title}?restrictScreenshot={restrictScreenshot}&restrictUrl={restrictUrl}", "Shared Folder", Icons.Default.Folder) {
        fun createRoute(ownerId: String, folderId: String, title: String, restrictScreenshot: Boolean = false, restrictUrl: Boolean = false) = 
            "shared_folder/$ownerId/$folderId/$title?restrictScreenshot=$restrictScreenshot&restrictUrl=$restrictUrl"
    }
    object SharedFile : Screen("shared_file/{ownerId}/{assetId}/{title}?restrictScreenshot={restrictScreenshot}", "Shared File", Icons.Default.Description) {
        fun createRoute(ownerId: String, assetId: String, title: String, restrictScreenshot: Boolean = false) = 
            "shared_file/$ownerId/$assetId/$title?restrictScreenshot=$restrictScreenshot"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.MyAssets,
    Screen.History,
    Screen.Profile
)
