package kr.moa.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences as StoredPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.File
import java.util.UUID

/** Real HTTPS/Retrofit + disk-backed Room/DataStore, without a KOPIS key. */
class RepositoryTest {
    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var database: MoaDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repository: MoaRepository
    private lateinit var api: CatalogApi
    private lateinit var preferenceFile: File
    private lateinit var databaseName: String
    private lateinit var settings: DataStore<StoredPreferences>
    private val p = Performance("PF123456", "테스트 콘서트", "콘서트", "대구", "테스트 공연장", "2026-10-01", "2026-11-10", artists = listOf("모아"))
    private val manifest = Manifest("version-1", "2026-10-13T00:00:00+09:00", listOf("performances/2026-10.json", "performances/2026-11.json"), count = 1,
        fileHashes = listOf("performances/2026-10.json", "performances/2026-11.json").associateWith {
            java.security.MessageDigest.getInstance("SHA-256").digest(json.encodeToString(listOf(p)).toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
        })

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "test-${UUID.randomUUID()}.db"
        database = Room.databaseBuilder(context, MoaDatabase::class.java, databaseName).addMigrations(MIGRATION_1_2).build()
        preferenceFile = File(context.cacheDir, "${UUID.randomUUID()}.preferences_pb")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        settings = PreferenceDataStoreFactory.create(scope = scope, produceFile = { preferenceFile })
        val cert = HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(cert).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(cert.certificate).build()
        server = MockWebServer()
        server.useHttps(serverTls.sslSocketFactory(), false)
        server.start()
        val client = OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager).callTimeout(3, java.util.concurrent.TimeUnit.SECONDS).build()
        api = Retrofit.Builder().baseUrl(server.url("/")).client(client).build().create(CatalogApi::class.java)
        repository = MoaRepository(context, database, api, settings)
    }
    @After fun tearDown() = runBlocking {
        database.close()
        scope.coroutineContext[Job]?.cancelAndJoin()
        runCatching { server.shutdown() }
        context.deleteDatabase(databaseName)
        preferenceFile.delete()
        Unit
    }
    private fun enqueueSnapshot(latest: Manifest = manifest) {
        listOf(json.encodeToString(manifest), json.encodeToString(listOf(p)), json.encodeToString(listOf(p)), json.encodeToString(latest)).forEach { server.enqueue(MockResponse().setBody(it).setHeader("Content-Type", "application/json")) }
    }
    private suspend fun loadSnapshot(): Catalog { enqueueSnapshot(); return repository.refresh(server.url("/").toString()) }
    private suspend fun assertRejected(block: suspend () -> Unit) {
        try { block(); fail("Expected failed refresh") } catch (e: Exception) { assertFalse(e is CancellationException) }
    }

    @Test fun httpsRefreshDeduplicatesAndPersistsOfflineCatalog() = runBlocking {
        val downloaded = loadSnapshot()
        assertEquals(listOf(p), downloaded.performances)
        assertFalse(downloaded.sample)
        assertEquals(manifest.updatedAt, downloaded.updatedAt)
        assertEquals("/manifest.json", server.takeRequest().path)
        assertEquals("/performances/2026-10.json", server.takeRequest().path)
        assertEquals("/performances/2026-11.json", server.takeRequest().path)
        assertEquals("/manifest.json", server.takeRequest().path)
        server.shutdown()
        database.close()
        database = Room.databaseBuilder(context, MoaDatabase::class.java, databaseName).addMigrations(MIGRATION_1_2).build()
        val reopened = MoaRepository(context, database, api, settings).initial()
        assertEquals(downloaded, reopened)
    }
    @Test fun httpErrorMalformedJsonAndEmptySnapshotKeepLastGoodData() = runBlocking {
        val original = loadSnapshot()
        server.enqueue(MockResponse().setResponseCode(503))
        assertRejected { repository.refresh(server.url("/").toString()) }
        assertEquals(original, repository.initial())
        server.enqueue(MockResponse().setBody("{invalid"))
        assertRejected { repository.refresh(server.url("/").toString()) }
        assertEquals(original, repository.initial())
        server.enqueue(MockResponse().setBody(json.encodeToString(manifest.copy(fileHashes = emptyMap()))))
        server.enqueue(MockResponse().setBody("[]")); server.enqueue(MockResponse().setBody("[]"))
        assertRejected { repository.refresh(server.url("/").toString()) }
        assertEquals(original, repository.initial())
    }
    @Test fun changingPublicationAndInvalidDatesCannotReplaceCache() = runBlocking {
        val original = loadSnapshot()
        enqueueSnapshot(manifest.copy(version = "version-2"))
        assertRejected { repository.refresh(server.url("/").toString()) }
        assertEquals(original, repository.initial())
        server.enqueue(MockResponse().setBody(json.encodeToString(manifest.copy(fileHashes = emptyMap()))))
        repeat(2) { server.enqueue(MockResponse().setBody(json.encodeToString(listOf(p.copy(endDate = "2020-01-01"))))) }
        assertRejected { repository.refresh(server.url("/").toString()) }
        assertEquals(original, repository.initial())
    }
    @Test fun preferencesAndFavoritesSurviveStorageReopen() = runBlocking {
        repository.setDefaults("대구", setOf("콘서트", "뮤지컬"))
        repository.setEndpoint(server.url("/").toString())
        repository.history("모아"); repository.history("모아"); repository.history("콘서트")
        repository.favorite(p)
        assertEquals(listOf(p), repository.favorites.first())
        scope.coroutineContext[Job]?.cancelAndJoin()
        database.close()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        database = Room.databaseBuilder(context, MoaDatabase::class.java, databaseName).addMigrations(MIGRATION_1_2).build()
        val reopened = MoaRepository(context, database, api, PreferenceDataStoreFactory.create(scope = scope, produceFile = { preferenceFile }))
        val prefs = reopened.preferences.first()
        assertEquals("대구", prefs.region)
        assertEquals(setOf("콘서트", "뮤지컬"), prefs.genres)
        assertEquals(listOf("콘서트", "모아"), prefs.history)
        assertEquals(server.url("/").toString(), prefs.endpoint)
        assertEquals(listOf(p), reopened.favorites.first())
        reopened.favorite(p)
        assertTrue(reopened.favorites.first().isEmpty())
    }
    @Test fun migrationKeepsVersionOneFavorites() = runBlocking {
        database.close()
        context.deleteDatabase(databaseName)
        val old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null)
        old.execSQL("CREATE TABLE performances (id TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
        old.execSQL("CREATE TABLE favorites (id TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
        old.execSQL("INSERT INTO favorites (id, payload) VALUES (?, ?)", arrayOf(p.id, json.encodeToString(p)))
        old.version = 1
        old.close()
        database = Room.databaseBuilder(context, MoaDatabase::class.java, databaseName).addMigrations(MIGRATION_1_2).build()
        assertEquals(listOf(Favorite(p.id, json.encodeToString(p))), database.dao().favorites().first())
        assertNull(database.dao().metadata())
    }
    @Test fun fiveThousandCachedPerformancesLoadWithinTwoSeconds() = runBlocking {
        val items = (1..5000).map { p.copy(id = "PF${100000 + it}") }
        database.dao().replaceCatalog(items.map { CachedPerformance(it.id, json.encodeToString(it)) }, CatalogMetadata(updatedAt = manifest.updatedAt))
        val started = android.os.SystemClock.elapsedRealtime()
        val catalog = repository.initial()
        val elapsed = android.os.SystemClock.elapsedRealtime() - started
        assertEquals(5000, catalog.performances.size)
        println("CACHE_LOAD_5000_MS=$elapsed")
        assertTrue("Cached 5000-record load took ${elapsed}ms", elapsed < 2000)
    }
    @Test fun mismatchedFileChecksumKeepsLastGoodSnapshot() = runBlocking {
        val original = loadSnapshot()
        val invalid = manifest.copy(fileHashes = mapOf("performances/2026-10.json" to "0".repeat(64)))
        server.enqueue(MockResponse().setBody(json.encodeToString(invalid)))
        server.enqueue(MockResponse().setBody(json.encodeToString(listOf(p))))
        assertRejected { repository.refresh(server.url("/").toString()) }
        assertEquals(original, repository.initial())
    }
}
