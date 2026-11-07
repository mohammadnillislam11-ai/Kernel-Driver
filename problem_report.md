# Kernel-Driver (NinMods) — Full Project Code Review & Problem Report

This document is a complete, file-by-file audit of the **mohammadnillislam11-ai/Kernel-Driver** (`com.andorid.ninmods`) Android repository. Every source file, Gradle configuration, resource, and manifest has been reviewed. No code has been modified.

---

## 1. Build System, Gradle & Environment Blockers (Why Dev Server / Build Fails)

### 1.1 Outdated Gradle & AGP Versions vs Modern JDK/SDK Container
- **Files**: `/gradle/wrapper/gradle-wrapper.properties` (Line 4), `/build.gradle.kts` (Lines 2–3), `/app/build.gradle.kts` (Lines 8, 13, 41–56)
- **Details**:
  - `gradle-wrapper.properties` uses `gradle-8.2-bin.zip`.
  - Root `build.gradle.kts` uses `com.android.application` version `8.2.2` and `org.jetbrains.kotlin.android` version `1.9.22`.
  - `app/build.gradle.kts` sets `compileSdk = 34`, `targetSdk = 34`, `JavaVersion.VERSION_17`, and `kotlinCompilerExtensionVersion = "1.5.8"`.
- **Problem**:
  - Gradle `8.2` does not support running on JDK 21 (which is the standard runtime in modern container environments like AI Studio / Android SDK 36 environments where Gradle 9.3.1 / AGP 9.1.1 / Kotlin 2.2.10 are expected).
  - Running `./gradlew assembleDebug` with Gradle 8.2 on JDK 21 fails immediately during Gradle script compilation (`Unsupported class file major version 65`) or triggers a huge download of old SDK 34 platform tools and Gradle 8.2 distributions that time out the dev server startup.

### 1.2 Missing Debug Signing Configuration in `app/build.gradle.kts`
- **File**: `/app/build.gradle.kts` (Lines 20–39)
- **Details**:
  - Only `signingConfigs.create("release")` and `buildTypes.release` are configured.
  - There is no `debugConfig` pointing to `${rootDir}/debug.keystore` (storePassword `"android"`, keyAlias `"androiddebugkey"`, keyPassword `"android"`).
- **Problem**:
  - Containerized Android preview environments require `buildTypes { debug { signingConfig = ... } }` bound to the root `debug.keystore` so `assembleDebug` produces a properly signed APK at `app/build/outputs/apk/debug/app-debug.apk`.

### 1.3 Package Name & Namespace Typo Across Entire Codebase
- **Files**: `/app/build.gradle.kts` (Lines 7, 11), `/app/proguard-rules.pro` (Line 2), and all 29 Kotlin files under `/app/src/main/java/com/andorid/ninmods/...`
- **Problem**:
  - The package is spelled `com.andorid.ninmods` (`andorid` instead of `android`). While internally consistent across Kotlin imports, any external script or intent expecting `com.android.ninmods` will fail.

---

## 2. Critical Logic & Functional Bugs (File-by-File)

### 2.1 `InteractiveProcessBridge.kt` — `exec` Breaks `cleanup()` and Stdin Commands After Process Launch
- **File**: `/app/src/main/java/com/andorid/ninmods/data/InteractiveProcessBridge.kt` (Lines 99–105 & 206–211)
- **Details**:
  - Lines 100–104 replace the `su` shell process image with the script/binary using `exec`:
    ```kotlin
    if (executableFile.name.endsWith(".sh")) {
        out.writeBytes("exec sh '${executableFile.absolutePath}'\n")
    } else {
        out.writeBytes("exec '${executableFile.absolutePath}'\n")
    }
    ```
  - Later, in `cleanup()` (Lines 208–210), the code writes shell commands (`killall -9 gemini-cli ...` and `exit\n`) to `outputStream` (which is now the stdin of the `gemini-cli` binary, **not** an interactive `su` shell!).
- **Problem**:
  - Once `exec` runs, `su` is replaced by `gemini-cli`. Sending `killall -9 gemini-cli...` to `outputStream` merely sends that string into `gemini-cli`'s stdin instead of executing a shell command!
  - Furthermore, because `gemini-cli.sh` spawns a background/daemon process and unlinks itself, `activeProcess?.destroy()` only kills the wrapper pipe while the detached root daemon continues running in memory unless killed via a separate `ShellExecutor.executeRoot("killall -9 gemini-cli; pkill -9 -f gemini_proc")` call.

