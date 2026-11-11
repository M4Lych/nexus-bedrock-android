# Third-party notices and provenance inventory

Audit status: source-level inventory for the current checkout, not a legal opinion or release clearance. The app has not been resolved/built in this environment, so the exact transitive dependency graph and artifact-embedded notices are unavailable. Any item marked **unverified** must be checked against the exact redistributed source/artifact before publishing an APK. No Minecraft APK or game assets are authorized or included by this inventory.

## Repository and source components

| Component | Version / source | License observed | Notice / obligation | Redistribution status |
| --- | --- | --- | --- | --- |
| Oblivion application sources | This repository | Apache-2.0, root `LICENSE` | Preserve Apache license and notices; identify modifications to upstream-derived code as required. | Root license verified; provenance of inherited files still needs review. |
| LeviLauncher Android upstream | `https://github.com/LiteLDev/LeviLauncher-Android` (upstream base; exact source revision not recorded in this worktree) | Upstream license must be checked against the pinned source revision and preserved notices. | Preserve upstream attribution and modifications. | **Not fully verified** for all inherited files. |
| Pojav Controls | `pojav_controls/`, notice points to PojavLauncher | LGPL-3.0-or-later (`pojav_controls/LICENSE`, `NOTICE`) | Provide LGPL license, notices, complete corresponding library source/modifications, and relinking/replacement rights. | License and local notice present; packaged compliance/relinkability not build-tested. |
| `worldtools` vendored NBT | `app/src/main/cpp/worldtools/vendor/nbt`; GlacieTeam NBT | MPL-2.0 (`LICENSE`) | Preserve notices and provide source for MPL-covered modifications under MPL terms. Source archive is included at `app/src/main/assets/worldtools/mpl-source.zip`. | License observed; source archive contents not extracted/compared in this audit. |
| `worldtools` BinaryStream | `app/src/main/cpp/worldtools/vendor/binarystream`; notice says 2.3.2 | MPL-2.0 (`LICENSE`) | Preserve notice and source availability for covered modifications. | License observed; exact archive correspondence not verified. |
| `worldtools` LevelDB | `app/src/main/cpp/worldtools/vendor/leveldb`; Falcon-MC fork | BSD-3-Clause (`LICENSE`, AUTHORS) | Retain copyright, conditions and disclaimer in source/binary notices. | License observed; fork revision and binary relationship not verified. |
| Snappy | `app/src/main/cpp/worldtools/vendor/snappy`; notice says 1.2.2 | BSD-3-Clause (`COPYING`) | Retain required copyright, conditions and disclaimer. | License observed; packaged binary relationship not verified. |
| Zstandard | `app/src/main/cpp/worldtools/vendor/zstd`; notice says 1.5.7 | BSD-3-Clause (`LICENSE`, `COPYING`) | Retain required copyright, conditions and disclaimer. | License observed; packaged binary relationship not verified. |
| `libHttpClient` Git submodule | `.gitmodules` source `https://github.com/microsoft/libHttpClient`; `app/src/main/cpp/libHttpClient`, pinned gitlink `f8ae1d9cf931d9d55542865eee395adca5bdbe41` | Microsoft upstream has MIT license/notices; exact pinned revision was not checked out. | Added by `app/src/main/cpp/CMakeLists.txt` and native build configuration; include notices for the pinned and transitive revisions. | **Pinned contents and APK binary relationship unverified**; submodule uninitialized. |
| `preloader-android` Git submodule | `.gitmodules` source `https://github.com/LiteLDev/preloader-android`; `app/src/main/cpp/preloader`, pinned gitlink `cc08b40a31471d3e2dedb267f5f17b28da2b0604` | No license located in this checkout; submodule not initialized. | `app/src/main/cpp/CMakeLists.txt` adds the submodule; the native `preloader` library is loaded through `ModManager.ensurePreloaderLoaded()`. Determine license and obligations from the pinned revision. | **License and redistribution unresolved; release blocker.** |
| Prebuilt native `libPlayFabMultiplayer.so` | `app/src/main/jniLibs/arm64-v8a/libPlayFabMultiplayer.so`; first introduced in repository history at `b5e3d134eea0a0fd6f9a10fd00d6546169c9196d`; 3,839,688 bytes; SHA-256 `35be8a69fce98c9341db8b400dcdaec88a5979caf30f94c7149850078e8ebad6` | Unknown; Git history establishes repository presence only, not upstream origin. | AGP packages this `jniLibs` ABI directory into the app APK; `GamePackageManager.systemLoadedLibs` identifies it as a library loaded by the game runtime. | **Origin, license and redistribution rights unresolved; release blocker.** |
| Prebuilt native `libgxcore.so` | `app/src/main/jniLibs/arm64-v8a/libgxcore.so`; first introduced at `a5afe0eec39ba33975c2832a154aec68d3111fde`; 3,352,048 bytes; SHA-256 `e1d44eddd4d6616a59b92147b6d1eec9db6d95386ef8f9c2d4c2c9629783abfc` | Unknown; exact source/build revision not evidenced by local files. | Packaged via `jniLibs`; loaded by `NativeBridgeHelper` and `GamePackageManager`. | **Origin, license and redistribution rights unresolved; release blocker.** |
| Prebuilt native `libinbuiltmods.so` | `app/src/main/jniLibs/arm64-v8a/libinbuiltmods.so`; first introduced at `2e52effc492086dc12abf87c24bbdda788306ead`; 69,800 bytes; SHA-256 `832cf21fcf5355f650dd0ed0f1df3438b92deca7c979aa136965a224f8efbf58` | Unknown; Git history establishes repository presence only. | Packaged via `jniLibs`; `InbuiltModsNative` loads it for bundled native mod features. | **Origin, license and redistribution rights unresolved; release blocker.** |
| Prebuilt native `libmaesdk.so` | `app/src/main/jniLibs/arm64-v8a/libmaesdk.so`; first introduced at `068493d258a78346b4573a798c014b88f0369296`; 6,117,776 bytes; SHA-256 `27a82fe4adb58f0710a14974856ba1d18cb9d101b9d7a92b6d0e9b46d8a2bc6a` | Unknown; Git history establishes repository presence only. | Packaged via `jniLibs`; `GamePackageManager.systemLoadedLibs` identifies it as a game-runtime library. | **Origin, license and redistribution rights unresolved; release blocker.** |
| Minecraft/Xbox compatibility module | `minecraft/` module source/resources; app includes it via `implementation project(':minecraft')`; source namespaces include `com.mojang`, `com.microsoft`, `com.xbox`, FMOD. Repository lineage is LeviLauncher Android (`https://github.com/LiteLDev/LeviLauncher-Android`); current source snapshot is repository commit `d9acd2ab402d28b26996f3c0da185d708b5f2ba8`. | No complete per-file license/provenance inventory; repo lineage and Git history do not prove original source or redistribution rights. | Android Gradle project dependency merges module classes/resources into the app variant. | **Exact upstream source/SDK revisions and redistribution evidence unresolved; release blocker.** |
| MiSans font | `app/src/main/res/font/misans.ttf`; SHA-256 `b8b1bdea22256904d05d576aa2e53d8092e5f92ea88ed6d68be6d827a82b402d` | No license file or source revision found. | Android resource is eligible for packaging into the APK. | **License and redistribution rights unresolved.** |
| `worldtools` MPL source archive | `app/src/main/assets/worldtools/mpl-source.zip`; SHA-256 `c22d79af699e89671ed01ce0b9eb698fbb1ff643d07f5cd341c142cf7654d355` | Intended to provide source for MPL components; archive contents/revision were not extracted or matched. | App assets source set packages `app/src/main/assets` into APK. | **Contents and corresponding-source completeness unverified.** |
| Gradle wrapper JAR | `gradle/wrapper/gradle-wrapper.jar`, 43,705 bytes; SHA-256 `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`. Extracted from `gradle-8.13/lib/plugins/gradle-wrapper-main-8.13.jar` inside the checksum-verified Gradle 8.13 binary ZIP. | Gradle official [release checksum reference](https://gradle.org/release-checksums/) lists the same 8.13 Wrapper JAR SHA-256. | Build-time wrapper artifact, not an APK library. | **Checksum verified against official release reference; repository copy replaced and rechecked in M12.3.** |
| Inherited raster UI assets | `app/src/main/res/drawable-*`, `app/src/main/res/mipmap-*`, `minecraft/src/main/res/drawable-*`; individual exact files are tracked in Git. | Individual origins/licenses are not fully annotated; upstream inheritance is not permission evidence. | Android resource merge packages app and `:minecraft` module resources into variants. | **Per-file provenance and redistribution rights unresolved; review required.** |
| LeviMC featured-tag sprites | MIT notice `app/src/main/assets/licenses/NOTICE-levimc-kit.txt`; source `https://github.com/LiteLDev/levimc.org` (`kit/assets/sprites.mjs`); generated resources `app/src/main/res/drawable/ic_tag_*.xml` | MIT, copyright LeviMC 2026, as recorded in the local notice. | VectorDrawable resources merge into the app; preserve the MIT notice. Exact upstream asset revision is not pinned. | **License evidence present; source revision not verified.** |
| `levimc-kit` sprites | Notice at `app/src/main/assets/licenses/NOTICE-levimc-kit.txt`; source points to `LiteLDev/levimc.org` | MIT, per bundled notice | Include MIT copyright and permission notice. | Notice present; exact asset revision not pinned. |
| Gradle Wrapper | `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, Gradle 8.13 | Gradle wrapper scripts/JAR are Apache-2.0 upstream components. | Preserve the Apache notice in redistributed source. | Current JAR SHA-256 `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`, matching Gradle’s published 8.13 Wrapper JAR checksum ([official checksum page](https://gradle.org/release-checksums/)). The distinct binary distribution ZIP checksum remains pinned in `gradle-wrapper.properties`. |

## Declared Maven dependencies and build plugins

Versions below are read from `gradle/libs.versions.toml` and module build scripts. **License status for every row is unverified against the exact resolved artifact** because Gradle could not resolve dependencies and no dependency report/verification metadata is present. The links identify upstream or artifact sources to review; they do not establish that the checked-in binary was built from that source. Resolve licenses and transitive dependencies before distribution.

| Component / coordinate | Declared version | License and distribution status |
| --- | --- | --- |
| Android Gradle Plugin `com.android.tools.build:gradle` | 8.13.2 | Unverified against exact plugin artifact; build-only component. |
| Kotlin Gradle plugin `org.jetbrains.kotlin.android` | 2.3.0 | Unverified against exact plugin artifact; build-only component. |
| Google Services plugin | 4.4.4 | Unverified against exact plugin artifact; build-only component. |
| Firebase Crashlytics Gradle plugin | 3.0.7 | Unverified against exact plugin artifact; build-only component. |
| AndroidX AppCompat | 1.7.1 | Unverified exact artifact/license; AndroidX source/artifact notices required. |
| AndroidX Annotation | 1.9.1 | Unverified exact artifact/license. |
| AndroidX Activity KTX | 1.12.2 | Unverified exact artifact/license. |
| AndroidX Browser | 1.9.0 | Unverified exact artifact/license. |
| AndroidX Fragment KTX | 1.8.9 | Unverified exact artifact/license. |
| AndroidX ConstraintLayout | 2.2.1 | Unverified exact artifact/license. |
| AndroidX Core SplashScreen | 1.2.0 | Unverified exact artifact/license. |
| AndroidX Preference KTX | 1.2.1 | Unverified exact artifact/license. |
| AndroidX Lifecycle artifacts | 2.10.0; lifecycle-extensions 2.2.0 | Unverified exact artifacts/license; legacy `lifecycle-extensions` remains declared. |
| AndroidX Room runtime/KTX/compiler | 2.8.4 | Unverified exact artifacts/license. |
| AndroidX SQLite framework | 2.6.2 | Unverified exact artifact/license. |
| AndroidX Games Activity | 4.4.2 | Unverified exact artifact/license. |
| Material Components | 1.13.0 | Unverified exact artifact/license. |
| Gson | 2.13.2 | Unverified exact artifact/license. |
| OkHttp | 5.3.2 | Unverified exact artifact/license. |
| Glide | 5.0.5 | Unverified exact artifact/license. |
| Google Guava | 33.5.0-jre | Unverified exact artifact/license. |
| Apache HttpClient | 4.5.14 | Unverified exact artifact/license. |
| Lenni0451 commons-httpclient / commons-gson | 1.9.2 | Unverified exact artifact/license. |
| Conscrypt Android | 2.5.3 | Unverified exact artifact/license. |
| SpongyCastle core / pkix / prov | 1.58.0.0 / 1.54.0.0 | Unverified exact artifacts/license. |
| Simple XML | 2.7.1 | Unverified exact artifact/license; module excludes some parser transitive dependencies. |
| Cloudburst Bedrock connection `org.cloudburstmc.protocol:bedrock-connection` | Previously declared `3.0.0.Beta10-SNAPSHOT` in the version catalog; repository is `https://repo.opencollab.dev/maven-snapshots`; the alias had no Gradle dependency use and was removed from the catalog in M12.2. | Not part of the declared resolved graph based on source use scan; no artifact resolution was performed. No migration needed for current Gradle call sites. |
| MinecraftAuth `com.github.RaphiMC:MinecraftAuth` | `main-SNAPSHOT`; JitPack (`https://jitpack.io`, configured in `settings.gradle`); app direct dependency. Used by `MsftAuthManager` for Microsoft/Xbox/Minecraft authentication flow and HTTP client setup. | Mutable branch snapshot; exact resolved commit/artifact/license unknown without dependency resolution. **High release reproducibility/security blocker**; retain until stable compatible version can be tested. |
| Firebase BOM / Crashlytics / Crashlytics NDK / Messaging / IID | BOM 34.14.0; some artifacts BOM-managed, Messaging 25.0.1 and IID 21.1.0 also declared | Exact resolved artifacts/licenses unverified; Firebase configuration and provider behavior require project-specific review. |
| Play Services Games v2 | 21.0.0 | Unverified exact artifact/license and service requirements. |
| Billing Client | 8.3.0 | Declared in `:minecraft`; verify whether needed and review its service/data behavior before release. No payment implementation was added in this milestone. |
| CommonMark core/extensions | 0.24.0 | Unverified exact artifacts/license. |
| xCrash | 3.1.2 (`com.github.RadiantByte:xCrash`) | Fork coordinate; exact fork/source license and binary provenance unverified. |
| JUnit 4 / AndroidX Test JUnit / Espresso | 4.13.2 / 1.3.0 / 3.7.0 | Test-only artifacts; exact artifact licenses unverified. |
| JetBrains annotations | 26.0.2-1 | Unverified exact artifact/license. |

No other dynamic `+`/`latest.*` version was found in the reviewed root/app/minecraft/version-catalog declarations. There is no Gradle dependency lock state, `gradle/verification-metadata.xml`, resolved SBOM, or artifact-level license report in the current worktree. The dependency list above is not a complete transitive SBOM. Add reproducible dependency locking/checksums and generate an artifact-based SBOM after a verified CI build; manually review the remaining MinecraftAuth SNAPSHOT.

## Build workflow supply-chain references

These are CI-time actions, not APK runtime dependencies. The table below records the mutable tags present at the initial M12.3 inspection and is retained as historical context. Current immutable SHA pins and their official repository/tag mapping are recorded in the M12.3 checkpoint and continuation sections below.

| Workflow | Action reference | Effect / current status |
| --- | --- | --- |
| `.github/workflows/android.yml` | `actions/checkout@v4`; `actions/setup-java@v4`; `android-actions/setup-android@v3`; `xmake-io/github-action-setup-xmake@v1`; `nttld/setup-ndk@v1` | CI validation/build toolchain setup. Workflow now has read-only repository permissions, no credential access, no tag trigger, no APK artifact upload, and no signing or release step. Action tags remain mutable. |
| `.github/workflows/android-build.yml` | `actions/checkout@v4` | Manual project inspection only; no build or publishing. No explicit permissions block was present at audit time; set workflow/job permissions to `contents: read`. |
| `.github/workflows/publish-news.yml` | `actions/checkout@v4`; `google-github-actions/auth@v2` | Main-branch content validation and optional Firebase Cloud Messaging news notification; not APK publishing. Workflow declares `contents: read`; service-account secret is scoped to notification steps. Action tags remain mutable. |

Workflow files do not contain signing credentials. Android CI no longer reads Google Services, signing, or webhook secrets. GitHub repository/organization settings, protected branches, environment reviewers, and secret scopes cannot be verified from this checkout.

## External native catalog content

The community mod catalog accepts `.so`, `.mcmod`, `.zip`, and `.levipack` assets over HTTPS; hash metadata may be omitted. The existing importer routes these into the native-mod manager. That manager loads enabled libraries with `System.load`; historically newly discovered mods defaulted enabled. M12.2 changes only the catalog path so newly imported catalog mod IDs are disabled before its success callback. Existing local SAF imports, explicit user enablement, mod SDK behavior, and overwrite state remain. This does not authenticate the source, verify publisher identity, sandbox native code, or establish redistribution permission. Catalog native code remains a High-risk feature pending trust and rights review.

## Required release actions

1. Initialize and audit both Git submodules at their pinned gitlink revisions; record licenses and transitive notices. `libHttpClient` points to Microsoft’s repository at `f8ae1d9cf931d9d55542865eee395adca5bdbe41`; `preloader` points to `LiteLDev/preloader-android` at `cc08b40a31471d3e2dedb267f5f17b28da2b0604`. The former repository contains MIT notices upstream, but the pinned contents were not fetched; the latter license remains unverified.
2. Identify the origins/licenses of all four prebuilt `.so` files, the Minecraft compatibility module, MiSans and inherited raster assets; obtain redistribution permission where needed or omit only after a compatibility review.
3. Resolve the exact Gradle dependency graph, verify licenses and notices, and review transitive dependencies. Keep mutable SNAPSHOT dependencies out of release builds unless pinned to immutable revisions and regression-tested.
4. Verify the Gradle Wrapper JAR against an independently obtained official artifact/checksum before using it; do not infer trust from its filename or manifest.
5. Include these notices and corresponding-source/relinking information in each distribution variant, including LGPL/MPL obligations.

The app is not an official Mojang or Microsoft product. Minecraft is a trademark of Mojang/Microsoft; no affiliation or endorsement is claimed.

## Milestone 12.3 current build-tool trust status — 2026-11-11

This checkpoint supersedes the historical statement above that all workflow actions still used mutable tags. It does not erase the historical state at the earlier audit date. The wrapper verification status below is superseded by the later continuation section at the end of this document.

- Gradle is configured as 8.13. Local wrapper JAR SHA-256: `e996d452d2645e70c01c11143ca2d3742734a28da2bf61f25c82bdc288c9e637`. Gradle's official [release checksum page](https://gradle.org/release-checksums/) identifies `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f` as the **8.13 Wrapper JAR Checksum** (not the distribution ZIP checksum). These values do not match.
- The pinned distribution checksum in `gradle-wrapper.properties`, `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`, matches the official 8.13 binary-only ZIP checksum on the same page.
- Retrieval attempts used the official Gradle release URL (`https://services.gradle.org/distributions/gradle-8.13-wrapper.jar`, redirected to `downloads.gradle.org`) and official Gradle source raw URL (`https://raw.githubusercontent.com/gradle/gradle/v8.13/gradle/wrapper/gradle-wrapper.jar`). The release URL returned HTTP 404; the tagged source path returned HTTP 404. No JAR was downloaded or substituted. The checksum page is authoritative expected-hash evidence, but no independent artifact comparison completed.
- Official action tag refs were resolved directly with `git ls-remote` against each named official repository. The workflow now pins: checkout `11d5960a326750d5838078e36cf38b85af677262` (`v4`); setup-java `cf277c60eb25467037889841efdb72551f06f6c3` (`v4`); setup-android `9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407` (`v3`); setup-xmake `3a1a5dddfc7fa625d9a698738334bf55655a861a` (`v1`); setup-ndk `ed92fe6cadad69be94a966a7ee3271275e62f779` (peeled `v1` tag); google auth `c200f3691d83b41bf9bbd8638997a462592937ed` (`v2`). No SHA was taken from an aggregator or third-party list.
- The Android CI content-validation job remains available. Its Gradle toolchain/build/unit-test job is skipped with a constant false condition until the wrapper artifact is independently verified. The manual workflow only inspects files. The news workflow can send an FCM notification under its separate conditional secret, but no workflow publishes or signs APKs.
- Action commit pins were confirmed as tag refs at this checkpoint; action source code and workflows were not executed. Branch protection, tag protection, and GitHub environment reviewers remain host-side settings not verified from this checkout.

## Milestone 12.3 continuation — Wrapper artifact verified

- `/tmp` had 4.0 GB free before download. The official `gradle-8.13-bin.zip` was downloaded outside the repository to `/tmp/gradle-8.13-bin.zip` (136,983,045 bytes). SHA-256 `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78` matched the official Gradle 8.13 binary ZIP checksum before archive contents were listed.
- The ZIP contains `gradle-8.13/lib/plugins/gradle-wrapper-main-8.13.jar` (42,890 bytes) and `gradle-8.13/lib/gradle-wrapper-shared-8.13.jar` (31,950 bytes). The main JAR contains an embedded `gradle-wrapper.jar` entry (43,705 bytes). Extracted inner JAR SHA-256 `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f` matches Gradle's official 8.13 Wrapper JAR checksum ([official reference](https://gradle.org/release-checksums/)). The nested and inner JAR archive entries were inspected without execution.
- Original repository JAR was saved at `/tmp/gradle-wrapper-before-m12.3.jar`, SHA-256 `e996d452d2645e70c01c11143ca2d3742734a28da2bf61f25c82bdc288c9e637`. It was replaced by the verified embedded JAR; the repository copy was rehashed to the official `81a82...ae45f` digest. The official distribution URL and checksum in `gradle-wrapper.properties` remain unchanged.
- All nine action references are full commit SHAs and were rechecked against the official repositories' tag refs. The Android CI build/test job is no longer gated; future eligible workflow events can execute the verified wrapper. No workflow was triggered, no Gradle task was run, and no APK signing or publication was introduced.
- This continuation supersedes the wrapper-mismatch and disabled-build status in the preceding section. Broader dependency reproducibility, unresolved third-party rights/provenance, and host-side branch/reviewer settings remain open.
