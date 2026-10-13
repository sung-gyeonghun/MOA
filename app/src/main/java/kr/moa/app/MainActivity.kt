package kr.moa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import kr.moa.app.data.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent {
        val vm: MoaViewModel = viewModel()
        val prefs by vm.preferences.collectAsStateWithLifecycle()
        MoaTheme(prefs.darkMode) { MoaApp(vm) }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MoaApp(vm: MoaViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var filterOpen by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.message.value = null } }
    val keyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    val open: (Performance) -> Unit = { nav.navigate("detail/${it.id}") }
    Scaffold( containerColor = Canvas, snackbarHost = { SnackbarHost(snack) }, bottomBar = {
        if (!route.startsWith("detail") && !keyboardVisible) Surface(color = MoaSurface, shadowElevation = 8.dp) {
            NavigationBar(containerColor = MoaSurface, tonalElevation = 0.dp) {
                listOf(Triple("home", "홈", Icons.Outlined.Home), Triple("search", "검색", Icons.Outlined.Search), Triple("favorites", "찜", Icons.Outlined.FavoriteBorder), Triple("settings", "설정", Icons.Outlined.Tune)).forEach { (r, label, icon) ->
                    NavigationBarItem(selected = route == r, onClick = { nav.navigate(r) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(icon, label) }, label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Purple, selectedTextColor = Purple, indicatorColor = Lavender, unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }
    }) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding()) {
            composable("home") { HomeScreen(catalog, filters, favorites, busy, { filterOpen = true }, { vm.defaults(it) }, vm::favorite, open, vm::refresh) }
            composable("search") { SearchScreen(catalog, filters, query, prefs.history, favorites, { vm.query.value = it }, vm::search, vm::clearHistory, { filterOpen = true }, { vm.defaults(it) }, vm::favorite, open) }
            composable("favorites") { FavoritesScreen(favorites, vm::favorite, open) }
            composable("settings") { SettingsScreen(filters, prefs.darkMode, vm::darkMode, { filterOpen = true }) }
            composable("detail/{id}") { back -> val id = back.arguments?.getString("id"); val p = catalog?.performances?.find { it.id == id } ?: favorites.find { it.id == id }; if (p != null) DetailScreen(p, favorites.any { it.id == p.id }, catalog?.sample == true || p.id.startsWith("DEMO"), { nav.popBackStack() }, { vm.favorite(p) }) else EmptyState("공연 정보를 찾지 못했어요", "목록으로 돌아가 다시 선택해주세요.") }
        }
    }
    if (filterOpen) FilterSheet(filters, { filterOpen = false }) { vm.defaults(it); filterOpen = false }
}

@Composable fun BrandHeader(region: String, onRegion: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Image(painterResource(R.drawable.app_logo), "모아", Modifier.width(82.dp).height(42.dp), contentScale = ContentScale.Fit)
        Surface(onClick = onRegion, shape = RoundedCornerShape(50), color = MoaSurface, border = BorderStroke(1.dp, Lavender)) { Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocationOn, null, tint = Purple, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(region, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Icon(Icons.Rounded.KeyboardArrowDown, null, modifier = Modifier.size(18.dp)) } }
    }
}
@Composable fun SectionTitle(title: String, subtitle: String = "", action: String = "", onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp); if (subtitle.isNotEmpty()) Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) }; if (action.isNotEmpty()) TextButton(onClick = onAction) { Text(action, fontSize = 12.sp) } }
}
@Composable fun Pill(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.semantics { this.selected = selected; stateDescription = if (selected) "선택됨" else "선택 안 됨" }, color = if (selected) Purple else MoaSurface, contentColor = if (selected) Color.White else Muted, shape = RoundedCornerShape(50), border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1) }
}
@Composable fun SampleNotice(sample: Boolean) { if (sample) Text("DESIGN PREVIEW  ·  실제 예매 정보가 아닌 체험 데이터", color = Muted, fontSize = 10.sp, letterSpacing = 0.3.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) }

