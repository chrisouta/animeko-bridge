# Animeko Local Media Bridge

Animeko Local Media Bridge is a small desktop tool I built mainly for personal use.

Its purpose is simple: scan my local anime folders, clean up episode matches when needed, and expose the result through a small Jellyfin-compatible bridge so Animeko can play the files.

This repository is public because it may still be useful to other people with a similar setup, but it is not meant to be a polished general-purpose media server.

## What It Does

- scan one or more folders recursively for local media files
- infer series titles and episode numbers from filenames and parent folders
- review and correct low-confidence matches in the desktop UI
- serve local media and sidecar subtitles over a Jellyfin-compatible API
- bind to `127.0.0.1` for local use or to the local network when needed

## Typical Usage

1. Launch the app.
2. Add one or more media folders.
3. Click `Scan Now`.
4. Review any low-confidence matches and fix them in the right panel.
5. Start the local server.
6. In Animeko, add a `Jellyfin` source with the Base URL, User ID, and API Key shown in the app.

## Scope

- desktop GUI only
- recursive library scanning
- subtitle sidecar serving
- local and LAN HTTP streaming
- only the minimal Jellyfin endpoints needed by Animeko

## Build And Run

Requirements:

- JDK 21 or newer

Common commands:

- `./gradlew run`
- `./gradlew test`
- `./gradlew packageReleaseDistributionForCurrentOS`

Packaging notes:

- GitHub Actions builds both the Windows installer and the macOS package in `.github/workflows/build-windows-exe.yml`.
- The Windows artifact is uploaded as `AnimekoLocalMediaBridge-windows-exe`.
- The macOS artifact is uploaded as `AnimekoLocalMediaBridge-macos`.
- Pushing a tag like `v0.1.0` will automatically create or update a GitHub release and attach the built desktop packages.
- If you package on macOS with Homebrew `openjdk@21`, Compose may require:
  - `./gradlew packageReleaseDistributionForCurrentOS -Pcompose.desktop.packaging.checkJdkVendor=false`

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
