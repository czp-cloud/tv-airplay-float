# Compatibility and validation records

These are dated engineering records for a particular TV and sender setup. They distinguish tool results from human picture/audio observations.

| Area | Evidence at the first source milestone |
| --- | --- |
| Target | Xiaomi MiTV-MTEQ0, Android 10 / API 29, ARM32 |
| Phone mirror | iPhone Control Center mirroring visible, including over HDMI |
| Window | Auto-show/hide, movement, sizing, portrait/landscape and right docking confirmed |
| Audio | TV/mirror switching and simultaneous HDMI + mirror sound confirmed by the tester |
| Opacity control | Browser fixture and device HTTP checks passed; separate visual acceptance pending |
| Background startup | Package-update start and simulated quick boot passed; physical power cycle pending |
| Other devices/routes | No general compatibility claim |

## Milestone records

- [Initial floating window](v1.md)
- [Mobile remote and audio switching, 0.2.0](v0.2.0.md)
- [Volume/direct control preparation, 0.2.1](v0.2.1.md)
- [Mixed audio and startup, 0.3.0](v0.3.0.md)
- [Aspect changes and right docking, 0.3.1–0.3.2](v0.3.1-0.3.2.md)
- [Opacity control, 0.3.3](v0.3.3.md)

Historical notes are retained in Chinese. Private local addresses in the current documents are replaced by placeholders. Raw logs and captures are intentionally excluded; artifact paths in these notes describe local evidence, not downloadable repository files.

## Repository maintenance

- [Open-source repository preparation](repository-preparation.md) — local checks and publication boundaries

## New reports

Use the device-compatibility issue template with model, firmware, app/sender versions, input/audio route and reproducible observations. A green CI job does not confirm physical HDMI composition, DRM playback, audible mixing or boot policy.
