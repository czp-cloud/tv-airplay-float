# Third-party notices

TV AirPlay Float is derived from [jqssun/android-airplay-server](https://github.com/jqssun/android-airplay-server), baseline a46cca0a6a1274e9e33c0e750f0f1c1eb1d881ea. The repository retains upstream history, source copyright headers and the root [GPL-3.0 license](LICENSE). Renaming the application or reorganizing documentation does not remove those notices.

## Bundled/native components

| Component | Notice/license location |
| --- | --- |
| UxPlay | [GPL-3.0](app/src/main/cpp/third_party/UxPlay/LICENSE), source headers and pinned submodule |
| playfair in UxPlay | [GPL-3.0](app/src/main/cpp/third_party/UxPlay/lib/playfair/LICENSE.md) |
| llhttp in UxPlay | [MIT and Fedor Indutny notice](LICENSES/llhttp-MIT.txt) |
| libplist and libcnary | [libplist LGPL-2.1](app/src/main/cpp/third_party/libplist/COPYING.LESSER), [libcnary notice](app/src/main/cpp/third_party/libplist/libcnary/COPYING) and source headers |
| FFmpeg subset | [Upstream license description](app/src/main/cpp/third_party/ffmpeg/LICENSE.md), [LGPL-2.1 text](LICENSES/LGPL-2.1.txt) and per-file notices |
| OpenSSL | [Apache-2.0](LICENSES/Apache-2.0.txt), OpenSSL source notices |
| openssl-cmake | [MIT / ViaDuck notice](LICENSES/openssl-cmake-MIT.txt) |
| Oboe | [Upstream license at 1.9.3](https://github.com/google/oboe/blob/1.9.3/LICENSE) |
| NanoHTTPD | [BSD-3-Clause and full copyright notice](LICENSES/NanoHTTPD-BSD-3-Clause.txt) |

## Android and development libraries

AndroidX/Compose/Media3, Kotlin coroutines, Dagger/Hilt and Playwright declare Apache-2.0 licensing. JUnit declares EPL-1.0. Exact coordinates, declared versions, roles and source links are listed in [docs/dependencies.md](docs/dependencies.md). Optional host-only adb-shell tooling is separate from the APK.

The original module source headers, Maven metadata and license files remain authoritative for their components and transitive dependencies. This document is an attribution index; it is not a replacement for those license texts or a complete binary SBOM.

## Source and binary packaging

Keep the source tag, submodule revisions, patch series, build instructions and relevant notices with a distribution. Downloaded OpenSSL sources and submodule contents are not all included in a plain top-level Git ZIP. The [build guide](docs/development/building.md) explains obtaining them.

The remote-preview image under docs/images is rendered from this project's web fixture. It contains synthetic state and no household capture. Legacy upstream store screenshots/listing metadata are not presented as screenshots or release channels of this derivative.
