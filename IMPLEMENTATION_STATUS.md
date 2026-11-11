# Oblivion implementation status

This document records verified repository state and implementation milestones. It distinguishes inherited functionality from changes made for Oblivion.

## Repository baseline

- Root: `/public/Oblivion`
- Branch: `oblivion/open-source-integration`
- Remote: `origin` → `https://github.com/M4Lych/nexus-bedrock-android.git`
- Tracking ref shown by Git: `origin/oblivion/rebrand-foundation`
- Baseline working tree was clean before this milestone.
- Root license file identifies Apache License 2.0. This does not establish the license of every bundled component.
- Existing Gradle modules: `:app`, `:minecraft`, `:pojav_controls`; root project name is `levilauncher`.
- App namespace and application ID remain `org.levimc.launcher`. No identity or data migration was made.
- Android configuration currently declares compile SDK 36, target SDK 35, minimum SDK 28, Java 21, NDK `28.2.13676358`, and `arm64-v8a` in `app/build.gradle`.
- `app` already contains launcher activities, version/instance management, account flows, catalogs/package management, local content and world tools, and native integration. These are inherited components; their runtime behavior has not been revalidated end to end in this environment.
- SAF based content import, pack/world archives, metadata inspection, world export, and backup code are present under `core/content`.
- Native build includes `preloader`, `libHttpClient`, and `worldtools`; `libHttpClient` and `preloader` are Git submodules. A full provenance and redistribution audit remains outstanding.
- Existing unit tests cover package/catalog logic, instance backup, version metadata, mod utilities, and UI package models. Content archive and NBT validation coverage needs review.
- Environment check: Java and Python are unavailable; `ANDROID_HOME` and `ANDROID_SDK_ROOT` are unset. The checked-in `gradlew` is not executable (`Permission denied`), so `./gradlew :app:testDebugUnitTest --no-daemon` did not start. Gradle/Android compilation and automated resource parsing could not be confirmed here.

## Milestones

| Task | State | Evidence / remaining work |
| --- | --- | --- |
| Verify repository, modules, Android configuration, manifest entry point, content/world code, native build hooks, tests, and root license | IMPLEMENTED | Read-only inspection completed on 2026-11-10. No full runtime or legal/provenance validation. |
| Rebrand launcher display name to Oblivion without changing application identity or storage | IMPLEMENTED | Updated `app_name` in default and localized resources and beta variant label. Validate resource merging with Android Gradle tooling when available. |
| Establish phase-by-phase implementation and compatibility inventory | IN PROGRESS | This status file records the initial baseline. Detailed caller/dependency/runtime and user-data inventories remain. |
| Validate current local pack import, world backup/export, and malformed archive handling | NOT STARTED | Existing implementation is substantial; inspect security edge cases and expand tests before changing behavior. |
| Review launch/profile behavior and app entry paths | NOT STARTED | Preserve existing launch flow; identify safe improvements only after caller and runtime review. |
| Audit dependencies, submodules, vendored code, prebuilt libraries, and third-party notices/SBOM | NOT STARTED | Root license alone is not sufficient. Do not claim redistribution clearance for unresolved binaries. |
| Add modular core/provider boundaries | DEFERRED | Create modules only after identifying an independently extractable component and compatibility plan. |
| Full Android build and unit tests | BLOCKED | No Java runtime/JDK or Android SDK configured in the current environment. Re-run after toolchain is installed. |

## Milestone 2: Core architecture and existing feature integration

### Existing components and boundaries verified

| Area | Existing implementation | Current boundary / reusable types |
| --- | --- | --- |
| Launcher and profiles | `ui/activities/MainActivity`, `InstancesActivity`; `core/minecraft/MinecraftLauncher`, `GamePackageManager`; `core/versions` | UI invokes existing launch/runtime services. `VersionProfileMetadata` and `VersionProfileMetadataStore` model and persist profiles, including legacy metadata fallback. |
| Accounts | `core/auth/MsftAuthManager`, `MsftAccountStore`, `XalExporter`; account/login activities | Existing provider integration is Microsoft-specific and coupled to auth library/UI. Retained as optional account feature; no new account facade added without a demonstrated consumer needing it. |
| Catalog and packages | `core/packages/Catalog`, `CatalogModels`, `PackageService`, package engine; `ui/packages` | Existing catalog models, service, engine listener/cancel contracts, and UI request/result models already provide boundaries. No parallel catalog interface introduced. |
| Add-On/content | `core/content/ContentImporter`, `ContentArchive`, `ResourcePackManager`, `ContentManager` | Callback contracts and typed `ContentItem` models exist; local import/storage does not call Firebase. |
| Worlds | `core/content/WorldManager`, `WorldMetadataReader`, `WorldItem`, `WorldDatabase`, `WorldOperations` | Local operations and callbacks exist, including archive import/export and staged backup. Local world code does not call Firebase. |
| Settings | `settings/FeatureSettings`, `SettingsStorage`, UI `SettingsActivity` | Typed accessors and existing serialized settings are used. Existing keys/storage were preserved; no migration performed. |
| Optional crash upload | `core/crash/CrashReporter`, xCrash, Crashlytics | Added `CrashReportUploadProvider`/`CrashReportEvent`; app composition root installs Firebase adapter, with no-op default. Local xCrash capture, log storage, crash UI, and old static call sites remain in place. |

### Implemented in this milestone

- Introduced the optional crash-upload interface and event model in `core/crash`.
- Moved Crashlytics-specific initialization, consent handling, report submission, and build metadata into `integration/firebase/FirebaseCrashReportUploadProvider`.
- Kept `CrashReporter` as the compatibility facade and retained `sendUnsentReports()` and `refreshCrashlyticsCollection(Context)` for existing callers.
- Installed the Firebase adapter from `LauncherApplication`, which is the existing composition root. `CrashReporter` defaults to a no-op provider so local crash handling has no backend call dependency.
- Removed the direct Firebase Analytics dependency and unused version-catalog alias; no Analytics API usage was found in app or game Java/Kotlin sources. The Firebase BOM remains for Crashlytics/Messaging.
- Existing Firebase messaging in `:minecraft` is directly referenced by inherited service subclasses and the legacy Minecraft notification bridge. Those dependencies were retained to avoid removing notification behavior. This means Firebase is not yet removable from the app artifact.
- Existing local launcher, package/catalog, content/world, settings, and profile functionality remains in its existing `:app` code. No empty Gradle modules or duplicate provider facades were created.

### Verification and remaining work

- Source checks: `git diff --check` passed. Firebase calls in the new adapter are isolated from `CrashReporter`; `LauncherApplication` wires the adapter before reporter initialization.
- Android compile/tests were not repeated due the known environment constraints. No Java, Gradle executable permission, Android SDK, or Python is available.
- No dependency was introduced. The unused Firebase Analytics dependency and alias were removed.
- Next: add focused unit tests for provider fallback/forwarding when the Android/JVM test toolchain is available; review Firebase initialization and notification service ownership before any further Firebase decoupling; continue archive validation and feature tests using existing manager APIs.

## Focused review: optional crash-upload integration

### Verified behavior from call sites and lifecycle

- `LauncherApplication.onCreate()` initializes `FeatureSettings`, installs `FirebaseCrashReportUploadProvider`, then calls `CrashReporter.init()`. Android calls `Application.onCreate()` before the manifest launch activity; repository search found no other `CrashReporter.init()` call. The crash process uses the same application initialization path.
- Before installation, `CrashReporter` holds `NoCrashReportUploadProvider`. It does nothing for configure, submit, crash, and exception calls. The existing `CrashReporter` static methods used by settings, crash recovery, and Minecraft lifecycle remain available.
- Crash upload consent still comes from `FeatureSettings.isCrashUploadEnabled()`. Settings persist through the existing `SettingsStorage` JSON key. The historical default remains enabled (`true`), and the adapter retains the previous behavior of disabling Crashlytics collection and deleting unsent reports when the user turns upload off. No consent default or preference key changed.
- The xCrash callbacks still initialize locally with the existing crash log directory and callbacks. They set `handlingCrash`, then attempt optional reporting, then launch `CrashActivity` with the same local log/summary extras. `MinecraftActivity` continues to consult `isHandlingCrash()` before normal-exit restart handling. The recovery activity reads and shares the local log and calls the existing upload facade.
- Review found and fixed a fault-containment gap: provider methods were invoked before recovery UI without facade-level exception handling. `CrashReporter` now catches provider failures in configure (including startup), record, send, and handler-error paths, so an implementation that throws cannot suppress local crash setup or the recovery activity.
- The Firebase adapter independently catches Firebase SDK failures. No runtime test was possible, so this establishes source-level exception containment only.
- The app manifest declares `LauncherNewsMessagingService` with the Firebase messaging event. The `:minecraft` manifest declares `NotificationListenerService`, which extends `FirebaseMessagingService`, with the same event. `:app` and `:minecraft` retain their messaging dependencies. Minecraft's listener creates a separate Firebase app from game-provided Firebase resources and remains unchanged.
- No Firebase Analytics use, dependency, or catalog alias remains in app/game source or Gradle files.
- Diff review found no change to `applicationId`, namespace, `LauncherStorage`, preference names/keys, crash intent extras, account state, or unrelated launcher behavior in this focused review. Existing Milestone 1 display-name changes remain in the working tree.

### Remaining risks and checks

- Firebase Crashlytics and Messaging remain dependencies of the app artifact. The `:minecraft` messaging types and manifest service make deleting Firebase Messaging unsafe without replacing inherited notification behavior.
- Turning crash upload off still deletes pending unsent Crashlytics reports, matching existing behavior; this review intentionally did not alter that consent behavior.
- The `CrashReportUploadProvider` interface includes Android `Context` for backend configuration and remains in the existing app module. No Gradle module extraction was attempted.
- Static source checks performed: reviewed provider implementation, all `CrashReporter` call sites, Application initialization order, `CrashActivity`, Minecraft crash lifecycle check, settings and persistence, both messaging manifests/services, Firebase dependency declarations, identity/storage constants, and `git diff --check`. Fourteen Node-based source assertions passed for fallback/wiring, fault containment, local XCrash setup, consent/key preservation, recovery calls, FCM configuration, and Analytics removal.
- Build/runtime limitation is unchanged: Java is unavailable, the checked-in Gradle wrapper is not executable, and Android SDK variables are unset. No Android compile, unit test, device test, real crash, Firebase initialization, or message-delivery test was run.

## Milestone 3: Local Add-On and world management

### Existing workflow audit