@Composable fun HomeScreen(catalog: Catalog?, filters: Filters, favorites: List<Performance>, busy: Boolean, onFilter: () -> Unit, change: (Filters) -> Unit, favorite: (Performance) -> Unit, open: (Performance) -> Unit, refresh: () -> Unit) {
    val all = catalog?.performances.orEmpty()
    val list = remember(all, filters) { filterPerformances(all, "", filters) }
    val upcoming = list.filter { it.startDate >= LocalDate.now().toString() }.ifEmpty { list }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Canvas, Lavender, Pink.copy(alpha = .35f)))).padding(horizontal = 24.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("YOUR NEXT STAGE AWAITS", modifier = Modifier.weight(1f), color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp); RegionMenu(filters, change, Modifier.widthIn(max = 145.dp), pill = true) }
            Spacer(Modifier.height(12.dp)); Text("당신의 다음 설렘을\n여기, 모아 ✦", color = Ink, fontSize = 32.sp, lineHeight = 43.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.4).sp)
            Text("보고 싶은 공연, 한곳에서 만나보세요.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            Spacer(Modifier.height(8.dp))
        } }
        item { DiscoveryMenus(filters, change) }
        item { SampleNotice(catalog?.sample == true) }
        item { Column(Modifier.padding(horizontal = 22.dp)) { SectionTitle("무대가 기다리고 있어요", "${filters.regionLabel}에서 만나는 ${list.size}개의 공연", "필터", onFilter) } }
        if (catalog == null) item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        else if (list.isEmpty()) item { EmptyState("아직 만날 공연이 없어요", "지역이나 날짜를 바꾸면 다른 무대를 찾을 수 있어요.") }
        else {
            item { LazyRow(contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) { items(upcoming.take(8), key = { it.id }) { p -> PosterCard(p, favorites.any { it.id == p.id }, { favorite(p) }, { open(p) }) } } }
            item { Column(Modifier.padding(horizontal = 22.dp)) { SectionTitle("모든 공연 둘러보기", "마음에 드는 무대를 찜해보세요", if (filters.descending) "종료일순 ↕" else "시작일순 ↕") { change(filters.copy(descending = !filters.descending)) } } }
            items(list, key = { it.id }) { p -> PerformanceRow(p, favorites.any { it.id == p.id }, { favorite(p) }, { open(p) }, Modifier.padding(horizontal = 22.dp)) }
        }
        item { Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("보고 싶은 공연, 여기 다 모아.", color = Muted, fontSize = 12.sp); if (!catalog?.updatedAt.isNullOrBlank()) Text("최종 갱신 · ${updatedLabel(catalog!!.updatedAt)}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)); TextButton(onClick = refresh, enabled = !busy) { Text(if (busy) "불러오는 중…" else "공연 정보 새로고침", fontSize = 12.sp) } } }
    }
}

@Composable fun PosterCard(p: Performance, saved: Boolean, favorite: () -> Unit, open: () -> Unit) {
    Column(Modifier.width(190.dp).clickable(onClick = open)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(20.dp))) { PosterArt(p, Modifier.fillMaxSize()); Heart(saved, favorite, Modifier.align(Alignment.TopEnd).padding(10.dp)); Surface(color = Pink, shape = RoundedCornerShape(50), modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) { Text(p.genre, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) } }
        Text(p.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp, modifier = Modifier.padding(top = 12.dp).heightIn(min = 44.dp))
        Text(dateLabel(p), fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 6.dp)); Text("${venueLabel(p.venue)} · ${p.region.ifBlank { "지역 정보 없음" }}", fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}
@Composable fun Heart(saved: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) { Surface(shape = RoundedCornerShape(50), color = MoaSurface.copy(alpha = .94f), modifier = modifier) { IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) { Icon(if (saved) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, if (saved) "찜 해제" else "찜하기", tint = if (saved) Color(0xFFD76B95) else Muted, modifier = Modifier.size(23.dp)) } } }

