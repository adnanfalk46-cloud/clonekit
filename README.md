# CloneKit — APK Cloner (Phase 1, v1 skeleton)

CloneKit creates **clones of installed apps** on-device, without root, using the
**repackage** approach:

> Extract APK → rename package in the binary `AndroidManifest.xml` → repack +
> zipalign → re-sign with a **per-clone** key → install via `PackageInstaller`.

Application ID: `com.apkcloner.app` · minSdk 28 · targetSdk 35 · Kotlin + Jetpack Compose (Material 3).

> **Important:** this project was written without compiling it here (no Android
> SDK in this environment). Open it in Android Studio and build there; expect
> small compile fixes on first build. See "Known rough edges" below.

## Project layout

```
apk-cloner/
├── app/                  # UI: Apps list, clone progress, My Clones, Settings
├── core/apkpatch/        # Pure-JVM binary AndroidManifest.xml (AXML) reader/writer + patcher
├── core/signer/          # Per-clone RSA-2048 keys (AndroidKeyStore / BouncyCastle) + apksig signing
├── core/installer/       # PackageInstaller Session wrapper (install + uninstall)
├── core/clonestore/      # Room database of created clones
├── core/cloner/          # ClonePipeline orchestrator + WorkManager CloneWorker + ZipAligner
├── README.md
└── LIMITATIONS.md        # Honest list of what v1 cannot do — read this first
```

## How to build

1. Install **Android Studio** (Hedgehog or newer) with Android SDK **API 35**.
2. `File → Open` and select the `apk-cloner` folder. Let Gradle sync finish
   (it downloads AGP 8.5.2, Kotlin 2.0.21 and all dependencies; first sync needs internet).
3. `Build → Make Project`. Fix any compile errors Android Studio reports
   (most likely candidates: dependency version drift in the version catalog).
4. `Run → Run 'app'` on a **physical test phone** (Android 9+, e.g. your daily driver
   is fine for v1 testing) — or `Build → Build Bundle(s) / APK(s) → Build APK(s)`
   to get a debug APK you can share/install manually.

### Known rough edges (uncompiled here — verify in Android Studio)

- `com.android.tools.build:apksig:8.5.2` is resolved from Google's Maven repo;
  if Gradle can't find it, the `apksig` version in `gradle/libs.versions.toml`
  may need bumping to match the installed AGP.
- `androidx.work.impl.foreground.SystemForegroundService` manifest merge snippet
  assumes WorkManager 2.9.0's manifest; check the merged manifest if the
  foreground-service type warning appears on API 34+.
- The AXML parser was written against AOSP's `ResourceTypes.h` layout; test it
  against a real `AndroidManifest.xml` early (see manual test flow).

## Manual test flow (on a real phone)

Do this exactly, in order. Use a **simple app** first (a basic offline tool, not
WhatsApp/banking — see LIMITATIONS.md for why).

1. **Install CloneKit** on the test phone and open it.
2. **Grant “Install unknown apps”**: go to the *Settings* tab → tap
   *“Open Install unknown apps”* → enable the toggle for CloneKit → go back.
   (Android 13+ will also ask for notification permission on first launch —
   allow it to see clone progress.)
3. **Pick a simple app** on the *Apps* tab (search helps), tap **Clone**.
   - Apps marked *“Split APK — v1 can't clone”* are App-Bundle apps; the button
     is disabled for them on purpose.
4. **Watch the progress screen**: Extract → Rename → Align → Sign → Install.
   When Android shows its own install dialog, tap **Install** (CloneKit cannot
   install silently without root — this is an Android rule, not a bug).
5. **Verify the clone**:
   - It appears in *My Clones* and in the phone's app drawer as
     “<AppName> (Clone 1)”.
   - Launch it from *My Clones* → it should open with fresh, empty data
     (separate login/storage from the original).
   - Rename it (pencil icon) and uninstall it (trash icon) to test management.
6. **Repeat** with a second clone of the same app → you should get
   “<AppName> (Clone 2)” with package `<original>.clone2`.

If a clone installs but crashes on launch, that app almost certainly hardcodes
its package name or checks its signature — expected for v1 (see LIMITATIONS.md).
Report which app failed; Phase 2 (virtual engine) is the answer for those.

## Permissions used (and why)

| Permission | Why |
|---|---|
| `QUERY_ALL_PACKAGES` | list installed apps |
| `REQUEST_INSTALL_PACKAGES` | install built clones via PackageInstaller |
| `POST_NOTIFICATIONS` | clone progress notifications |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | keep long clone jobs alive on Android 14+ |

## Tech notes

- Signing uses Google's `apksig` library (v1+v2 schemes), the same engine as the
  `apksigner` CLI — embedded, so signing happens fully on-device.
- Each clone gets its **own RSA-2048 key** in AndroidKeyStore (BouncyCastle
  PKCS#12 fallback), which also gives each clone a distinct `ANDROID_ID`.
- Manifest patching is a hand-written, pure-JVM AXML parser/serializer
  (`:core:apkpatch`) — no apktool needed on-device.
