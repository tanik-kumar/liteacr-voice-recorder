# LiteACR

LiteACR is a simple Android voice recorder built for manual recording on modern Android devices.

It records from the microphone, keeps a foreground notification active while recording, and stores finished files in `Music/LiteACR` so they are easy to find from the Files app.

## Features

- start, stop, pause, and resume recording
- shared storage output in `Music/LiteACR`
- separate recordings screen with in-app playback
- share and delete actions for saved files
- Android 13 notification-permission support

## Storage

Saved recordings are written to:

`Music/LiteACR`

The app also shows the active folder path on the main screen and on the recordings screen.

## Build

Requirements:

- Android Studio or Android SDK command-line tools
- JDK 17
- Android SDK 34

Debug build:

```bash
./gradlew assembleDebug
```

Release build:

```bash
./gradlew assembleRelease
```

## Release signing

The release build supports local signing through `keystore.properties`.

1. Copy `keystore.properties.example` to `keystore.properties`
2. Point `storeFile` to your `.jks` file
3. Fill in the passwords and alias
4. Run `./gradlew assembleRelease`

Sensitive files are ignored through `.gitignore`:

- `keystore.properties`
- `keystore/`

Back up your release keystore before publishing updates. You need the same key for future app updates.

## GitHub About

Suggested repo description and topics are in `docs/github-about.md`.

## Release notes

Initial release notes are in `release-notes/v1.0.0.md`.
