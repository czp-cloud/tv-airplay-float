# Security and privacy

## Network behavior

The receiver accepts AirPlay traffic and advertises services on the local network. The embedded web remote listens on port 8765 and currently uses HTTP without authentication or pairing. Other devices able to reach that endpoint can move/resize the window, change opacity, choose audio and change the background-start preference.

The controller validates local Host/Origin and request parameters, exposes fixed assets and bounded API commands, and has no shell or arbitrary-file API. These controls do not provide user authentication or transport encryption. Use a trusted LAN; do not expose receiver/control ports to the internet.

The webpage contains no third-party scripts, analytics or remote fonts. The position preview is a diagram, not a captured TV image. Host diagnostic tools can collect sensitive window/audio state or screenshots; their outputs are local and ignored by Git.

## Permissions

The app uses display-over-other-apps permission and foreground services for reception/playback. Its diagnostic receivers require Android's DUMP permission. It does not request camera, microphone, accessibility or root access. Boot reception is controlled by the app's setting and by the device firmware.

See the [manifest](app/src/main/AndroidManifest.xml) for the exact permission list. The tested platform is Android 10; newer-platform permission/boot behavior requires separate verification.

## Reporting a vulnerability

Use your hosting platform's private vulnerability reporting channel when enabled. If no private channel is available, contact the repository owner through the host's private contact mechanism, or open an issue requesting a private contact without including the vulnerability details, logs or exploit.

This repository has no published security response SLA or dedicated security email. Maintainers must configure a reachable private reporting channel before public launch; see the [publication guide](docs/maintainers/releases.md). No upstream project's security address is presented as responsible for this derivative.

Include the affected revision, attack prerequisites, impact and a minimal reproduction over a private channel. Do not post receiver secrets, credentials or household captures.

## Supported state

v0.3.3 is the first source milestone, not a promise of a long-term support branch. Report issues against current main when possible. Security fixes should receive a focused review and release note.