- Reused `ContentArchive` as the shared archive parser/extractor. It already handles `.mcpack` and `.mcaddon` ZIP archives, nested `.mcpack` entries inside add-on archives, `.mcworld`/`.mctemplate` world containers, manifest module classification (`resources`, `data`/`script`, and `skin_pack`), path traversal rejection, staged extraction, and progress callbacks.
- Reused `ResourcePackManager` and `ResourcePackItem` for the resource, behavior, and skin pack libraries. The model reads pack name, description, version, UUID, icon, and localization from each manifest.
- Reused `WorldManager`, `WorldItem`, and `WorldMetadataReader` for world import/export, metadata summaries, backup-before-delete, and directory listing.
- Reused `ContentManager` as the existing observable/count integration and kept its current storage directory configuration. `ContentManagementActivity` remains the import entry point; `MainActivity` already routes to `ContentListActivity` for library screens and to content management.
- Existing import UI used a multi-select content picker but restricted MIME types to ZIP/octet-stream. The parser decides validity from the archive and manifest, not the MIME type.
- Existing pack imports used random destination folder names without checking manifest header UUIDs, so the same known pack could appear multiple times. World imports also use random destination directories; no reliable duplicate-world identifier is currently exposed by the existing world metadata reader.
- Local content managers and archive code have no direct Firebase/service dependency. Existing import UI shows callback success/error text and refreshes content counts after success.

### Implemented

- Changed the existing content picker to `ACTION_OPEN_DOCUMENT`, `CATEGORY_OPENABLE`, multiselect, and `*/*`. This lets document providers expose Bedrock custom file MIME types while preserving the existing archive parser as the format validator.
- Added case-insensitive duplicate detection for existing resource/behavior/skin pack folders using the Bedrock manifest `header.uuid`. Existing manifests are scanned with a 1 MiB read limit. Extraction is skipped when a matching UUID is already in the target library; files without a usable UUID retain the previous import behavior.
- Added duplicate counts to the archive import result, including nested add-on imports. A duplicate-only import reports that no new content was imported and how many packs were skipped; mixed imports report imported categories and duplicates. Invalid archives continue to report errors.
- Added English and Indonesian feedback resources and wired them into the existing manager callbacks. Imports continue to mean local import only; no message claims that content is enabled in a world.
- Preserved existing storage directories, random folder naming for new imports, pack UUIDs, world formats, storage preference keys, and the existing navigation.

### Files changed in Milestone 3

- `app/src/main/java/org/levimc/launcher/core/content/ContentArchive.java`
- `app/src/main/java/org/levimc/launcher/core/content/ContentImporter.java`
- `app/src/main/java/org/levimc/launcher/core/content/ResourcePackManager.java`
- `app/src/main/java/org/levimc/launcher/ui/activities/ContentManagementActivity.java`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`
- `app/src/main/res/values-idn/strings.xml`
- `IMPLEMENTATION_STATUS.md`

### Verification and limitations

- `git diff --check` and source-level assertions passed for picker configuration, UUID-based duplicate handling, bounded manifest scanning, result accounting, callback feedback, and existing manager/storage wiring.
- Resource keys and `%1$d` formatting were checked against all three edited locale files, and basic XML element nesting checks passed. No installed XML parser (`xmllint`, Node XML parser modules, PHP, Perl, or Ruby) was available, so a full XML parser check could not be run.
- Java is unavailable, `./gradlew` is not executable, and the Android SDK is not configured. Existing unit tests and Android compilation were not run; no runtime behavior has been claimed.
- Duplicate pack detection depends on a valid, non-empty Bedrock `header.uuid`; manifests without one are still imported and may duplicate. World duplicate detection remains deferred because the current reader does not provide a stable world identifier, and comparing by display name could incorrectly reject distinct worlds with the same name.
- Remaining concerns: content errors still use some existing hardcoded English messages; malformed archive and SAF provider behavior need instrumented/device tests. Next milestone: focused archive/import tests and safe world identity/duplicate analysis, followed by local settings/data compatibility work.

## Milestone 4: Launcher and profile integration

### Existing functionality reviewed and reused

- `MainActivity` is the launcher home and composition surface. It already provides navigation to instances, content management/libraries, mods/packages, settings, accounts, and external catalog screens.
- `VersionManager` discovers the official installed Minecraft package through `PackageManager` and custom imported profiles through the existing `base.apk.levi` layout. `GameVersion` carries installed/custom status, version, display name, repair state, and existing profile metadata.
- Installed/custom profile selection is persisted by the existing `version_manager` preferences (`selected_type`, `selected_package`, `selected_dir`). `VersionProfileMetadataStore` retains the JSON and legacy metadata compatibility path.
- The home uses a quick profile popup; `InstancesActivity` provides full management and APK import. Launch continues through `MinecraftLauncher`, which checks for a selected profile, unsupported game versions, repair state, Play Store verification, and reports failures through existing callbacks/dialogs.
- Initial loading disables the launch button until asynchronous `VersionManager` initialization completes. Missing profile state already has a localized launch error. No separate installation or profile system was needed.

### Implemented

- The home quick selector now shows the existing Installed or Custom badge for each profile, matching the status already shown in the full Instances screen.
- Selecting a profile from the home popup now refreshes the content libraries/counts as well as the selected-profile label and `MainViewModel`. This fixes counts remaining associated with the profile selected before the popup change.
- When no installed game or imported profile is available, the quick selector now shows an explicit empty state and a button to open the existing Instances manager/import workflow.
- No launch logic, profile model, discovery logic, persistence key, app identity, storage path, or external service dependency was changed.

### Files changed in Milestone 4

- `app/src/main/java/org/levimc/launcher/ui/activities/MainActivity.java`
- `app/src/main/res/layout/popup_instance_selector.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`
- `app/src/main/res/values-idn/strings.xml`
- `IMPLEMENTATION_STATUS.md`

### Verification and remaining work

- Static source review confirmed the selector uses the existing `GameVersion.isInstalled` flag and the existing `InstancesActivity`; selection still calls `VersionManager.selectVersion` and `MainViewModel.setCurrentVersion`, with the added content refresh.
- `git diff --check`, resource-key/placeholder checks, and basic XML element nesting checks passed for the edited files. Full Android resource processing and UI interaction could not be tested in the current environment.
- Java is unavailable, the checked-in Gradle wrapper is not executable, and the Android SDK is not configured. Android compilation, unit tests, and device/runtime testing were not run.
- Remaining work: test selector sizing/empty-state behavior on narrow and translated layouts, verify profile switching updates counts on device, and consider a non-blocking refresh when the official Minecraft app is installed/uninstalled while Oblivion is backgrounded. Next milestone: settings/configuration compatibility and migration audit.

### Milestone 4 continuation: dashboard profile and launch status

#### Work already completed in Milestone 4

- The existing home profile selector shows Installed/Custom state, refreshes content counts on selection, and links to `InstancesActivity` when no profiles are discovered.
- Profile discovery/persistence remains in `VersionManager`; the dashboard still delegates launch to `MinecraftLauncher` and keeps its existing validation and error handling.

#### Additional improvements implemented

- Added a dashboard status row showing the selected profile type and version using the existing `GameVersion` model. It refreshes after discovery, profile selection, and return to the home activity.
- Added an inline route to the existing Instances Manager for the no-profile state. Launch stays disabled until `VersionManager` reports a selected profile.
- The status row displays profile loading and launch-starting states. If the existing launch callback or synchronous launch path reports failure before the loading screen starts, the dashboard reports that failure and re-enables Launch only when a profile remains selected. Existing `MinecraftLauncher` dialogs/toasts and validation remain authoritative.
- Added English and Indonesian labels. Other locales use the existing Android default-resource fallback.
- No new manager, launch route, model, dependency, preference, identifier, or storage path was added or changed.

#### Files changed in this continuation

- `app/src/main/java/org/levimc/launcher/ui/activities/MainActivity.java`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`
- `app/src/main/res/values-idn/strings.xml`
- `IMPLEMENTATION_STATUS.md`

#### Verification and remaining launcher limitations

- Static review covered the home selector, `VersionManager` selected-profile fallback and persistence, dashboard refresh call sites, `MinecraftLauncher` callback and validation call sites, and existing navigation to `InstancesActivity` and `ContentManagementActivity`.
- `git diff --check` passed. Node source assertions passed for the new dashboard binding IDs, selected-profile status deriving from `GameVersion.isInstalled`, existing Instances Manager navigation, refresh call presence, disabled launch state without a selected profile, and continued use of the existing `MinecraftLauncher.launch` callback path.
- English, Indonesian, and `idn` resources contain each new status string; a basic element-nesting check passed for `activity_main.xml`. This check is structural only; full Android resource parsing was unavailable.
- Reviewed `InstancesActivity` selection through the existing shared `VersionManager` and the return-to-home `onResume()` refresh; reviewed existing home navigation to both `InstancesActivity` and `ContentManagementActivity`.
- No Android compilation, unit tests, UI automation, or device/runtime test was run. Java is unavailable, `gradlew` is not executable, and the Android SDK is not configured in this environment.
- The home reports what the existing `VersionManager` discovered; it does not independently verify Play Store provenance or guarantee launchability. `MinecraftLauncher` remains responsible for those checks. Installation discovery is not refreshed from the package manager on every home resume, so installing/uninstalling Minecraft while Oblivion is backgrounded may require the existing manager refresh/restart flow.
- Next milestone: settings/configuration compatibility and migration audit; separately validate the dashboard states and selector sizing on a device when the Android toolchain is available.

## Milestone 5: Oblivion UI/UX system

### Design decisions

- Kept the existing Android view, Material 3/Components, and MiSans typography stack; no UI framework or dependency was added.
- Retained the existing forest-green and mint accent identity. Refined semantic light/dark background, surface, text-secondary, outline, and divider colors so the same controls remain legible in both themes.
- Added reusable `OblivionScreenTitle`, `OblivionSectionTitle`, and `OblivionSupportingText` styles. Existing shared card and dialog drawables now use one-pixel semantic divider outlines and matching rounded corners.
- Raised primary profile, launch, navigation account, settings tab, instance filter/action, content toolbar, and profile-selector controls to 48dp where they are interactive. Added native selectable feedback to navigation and tabs.
- Kept existing activity navigation and feature managers. The Instances Manager now displays an empty-library state or a distinct no-results state based on its actual filtered list; import and backup actions remain available above it.

### Screens/components improved

- Home dashboard and profile selector: consistent button targets and shared card treatment, preserving Milestone 4 profile/launch states and actions.
- Main navigation: semantic brand color and larger sign-in/account touch areas, preserving existing navigation and account handlers.
- Instances Manager: shared title style, consistent filter/action targets, and localized empty/no-results presentation wired to `applyFilters()`.
- Content management and content library: shared screen-title style and a 48dp library toolbar search/action target.
- Settings: shared screen-title style and larger tab targets with Android ripple feedback.
- Dialog/card containers: reused existing drawables and aligned their outline weight, color, and corner radius.

### Files changed in this milestone

- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values-night/colors.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/res/drawable/card_background.xml`
- `app/src/main/res/drawable/card_background_small.xml`
- `app/src/main/res/drawable/bg_rounded_card.xml`
- `app/src/main/res/drawable/dialog_background_black_border.xml`
- `app/src/main/res/layout/nav_bar.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/popup_instance_selector.xml`
- `app/src/main/res/layout/activity_instances.xml`
- `app/src/main/res/layout/activity_content_management.xml`
- `app/src/main/res/layout/activity_content_list.xml`
- `app/src/main/res/layout/activity_settings.xml`
- `app/src/main/java/org/levimc/launcher/ui/activities/InstancesActivity.java`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`
- `app/src/main/res/values-idn/strings.xml`
- `IMPLEMENTATION_STATUS.md`

### Verification and remaining UI inconsistencies

- `git diff --check` passed.
- Basic XML element nesting and duplicate-attribute checks passed for all 17 edited XML resource files. App-owned resource-reference checks passed for the edited layouts/styles/colors/drawables; Material library styles and the library scrolling behavior were excluded from the app resource inventory.
- English and both Indonesian resource directories contain matching new empty/no-results strings. A contrast calculation passed the 4.5:1 threshold for secondary/body text against the revised background and surface colors in light and dark palettes.
- Source checks passed for instance selection, filtered-list empty state, home routes to Instances and Content Management, existing content import wiring, and existing settings-tab click handling.
- No profile persistence, content storage, launch validation, navigation destinations, account behavior, application identity, or service dependency was changed.
- Several secondary screens still contain individually styled controls and older spacing/color literals. The current pass standardizes shared shell screens and key interactions; a full per-screen visual audit on a device remains necessary.
- No Android compile, automated UI test, or runtime/device test is available here because Java is unavailable, `gradlew` is not executable, and Android SDK variables are unset.
- Next: complete the settings/configuration compatibility and migration audit, then verify light/dark layouts, screen-reader focus, and touch behavior on device.

## Milestone 6: Microsoft and Xbox integration

### Existing authentication architecture audited

- `MsftLoginActivity` supports the inherited Microsoft authorization-code WebView flow with a random state value checked against the redirect, and the browser-based device-code flow. `MsftAuthManager` uses the existing MinecraftAuth `BedrockAuthManager` and Microsoft/Xbox request classes; no endpoints or authentication libraries were replaced.
- Successful auth refreshes Microsoft, Xbox, Minecraft certificate, and optional PlayFab/Realms credentials, retrieves the Xbox profile and Bedrock certificate identity, then persists the serialized auth manager and account summary.
- `MsftAccountStore` keeps accounts in `filesDir/xal/Xal.Accounts.json` using `AtomicFile`. `XalExporter` reads native Minecraft refreshed credentials, exports the selected account to the existing per-user XAL files, and removes per-user XAL files when an account is deleted. The separate device-key preferences remain unchanged.
- Refresh uses the existing five-minute expiry margin and Microsoft refresh-token request; Xbox/XSTS and certificate credentials use MinecraftAuth's existing holders. Launch-time refresh runs only when launcher-managed Minecraft login is enabled, through `MinecraftRuntimePreparer`. Otherwise local launcher, pack, and world management do not require login.
- Account selection refreshes tokens before activation. Login and account-switch progress are shown in existing screens. Deleting an account is the inherited sign-out/removal path.
- The application explicitly fetches Xbox profile and Minecraft certificate identity. No distinct Minecraft entitlement-check endpoint/call was found in app source; no separate entitlement guarantee is claimed.
- `MinecraftAuth` remains the existing dependency and is currently pinned to `main-SNAPSHOT` in the version catalog, so dependency reproducibility remains a separate risk.

### Improvements implemented

- Provider-controlled OAuth error descriptions, Xbox/MSA API messages, WebView network descriptions, and generic exception messages are no longer copied verbatim into UI. Errors are classified into safe connection, expiry, account-policy, service, or generic messages. The authorization-code WebView still validates its `state`; returned OAuth error text is not displayed.
- Auth/account/XAL log calls now record only an error class name and a fixed operation description, not throwable messages or stack traces that could contain credential material.
- The active account row now has a Refresh action. It reuses `refreshAccountAndActivate`, shows a specific localized Microsoft/Xbox refresh progress message, and reports existing sanitized failures. Account detail text now says that the account is active on this device and prompts refresh to verify the session rather than implying its tokens are necessarily current.
- Removing any account now re-exports the remaining active account to the existing XAL store. Previously this refresh happened only when the account list became empty, leaving credentials for the removed account selected when another account remained.
- Added localized OAuth-declined, OAuth-service, WebView-network, active-account status, and account-refresh progress labels in English and Indonesian. Existing device-code polling, retry behavior, progress states, cancellation, token refresh, and launch-time validation remain in place.
- No auth endpoint, dependency, account schema, storage key/path, app identity, login setting, or entitlement validation was changed.

### Security and compatibility notes

- No access token, refresh token, authorization code, password, serialized auth JSON, provider error body, or exception stack is sent to logs or shown by the updated auth error paths. Device-code user code remains visible only in the existing sign-in UI because that flow requires the user to enter it at Microsoft.
- Existing serialized credentials remain in the app-private `xal` files and are exported to the inherited XAL layout required by Minecraft. No storage encryption or migration was attempted, to preserve existing account and game compatibility; device backup/root-compromise protections remain outside this change.
- Microsoft/Xbox entitlement enforcement remains whatever the existing MinecraftAuth flow and official game require; this milestone does not bypass or weaken it.

### Files changed in this milestone

