# Sluer

An Android device-optimization toolbox that talks to your device through
[Shizuku](https://shizuku.rikka.app/) — no root required.

## Roadmap

- **Dashboard** — Shizuku status (binder, permission, ADB/root identity) + device stats
- **Boost** — one-tap memory cleanup and force-stop of a user-editable app list
- **Debloat** — disable / uninstall system apps (fully reversible)
- **Battery** — per-app standby buckets and Doze whitelist control
- **Tweaks** — animation scales, Private DNS, background process limit (with undo)
- **Console** — run shell commands with Shizuku privileges (validation tool)

## Build

Debug APKs are built automatically by GitHub Actions
(`.github/workflows/build.yml`) on every push, and attached as downloadable
artifacts on the run page.

Built with Kotlin + Jetpack Compose (Material 3), minSdk 26.
