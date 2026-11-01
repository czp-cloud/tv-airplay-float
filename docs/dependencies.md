# Dependency inventory

This inventory describes the direct dependencies and bundled native components used by this derivative. Versions come from committed configuration and initialized source checkouts, not from an unverified list of upstream features.

- Android/JVM coordinates: [version catalog](../gradle/libs.versions.toml), [app build](../app/build.gradle.kts).
- Native revisions: [.gitmodules](../.gitmodules) plus committed gitlinks, mirrored in [dependencies.json](dependencies.json).
- Browser tooling: [package.json](../tools/package.json) and [package-lock.json](../tools/package-lock.json).
- Optional device tooling: [requirements-device.txt](../tools/requirements-device.txt).

## Android runtime

| Component | Declared version | Purpose | License/source |
| --- | --- | --- | --- |
| AndroidX Core KTX | 1.16.0 | Android helpers | [Apache-2.0 / AndroidX](https://android.googlesource.com/platform/frameworks/support/) |
| Activity Compose | 1.9.3 | Activity/Compose integration | Apache-2.0 / AndroidX |
| Lifecycle runtime/service/viewmodel | 2.9.4 | Service and UI lifecycle | Apache-2.0 / AndroidX |
| DataStore Preferences | 1.1.1 | Preferences support | Apache-2.0 / AndroidX |
| AndroidX Media | 1.7.0 | Media session integration | Apache-2.0 / AndroidX |
| Media3 ExoPlayer/HLS/UI/transformer | 1.11.1 | Inherited media playback machinery | [Apache-2.0 / Media](https://github.com/androidx/media) |
| Compose UI/graphics/tooling/material | BOM 2026.05.00 | Inherited control UI | Apache-2.0 / AndroidX; individual versions follow the BOM |
| Kotlin coroutines Android | 1.9.0 | Asynchronous service/UI work | [Apache-2.0](https://github.com/Kotlin/kotlinx.coroutines) |
| Dagger Hilt | 2.60.1 | Dependency injection | [Apache-2.0](https://github.com/google/dagger) |
| Hilt Navigation Compose | 1.2.0 | Inherited navigation integration | Apache-2.0 / AndroidX |
| Oboe | 1.9.3 | Native shared AAudio output | [Apache-2.0](https://github.com/google/oboe/blob/1.9.3/LICENSE) |
| NanoHTTPD | 2.3.1 | Embedded LAN HTTP server | [BSD-3-Clause](../LICENSES/NanoHTTPD-BSD-3-Clause.txt) |

A dependency being present does not mean all its capabilities are exposed. Direct AirPlay video URL/HLS handoff remains disabled in the service. No new browser framework, CDN asset or Node runtime is needed on the TV.

## Native components

| Component | Revision/version | Built role | License text |
| --- | --- | --- | --- |
| UxPlay | df67c212a433cf6dda3676dd40c097900d24e645 | AirPlay/RAOP receiver core | [GPL-3.0](../app/src/main/cpp/third_party/UxPlay/LICENSE) |
| llhttp embedded in UxPlay | 9.3.0, at the UxPlay pin | HTTP parsing | [MIT](../LICENSES/llhttp-MIT.txt) |
| playfair embedded in UxPlay | at the UxPlay pin | Receiver protocol support | [GPL-3.0](../app/src/main/cpp/third_party/UxPlay/lib/playfair/LICENSE.md) |
| libplist | f41b1ea67045e0c09339974d83e389972d84f166 | Property lists | [LGPL-2.1](../app/src/main/cpp/third_party/libplist/COPYING.LESSER) |
| libcnary within libplist | at the libplist pin | Native data structures | [LGPL-2.1](../app/src/main/cpp/third_party/libplist/libcnary/COPYING) |
| FFmpeg | 38b88335f99e76ed89ff3c93f877fdefce736c13 | Static avcodec/avutil; AAC and ALAC decode | [Upstream license conditions](../app/src/main/cpp/third_party/ffmpeg/LICENSE.md) |
| openssl-cmake | 4edd36a8dab5f85a8f92650b5bdf6e0cab13aab8 | OpenSSL build integration | [MIT](../LICENSES/openssl-cmake-MIT.txt) |
| OpenSSL | 3.4.4 | Cryptography | [Apache-2.0](../LICENSES/Apache-2.0.txt) |

Native wiring is in [CMakeLists.txt](../app/src/main/cpp/CMakeLists.txt). FFmpeg is configured with disable-all, no networking/autodetection, AAC/ALAC decoders and static codec/utility libraries; this build does not enable GPL, nonfree or version3 options. FFmpeg's source tree has components under different terms, so inspect its license file and the [actual build flags](../app/src/main/cpp/cmake/BuildFFmpeg.cmake) when changing that configuration.

OpenSSL is downloaded by the pinned openssl-cmake helper at native-build time; it is not another Git submodule. UxPlay receives the committed [patch series](../app/src/main/cpp/patches/UxPlay). Keep these patches with the source distribution.

## Build and test dependencies

| Component | Version | Scope |
| --- | --- | --- |
| JDK | 21 | Gradle host |
| Gradle wrapper | 9.8.1, distribution checksum pinned | Build |
| Android Gradle Plugin | 9.4.1 | Build |
| Kotlin Compose plugin | 2.2.10 | Build |
| KSP | 2.3.12 | Annotation processing |
| Hilt compiler/plugin | 2.60.1 | Build |
| Android SDK | Platform 37.0, validated build-tools 36.0.0 | Build |
| NDK / CMake | 27.0.12077973 / 3.22.1 | Native build |
| JUnit | 4.13.2; EPL-1.0 | JVM tests |
| Node / npm | Node 20+; CI selects 24 | Browser test host only |
| Playwright | 1.64.1; Apache-2.0 | Browser fixtures only |
| Python | 3.11+ for repository checker | Host checks only |
| adb-shell | 0.4.4; Apache-2.0 | Optional device diagnostics only |

SDK/JDK/Gradle distribution components have their own upstream terms. They are not all embedded in the APK. Python's adb-shell pulls cryptography, pyasn1 and rsa dependencies; install these in an ignored virtual environment and inspect the resolved pip graph for your host. Playwright downloads test browsers separately from the npm package.

## Resolve and audit

The tables are a direct-dependency inventory, **not a complete transitive SBOM**. Inspect the resolved versions and license notices before shipping a changed binary:

```sh
./gradlew :app:dependencies --configuration debugRuntimeClasspath
npm ls --prefix tools --all
python3 -m pip inspect
git submodule status
```

Run pip inspection from the virtual environment you created for device tools. The npm lockfile fixes the browser tooling graph. Runtime transitive Gradle versions are resolved by the catalog/BOM and Gradle rules; they are not claimed to be independently locked by this inventory.

Use [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) and the original source/header notices. Dependency updates must review licensing, native patch applicability, ABIs and functional behavior together.