- `app/src/main/java/org/levimc/launcher/core/auth/MsftAuthManager.java`
- `app/src/main/java/org/levimc/launcher/core/auth/MsftAccountStore.java`
- `app/src/main/java/org/levimc/launcher/core/auth/storage/XalExporter.java`
- `app/src/main/java/org/levimc/launcher/ui/adapter/AccountsAdapter.java`
- `app/src/main/java/org/levimc/launcher/ui/activities/AccountsActivity.java`
- `app/src/main/java/org/levimc/launcher/ui/activities/MainActivity.java`
- `app/src/main/java/org/levimc/launcher/ui/activities/MsftLoginActivity.java`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`
- `app/src/main/res/values-idn/strings.xml`
- `IMPLEMENTATION_STATUS.md`

### Verification and remaining limitations

- Authentication call sites reviewed: WebView redirect/state validation, device-code polling/cancellation, successful account save, account refresh/switch/delete, XAL import/export/cleanup, active account display, runtime preparation, and existing launch gate.
- Source audit confirmed refresh-token expiry checks and 401/expired-grant messaging remain active. Auth logging and user-facing provider error paths were checked for raw credential or response output. No explicit entitlement endpoint was identified.
- `git diff --check` passed. Basic XML structure checks passed for English and Indonesian string resources; account, OAuth, network, and refresh keys match across both Indonesian resource directories and English. Source assertions passed for redaction, refresh/expiry behavior, active-account refresh, account-removal XAL refresh, and the optional-login gate.
- Reviewed auth-related `Log` calls; the updated auth, account-store, and XAL exporter paths pass only fixed operation text and exception class names. This is source-level verification, not runtime logcat observation.
- No Android compile, authentication integration test, network flow, Microsoft/Xbox login, entitlement verification, or device test was run. Java is unavailable, `gradlew` is not executable, and the Android SDK is unconfigured.
- Existing app-private serialized auth data is not encrypted by a new mechanism, and `MinecraftAuth` is still a SNAPSHOT dependency. Entitlement behavior inside that library was not independently audited.
- Next: review settings/data migration compatibility, then pin/review auth dependencies and validate login, refresh, account removal, expired-session, and network-failure flows on a supported Android build/device.

## Milestone 6 security review: Microsoft/Xbox credentials

### Credential storage findings

- `MsftAccountStore` serializes the complete MinecraftAuth `BedrockAuthManager` JSON into `filesDir/xal/Xal.Accounts.json` using `AtomicFile`. This JSON may hold Microsoft access and refresh tokens; Xbox device, user, title, and XSTS tokens; PlayFab and Realms tokens; Minecraft session and certificate data; device/session identifiers; and device/session key pairs. The exact fields present vary by auth-manager state. The file is app-private, but is not encrypted by this application layer.
- `XalExporter` writes Minecraft-compatible files in `filesDir/xal/<base64-user-id>/`, including Microsoft access/refresh token data and Xbox/XSTS bearer-token envelopes. It copies the device identity key pair into app-private SharedPreferences named `org.levimc.xal.crypto`. These are plaintext at the application serialization layer; Android's app sandbox is access isolation, not application-level encryption.
- Refresh updates the saved BedrockAuthManager after importing any newer Minecraft-side refresh token. The app checks a five-minute expiry margin before refreshing Microsoft and the MinecraftAuth holders. Account removal deletes the user's XAL directory and then re-exports the newly active account, or clears all XAL data and device-key preferences when no account remains. No app-managed temporary credential files were found in the reviewed paths. Filesystem deletion is best effort for XAL directory entries; secure media erasure is not guaranteed.

### Encryption, backup, entitlement, and dependency assessment

- A Keystore-backed encrypted account-store migration is **deferred**. Existing `Xal.Accounts.json` is the compatibility source for account restore, while Minecraft requires the plaintext-compatible XAL exports. Safe migration requires a versioned encrypted format, atomic migration/rollback across the auth store and existing XAL consumer data, Keystore invalidation handling, and device tests. Starting such a migration without Android runtime coverage could strand accounts or break game sign-in. No stored account data was read or changed during this review.
- The app manifest previously relied on Android's default backup behavior and provided no explicit exclusions. Since `files/xal/` and `org.levimc.xal.crypto` are credential material, added legacy full-backup and Android 12+ extraction rules excluding only those locations for cloud backup and device transfer. Unrelated app data remains eligible for the existing platform backup behavior.
- App code explicitly retrieves the Xbox user profile and Bedrock Minecraft certificate identity from `BedrockAuthManager`; this is not a separate documented entitlement/ownership API call. The Bedrock authentication chain obtains Xbox/XSTS and a Minecraft certificate through MinecraftAuth. The current upstream manager source contains a `MinecraftCertificateChain` refresh request and serialization, but no explicit entitlement-named check. This does not establish a separate game-ownership verdict. No duplicate entitlement logic was added. Upstream library behavior must not be represented as an independently verified entitlement check.
- Dependency coordinate is `com.github.RaphiMC:MinecraftAuth:main-SNAPSHOT` through the version-catalog alias. Upstream currently publishes tagged releases, including 5.0.2, so stable releases exist. A replacement was not made: the app uses BedrockAuthManager APIs and serialized state whose compatibility with tagged releases has not been built or tested, and the SNAPSHOT's exact resolved commit could not be verified from this environment's available local source/artifact metadata.

### Changes and verification

- Added `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml`, and wired them from the application manifest. Only the XAL credential directory and device-key preference file are excluded; the app's backup behavior for unrelated user data is retained.
- No account schema, account identifier, auth flow, storage path, app identity, dependency, or entitlement behavior was changed. No cryptographic migration was attempted.
- Static source review covered account JSON serialization, XAL token export/import, key-pair preference writes, refresh and expiry paths, account deletion cleanup, manifest backup settings, app profile/certificate retrieval, and MinecraftAuth dependency configuration. The changed XML was checked for well-formed structure and the backup-exclusion paths were checked against the actual write locations. `git diff --check` passed.
- No credential values were emitted, copied, or inspected. No Android build, backup/restore exercise, Keystore test, live authentication, or runtime verification was performed. Java and Android SDK remain unavailable and the Gradle wrapper is not executable.
- Remaining risks: auth-manager JSON and Minecraft-compatible XAL exports remain plaintext inside the app sandbox; the external Minecraft client consumes XAL credentials and constrains encryption choices. Keystore migration requires a dedicated compatibility design and device verification. MinecraftAuth remains on a mutable SNAPSHOT. Separate entitlement behavior is not established by app source or the inspected upstream manager source.

## Milestone 7: Minecraft installation and version management

### Existing implementation reused

- `VersionManager` remains the single discovery and profile selection owner. It discovers the official package through `PackageManager`, custom instances through the existing launcher storage root, profile metadata through `VersionProfileMetadataStore`, and selection through the existing `version_manager` preference keys.
- `GameVersion` remains the installation/profile model. The dashboard already uses its selected profile and version metadata. `MinecraftLauncher` remains the only launch path and performs its existing supported-version, repair, and Play Store validation before starting the game.
- `InstancesActivity` already refreshes `VersionManager` when resumed from its management flows. No second profile manager, storage path, or launch validator was introduced.

### Improvements implemented

- Added a light-weight package fingerprint check using package name, version name/code, update time, source path, and native-library path. `MainActivity.onResume()` now asks `VersionManager` to compare current package metadata; it only repeats full version/profile discovery when Minecraft is installed, removed, or updated. A PackageManager runtime/visibility failure is treated as unknown and does not falsely mark the installation missing.
- `loadAllVersions()` now clears its in-memory selected profile before restoring the saved selection from the newly discovered lists. This prevents a removed installation or profile from remaining selected as a stale object. Existing preference keys and identifiers are unchanged; the normal existing fallback chooses an available profile when needed.
- Dashboard status and profile selection continue to use the refreshed `GameVersion` discovered state. Launch validation remains in `MinecraftLauncher`; the UI does not claim that a discovered installation has passed launch validation.

### Entitlement and dependency findings

- The app's Bedrock auth flow requests Xbox profile data and a Minecraft certificate chain through `BedrockAuthManager`, but has no explicit Minecraft entitlement/ownership call. The inspected upstream Bedrock manager source likewise exposes certificate/profile/token flow rather than a distinct entitlement check. A profile/certificate response is not treated as proof of a separately checked game entitlement.
- MinecraftAuth's v5.0.2 release notes report a method for Minecraft game entitlements/ownership, and a stable 5.0.2 release exists. The project still declares `com.github.RaphiMC:MinecraftAuth:main-SNAPSHOT`; this mutable coordinate does not identify the exact resolved commit. The entitlement method's applicability to this Bedrock flow and compatibility with the app's auth-manager API and persisted JSON could not be verified here, so no dependency upgrade or speculative call was added.

### Files changed, verification, and limitations

- Modified `app/src/main/java/org/levimc/launcher/core/versions/VersionManager.java`, `app/src/main/java/org/levimc/launcher/ui/activities/MainActivity.java`, and this status document.
- Static inspection covered the discovery scan, persisted selection restore, Instances Manager refresh, dashboard resume/selector/launch handlers, `MinecraftLauncher` validation, and MinecraftAuth imports/call sites. `git diff --check` passed. Source assertions confirmed fingerprint refresh wiring, refresh-before-home-state update, stale-selection clearing, and that launch still calls the existing `MinecraftLauncher.launch` path. Existing app-owned resource references were not changed.
- No profile models, install identifiers, preference keys, storage locations, launch parameters, account behavior, or application identity changed. No game APK/assets or download logic was added.
- No Java compiler, executable Gradle wrapper, or configured Android SDK is available. Android compilation, unit tests, PackageManager behavior, foreground install/update detection, and launch behavior were not runtime-tested. Package update detection uses Android package metadata; inaccessible metadata is intentionally left unknown until a later resume check.
- Remaining limitation: a listed installation is discovered/available according to PackageManager, but final launchability still depends on the existing `MinecraftLauncher` validations and runtime conditions. The exact MinecraftAuth SNAPSHOT resolution and entitlement API compatibility require a build with dependency resolution before any upgrade decision.

### Milestone 7 focused continuation: explicit installation state

- Added `VersionManager.MinecraftInstallationState` with `DETECTED`, `METADATA_CHANGED`, `NOT_FOUND`, and `CHECK_UNAVAILABLE`. A single `PackageInfo` snapshot now supplies both the fingerprint and discovered official installation data, avoiding a second package query during each full scan.
- If PackageManager temporarily throws or cannot answer, the manager reports `CHECK_UNAVAILABLE` and preserves its last known profile snapshot. On first launch it can still discover local profiles. If the saved selection points to an installed profile that cannot currently be resolved, `getSelectedVersion()` leaves that preference untouched for both unavailable checks and confirmed removal; the user can explicitly select a different local profile, or the original installed profile will restore if Minecraft returns.
- Confirmed removal clears stale installed-profile objects and restores selection from the existing preference data; package reappearance or a metadata fingerprint change triggers one profile scan. Version, update time, source path, and native library path feed that fingerprint. An update is surfaced as `METADATA_CHANGED` until the dashboard presents the update status and acknowledges it; an unchanged package check does not erase an update before the dashboard can display it.
- Dashboard status and the existing profile selector now report detected version, confirmed missing installation, metadata update, or temporarily unavailable check. Confirmed missing installation disables the dashboard launch control; temporary check failure is shown distinctly and does not claim the package is absent. `MinecraftLauncher` remains the final launch validator; failures continue to show the existing launch-failed state and error dialog.
- Added localized English and Indonesian status strings. Profile preferences, profile IDs, storage paths, launch path, and MinecraftAuth dependency remain unchanged.
- Static checks: `git diff --check` passed; source assertions covered query result distinctions, snapshot preservation on temporary failure, selected installed-profile preference retention, change-triggered scanning, dashboard state presentation, selector status resource references, and existing `MinecraftLauncher.launch`/Play Store validation call sites. XML structural/resource checks were run on the changed layout and strings where available.
- Runtime/build: not verified. Java is unavailable, `gradlew` is not executable, and the Android SDK is not configured. PackageManager install/uninstall/update conditions, activity lifecycle, UI rendering, and launch validation require an Android build/device test. `NameNotFoundException` remains the platform result treated as confirmed absence; PackageManager does not distinguish every possible visibility configuration failure from absence.

## Milestone 8: Library and catalog integration

### Existing catalog and local content architecture reused

- `CurseForgeActivity` and `CurseForgeClient` are the existing external-content catalog path. They already provide paged search, category filtering (Add-Ons, maps, skins, textures, scripts), popular/download/recently-updated/name sorting, and navigation to the existing details screen. This is a single-provider adapter in the current project; no new catalog framework or module was needed.
- `ContentDetailsActivity` displays the provider title, author, counts, category, modified date, artwork, description, and source page. It obtains provider files and feeds downloaded archives to `ContentImporter`.
- Local import continues through `ContentImporter` → `ContentArchive`; existing Bedrock manifest inspection, UUID duplicate detection, staged extraction, traversal checks, and destinations for resource, behavior, skin, and world content are reused. A downloaded item is imported into local storage only; no pack is enabled in a world.
- `core/packages/Catalog` is the separate LeviLamina/native package catalog, not a Bedrock content source. It was not repurposed. The existing dashboard route to CurseForge and existing local Content Management route remain unchanged.

### Improvements implemented

- Added the official CurseForge file `isAvailable` and `fileLength` metadata to the existing file model. Unavailable files or entries without a direct URL now lead to the creator's HTTPS page instead of a forced direct download. A missing/unknown availability flag remains compatible with existing API responses if a direct URL is present.
- Catalog downloads now require HTTPS and supported archive suffixes (`.mcpack`, `.mcaddon`, `.mcworld`, `.mctemplate`, or `.zip`), use a generated cache filename rather than provider-supplied path text, reject metadata or streamed sizes above 100 MiB, and reject a received byte count inconsistent with provider file metadata. Canceled or failed downloads are cleaned up. Completed downloads use the existing local importer; malformed archives continue to fail through its validation.
- `ContentArchive` now limits each ZIP archive to 20,000 entries and 256 MiB of expanded file data, checking both declared sizes and bytes actually written into its staging directory. This protects local SAF imports and catalog imports alike while retaining staged extraction and path traversal rejection.
- Catalog search and file-loading errors now show localized, generic messages rather than raw provider/network exception text. Empty search results and unavailable catalog states are distinct. Category names, sort labels, download counts, source attribution, unsupported-file, unavailable-file, and size-limit messages have English and Indonesian strings.
- CurseForge request construction omits an empty API-key header. No new API key, dependency, endpoint, credential, or proprietary service was added.
- Installed catalog markers are not shown. CurseForge search metadata does not include a Bedrock manifest UUID, so a name-only match would be unreliable. Imported content remains viewable and manageable through the existing local library.

### External-source and licensing review

- Evaluated `ModderzEnterprises/modhub-for-minecraft-bedrock`. Its public GitHub page identifies an MIT license and its README describes multi-source search, category filters, details, downloads, and opt-out fallback. The visible repository history is small (five commits and one star at inspection). Its source tree/license text could not be independently fetched through the available web reader, so no code was copied and its provider adapters or implementation were not treated as audited/reusable. MIT covers that repository's code, not third-party catalog data, artwork, or downloads.
- Existing CurseForge requests use the official API and retain the existing categories. CurseForge requires an approved, unique API key for API use and its third-party API terms prohibit sharing the key ([API key application](https://support.curseforge.com/support/solutions/articles/9000208346-about-the-curseforge-api-and-how-to-apply-for-a-key), [third-party API terms](https://support.curseforge.com/support/solutions/articles/9000207405-curseforge-3rd-party-api-terms-and-conditions)). Milestone 8.1 confines any configured developer key to local Debug builds; Release and Beta compile an empty key and disable catalog access. No key value was read, printed, or changed. Direct download authentication is required by CurseForge for CDN requests after July 16, 2026 ([download key rollout](https://blog.curseforge.com/introducing-api-key-authentication-for-curseforge-file-downloads/)); authorized Debug requests send the configured key, while Release/Beta do not attempt downloads. File `isAvailable` semantics are from the [official API schema](https://docs.curseforge.com/rest-api/).
- Modrinth was not added: its documented project types target Java/other supported project categories and do not provide a verified Bedrock Add-On catalog in this integration. MCPEDL was not added because a documented permitted API and applicable access/content terms were not verified; no scraping was introduced.
- CurseForge files that are available through its API remain creator-owned material and are not copied into Oblivion resources. The app links back to the project page, does not infer missing creator/version/compatibility data, and does not auto-enable content. CurseForge category metadata is not a guarantee that an archive is compatible with a user's installed Minecraft version.

### Files changed in this milestone

- `app/src/main/java/org/levimc/launcher/core/content/ContentArchive.java`
- `app/src/main/java/org/levimc/launcher/core/curseforge/CurseForgeClient.java`
- `app/src/main/java/org/levimc/launcher/core/curseforge/models/ContentFile.java`
- `app/src/main/java/org/levimc/launcher/ui/activities/ContentDetailsActivity.java`
- `app/src/main/java/org/levimc/launcher/ui/activities/CurseForgeActivity.java`
- `app/src/main/java/org/levimc/launcher/ui/adapter/FileListAdapter.java`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`
- `app/src/main/res/values-idn/strings.xml`
- `IMPLEMENTATION_STATUS.md`

