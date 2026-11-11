# Oblivion account, cloud backup, and sync architecture

Status: design review only. No Oblivion account provider, cloud backend, sync client, payment flow, or premium entitlement service is implemented. All recommendations below must be reviewed before service deployment or collection of user data.

## Repository findings

### Existing accounts are Minecraft accounts

- The existing Microsoft login lives in `MsftLoginActivity` and `MsftAuthManager`, and uses the inherited MinecraftAuth Bedrock flow. It performs Microsoft authorization-code/device-code login, then Xbox/Minecraft token and certificate refresh through `BedrockAuthManager`.
- `MsftAccountStore` persists account identity/display fields and `BedrockAuthManager.toJson()` in `filesDir/xal/Xal.Accounts.json`, using `AtomicFile`. That serialized auth state can contain Microsoft access/refresh credentials, Xbox/XSTS credentials, Minecraft certificate/session data, and device identity/key material. It must never enter an Oblivion account or sync API.
- `XalExporter` writes Minecraft-compatible credential files below `filesDir/xal/<encoded-user-id>/` and stores device key data in the `org.levimc.xal.crypto` SharedPreferences file. Account removal deletes the matching exported user directory and re-exports the remaining active account; removing the final account clears the XAL directory and device-key preferences. Deletion is best effort and is not secure media erasure.
- `AccountsActivity` displays and manages these Microsoft/Xbox accounts. Its visibility depends on the existing launcher-managed Minecraft login setting. It is not an Oblivion account screen. Local launcher, profiles, worlds, settings, and packs do not require this account unless the user enables the existing official Minecraft login flow.
- `FeatureSettings`/`SettingsStorage` persist local settings in SharedPreferences `feature_settings`, key `settings_json`. Profile selection uses `version_manager` preferences; profile metadata uses `profile.json` and legacy files. `LauncherStorage` selects its app root from `getExternalMediaDirs()` when available, then falls back to `filesDir`; profile content is under existing per-profile/shared `games/com.mojang` locations. Content-count cache keys are in `content_counts`. Preserve these paths, identifiers, and keys.
- World and instance backup already have local/export workflows (`WorldManager`, `InstanceBackupManager`) using SAF-selected destinations. Those user-directed archives are not a cloud sync service. World data can be large and version-sensitive; restoring it must be explicit.

### Existing Android backup rules

The manifest has `allowBackup="true"`, `fullBackupContent`, and Android 12+ `dataExtractionRules`. Current rules exclude `file:xal/` and `sharedpref:org.levimc.xal.crypto.xml` from both cloud backup and device transfer while retaining the platform's existing default handling for other app-private data. This protects the reviewed Microsoft/Xbox credential locations. It does not imply that Oblivion cloud backup is enabled or consented to.

Android Auto Backup can include most app data by default and its supported `external` domain is the directory returned by `getExternalFilesDir`; Oblivion's normal app root may instead be under `getExternalMediaDirs()`, with an internal `filesDir` fallback. Therefore the exact treatment of the current external-media layout should be validated on supported Android/OEM backup implementations before claiming that existing worlds/content are covered by system backup. Temporary cache is not a durable backup source. These findings are from source inspection, not a device backup/restore exercise. See [Android Auto Backup rules](https://developer.android.com/identity/data/autobackup).

## Data classification and proposed policy

| Data | Repository location/example | Proposed Oblivion cloud policy | Restore considerations |
| --- | --- | --- | --- |
| Launcher preferences | `feature_settings/settings_json`, theme/language and other local preferences | Optional allowlist sync only after separate opt-in. Exclude storage mode/paths, device permissions, overlay/device state, crash consent, auth state, and transient UI state by default. | Merge per preference with local value and revision; show conflicts. Do not replace the full SharedPreferences file. |
| Profile metadata | `minecraft/<profile-id>/metadata/profile.json`, profile display/version fields | Optional sync of user-authored metadata only. Exclude APKs, package source paths, native libs, install state, device IDs, cache and executable/mod payloads by default. | A profile definition is not an installation. Restore as metadata/draft; verify local installation separately and require selection if unresolved. |
| Add-On metadata | `manifest.json` and content listing for local resource/behavior/skin packs | Optional metadata index; include only fields needed for library organization and duplicate recognition. Treat names/descriptions/UUIDs as user data. | Re-scan local files; never report a pack installed from cloud metadata alone. |
| World metadata | Level metadata/name and world listing | Optional metadata only; don't upload world databases automatically. | Match using stable content hashes/IDs when available, not display name alone. |
| User-selected archives | `.mcpack`, `.mcaddon`, `.mcworld`, exported `.levibackup` | Explicit per-backup selection and separate consent. No automatic upload. Exclude official Minecraft APKs/assets, game installation data, private server credentials, screenshots and crash logs unless separately designed and reviewed. Check rights and size before upload. | Stage, validate archive contents, hash, size, format/schema, and preview before restore. Import through existing validators where format is supported. No auto-enable or world overwrite. |
| Authentication credentials | `filesDir/xal/`, `org.levimc.xal.crypto` | **Never sync or include in Oblivion backup.** Includes Microsoft/Xbox/Minecraft tokens, XAL exports, device private keys and any future account refresh tokens. | Reauthenticate independently on the destination device. |
| Device-specific state | installation paths, package/version discovery, device ID/key, FCM registration token, permissions, hardware paths, cache | Device-local; never sync. Recreate through discovery or OS registration. | Re-detect installations and request permissions as needed. |
| AI/crash/diagnostic data | current AI session; xCrash logs; optional crash upload integration | Exclude from sync and backup by default. No crash log attachment to cloud sync. AI history is currently session-only. | No restore path. |

