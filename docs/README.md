# Swifty Companion

Swifty Companion is an Android app, written in Kotlin with Jetpack Compose,
that looks up the profile of a 42 student through the 42 API v2. A login typed
on the first screen opens the second one: photo, level and progress, login,
grade and title, workstation, wallet, correction points, coalition and score,
the skills with their level and percentage, and every project of the selected
cursus, failed ones included. The app is in English and Spanish.

## How it works

- **Two screens.** The search screen validates the login and fetches the
  profile before opening the profile screen, so every error (empty or
  malformed login, unknown login, hidden profile, no network, rate limit,
  server error) appears under the search field, with a retry button when
  retrying can help. The top bar arrow and the system back gesture return to
  the search.
- **Profile.** The card shows the identity and the numbers on its front and a
  radar chart of the skills on its back, drawn on a `Canvas`; a tap on a point
  or a skill name shows its level and percentage. A selector switches between
  the main cursus and the piscine, and the projects can be filtered by status
  and sorted by date, grade or name.
- **Layout.** One scrolling column below 600 dp; from 600 dp (tablets, phones
  in landscape) the card and details on the left and the projects on the right.
  The selected cursus, the card side and the search text survive rotations and
  the app being closed in the background.
- **Token.** Searches use the application's own token (OAuth2 client
  credentials), requested once, kept in private preferences and reused across
  requests and restarts. The same token is required by the intra CDN for
  profile photos, so the image loader adds it to those requests only.
- **Sign-in.** The first search asks to accept the terms of use and the
  privacy policy and to sign in with a 42 account (OAuth2 authorization code
  with PKCE, in a Custom Tab); only the login is kept.
- **Profile kinds.** The API has no single field for this, so the app decides
  from the user and the main cursus: active students and alumni are shown;
  people who have not started the cursus are not; staff, blackholed and frozen
  profiles appear only with their chip under the search button turned on.
- **Architecture.** MVVM: Compose screens observe a `StateFlow` of their
  ViewModel, which calls `UserRepository` (Retrofit, OkHttp,
  kotlinx.serialization); dependencies are wired by hand in `AppContainer`.
  Requests are spaced to stay under 2 per second, and a `429` is retried after
  the delay the server asks for.

## Dependencies

| Dependency | Version | Debian / Ubuntu | Arch |
|---|---|---|---|
| A JDK, to start Gradle | 17 or newer | `openjdk-17-jdk` | `jdk17-openjdk` |
| Android SDK with Platform 37 | Android Studio 2026.1 or newer, or the command-line tools | https://developer.android.com/studio | same |
| GNU Make | any | `make` | `make` |
| An emulator or a device | Android 8.0 (API 26) or newer | Device Manager in Android Studio, or a phone with USB debugging | same |
| A 42 API application | its UID and SECRET | https://profile.intra.42.fr/oauth/applications | same |

The Gradle wrapper downloads Gradle 9.7.1, the Android Gradle Plugin 9.4.1 and
every library; the build runs on JDK 25, which Gradle downloads the first time
if none is installed. `make install` checks the JDK and the SDK (`ANDROID_HOME`,
or `~/Android/Sdk`, written to `android/local.properties`).

## Configuration

The credentials are read from `.env` at the repository root, next to
`.env.example` and ignored by git, and compiled into the build:

```
INTRA_CLIENT_ID=u-s4t2ud-...
INTRA_CLIENT_SECRET=s-s4t2ud-...
```

The application page on the intra must list the redirect URI
`com.ravazque.swiftycompanion://oauth`, where the sign-in returns. Without
credentials the build still succeeds and the app explains what is missing when
a search is made; an expired secret shows "The API rejected the credentials".

## Build and run

```
make           # the list of targets
make install   # checks the JDK, the SDK and .env; builds nothing
make apk       # builds the debug APK into android/app/build/outputs/apk/debug/
make run       # builds, installs the debug build on the connected device and opens it
make test      # 79 unit tests on the JVM
make lint      # static checks
make clean     # Gradle's build output
make fclean    # clean, and Gradle's project caches
```

`make install` only prepares the machine, `make apk` produces the APK file,
and `make run` puts the app on a device through Gradle's `installDebug`, which
is an install on the phone, not on the computer. The `android/` folder also
opens in Android Studio, where the `app` configuration runs on a virtual
device. The unit tests run against a local fake of the API (OkHttp's MockWebServer), so they need no network: token
reuse and renewal, error mapping, profile kinds, project status and order,
the sign-in flow, state restoration, and the token on profile photos only.

## Bonus

**Token renewal at expiry.** The token is renewed one minute before it
expires, and renewed again with the request replayed once if the server
answers `401`; renewal is synchronized, so concurrent requests never ask for
two tokens. Debug builds show a token inspector (lock icon) whose buttons
expire or corrupt the stored token to exercise both paths.

## Layout

```
swifty-companion/
├── Makefile                                              # install, apk, run, test, lint, clean
├── .env.example                                          # template for the API credentials
└── android/                                              # the Android project
    ├── app/build.gradle.kts                              # app module; reads ../.env into BuildConfig
    ├── app/src/main/java/com/ravazque/swiftycompanion/
    │   ├── model/                                        # Profile, profile kinds, errors, login validation
    │   ├── data/                                         # UserRepository and the JSON to model mapping
    │   ├── data/auth/                                    # app token, sign-in session, CDN token
    │   ├── data/net/                                     # Retrofit interface, JSON models, HTTP clients, rate limit
    │   └── ui/                                           # search, profile, card, legal texts, token inspector
    └── app/src/test/                                     # unit tests and the local fake of the API
```
