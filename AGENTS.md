# Repository guidance for coding agents

## Scope and entry points

This file applies to this repository. Read README.md, CONTRIBUTING.md and the relevant document under docs/ before changing code. Current behavior and validation limits are recorded in docs/validation/README.md.

- Application ID: `dev.czp.tvairplay`.
- Kotlin/JNI namespace: `io.github.jqssun.airplay`. Do not mechanically rename it: native registration, receivers and manifest entries depend on it.
- JVM/UI code: `app/src/main/kotlin/io/github/jqssun/airplay/`.
- Native code and patches: `app/src/main/cpp/`.
- Web assets: `app/src/main/assets/remote/`; they ship inside the APK without a web build step.
- Host tools: `tools/`. Python needs 3.11+ for the repository checker; browser tools need Node 20+.
- Dependencies and provenance: `docs/dependencies.md`, `THIRD_PARTY_NOTICES.md`, `.gitmodules`.

## Working rules

1. Inspect Git status and preserve unrelated changes. Do not force-push, rewrite existing tags, or change repository visibility as part of routine maintenance.
2. Keep patches focused. Preserve upstream copyright notices and license files.
3. WindowManager and audio policy mutations belong on the service's main handler. Receiver callbacks must not block decoder/network threads.
4. Keep idle reception invisible, retain underlying input focus, and show only after decoded video. Do not launch a full-screen Activity on phone connection.
5. Do not change system media volume to simulate mirror-volume control. Preserve the distinction between TV-only, exclusive mirror audio and mixed output.
6. Keep web assets local; do not add CDN scripts, analytics or remote fonts. HTTP commands must use the existing bounded dispatch and parameter validation.
7. Do not assume ADB access means root or that a successful focus request proves audible mixing.
8. Never run device smoke tests, install APKs, take screenshots or restart household devices without task authorization and an explicit target. Repository/browser checks never need a TV.
9. Do not commit signing keys, credentials, local.properties, SDK/cache directories, APKs, logs or household captures. Use ignored artifacts/ for evidence.
10. Document actual validation separately from planned work. Do not describe a fixture, simulator or denied broadcast as physical-device acceptance.

## Builds and submodules

Read docs/development/building.md before building. The inherited applyUxplayPatches task restores its known touched files in the UxPlay submodule before applying committed patches. Check for user edits in that submodule first; do not run it over unrelated edits. Change upstream code through patch files, not an unrecorded submodule working tree.

Builds leave the known patch files dirty inside UxPlay. Review that diff and restore only generated changes when safe; do not reset arbitrary submodule work. Keep the committed submodule revisions unchanged unless intentionally updating dependencies.

## Checks

```sh
python3 tools/repo_check.py
./gradlew --no-daemon :app:assembleDebug :app:lintDebug :app:testDebugUnitTest -PtargetAbi=armeabi-v7a
npm ci --prefix tools
npm run browsers --prefix tools
npm test --prefix tools
```

Choose checks relevant to the change. Read docs/development/testing.md for browser overrides and optional hardware tooling. Documentation-only edits need the repository/link check and git diff --check; do not reinstall the TV app for them.

## Version control and delivery

Use a descriptive commit subject that explains the resulting behavior. PRs should state the problem, final change, checks performed and remaining hardware limits. Follow the configured project remote for authorized commits/pushes; do not push to the upstream author's repository accidentally. The immutable v0.3.3 tag records the first source milestone.
