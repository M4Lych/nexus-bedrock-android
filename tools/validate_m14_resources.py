#!/usr/bin/env python3
"""Static checks for M14 launcher identity and localized resource XML."""

from pathlib import Path
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
ALLOWED_LEVILAUNCHER_KEYS = {
    # These values identify original contributors or preserve an existing on-disk path.
    "about_author_role",
    "instance_backup_desc",
    "backup_select_downloads",
}


def resource_name(node):
    return node.attrib.get("name", "")


def main():
    strings_files = sorted(RES.glob("values*/strings*.xml"))
    if not strings_files:
        raise SystemExit("No localized string resources found")

    for path in RES.rglob("*.xml"):
        try:
            ET.parse(path)
        except ET.ParseError as error:
            raise SystemExit(f"Malformed XML: {path.relative_to(ROOT)}: {error}") from error

    default_strings = ET.parse(RES / "values" / "strings.xml").getroot()
    values = {resource_name(node): "".join(node.itertext()).strip() for node in default_strings}
    if values.get("app_name") != "Oblivion":
        raise SystemExit("Default app_name must be Oblivion")
    if values.get("about_github_repo") != "GitHub · Oblivion":
        raise SystemExit("About screen must identify the Oblivion repository")

    failures = []
    for path in strings_files:
        root = ET.parse(path).getroot()
        for node in root:
            name = resource_name(node)
            value = "".join(node.itertext())
            if "levilauncher" in value.lower() and name not in ALLOWED_LEVILAUNCHER_KEYS:
                failures.append(f"{path.relative_to(ROOT)}:{name}")
    if failures:
        raise SystemExit("Unexpected LeviLauncher product branding:\n" + "\n".join(failures))

    language_manager = (ROOT / "app/src/main/java/org/levimc/launcher/util/LanguageManager.java").read_text()
    splash = (ROOT / "app/src/main/java/org/levimc/launcher/ui/activities/SplashActivity.java").read_text()
    if 'return "en";' not in language_manager:
        raise SystemExit("Fresh installs must resolve to English")
    if "syncSystemLocale" in splash:
        raise SystemExit("Splash must not replace the saved app language with the system locale")

    bridge = (ROOT / "app/src/main/java/org/levimc/launcher/util/NativeBridgeHelper.kt").read_text()
    guard = (ROOT / "app/src/main/java/org/levimc/launcher/util/NativeImageGuard.java").read_text()
    game_manager = (ROOT / "app/src/main/java/org/levimc/launcher/core/minecraft/GamePackageManager.kt").read_text()
    if "recordGxCoreFailure" not in bridge or "getGxCoreLoadFailure" not in guard:
        raise SystemExit("Native bridge failure diagnostics are not propagated")
    if "NativeBridgeHelper.getGxCoreLoadFailure()" not in game_manager:
        raise SystemExit("GamePackageManager must include gxcore load details in launch diagnostics")

    news = (ROOT / "resources/launcher/news.json").read_text()
    if '"items": []' not in news:
        raise SystemExit("Bundled Oblivion news must not contain unverified announcements")

    print(f"Validated {len(strings_files)} localized string files and all resource XML files.")
    print("Oblivion identity, English default, and empty bundled news feed checks passed.")


if __name__ == "__main__":
    main()
