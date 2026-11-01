# Building

## Required toolchain

| Component | Pinned/configured version | Source of truth |
| --- | --- | --- |
| JDK | 21 to run Gradle; Java source target 17 | app/build.gradle.kts |
| Gradle | 9.8.1 | gradle/wrapper/gradle-wrapper.properties |
| Android Gradle Plugin | 9.4.1 | gradle/libs.versions.toml |
| SDK platform | 37.0; compile/target API 37 | app/build.gradle.kts |
| Build-tools | 36.0.0 in the validated local toolchain | Android SDK installation |
| NDK | 27.0.12077973 | app/build.gradle.kts |
| CMake | 3.22.1 | app/build.gradle.kts |
| Host tools | Git, make, Perl, a shell, compiler/build utilities | native dependency builds |

macOS was used for the recorded local builds. CI is configured for Ubuntu 24.04. On Windows, use a Linux environment such as WSL2 for the shell-based native builds; a Windows-native build has not been validated.

## Obtain the sources

Clone using the URL of the repository you are viewing, with `--recurse-submodules`. If you already cloned without them:

```sh
git submodule update --init --recursive
git submodule status
```

A source ZIP does not contain the contents of Git submodules. Preserve their revisions and patch series when preparing source distributions.

## Android SDK

Set JAVA_HOME to JDK 21 and ANDROID_HOME to your SDK, or configure an ignored local.properties containing sdk.dir. Install the components with your SDK's sdkmanager:

```sh
sdkmanager "platforms;android-37.0" "build-tools;36.0.0" "ndk;27.0.12077973" "cmake;3.22.1"
```

Accept Android SDK licenses using sdkmanager when prompted. The API 37 platform directory/package name in the validated SDK is android-37.0.

## Build

```sh
./gradlew --no-daemon :app:assembleDebug -PtargetAbi=armeabi-v7a
```

Output: app/build/outputs/apk/debug/app-debug.apk. ARM32 is the tested TV ABI. The build accepts other ABIs, for example `-PtargetAbi=arm64-v8a` or a comma-separated list, but that does not imply hardware validation.

Dependencies are downloaded from Google Maven, Maven Central, Gradle distribution servers, Git submodule hosts and the OpenSSL source host. An optional `-PuseMavenMirror=true` adds the configured Aliyun repositories; it is not enabled by default. Configure a host-specific HTTPS proxy through the toolchain/environment when needed. Do not commit local proxy endpoints.

## Native patches

The inherited applyUxplayPatches task restores its known touched files in the UxPlay checkout and applies app/src/main/cpp/patches/UxPlay in order. Check submodule status before building so this does not overwrite a contributor's edits. Keep intentional protocol changes in the committed patch series.

A successful build can leave those generated modifications in UxPlay. Review and remove only the known generated changes before a repository handoff; do not reset unrelated edits.

## Signing and releases

Debug builds use the host's Android debug key. Different hosts may produce differently signed APKs, and an APK with a different signature cannot update an installed app directly.

Release signing reads storeFile, storePassword, keyAlias and keyPassword from ignored local.properties when configured. Keep keystores and credentials outside the repository. No CI signing secrets are required for the current debug-build checks.

v0.3.3 is the first source tag and corresponds to app version 0.3.3-opacity / versionCode 42. Packaging documentation changes do not alter this installed application version. See the [release guide](../maintainers/releases.md).
