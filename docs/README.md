# Swifty Companion

An Android app to look up 42 student profiles through the 42 API v2. Type a
login and the app shows the profile: photo, level, contact details, location,
wallet and more.

Built with Kotlin and Jetpack Compose.

## Features

- Search by login. Invalid input, unknown logins, missing connection, rate
  limiting and server errors each get their own message, with a retry button
  when retrying can help.
- Profile card in the coalition's color: coalition logo and name, level tag,
  photo inside a ring that fills with the progress towards the next level,
  full name, login, grade, selected title, level, wallet, correction points and
  the workstation the user is logged in at (or "Offline"). The card keeps its
  proportions on every screen size.
- Cursus selector when the user has more than one cursus (for example the
  piscine and the main cursus): the card and the level panel follow the
  selected one.
- Level panel with the exact level and the percentage towards the next one.
- Details: email, phone (or "Hidden"), campus and pool.
- Refresh from the profile; back to the search with the top bar arrow or the
  system back gesture.
- One access token reused across requests and app restarts, renewed before it
  expires and again if the server rejects it.
- English and Spanish, following the system language.

## Repository layout

```
.env.example     template for the API credentials (copy to .env)
docs/            documentation
android/         the Android project: open this folder in Android Studio
```

## Requirements

- Android Studio 2026.1 or newer (Android Gradle Plugin 9.4.1, Gradle 9.7.1
  through the wrapper).
- Android SDK Platform 37 to compile. Android Studio offers to install it on the
  first sync.
- Gradle runs on JDK 25 (`android/gradle/gradle-daemon-jvm.properties`).
  Android Studio uses its bundled runtime, which is JDK 25; from a terminal,
  Gradle downloads a JDK 25 automatically the first time if none is installed.
- An emulator or a device running Android 8.0 (API 26) or newer.
- A 42 API application created at
  <https://profile.intra.42.fr/oauth/applications>. The redirect URI is not
  used; the app only uses the client credentials flow.

## Configuration

The API credentials are read from a `.env` file at the repository root (next
to `.env.example`, not inside `android/`), which is ignored by git:

```
cp .env.example .env
```

Fill in the application's UID and SECRET, without quotes:

```
INTRA_CLIENT_ID=u-s4t2ud-...
INTRA_CLIENT_SECRET=s-s4t2ud-...
```

The values are compiled into the build, so rebuild after changing them. If they
are missing the build still succeeds and the app explains what is wrong when a
search is made. Application secrets expire periodically; if searches fail with
"The API rejected the credentials", copy the current secret from the
application page.

## Running

1. Open the `android/` folder (not the repository root) in Android Studio and
   let Gradle sync.
2. Create a virtual device in Device Manager if there is none.
3. Select the `app` configuration and press Run.

From a terminal, starting at the repository root:

```
cd android
./gradlew installDebug          # build and install on the running emulator or device
./gradlew testDebugUnitTest     # unit tests
./gradlew lintDebug             # static checks
```

## Usage

1. Type a login and press Search or the keyboard's search key.
2. The profile opens if the login exists. The refresh icon reloads it.
3. Go back with the arrow in the top bar or the system back gesture.

Filter Logcat by the tag `Auth` to see when the access token is reused or
renewed. The token itself is never logged.

## How it works

- **Architecture**: MVVM. Compose screens observe a `StateFlow` exposed by a
  ViewModel; the ViewModel calls `UserRepository`, which uses Retrofit and
  OkHttp and maps the JSON into plain Kotlin models. Dependencies are wired by
  hand in `AppContainer`.
- **Search flow**: the search screen validates the login, fetches the profile
  and only then opens the profile screen, so the profile view never shows an
  unknown login and every search error appears next to the text field.
- **Authentication**: OAuth2 client credentials. `TokenManager` keeps the
  access token in memory and in the app's private preferences and reuses it
  until one minute before it expires. `AuthInterceptor` adds it to every
  request and renews it when it is about to expire; `TokenAuthenticator`
  renews it and replays the request once if the server answers 401. Renewal is
  synchronized, so concurrent requests never ask for two tokens.
- **Rate limit**: requests are spaced to stay under 2 per second, and a 429
  answer is retried after the delay the server asks for.
- **Errors**: every failure is mapped to a typed `AppError` with its own
  translated message.
- **Profile card**: drawn with Compose layouts and a `Canvas` for the level ring.
  Every size inside the card is a multiple of one unit (card width / 32), so the
  card scales as one piece; its text intentionally ignores the system font
  scale to keep the proportions, while the rest of the app follows it. Images
  are loaded with Coil, with an SVG decoder for coalition logos.

## Project structure

```
android/
  settings.gradle.kts, build.gradle.kts, gradle/    Gradle setup, wrapper and version catalog
  app/build.gradle.kts                              app module; reads ../.env into BuildConfig
  app/src/main/java/com/ravazque/swiftycompanion/
    SwiftyApp.kt, AppContainer.kt, MainActivity.kt  app entry point and dependency wiring
    model/                                          Profile and related models, AppError, login validation
    data/                                           UserRepository, JSON to model mapping
    data/auth/                                      token storage, renewal, interceptor, authenticator
    data/net/                                       Retrofit interface, JSON models, HTTP client, rate limit
    ui/                                             navigation, theme, search screen
    ui/profile/                                     profile screen, cursus selector, level panel, details
    ui/profile/card/                                profile card and level ring
  app/src/test/                                     unit tests and a local fake of the API
```

## Tests

`./gradlew testDebugUnitTest` runs the unit tests on the JVM. The network tests
use a local HTTP server that imitates the token and user endpoints, so they go
through the real OkHttp and Retrofit stack without touching the API. They
cover token reuse, reuse after a restart, renewal before expiry, renewal and
replay after a 401, rejected credentials, error mapping (404, 429, 5xx,
malformed JSON, no connection), JSON to model mapping, cursus selection and
login validation.