### 2.2 `InteractiveProcessBridge.kt` — `_isRunning` State Bug Prevents Stopping or Restarting Before "Active" Signal
- **File**: `/app/src/main/java/com/andorid/ninmods/data/InteractiveProcessBridge.kt` (Lines 57, 145–152) & `/app/src/main/java/com/andorid/ninmods/ui/dashboard/external_mods/ExternalModsCard.kt` (Line 342)
- **Details**:
  - In `InteractiveProcessBridge.start()`, `_isRunning.value` is **not** set to `true` when the process starts; it is only set to `true` at Line 150 when stdout outputs `"Loading overlay engine into RAM"` or `"daemon active"`.
  - However, in `ExternalModsCard.kt` (Lines 341–345), during the License Verification step (before `"START"` is sent, so before the overlay engine is loaded into RAM), the code checks:
    ```kotlin
    val sent = processBridge.sendInput(submittedKey)
    if (!sent || !processBridge.isRunning.value) {
        selectedModForAction?.let { mod ->
            processBridge.stop()
            processBridge.start(...)
    ```
- **Problem**:
  - Because `processBridge.isRunning.value` is still `false` at the license prompt stage, `!processBridge.isRunning.value` is **always true**!
  - As a result, right after sending the license key to stdin (`processBridge.sendInput(submittedKey)`), `ExternalModsCard` immediately kills the running process (`processBridge.stop()`) and restarts it from scratch!
  - And when it restarts it, `onLicensePrompt = {}` is passed, and `"START"` is later sent before the restarted process may even be at the `START` gate, or the restarted process waits at the license prompt again while the UI thinks it is verified (`isVerified = true`).
  - Also, because `_isRunning.value` is `false` while waiting at the prompt, calling `start()` again before the previous process finishes does not guard against duplicate processes (`if (_isRunning.value) return` on Line 57 is bypassed).

### 2.3 `ModDownloader.kt` & `DriverLoaderScreen.kt` & `KernelModsCard.kt` — Cancel Download Does Not Cancel the Network Request
- **Files**:
  - `/app/src/main/java/com/andorid/ninmods/ui/loader/DriverLoaderScreen.kt` (Lines 89–103, 362–370)
  - `/app/src/main/java/com/andorid/ninmods/ui/dashboard/external_mods/ExternalModsCard.kt` (Lines 273–295, 311–313)
  - `/app/src/main/java/com/andorid/ninmods/ui/dashboard/kernel_mods/KernelModsCard.kt` (Lines 176–191, 197–206)
- **Problem**:
  - When the user clicks **"Cancel"** on `ApatchDownloadDialog` or `ApatchUnifiedModDialog`, the `onCancel` callback only sets `unifiedDialogState = null` or `isDownloadingDrivers = false` (hiding the dialog).
  - The coroutine `Job` running `ModDownloader.downloadMod(...)` is **never cancelled**!
  - Worse, in `ExternalModsCard.kt` (Lines 286–289) and `DriverLoaderScreen.kt` (Lines 92–97), when the background download eventually finishes after the user cancelled it, `onComplete` / `result.isSuccess` still fires, showing a Toast and **automatically launching the mod or opening the bottom sheet** even though the user cancelled!

### 2.4 `ModDownloader.kt` — Incorrect Return File When Extracting Multi-File ZIPs (`Kernel_Drivers.zip`)
- **File**: `/app/src/main/java/com/andorid/ninmods/data/ModDownloader.kt` (Lines 142–147)
- **Details**:
  ```kotlin
  val finalTargetFile: File = if (fileName.endsWith(".zip", ignoreCase = true) || isZipFile(tempFile)) {
      unzip(tempFile, targetDir)
      tempFile.delete()
      File(targetDir, fileName.removeSuffix(".zip") + ".sh").let {
          if (it.exists()) it else targetDir
      }
  }
  ```
  And then Lines 157–158:
  ```kotlin
  finalTargetFile.setExecutable(true, false)
  ShellExecutor.executeRoot("chmod 755 '${targetDir.absolutePath}'/*")
  ```
- **Problem**:
  - When `Kernel_Drivers.zip` is downloaded into `driversDir`, it extracts subdirectories `QX/` and `RT/`. `chmod 755 '${targetDir.absolutePath}'/*` is non-recursive (missing `-R`), so scripts inside `QX/` and `RT/` do not get `755` from `ModDownloader` (though `DriverRepository` works around it by running `chmod -R 755` afterward).
  - Furthermore, if `downloadUrl` falls back to `.sh` (Lines 59–63 of `ModDownloader.kt`, where `candidateUrls` tries `gemini-cli.sh` when `gemini-cli.zip` 404s), `fileName` passed into `downloadMod` is still `"gemini-cli.zip"`. Since `fileName.endsWith(".zip")` is `true` on Line 142, `ModDownloader` calls `unzip(tempFile, targetDir)` on a plain `.sh` script! `ZipInputStream` reads 0 entries, deletes `tempFile` (`tempFile.delete()`), and nothing is saved!
  - **Fix needed for agent**: Line 142 should check `isZipFile(tempFile)` ONLY (not `fileName.endsWith(".zip")`), and if `!isZipFile(tempFile)` when `fileName` ended with `.zip`, it should save the file as `.sh` (`fileName.removeSuffix(".zip") + ".sh"`).

