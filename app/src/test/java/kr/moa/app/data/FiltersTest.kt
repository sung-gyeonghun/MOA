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

    @Test fun multiRegionSelectionAndNationwideAreExclusive() {
        val f = Filters().toggleRegion("대구").toggleRegion("서울")
        assertEquals(setOf("대구", "서울"), f.effectiveRegions)
        assertEquals(2, filterPerformances(listOf(a,b), "", f, LocalDate.of(2026,10,13)).size)
        assertTrue(f.toggleRegion("전국").effectiveRegions.isEmpty())
        assertEquals(setOf("부산"), f.toggleRegion("전국").toggleRegion("부산").effectiveRegions)
    }
    @Test fun monthPresetsIncludeFullEndMonthAcrossYear() {
        val edge = b.copy(startDate = "2027-02-28", endDate = "2027-02-28")
        val outside = b.copy(id = "c", startDate = "2027-03-01", endDate = "2027-03-01")
        val today = LocalDate.of(2026,12,15)
        assertEquals(listOf(edge), filterPerformances(listOf(edge,outside), "", Filters(date = "3개월"), today))
        assertEquals(2, filterPerformances(listOf(edge,outside), "", Filters(date = "6개월"), today).size)
    }
    @Test fun customMonthRangeIncludesFirstAndLastDayAndSpanningPerformances() {
        val f = Filters(date = "직접 선택", from = LocalDate.of(2026,2,1), to = LocalDate.of(2026,8,31))
        val first = b.copy(id = "first", startDate = "2026-02-01", endDate = "2026-02-01")
        val last = b.copy(id = "last", startDate = "2026-08-31", endDate = "2026-08-31")
        val span = a.copy(startDate = "2026-01-01", endDate = "2026-09-01")
        assertEquals(setOf("first", "last", "a"), filterPerformances(listOf(a,first,last,span), "", f).map { it.id }.toSet())
    }
}