### Verification, limitations, and next work

- Source review covered existing catalog navigation and search/category/page callbacks; detail metadata and source-page behavior; file availability and URL selection; the existing ContentImporter → ContentArchive path; storage destinations; and manifest-UUID duplicate handling.
- Static verification performed for this milestone: `git diff --check` passed. Node-based structural checks passed for the English and two Indonesian string XML files, duplicate string names, new catalog key parity, and references used by the changed Java screens. Source assertions passed for HTTPS-only download, streamed size and metadata checks, availability fallback, reuse of `ContentImporter`, archive entry/expanded-byte/manifest bounds, catalog category wiring, and generic error-state wiring. These are source-level checks and do not execute archive parsing or network requests.
- No CurseForge API requests or downloads were made during this implementation. The environment has no Java, the Gradle wrapper is not executable, and Android SDK configuration remains unavailable; Android compilation, unit tests, resource linking, and device behavior therefore remain unverified.
- Remaining concerns: public catalog availability requires a confirmed authorization model; CurseForge catalog coverage of Bedrock content is provider-dependent, and there is no verified second provider. No reliable UUID-based installed-content correlation is available from current catalog metadata. Archive limits are per ZIP (nested ZIPs are each bounded separately).
- Suggested next milestone: add a second documented Bedrock source only after confirming API access, redistribution rules, and data attribution. Add focused archive bound tests and exercise import/fallback/network states on an Android build/device.

## Milestone 8.1: CurseForge integration security and distribution review

### API key source and build-variant boundary

- The only app Gradle property source is `curseforge.api_key` in root `local.properties`. `local.properties` is ignored by Git and was absent in the audited worktree. No CurseForge API key value was read or printed. The prior Android Actions workflow also read the GitHub secret name `CURSEFORGE_API_KEY`, wrote it to `local.properties`, and built Release/Beta APKs; that injection step has been removed.
- `gradle.properties`, the version catalog, Android resources, manifest, Java/Kotlin sources, and current generated build output contain no separate key setting or generated key configuration. No CurseForge key source via Gradle project property or environment variable was found. No matching hardcoded key credential pattern was found in tracked or ordinary untracked source files. Environment inspection reported names only.
- Gradle now has empty/disabled defaults, explicit empty key and disabled API for Release and Beta, and a local-property key only in Debug. A configured local key is extractable from a Debug APK and is for local development only. It is not included in Release or Beta BuildConfig. The app provider client refuses to issue catalog API requests or downloads when its generated variant flag is disabled.
- The API key must be approved for this specific app and must not be shared. No proxy/backend or key-sharing mechanism was added. Public distribution of CurseForge API functionality remains disabled until a compliant authorization model is confirmed in writing. A developer must not publish or share a Debug APK built with a key.

### API response retention and unavailable behavior

- `CurseForgeClient` uses OkHttp without a configured response disk cache. Search results, descriptions, and file metadata are held in activity/model memory for the active UI only; no catalog database, preference, history, or persistent metadata cache was found.
- Catalog list/detail artwork was loaded through Glide defaults, which cache data. Glide disk and memory caching are now disabled for both provider image call sites. WebView uses `LOAD_NO_CACHE` for the API description. These settings were source-inspected only; platform runtime caching was not measured.
- Download archives are written to a generated cache temporary file, passed to existing `ContentImporter`, then deleted on success or failure. Imported packs/worlds remain in existing user-selected local storage as user content; no API metadata is separately retained. Written clarification from CurseForge is still needed if its terms could treat this artifact handling as restricted.
- Release/Beta catalog screens show localized unavailable text and make no provider API calls. The direct download action is likewise disabled. Local Add-On/world management, SAF imports, content storage, Minecraft launch, and profiles do not depend on the catalog gate. Debug can browse only when its local key is configured; API/network failures use the localized unavailable/error state and no fake results are shown.

### CDN behavior and verification

