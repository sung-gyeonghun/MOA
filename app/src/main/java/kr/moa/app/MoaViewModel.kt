package kr.moa.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kr.moa.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@HiltViewModel
class MoaViewModel @Inject constructor(private val repository: MoaRepository) : ViewModel() {
    val catalog = MutableStateFlow<Catalog?>(null)
    val preferences = repository.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, Preferences())
    val favorites = combine(repository.favorites, catalog) { saved, current ->
        val latest = current?.performances?.associateBy { it.id }.orEmpty()
        saved.map { latest[it.id] ?: it }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val filters = MutableStateFlow(Filters())
    val query = MutableStateFlow("")
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    init { viewModelScope.launch { catalog.value = repository.initial(); val defaults = repository.preferences.first(); filters.value = Filters(defaults.region, defaults.genres, selectedRegions = defaults.selectedRegions); if (defaults.endpoint.isNotBlank()) refresh() } }
    fun refresh() {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                val url = repository.preferences.first().endpoint
                if (url.isBlank()) { message.value = "설정에서 공연 데이터 주소를 연결해주세요."; return@launch }
                catalog.value = repository.refresh(url)
                message.value = "새로운 공연 정보를 불러왔어요."
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { message.value = "갱신하지 못했어요. 저장된 데이터는 계속 볼 수 있어요."
            } finally { busy.value = false }
        }
    }
    fun favorite(p: Performance) { viewModelScope.launch { repository.favorite(p) } }
    fun defaults(f: Filters) { filters.value = f; viewModelScope.launch { repository.setDefaults(f.region, f.genres, f.effectiveRegions) } }
    fun endpoint(url: String) { viewModelScope.launch { repository.setEndpoint(url); message.value = "데이터 주소를 저장했어요." } }
    fun search(word: String) { query.value = word; viewModelScope.launch { repository.history(word) } }
    fun clearHistory(word: String? = null) { viewModelScope.launch { repository.clearHistory(word) } }
}