The operating-system backup feature and future Oblivion backup are separate. A future cloud preference must default off even though Android's existing backup may be user-enabled at OS level. Any future change to Android backup inclusion requires a data-loss/compatibility review and backup/restore tests; this milestone does not alter those rules.

## Proposed boundaries

Keep public, backend-independent contracts in open-source core, implemented only when a real consumer/backend is approved:

- `AccountProvider`: sign in/out, account state/profile, session refresh/revocation; independent from `MsftAuthManager` and MinecraftAuth.
- `CloudBackupProvider`: list/create/download/delete versioned backup objects; supports bounded streaming, resumable transfer, cancellation and typed failures.
- `SyncRepository`: local allowlist, revisions/conflict descriptions, offline operation state and manifest validation. It coordinates local data sources without making local features depend on remote availability.
- `EntitlementProvider`: query/verify a server-issued entitlement result. It cannot grant local launch, pack, world, or settings access.

The app remains the composition root. Provider interfaces must not include UI classes, Firebase types, proprietary URLs, provider admin keys, or direct token sharing. No modules are needed yet. The existing Microsoft/Xbox workflow remains independent and unchanged.

An Oblivion account would use a documented account-specific flow (for example OIDC authorization code with PKCE through a system browser, or a backend-managed verified email flow). Never ask users for Microsoft passwords or reuse Microsoft/Xbox tokens. Only the Oblivion backend may issue/verify its own session. Use short-lived access sessions, revocation, explicit logout, and Android Keystore-backed device protection for any refresh secret that must be stored locally. Device-bound Keystore keys are not themselves a cross-device recovery mechanism.

## Backup and restore protocol requirements

1. Login and cloud backup consent are separate, explicit choices. Both default off; consent identifies data categories, destination/provider, retention/deletion behavior, and whether files leave the device. Allow revocation and account deletion from the same product surface.
2. Use a versioned manifest with opaque object IDs, schema version, item type, length, cryptographic digest, creation/update time, and encryption metadata. Do not expose absolute local paths, XAL identifiers, or device-specific install paths.
3. Encrypt backups end to end before upload with an established, reviewed cryptographic format and authenticated encryption. TLS and provider-side encryption-at-rest are not a substitute for end-to-end encryption. Define device recovery/key rotation before release; never upload raw key material, passwords, or private keys. If recovery key is lost, explain that encrypted data may be unrecoverable.
4. Upload/download through bounded streaming and resumable chunks, with content hash verification, cancellation, offline status, and cleanup of incomplete server objects. Do not retry indefinitely. Never update the local library until a complete object has passed verification.
5. Restore first downloads to isolated staging and presents a preview: object names/types/versions/sizes, destination profile, conflicts, and unsupported items. Default to keep-both or skip; never silently overwrite. A selected overwrite requires explicit confirmation and a recoverable pre-restore snapshot. Publish atomically where feasible and keep a recovery journal for interrupted restore.
6. Detect conflicts by stable IDs/revisions/content digest, not names alone. Offer local, cloud, keep-both, or cancel. Preserve unrecognized schema versions and unsupported files instead of deleting them.
7. Offline operation remains local-first. Show queued, paused, failed, and completed status distinctly; resume only after user-authorized work is revalidated. Local changes are never blocked by account/session/network availability.
8. Deleting a cloud account must revoke sessions and provide an explicit deletion/export choice. Define backup retention and deletion propagation, including snapshots, object versions, logs and provider-level retention, before implementation. Local content is not deleted by cloud-account deletion.

## Backend/provider evaluation (reviewed 2026-11-11)

No provider is selected or deployed in this milestone. Managed free tiers are quotas, not a promise of zero-cost production; large world backups can exceed them. Pricing and plans change and must be rechecked before any approval.

