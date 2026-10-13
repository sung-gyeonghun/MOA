package kr.moa.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class MoaFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun searchDetailFavoriteAndPersistence() {
        compose.onNodeWithText("찜", useUnmergedTree = true).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("마음에 모아둔 무대").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithText("뮤지컬 달의 정원").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("찜 해제").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("뮤지컬 달의 정원").fetchSemanticsNodes().isEmpty() }
        }
        compose.onNodeWithText("검색", useUnmergedTree = true).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("어떤 무대를 찾고 있나요?").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("기간 선택").performClick()
        compose.onNodeWithText("6개월").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("달의 정원")
        compose.onNodeWithText("검색 결과  1").assertExists()
        compose.onNodeWithText("뮤지컬 달의 정원").performClick()
        compose.onNodeWithText("공식 예매처로 이동").assertIsNotEnabled()
        compose.onNodeWithContentDescription("찜하기", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithText("찜", useUnmergedTree = true).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("뮤지컬 달의 정원").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { compose.onAllNodesWithText("찜", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("찜", useUnmergedTree = true).performClick()
        compose.onNodeWithText("뮤지컬 달의 정원").assertExists()
        compose.onNodeWithContentDescription("찜 해제").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("첫 번째 설렘을 모아보세요").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("첫 번째 설렘을 모아보세요").assertExists()
    }
    @Test fun unmatchedSearchShowsEmptyState() {
        compose.onNodeWithText("검색", useUnmergedTree = true).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("존재하지않는공연123")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitUntil(10000) { compose.onAllNodesWithText("찾는 공연이 없어요").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("찾는 공연이 없어요").assertExists()
    }

    @Test fun regionAndMultipleGenresApplyAndSurviveRecreation() {
        compose.onNodeWithText("필터").performClick()
        compose.onNodeWithText("초기화").performClick()
        compose.onAllNodesWithContentDescription("지역 선택").onLast().performClick()
        compose.onNodeWithText("대구").performClick()
        compose.onNodeWithText("서울").performClick()
        compose.onNodeWithText("선택 완료").performScrollTo().performClick()
        compose.onAllNodesWithContentDescription("장르 선택").onLast().performClick()
        compose.onNodeWithText("콘서트").performClick()
        compose.onAllNodesWithText("뮤지컬").onLast().performClick()
        compose.onNodeWithText("선택 완료").performScrollTo().performClick()
        compose.onNodeWithText("이 조건으로 찾아보기").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("서울, 대구에서 만나는", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { compose.onAllNodesWithText("서울, 대구에서 만나는", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("필터").performClick()
        compose.onNodeWithText("초기화").performClick()
        compose.onNodeWithText("이 조건으로 찾아보기").performClick()
    }
    @Test fun monthOnlyRangeAndSettingsHaveNoDataAddress() {
        compose.onNodeWithContentDescription("기간 선택").performClick()
        compose.onNodeWithText("직접 선택").performClick()
        compose.onNodeWithText("공연을 만날 기간").assertExists()
        compose.onNodeWithText("2월").performClick()
        compose.onNodeWithText("8월").performClick()
        compose.onNodeWithText("기간 적용").performClick()
        compose.onNodeWithText("2026년 2월 – 2026년 8월").assertExists()
        compose.onNodeWithText("설정", useUnmergedTree = true).performClick()
        compose.onNodeWithText("GitHub Pages 데이터 주소").assertDoesNotExist()
        compose.onNodeWithText("공연 데이터").assertDoesNotExist()
    }
}