- HTTPS validation, 100 MiB download cap, generated temporary names, provider `isAvailable` check, creator-page fallback, and existing `ContentImporter` integration are preserved. Authorized Debug requests use the configured key on the HTTPS CDN request. Release/Beta do not attempt direct downloads. No CDN request was made, so no direct download is claimed as runtime-functional.
- The key-bearing downloader accepts only `https://edge.forgecdn.net` and HTTP redirects are disabled for the CurseForge client, preventing an API-supplied URL or redirect from forwarding the key to another host. Redirected downloads use the existing creator-page fallback.
- Files changed in Milestone 8.1: `app/build.gradle`, `.github/workflows/android.yml`, `app/src/main/java/org/levimc/launcher/core/curseforge/CurseForgeClient.java`, `app/src/main/java/org/levimc/launcher/ui/activities/CurseForgeActivity.java`, `app/src/main/java/org/levimc/launcher/ui/activities/ContentDetailsActivity.java`, `app/src/main/java/org/levimc/launcher/ui/adapter/CurseForgeContentAdapter.java`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values-in/strings.xml`, `app/src/main/res/values-idn/strings.xml`, and this document.
- Static checks: `git diff --check` passed. A sanitized source scan reported only locations/configuration names and did not print secret values. Gradle variant assertions and workflow checks confirmed Release/Beta empty/disabled fields, Debug-only local property use, and removal of CI key injection. XML/resource structure and English/Indonesian catalog string parity/reference checks passed. Source assertions confirmed client-side gate checks, HTTPS-only download request creation, exact CDN host validation, no automatic redirects, no configured OkHttp cache, Glide cache disabled in both image call sites, WebView no-cache mode, and existing importer integration.
- Build/runtime: unverified. Java and Gradle are unavailable, `gradlew` is not executable, Android SDK variables are unset, and no APK/generated BuildConfig exists to inspect. No API key value or credential was emitted during this audit. No Android compile, catalog call, CDN request, local import, or runtime cache test was performed.
- Remaining requirement: obtain written CurseForge authorization for this app's API use, CDN key use, and any retained/downloaded content handling before enabling catalog access in distributed variants. The Terms prohibit sharing the developer API key; a client-distributed key is not a valid release solution. Local launcher and content workflows remain available independently.

## Change record

### 2026-11-10 — Initial Oblivion milestone

- Changed only visible launcher display-name resources and the beta display name.
- Added this status inventory.
- No dependencies, application identifiers, package namespaces, storage paths, preference keys, auth flows, or native behavior changed.
- Tests/build: `./gradlew :app:testDebugUnitTest --no-daemon` attempted, but the wrapper could not start because it is not executable; Java and Android SDK are also unavailable. `git diff --check` passed. Python-based XML validation was unavailable because Python is not installed.
- Next: review and test content archive import safety using existing code/tests, then improve local functionality without replacing current managers.

## Milestone 9: AI assistant integration

### Architecture and implementation

- Added provider-independent models and contracts under `app/src/main/java/org/levimc/launcher/core/ai/`: `AIProvider`, `AIService`, `AIProviderConfig`, `AIRequest`, `AIResponse`, `AIMessage`, `AIConversation`, `AIContextField`, `AICapability`, `AIAvailability`, and `AIError`. The contracts do not depend on Android UI, Firebase, CurseForge, or a proprietary backend.
- Added `UnavailableAIProvider`, the provider composed into the current app. It performs no network activity, reports `NOT_CONFIGURED`, and cannot generate a placeholder response. `AIService` checks opt-in, provider availability, HTTPS endpoint metadata, capability support, and catches provider runtime failures. Provider documentation specifies bounded requests/responses, timeouts, cancellation, no retries, and no request logging for future implementations; no HTTP provider is implemented yet.
- Added a session-only assistant activity with topic selection for Add-On discovery, metadata explanation, compatibility assistance, and troubleshooting. The user can type a prompt, see loading/error/empty/unavailable states, clear the conversation, and reach the screen from the existing home dashboard. Since no provider exists, the app shows unavailability and will not send the prompt or fabricate catalog/content answers.
- Conversation history exists only in memory for the current activity session and is bounded to 32 messages. It is not serialized. The opt-in preference is `ai_assistant_privacy/external_requests_opt_in`, defaults to false, requires a notice confirmation, and is revoked by turning the switch off; opting out clears session history and cancels the provider. The prompt input is excluded from Android view-state saving.
- Request context is explicit and restricted to typed fields; the UI currently supplies no metadata or diagnostic context. Worlds, pack archives, account state, credentials, and crash logs are not automatically read or attached. Common credential patterns are redacted from messages/context, but this is heuristic protection only; localized privacy text now warns users not to enter secrets or private information. Future providers must still minimize and validate outbound data.

### Files added or changed for this milestone

- Added `app/src/main/java/org/levimc/launcher/core/ai/AIAvailability.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AICapability.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIContextField.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIConversation.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIError.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIMessage.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIPrivacySanitizer.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIProvider.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIProviderConfig.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIRequest.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIResponse.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/AIService.java`
- Added `app/src/main/java/org/levimc/launcher/core/ai/UnavailableAIProvider.java`
- Added `app/src/main/java/org/levimc/launcher/ui/activities/AIAssistantActivity.java`
- Added `app/src/main/res/layout/activity_ai_assistant.xml`
- Added `app/src/main/res/drawable/ic_ai_assistant.xml`
- Modified `app/src/main/AndroidManifest.xml` to register the assistant screen.
- Modified `app/src/main/java/org/levimc/launcher/ui/activities/MainActivity.java` and `app/src/main/res/layout/activity_main.xml` to add dashboard navigation.
- Modified `app/src/main/res/values/strings.xml`, `app/src/main/res/values-in/strings.xml`, and `app/src/main/res/values-idn/strings.xml` with English and Indonesian UI/privacy/error text.
- No Gradle dependencies, secrets, backend endpoints, or provider credentials were added.

### Verification and limitations

- `git diff --check` passed before this milestone's final text adjustment; rerun below for final state.
- Source review verified the activity composes only `UnavailableAIProvider`, the provider has no network code, requests have bounded payloads and explicit typed context, no AI request is automatically populated from content/account/crash data, and dashboard navigation/activity registration are connected.
- Android resource XML, manifest, resource references, and English/Indonesian key parity are checked in the final validation pass. Credential scanning is source-only and must not print any detected values.
- Implemented and source-verified: provider contracts, unavailable behavior, opt-in confirmation/state, session-only history, clear/cancel behavior, UI states/topic selection, and navigation wiring.
- Not implemented: a real AI backend/provider, catalog query execution, metadata injection, compatibility analysis, or error-context collection. The listed topics are prepared UI capabilities only. No external request can currently occur.
- Runtime/build status: not tested. Java is unavailable, the Gradle wrapper is not executable, and Android SDK configuration is unavailable in this environment. No Android compilation, network request, or device interaction is claimed.
- Remaining risks: common-pattern secret redaction cannot guarantee detection of arbitrary secrets pasted into free text. A future provider requires a reviewed authorization/deployment arrangement, concrete bounded HTTPS transport/cancellation behavior, and explicit confirmation of data handling. Do not treat the UI opt-in as permission to upload content or diagnostics automatically.
- Suggested next milestone: define the provider/backend trust and data-retention contract before implementing any real provider; preserve this disabled default until then.

## Milestone 10: Cloud, account and sync architecture

- **State: design review complete; no backend/account/sync code implemented.** Existing Microsoft/Xbox authentication remains in `MsftAuthManager`, `MsftAccountStore`, and `XalExporter`; it stores credential-bearing serialized auth state and XAL files in `filesDir/xal/` and device-key data in `org.levimc.xal.crypto` SharedPreferences. The existing Accounts screen is for this Minecraft account workflow, not an Oblivion account. XAL credentials remain explicitly excluded from Android cloud backup and device transfer by the existing Milestone 6.1 rules.
- Reviewed existing `feature_settings/settings_json`, version/profile metadata and storage paths, local world/Add-On libraries, SAF world/instance backup, and Android Auto Backup configuration. The new `docs/CLOUD_ACCOUNT_ARCHITECTURE.md` classifies data and specifies opt-in, staging, preview, conflict handling, offline/resume, encryption, deletion and restore rules.
- The design keeps Oblivion identity separate from Microsoft/Xbox identity; proposes provider-neutral public contracts and E2E encrypted, user-selected backups; and keeps local launcher functionality offline-first. No provider, dependency, endpoint, secret, payment, entitlement, storage path, preference key or app identity was added or changed.
- Provider review recommends no current provider selection/deployment. Appwrite is the Java-compatible candidate for later evaluation; Supabase and Firebase tradeoffs, current public quotas/pricing, SDK/security constraints and relevant official sources are documented. Managed usage costs and operations require explicit ownership/budget approval.
- Premium architecture remains a recommendation only: server-authorized entitlements, no client trust, no local feature gate, and no payment implementation.
- Static checks: `git diff --check` passed; documentation links and source findings were reviewed. This is not an Android build, backup/restore exercise, server security review, or runtime test. Java/Gradle/Android SDK limitations recorded earlier remain unchanged.
- Decisions requiring approval are listed in the design document: backend/account ownership, provider/hosting and budget, data scope/retention, E2E recovery model, conflict policy, privacy/legal/age process, and future premium/payment authorization.

## Milestone 11: Advanced features and personalization

### Implemented

- Added `LocalPersonalizationStore`, using the separate app-private SharedPreferences file `oblivion_local_personalization`. New keys are `favorite_profiles`, `favorite_content`, `recent_world_<sha256-id>`, and `dashboard_<section-or-action>`. Existing preference files and keys were not migrated or rewritten. Missing dashboard values default to visible. Reset removes only keys with the new `dashboard_` prefix.
- Added local dashboard visibility controls in Settings → Personalize → Dashboard. Users can show/hide the existing Mods, local content, and shortcuts cards, plus individual existing shortcut rows. Dashboard preferences are reapplied on activity creation and resume. Essential selected-profile and launch controls cannot be hidden. Existing section order is retained; drag reordering is not implemented.
- Added profile favorites and a Favorites filter in Instances Manager. Favorite profiles sort before non-favorites while selected-profile behavior and profile identifiers remain unchanged. Favorite markers remain stored when a profile is temporarily unavailable and can apply again if that same stable ID returns.
- Added favorites for local worlds and resource/behavior/skin/add-on packs, plus favorite filtering and favorite-first ordering. Pack favorites use manifest UUID and pack type where available, with canonical-path hashing as fallback. World favorites use canonical-path hashing. Favorite operations only update local metadata and do not enable, delete, or edit packs/worlds.
- Added a Recently Opened filter for worlds. It records a local timestamp only after a valid world is explicitly opened with Oblivion’s existing world editor; it is not Minecraft play history. Pack history was not added because the launcher has no reliable signal that a pack was used.
- Reused existing navigation, launch, content adapters, managers, world editor, profile persistence, and settings screen. No provider, cloud, AI, account, or analytics dependency was introduced. English and Indonesian resources include new labels and empty states.
- Increased the Personalize subtab touch targets to 48dp for theme, wallpaper, and dashboard tabs.

### Files added or changed for this milestone

- Added `app/src/main/java/org/levimc/launcher/util/LocalPersonalizationStore.java`.
- Modified `app/src/main/java/org/levimc/launcher/core/content/ResourcePackItem.java` to expose existing manifest UUID and pack type for stable local favorite identifiers.
- Modified `app/src/main/java/org/levimc/launcher/ui/activities/MainActivity.java`, `InstancesActivity.java`, `ContentListActivity.java`, and `SettingsActivity.java`.
- Modified `app/src/main/java/org/levimc/launcher/ui/adapter/WorldsAdapter.java` and `ResourcePacksAdapter.java`.
- Modified `app/src/main/res/layout/activity_main.xml`, `activity_instances.xml`, `activity_content_list.xml`, `activity_settings.xml`, `item_instance_card.xml`, `item_world.xml`, and `item_resource_pack.xml`.
- Modified English and Indonesian strings in `app/src/main/res/values/strings.xml`, `app/src/main/res/values-in/strings.xml`, and `app/src/main/res/values-idn/strings.xml`.
- Updated this status document. No dependencies, app identity, storage paths, existing preference keys, backup rules, profile IDs, or launch/auth behavior were changed.

### Verification and limitations

- Source review covered dashboard resume application, visibility defaults/reset behavior, action row IDs and existing click handlers, profile favorite sorting/filtering, content favorite callbacks, UUID/path based content identity, and world-editor visit recording.
- Static checks passed: `git diff --check`; XML nesting across 354 resource XML files; duplicate string-name checks and English/Indonesian parity across 1,422 keys; Java/Kotlin `R.string` references across 285 source files; layout string references; and eight source assertions covering dashboard refresh/default/reset, favorite profile filtering, UUID identity, editor-based recent history, settings wiring, and no remote-service coupling in the preference store. The reference check also found `MainActivity` used `R.string.unknown` without a base resource; added the missing English and Indonesian values.
- Build and runtime are unverified: Java and Android SDK are unavailable and the Gradle wrapper is not executable in this environment. No Android compilation, unit test, device interaction, preference-persistence runtime test, or on-device accessibility audit is claimed.
- Remaining limitations: dashboard sections cannot yet be reordered; recent history is limited to explicitly opening a world in Oblivion’s editor; favorite fallback identity is path-based when pack UUID is absent; favorites are app-local and are not cloud-synchronized. Small-screen and contrast behavior require device review.
- Suggested next milestone: prioritize build/device verification of accumulated local content and personalization changes; add dashboard reordering only if it can safely preserve essential launcher controls and current navigation.

## Milestone 12: Optimization, security and release preparation

### Audit scope and concrete changes

- Re-reviewed the three Gradle modules, manifests/entry points, main launch/profile/content/auth/catalog/AI/personalization/crash call paths, backup rules, network configuration, update callers, native build hooks, submodule pins, assets, root and component notices, wrapper configuration, and GitHub Actions workflow. Detailed release findings and a device matrix are in `docs/RELEASE_READINESS.md`; the cloud/account design remains in `docs/CLOUD_ACCOUNT_ARCHITECTURE.md`.
- Removed the automatic and manual in-app release checks that pointed at `LiteLDev/LeviLauncher-Android`. Settings now reports in English/Indonesian that Oblivion release updates are not configured. No upstream APK will be offered through these app entry points until an Oblivion release channel is reviewed.
- Changed the Android network security base policy to disallow cleartext HTTP. Normal system certificate validation remains enabled; debug user certificates remain limited to debuggable apps by Android's debug-overrides behavior. Runtime regression testing is still required.
- Added a 256 MiB compressed input limit before content archives are copied to cache or processed. Existing 256 MiB expanded-data and 20,000 entry limits remain. This operation stays on the existing import executor.
- Removed full incoming Minecraft URI values and parsed actions from debug logs because invite/import/server query parameters may contain private information.
- Made `gradlew` executable, pinned the Gradle 8.13 distribution SHA-256 from Gradle's official checksum reference, explicitly installed Android API 36, Build Tools 36/35, and CMake 3.22.1 in CI, and made unit tests independent of presence of Firebase secrets. No workflow/release was triggered.
- Removed one redundant `conscrypt-android` declaration from the app dependency block; the dependency remains through the single existing declaration and inherited module use.
- Added `THIRD_PARTY_NOTICES.md` with observed notices, direct dependency/version inventory, submodule/native/font/Minecraft-module provenance gaps and concrete release obligations. Added `docs/RELEASE_READINESS.md` with priority findings, feature regression states, and Android test matrix.
- M12.2 traced catalog native-code import and load behavior. New catalog imports are disabled before the install callback; local SAF imports and existing enabled state on updates are preserved, and English/Indonesian feedback explains that new catalog mods start disabled.
- Removed the unused `org.cloudburstmc.protocol:bedrock-connection` SNAPSHOT version-catalog alias after confirming no Gradle consumer. Kept the actually used MinecraftAuth SNAPSHOT unchanged.
- Removed Android CI's tag trigger, credential use, APK artifact uploads, signing, GitHub Release and Discord APK notification steps. Retained safe release-content validation, Debug build, and unit-test jobs with read-only token permissions. Added read-only permissions to the manual inspection workflow.

### Findings that remain open

- Release distribution is blocked by unresolved licenses/provenance for four prebuilt native `.so` files, `preloader-android`, source/resources in the Minecraft compatibility module, MiSans font, and the pinned submodule/artifact relationships. `THIRD_PARTY_NOTICES.md` labels these unresolved rather than assigning guessed licenses.
- Microsoft/Xbox/Minecraft serialized auth state remains plaintext in app-private files. Existing Android backup rules exclude XAL files and crypto preferences, but no Keystore encryption migration is implemented or runtime-tested.
- `MinecraftAuth` (`com.github.RaphiMC:MinecraftAuth:main-SNAPSHOT`, JitPack) is directly used by `MsftAuthManager` and remains mutable; no resolved dependency graph or stable-version compatibility test is available. `bedrock-connection` (`org.cloudburstmc.protocol:bedrock-connection:3.0.0.Beta10-SNAPSHOT`, OpenCollab snapshot repo) had a version-catalog alias but no Gradle dependency use; its unused version and alias were removed. No dependency lock state or Gradle verification metadata exists.
- The Gradle Wrapper JAR hash is `e996d452d2645e70c01c11143ca2d3742734a28da2bf61f25c82bdc288c9e637`, which differs from Gradle's official 8.13 wrapper JAR checksum. The distribution ZIP checksum is pinned, but wrapper JAR provenance remains unresolved; it was not replaced.
- Android CI no longer has a tag trigger, APK artifact uploads, signing, GitHub Release, or Discord APK notification steps. It retains release-content validation, a Debug compile, and unit tests with a CI-only Firebase placeholder and no secrets. All Actions references remain mutable tags. GitHub-side branch/tag protection and reviewer-gated environments require external configuration and were not inspected.
- Debug build embeds a configured local CurseForge key in `BuildConfig`; that key is extractable. Release/Beta force the key empty and integration disabled. No APK or generated BuildConfig was available for binary inspection.
- `versionCode` is time-derived; untagged `versionName` falls back to `unknown`. Debug build type is minified and non-debuggable. Resolve the release version policy and test upgrades before distribution.
- Archive extraction has per-ZIP limits, but recursive nested archives do not yet share a total depth/work budget. The community ModCatalog installer now has bounded connection/read/write/call timeouts and a 256 MiB declared, response-header, and streamed download limit. The feed/news clients still use default timeouts and unbounded response reads; avatar image decoding also lacks uniform size bounds. No performance measurements were made.
- The external mod catalog accepts native `.so` packages and SHA-256 metadata is optional. Newly created catalog mods now start disabled, but users may explicitly enable them; publisher trust, provenance, and integrity remain a High supply-chain concern.
- Manifest/source review found a non-exported FileProvider with explicit URI grants but broad configured path roots. `IntentHandler` is exported for incoming Minecraft/content URIs; `MainActivity` is exported without an intent filter. No export/path changes were made because external callers and URI grants need runtime compatibility testing.
- Local settings and content remain offline-capable; AI is still a no-op provider; Oblivion cloud/account/sync are design-only. The app remains a community launcher and does not claim Mojang/Microsoft affiliation.

### Verification and environment

- `git diff --check` passed after Milestone 12 changes.
- Static Node checks passed for XML structure, string reference/parity checks from Milestone 11, manifest/network policy assertions, updater source wiring, input-archive bounds, and update-unavailable localization. A credential scan reported locations/configuration names only and emitted no values.
- `sh -n gradlew` passed. The submodule state is pinned but uninitialized for `libHttpClient` (`f8ae1d9cf931d9d55542865eee395adca5bdbe41`) and `preloader` (`cc08b40a31471d3e2dedb267f5f17b28da2b0604`). No Gradle dependency graph could be resolved; MinecraftAuth `main-SNAPSHOT` remains mutable. The unused Bedrock SNAPSHOT alias was removed.
- M12.2 traced external catalog downloads through FileHandler/ModManager to runtime `System.load`. Newly imported catalog mods are now disabled before the success callback; local SAF behavior and existing enabled state on overwrite are preserved. English and Indonesian success feedback explains that new catalog mods start disabled.
- Final M12.2 static assertions passed for the catalog-disabled flow, local-import delegation, completion ordering, localized resources, native loader call, removal of the unused Bedrock alias, retention of MinecraftAuth, read-only workflow permissions, absence of Android CI secrets/tag/release/artifact steps, and retention of Debug build/unit-test steps. English/default and Indonesian resource key counts match at 1,053 each. `git diff --check` and `sh -n gradlew` passed.
- No YAML parser/actionlint or XML validator was available for the final M12.2 pass. Workflow conclusions are based on source inspection and assertions, not parsed/executed workflows. Earlier M12 resource/XML static validation remains recorded above.
- `scripts/validate_release_content.py` could not run because Python is unavailable. Java is unavailable, Android SDK variables are unset, and no installed Gradle exists; Android build, Gradle dependency resolution, unit tests, binary APK/key inspection, runtime behavior, and device tests were not performed.
- `gradlew` now has executable mode, but this does not remove the Java/SDK requirement. CI workflow correctness is source-inspected only and has not been executed.
- Full release readiness remains blocked. Use the Critical/High priority list and test matrix in `docs/RELEASE_READINESS.md` before internal/beta/public distribution decisions.

### Pause checkpoint — 2026-11-11

The user requested a safe pause before Milestone 12 was finished. Current work is intentionally **IN PROGRESS**, not a completed release audit. All uncommitted Milestones 1–11 changes remain in place; nothing was reset, stashed, discarded, committed, or pushed.

#### Completed during this partial Milestone 12 pass

- Added release-readiness and third-party provenance documents.
- Disabled both in-app checks that targeted LeviLauncher upstream APK releases; Settings now reports that Oblivion release updates are not configured.
- Disabled cleartext network traffic in the app's network security configuration.
- Bounded local archive input to 256 MiB before processing, retaining existing per-ZIP expanded-data and entry-count limits.
- Removed logging of full Minecraft deep-link URI values; ordinary class-detection logs remain.
- Made `gradlew` executable, pinned the Gradle 8.13 distribution checksum, clarified CI SDK platform/build-tools/CMake setup, and removed the Firebase-secret condition from the unit-test step.
- Removed a duplicate direct `conscrypt-android` app dependency declaration.
- The documented audit records unresolved native/Minecraft-source/font/submodule provenance, mutable SNAPSHOTs, plaintext credential storage, wrapper JAR checksum mismatch, updater design needs, and device/build limitations.

#### Current checks and incomplete work

- `git diff --check` passed at the pause checkpoint; `git status --short` confirms the existing large uncommitted milestone work remains. No reset/stash/commit/push occurred.
- Earlier validation passed XML nesting, English/Indonesian parity and Java/Kotlin/layout resource-reference checks for app and library resources, plus JSON parsing. The last combined source-assertion script stopped at an overly broad assertion that expected `IntentHandler` to contain no `Log.` calls; it correctly retains unrelated class-detection logs. Replace that assertion with one checking that incoming URI values/event text are not logged, then rerun the complete script and capture its final output.
- Credential scan emitted no credential values or credential-shaped candidate assignments. It found no local `local.properties`, no configured CurseForge key, no tracked `google-services.json`, and no generated APK/BuildConfig output. No actual key/token values were printed.
- `sh -n gradlew` passed in a prior check. Java, Python, and Android SDK are unavailable; Gradle/Android builds, dependency resolution, unit tests, Python release validation, APK inspection, runtime, and device tests remain unperformed.
- Wrapper JAR provenance remains open: local SHA-256 differs from Gradle's official Gradle 8.13 wrapper JAR checksum. Distribution ZIP checksum is pinned, but do not replace the JAR without independently verifying the official artifact and preserving/reviewing its provenance.
- The code/doc audit is not complete. Remaining required work: inspect all lifecycle/background/network/image call paths and update mechanism references; complete manifest/exported-component, file-provider and update security assertions; audit dependency aliases/transitives and CI release permissions/actions; finish source/regression and archive-limit review; rerun all available static checks after corrections; and finalize the release recommendation in the docs.

#### Exact continuation steps

1. Read this checkpoint and `docs/RELEASE_READINESS.md`; keep the current branch and preserve all current work.
2. Re-run the source/resource/manifest/credential static checks, correcting only the URI-log assertion as described; confirm `git diff --check`.
3. Finish review of update/network/lifecycle and GitHub Actions release permissions; do not enable an updater or publish anything.
4. Verify the direct and transitive dependency inventory, submodule status and notices to the extent available. Keep unresolved license/provenance marked unknown; do not upgrade SNAPSHOTs without build and compatibility evidence.
5. Complete `IMPLEMENTATION_STATUS.md`, `THIRD_PARTY_NOTICES.md`, and `docs/RELEASE_READINESS.md` with final pass/fail results and remaining release blockers.
6. Report actual environment limits. Do not claim build/runtime/device verification, and do not commit or push.

### Resume update — 2026-11-11

The continuation steps above were carried out to the extent supported by this environment; the earlier pause checkpoint is historical and its request to rerun/finalize the docs is superseded by this update. `ModCatalogInstaller` was reviewed end-to-end and now applies bounded network timeouts and a 256 MiB declared, response-header, and streamed download limit while retaining optional SHA-256 verification, temporary-file cleanup, and the existing importer handoff. `docs/RELEASE_READINESS.md` now records remaining feed/image bounds, native `.so` catalog integrity risk, and exported component/FileProvider scope findings.

Final status remains **IN PROGRESS / release blocked**, not complete: unresolved license/provenance, plaintext auth storage, mutable dependencies, wrapper JAR provenance, CI release protections, catalog trust, and unavailable build/runtime/device verification remain. Exact next actions are to resolve Critical provenance blockers; establish native catalog publisher/integrity policy; bound remaining feed/image operations; review Actions SHAs and host-side release protection; add archive-wide recursion/work budgets; resolve dependency/transitive notices; obtain a reproducible CI build; and execute the release test matrix plus signed APK review. Do not distribute an APK before these gates are resolved.

## Milestone 12.3 — Build Tool Trust Gate (2026-11-11)

### Baseline and preserved state

- Inspected the existing worktree before edits. Prior Milestones 1–12.2 changes were present; no reset, stash, discard, commit, push, build, or workflow dispatch was performed. Baseline `git diff --check` was clean. The baseline tracked diff contained 68 modified files (1,684 insertions and 542 deletions); untracked files were omitted from that stat, and all remain preserved.
- Existing CI no longer had APK signing or publication steps. The source validation job, debug build/test job, manual inspection workflow, and optional news notification workflow were reviewed as they existed before this milestone.

### Wrapper integrity

- `gradle-wrapper.properties` selects Gradle 8.13 and pins the binary distribution ZIP SHA-256 `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`. This matches Gradle's official 8.13 binary-only ZIP checksum.
- Local checked-in `gradle-wrapper.jar` SHA-256 is `e996d452d2645e70c01c11143ca2d3742734a28da2bf61f25c82bdc288c9e637`. Gradle's official checksum reference lists `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f` specifically for the Gradle 8.13 Wrapper JAR. The mismatch remains.
- Retrieval was attempted from `https://services.gradle.org/distributions/gradle-8.13-wrapper.jar` (redirected to the official download host) and `https://raw.githubusercontent.com/gradle/gradle/v8.13/gradle/wrapper/gradle-wrapper.jar`; both returned HTTP 404. No downloaded JAR was verified; the repository copy was not changed. The JAR was not executed. The official checksum source is [Gradle release checksums](https://gradle.org/release-checksums/), which distinguishes Wrapper JAR and distribution ZIP entries.

