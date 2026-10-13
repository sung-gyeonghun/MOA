package kr.moa.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kr.moa.app.data.*
import java.time.YearMonth

@Composable fun SelectionNote(value: String) {
    Text(value, color = Muted, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 7.dp, start = 4.dp))
}

@Composable private fun MenuField(label: String, description: String, modifier: Modifier, pill: Boolean = false, content: @Composable (close: () -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(onClick = { expanded = true }, modifier = (if (pill) Modifier else Modifier.fillMaxWidth()).semantics { contentDescription = description }, color = Color.White, shape = if (pill) RoundedCornerShape(50) else RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Lavender)) {
            Row(Modifier.heightIn(min = 48.dp).padding(horizontal = if (pill) 12.dp else 14.dp, vertical = if (pill) 10.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (pill) { Icon(Icons.Outlined.LocationOn, null, tint = Purple, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)) }
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = !pill))
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = if (pill) Ink else Purple, modifier = Modifier.size(if (pill) 18.dp else 20.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.widthIn(min = 210.dp, max = 300.dp), containerColor = Color.White) {
            content { expanded = false }
        }
    }
}

@Composable private fun Choice(label: String, checked: Boolean, click: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) }, onClick = click,
        modifier = Modifier.semantics { selected = checked; stateDescription = if (checked) "선택됨" else "선택 안 됨" })
}

@Composable fun RegionMenu(filters: Filters, change: (Filters) -> Unit, modifier: Modifier = Modifier, pill: Boolean = false) {
    val selection = filters.effectiveRegions
    val label = if (selection.size > 1) "${regions.first { it in selection }} 외 ${selection.size - 1}곳" else filters.regionLabel
    MenuField(label, "지역 선택", modifier, pill) { close ->
        regions.forEach { value -> Choice(value, if (value == "전국") selection.isEmpty() else value in selection) { change(filters.toggleRegion(value)) } }
        DropdownMenuItem(text = { Text("선택 완료", color = Purple, fontWeight = FontWeight.Bold) }, onClick = close)
    }
}

@Composable fun GenreMenu(filters: Filters, change: (Filters) -> Unit, modifier: Modifier = Modifier) {
    val label = if (filters.genres.isEmpty()) "전체 장르" else if (filters.genres.size == 1) filters.genres.first() else "${genres.first { it in filters.genres }} 외 ${filters.genres.size - 1}개"
    MenuField(label, "장르 선택", modifier) { close ->
        Choice("전체", filters.genres.isEmpty()) { change(filters.copy(genres = emptySet())) }
        genres.forEach { value -> Choice(value, value in filters.genres) { change(filters.copy(genres = if (value in filters.genres) filters.genres - value else filters.genres + value)) } }
        DropdownMenuItem(text = { Text("선택 완료", color = Purple, fontWeight = FontWeight.Bold) }, onClick = close)
    }
}

@Composable fun PeriodMenu(filters: Filters, change: (Filters) -> Unit, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf(false) }
    MenuField(filters.date, "기간 선택", modifier) { close ->
        listOf("이번 달", "3개월", "6개월", "직접 선택").forEach { value ->
            DropdownMenuItem(text = { Text(value) }, trailingIcon = { if (filters.date == value) Icon(Icons.Rounded.Check, null, tint = Purple) }, onClick = {
                close()
                if (value == "직접 선택") picking = true else change(filters.copy(date = value, from = null, to = null))
            })
        }
    }
    if (picking) MonthRangeDialog(filters, { picking = false }) { start, end ->
        change(filters.copy(date = "직접 선택", from = start.atDay(1), to = end.atEndOfMonth()))
        picking = false
    }
}

@Composable fun DiscoveryMenus(filters: Filters, change: (Filters) -> Unit) {
    Column(Modifier.padding(horizontal = 22.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GenreMenu(filters, change, Modifier.weight(1f))
            PeriodMenu(filters, change, Modifier.weight(1f))
        }
        if (filters.date == "직접 선택") SelectionNote(filters.periodLabel)
    }
}

@Composable fun MonthRangeDialog(filters: Filters, dismiss: () -> Unit, apply: (YearMonth, YearMonth) -> Unit) {
    val now = YearMonth.now()
    var start by remember { mutableStateOf(filters.from?.let { YearMonth.from(it) } ?: now) }
    var end by remember { mutableStateOf(filters.to?.let { YearMonth.from(it) } ?: now) }
    var editingEnd by remember { mutableStateOf(false) }
    var year by remember { mutableIntStateOf(start.year) }
    AlertDialog(onDismissRequest = dismiss, containerColor = Color.White,
        title = { Text("공연을 만날 기간", fontSize = 21.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Stack the two fields so large fonts never squeeze a date into a narrow column.
                MonthEndpoint("시작 월", start, !editingEnd) { editingEnd = false; year = start.year }
                MonthEndpoint("종료 월", end, editingEnd) { editingEnd = true; year = end.year }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { if (year > 1900) year-- }, enabled = year > 1900) { Icon(Icons.Rounded.ChevronLeft, "이전 연도") }
                    Text("${year}년", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { if (year < 2100) year++ }, enabled = year < 2100) { Icon(Icons.Rounded.ChevronRight, "다음 연도") }
                }
                (1..12).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { month ->
                            val value = YearMonth.of(year, month)
                            val chosen = value == if (editingEnd) end else start
                            Surface(onClick = {
                                if (editingEnd) { end = value; if (end < start) start = end }
                                else { start = value; if (end < start) end = start; editingEnd = true; year = end.year }
                            }, modifier = Modifier.weight(1f).semantics { selected = chosen }, shape = RoundedCornerShape(12.dp),
                                color = if (chosen) Purple else Color.White, contentColor = if (chosen) Color.White else Ink, border = BorderStroke(1.dp, if (chosen) Purple else Lavender)) {
                                Box(Modifier.heightIn(min = 48.dp).padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text("${month}월", fontSize = 14.sp) }
                            }
                        }
                    }
                }
                Text("시작 월 1일부터 종료 월 마지막 날까지 조회해요.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
            }
        }, confirmButton = { TextButton(onClick = { apply(start, end) }) { Text("기간 적용") } }, dismissButton = { TextButton(onClick = dismiss) { Text("취소") } })
}

@Composable private fun MonthEndpoint(label: String, value: YearMonth, active: Boolean, click: () -> Unit) {
    Surface(onClick = click, color = if (active) Lavender else Color.White, border = BorderStroke(1.dp, if (active) Purple else Lavender), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, fontSize = 11.sp, color = Muted)
            Text("${value.year}년 ${value.monthValue}월", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