### 2.5 `ExternalModsCard.kt` & `KernelModsCard.kt` — Pre-filling License Key with User's Account Password!
- **Files**:
  - `/app/src/main/java/com/andorid/ninmods/ui/dashboard/external_mods/ExternalModsCard.kt` (Lines 116, 229)
  - `/app/src/main/java/com/andorid/ninmods/ui/dashboard/kernel_mods/KernelModsCard.kt` (Line 230)
- **Details**:
  ```kotlin
  val initialKey = sessionManager?.activeLicenseKey?.ifBlank { sessionManager?.userPassword ?: "" } ?: (sessionManager?.userPassword ?: "")
  ```
- **Problem**:
  - If `activeLicenseKey` is null or blank, it falls back to `sessionManager.userPassword` (which is only set if the user changed their password in `ProfileScreen`, otherwise `""`). A user's login password is not a license key; pre-filling or passing `userPassword` as `licenseKey` into `processBridge.start(..., licenseKey = initialKey)` exports the user's password as `NIN_LICENSE_KEY` environment variable to the shell script.

### 2.6 `ForgotPasswordScreen.kt` — Keyboard "Done" Action Skips API Call and Fakes Success!
- **File**: `/app/src/main/java/com/andorid/ninmods/ui/forgotpassword/ForgotPasswordScreen.kt` (Lines 132–140)
- **Details**:
  ```kotlin
  keyboardActions = KeyboardActions(
      onDone = {
          focusManager.clearFocus()
          if (email.trim().isNotEmpty()) {
              submitted = true
          } else {
              errorMessage = "Email is required"
          }
      }
  ),
  ```
- **Problem**:
  - If the user types their email and presses the **"Done"** checkmark on the soft keyboard instead of tapping the `"Send Reset Link"` button, it sets `submitted = true` **without calling `AuthApiClient.resetPassword(email.trim())` at all!** The UI immediately shows `"A reset link has been dispatched to..."` even though zero network request was sent.

### 2.7 `SettingsScreen.kt` — Fake / Non-Functional Switches & Hardcoded Old Version String
- **File**: `/app/src/main/java/com/andorid/ninmods/ui/settings/SettingsScreen.kt`
- **Details**:
  1. **Lines 95–96 & 473–488 (`globalNamespace` and `legacySu` switches)**:
     - `var globalNamespace by remember { mutableStateOf(true) }`
     - `var legacySu by remember { mutableStateOf(false) }`
     - Toggling these switches in the UI only mutates local Compose memory state. They are not persisted in `SharedPreferences` / `SessionManager`, and they do not execute any root/APatch commands. When navigating away and back, they reset.
  2. **Line 691 (Hardcoded Outdated Version Name)**:
     - Displays `Text(text = "DriverLoader v1.2.6")`, whereas `AppConfig.VERSION_NAME` is `"v1.3.6"` and `app/build.gradle.kts` has `versionName = "1.3.6"`.
  3. **Lines 158, 273 (`Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))`)**:
     - Instead of using `ShellExecutor.isRootAvailable()` (which checks `/data/adb/ap/bin/su`, `/data/adb/ksu/bin/su`, `/data/adb/magisk/su`, etc.), `SettingsScreen.kt` runs bare `"su"`. On APatch or KernelSU setups where `su` is not in `/system/bin/su` (default `PATH`), `SettingsScreen` falsely reports `"SuperUser (su) Access: Denied"` even when root is available via `/data/adb/ap/bin/su`!

### 2.8 `LicenseClockCard.kt` & `SessionManager.kt` — Reactivity & Heartbeat Missing
- **Files**:
  - `/app/src/main/java/com/andorid/ninmods/ui/clock/LicenseClockCard.kt` (Lines 55–73)
  - `/app/src/main/java/com/andorid/ninmods/data/SessionManager.kt` (Lines 22–72)
  - `/app/src/main/java/com/andorid/ninmods/network/AuthApiClient.kt` (Lines 362–385)
