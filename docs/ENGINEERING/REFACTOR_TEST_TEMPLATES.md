# Template di test per il refactor (SOLID)

Obiettivo: dare convenzioni e template eseguibili per ogni sezione del refactor (Quick Wins, Use-Cases, Repository split, ViewModel/orchestrator, Integration tests, DB migrations, PDF generator). Fornisce esempi con JUnit + MockK + Kotlin coroutines test + MockWebServer.

---

Indice veloce
- Unit tests (UseCase / Small components)
- Repository tests (unit + integration con MockWebServer)
- ViewModel tests
- Orchestrator (integration-style) tests
- PDF generator tests (parsing + snapshot)
- Room migration tests
- CI/Smoke tests & gating

---

Principi generali
- Usa JUnit4 / StandardTestDispatcher / kotlinx-coroutines-test per coroutines deterministiche.
- MockK per mocking (relaxed = true quando ha senso) — coerente con il repo.
- Separare unit test (mocked I/O), integration test (MockWebServer) e e2e/acceptance (strumentati o emulatori).
- Test nomi: GIVEN_When_THEN (o should_when_given) — preferire chiarezza e predicabilità.
- Test coverage: ogni UseCase deve avere test happy-path, edge case, timeout/cancellation, e error mapping.

---

1) Unit test template — UseCase (single responsibility)

File: app/src/test/java/com/app/miklink/domain/ApplyNetworkConfigUseCaseTest.kt

Scaffold consigliato:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ApplyNetworkConfigUseCaseTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: ProbeRepository // interfaccia
    private lateinit var sut: ApplyNetworkConfigUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
        sut = ApplyNetworkConfigUseCase(repo)
    }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `GIVEN dhcp not bound WHEN apply THEN returns success`() = runTest(dispatcher) {
        // arrange
        val probe = sampleProbe()
        coEvery { repo.getDhcpCliStatus(probe, any()) } returns null
        coEvery { repo.addDhcpClient(probe, any()) } returns Unit

        // act
        val result = sut.execute(probe, sampleClient())

        // assert
        assertTrue(result is Result.Success)
        coVerify { repo.addDhcpClient(probe, any()) }
    }
}
```

Test di sicurezza: testa anche che l'UseCase non logghi password in chiaro e che non mutazioni state se exception.

---

2) Repository tests — unit + integration

- Unit tests: Mock retrofit/service by mocking `MikroTikApiService` and validate mapping logic and error transformation.
- Integration tests: use MockWebServer to simulate MikroTik REST endpoints, including slow responses, error codes, and self-signed HTTPS.

Integration example (MockWebServer)

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MikroTikServiceIntegrationTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var repository: NetworkRepository // uses Retrofit created with mockServer.url("/")

    @Before fun setup() { mockServer = MockWebServer(); mockServer.start() }
    @After fun teardown() { mockServer.shutdown() }

    @Test
    fun `WHEN API returns board-name THEN repository parse correctly`() = runTest {
        val body = "[{\"boardName\":\"RB750\"}]"
        mockServer.enqueue(MockResponse().setBody(body).setResponseCode(200))

        val result = repository.getSystemResource(listOf("board-name"))

        assertTrue(result is Resource.Success)
        assertEquals("RB750", (result as Resource.Success).data.first().boardName)
    }
}
```

Considerazioni HTTPS: MockWebServer consente TLS con certificati auto-generati; testate sia http che https modes and assert OkHttp sslBuilder is configurable.

---

Example: MikroTikServiceFactory integration tests

MockWebServer is a great fit to validate Retrofit/OkHttp factory behaviour. Key checks to include:

- Base URL construction when `probe.isHttps` is true/false
- Authorization header injection when `username`/`password` are provided
- Timeouts and delayed responses to verify client behavior
- 4xx/5xx responses mapping to exceptions

TLS note — when testing HTTPS you can generate a temporary server certificate and let the client trust it inside the test (see `okhttp-tls` HandshakeCertificates/HeldCertificate helpers). Minimal example:

