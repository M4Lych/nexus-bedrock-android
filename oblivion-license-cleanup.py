#!/usr/bin/env python3
"""Apply a conservative Oblivion cleanup that disables the unlicensed native preloader/mod path.

Run from the root of a clean clone of M4Lych/nexus-bedrock-android.
The script creates/switches to oblivion/license-runtime-cleanup, applies changes,
then stages them for review. It does not create a commit or push anything.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

BRANCH = "oblivion/license-runtime-cleanup"
BASE = "origin/oblivion/rebrand-foundation"


def run(*args: str, check: bool = True) -> str:
    proc = subprocess.run(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if check and proc.returncode != 0:
        raise RuntimeError(f"Command failed: {' '.join(args)}\n{proc.stderr.strip()}")
    return proc.stdout.strip()


def require_clean_tree() -> None:
    status = run("git", "status", "--porcelain")
    if status:
        raise RuntimeError(
            "Working tree is not clean. Save/commit/stash your existing work first; "
            "this script deliberately refuses to mix changes.\n" + status
        )


def replace_method(source: str, signature: str, replacement: str) -> str:
    """Replace one Java method body, matching braces while ignoring comments/strings."""
    pos = source.find(signature)
    if pos < 0:
        raise RuntimeError(f"Could not find expected method signature: {signature}")
    brace = source.find("{", pos)
    if brace < 0:
        raise RuntimeError(f"Could not find method body for: {signature}")

    depth = 0
    i = brace
    state = "code"
    quote = ""
    while i < len(source):
        ch = source[i]
        nxt = source[i + 1] if i + 1 < len(source) else ""
        if state == "code":
            if ch == "/" and nxt == "/":
                state = "line_comment"
                i += 2
                continue
            if ch == "/" and nxt == "*":
                state = "block_comment"
                i += 2
                continue
            if ch in ('"', "'"):
                quote = ch
                state = "string"
                i += 1
                continue
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    return source[:brace] + replacement + source[i + 1 :]
        elif state == "line_comment":
            if ch == "\n":
                state = "code"
        elif state == "block_comment":
            if ch == "*" and nxt == "/":
                state = "code"
                i += 2
                continue
        elif state == "string":
            if ch == "\\":
                i += 2
                continue
            if ch == quote:
                state = "code"
        i += 1
    raise RuntimeError(f"Unbalanced method body for: {signature}")


def read(path: str) -> str:
    p = Path(path)
    if not p.is_file():
        raise RuntimeError(f"Required file is missing: {path}")
    return p.read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    Path(path).write_text(content, encoding="utf-8", newline="\n")


def main() -> int:
    root = Path.cwd()
    if not (root / ".git").exists() or not (root / "settings.gradle").exists():
        raise RuntimeError("Run this script from the repository root.")

    require_clean_tree()
    run("git", "fetch", "origin", "--prune")
    if run("git", "show-ref", "--verify", f"refs/heads/{BRANCH}", check=False):
        run("git", "switch", BRANCH)
    else:
        run("git", "switch", "-c", BRANCH, BASE)

    # Confirm the target branch still starts from the expected audited base.
    base_sha = run("git", "rev-parse", BASE)
    head_sha = run("git", "rev-parse", "HEAD")
    if base_sha != head_sha:
        raise RuntimeError(
            f"Branch {BRANCH} is not at the audited base ({BASE}).\n"
            "No files were edited. Review the branch history before continuing."
        )

    # 1) Remove the unlicensed native preloader submodule reference/build path.
    gm = read(".gitmodules")
    gm_new, count = re.subn(
        r'(?ms)^\[submodule "app/src/main/cpp/preloader"\]\n(?:^[ \t].*\n)*\n?',
        "",
        gm,
    )
    if count != 1:
        raise RuntimeError("Expected exactly one preloader submodule block in .gitmodules.")
    write(".gitmodules", gm_new.rstrip() + "\n")

    cmake = read("app/src/main/cpp/CMakeLists.txt")
    if "add_subdirectory(preloader)" not in cmake:
        raise RuntimeError("Expected preloader CMake entry was not found.")
    write("app/src/main/cpp/CMakeLists.txt", cmake.replace("add_subdirectory(preloader)\n", ""))

    gradle = read("app/build.gradle")
    gradle_new = gradle.replace('                    "**/libpreloader.so",\n', "")
    gradle_new = gradle_new.replace('                    "**/libinbuiltmods.so"\n', '                    "**/libgxcore.so"\n')
    # If the gxcore entry was already present (as it is in the audited file), avoid duplication.
    gradle_new = gradle_new.replace(
        '                    "**/libgxcore.so",\n                    "**/libgxcore.so"\n',
        '                    "**/libgxcore.so"\n',
    )
    if gradle_new == gradle:
        raise RuntimeError("Expected preloader/inbuiltmods debug-symbol entries were not found.")
    write("app/build.gradle", gradle_new)

    # 2) Leave Java call sites buildable but make the preloader API inert.
    preloader_input = r'''package org.levimc.launcher.preloader;

import java.io.File;

/**
 * Compatibility no-op. Native game hooks are intentionally disabled in Oblivion.
 */
public final class PreloaderInput {
    private PreloaderInput() {}

    public static void prepareRuntime() {}
    public static void configureSignatureRules(File rulesFile, String minecraftVersion) {}
    public static boolean isPauseMenuOpen() { return false; }
    public static boolean isHudScreenOpen() { return false; }
    public static boolean isShowingMenu() { return false; }
    public static boolean shouldForceGlobalModMenu() { return false; }
    public static boolean onTouch(int action, int pointerId, float x, float y) { return false; }
    public static boolean onKeyEvent(int keyCode, int unicodeChar, boolean isKeyDown) { return false; }
    public static boolean onTextInput(CharSequence text) { return false; }
    public static void onDocumentResult(boolean success, String path, String displayName, String error) {}
    public static void cancelDocumentRequest(String reason) {}
    public static boolean onMouse(int button, boolean isDown) { return false; }
    public static boolean onMouseMotion(int action, int actionButton, int buttonState) { return false; }
    public static void resetMouseState() {}
    public static void setActivity(Object activity) {}
    public static void clearActivity() {}
}
'''
    write("app/src/main/java/org/levimc/launcher/preloader/PreloaderInput.java", preloader_input)

    rules_manager = r'''package org.levimc.launcher.preloader;

import android.content.Context;
import java.io.File;

/** Remote game-hook signature updates are disabled in this build. */
public final class PreloaderSignatureRulesManager {
    private PreloaderSignatureRulesManager() {}

    public interface RefreshCallback {
        void onFinished(RefreshResult result);
    }

    public static final class RefreshResult {
        public final boolean success;
        public final boolean updated;
        public final String message;
        public final long lastSuccessfulUpdateTime;

        public RefreshResult(boolean success, boolean updated, String message, long lastSuccessfulUpdateTime) {
            this.success = success;
            this.updated = updated;
            this.message = message == null ? "" : message;
            this.lastSuccessfulUpdateTime = lastSuccessfulUpdateTime;
        }
    }

    public static void refreshOnLauncherStart(Context context) {}
    public static void refreshNow(Context context, RefreshCallback callback) {
        if (callback != null) {
            callback.onFinished(new RefreshResult(false, false, "Native hooks are disabled.", 0L));
        }
    }
    public static boolean hasRemoteRulesUrl() { return false; }
    public static long getLastSuccessfulUpdateTime(Context context) { return 0L; }
    public static File getRulesFile(Context context) { return null; }
}
'''
    write("app/src/main/java/org/levimc/launcher/preloader/PreloaderSignatureRulesManager.java", rules_manager)

    # 3) Disable native preloader calls in ModManager.
    mm_path = "app/src/main/java/org/levimc/launcher/core/mods/ModManager.java"
    mm = read(mm_path)
    mm = mm.replace("    private static native boolean nativeLoadMod(String libPath, String modRootPath, Mod modObj);\n", "")
    mm = mm.replace("    private static native void nativeEnableLoadedMods();\n", "")
    mm = mm.replace("    private static native void nativeDisableAndUnloadLoadedMods();\n", "")
    mm = replace_method(mm, "public static boolean ensurePreloaderLoaded()", "{\n        return false;\n    }")
    mm = replace_method(mm, "public static boolean initializeLoadedMod(String libPath, String modRootPath, Mod mod)", "{\n        return false;\n    }")
    mm = replace_method(mm, "public static void enableLoadedMods()", "{\n        // Native mods are disabled in this build.\n    }")
    mm = replace_method(mm, "public static void disableAndUnloadLoadedMods()", "{\n        // Native mods are disabled in this build.\n    }")
    write(mm_path, mm)

    # 4) Ensure arbitrary native mod packages are never loaded from instance folders.
    loader_path = "app/src/main/java/org/levimc/launcher/core/mods/ModNativeLoader.java"
    loader = read(loader_path)
    loader = replace_method(
        loader,
        "public static void loadEnabledSoMods(ModManager modManager, File cacheDir, LoadListener listener)",
        "{\n        if (listener != null) {\n            listener.onScanStarted(0);\n            listener.onMessage(\"Native mods are disabled in this Oblivion build.\");\n        }\n    }",
    )
    write(loader_path, loader)

    # 5) Disable built-in JNI mods and remove the bundled inbuilt-mod binary.
    inbuilt_native = r'''package org.levimc.launcher.core.mods.inbuilt.nativemod;

/** Native in-game modifications are disabled in this Oblivion build. */
public final class InbuiltModsNative {
    private InbuiltModsNative() {}
    public static synchronized boolean loadLibrary() { return false; }
    public static boolean isLoaded() { return false; }
}
'''
    write("app/src/main/java/org/levimc/launcher/core/mods/inbuilt/nativemod/InbuiltModsNative.java", inbuilt_native)

    # 6) Add explicit release blockers for other binaries/module whose provenance is not yet known.
    blockers = r'''# Oblivion — license review blockers

This file tracks items intentionally **not** declared license-cleared by the native cleanup.
Do not publish a release build containing an item below until its origin, license, and
redistribution rights are verified or the item is replaced/removed.

## Disabled/removed by `oblivion/license-runtime-cleanup`

- `LiteLDev/preloader-android` submodule: the pinned repository has no declared license,
  and its code implements native game hooks/patching. Its build integration is removed.
- Native preloader Java bridge and remote signature-rule update path: replaced with no-op APIs.
- Arbitrary native `.so` mod loading and inbuilt JNI mods: disabled. `libinbuiltmods.so`
  and the preloader signature-rules asset are removed.

## Still requires provenance/license confirmation

- `app/src/main/jniLibs/arm64-v8a/libgxcore.so`
- `app/src/main/jniLibs/arm64-v8a/libPlayFabMultiplayer.so`
- `app/src/main/jniLibs/arm64-v8a/libmaesdk.so`
- The `minecraft/` Android library module, especially sources under `com.mojang`,
  `com.microsoft`, `com.xbox`, and `org.fmod`. Its presence in the fork does not by itself
  establish a redistribution license. It remains in place because the current in-process
  launch path directly depends on it; do not treat it as cleared for public distribution.

## Known third-party obligations to preserve

- Root LeviLauncher-derived source: Apache-2.0; retain upstream notices and mark modified files.
- `pojav_controls/`: LGPL-3.0-or-later with its `LICENSE` and `NOTICE`; preserve the library's
  source and the user's practical ability to replace/relink it when distributing binaries.
- MPL-2.0 NBT/BinaryStream files and BSD/MIT native dependencies: keep the relevant licenses,
  notices, and corresponding source obligations with the distributed source/binary package.

## Validation still required

1. Run `./gradlew :app:assembleDebug` on the project's supported Android build environment.
2. Verify the app launches without the preloader and that Add-On/world management still works.
3. Verify no code path attempts to load arbitrary native `.so` mods.
4. Resolve the remaining blockers above before a public release.
'''
    write("LICENSE_REVIEW_BLOCKERS.md", blockers)

    # Remove the Git submodule entry, unsupported native mod binary, and remote hook rules.
    run("git", "rm", "-f", "app/src/main/cpp/preloader")
    run("git", "rm", "-f", "app/src/main/jniLibs/arm64-v8a/libinbuiltmods.so")
    run("git", "rm", "-f", "resources/preloader/preloader_signature_rules_source.json")

    # Stage all expected changes for inspection, but never auto-commit or push.
    run("git", "add", "-A")
    run("git", "diff", "--cached", "--check")

    print("Cleanup applied and staged on branch:", BRANCH)
    print("No commit or push was made.")
    print("Review with: git diff --cached --stat && git diff --cached")
    print("Then build with: ./gradlew :app:assembleDebug")
    print("Commit only after reviewing the diff and a successful build.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)