- **Problems**:
  1. **Non-reactive `SharedPreferences` reads in Compose**:
     - `sessionManager.serverRemainingSeconds` and `sessionManager.activeLicenseKey` are plain getters reading from `SharedPreferences`, **not** Compose `State` or `StateFlow`.
     - In `LicenseClockCard`, `remember(sessionManager.serverRemainingSeconds, sessionManager.activeLicenseKey)` will **not** recompose when `ExternalModsCard` or `ProfileScreen` updates `sessionManager.serverRemainingSeconds` unless the parent composable happens to recompose.
  2. **`AuthApiClient.sendHeartbeat()` is Never Called Anywhere**:
     - `DOCS.md` (Line 91) states that the license clock is synchronized with `/api/v2/app/heartbeat`, and `AuthApiClient.sendHeartbeat` is defined at Line 362, but `sendHeartbeat` is **dead code** — it is never called anywhere in the app.
  3. **Reboot Wall-Clock Fallback Bug (`SessionManager.kt` Lines 58–70)**:
     - If `currentUptime >= syncUptime` happens to be true even after a device reboot (e.g., the user used the phone for 2 hours, synced license, rebooted the phone, and then opened the app 3 hours after reboot so `currentUptime` [3h] >= `syncUptime` [2h]), the code assumes no reboot occurred and miscalculates `elapsedSec` as `3h - 2h = 1h` instead of actual wall time elapsed!

---

## 3. Security & Shell Injection Vulnerabilities

### 3.1 Unescaped Single Quotes in Root Shell Commands (`InteractiveProcessBridge.kt`, `MainViewModel.kt`, `DriverRepository.kt`)
- **Files**:
  - `InteractiveProcessBridge.kt` (Lines 85–97, 101–103):
    ```kotlin
    out.writeBytes("export NIN_LICENSE_KEY='$licenseKey'\n")
    ```
  - `MainViewModel.kt` (Lines 155–156):
    ```kotlin
    "chmod 777 '$path'",
    "sh '$path'"
    ```
- **Problem**:
  - `licenseKey` comes from user input (`OutlinedTextField` in `ModActionDialogs.kt`). If `licenseKey` contains a single quote `'`, it breaks out of `'...'` in the root `su` shell and executes arbitrary root commands.
  - Also, `MainViewModel.kt` sets `chmod 777` (world-writable by any unprivileged app on the device!) on a script right before executing it as `root`. Any other app could modify the script before `sh '$path'` runs if it knows the path, or if SELinux allows it. Should be `chmod 700` or `755`.

### 3.2 Plain-Text Credentials in `SharedPreferences` & `build.gradle.kts`
- **Files**:
  - `/app/src/main/java/com/andorid/ninmods/data/SessionManager.kt` (Lines 14–17, 82–96): Stores `auth_token`, `user_password`, and `active_license_key` in unencrypted `SharedPreferences` (`ninmods_auth_session.xml`).
  - `/app/build.gradle.kts` (Lines 21–26): Hardcodes `storePassword = "ninmods_release_key"` and `keyPassword = "ninmods_release_key"`.

---

## 4. Dead Code, Unused Files & Documentation Discrepancies

### 4.1 Unused UI Components & Dead Methods
1. **`KernelModsCard.kt`** (`/app/src/main/java/com/andorid/ninmods/ui/dashboard/kernel_mods/KernelModsCard.kt` — 453 lines):
   - Completely unused! `DashboardScreen.kt` only renders `LicenseClockCard` and `ExternalModsCard`.
   - Furthermore, `KernelModsCard.kt` has a bug on Line 345: tapping a downloaded mod sets `warningMod = mod`, which opens `ApatchWarningDialog` whose default title is `"Stop Mod"` and message is `"Do you want to stop ... and unload it from memory?"` even when the mod is **not running** and the user is trying to **launch** it!
2. **`WarningCard.kt`** (`/app/src/main/java/com/andorid/ninmods/ui/component/WarningCard.kt`):
   - Never referenced anywhere in the project.
3. **`RadioItem` in `SettingsItem.kt`** (`/app/src/main/java/com/andorid/ninmods/ui/component/SettingsItem.kt` Lines 102–115):
   - Never referenced anywhere in the project.
4. **`AppCustomBottomSheet` in `AppBottomSheet.kt`** (`/app/src/main/java/com/andorid/ninmods/ui/dialogs/AppBottomSheet.kt` Lines 286–331):
   - Never referenced anywhere in the project.
