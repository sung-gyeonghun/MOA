package kr.moa.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kr.moa.app.data.Booking
import kr.moa.app.data.Performance
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BookingUiTest {
    @get:Rule val compose = createComposeRule()
    private val performance = Performance("PF123456", "테스트 공연", "콘서트", "대구", "테스트 공연장", "2026-10-13", "2026-10-14", bookingUrls = listOf(Booking("공식 예매처 A", "https://example.com/ticket/123"), Booking("공식 예매처 B", "https://example.org/ticket/123")))
    @Test fun selectedOfficialBookingLinkIsPassedUnchangedToBrowserHandler() {
        var opened: String? = null
        val handler = object : UriHandler { override fun openUri(uri: String) { opened = uri } }
        compose.setContent { MaterialTheme { CompositionLocalProvider(LocalUriHandler provides handler) { DetailScreen(performance, false, false, {}, {}) } } }
        compose.onNodeWithText("공식 예매처로 이동").assertIsEnabled().performClick()
        compose.onNodeWithText("공식 예매처 B").performClick()
        compose.runOnIdle { assertEquals("https://example.org/ticket/123", opened) }
    }
    @Test fun unavailableBrowserShowsErrorInsteadOfCrashing() {
        val handler = object : UriHandler { override fun openUri(uri: String) { throw IllegalArgumentException("No browser available") } }
        compose.setContent { MaterialTheme { CompositionLocalProvider(LocalUriHandler provides handler) { DetailScreen(performance, false, false, {}, {}) } } }
        compose.onNodeWithText("공식 예매처로 이동").performClick()
        compose.onNodeWithText("공식 예매처 A").performClick()
        compose.onNodeWithText("링크를 열 수 없어요").assertExists()
    }
}
