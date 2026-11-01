# Contributing

Thanks for helping make the receiver work on more TVs. Start with the [architecture](docs/development/architecture.md), [build](docs/development/building.md) and [testing](docs/development/testing.md) guides.

## Before a change

Open an issue for a substantial behavior change. A small bug fix or documentation correction can go straight to a pull request. Explain the concrete use case, device constraints and expected behavior.

Preserve the project license, upstream attribution and existing Git history. No contributor license agreement is currently used; contributions are made under the repository license.

## Development

1. Clone with submodules and create a focused branch.
2. Configure the Android toolchain; Node and Python tooling are optional for their respective checks.
3. Make the change and run relevant checks.
4. Include a concise explanation and actual verification in the pull request.

Keep production code, fixtures and private hardware evidence separate. Generated captures and reports belong in ignored artifacts/. Keep runtime dependencies small and update [dependency documentation](docs/dependencies.md) and notices when adding or replacing one.

Do not change application/JNI namespaces or submodule revisions as incidental cleanup. For native receiver changes, inspect the existing UxPlay patches and record changes as patches.

## Reporting a device issue

Include:

- TV model, Android version and app version.
- Sender device/OS and whether this is Control Center Screen Mirroring.
- Foreground app or HDMI input, output device and selected sound mode.
- Reproduction steps, expected behavior and what actually appeared or was audible.
- Relevant redacted errors or state fields.

Remove personal media, IP/MAC addresses, receiver identifiers and credentials before attaching logs. Do not put vulnerabilities in a public issue; follow [SECURITY.md](SECURITY.md).

## Pull requests

Describe the problem and resulting behavior first. List checks you actually ran, identify fixture versus hardware evidence, and note anything still unverified. Screenshots of UI changes should use synthetic/demo content rather than household media.

The [GitHub CI definition](.github/workflows/ci.yml) checks repository structure, builds/tests/lints the Android app, and runs the browser fixture suite. It does not connect to TVs or publish signed releases. Local equivalents are documented even when CI has not yet run on the hosting platform.