### Workflow hardening

- Resolved each used Actions tag from its official GitHub repository via `git ls-remote`, using peeled commit ID for annotated `nttld/setup-ndk@v1`. Pinned all workflow `uses:` references to full commit SHAs and kept readable tag comments. Current mapping is recorded in `THIRD_PARTY_NOTICES.md`.
- Android content validation remains enabled. The only CI job containing Gradle execution is guarded by constant `if: false`, so it cannot execute the unverified wrapper through normal event triggers. The manual structure-inspection workflow remains non-building. The optional news workflow remains a separate FCM notification path; no APK publication/signing steps were added or restored.
- Workflow source shows `contents: read` permissions and no tag-triggered build/release. GitHub-side protected branch/tag rules, reviewers, and secret scopes were not inspected. No workflow was run.

### Verification performed and limitations

- Baseline and post-edit `git diff --check`; local wrapper SHA-256; official Gradle checksum page lookup; official action tag-ref lookups; workflow-source review; and, after edits, shell syntax and static workflow assertions are recorded in the final continuation report.
- A successful download/checksum comparison of the official wrapper JAR, archive listing of that official JAR, YAML parser/actionlint, Java/Android build, Gradle dependency resolution, unit tests, workflow execution, APK inspection, and device testing were not performed. `sh -n gradlew` (if successful) proves only shell syntax and does not establish wrapper JAR trust.
- Severity remains: **High** wrapper mismatch and unverified dependency state; **High** action source/host-side policy limitations despite immutable SHA pinning; prior **Critical** native/Minecraft/preloader provenance blockers remain unchanged. CI is not trusted to execute Gradle until the official wrapper artifact is independently verified and matched.
- Historical M12/M12.2 text above describing mutable action tags or enabled CI build/test jobs is retained as a record of the earlier checkpoint; this M12.3 section and the current workflow source supersede those statements.

