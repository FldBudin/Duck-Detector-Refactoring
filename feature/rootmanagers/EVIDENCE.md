# Root Managers evidence record

Status: reviewed

Root Managers asks whether any mainstream root manager app (the KernelSU family, APatch, Magisk or
SKRoot) is present and launcher-visible to this app, in the owner profile or in any other profile the
LauncherApps API exposes. A match is a finding that such an app is installed and visible — not a
claim that the device is rooted, that the manager is active, or that no manager exists. A clean
result means the enumeration searched every accessible profile and matched nothing, which still does
not prove absence: an app that is hidden, keeps no enabled launcher activity, or lives in an
inaccessible profile leaves no trace the launcher visibility path can see. This detector reads only
public `LauncherApps` / `LauncherActivityInfo` / `ApplicationInfo` values; it reads no signatures,
no APK contents and no root state.

## Version model

| Platform | AOSP lifecycle difference | Duck policy |
| --- | --- | --- |
| Android 10 / API 29 | `<queries>`/package visibility exists but filtering is enforced only for targetSdk 30+. `LauncherApps` and `getActivityList` exist since API 21; `ApplicationInfo.zygotePreloadName` exists since API 29. | `minSdk 29`. This app targets 37, so filtering applies on this release too. The zygote anchor is only probed where the field exists. |
| Android 11–16 / API 30–36 | Package visibility filtering is stable. A `<queries>` grant is stored as an app-id pair with no user dimension, so a launcher-visible app in the caller's profile group stays visible even from another profile. | The cross-profile behaviour this detector relies on. An empty result is reported as "not observed", never as "absent". |
| All releases | `getActivityList` never returns null; an empty result is `Collections.EMPTY_LIST`. `LauncherApps.getProfiles()` returns `UserManager.getUserProfiles()` for a normal caller, which includes the owner (user 0); a managed-profile caller sees only itself. | An empty list is a real "nothing matched"; only an exception is an unavailable enumeration. The owner profile is covered without a separate query. |

AOSP baseline read for this detector: `frameworks/base` at `android-security-16.0.0_r6`,
commit `37711ae187558096ac0cd389a91a01360035287a` (Android 16 / API 36).

## Measured family anchors

The anchors come from the shipped manifests of each family's released APKs (decoded for this
detector), not from source: the package name is only what the build shipped with, while the
application class and zygote preload name reveal the family after a rename.

| Family | Default package name | Application class (suffix) | Zygote preload | Label prefix |
| --- | --- | --- | --- | --- |
| KernelSU | `me.weishu.kernelsu` | `.KernelSUApplication` | `.magica.AppZygotePreload` (v3.3.0; absent in the v3.1.0 build) | `KernelSU` |
| KernelSU-Next | `com.rifsxd.ksunext` | `.KernelSUApplication` | none | `KernelSU-Next` |
| SukiSU Ultra | `com.sukisu.ultra` | `.KernelSUApplication` | none | `SukiSU` |
| ReSukiSU | `com.resukisu.resukisu` | `.KernelSUApplication` | `.magica.AppZygotePreload` | `ReSukiSU` |
| APatch | `me.bmax.apatch` | `.APApplication` | `.magica.AppZygotePreload` | `APatch` |
| SKRoot | `com.linux.permissionmanager` | none declared | unverified (`.helper.AppZygotePreload`) | `PermissionManager` |
| Magisk | `com.topjohnwu.magisk` | none catalogued | none | `Magisk` |

Notes:
- The zygote/magica mechanism is per fork and per version: present in KernelSU v3.3.0, ReSukiSU and
  APatch; absent in the KernelSU v3.1.0 build, SukiSU and KernelSU-Next. The anchor matches by suffix
  and only contributes when the field is present.
- Magisk's application class (`.core.App`) is deliberately not catalogued: it is generic enough to
  promote unrelated apps to a strong anchor. A hidden Magisk randomizes package name, label, class
  names and signature, so it stays below the strong-anchor floor and is only ever corroborating.
- The shared `.KernelSUApplication` suffix cannot separate the KernelSU-family forks on its own; the
  match rules attribute a record to its most specific family (package field first, then the longest
  label prefix) so a variant is not mislabelled as KernelSU.

## Signals