@Composable fun PosterArt(p: Performance, modifier: Modifier = Modifier) {
    val palette = when (p.artwork) {
        "night" -> listOf(Color(0xFF171C42), Color(0xFF6159AA))
        "rose" -> listOf(Color(0xFF482034), Color(0xFFAE647C))
        "mint" -> listOf(Color(0xFF194F4B), Color(0xFF81B6A8))
        "amber" -> listOf(Color(0xFF54301D), Color(0xFFC9995D))
        "blue" -> listOf(Color(0xFF264B88), Color(0xFF8BAEDC))
        else -> listOf(Color(0xFF5D418C), Color(0xFFC59DD1))
    }
    BoxWithConstraints(modifier.background(Brush.linearGradient(palette))) {
        val compact = maxWidth < 120.dp
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = .08f), radius = size.width * .55f, center = androidx.compose.ui.geometry.Offset(size.width * .95f, size.height * .45f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .1f), radius = size.width * .38f, center = androidx.compose.ui.geometry.Offset(size.width * .95f, size.height * .45f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .08f), radius = size.width * .26f, center = androidx.compose.ui.geometry.Offset(size.width * .22f, size.height * .8f))
            drawLine(Color.White.copy(alpha = .15f), androidx.compose.ui.geometry.Offset(size.width * .1f, size.height * .23f), androidx.compose.ui.geometry.Offset(size.width * .85f, size.height * .23f))
        }
        Column(Modifier.fillMaxSize().padding(if (compact) 8.dp else 20.dp), verticalArrangement = Arrangement.Center) {
            if (!compact) Text("M O A  /  STAGE", color = Color.White.copy(alpha = .7f), fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(if (compact) 4.dp else 18.dp))
            Text("✦", fontSize = if (compact) 22.sp else 42.sp, color = Color(0xFFFFD7E7))
            Spacer(Modifier.height(if (compact) 4.dp else 14.dp))
            Text(p.title.substringBefore(" — "), color = Color.White, fontSize = if (compact) 12.sp else 24.sp, fontWeight = FontWeight.Bold, lineHeight = if (compact) 17.sp else 31.sp, maxLines = if (compact) 3 else 4, overflow = TextOverflow.Ellipsis)
            if (!compact) { Spacer(Modifier.height(12.dp)); Text(p.genre.uppercase(), color = Color.White.copy(alpha = .7f), fontSize = 10.sp, letterSpacing = 2.sp) }
        }
        if (p.posterUrl.isNotBlank()) AsyncImage(model = p.posterUrl, contentDescription = "${p.title} 포스터", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable fun PerformanceRow(p: Performance, saved: Boolean, favorite: () -> Unit, open: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = open, shape = RoundedCornerShape(20.dp), color = MoaSurface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PosterArt(p, Modifier.width(82.dp).height(118.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("${if (p.id.startsWith("DEMO")) "체험용 · " else ""}${p.genre} · ${p.region}", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = favorite, modifier = Modifier.size(48.dp)) { Icon(if (saved) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, if (saved) "찜 해제" else "찜하기", tint = if (saved) Color(0xFFD76B95) else Muted, modifier = Modifier.size(21.dp)) } }
                Text(p.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(dateLabel(p), fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 8.dp)); Text(venueLabel(p.venue), fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable fun EmptyState(title: String, body: String) { Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) { Surface(color = Lavender, shape = RoundedCornerShape(24.dp)) { Icon(Icons.Outlined.TheaterComedy, null, tint = Purple, modifier = Modifier.padding(20.dp).size(32.dp)) }; Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp)); Text(body, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }

@Composable fun SearchScreen(catalog: Catalog?, filters: Filters, query: String, history: List<String>, favorites: List<Performance>, setQuery: (String) -> Unit, search: (String) -> Unit, clearHistory: (String?) -> Unit, onFilter: () -> Unit, change: (Filters) -> Unit, favorite: (Performance) -> Unit, open: (Performance) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val list = filterPerformances(catalog?.performances.orEmpty(), query, filters)
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Column(Modifier.padding(horizontal = 22.dp)) { Text("어떤 무대를 찾고 있나요?", fontWeight = FontWeight.Bold, fontSize = 25.sp, letterSpacing = (-.8).sp); Spacer(Modifier.height(16.dp)); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = query, onValueChange = setQuery, singleLine = true, placeholder = { Text("공연명이나 아티스트", fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Purple) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { setQuery("") }) { Icon(Icons.Rounded.Close, "검색어 지우기") } }, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { search(query); keyboard?.hide() })); FilledTonalIconButton(onClick = onFilter, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Tune, "필터") } } } }
        item { DiscoveryMenus(filters, change) }
        if (history.isNotEmpty()) item { Column(Modifier.padding(horizontal = 22.dp)) { SectionTitle("최근 검색어", action = "전체 삭제", onAction = { clearHistory(null) }); Spacer(Modifier.height(8.dp)); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { history.forEach { word -> InputChip(selected = false, onClick = { search(word) }, label = { Text(word, fontSize = 12.sp) }, trailingIcon = { IconButton(onClick = { clearHistory(word) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Rounded.Close, "검색 기록 삭제", modifier = Modifier.size(14.dp)) } }) } } } }
        item { SampleNotice(catalog?.sample == true) }
        item { Column(Modifier.padding(horizontal = 22.dp)) { SectionTitle("${if (query.isBlank()) "공연 둘러보기" else "검색 결과"}  ${list.size}", action = if (filters.descending) "종료일순 ↕" else "시작일순 ↕", onAction = { change(filters.copy(descending = !filters.descending)) }) } }
        if (list.isEmpty()) item { EmptyState("찾는 공연이 없어요", "검색어나 필터를 바꿔 다시 찾아보세요.") }
        items(list, key = { it.id }) { p -> PerformanceRow(p, favorites.any { it.id == p.id }, { favorite(p) }, { keyboard?.hide(); search(query); open(p) }, Modifier.padding(horizontal = 22.dp)) }
    }
}

