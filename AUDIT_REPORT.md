# USB2K / USB Media Explorer — Independent Repository Audit

> **Auditor:** Arena Agent (senior-engineer review) · **Date:** 2026-09-09 (UTC)
> **Repo:** `hashmllalan-cpu/USB2K` · **Branch inspected:** `arena/01a0861f-usb2k`
> **Commit inspected:** `a97aad1bcd169cb79ef754f254b6bafdd224a554` (`a97aad1 build(deps): bump androidx.navigation:navigation-compose (#2)`)
> **Method:** full static read of all 144 tracked files (~21.3k lines Kotlin), resource/secret/permission scans, Gradle/CI forensics, live GitHub Actions run inspection, attempted build/test execution.

---

## 0. Executive summary

**USB Media Explorer is a well-architected, offline-first Android file/media explorer (Kotlin + Compose + Media3) with genuinely strong engineering in its storage abstraction, staged file operations, and secrets hygiene. It is *not* shippable today: the CI verification pipeline has never been green in observable history, the release-signing secret is missing so every release build fails in 33 seconds, and dependabot PRs were merged on red checks.**

| Area | Verdict | One-line reason |
|---|---|---|
| Architecture & code quality | ✅ Strong | Clean `DocProvider` abstraction, staged ops + journal, bounded thumbnail pipeline |
| Security posture (static) | ✅ Good | No secrets, no `MANAGE_EXTERNAL_STORAGE`, zip-slip guards, no network/telemetry |
| Build reproducibility | 🔴 Blocked | `Verify` fails at **Unit tests** on every observed run; cause needs CI log read |
| Release pipeline | 🔴 Blocked | `Build APK` fails at **Materialize signing keystore** — `USBMEDIA_KEYSTORE_B64` missing |
| Process / branch safety | 🔴 Weak | Red-check PRs merged; no releases published; single squashed commit (no history) |
| Tests | 🟡 Thin | 17 JVM test files cover pure logic only; 1 smoke test; no Robolectric/ViewModel coverage |
| Docs | 🟡 Good but drifting | Excellent Arabic docs; 5+ stale references (SDK 35→36, missing files, debug-APK claims) |
| Legal / store readiness | 🔴 Missing | No `LICENSE`, no privacy policy — both block any public distribution |

**The single most important sentence of this audit:** *open the failing `Verify → Unit tests` step log in the GitHub UI (run `34348426034` or `34346856877`) — that one log determines whether the repo needs a 1-hour dependency fix or a deeper repair — and only then proceed down the roadmap in §12.*

Prior remediation context: `remediation_status.md` (2026-09-08) reached the same "blocked by environment" conclusion for local builds and fixed only docs/tests. This audit supersedes it with live CI evidence.

---

## 1. Provenance — what was inspected, exactly

```bash
git clone https://github.com/hashmllalan-cpu/USB2K.git
cd USB2K
git checkout arena/01a0861f-usb2k
git rev-parse HEAD   # a97aad1bcd169cb79ef754f254b6bafdd224a554
git log --oneline    # single squashed commit (no prior history)
git fetch origin main && git diff --stat HEAD origin/main  # empty: branch == main
```

- Remote: `https://github.com/hashmllalan-cpu/USB2K.git` (fetch+push).
- `origin/main` and `arena/01a0861f-usb2k` both point at `a97aad1`; the working tree was clean.
- GitHub PRs #1–#6 (dependabot) show as MERGED but git history contains one squashed commit — **history was rewritten**, so `git blame`/archaeology is unavailable and PR metadata can't be mapped to commits. All process conclusions below are drawn from behavior (run conclusions, merge states), not from history.
- Live CI state was queried via `gh run list` / `gh run view --json` on 2026-09-09. Full step logs could not be downloaded from this sandbox (network allowlist blocks `results-receiver.actions.githubusercontent.com`), but per-step pass/fail is available and cited with run IDs.

---

## 2. Inventory — languages, frameworks, runtimes, entry points

### 2.1 Languages & size

