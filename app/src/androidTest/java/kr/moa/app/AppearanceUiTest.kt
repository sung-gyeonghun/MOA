package kr.moa.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class AppearanceUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeHasNoSearchShortcutAndDarkModeSurvivesRecreation() {
        compose.onNodeWithText("공연명, 아티스트를 검색해보세요").assertDoesNotExist()
        compose.onNodeWithText("설정", useUnmergedTree = true).performClick()
        val toggle = compose.onNodeWithContentDescription("다크모드")
        toggle.assertIsOff().performClick()
        compose.waitUntil(10000) { compose.onAllNodes(isToggleable() and isOn()).fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("설정", useUnmergedTree = true).performClick()
        compose.waitUntil(10000) { compose.onAllNodes(isToggleable() and isOn()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("다크모드").assertIsOn().performClick()
        compose.waitUntil(10000) { compose.onAllNodes(isToggleable() and isOff()).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun navigationIsHiddenWhileKeyboardIsOpenAndReturnsAfterSubmit() {
        compose.onNodeWithText("검색", useUnmergedTree = true).performClick()
        compose.onNode(hasSetTextAction()).performClick().performTextInput("공연")
        compose.waitUntil(10000) { compose.onAllNodesWithText("홈", useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("홈", useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitUntil(10000) { compose.onAllNodesWithText("홈", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("홈", useUnmergedTree = true).assertIsDisplayed()
    }
}