| Candidate | Advantages | Disadvantages / security and operational considerations |
| --- | --- | --- |
| **Appwrite (candidate for a later prototype)** | Official Android Java SDK; integrated auth, databases and file storage; open-source BSD-3-Clause server and documented self-hosting. Storage permissions default to no access until granted. It documents bucket file encryption. This fits the current Java project and a provider adapter can target cloud or self-hosted endpoints. | Self-hosting means operating/upgrading/monitoring database, workers, storage, TLS, backups and incident response; vendor docs warn it often costs more in time. Managed Free currently has quotas (pricing page lists 5 GB bandwidth, 2 GB storage, 75K MAU, 2 projects and pause after inactivity); exceeding Free limits can freeze/deny writes. Its documented storage encryption is server-side, not client end-to-end encryption. Must implement strict user-scoped permissions and an independent E2E backup format. [Android Java SDK](https://appwrite.io/docs/quick-starts/android-java), [permissions](https://appwrite.io/docs/products/storage/permissions), [encryption](https://appwrite.io/docs/advanced/security/encryption), [self-hosting](https://appwrite.io/docs/advanced/self-hosting), [pricing](https://appwrite.io/pricing), [server license](https://github.com/appwrite/appwrite/blob/main/README.md). |
| **Supabase** | PostgreSQL provides portable relational data and export options; Auth, object Storage, and row-level security make user isolation explicit when policies are correctly configured. Self-hosting is possible. Managed Free currently lists 500 MB database, 1 GB file storage, 5 GB egress, and pauses inactive projects; Pro currently starts at $25/month. [Pricing](https://supabase.com/pricing), [RLS guidance](https://supabase.com/docs/guides/database/postgres/row-level-security). | The documented Kotlin Android client is community-maintained rather than official; a Java-first app would need community Kotlin or carefully implemented REST. Every exposed table needs tested RLS/grants; service-role/secret keys must never ship in the app. Managed use may require paid tier/usage; self-hosting adds operations. Provider storage encryption is not E2E. [Kotlin client status](https://supabase.com/docs/reference/kotlin/introduction), [security guidance](https://supabase.com/docs/guides/database/postgres/row-level-security). |
| **Firebase** | Lowest integration friction because this repository already includes Firebase Messaging and Crashlytics; mature Android SDKs and managed Auth/Firestore. Useful for metadata and authentication if a Firebase project and rules are properly operated. [Android setup](https://firebase.google.com/docs/android/setup). | Creates more Google/Firebase coupling and data-model lock-in than a portable SQL export. Cloud Storage for Firebase now requires Blaze billing, even where some no-cost usage applies; this conflicts with a no-paid-service assumption and requires a budget owner. Firestore usage can also require billing when free quota is exceeded. Never put admin credentials in the APK; security rules and auth must be reviewed. [Storage billing requirement](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024), [Firestore billing](https://firebase.google.com/docs/firestore/pricing), [plans](https://firebase.google.com/pricing). |

**Recommendation:** maintain the provider-neutral contracts and local-first behavior; do not deploy or add an SDK now. If a later implementation is approved, evaluate Appwrite first for a Java-compatible, self-hostable prototype, with Supabase as the relational/data-portability alternative. This is not approval to operate either service: hosting ownership, ongoing costs, privacy terms, security operations, E2E key recovery, adult/guardian involvement where required, and legal review remain undecided. Firebase is operationally convenient but less aligned with the no-paid-service and portability goals for file backup.

## Premium and entitlement separation

- Keep the current launcher, profile, pack, world, settings, and local backup features available without an Oblivion account or network.
- Do not put artificial premium checks in local features. Future premium-only hosted operations belong behind the service boundary, but local data export and deletion must remain available.
- Never accept a client preference, local boolean, UI state, or unsigned cached claim as entitlement proof. A future backend must authorize premium API operations and return short-lived, scoped, verifiable claims if the client needs to display status. Revocation, expiry, offline behavior and support checks must be specified. No paid feature or status is represented as active until a real authority verifies it.
- No payments, subscriptions, purchases, pricing UI, or entitlement implementation are part of this milestone.

## Decisions requiring explicit approval before implementation

1. Account ownership/hosting organization, domain, identity provider, support contact, age/guardian process, privacy policy, data-processing terms and data region.
2. Managed provider versus self-hosted service, named operators/on-call responsibility, budget cap and who can authorize recurring billing.
3. Which exact data categories and file-size caps users may back up; retention, deletion propagation, export and account-closure policy.
4. E2E encryption and cross-device recovery model; whether unrecoverable backups are acceptable if users lose recovery material.
5. Conflict semantics and profile/pack/world restore test matrix, including disk-space requirements and rollback behavior.
6. Premium product definitions, authorized entitlement source, legal/payment review and adult/guardian involvement. No payments should be implemented before these are resolved.

## Source review performed

Reviewed `MsftAccountStore`, `MsftAuthManager`, `XalExporter`, `AccountsActivity`, `FeatureSettings`, `SettingsStorage`, `LauncherStorage`, `VersionProfileMetadataStore`, `ContentManager`, `WorldManager`, `InstanceBackupManager`, the manifest and both backup rule files. Existing account paths, fields and deletion call sites were source-inspected; no account or storage data was read at runtime or modified.
