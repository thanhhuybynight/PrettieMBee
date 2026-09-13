package anhiutangerine.prettiembee.data.repository

import android.app.Application
import android.content.ContextWrapper
import android.net.Uri
import anhiutangerine.prettiembee.data.model.CommunityTheme
import anhiutangerine.prettiembee.data.model.MbStoreTheme
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private lateinit var context: ContextWrapper
    private lateinit var repository: ThemeRepository
    private lateinit var server: MockWebServer
    private val responses = mutableMapOf<String, () -> MockResponse>()
    private val catalogRequests = AtomicInteger()
    private var catalogBody = "[]"
    private var catalogStatus = 200

    @Before fun setUp() {
        filesDir = temporaryFolder.newFolder("files")
        cacheDir = temporaryFolder.newFolder("cache")
        context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getFilesDir() = this@ThemeRepositoryTest.filesDir
            override fun getCacheDir() = this@ThemeRepositoryTest.cacheDir
        }
        server = MockWebServer()
        responses.clear()
        responses["/catalog"] = {
            catalogRequests.incrementAndGet()
            response(catalogBody.toByteArray(), catalogStatus)
        }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                responses[request.requestUrl?.encodedPath]?.invoke() ?: MockResponse().setResponseCode(404)
        }
        server.start()
        repository = ThemeRepository(context, url("/catalog"))
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun downloadedLayoutRequiresLowercasePngFileAndToken() {
        val dir = repository.getThemeDir("theme")
        write(File(dir, "theme/token.json"), "{}")
        for (name in listOf("notes.txt", "photo.jpg", "photo.PNG")) {
            val image = File(dir, "images/$name")
            write(image, "not a supported image name")
            assertFalse(name, repository.isThemeDownloaded(theme()))
            image.delete()
        }
        File(dir, "images/directory.png").mkdir()
        assertFalse(repository.isThemeDownloaded(theme()))
        write(File(dir, "images/photo.png"), "PNG content is not decoded by the injector")
        assertTrue(repository.isThemeDownloaded(theme()))
        File(dir, "theme/token.json").delete()
        assertFalse(repository.isThemeDownloaded(theme()))
    }

    @Test fun cachedDefaultAvoidsNetworkAndForcedRefreshReplacesCache() = runBlocking {
        cacheCatalog(theme("cached"))
        catalogBody = catalog(theme("remote"))
        assertEquals(listOf("cached"), repository.getCommunityThemes().map { it.id })
        assertEquals(0, catalogRequests.get())
        assertEquals(listOf("remote"), repository.getCommunityThemes(true).map { it.id })
        assertEquals(catalogBody, catalogCache().readText())
        assertEquals(1, catalogRequests.get())
        assertEquals(listOf("remote"), repository.getCommunityThemes().map { it.id })
        assertEquals(1, catalogRequests.get())
    }

    @Test fun coldStartFetchesRemoteCatalog() = runBlocking {
        catalogBody = catalog(theme("remote"))
        assertEquals(listOf("remote"), repository.getCommunityThemes().map { it.id })
        assertEquals(catalogBody, catalogCache().readText())
    }

    @Test fun malformedRemoteDoesNotPoisonValidCache() = runBlocking {
        cacheCatalog(theme("cached"))
        val original = catalogCache().readText()
        for (bad in listOf("not json", " ", "{}")) {
            catalogBody = bad
            assertEquals(listOf("cached"), repository.getCommunityThemes(true).map { it.id })
            assertEquals(original, catalogCache().readText())
        }
    }

    @Test fun malformedRemoteAndCacheFallBackToAssetsAndKeepCustomImports() = runBlocking {
        catalogCache().writeText("broken cache")
        catalogBody = "broken remote"
        val custom = theme("custom_existing").copy(isCustomImport = true)
        File(filesDir, "custom_themes.json").writeText(catalog(custom))
        val assets = assetCommunityThemes()
        assertFalse(assets.isEmpty())
        assertEquals(assets + custom, repository.getCommunityThemes(true))
    }

    @Test fun httpErrorFallsBackToCachedCatalog() = runBlocking {
        cacheCatalog(theme("cached"))
        catalogStatus = 503
        assertEquals(listOf("cached"), repository.getCommunityThemes(true).map { it.id })
    }

    @Test fun validEmptyRemoteCatalogDoesNotFallBackToAssets() = runBlocking {
        assertEquals(emptyList<CommunityTheme>(), repository.getCommunityThemes(true))
        assertEquals("[]", catalogCache().readText())
    }

    @Test fun cancelledCatalogRefreshDoesNotOverwriteCache() = runBlocking {
        cacheCatalog(theme("cached"))
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        responses["/slow-catalog"] = {
            entered.countDown()
            release.await(5, TimeUnit.SECONDS)
            response(catalog(theme("remote")).toByteArray())
        }
        val task = launch(Dispatchers.Default) {
            ThemeRepository(context, url("/slow-catalog")).getCommunityThemes(true)
            fail("Cancelled refresh must not return normally")
        }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            task.cancel()
        } finally {
            release.countDown()
        }
        task.join()
        assertTrue(task.isCancelled)
        assertEquals(catalog(theme("cached")), catalogCache().readText())
    }

    @Test fun unsafeCatalogAndCustomIdsAreNotExposedToUi() = runBlocking {
        catalogBody = catalog(theme("../outside"), theme("valid"))
        File(filesDir, "custom_themes.json").writeText(catalog(theme("../../bad")))
        assertEquals(listOf("valid"), repository.getCommunityThemes(true).map { it.id })
    }

    @Test fun storeAliasesAreDeduplicatedByUuidWithoutInventingIds() = runBlocking {
        val raw = context.assets.open("mb_store_catalog.json").bufferedReader().use {
            Json.decodeFromString<List<MbStoreTheme>>(it.readText())
        }
        assertTrue("Fixture must contain the known alias", raw.size > raw.distinctBy { it.uuid }.size)
        assertEquals(raw.distinctBy { it.uuid }, repository.getStoreThemes())
        assertEquals(raw.distinctBy { it.uuid }, repository.getStoreThemes())
    }

    @Test fun rejectsUnsafeThemeIdsBeforeReadingOrDeleting() {
        val outside = File(filesDir, "outside")
        write(File(outside, "keep.txt"), "keep")
        for (id in listOf("", ".", "..", "../outside", "a/b", "a\\b", "/outside", "a\n")) {
            assertThrows(id, IllegalArgumentException::class.java) { repository.getThemeDir(id) }
            assertFalse(id, repository.isThemeDownloaded(theme(id)))
            assertTrue(id, repository.deleteDownloadedTheme(theme(id)).isFailure)
        }
        assertEquals("keep", File(outside, "keep.txt").readText())
        assertEquals("safe-theme_1.2", repository.getThemeDir("safe-theme_1.2").name)
    }

    @Test fun symlinkedThemeCannotDeleteOutsideDirectory() {
        val outside = temporaryFolder.newFolder("outside")
        write(File(outside, "keep.txt"), "keep")
        Files.createSymbolicLink(File(repository.getThemesDirectory(), "linked").toPath(), outside.toPath())
        assertTrue(repository.deleteDownloadedTheme(theme("linked")).isFailure)
        assertEquals("keep", File(outside, "keep.txt").readText())
    }

    @Test fun nestedSymlinkCannotDeleteOutsideDirectory() {
        val outside = temporaryFolder.newFolder("outside")
        write(File(outside, "keep.txt"), "keep")
        val dir = repository.getThemeDir("theme").apply { mkdirs() }
        Files.createSymbolicLink(File(dir, "linked").toPath(), outside.toPath())
        assertTrue(repository.deleteDownloadedTheme(theme()).isFailure)
        assertEquals("keep", File(outside, "keep.txt").readText())
    }

    @Test fun failedRecursiveDeleteReturnsFailureAndKeepsCustomMetadata() {
        val custom = theme().copy(isCustomImport = true)
        File(filesDir, "custom_themes.json").writeText(catalog(custom))
        val dir = repository.getThemeDir(custom.id)
        write(File(dir, "keep.txt"), "keep")
        dir.setWritable(false, false)
        try {
            assumeFalse("Requires a non-root filesystem user", dir.canWrite())
            assertTrue(repository.deleteDownloadedTheme(custom).isFailure)
            assertTrue(File(dir, "keep.txt").isFile)
            assertEquals(catalog(custom), File(filesDir, "custom_themes.json").readText())
        } finally {
            dir.setWritable(true, true)
        }
    }

    @Test fun successfulDeleteRemovesDirectoryAndCustomMetadata() {
        val custom = theme().copy(isCustomImport = true)
        File(filesDir, "custom_themes.json").writeText(catalog(custom))
        write(File(repository.getThemeDir(custom.id), "keep.txt"), "remove")
        assertTrue(repository.deleteDownloadedTheme(custom).isSuccess)
        assertFalse(repository.getThemeDir(custom.id).exists())
        assertEquals("[]", File(filesDir, "custom_themes.json").readText())
        assertTrue(repository.deleteDownloadedTheme(custom).isSuccess)
    }

    @Test fun validNestedAndLegacyZipLayoutsStillImport() = runBlocking {
        val payload = zip("wrapped/images/image.png" to "png".toByteArray(), "wrapped/theme/token.json" to "{}".toByteArray())
        for (bytes in listOf(payload, zip("template.bin" to payload))) {
            val source = temporaryFolder.newFile()
            source.writeBytes(bytes)
            val imported = repository.importCustomZip(Uri.fromFile(source), "custom_theme.zip").getOrThrow()
            assertEquals("custom theme", imported.name)
            assertTrue(repository.isThemeDownloaded(imported))
            assertEquals(emptyList<String>(), cacheDir.list()!!.toList())
            assertTrue(repository.deleteDownloadedTheme(imported).isSuccess)
        }
    }

    @Test fun jpgOnlyImportIsRejectedAndCleaned() = runBlocking {
        val source = temporaryFolder.newFile()
        source.writeBytes(zip("images/image.jpg" to byteArrayOf(1), "theme/token.json" to "{}".toByteArray()))
        assertTrue(repository.importCustomZip(Uri.fromFile(source), "invalid.zip").isFailure)
        assertNoTransferArtifacts()
    }

    @Test fun failedImportExtractionCleansZipAndPartialDirectory() = runBlocking {
        val source = temporaryFolder.newFile()
        source.writeBytes(conflictingZip())
        assertTrue(repository.importCustomZip(Uri.fromFile(source), "broken.zip").isFailure)
        assertNoTransferArtifacts()
        assertFalse(File(filesDir, "custom_themes.json").exists())
    }

    @Test fun missingImportSourceCleansDestination() = runBlocking {
        assertTrue(repository.importCustomZip(Uri.fromFile(File(filesDir, "absent.zip")), "absent.zip").isFailure)
        assertNoTransferArtifacts()
    }

    @Test fun importCopyFailureCleansTempFiles() = runBlocking {
        val uri = Uri.parse("content://theme-test/broken.zip")
        val stream = object : ByteArrayInputStream(ByteArray(32768)) {
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                if (pos > 0) throw IOException("source failed")
                return super.read(bytes, offset, length)
            }
        }
        shadowOf(context.contentResolver).registerInputStream(uri, stream)
        assertTrue(repository.importCustomZip(uri, "broken.zip").isFailure)
        assertNoTransferArtifacts()
    }

    @Test fun importCancellationIsRethrownAndCleansTempFiles() = runBlocking {
        val uri = Uri.parse("content://theme-test/cancel.zip")
        val cancellation = CancellationException("cancel import")
        val stream = object : ByteArrayInputStream(ByteArray(32768)) {
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                if (pos > 0) throw cancellation
                return super.read(bytes, offset, length)
            }
        }
        shadowOf(context.contentResolver).registerInputStream(uri, stream)
        try {
            repository.importCustomZip(uri, "cancel.zip")
            fail("Cancellation must escape Result")
        } catch (actual: CancellationException) {
            assertEquals(cancellation.message, actual.message)
        }
        assertNoTransferArtifacts()
    }

    @Test fun cancelledImportJobDoesNotPublishThemeOrMetadata() {
        val job = Job()
        val uri = Uri.parse("content://theme-test/job-cancel.zip")
        val stream = object : ByteArrayInputStream(validZip()) {
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                return super.read(bytes, offset, length).also { job.cancel() }
            }
        }
        shadowOf(context.contentResolver).registerInputStream(uri, stream)
        try {
            runBlocking(job) { repository.importCustomZip(uri, "cancel.zip") }
            fail("Cancelled job must throw")
        } catch (_: CancellationException) {
            assertNoTransferArtifacts()
            assertFalse(File(filesDir, "custom_themes.json").exists())
        }
    }

    @Test fun failedDownloadExtractionCleansZipAndPartialDirectory() = runBlocking {
        serve("/broken.zip", conflictingZip())
        assertTrue(repository.downloadTheme(theme().copy(downloadUrl = url("/broken.zip"))) {}.isFailure)
        assertNoTransferArtifacts()
    }

    @Test fun failedDownloadPreservesPreviouslyValidTheme() = runBlocking {
        val existing = repository.getThemeDir("theme")
        write(File(existing, "images/original.png"), "original")
        write(File(existing, "theme/token.json"), "old token")
        serve("/broken-replacement.zip", conflictingZip())

        assertTrue(repository.downloadTheme(theme().copy(downloadUrl = url("/broken-replacement.zip"))) {}.isFailure)
        assertEquals("original", File(existing, "images/original.png").readText())
        assertEquals("old token", File(existing, "theme/token.json").readText())
        assertEquals(emptyList<String>(), cacheDir.list()!!.toList())
        assertEquals(listOf("theme"), repository.getThemesDirectory().list()!!.toList())
    }

    @Test fun failedHttpDownloadLeavesNoArtifacts() = runBlocking {
        serve("/unavailable", "unavailable".toByteArray(), 503)
        assertTrue(repository.downloadTheme(theme().copy(downloadUrl = url("/unavailable"))) {}.isFailure)
        assertNoTransferArtifacts()
    }

    @Test fun downloadCancellationFromProgressEscapesAndCleansZip() = runBlocking {
        serve("/theme.zip", validZip())
        val cancellation = CancellationException("cancel download")
        try {
            repository.downloadTheme(theme().copy(downloadUrl = url("/theme.zip"))) { throw cancellation }
            fail("Cancellation must escape Result")
        } catch (actual: CancellationException) {
            assertEquals(cancellation.message, actual.message)
        }
        assertNoTransferArtifacts()
    }

    @Test fun cancelledDownloadJobDoesNotPublishExtractedTheme() {
        serve("/theme.zip", validZip())
        val job = Job()
        try {
            runBlocking(job) {
                repository.downloadTheme(theme().copy(downloadUrl = url("/theme.zip"))) { job.cancel() }
            }
            fail("Cancelled job must throw")
        } catch (_: CancellationException) {
            assertNoTransferArtifacts()
        }
    }

    @Test fun downloadsThroughRelative303And308Redirects() = runBlocking {
        redirect("/start", "middle", 303)
        redirect("/middle", "/theme.zip", 308)
        serve("/theme.zip", validZip())
        val progress = mutableListOf<Float>()
        val result = repository.downloadTheme(theme().copy(downloadUrl = url("/start"))) { progress.add(it) }
        assertTrue(result.toString(), result.isSuccess)
        assertTrue(repository.isThemeDownloaded(theme()))
        assertEquals(1f, progress.last())
        assertEquals(emptyList<String>(), cacheDir.list()!!.toList())
    }

    @Test fun redirectLoopAndMissingLocationFailWithoutArtifacts() = runBlocking {
        val requests = AtomicInteger()
        responses["/loop"] = {
            requests.incrementAndGet()
            MockResponse().setResponseCode(302).addHeader("Location", "/loop")
        }
        assertTrue(repository.downloadTheme(theme().copy(downloadUrl = url("/loop"))) {}.isFailure)
        assertTrue("Redirects must be bounded", requests.get() <= 6)
        assertNoTransferArtifacts()
        serve("/missing-location", byteArrayOf(1), 302)
        assertTrue(repository.downloadTheme(theme().copy(downloadUrl = url("/missing-location"))) {}.isFailure)
        assertNoTransferArtifacts()
    }

    @Test fun catalogFollowsRelative308Redirect() = runBlocking {
        redirect("/catalog-start", "catalog", 308)
        catalogBody = catalog(theme("remote"))
        assertEquals(listOf("remote"), ThemeRepository(context, url("/catalog-start")).getCommunityThemes(true).map { it.id })
    }

    @Test fun unsafeDownloadIdCannotDeleteOutsideDirectory() = runBlocking {
        write(File(filesDir, "outside/keep.txt"), "keep")
        serve("/theme.zip", validZip())
        assertTrue(repository.downloadTheme(theme("../outside").copy(downloadUrl = url("/theme.zip"))) {}.isFailure)
        assertEquals("keep", File(filesDir, "outside/keep.txt").readText())
        assertNoTransferArtifacts()
    }

    private fun theme(id: String = "theme") = CommunityTheme(id, id, "test", defaultTargetUuid = "target")
    private fun catalog(vararg themes: CommunityTheme) = Json.encodeToString(themes.toList())
    private fun catalogCache() = File(filesDir, "remote_community_catalog.json")
    private fun cacheCatalog(vararg themes: CommunityTheme) = catalogCache().writeText(catalog(*themes))
    private fun assetCommunityThemes() = context.assets.open("community_catalog.json").bufferedReader().use {
        Json.decodeFromString<List<CommunityTheme>>(it.readText())
    }
    private fun url(path: String) = server.url(path).toString()
    private fun write(file: File, content: String) { file.parentFile!!.mkdirs(); file.writeText(content) }
    private fun serve(path: String, bytes: ByteArray, status: Int = 200) {
        responses[path] = { response(bytes, status) }
    }
    private fun redirect(path: String, location: String, status: Int) {
        responses[path] = { MockResponse().setResponseCode(status).addHeader("Location", location) }
    }
    private fun response(bytes: ByteArray, status: Int = 200) = MockResponse()
        .setResponseCode(status).setBody(Buffer().write(bytes))
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
    private fun validZip() = zip("images/image.png" to byteArrayOf(1), "theme/token.json" to "{}".toByteArray())
    private fun conflictingZip() = zip(
        "theme/token.json" to "{}".toByteArray(),
        "images" to byteArrayOf(1),
        "images/image.png" to byteArrayOf(1)
    )
    private fun assertNoTransferArtifacts() {
        assertEquals(emptyList<String>(), cacheDir.list()!!.toList())
        assertEquals(emptyList<String>(), repository.getThemesDirectory().list()!!.toList())
    }
}
