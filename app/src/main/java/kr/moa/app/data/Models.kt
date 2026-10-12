package kr.moa.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Performance(
    val id: String, val title: String, val genre: String, val region: String,
    val venue: String, val startDate: String, val endDate: String,
    val posterUrl: String = "", val artists: List<String> = emptyList(),
    val status: String = "공연예정", val ticketPrice: String = "", val bookingUrls: List<Booking> = emptyList(),
    val lastUpdated: String = "", val runtime: String = "", val age: String = "", val schedule: String = "",
    val description: String = "", val artwork: String = "", val festival: Boolean = false
)
@Serializable data class Booking(val name: String, val url: String)
@Serializable data class Manifest(val version: String, val updatedAt: String, val files: List<String>, val source: String = "KOPIS", val count: Int = 0)
@Serializable data class Catalog(val updatedAt: String, val performances: List<Performance>, val sample: Boolean = false)

val genres = listOf("콘서트", "뮤지컬", "연극", "클래식", "국악", "무용", "서커스/마술", "복합", "축제")
val regions = listOf("전국", "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주")

data class Filters(val region: String = "전국", val genres: Set<String> = emptySet(), val date: String = "전체 일정", val from: LocalDate? = null, val to: LocalDate? = null, val descending: Boolean = false)

fun filterPerformances(items: List<Performance>, query: String, filters: Filters, today: LocalDate = LocalDate.now()): List<Performance> {
    val range = when (filters.date) {
        "오늘" -> today to today
        "이번 주" -> today.with(java.time.DayOfWeek.MONDAY) to today.with(java.time.DayOfWeek.SUNDAY)
        "이번 주말" -> today.with(java.time.DayOfWeek.SATURDAY) to today.with(java.time.DayOfWeek.SUNDAY)
        "이번 달" -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        "직접 선택" -> (filters.from ?: today) to (filters.to ?: filters.from ?: today)
        else -> null
    }
    val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return items.filter { p ->
        (filters.region == "전국" || p.region == filters.region) &&
        (filters.genres.isEmpty() || p.genre in filters.genres || ("축제" in filters.genres && p.festival)) &&
        words.all { word -> (p.title + " " + p.artists.joinToString(" ")).contains(word, ignoreCase = true) } &&
        (range == null || runCatching { LocalDate.parse(p.startDate) <= range.second && LocalDate.parse(p.endDate) >= range.first }.getOrDefault(false))
    }.let { if (filters.descending) it.sortedBy { p -> p.endDate } else it.sortedBy { p -> p.startDate } }
}
