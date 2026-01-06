package com.clicktoearn.linkbox

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigationTabs() {
        // Check if "My" tab is displayed (starting screen)
        composeTestRule.onNodeWithText("My Links").assertExists()

        // Navigate to Joined
        composeTestRule.onNodeWithText("Joined").performClick()
        composeTestRule.onNodeWithText("No joined circles or links yet").assertExists()

        // Navigate to Earn
        composeTestRule.onNodeWithText("Earn").performClick()
        composeTestRule.onNodeWithText("Total Estimated Earnings").assertExists()

        // Navigate to Settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("App Settings").assertExists()
    }
}
