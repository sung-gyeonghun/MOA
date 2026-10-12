package kr.moa.app.data
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FiltersTest {
    @Test fun datesAcrossYearsIncludeBothYears() { assertEquals("2026.12.20 — 2027.01.10", dateLabel(Performance("a", "공연", "뮤지컬", "서울", "극장", "2026-12-20", "2027-01-10"))) }
    private val a = Performance("a", "달의 정원", "뮤지컬", "대구", "극장", "2026-09-01", "2026-11-01", artists = listOf("김모아"))
    private val b = Performance("b", "별빛", "콘서트", "서울", "무대", "2026-10-17", "2026-10-18")
    @Test fun spanningPerformanceIncluded() { assertEquals(listOf(a), filterPerformances(listOf(a), "", Filters(date = "오늘"), LocalDate.of(2026,10,13))) }
    @Test fun artistSearchAndRegionIntersection() { assertEquals(listOf(a), filterPerformances(listOf(a,b), "김모아", Filters(region = "대구"))); assertTrue(filterPerformances(listOf(a), "김모아", Filters(region = "서울")).isEmpty()) }
    @Test fun multipleGenresUseOr() { assertEquals(2, filterPerformances(listOf(a,b), "", Filters(genres = setOf("뮤지컬", "콘서트"))).size) }
    @Test fun endSortAndCustomRange() { assertEquals(listOf(b,a), filterPerformances(listOf(a,b), "", Filters(descending = true))); assertEquals(listOf(a,b), filterPerformances(listOf(a,b), "", Filters(date = "직접 선택", from = LocalDate.of(2026,10,18), to = LocalDate.of(2026,10,20)))) }
}
