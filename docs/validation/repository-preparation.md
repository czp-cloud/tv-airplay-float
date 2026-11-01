# Repository preparation — 2026-11-01

This change reorganizes documentation and host tooling; it does not install an APK, change TV settings, publish a public repository or move the v0.3.3 tag.

## Local checks

- Android assembleDebug, lintDebug and testDebugUnitTest passed after catalog/tooling changes. Seven geometry tests passed, with zero failures/errors.
- Chromium (installed Chrome override) and WebKit fixture suite: 90 checks passed, using the new npm lockfile installation and portable launcher.
- Optional adb-shell requirements installed in a fresh local virtual environment; both ADB CLI help commands, HTTP CLI help and Python byte-compilation passed. No device connection was made.
- actionlint 1.7.12 accepted the new GitHub workflow. The downloaded validator archive was checked against its published checksum. Issue/PR/dependency YAML was parsed locally.
- Repository checker passed: required documents, relative document/image links, submodule inventory, npm manifest/lock consistency, Gradle distribution checksum and current-document host-address checks.

Reports, caches, the validator binary and build outputs remain in ignored local paths. Exact file/link counts can change as documents are added; rerun tools/repo_check.py for the current counts.

## What these checks do not prove

The GitHub workflow has not run on a GitHub-hosted repository. Local-equivalent checks and actionlint do not constitute a remote green build. Hardware sound/video/boot results remain those in the dated milestone records; no new physical acceptance was performed for this packaging work.

Current files no longer contain the known private LAN addresses, but earlier private commits and the immutable first milestone retain their historical documents. Full-history publication review and the public contact/hosting decisions remain described in the [release guide](../maintainers/releases.md).