| Item | Value |
|---|---|
| Production language | **Kotlin only** (no Java sources) |
| Kotlin LOC | **~21,283 lines** across ~100 `.kt` files (`main` + `test` + `androidTest`) |
| Tracked files | **144** total |
| Build scripts | Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`) |
| Resources | XML drawables/strings/themes (427 strings × EN+AR, full parity) |
| Docs | Markdown, Arabic-first (`README.md`, `docs/*.md`, `remediation_status.md`) |
| Docker / K8s / backend | **None** — correct for a pure Android client |

### 2.2 Frameworks & dependencies (from `gradle/libs.versions.toml`)

| Component | Version | Notes |
|---|---|---|
| Android Gradle Plugin | 8.7.3 | |
| Kotlin | 2.0.21 | Compose plugin `org.jetbrains.kotlin.plugin.compose` |
| Gradle wrapper | 8.9 (jar **committed**, SHA-256 pinned) | ✅ supply-chain good |
| `compileSdk` / `targetSdk` | **36** | ⚠️ docs still say 35 (DOC-01) |
| `minSdk` | 24 (Android 7.0) | |
| Compose BOM | **2026.08.00** | Very new; with Kotlin 2.0.21 this combo is *unverified* — candidate cause of red Verify |
| Navigation Compose | 2.10.0 | Just bumped by the inspected commit |
| Media3 (ExoPlayer/UI/DataSource/Common) | 1.11.0 | Player core |
| Coil (+GIF) | 2.7.0 | Thumbnails via custom `ThumbFetcher` |
| DataStore Preferences | 1.1.1 | App settings |
| DocumentFile | 1.1.0 | (Available; SAF mostly via `DocumentsContract` directly) |
| Coroutines | 1.11.0 | |
| Okio | 3.18.2 | |
| ExifInterface | 1.4.2 | EXIF rotation for thumbnails |
| Lifecycle / Activity-Compose / AppCompat | 2.8.7 / 1.9.3 / 1.7.0 | AppCompat required for pre-33 per-app language |
| JUnit4 / Espresso / UIAutomator-test | 4.13.2 / 3.6.1 / — | Test-only |
| DI framework | **None (hand-rolled `AppContainer`)** | Deliberate, justified, appropriate at this scale |

Dependency graph: no Hilt/KSP, no Room, no Retrofit/OkHttp direct use, no Firebase — the graph is small and fully offline.

### 2.3 Runtimes & package management

- **JDK 17** (Temurin in CI; `sourceCompatibility/targetCompatibility = 17`, `jvmTarget = 17`).
- **Android SDK**: platform 36 + build-tools (auto-provisioned by AGP on CI; must be installed locally — see §5.3).
- Repositories: `google()` + `mavenCentral()` only, with `FAIL_ON_PROJECT_REPOS` ✅.
- No Gradle dependency verification (`verification-metadata.xml` missing — finding SEC-04).

### 2.4 Entry points (all in `app/src/main`)

| Entry | File | Role |
|---|---|---|
| `UsbMediaExplorerApp : Application` | `UsbMediaExplorerApp.kt` | Builds `AppContainer`, installs crash handler, `ImageLoaderFactory` |
| `MainActivity` (exported, `singleTask`) | `MainActivity.kt` | Sole UI host; handles `ACTION_VIEW` video, `USB_DEVICE_ATTACHED`, transfers deep-link |
| `CrashReportActivity` (`:crash` process, not exported) | `CrashReportActivity.kt` | Post-crash report screen; survives main-process death |
| `FileOpsService` (foreground, `dataSync`) | `data/ops/FileOpsService.kt` | Long copy/move/zip jobs + progress notification |
| `VolumeEventReceiver` (not exported) | `data/volume/VolumeEventReceiver.kt` | Manifest-registered `MEDIA_*` mount broadcasts |
| `OpsActionReceiver` (not exported) | `data/ops/FileOpsService.kt:158` | Notification Pause/Cancel actions |
| `FileProvider` (`${applicationId}.fileprovider`) | Manifest + `res/xml/file_paths.xml` | Share/open-with URIs (⚠️ paths overly broad — SEC-02) |

---

## 3. Directory map & annotated file-level mapping

```
USB2K/
├── README.md                    # Product overview (Arabic). P0 setup truth lives here + keystore/README.
├── remediation_status.md        # Prior 2026-09-08 audit (Arabic). Superseded by this report for CI status.
├── AUDIT_REPORT.md              # ← this file
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradle/
│   ├── libs.versions.toml       # Single source of truth for all dependency versions ✅
│   └── wrapper/                 # gradle-wrapper.jar COMMITTED + distributionSha256Sum ✅
├── keystore/README.md           # "Never commit keys" policy; CI materializes keystore/ci.p12 (gitignored) ✅
├── .github/
│   ├── dependabot.yml           # Weekly Gradle + Actions updates ✅ (proven working: PRs #1–#6)
│   └── workflows/
│       ├── verify.yml           # PR/main gate: unit tests + lint + debug build + API 29/35 emulator smoke
│       └── build-apk.yml        # Tags/manual: debug+release APK → apk-latest release (needs secrets)
├── docs/                        # Arabic deep-docs: ARCHITECTURE, BUILD, TEST_MATRIX, SAF/OPS/SIGNING reviews…
└── app/
    ├── build.gradle.kts         # Flavors, secret-only release signing, R8, resourceConfigs en+ar ✅
    ├── proguard-rules.pro       # Media3 keeps, ops keeps, line numbers for crashes
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/usbmediaexplorer/
        │   │   ├── UsbMediaExplorerApp.kt / MainActivity.kt / CrashReportActivity.kt
        │   │   ├── di/AppContainer.kt            # 152 L. Entire object graph, lazy singletons, appScope ✅
        │   │   ├── data/doc/      (9 files)      # ★ Storage abstraction — the app's best module
        │   │   ├── data/volume/   (5 files)      # Volume discovery, SAF grants, mount monitoring
        │   │   ├── data/thumb/    (9 files)      # ★ Thumbnail pipeline — bounded, cached, well-designed
        │   │   ├── data/metadata/ (4 files)      # MediaMetadataReader + cached MetadataRepository
        │   │   ├── data/ops/      (8 files)      # ★ Staged file ops + journal + foreground service
        │   │   ├── data/search/   (2 files)      # Budgeted recursive search w/ snapshot cache
        │   │   ├── data/settings/ (2 files)      # DataStore-backed AppSettings (single settings flow)
        │   │   ├── data/store/    (6 files)      # JsonStore: atomic JSON docs w/ backup+restore ✅
        │   │   ├── ui/            # Compose screens: AppRoot, nav, home, browse, player, viewer,
        │   │   │                 # search, library, settings, ops, theme, common design system
        │   │   └── util/          (10 files)     # Dispatchers, Formatters, Bitmaps, Permissions, …
        │   └── res/               # 16 drawables, launcher icons, 427×2 strings, 5 xml configs
        ├── test/                  # 17 JVM test files — pure-logic coverage (see §6.2)
        └── androidTest/           # 1 smoke test (MainActivity launches)
```

### Annotated module notes (what each part does well / where it hurts)

| Module | Files | Assessment |
|---|---|---|
| `data/doc` | `DocNode`, `DocProvider`, `FileDocProvider` (249 L), `SafDocProvider` (321 L), `DocRepository` (202 L), `DocUri`, `DocSorter`, `DocRelation`, `MediaKind` | **Excellent.** One immutable `DocNode` unifies `file://` and SAF `content://`; `DocRepository` routes by scheme, synthesizes breadcrumbs without N queries, and never leaks `file://` (FileProvider). `DocRelation` self-copy guard is pure & tested. Minor: `FileDocProvider.directorySize` unbounded/uncancellable (PERF-01). |
| `data/volume` | `VolumeRepository` (443 L), `VolumeMonitor`, `VolumeEventBus`, `VolumeEventReceiver`, `VolumeInfo` (+ `FileSystemProbe` object inside Repository file) | **Strong.** Precedence grant→mount→permission-card; runtime + manifest receivers funnel into a `SharedFlow` bus; hidden-API reflection (`getUuid`/`getDirectory`) has safe fallbacks. Dead `hasAllFilesAccess` branch (P0-3). |
| `data/thumb` | `ThumbnailRepository` (255 L), `ThumbnailCache` (266 L), `VideoFrameExtractor` (246 L), `ImageThumbExtractor`, `FolderCoverExtractor` (178 L), `CoverRules` (171 L, pure+tested), `AudioArtExtractor`, `CoilSetup`, `ThumbModels` | **Strong.** AUTO frame scoring with early exit, header-only cover ranking, `md5(uri\|size\|mtime\|…\|engine=5)` cache keys, LRU prune, negative cache, per-key `Mutex` map (256-cap), `limitedParallelism(3)` dispatcher. Correctly reads from USB fds, never copies. |
| `data/ops` | `FileOpsEngine` (614 L), `FileOpsManager` (402 L), `FileOpsService` (173 L), `OpsSafety` (pure+tested), `OpsIntegrity` (SHA-256), `OpsJournal` (200-entry cap ✅), `OpModels`, `OpsNotifications` | **Strong.** Hidden tokenized staging names → verify → rename; copy+delete fallback cross-volume; zip-slip + depth/segment/space quotas; pause/resume/cancel; per-volume `Mutex`; journal recovery of STAGING leftovers. Engine is the biggest data file — consider splitting zip vs copy paths later (P2). |
| `data/search` | `SearchEngine` (230 L), `SearchBudget` | **Good.** Cold progressive `Flow`, iterative walk, `MAX_WALK_NODES=15k` + 10 s budget, snapshot cache invalidated on ops completion. `@Volatile` snapshot benign. |
| `data/store` | `JsonStore` + 5 typed stores + migrations | **Good.** Atomic tmp-write + `.bak` + `.corrupt.TIMESTAMP` quarantine + `schemaVersion` migrations + diagnostics flow. Right call vs Room at this scale. Default `CoroutineScope(Dispatchers.IO)` param hurts testability slightly (P3). |
| `data/settings` | `AppSettings`, `SettingsRepository` (181 L) | **Good.** Typed keys, `catch{emptyPreferences}`, per-field setters. |
| `data/metadata` | Reader/Repository/Store | **Good.** fd-based `MediaMetadataRetriever`, cached, `limitedParallelism(2)`. |
| `di` | `AppContainer` | **Good.** Lazy, ordered, documented; `onAppStart` wires cache-budget + search-invalidation observers with crash-safe `runCatching`. |
| `ui/browse` | ViewModel **1025 L**, Screen 780 L, TopBar/Content/SheetsHost + components | **Works, but largest UI debt.** State assembled from combined flows with debounce/conflate ✅; 1025-line ViewModel + 780-line Screen need splitting (UI-01). |
| `ui/player` | ViewModel 524 L, Screen 641 L, `PlaybackFailure` | **Good.** Direct `content://` playback, folder playlist, track/subtitle selection, resume prompt, classified errors — but classifier has a **real substring bug** (`"RESOURCE"` contains `"SOURCE"`, BUG-01). |
| `ui/home` | Screen **1056 L**, Card 502 L, VM 265 L | **Debt.** Screen does too much; extract sections. |
| `ui/settings` | Screen **1211 L** | **Biggest file in repo.** Must split into per-section composables + rows file (UI-01). |
| `ui/common` | `DesignSystem` 723 L, `Dialogs` 344 L, `MediaIcons`, `LocalApp`, `ThumbModels` | **Good.** Centralized tokens/states; `StateBlock`/skeletons used consistently. |
| `ui/nav` | `Navigation` (Routes+Navigator), `AppNavHost` | **Good.** `launchSingleTop`/`restoreState`/`popUpTo`, breadcrumb-aware back-stack. |
| `ui/viewer/search/library/ops/theme` | Viewer 330 L, Search 270 L, Library 668 L, Transfers 237 L, theme ×4 | **Good.** No anomalies; Library file could split Favorites/Recent later. |
| `util` | 10 small files | **Good.** `AppDispatchers` centralizes threading; `Permissions` has precise per-API logic (except dead all-files branch). |
| `res/` | 16 drawables, 427×2 strings, themes, 5 xml | **Excellent hygiene.** 0 broken `R.*` refs (verified); EN/AR parity 427/427 (verified). Only 4 hardcoded strings (crash screen, I18N-01). |

---

## 4. Data flows & external integrations

### 4.1 Runtime data flows (all on-device; **zero network calls** — verified by grep: no HTTP client, WebView, or cleartext config)

1. **Volume discovery:** `VolumeMonitor` (USB attach/detach + `MEDIA_*` + `StorageVolumeCallback`) and manifest `VolumeEventReceiver` → `VolumeEventBus` → `VolumeRepository.refresh()` → precedence: persisted SAF grant → readable `/storage/XXXX-YYYY` → `NEEDS_PERMISSION` card with picker intent → `takePersistableUriPermission` (+ session fallback) → `READY`.
2. **Browsing:** `BrowseViewModel(folderUri)` → `DocRepository.children()` → `FileDocProvider` (`listFiles` probe) or `SafDocProvider` (single-cursor projection query) → sort/filter in memory → `DocGrid` + Coil `AsyncImage(ThumbRequest)`.
3. **Thumbnails:** UI → `ThumbnailRepository.thumbnail()` → `ThumbnailCache` (memory via Coil + `cacheDir/thumbs/<md5>.webp` + JSON index) → miss → per-kind extractor on `thumbnail` dispatcher (3-way) → `VideoFrameExtractor` (MMR over SAF fd → AUTO scoring → `ContentResolver.loadThumbnail` fallback → embedded cover → type icon) / `ImageThumbExtractor` (ImageDecoder → BitmapFactory+EXIF) / `FolderCoverExtractor` (`coverScan` → `CoverRules.rank` → header-only dimension check → winning decode).
4. **Playback:** `PlayerViewModel` builds Media3 `ExoPlayer` directly on the source `content://` URI; playlist = folder videos in browser sort order; subtitles auto-detected (`srt/ass/ssa/vtt` sibling match) + manual add via `replaceMediaItem`; positions persisted every ~10 s + on stop; `VolumeEvent.Detached` → `SOURCE_UNAVAILABLE`.
5. **File operations:** UI → `FileOpsManager` (job registry, `JobProgress` w/ `SpeedTracker` ETA, clipboard, per-volume locks) → `FileOpsEngine` (256 KB streaming copy File↔SAF, staged hidden names, size + optional SHA-256 verify ≤64 MB, unique-name resolution, zip-slip-guarded unzip under budget) → `OpsJournal` (`begin→finish`, STAGING recovery on restart) → `FileOpsService` foreground notification; completion invalidates search snapshot + thumbnails + metadata.
6. **Search:** `SearchEngine.search(root, query)` cold flow → iterative walk (15k nodes / 10 s budget, cooperative cancel) → progressive `SearchResult` emissions; finished walk cached as snapshot for keystroke filtering.
7. **Settings/i18n/theme:** `SettingsRepository.settings: Flow<AppSettings>` → `MainActivity` applies `AppCompatDelegate` locales + `UsbMediaExplorerTheme` once above `AppRoot`; `supportsRtl=true`, `locales_config.xml`, `resourceConfigurations en+ar`.

### 4.2 External integrations

| Integration | Type | Status |
|---|---|---|
| Android system: SAF/DocumentsContract, StorageManager, UsbManager, MediaMetadataRetriever/ImageDecoder, MediaStore-free | Runtime | ✅ core design |
| Google Maven / Maven Central / `services.gradle.org` | Build-time only | Required for first build; **blocked in this sandbox** (§6) |
| GitHub Actions (ubuntu-latest, Temurin 17, emulator images) | CI-only | Configured; currently red (§7) |
| Backend / cloud / analytics / crash upload / ads | — | **None. The app is fully offline; crash reports are user-shared, never uploaded.** ✅ privacy-positive |

---

## 5. Environment variables, secrets, containers, setup

### 5.1 Required variables (release signing — secret-only, no fallback ✅)

| Variable | Source | Purpose |
|---|---|---|
| `USBMEDIA_KEYSTORE_PATH` | env (CI: `$GITHUB_WORKSPACE/keystore/ci.p12`) | PKCS12 keystore file |
| `USBMEDIA_STORE_PASSWORD` | env / `secrets.USBMEDIA_STORE_PASSWORD` | Keystore password |
| `USBMEDIA_KEY_ALIAS` | env / `secrets.USBMEDIA_KEY_ALIAS` | Key alias |
| `USBMEDIA_KEY_PASSWORD` | env / `secrets.USBMEDIA_KEY_PASSWORD` | Key password |
| `USBMEDIA_KEYSTORE_B64` | **GitHub secret only** | Base64 keystore materialized to `keystore/ci.p12` |

Verified: `app/build.gradle.kts` reads env-only, `packageRelease.doFirst{check(...)}` fails loudly, `.gitignore` covers `*.p12/*.jks/*.keystore` + `keystore/ci.p12` (verified via `git check-ignore`), **zero** key files committed, no passwords/tokens in source (secret scan clean — only staging-`token` and design-`token` false positives).

**Proven gap:** the `Build APK` run on current HEAD (`34349094953`, 2026-09-09) failed at the `Materialize signing keystore` step → **`USBMEDIA_KEYSTORE_B64` is not configured.** Release is 100% blocked. (Secret names themselves couldn't be listed: API returns 403 for this integration.)

### 5.2 Docker / Kubernetes

**None, and none needed.** This is a pure Android client. No `Dockerfile`, no K8s manifests, no backend to containerize. CI runs on GitHub-hosted runners. Do not add containers; the "reproducible environment" story is JDK 17 + Android SDK + committed Gradle wrapper.

### 5.3 Local setup — what works, what's missing

Documented prerequisites (README + `docs/BUILD.md`): JDK 17, Android SDK, `./gradlew` commands. **Missing / stale setup steps:**

1. No SDK package list — onboarding needs: `platforms;android-36`, `build-tools;36.x`, `platform-tools`, plus emulator images `system-images;android-29;google_apis;x86_64` and `…;android-35;…` for the smoke matrix.
2. No `local.properties` / `ANDROID_HOME` guidance, no license-acceptance step (`sdkmanager --licenses`).
3. `docs/BUILD.md` says SDK **35**; build requires **36** (DOC-01).
4. Signing doc chain is broken: `keystore/README.md` and `build-apk.yml` reference **`SETUP_SIGNING.md`, which does not exist** (DOC-02). The real instructions live in `README.md` §6.
5. `README.md` references **`docs_backup_20260908T042855Z.tar.gz`, which is not in the repo** (DOC-03).
6. `docs/BUILD.md` §0 still tells users to download a **debug APK from the `apk-latest` release** — the workflow now publishes **only `app-release.apk`** there (debug is an Actions artifact). Stale (DOC-04). `gh release list` is also empty: **no release exists yet**, so all download links are currently dead.

---

## 6. Build & test — execution evidence

### 6.1 Local execution (this sandbox, 2026-09-09)

| Command | Result | Output (verbatim) |
|---|---|---|
| `which java javac sdkmanager adb apksigner` | ❌ all missing | `java: command not found` |
| `echo $JAVA_HOME $ANDROID_HOME $ANDROID_SDK_ROOT` | ❌ all empty | |
| `curl -sI https://services.gradle.org/` | ❌ blocked | empty (allowlist) |
| `curl -sI https://github.com` | ✅ 200 | (only github/pypi hosts allowed) |
| `timeout 25 ./gradlew --version` | ❌ fails, fast | `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.` |
| `./gradlew :app:assembleDebug` | ⛔ not attempted beyond wrapper check | Requires JDK+SDK+network; outcome predetermined by above |
| `./gradlew :app:testDebugUnitTest` / `lintDebug` / `connectedDebugAndroidTest` | ⛔ blocked | Same |

**Reproduction (any reviewer):** fresh checkout → `java -version` (absent) → `./gradlew --version` → same error. Conclusion matches `remediation_status.md` §2: **local build/test is `Blocked by Environment`, not by code.** No build/test success is claimed anywhere in this report.

### 6.2 Static test inventory (what *would* run in CI)

- **17 JVM test files** (`app/src/test`): `CoverNames/Rules`, `Formatters`, `NavigationRoutes`, `PlaybackAndProgress`, `PlaybackFailure`, `QaContract` (+`FakeDocProvider`/`FakePlaybackFixtures`), `RenameAndSort`, `DocRelation`, `OpsIntegrity`, `OpsSafety`, `SearchBudget`, `SearchFilter`, `ViewModeAndScale`, `JsonStoreMigration`. All pure-logic, hermetic — good design, but they cover **~10% of behavior**: no `DocRepository`, `VolumeRepository`, `SearchEngine`, `FileOpsEngine`, `ThumbnailRepository`, or ViewModel coverage (all need Android/Robolectric).
- **1 instrumented test**: `ApplicationSmokeTest.mainActivityLaunches` — correct minimal gate for the API 29/35 matrix.
- Not present: Robolectric, Turbine, screenshot tests, Macrobenchmark/Baseline Profile, fuzzing for zip/doc-uri parsing.

### 6.3 What CI actually proves (live runs — the critical evidence)

| Run | Workflow / trigger | Conclusion | Failed step(s) |
|---|---|---|---|
| `34349094953` (HEAD `a97aad1`, today) | Build APK / `workflow_dispatch` | ❌ failure in **33 s** | `Materialize signing keystore from secrets` → secrets missing (P0-2, proven) |
| `34348426034` | Verify / PR (nav 2.10.0 bump) | ❌ failure in 16m47s | `verify → Unit tests` + both emulator smokes at `Run instrumented tests` |
| `34346856877` | Verify / push to main | ❌ failure in 59 s | `verify → Unit tests` + both smokes at `Install Android emulator image` (infra flake) |
| `34347509273` | Verify / PR (setup-gradle 6.3.0) | ❌ failure in 20m46s | `verify → Unit tests` + both emulator smokes |
| 4 more main-push Verify runs | Verify | ⚠️ cancelled | Concurrency `cancel-in-progress` churn from back-to-back merges |

Step logs were not downloadable from this sandbox (allowlist blocks the log host), so the *root cause inside* `Unit tests` is unconfirmed. Ranked hypotheses: (1) dependency resolution/version conflict (BOM `2026.08.00` vs Kotlin 2.0.21 vs AGP 8.7.3 is an unusual combo); (2) Kotlin/Compose compiler mismatch; (3) genuine test assertion failure; (4) runner/SDK environment issue. **Every hypothesis is resolved by reading one step log in the GitHub UI — that is remediation step #1 (§12, Phase 0).**

Process facts established: **no green `Verify` in the observable window; 5 dependabot PRs merged while red; `apk-latest` release was never created;** rapid merges + `cancel-in-progress: true` mean main-push verification rarely completes.

---

## 7. CI/CD, deployment & supply chain

### 7.1 `verify.yml` (PR + push to `main`)

Good bones: `contents: read`, SHA-pinned `checkout`/`setup-java`/`setup-gradle` ✅, 30-min timeout, unit→lint→build order, API 29+35 emulator matrix with `fail-fast: false`. Issues:

- **VRF-01 (P1) — ✅ fixed 2026-09-11:** `reactivecircus/android-emulator-runner@v2` was a **floating tag**, breaking the otherwise strict SHA-pinning policy. Now pinned to `a421e438…`, the commit the `v2` tag resolves to (= release v2.38.0, verified through the tags API).
- **VRF-02 (P1) — ✅ fixed 2026-09-11:** `setup-gradle` SHA **differed between workflows** (`ed408507…`, a Sept-2025 commit, in verify.yml vs `9c971963…` in build-apk) while both carried a wrong `# v4.4.0` label. Both workflows now use one SHA — `9c971963…`, which the tags API resolves to **v6.3.0** — with corrected labels; dependabot PR #15 already proved that SHA green on the `verify` job (run `34373598827`).
- **VRF-03 (P2):** full 2-API emulator matrix on *every PR* is slow/expensive (~2×45 min). Gate smokes to `main` pushes + labeled PRs, or keep one API on PRs.
- **VRF-04 (P1):** checks are evidently **not required** (red PRs merged). Enable branch protection on `main`: require `verify` + smoke, 1 review, no force-push; protect `v*`/`apk-latest` tags. (Setting unverifiable from here — API 403 — so this is behavioral evidence + recommendation.)

### 7.2 `build-apk.yml` (tags `v*` + manual dispatch)

Good bones: tag/manual-only trigger with `if:` guard, secrets→ephemeral `keystore/ci.p12`, explicit `app-release.apk` path with `fail_on_unmatched_files` (unsigned APK can never publish ✅), legacy-asset cleanup, debug kept as artifact-only ✅, SHA-pinned actions ✅.

- **REL-01 (P0): keystore materialization runs *before* the debug build**, so missing secrets kill the *entire* job in 33 s — no debug APK, no test/lint signal, nothing. Restructure: build→test→lint→debug artifact first; materialize secrets only in a `release` job (or before the release build). This single reorder restores CI signal *today*, before secrets exist.
- **REL-02 (P1):** no `apksigner verify --print-certs` gate post-build. Docs promise it manually; automate it as a required step before publish (and print the SHA-256 fingerprint into the run summary for rotation audits).
- **REL-03 (P2):** R8 `mapping.txt` is not uploaded. Upload per-release (needed to deobfuscate Play/framework crash stacks; `.gitignore` already anticipates `ci-mapping.txt`).
- **REL-04 (P2):** `continue-on-error: true` + separate fail-steps works but obscures the Checks UI; prefer `if: failure()` summary steps.

### 7.3 Deployment model

Sideload-first: `apk-latest` evergreen release + versioned `v*` GitHub releases. No Play automation, no staged rollout, no update channel. `versionCode=3 / versionName=0.2.1`, debug gets `.debug` suffix ✅. **No release has ever been published** — first-release runbook (key rotation notice, uninstall-reinstall caveat, backup exclusions) is documented but unexercised.

### 7.4 Supply chain

✅ Committed wrapper jar + `distributionSha256Sum` + `validateDistributionUrl`; ✅ `FAIL_ON_PROJECT_REPOS`; ✅ weekly Dependabot for Gradle+Actions (proven active); ✅ no lockfiles to drift (version catalog is the lock). Gaps: **SEC-04 (P1)** no `verification-metadata.xml` / signature verification; **SEC-05 (P1)** no CodeQL/SAST, no OSV/dependency-audit job; VRF-01/VRF-02 pin gaps above. Note: PR #5 (Kotlin 2.4.10) was correctly CLOSED unmerged — AGP 8.7.3 caps Kotlin at ~2.1; codify a version-ceiling policy so a future merge doesn't break the build.

---

## 8. Code quality, architecture, scalability, performance

### 8.1 Strengths (keep)

- **Storage unification done right:** `DocNode` + `DocProvider` + scheme routing removes backend branching from UI/ops/thumbnails; cross-backend `moveTo` degrades to copy+delete; `DocRelation` blocks self/descendant copies (case-insensitive, SAF↔file bucket mapping).
- **Safe-by-construction file ops:** staging tokens, size+SHA-256 verify, journal with 200-entry cap and STAGING recovery, unzip quotas (`MAX_ENTRIES 10k`, depth 32, 8 GB cap, 64 MB free-margin, enforced on bytes *written*), zip-slip protection.
- **Thumbnail engine is production-grade thinking:** fd-direct reads, AUTO scoring w/ early exit, header-only cover ranking, content+settings-sensitive cache keys, LRU+negative caching, bounded parallelism, per-key locks, orphan cleanup, parent-cover invalidation on mutation.
- **UI state hygiene:** flows + `debounce(140ms)`/`conflate`, metadata resolved lazily per-card, nav state restoration, unified loading/empty/error blocks, bidi isolation (`bidiName`), content descriptions, RTL + 200%-font + TalkBack considered in test matrix.
- **Defensive runtime habits:** `runCatching` at I/O boundaries, `ensureActive` on walks, `SupervisorJob` app scope, crash-safe observers, atomic JSON writes with backup/quarantine restore, `allowBackup=false` + history exclusions.

### 8.2 Design patterns in use

Facade (`DocRepository`), Strategy (`FrameStrategy`, providers), Repository + `StateFlow`, hand-rolled DI graph,-rs `Mutex` per volume/key, foreground-service worker, cold-Flow progressive search, versioned document stores, Builder-free immutable UI states. No over-engineering; patterns fit the problem.

### 8.3 Findings — correctness & maintainability

| ID | Sev | Finding | Evidence | Fix | Effort |
|---|---|---|---|---|---|
| **BUG-01** | P1 | `PlaybackFailure.from()` matches `"SOURCE" in text` — the word **"RESOURCE" contains "SOURCE"**, so resource/decoding errors misclassify as `SOURCE_UNAVAILABLE` (wrong message + wrong recovery). `"ACCESS"`/`"IO_"` matching is similarly loose. | `ui/player/PlaybackFailure.kt:13-20` | Classify on `PlaybackException.errorCode` ints (`ERROR_CODE_IO_*`, `ERROR_CODE_DECODER_*`, …), keep string match as last-resort fallback; add unit tests with adversarial messages (`"RESOURCE_EXHAUSTED"`, …) | S |
| **UI-01** | P2 | God files: `SettingsScreen` **1211 L**, `HomeScreen` 1056 L, `BrowseViewModel` 1025 L, `BrowseScreen` 780 L, `DesignSystem` 723 L, `LibraryScreens` 668 L. Review/merge-conflict/testability cost grows superlinearly. | LOC table (§3) | Split incrementally: settings→per-section files; home→sections; BrowseVM→use-case helpers; no behavior change | M (sliced) |
| **I18N-01** | P2 | 4 hardcoded Arabic strings in `CrashReportActivity` bypass `strings.xml` (EN users see Arabic; breaks the 427/427 parity story). | `CrashReportActivity.kt:96-108` | Extract to resources + lint `HardcodedText` | XS |
| **TEST-01** | P1 | No coverage for `DocRepository` breadcrumb, `SearchEngine`, `FileOpsEngine`, `ThumbnailRepository`, `VolumeRepository`, any ViewModel. Pure-logic tests can't catch flow/lifecycle regressions. | `app/src/test` listing | Add Robolectric + Turbine; start with `DocRepository.breadcrumb` + `PlaybackFailure` + `SearchEngine` budgets | M |
| **LINT-01** | P2 | No ktlint/detekt/`editorconfig`; `lint{abortOnError=false}` + CI Fatal-only gate is lenient. Style drift likely as contributors join. | `app/build.gradle.kts`, workflows | Add ktlint-check to `verify`; enable `abortOnError` after baseline | S |

### 8.4 Performance hotspots & scalability

| ID | Area | Assessment |
|---|---|---|
| PERF-01 (P2) | `FileDocProvider.directorySize` | **Unbounded, uncancellable** full-tree walk (no `ensureActive`, no visited cap — unlike the SAF twin's 20k cap). Computing "size" on a huge internal tree pins an IO thread. Add `ensureActive()` per N nodes + same 20k cap + surface "≥" approximation. (S) |
| PERF-02 (P3) | `mediaCount`/folder tallies | Single-level scans per visible folder; fine. Re-audit if tallies ever become recursive. |
| ✅ bounded | Search (15k nodes/10 s), AUTO frame probes (5×128px, early-exit ≥0.68), `coverScan` limits, thumbnail‖3 + metadata‖2, generation locks, Coil cancel-on-scroll, search snapshot | All correctly budgeted. Device validation on 1k/10k folders + slow USB still required (TEST_MATRIX exists ✅). |
| SCALE | Single-module app | Correct for a 1–3 dev project. If the team grows: split `:core:doc`, `:core:thumb`, `:feature:player` etc. (P3, L — not now). Baseline Profile + Macrobenchmark after first green build (P2, M). |

---

## 9. Security review

**Scope:** static review (no dynamic instrumentation possible without a device). No penetration test was performed.

### 9.1 What's solid ✅

- Zero committed secrets/keys; secret-only signing; `.gitignore` verified effective.
- No `MANAGE_EXTERNAL_STORAGE`; SAF + scoped `READ_MEDIA_*` (incl. Android 14 partial-access) is the access model.
- `MainActivity` exported surface is tight: `MAIN/LAUNCHER`, `VIEW content:// video/*` only (no `BROWSABLE`, no `file://`), `USB_DEVICE_ATTACHED`. All other components `exported=false`.
- No network stack, no telemetry, no WebView, no cleartext, no backup of history (`allowBackup=false` + `backup_rules`/`data_extraction_rules` exclusions).
- Logging: 3 `Log.e` total, none include filenames/URIs/paths ✅. Crash report capped (180k chars), stored in private `filesDir`, shared only by explicit user action.
- File-op threat model is handled: zip-slip, traversal depth/segment limits, extraction budgets on bytes-written, staging isolation, self-copy blocking, unique-name anti-collision, SHA-256 verify option.
- R8 minify+shrink on release; `SourceFile`/`LineNumberTable` kept for readable stacks.

### 9.2 Findings

| ID | Sev | Finding | Fix | Effort |
|---|---|---|---|---|
| **P0-3** | P0 | **Dead all-files-access branch contradicts the security story.** `Permissions.hasAllFilesAccess()` wraps `Environment.isExternalStorageManager()`, which can only return true if `MANAGE_EXTERNAL_STORAGE` is declared — it isn't, so the branch is dead, and `VolumeRepository`'s "all-files" comments + `allFilesAccessIntent()` (a settings deep-link to a toggle the app can't use) are misleading UX dead-ends. | Remove the all-files branch + intent helper + stale comments (recommended — SAF model is strictly better for Play policy), *or* formally decide to request it with a Play-policy justification. Grep-gate `isExternalStorageManager` in CI. | S |
| **SEC-02** | P1 | **FileProvider paths are maximally broad:** `<external-path path="."/>` + `<files-path path="."/>` expose the entire external storage and app files dir (including `recent/favorites/playback.json` history). Only explicitly-shared URIs are reachable in practice, but least-privilege says narrow it. | Restrict to `<cache-path path="thumbs/"/>` + `<cache-path path="shared/"/>` (+ whatever `externalUri()` provably needs); re-test share/open-with matrix. | S |
| **SEC-03** | P2 | Manifest hygiene drift: `tools:targetApi="34"` vs `targetSdk 36`; `requestLegacyExternalStorage="true"` is a no-op on API 30+ and confuses reviewers. | Set `targetApi="36"`, drop the legacy flag. | XS |
| **SEC-04** | P1 | No Gradle dependency verification (`verification-metadata.xml`) — a compromised mirror/proxy could ship tampered AARs undetected. | `gradle --write-verification-metadata pgp,sha256 --export-keys`; enforce in CI. | S |
| **SEC-05** | P1 | No SAST/dependency-vulnerability scanning (Dependabot versions ≠ vuln alerts on behavior; no CodeQL). | Enable CodeQL (Kotlin) + `osv-scanner`/`dependency-check` job on schedule. | S |
| SEC-06 | P3 | Hidden-API reflection (`StorageVolume.getUuid/getDirectory`) sits on Android's greylist — has fallbacks today, may break on future releases. | Keep fallbacks; add a smoke assertion that volumes still list when reflection returns null (Robolectric). | S |
| SEC-07 | P3 | Crash report file in `filesDir` is never rotated — unbounded small-disk write per crash. | Keep last 3 + 1 MB cap. | XS |

**No critical (exploitable now) vulnerabilities found.** The realistic attack surface is: malicious `content://` video URIs from other apps (handled via resolver + ExoPlayer sandbox), malicious ZIPs (handled), malicious USB volume labels/mounts (display-only use ✅), and a compromised build dependency (addressed by SEC-04/SEC-05).

---

## 10. Licenses & third-party services

- **LIC-01 (P0): no `LICENSE` file.** An unlicensed public repo is "all rights reserved" by default — this blocks lawful redistribution, F-Droid/Play third-party packaging, and contributor confidence. Decide: Apache-2.0 (recommended — matches the entire dependency tree) or explicit proprietary notice. (XS + a maintainer decision)
- **Dependency licenses (all permissive, no GPL):** AndroidX/Media3/Coil/Okio/ExifInterface/Kotlin/Coroutines/DataStore/DocumentFile/Activity/AppCompat/Lifecycle/Espresso → **Apache-2.0**; JUnit4 (test-only) → EPL-1.0 (fine, not shipped); Gradle wrapper → Apache-2.0. No license conflicts for an Apache-2.0 or proprietary app.
- **LIC-02 (P2):** no OSS-notices screen/asset. Add generated notices (e.g., AboutLibraries) — Play reviewers and enterprise users expect it. (S)
- **Privacy (P0 for any store release):** no privacy-policy file/URL. Draft one (offline app: "no data leaves the device; history excluded from backup; crash reports shared only by you"). Required by Play even for no-data apps. (S)
- **Third-party services:** GitHub Actions only. No Firebase, no crash backend, no analytics, no CDNs, no license servers. ✅ Nothing to rotate except the signing key (rotation runbook exists in README §6; unexercised).

---

## 11. Documentation findings

| ID | Sev | Finding |
|---|---|---|
| DOC-01 | P1 | `docs/BUILD.md` says SDK 35; build is 36. Also fix the SDK package list (add §5.3 items). |
| DOC-02 | P1 | `SETUP_SIGNING.md` referenced by `keystore/README.md` + `build-apk.yml` release notes but **doesn't exist**. Point refs at `README.md` §6 or create the file. |
| DOC-03 | P1 | `docs_backup_20260908T042855Z.tar.gz` referenced by README but **absent**. Attach it or drop the reference. |
| DOC-04 | P1 | `docs/BUILD.md` §0 directs users to a **debug APK in `apk-latest`**; workflow publishes **release-only** there now. Rewrite §0 around `app-release.apk` (+ artifact path for debug). |
| DOC-05 | P2 | `docs/ACCEPTANCE_CRITERIA.md` references `FileOpsManifest` (no such class — it's `OpsJournal` + `OpModels`). Also contains mojibake/non-Arabic glyph runs in several docs (`immunostaining`, CJK fragments) — run a docs lint pass. |
| DOC-06 | P2 | `ARCHITECTURE.md` file map omits newer files (`OpsSafety`, `OpsIntegrity`, `SearchBudget`, `CoverRules`, `AudioArtExtractor`, …) and still lists `FileSystemProbe` as if standalone (it's an object inside `VolumeRepository.kt` — fine, just clarify). |
| DOC-07 | P3 | Docs are Arabic-only. Add a 1-page English `README_EN.md`/header summary for international contributors and store review. |

---

## 12. Remediation roadmap (prioritized, with estimates)

> **Remediation log (2026-09-09, branch `arena/01a0861f-usb2k`):** REL-01 ✅ fixed — `Materialize signing keystore` now warns and sets `has_signing=false` instead of killing the whole job (`8517519`); release build gated on `has_signing`; tags without secrets fail loudly via `Require signing secrets on release tags`; REL-02 ✅ added `Verify release signature (apksigner)` gate. Debug APK + test/lint signal now work with zero secrets; signed release still needs the `USBMEDIA_*` secrets (§5.1). The `KEYSTORE_B64`-empty failure pasted from run `34352006834` came from the old workflow on `main`.
>
> **Remediation log (2026-09-09, branch `arena/01a0867e-usb2k` — Verify/Unit-tests root causes):** the `verify → Unit tests` step failed in ~15–50 s on *every* run (e.g. job `102471973361`/run `34353411094`, runs `34348426034`, `34346856877`), i.e. before any test could execute. Four stacked causes, all fixed:
> 1. **AGP 8.7.3 rejects `compileSdk 36`** ("unsupported compileSdk" — AGP 8.7 caps at 35). Present since `d1e1504`, so it failed even before the dependabot bumps. Fixed with explicit `android.suppressUnsupportedCompileSdk=36` + comment (remove on AGP 8.9+/9.x).
> 2. **Compose BOM `2026.08.00` (Compose 1.12) and Navigation `2.10.0` declare minCompileSdk 37 / min AGP 9.2** (official Aug-2026 release notes) → `checkDebugAarMetadata` failure. Downgraded to the newest 36-compatible line: BOM `2026.04.01` (Compose 1.11) + Navigation `2.9.5`; dependabot `ignore` ceilings (`>= 2026.08.00`, `>= 2.10`) + catalog comments prevent recurrence until the planned AGP 9 upgrade.
> 3. **JVM unit tests used `Uri.parse(...)`**, which returns `null` under `isReturnDefaultValues` → NPEs. `Routes` gained JVM-safe `String` overloads + `encodeUri()` fallback (production behavior unchanged); `QaContractTest` now mocks `Uri` with Mockito.
> 4. **`JsonStoreMigrationTest`/`PlaybackPosition.toJson()` need a real `org.json`** (android.jar stubs return null) → added `testImplementation` `org.json:json` + `mockito-core`.
> Also added a keepable `Report unit-test failure cause` step (failure excerpt → run summary + check annotation) and failed-only `testLogging`. Proven-compatible-kept: media3 `1.11.0`, exifinterface `1.4.2`, coroutines `1.11.0`, okio `3.18.2` (all pass AAR checks against 36).
>
> **Remediation log, continued — compiler + lint (same branch):** peeling the AAR layers revealed two more stacked failures. (a) KGP `2.0.21` crashed deterministically in FIR checkers (`FileAnalysisException …CrashReportActivity.kt:49:5: source must not be null`, `fir.pipeline.AnalyseKt.runCheckers`) on every branch; simplifying the file did not help → bumped Kotlin `2.0.21 → 2.1.20` (newest line AGP 8.7.3 accepts), crash gone. That exposed a real code bug: `SelectionTopBar` referenced but never defined → implemented it (count + size + close/select-all/invert). (b) `lintAnalyzeDebug` then crashed in `LintDriver.initializeExtraRegistries` — the Compose 1.11 (BOM `2026.04.01`) custom lint checks are binary-incompatible with lint 8.7.3; only an AGP 9 upgrade brings a compatible lint. Reverted to the coherent written-for set BOM `2024.12.01` (Compose 1.7) + Navigation `2.8.5` and widened the dependabot ceilings (`> 2024.12.01`, `>= 2.9`) until the AGP 9 upgrade. Triage script extracted to `.github/workflows/report_failure.py` and wired for unit-tests/lint/build steps.
>
> **Milestone (same branch, run `34366429897`): `verify` job GREEN for the first time in repo history** — Unit tests + Lint + Debug build all pass. Remaining: instrumented-smoke jobs (API 29 failed inside the emulator script after ~8 min; triage wiring added for the nested Gradle output, connected-test XMLs included).
>
> **Remediation log (2026-09-11, branch `arena/01a0921e-usb2k` — the smoke gate is the only red check left):**
> `main` is at `2989f93` and its `verify` job is **green** (run `34373434549`: unit tests + lint + `assembleDebug` all pass in 5m50s). The two
> `instrumented-smoke` jobs (API 29 + 35) fail on **every** run — the `main` push `34373434549`, the draft-PR run `34378498906`, and all six open
> dependabot PRs (`34373517051`, `34373527355`, `34373567004`, `34373587630`, `34373598827`, `34373636256`), whose `verify` jobs are all green. The
> dependency bumps are therefore not the cause and the app still builds; the failure is inside the emulator run.
>
> What the evidence does **not** yet say: the only Gradle-level cause is `There were failing tests. See the report at: …/reports/androidTests/connected/debug/index.html`,
> and the triage annotation of run `34378498906` carried **no** `FAILING TEST:` entry — i.e. no failing `<testcase>` was found under `app/build/outputs/androidTest-results/`.
> The raw step log is unreadable from a sandbox (the log host `results-receiver.actions.githubusercontent.com` returns `EOF` on every attempt; only `api.github.com`
> is reachable), so the **root cause is still unknown**. Ranked hypotheses: (1) the app crashes while launching on the emulator; (2) the instrumentation never
> starts or the install fails (`installPackages` frames appear in the annotation); (3) a genuine assertion failure in `ApplicationSmokeTest`.
>
> Shipped in this branch — make the next run self-diagnosing instead of guessing:
> 1. **`.github/workflows/smoke_evidence.sh` (new):** while the emulator is still alive it dumps `adb devices` / `getprop` / `pm list packages` / `pm list instrumentation` /
>    `dumpsys package` / `dumpsys meminfo`, the **crash buffer**, the full **logcat** and the ANR/tombstone listings into `smoke-evidence/`. Best-effort by design
>    (every probe guarded, always exits 0) so diagnostics can never mask the real Gradle exit code.
> 2. **`report_failure.py`:** now accepts device logcat paths and mines them for `FATAL EXCEPTION` / `INSTRUMENTATION_FAILED` / `INSTALL_FAILED` / `am_crash` / `ANR in`
>    (emitted as `DEVICE CRASH:` entries, ordered before the Gradle frames); the androidTest XML globs widened to `androidTest-results/**` and `reports/androidTests/**`;
>    a `<failure>` with an empty `message` attribute falls back to its text; the annotation slice grew from 16 to 24 entries so device evidence is no longer truncated away.
> 3. **`verify.yml`:** a failed smoke run now uploads a `smoke-api-<N>-evidence` artifact (`connected.log` + the evidence dir + both report trees, 14-day retention),
>    and the VRF-01 / VRF-02 pin gaps above are closed.
>
> Verified locally in this sandbox (no JDK/Android SDK, and Gradle/Maven/Google hosts answer `000`, so Gradle itself cannot run here): both workflows parse as YAML;
> the emulator `script:` block passes `sh -n` and `bash -n`; `smoke_evidence.sh` was executed end-to-end against a stub `adb` (10 evidence files written, annotation
> emitted, exit 0) and again with `adb` absent from `PATH` (writes `adb-missing.txt`, exit 0); `report_failure.py` was run against a synthetic `connected.log` + logcat +
> two connected-test XMLs and emitted the `DEVICE CRASH` plus both `FAILING TEST` lines. The 2-argument invocation used by the `verify` job is unchanged and still works.
>
> **Root cause of the emulator-smoke failure (found 2026-09-11, run `34649210471`):** it was never a test problem first — it was the harness.
> Read from the pinned action's own source at `a421e438` (`src/main.ts` → `parseScript(scriptInput)`, `src/script-parser.ts` →
> `.split(/\r\n|\n|\r/)` then `exec.exec('sh', ['-c', script])` per element): `reactivecircus/android-emulator-runner` splits the `script`
> input on **every newline** and runs each line as a **separate `sh -c` process**. So `code=0` on line 1 never reached line 3, and
> `if [ "$code" -ne 0 ]; then` executed alone is a shell syntax error → the job died with **exit code 2** before Gradle's real status or any
> triage could run. That is exactly why main's runs `34373434549` reported `exit code 2` with no annotation while PR #17's single-line
> rewrite reported `exit code 1` *with* the Gradle annotation — the same underlying failure, two different harness bugs.
> Fixed by folding the whole flow into ONE `sh -c` (YAML `>-`) that `cd`s to `$GITHUB_WORKSPACE` (the action only `chdir()`s when
> `working-directory` is set), captures `${PIPESTATUS[0]}`, and runs the evidence dump on failure. Verified by re-implementing `parseScript`
> over the parsed YAML value (1 element, 0 newlines) and executing the payload against a stub `gradlew`: failure path emits
> `SMOKE gradle_exit=1` + the `::error::` triage and exits 1; success path exits 0 and collects nothing.

**Effort scale:** XS <1 h · S 1–4 h · M 1–3 d · L 1–2 w. All phases assume one Android engineer with a JDK 17 + SDK 36 workstation (or CI access).

### Phase 0 — Restore signal (Day 0–1) · total ~1–2 d · **do first, in order**

| # | Action | ID | Effort |
|---|---|---|---|
| 1 | Open run `34348426034` → `verify` job → **`Unit tests` step log**; fix root cause (dependency bump, compiler flag, or test). Re-run to green. This unblocks everything. | — | S–M |
| 2 | Reorder `build-apk.yml`: debug build + tests + lint + debug artifact **before** keystore materialization; gate only release on secrets. | REL-01 | S |
| 3 | Enable branch protection on `main` (require `verify`+smoke, ≥1 review, block force-push) + tag protection for `v*`/`apk-latest`. | VRF-04 | XS |
| 4 | Triage emulator-smoke infra flakes (`Install Android emulator image`); pin `android-emulator-runner` to SHA; consider one API on PRs. | VRF-01/03 | S |

Exit criteria: `Verify` green on `main`; `Build APK` produces debug artifacts without secrets.

### Phase 1 — Release readiness (Week 1) · total ~3–5 d

| # | Action | ID | Effort |
|---|---|---|---|
| 5 | Generate + store new signing key; set `USBMEDIA_KEYSTORE_B64` + 3 passwords; run `workflow_dispatch` release; capture `apksigner` fingerprint. | P0-2 | S (+owner decision) |
| 6 | Add `apksigner verify --verbose --print-certs` gate + fingerprint in run summary; upload `mapping.txt`. | REL-02/03 | S |
| 7 | Add `LICENSE` (Apache-2.0 recommended) + draft privacy policy. | LIC-01 + privacy | S |
| 8 | Remove dead all-files-access branch **or** formally adopt it with Play justification. | P0-3 | S |
| 9 | Narrow FileProvider paths; re-run share/open-with matrix. | SEC-02 | S |
| 10 | Fix DOC-01…DOC-04 (SDK 36, signing refs, tarball ref, debug-APK instructions). | docs | S |

Exit criteria: signed `app-release.apk` published to `apk-latest`; legal docs present; first-release runbook exercised once.

### Phase 2 — Quality & hardening (Weeks 2–3) · total ~1–2 w

| # | Action | ID | Effort |
|---|---|---|---|
| 11 | Fix `PlaybackFailure` classifier (errorCode-based) + adversarial tests. | BUG-01 | S |
| 12 | Bound `FileDocProvider.directorySize` (cancel + cap). | PERF-01 | S |
| 13 | Add Robolectric + Turbine; cover breadcrumb, search budgets, ops happy-path, one ViewModel. | TEST-01 | M |
| 14 | Pin `setup-gradle` consistently; add `verification-metadata.xml`; enable CodeQL + OSV scan; ktlint gate. | VRF-02, SEC-04/05, LINT-01 | M |
| 15 | Extract crash-screen strings; fix `targetApi`/legacy flag; DOC-05/06 cleanup. | I18N-01, SEC-03 | S |
| 16 | Physical-device matrix (TEST_MATRIX.md): USB slow/SSD, FAT32/exFAT, detach-during-copy/play, 1k/10k folders, RTL+TalkBack+200% font, Android 11/13/14/15. | — | M |

Exit criteria: coverage on all data-layer public APIs; device matrix signed off; SAST clean.

### Phase 3 — Sustain (Month 2+, ongoing)

Split god files incrementally (UI-01, M sliced) · Baseline Profile + Macrobenchmark (M) · OSS notices screen (LIC-02, S) · crash-report rotation (SEC-07, XS) · English README summary (DOC-07, S) · version-ceiling policy for Kotlin/AGP/BOM (XS) · consider `:core`/`:feature` modularization only if team grows (L).

---

## 13. Onboarding — concrete next steps for a new engineer (Day 1)

```bash
# 1. Clone + inspect (matches this audit)
git clone https://github.com/hashmllalan-cpu/USB2K.git && cd USB2K
git checkout arena/01a0861f-usb2k && git rev-parse HEAD

# 2. Install toolchain (Ubuntu example)
#    - Temurin JDK 17, Android cmdline-tools, then:
sdkmanager --licenses
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools" \
  "system-images;android-29;google_apis;x86_64" "system-images;android-35;google_apis;x86_64"
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # never commit (gitignored ✅)

# 3. Verify (expect green after Phase 0; red today — see §6.3)
./gradlew --version
./gradlew :app:testDebugUnitTest --no-daemon --console=plain
./gradlew :app:lintDebug --no-daemon --console=plain
./gradlew :app:assembleDebug --no-daemon --console=plain

# 4. Run: connect a USB-OTG-capable device (emulator can't do real USB),
#    ./gradlew :app:installDebug, grant the SAF tree on the stick's root.
```

Read order: `README.md` → `docs/ARCHITECTURE.md` → `di/AppContainer.kt` → `data/doc/DocProvider.kt` → `data/thumb/ThumbnailRepository.kt` → `data/ops/FileOpsEngine.kt` → `ui/nav/Navigation.kt`. Mental model: **one Activity, one DI graph, two storage backends behind one interface, everything offline.**

---

## 14. Appendix — verification checklist (evidence for every claim)

| Check | Method | Result |
|---|---|---|
| Broken `R.string` refs | Python scan: 390 refs vs 427 defined | **0 missing** |
| Broken `R.drawable`/`R.mipmap` refs | Same scan | **0 missing** |
| EN↔AR string parity | key-set diff | **427/427, zero skew** |
| Hardcoded `Text("…")` | grep | 4 (crash screen only) |
| Secrets in source | case-insensitive grep (password/secret/token/key/AKIA/BEGIN PRIVATE) | Clean (staging/design-token FPs only) |
| Committed keystores | `git ls-files \| grep p12/jks/keystore` | **None** |
| `keystore/ci.p12` ignored | `git check-ignore -v` | Ignored by `.gitignore:*.p12` ✅ |
| `MANAGE_EXTERNAL_STORAGE` in manifest | grep | **Absent** ✅ |
| Exported components | manifest grep | Only `MainActivity` ✅ |
| Network usage | grep http/OkHttp/Retrofit/WebView | None (namespace URIs only) ✅ |
| `TODO/FIXME` | grep | None (only `XXXX-XXXX` path patterns) ✅ |
| `Log.*` with user data | grep 3 hits | Filenames/URIs never logged ✅ |
| Wrapper committed + valid | `git ls-files` + 46,175-byte jar | ✅ |
| `OpsJournal` unbounded? | read code | Capped at 200 ✅ |
| `FileSystemProbe` missing? | grep | Exists (object in `VolumeRepository.kt`) ✅ — not drift |
| `FileOpsManifest` (docs ref) | grep + `find` | **No such class — docs drift** (DOC-05) |
| `SETUP_SIGNING.md` (docs ref) | `ls` + grep | **Missing** (DOC-02) |
| Backup tarball (README ref) | `ls *.tar.gz` | **Missing** (DOC-03) |
| `compileSdk` docs vs build | grep | Docs 35 vs build 36 (DOC-01) |
| Debug APK in `apk-latest` (docs claim) | workflow read | Release-only now (DOC-04) |
| `setup-gradle` pin consistency | grep both workflows | ~~Different SHAs, same label~~ → **one SHA (`9c971963…` = v6.3.0) in both** (VRF-02 ✅) |
| Emulator runner pin | grep | ~~Floating `@v2`~~ → **`a421e438…` (= v2.38.0)** (VRF-01 ✅) |
| Branch protection | `gh api …/protection` | 403 (unverifiable); red merges prove non-enforcement |
| Secrets configured | build-apk run `34349094953` | **Proven missing** (fails at materialize step) |
| Any release published | `gh release list` | **Empty** |
| Unit-test failure | runs `34348426034/34346856877/34347509273` | `verify → Unit tests` red on all; root cause needs log read |
| `LICENSE` / privacy policy | `ls` | **Both absent** |

---

*End of report. Highest-ROI next action (<5 min): open the `Unit tests` step log of run `34348426034` in GitHub Actions — everything downstream depends on what it says.*
