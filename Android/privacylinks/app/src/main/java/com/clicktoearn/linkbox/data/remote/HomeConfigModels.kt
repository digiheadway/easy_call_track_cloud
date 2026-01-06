package com.clicktoearn.linkbox.data.remote

data class HomeLayoutConfig(
    val items: List<HomeLayoutItem>
)

data class HomeLayoutItem(
    val type: String, // "section", "ad_native", "ad_native_video", "ad_banner"
    val id: String,   // "supers", "apps", "games", "content" (for sections)
    val title: String? = null, // Optional title override
    val visible: Boolean = true
)

// UI Model for the Screen
sealed class HomeRenderItem {
    data class Section(val data: com.clicktoearn.linkbox.ui.screens.HomeSection) : HomeRenderItem()
    data class Ad(val type: String) : HomeRenderItem()
}