@Composable fun FavoritesScreen(favorites: List<Performance>, favorite: (Performance) -> Unit, open: (Performance) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("마음에 모아둔 무대", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, modifier = Modifier.padding(top = 18.dp)); Text("다음 설렘을 위한 나만의 컬렉션 · ${favorites.size}", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)) }
        if (favorites.isEmpty()) item { EmptyState("첫 번째 설렘을 모아보세요", "공연 카드의 하트를 누르면 여기에 저장돼요.") }
        items(favorites, key = { it.id }) { p -> PerformanceRow(p, true, { favorite(p) }, { open(p) }) }
    }
}

@Composable fun DetailScreen(p: Performance, saved: Boolean, sample: Boolean, back: () -> Unit, favorite: () -> Unit) {
    val uri = LocalUriHandler.current
    var booking by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf(false) }
    BackHandler(onBack = back)
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로") }; Spacer(Modifier.weight(1f)); Text("PERFORMANCE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp); Spacer(Modifier.weight(1f)); IconButton(onClick = favorite) { Icon(if (saved) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, if (saved) "찜 해제" else "찜하기", tint = Purple) } }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Lavender, Canvas))).padding(horizontal = 76.dp, vertical = 22.dp)) { PosterArt(p, Modifier.fillMaxWidth().aspectRatio(.72f).clip(RoundedCornerShape(22.dp))) } }
            item { Column(Modifier.padding(horizontal = 24.dp)) { Text("${p.genre}  /  ${p.region}  /  ${p.status}", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(p.title, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp, letterSpacing = (-.8).sp, modifier = Modifier.padding(top = 10.dp)); if (sample) Text("디자인 체험용 가상 공연입니다.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)) } }
            item { Surface(color = MoaSurface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Lavender), modifier = Modifier.padding(horizontal = 22.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) { InfoRow(Icons.Outlined.CalendarMonth, "공연 기간", dateLabel(p)); InfoRow(Icons.Outlined.LocationOn, "공연장", venueLabel(p.venue)); InfoRow(Icons.Outlined.Schedule, "관람 시간", p.runtime); InfoRow(Icons.Outlined.PersonOutline, "관람 연령", p.age); InfoRow(Icons.Outlined.ConfirmationNumber, "티켓 가격", p.ticketPrice) } } }
            item { Column(Modifier.padding(horizontal = 24.dp)) { SectionTitle("함께하는 아티스트"); Spacer(Modifier.height(12.dp)); Text(p.artists.joinToString(" · ").ifBlank { "출연진 정보가 제공되지 않았어요." }, color = Muted, fontSize = 14.sp, lineHeight = 24.sp) } }
            item { Column(Modifier.padding(horizontal = 24.dp)) { SectionTitle("공연 안내"); Spacer(Modifier.height(12.dp)); Text(p.description.ifBlank { "상세 소개는 예매처에서 확인해주세요." }, color = Muted, fontSize = 14.sp, lineHeight = 25.sp); if (p.schedule.isNotBlank()) Text("\n공연 일정\n${p.schedule}", color = Ink, fontSize = 13.sp, lineHeight = 23.sp) } }
            item { Text(if (sample) "MOA DESIGN PREVIEW" else "출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)\n정보는 변경될 수 있으니 예매처에서 최종 확인해주세요.", color = Muted, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 24.dp)) }
        }
        Surface(color = MoaSurface, shadowElevation = 8.dp) { Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) { Button(onClick = { booking = true }, enabled = p.bookingUrls.isNotEmpty() && !sample, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Outlined.ConfirmationNumber, null); Spacer(Modifier.width(10.dp)); Text("공식 예매처로 이동", fontWeight = FontWeight.Bold) }; if (p.bookingUrls.isEmpty() || sample) Text(if (sample) "체험 데이터에서는 예매를 제공하지 않아요." else "제공된 공식 예매 링크가 없어요.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) } }
    }
    if (booking) AlertDialog(onDismissRequest = { booking = false }, title = { Text("예매처 선택") }, text = { Column { p.bookingUrls.forEach { link -> TextButton(onClick = { booking = false; if (link.url.startsWith("https://") || link.url.startsWith("http://")) runCatching { uri.openUri(link.url) }.onFailure { linkError = true } else linkError = true }) { Text(link.name.ifBlank { "공식 예매처" }) } } } }, confirmButton = { TextButton(onClick = { booking = false }) { Text("닫기") } })
    if (linkError) AlertDialog(onDismissRequest = { linkError = false }, title = { Text("링크를 열 수 없어요") }, text = { Text("예매처 주소나 브라우저 설정을 확인해주세요.") }, confirmButton = { TextButton(onClick = { linkError = false }) { Text("확인") } })
}
@Composable fun InfoRow(icon: ImageVector, label: String, value: String) { Row(verticalAlignment = Alignment.Top) { Icon(icon, null, tint = Purple, modifier = Modifier.size(19.dp)); Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.width(88.dp).padding(start = 10.dp)); Text(value.ifBlank { "정보 없음" }, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)) } }

