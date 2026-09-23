# Swifty Companion

An Android app to look up a student profile through the intra API v2. Type a
login and the app shows a profile card with the photo, level, coalition and
contact details, the skills with their level and percentage, and every project
the user has taken, validated or failed.

Built with Kotlin and Jetpack Compose.

## Features

- Search by login, with clear messages for unknown logins, invalid input,
  missing connection and server errors.
- Profile card: photo inside a level ring, coalition color and logo, name,
  login, grade, title, level, wallet and correction points. Tap it to flip it
  and see the skill radar.
- Details: email, phone, current location, campus and pool.
- Skills of the selected cursus with level and percentage.
- Projects grouped by cursus, filterable by status (validated, failed, in
  progress).
- Layout that adapts to small phones, large phones, tablets and landscape.
- One access token reused across requests and renewed automatically when it
  expires or is rejected.
- English and Spanish, following the system language.

## Requirements

- Android Studio 2026.1 or newer (Android Gradle Plugin 9.4).
- Android SDK 36 and an emulator or a device running Android 8.0 (API 26) or
  newer.
- An API application created at
  <https://profile.intra.42.fr/oauth/applications> (any redirect URI works; the
  app only uses the client credentials flow).

## Configuration

The API credentials are read from a `.env` file at the repository root, which
is ignored by git:

```
cp .env.example .env
```

Fill in the application's UID and SECRET, without quotes:

```
INTRA_CLIENT_ID=u-s4t2ud-...
INTRA_CLIENT_SECRET=s-s4t2ud-...
```

The values are compiled into the debug build. Rebuild after changing them.

## Running

1. Open the repository folder in Android Studio and let Gradle sync.
2. Create a virtual device in Device Manager if there is none.
3. Select the `app` configuration and press Run.

From a terminal:

```
./gradlew installDebug          # build and install on the running emulator/device
./gradlew testDebugUnitTest     # unit tests
```

## Usage

1. Type a login and press Search (or the keyboard's search key).
2. The profile opens if the login exists. Tap the card to flip it, switch
   between the Skills and Projects tabs, and use the cursus chips when the user
   has more than one cursus.
3. Go back with the arrow in the top bar or the system back gesture.

## How it works

- **Architecture**: MVVM. Compose screens observe a `StateFlow` exposed by a
  ViewModel; the ViewModel calls a repository, which uses Retrofit and OkHttp.
  Dependencies are wired by hand in `AppContainer`.
- **Authentication**: OAuth2 client credentials. The access token is kept in
  memory and in the app's private preferences, and reused until shortly before
  it expires. An OkHttp interceptor adds it to every request and renews it when
  it is about to expire; an OkHttp authenticator renews it and replays the
  request once if the server answers 401.
- **Rate limit**: requests are spaced to stay under 2 per second, and a 429
  answer is retried after the time the server asks for.
- **Errors**: every failure is mapped to a typed error with its own message;
  retry is offered when it can help.