### Launcher visibility enumeration

- Observable signal: the launcher activity list `LauncherApps.getActivityList(null, user)` returns for every profile in `getProfiles()`, and the `ApplicationInfo` each `LauncherActivityInfo` carries.
- Producing subsystem: the system `LauncherAppsService`, and the `<queries>` visibility model enforced by `AppsFilterBase` in PackageManager.
- Mechanism: a `<queries>` grant is stored as an app-id pair (`mQueriesViaPackage`/`mQueriesViaComponent`) with no user dimension, and `canAccessProfile` allows a parent profile to read its same-profile-group profiles without a permission, so launcher visibility reaches across the profile group; `getActivityList` injects the caller's uid and returns each launcher activity with its application class, zygote preload name, uid and source path.
- References: frameworks/base core/java/android/content/pm/LauncherApps.java; services/core/java/com/android/server/pm/LauncherAppsService.java; services/core/java/com/android/server/pm/AppsFilterBase.java; services/core/java/com/android/server/pm/UserManagerService.java; developer.android.com package visibility documentation.
- Applicability: `minSdk 29`; the `<queries>` mechanism is consistent across API 30–36; the app already holds `QUERY_ALL_PACKAGES`, which short-circuits the filter.
- Visibility limits: an inaccessible profile, a disabled work profile, or a launcher activity that is not enabled by default yields no record — an empty enumeration means "not observed", not "absent"; a per-profile `SecurityException` is recorded as an issue and does not discard the other profiles' results.
- Result states: evaluated (profiles searched), unavailable (the platform refused or failed), undecidable (no accessible profile returned).
- Interpretation: a record is evidence an app is launcher-visible to this app in that profile; it is never evidence the device is rooted.

### Family identity anchors

- Observable signal: `ApplicationInfo.packageName`, `ApplicationInfo.className`, `ApplicationInfo.zygotePreloadName`, the launcher activity label and the launcher component class.
- Producing subsystem: the package's own manifest as resolved and parceled by PackageManager.
- Mechanism: `ApplicationInfo.writeToParcel` writes `processName`, `className`, `sourceDir`, `uid` and `zygotePreloadName` unconditionally, so these fields survive the trip from system_server to the app; an `applicationId`-only rename leaves the application class and namespace intact, and a namespace-randomising spoof still keeps the class simple name. `zygotePreloadName` is `@hide` and absent from the SDK, so it is read reflectively through the LSPosed hidden-api bypass (as the TEE keystore probes do), not through a public accessor.
- References: frameworks/base core/java/android/content/pm/ApplicationInfo.java; core/java/android/content/pm/LauncherActivityInfo.java; the released manifests of each family listed under Measured family anchors.
- Applicability: `ApplicationInfo.className` is public since API 1 and every other field is public on API 29+; `zygotePreloadName` exists since API 29 but is `@hide`, so its match relies on the hidden-api bypass and is simply absent when that read fails.
- Visibility limits: Magisk's hidden mode randomizes package name, label, class names and signature, so no strong anchor survives there; SKRoot declares no application class; the weak anchors (label, launcher class) are shared words a coincidental app could carry; a failed hidden-api read drops only the zygote anchor, while the package and application-class anchors still match.
- Result states: high (a strong anchor plus another field), medium (one strong anchor alone), low (two weak fields only), no match (fewer than one strong or two weak fields).
- Interpretation: a strong match is a warning; a weak-only match is informational, because two family-shaped words can meet on an unrelated app.

## Relationship to other detectors

This detector's catalogue is the authoritative superset for root managers. Native Root keeps its own
narrower `KERNELSU_MANAGER_PACKAGES` list and its kernel-side signals, and Dangerous Apps keeps its
own root-tool entries; both are unchanged except that Dangerous Apps now also lists the official
KernelSU and APatch package names. Reporting the same app from more than one detector is independent
evidence — launcher visibility, a manifest read and a directory fallback are different mechanisms —
and each detector states its own path.

Sources measured on 2026-10-08 from the Android 16 AOSP baseline above and the released APKs of each
family (KernelSU 3.1.0 and 3.3.0, SukiSU Ultra, ReSukiSU, KernelSU-Next 3.4.0 normal and spoofed,
APatch, SKRoot, Magisk 30.7).