@Composable fun SettingsScreen(filters: Filters, darkMode: Boolean, changeDarkMode: (Boolean) -> Unit, onFilter: () -> Unit) {
    var privacyOpen by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val privacy = remember(context) { context.assets.open("privacy_policy.txt").bufferedReader().use { it.readText() } }
    if (privacyOpen) AlertDialog(onDismissRequest = { privacyOpen = false }, title = { Text("개인정보처리방침") }, text = { Text(privacy, modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), fontSize = 13.sp, lineHeight = 22.sp) }, confirmButton = { TextButton(onClick = { privacyOpen = false }) { Text("닫기") } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("나의 모아", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp))
        Text("취향에 맞게, 더 편안하게.", color = Muted, fontSize = 13.sp)
        Surface(color = Lavender, shape = RoundedCornerShape(24.dp)) { Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.app_icon), null, Modifier.size(62.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.padding(start = 16.dp)) { Text("보고 싶은 공연,", fontWeight = FontWeight.Bold); Text("여기 다 모아.", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Purple) }
        } }
        Surface(color = MoaSurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Lavender)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("다크모드", fontWeight = FontWeight.Bold); Text("눈이 편안한 어두운 화면", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) }
                Switch(checked = darkMode, onCheckedChange = changeDarkMode, modifier = Modifier.semantics { contentDescription = "다크모드" })
            }
        }
        SectionTitle("내 탐색 설정")
        Surface(onClick = onFilter, shape = RoundedCornerShape(18.dp), color = MoaSurface, border = BorderStroke(1.dp, Lavender)) { Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("기본 지역 · 관심 장르", fontWeight = FontWeight.Bold)
            Text("${filters.regionLabel} · ${filters.genres.joinToString(", ").ifBlank { "전체 장르" }}", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        } }
        TextButton(onClick = { privacyOpen = true }) { Text("개인정보처리방침") }
        HorizontalDivider(color = Lavender)
        Text("모아 v${BuildConfig.VERSION_NAME}\n공연 정보 출처: (재)예술경영지원센터 KOPIS\n찜과 선호 설정은 이 기기에 저장됩니다.", color = Muted, fontSize = 12.sp, lineHeight = 22.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FilterSheet(current: Filters, dismiss: () -> Unit, apply: (Filters) -> Unit) {
    var draft by remember { mutableStateOf(current) }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MoaSurface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SectionTitle("나에게 맞는 무대", action = "초기화", onAction = { draft = Filters() })
            Column { Text("지역", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); RegionMenu(draft, { draft = it }, Modifier.fillMaxWidth()); SelectionNote(draft.regionLabel) }
            Column { Text("장르", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); GenreMenu(draft, { draft = it }, Modifier.fillMaxWidth()); SelectionNote(draft.genres.joinToString(", ").ifBlank { "전체 장르" }) }
            Column { Text("기간", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); PeriodMenu(draft, { draft = it }, Modifier.fillMaxWidth()); SelectionNote(draft.periodLabel) }
            Button(onClick = { apply(draft) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("이 조건으로 찾아보기", fontWeight = FontWeight.Bold) }
        }
    }
}
