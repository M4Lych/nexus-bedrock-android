# M14 identity and account-data compatibility

## Current compatibility boundary

M14 changes visible product copy, the About links, the Microsoft WebView/User-Agent label, and the news-feed source. It does not change `applicationId`, Java namespace, profile IDs, Minecraft package identifiers, account serializers, XAL export paths, account preference names, backup file format, or the existing `Downloads/LeviLauncher/Backups` directory. The latter remains a legacy on-disk location and is named explicitly in backup instructions so users can still find earlier backups.

The existing Microsoft/Xbox flow remains the game-account flow. `MsftAccountStore` continues to serialize its current account format to the existing `Xal.Accounts.json` file, and `XalExporter` continues to maintain its existing XAL files and preference namespaces. M14 changes no token handling, login protocol, account identifiers, or credential storage. Existing accounts should remain readable, but this source-only review cannot establish runtime compatibility.

The app-language preference remains `settings` / `language`. When the key is absent or empty, M14 stores and applies `en`; an explicit saved tag remains in use, including the legacy `in` and `idn` normalization to Indonesian `id`. No system locale is copied over the app preference.

The prior cached news file is not deleted. It is ignored unless the existing news preference namespace has the new `feed_source=oblivion` marker; after a successful refresh from the Oblivion feed, new data is cached with that marker. This prevents an inherited cached LeviLauncher feed from being presented as Oblivion news while retaining the local file non-destructively.

## Separate future identity migration

Do not rename package or storage identifiers as part of visual rebranding. A future internal identity or encrypted-account-storage migration requires a separate reviewed change with:

1. An inventory of all app-private account files, XAL exports, backup paths, preference keys, and profile/content IDs on supported versions.
2. Versioned read-old/write-new migration with atomic replacement, integrity checks, and preservation of old data until verification succeeds.
3. Keystore key lifecycle design and an explicit recovery story for key invalidation, restore-to-new-device, and interrupted migration.
4. Upgrade tests starting with real legacy-format fixtures, plus rollback and account-removal tests. Fixtures must contain synthetic accounts only.
5. Confirmation that Microsoft/Xbox and Minecraft authentication continue to follow their supported flows and that no account tokens enter logs, cloud backup, or product analytics.

Until those steps are completed, retain the current package identity and account/file identifiers. Never migrate by clearing preferences, deleting XAL data, or silently changing the backup directory.
