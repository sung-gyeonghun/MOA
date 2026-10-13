package kr.moa.app.data

import android.content.Context
import androidx.room.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences as StoredPreferences
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.moa.app.BuildConfig

private val Context.settings by preferencesDataStore("moa_preferences")
val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Entity(tableName = "performances") data class CachedPerformance(@PrimaryKey val id: String, val payload: String)
@Entity(tableName = "favorites") data class Favorite(@PrimaryKey val id: String, val payload: String)
@Entity(tableName = "catalog_metadata") data class CatalogMetadata(@PrimaryKey val id: Int = 0, val updatedAt: String, val sample: Boolean = false)
@Dao interface MoaDao {
    @Query("SELECT * FROM performances") suspend fun catalog(): List<CachedPerformance>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCatalog(items: List<CachedPerformance>)
    @Query("DELETE FROM performances") suspend fun clearCatalog()
    @Query("SELECT * FROM catalog_metadata WHERE id = 0") suspend fun metadata(): CatalogMetadata?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMetadata(metadata: CatalogMetadata)
    @Transaction suspend fun replaceCatalog(items: List<CachedPerformance>, metadata: CatalogMetadata) { clearCatalog(); insertCatalog(items); insertMetadata(metadata) }
    @Query("SELECT * FROM favorites") fun favorites(): Flow<List<Favorite>>
    @Query("SELECT * FROM favorites WHERE id = :id") suspend fun favorite(id: String): Favorite?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveFavorite(item: Favorite)
    @Query("DELETE FROM favorites WHERE id = :id") suspend fun deleteFavorite(id: String)
    @Transaction suspend fun toggleFavorite(item: Favorite) { if (favorite(item.id) == null) saveFavorite(item) else deleteFavorite(item.id) }
}
@Database(entities = [CachedPerformance::class, Favorite::class, CatalogMetadata::class], version = 2, exportSchema = false)
abstract class MoaDatabase : RoomDatabase() { abstract fun dao(): MoaDao }
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS catalog_metadata (id INTEGER NOT NULL, updatedAt TEXT NOT NULL, sample INTEGER NOT NULL, PRIMARY KEY(id))")
    }
}
interface CatalogApi { @GET suspend fun fetch(@Url url: String): ResponseBody }

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): MoaDatabase =
        Room.databaseBuilder(context, MoaDatabase::class.java, "moa.db").addMigrations(MIGRATION_1_2).build()
    @Provides @Singleton fun api(): CatalogApi = Retrofit.Builder().baseUrl("https://example.com/")
        .client(okhttp3.OkHttpClient.Builder().callTimeout(45, java.util.concurrent.TimeUnit.SECONDS).build()).build().create(CatalogApi::class.java)
    @Provides @Singleton fun settings(@ApplicationContext context: Context): DataStore<StoredPreferences> = context.settings
}

data class Preferences(val region: String = "전국", val genres: Set<String> = emptySet(), val endpoint: String = "", val history: List<String> = emptyList(), val updatedAt: String = "", val sample: Boolean = true, val selectedRegions: Set<String> = emptySet())