5. **`ApatchConfirmDownloadDialog`, `ApatchWarningDialog`, `ApatchLicenseKeyDialog`, `ApatchRootRequiredDialog` in `ModActionDialogs.kt`** (Lines 625–1227):
   - Over 600 lines of legacy dialogs in `ModActionDialogs.kt` are only referenced by the unused `KernelModsCard.kt` (or not referenced at all in the case of `ApatchRootRequiredDialog`), because `ExternalModsCard.kt` switched to `ApatchUnifiedModDialog`.
6. **Unused API Client Methods in `AuthApiClient.kt`**:
   - `sendHeartbeat()` (Line 362), `fetchAppConfig()` (Line 390), `submitBugReport()` (Line 414), and `getProfile()` (Line 330) are never called anywhere in the codebase.

### 4.2 Unused XML Resources & Localization Mismatch
1. **`values-bn/strings.xml` vs `values/strings.xml`**:
   - Almost all UI text in the Jetpack Compose screens (`DashboardScreen`, `DriverLoaderScreen`, `LoginScreen`, `SignUpScreen`, `ProfileScreen`, `SettingsScreen`, `InstallationSetupScreen`, `ModActionDialogs`) is **hardcoded in English strings** inside Kotlin files instead of using `stringResource(R.string.*)`.
   - Consequently, the Bengali translation file `/app/src/main/res/values-bn/strings.xml` and most entries in `/app/src/main/res/values/strings.xml` are completely ignored by the app!
   - Even `app_name` in `values-bn/strings.xml` is `"ড্রাইভার লোডার"` (Driver Loader) while `values/strings.xml` has `"NinMods"`.
2. **Unused Drawables in `/app/src/main/res/drawable/`**:
   - The app uses Material Icons (`Icons.Filled.*`) in Compose, leaving most custom XML drawables (`apatch_dot_online.xml`, `apatch_floating_bar_bg.xml`, `apatch_pill_selected.xml`, `badge_bg.xml`, `device_mobile_down.xml`, `github.xml`, `ic_nav_*.xml`, `ic_user_profile.xml`, `info_circle_filled.xml`, `package_import.xml`, `play_circle.xml`, `settings.xml`, `telegram.xml`, `trash.xml`, `undo.xml`, `weblate.xml`, `webui.xml`) completely unused.

---

## 5. Summary Checklist for the Fixing Agent

1. **To Fix Build / Dev Server Startup**:
   - Update `gradle-wrapper.properties` to Gradle `9.3.1` (or match container's pre-installed Gradle), root `build.gradle.kts` AGP to `9.1.1` / Kotlin `2.2.10` (with `org.jetbrains.kotlin.plugin.compose`), and `compileSdk = 36` / `targetSdk = 36` in `app/build.gradle.kts` if building in the AI Studio JDK 21 container.
   - Add `debugConfig` signing configuration in `app/build.gradle.kts` pointing to `${rootDir}/debug.keystore`.
2. **To Fix Runtime & Mod Execution Bugs**:
   - Fix `InteractiveProcessBridge.kt` so `_isRunning` (or a separate `_isProcessAlive` state) tracks whether the `su` process is alive from the moment `ProcessBuilder(suBin).start()` succeeds, preventing `ExternalModsCard.kt` (Line 342) from killing and restarting the process right after `sendInput(submittedKey)`.
   - Fix `InteractiveProcessBridge.cleanup()` to run `ShellExecutor.executeRoot("killall -9 gemini-cli 2>/dev/null; pkill -9 -f gemini_proc 2>/dev/null")` instead of writing `killall` to the `exec`-replaced process stdin.
   - Fix `ModDownloader.kt` (Line 142) to check `isZipFile(tempFile)` before unzipping so fallback `.sh` downloads aren't corrupted/deleted when `fileName` ends in `.zip`.
   - Store the download coroutine `Job` in `DriverLoaderScreen.kt` and `ExternalModsCard.kt` so clicking **"Cancel"** actually cancels the active HTTP download job.
3. **To Fix Auth, UI & Settings Bugs**:
   - Fix `ForgotPasswordScreen.kt` `onDone` keyboard action to invoke the actual `AuthApiClient.resetPassword` coroutine instead of setting `submitted = true`.
   - Remove the fallback to `sessionManager.userPassword` when pre-filling `initialKey` for license dialogs in `ExternalModsCard.kt`.
   - Replace `Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))` in `SettingsScreen.kt` with `ShellExecutor.isRootAvailable()`.
   - Update hardcoded `"DriverLoader v1.2.6"` in `SettingsScreen.kt` (Line 691) to use `AppConfig.VERSION_NAME`.
   - Make `SessionManager` license state observable via `StateFlow` so `LicenseClockCard` updates immediately across screens.
