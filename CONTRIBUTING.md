# Contributing

## Before you open a change

- check existing issues and release notes first
- keep changes focused and easy to review
- do not commit generated APKs, keystores, or local SDK settings

## Local setup

1. Install JDK 17
2. Install Android SDK 34
3. Run `./gradlew assembleDebug`

## Pull requests

- describe the problem and the approach
- include screenshots for UI changes
- mention tested Android version and device when relevant
- update `README.md`, `CHANGELOG.md`, or `release-notes/` when the user-facing behavior changes

## Code style

- Kotlin style follows the existing project layout
- prefer small, readable changes over broad refactors
- keep user-facing text explicit and easy to scan