@Singleton
class MoaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MoaDatabase,
    private val api: CatalogApi,
    private val settings: DataStore<StoredPreferences>
) {
    private val dao = database.dao()
    private val regionsKey = stringSetPreferencesKey("selected_regions")
    private val regionKey = stringPreferencesKey("region")
    private val genresKey = stringSetPreferencesKey("genres")
    private val endpointKey = stringPreferencesKey("endpoint")
    private val historyKey = stringPreferencesKey("history")
    private val updatedKey = stringPreferencesKey("updated")
    private val sampleKey = booleanPreferencesKey("sample")
    val preferences = settings.data.map { p -> Preferences((p[regionKey] ?: "전국").let { if (it in setOf("광주", "전남", "전남광주통합특별시")) "전남광주" else it }, p[genresKey] ?: emptySet(), p[endpointKey] ?: if (BuildConfig.DEBUG) "" else "https://sung-gyeonghun.github.io/MOA/", runCatching { json.decodeFromString<List<String>>(p[historyKey] ?: "[]") }.getOrDefault(emptyList()), p[updatedKey] ?: "", p[sampleKey] ?: true, (p[regionsKey] ?: setOf(p[regionKey] ?: "전국")).filter { it != "전국" }.map { if (it in setOf("광주", "전남", "전남광주통합특별시")) "전남광주" else it }.toSet()) }
    val favorites = dao.favorites().map { list -> list.mapNotNull { runCatching { json.decodeFromString<Performance>(it.payload) }.getOrNull() } }
    suspend fun initial(): Catalog = withContext(Dispatchers.IO) {
        val saved = dao.catalog().mapNotNull { runCatching { json.decodeFromString<Performance>(it.payload) }.getOrNull() }
        val prefs = preferences.first()
        val metadata = dao.metadata()
        if (saved.isNotEmpty()) {
            Catalog(metadata?.updatedAt ?: prefs.updatedAt, saved, metadata?.sample ?: prefs.sample)
        } else if (BuildConfig.DEBUG) {
            json.decodeFromString(context.assets.open("sample_catalog.json").bufferedReader().use { it.readText() })
        } else {
            val bundled = runCatching { context.assets.open("bootstrap_catalog.json").bufferedReader().use { json.decodeFromString<Catalog>(it.readText()) } }.getOrNull()
            if (bundled != null && bundled.performances.isNotEmpty() && !bundled.sample) {
                dao.replaceCatalog(bundled.performances.map { CachedPerformance(it.id, json.encodeToString(it)) }, CatalogMetadata(updatedAt = bundled.updatedAt))
                bundled
            } else Catalog("", emptyList(), false)
        }
    }
    suspend fun refresh(endpoint: String): Catalog = withContext(Dispatchers.IO) {
        require(endpoint.startsWith("https://") && URI(endpoint).host != null) { "HTTPS 데이터 주소를 입력해주세요." }
        val base = endpoint.trimEnd('/') + "/"
        val manifest = api.fetch(base + "manifest.json").use { json.decodeFromString<Manifest>(it.string()) }
        require(manifest.files.isNotEmpty() && manifest.files.size <= 36) { "데이터 파일 목록을 확인해주세요." }
        require(manifest.version.isNotBlank()) { "데이터 버전이 누락되었습니다." }
        java.time.OffsetDateTime.parse(manifest.updatedAt)
        val all = manifest.files.flatMap { path ->
            require(!path.contains("..") && path.matches(Regex("performances/[0-9]{4}-[0-9]{2}\\.json"))) { "잘못된 파일 경로입니다." }
            java.time.YearMonth.parse(path.substringAfter('/').substringBefore('.'))
            api.fetch(base + path).use {
                val payload = it.bytes()
                val expected = manifest.fileHashes[path]
                if (expected != null) {
                    val actual = java.security.MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { byte -> "%02x".format(byte) }
                    require(actual == expected) { "공연 데이터 무결성 검증에 실패했습니다." }
                }
                json.decodeFromString<List<Performance>>(payload.toString(Charsets.UTF_8))
            }
        }.distinctBy { it.id }
        require(all.isNotEmpty()) { "공연 데이터가 비어 있습니다. 기존 데이터를 유지합니다." }
        all.forEach { require(it.id.matches(Regex("PF[0-9]+")) && it.title.isNotBlank() && java.time.LocalDate.parse(it.startDate) <= java.time.LocalDate.parse(it.endDate)) }
        val latest = api.fetch(base + "manifest.json").use { json.decodeFromString<Manifest>(it.string()) }
        require(latest.version == manifest.version && latest.files == manifest.files) { "갱신 중 데이터 버전이 변경되었습니다. 다시 시도해주세요." }
        dao.replaceCatalog(all.map { CachedPerformance(it.id, json.encodeToString(it)) }, CatalogMetadata(updatedAt = manifest.updatedAt))
        Catalog(manifest.updatedAt, all, false)
    }
    suspend fun favorite(p: Performance) { dao.toggleFavorite(Favorite(p.id, json.encodeToString(p))) }
    suspend fun setDefaults(region: String, genres: Set<String>, selectedRegions: Set<String> = if (region == "전국") emptySet() else setOf(region)) { settings.edit { it[regionKey] = region; it[genresKey] = genres; it[regionsKey] = selectedRegions } }
    suspend fun setEndpoint(url: String) { settings.edit { it[endpointKey] = url.trim() } }
    suspend fun history(word: String) { if (word.isNotBlank()) settings.edit { p -> val old = runCatching { json.decodeFromString<List<String>>(p[historyKey] ?: "[]") }.getOrDefault(emptyList()); p[historyKey] = json.encodeToString((listOf(word.trim()) + old).distinct().take(10)) } }
    suspend fun clearHistory(word: String? = null) { settings.edit { p -> val old = runCatching { json.decodeFromString<List<String>>(p[historyKey] ?: "[]") }.getOrDefault(emptyList()); p[historyKey] = json.encodeToString(if (word == null) emptyList() else old.filterNot { it == word }) } }
}