```kotlin
// Generate server certificate
val serverHeld = HeldCertificate.Builder().addSubjectAlternativeName(mockServer.hostName).build()
val serverCerts = HandshakeCertificates.Builder().heldCertificate(serverHeld).build()
mockServer.useHttps(serverCerts.sslSocketFactory(), false)

// Create client trusting the server certificate
val clientCerts = HandshakeCertificates.Builder().addTrustedCertificate(serverHeld.certificate).build()
val client = OkHttpClient.Builder().sslSocketFactory(clientCerts.sslSocketFactory(), clientCerts.trustManager).hostnameVerifier { _, _ -> true }.build()

// Use client into Retrofit builder and run the same assertions as the HTTP case
```

Add such integration checks for every factory and repository that builds Retrofit clients dynamically.

Connectivity provider tests

- For TCP checks, prefer using `MockWebServer` as a reachable TCP endpoint for "connect" tests.
- For refused/timeouts use a short timeout and connect to an unused/high port or non-routable address; assert false on exception/timeouts.


---

3) ViewModel tests

- Isolare orchestration con mocked UseCases/Repositories.
- Usa StandardTestDispatcher + runTest; test flows using StateFlow.value. Preferare small unit tests for each step (network config apply success/fail, link status no-link stops pipeline, runPing called only when link ok, etc.).

Esempio già presente: `TestViewModelStartFlowTest.kt` — seguirne pattern e aggiungere test di cancellazione e state transitions.

---

4) Orchestrator / TestOrchestrator tests

- Orchestrator coordina UseCases: testarlo con integration style unit tests, mocking UseCases but asserting correct sequencing, early-exit, and side-effects (calls to repository, database writes, report persisted).

Template:

```kotlin
class TestOrchestratorTest {
   @Test
   fun `GIVEN link no-link WHEN run THEN orchestrator ends early and calls persistReport with FAIL`() {
       // mock usecases: ApplyNetworkConfig success, GetLinkStatus returns no-link
       // verify orchestrator calls emitImmediateFail and persists report with FAIL
   }
}
```

---

5) PDF generator tests

- Unit tests for `parseResults` parsing backward compatible JSON (use moshi real adapter), snapshot tests for document layout are optional (binary diffs are brittle) — prefer tests that validate key converted values, presence of tables and safety against malformed JSON.

Example:

```kotlin
@Test
fun `parseResults accepts legacy map and returns ParsedResults`() {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val pdf = PdfGeneratorIText(context, moshi)
    val sampleJson = // legacy map string
    val parsed = pdf.parseResults(sampleJson)
    assertNotNull(parsed)
    assertFalse(parsed?.ping.isNullOrEmpty())
}
```

---

6) Room Migration tests

- For schema changes (ProbeConfig single-probe), add Room autoMigration tests or manual migration tests: build DB with old schema + insert data + run migration + assert transformed state.

Snippet (androidTest):

```kotlin
// use Room.inMemoryDatabaseBuilder and supportSetup + MigrationTestHelper
```

---

7) CI & gating

- Pipeline stages:
  1. lint + static analysis + detect-secrets
  2. unit tests
  3. integration tests (MockWebServer) — run in a separate job with longer timeout
  4. instrumentation (optional)

Add test coverage gate for modified packages (e.g., repo + usecases + viewmodel must keep coverage >= X%).

---

8) Naming & structure recommendations
- Test file name pattern: ClassNameTest or FeatureActionTest
- Place tests next to package under app/src/test/java (unit) or app/src/androidTest/java (instrumented / Room migrations)
- Use fixtures in testFixtures directory or object provider functions in tests/helpers/TestFixtures.kt

---

Se vuoi, posso creare esempi concreti di file test (unit + integration/mocks) per i Quick Wins (MikroTikServiceFactory, ConnectivityProvider, ParsedResultsParser) e aggiungerli alla repository come PR-ready patch — confermi che proceda con quei test di esempio ora? 