## Milestone 12.3 continuation — verification record

- Current branch at start and end: `oblivion/open-source-integration`. Initial state already contained all earlier milestone work and 68 tracked changed files (1,684 insertions, 542 deletions in the starting diff stat); untracked documentation/source was not included in those counts. Initial `git diff --check` passed.
- Updated `.github/workflows/android.yml`, `.github/workflows/android-build.yml`, and `.github/workflows/publish-news.yml`: all external `uses:` references are pinned to 40-character commits resolved from the corresponding official repository's tags. The exact mapping and official repository links are in `THIRD_PARTY_NOTICES.md`. For annotated `nttld/setup-ndk@v1`, the peeled commit SHA was pinned.
- Added a constant-false condition to only the Android CI `build` job, keeping the independent release-content validation job available. The gated job contains the JDK/SDK setup plus `assembleDebug` and `:app:testDebugUnitTest`, so these cannot run from normal workflow events while wrapper provenance is unresolved. The structure inspection and optional main-branch FCM news workflows remain; neither publishes APKs. Workflow triggers, permissions, credentials scope, and absence of APK signing/publication were source-inspected; no workflow was dispatched.
- Official source: [Gradle release checksums](https://gradle.org/release-checksums/), version 8.13 entries. Expected Wrapper JAR SHA-256 is `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`; checked-in JAR remains `e996d452d2645e70c01c11143ca2d3742734a28da2bf61f25c82bdc288c9e637`. The official binary distribution checksum `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78` is still configured and matches the separate 8.13 `-bin` checksum. Retrieval from the official standalone distribution URL and official `v8.13` raw source path both returned HTTP 404. No artifact was substituted, and neither the existing nor downloaded wrapper JAR was executed. No official JAR archive inspection could be made because retrieval failed.
- `git diff --check` passed after edits. `sh -n gradlew` passed; this checks only shell syntax. `unzip -t gradle/wrapper/gradle-wrapper.jar` exited successfully without running the JAR. Static action-ref/release-gate assertions are recorded as passed after the final edit. No YAML parser/actionlint, build, dependency resolution, tests, workflow run, APK inspection, or device test was available/performed.
- Existing earlier status statements that the wrapper was non-executable refer to a historical checkpoint; the file mode was changed in M12, but its JAR is still untrusted. Current decision remains **not suitable for APK distribution**; CI is not trusted to run Gradle. Content-validation source remains enabled but its workflow has not itself been executed here.

## Milestone 12.3 continuation — verified Gradle 8.13 Wrapper JAR

- Checked `/tmp` before download: 4.0 GB free; the official 8.13 binary-only ZIP was downloaded outside the repository to `/tmp/gradle-8.13-bin.zip` (136,983,045 bytes).
- Verified the ZIP SHA-256 **before opening it**: calculated `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`, matching the configured and official Gradle 8.13 binary ZIP checksum.
- Archive inspection found `gradle-8.13/lib/plugins/gradle-wrapper-main-8.13.jar` (42,890 bytes) and `gradle-8.13/lib/gradle-wrapper-shared-8.13.jar` (31,950 bytes). The main JAR was extracted to `/tmp/gradle-wrapper-main-8.13.jar` (SHA-256 `78e3fe4f5eb0121f818ef2563eec8e6a7facf24203b7690fcdf9679befc5ae22`). Its archive contains an entry named `gradle-wrapper.jar` (43,705 bytes). Extracted to `/tmp/gradle-wrapper-embedded-8.13.jar`, this inner JAR has SHA-256 `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`, matching Gradle's official 8.13 Wrapper JAR checksum ([Gradle release checksums](https://gradle.org/release-checksums/)). The inner archive was listed without execution.
- Before replacing the repo copy, saved the old JAR to `/tmp/gradle-wrapper-before-m12.3.jar`; its hash is the previous `e996d452d2645e70c01c11143ca2d3742734a28da2bf61f25c82bdc288c9e637`. Replaced `gradle/wrapper/gradle-wrapper.jar` with the verified inner JAR and recalculated its hash: `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`.
- Re-resolved all six unique action tag refs directly from their official repositories. All nine workflow `uses:` occurrences remain pinned to full SHA commits; `nttld/setup-ndk` uses the peeled commit from its annotated `v1` tag. Provenance mapping remains in `THIRD_PARTY_NOTICES.md`.
- Removed the temporary constant-false condition from the Android CI build job. Content validation remains separate; Android CI can execute the now-verified wrapper on future eligible workflow events. No workflow was dispatched, and no Gradle task was run during this continuation.
- Confirmed `gradle-wrapper.properties` still contains the original Gradle 8.13 `-bin.zip` URL and SHA; neither was modified. Final non-executing verification: `git diff --check`, SHA-256 checks for the source ZIP, nested JAR, embedded JAR, old backup, and replacement; archive listing; static assertions for all nine SHA pins, wrapper configuration, and absence of APK signing/publication steps. No YAML parser/actionlint, build, tests, dependency resolution, APK inspection, or device test was performed.
- Current M12.3 decision: **the Gradle Wrapper and action-reference trust gate is satisfied for CI execution**. This does not establish a reproducible or release-ready Android build; mutable MinecraftAuth SNAPSHOT, unresolved third-party provenance, and other High/Critical findings remain. Prior M12.3 entries describing the wrapper mismatch and constant-false gate are historical and superseded by this continuation.

## Milestone 12.4 — First Controlled Build Validation

### Baseline and environment

- Branch: `oblivion/open-source-integration`. The worktree contains the accumulated uncommitted M1–M12.3 changes. Initial `git diff --check` passed; no changes were reset, stashed, discarded, or overwritten.
- Wrapper SHA-256 was rechecked as `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`. `gradle-wrapper.properties` still selects Gradle 8.13 and the verified binary distribution checksum.
- Environment check found no `java`, `javac`, or `sdkmanager`; `ANDROID_HOME` and `ANDROID_SDK_ROOT` are unset, and no SDK directories were found in the checked conventional locations. Therefore JDK 21, Android Platform 36, Build Tools 36.0.0, and NDK `28.2.13676358` are unavailable/unverified. `/tmp` and the project filesystem had 8.8 GB available. `local.properties` is absent; its contents were not read.
- Network-only HEAD/spider probes succeeded for Maven Central, Google Maven, JitPack, and OpenCollab snapshots. These probes do not establish dependency resolution or artifact integrity.

### Build and workflow results

- **No Gradle command was executed.** `./gradlew --no-daemon --version` was not attempted because Java is absent. Gradle tasks were not enumerated. `:app:assembleDebug` and `:app:testDebugUnitTest` were not run. No APK was generated; build duration and test count are not applicable.
- Workflow source review/static assertions confirmed nine full-SHA action references; retained release-content validation, Debug build, and unit-test steps; and no APK signing/publication or credential injection in the inspected workflows. No workflow was dispatched.
- `git diff --check` passed. Wrapper hash and workflow assertions passed. No compiler output, dependency versions resolved, manifest/resource merge results, native packaging result, or test failure log exists from this milestone because the build could not start.

### Decision and remaining work

- Build status: **NOT RUN — blocked by missing Java 21 and Android SDK/NDK**. This is an environment prerequisite failure, not a source compilation pass or failure.
- No files outside `IMPLEMENTATION_STATUS.md` and `docs/RELEASE_READINESS.md` were changed for this milestone. The verified Wrapper JAR and its properties were not modified.
- Next: use a controlled workstation or hosted CI runner with JDK 21, Android SDK Platform 36, Build Tools 36.0.0 and NDK 28.2.13676358; ensure recursive submodules initialize; run the documented Gradle version command first, then `:app:assembleDebug`, then `:app:testDebugUnitTest` only if the build succeeds. Capture the first actionable failure and actual resolved MinecraftAuth version without exposing secrets. Do not infer device behavior from a successful compile.
- Critical third-party/native/Minecraft/preloader provenance blockers remain. High risks include mutable MinecraftAuth SNAPSHOT, plaintext Microsoft/Xbox serialized state, extractable local Debug CurseForge key configurations, and untrusted catalog native code. No APK distribution recommendation changes.

### M12.4 transferred-workstation recheck

- Inspected the transferred worktree before any build action. Branch remains `oblivion/open-source-integration`; accumulated tracked modifications and untracked milestone source/document files are present. Initial `git diff --check` passed. Initial tracked diff stat: 70 files changed, 1,693 insertions, 551 deletions; untracked files are excluded from that stat.
- Toolchain is still unavailable: `java`, `javac`, and `sdkmanager` are missing; `ANDROID_HOME` and `ANDROID_SDK_ROOT` are unset; no configured SDK directory was found to verify Android Platform 36, Build Tools 36.0.0, or NDK `28.2.13676358`. Disk availability is 8.8 GB on project storage and `/tmp`. `local.properties` is absent and was not opened.
- Wrapper SHA-256 remains the verified `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`. No wrapper or build configuration changed.
- Stop condition applied: did not run the wrapper version command, task listing, Debug assembly, or unit tests. No APK was generated; build/test results remain **NOT RUN — missing JDK/Android SDK**. No workflow was triggered.
- Final `git diff --check` passed. No source, configuration, or workflow files changed. Only this status update and the release-readiness update record this recheck. Safest next step remains a controlled workstation/hosted runner with the specified JDK/SDK/NDK, followed by the ordered Gradle commands above.
