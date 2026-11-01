# Changelog

## Unreleased

Repository preparation for public collaboration; the installed application version is unchanged.

- Add English/Chinese project overviews, a synthetic remote preview and a documentation index.
- Group usage, architecture, research and dated validation records under docs/.
- Add coding-agent guidance, contribution/security/conduct documents and issue/PR templates.
- Document direct/runtime/native/build/test dependencies, submodule pins and third-party notices.
- Add portable, lockfile-backed browser tooling, optional device requirements and repository checks.
- Replace the inherited signing-dependent release workflow with pinned build/lint/test CI and dependency-update configuration.
- Pin the Gradle distribution checksum and centralize existing NanoHTTPD/JUnit versions without upgrading them.
- Remove upstream store listing/screenshots that do not describe this derivative; remove private network addresses from current documents.
- Add the GitHub clone URL, contribution entry points and private vulnerability-reporting channel for the public repository.

## v0.3.3 — 2026-11-01

First source milestone. Application version: 0.3.3-opacity; versionCode: 42. Earlier commits retain the development history rather than implying earlier public releases.

- Rootless AirPlay floating window using the upstream receiver and decoding stack.
- Display after decoded video and hide on mirror disconnect, without opening a full-screen control Activity.
- Follow portrait/landscape geometry, dock right on connection/aspect changes, and allow manual movement/size adjustment.
- Mobile LAN remote with joystick, size, opacity, volume, audio mode and startup controls.
- TV-only, mirror-only and mixed audio; simultaneous HDMI and mirror audio confirmed on the tested TV.
- Background-start preference enabled by default; package-update and simulated quick-boot startup verified, physical power-cycle validation pending.
- Preserve upstream history, licenses, submodule revisions and build instructions.

Opacity control ranges from 12–100%, updates live and persists. Browser/device HTTP checks passed; a separate visual acceptance remains pending. The locally tested APK uses a debug signature. Keys, caches, generated APKs, logs and household captures are excluded from Git.
