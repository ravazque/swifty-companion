# Swifty Companion

An Android app to look up the profiles of active 42 students through the 42
API v2. Type a login and the app shows the profile: photo, level, location,
wallet, correction points, skills, projects and more.

Built with Kotlin and Jetpack Compose.

## Features

- Search by login. Invalid input, unknown logins, missing connection, rate
  limiting and server errors each get their own message, with a retry button
  when retrying can help.
- Active students are shown; alumni too, with an "Alumni" tag. People who
  have not started the main cursus yet get a message under the search field
  instead of a profile. So do staff and blackholed students, unless the Staff
  or Blackholed chip under the search button is on (both are off by default
  and kept across restarts). See "Profile kinds".
- English and Spanish: the EN | ES switch at the top of the search screen
  changes the language inside the app. On Android 13 and newer the choice is
  the same as the app language in the system settings.
- Profile card: coalition logo and name, level tag, photo inside a ring that
  fills with the progress towards the next level, full name, login, grade,
  selected title, the workstation the user is logged in at (or "Offline")
  centered under the name, level, wallet and correction points. The card keeps
  its proportions on every screen size, and its layout does not move when the
  cursus or the location changes.
- Tapping the card flips it to the back, a radar chart of the skills in the
  selected cursus on the intra's 0-21 scale. The main cursus always shows its
  20 skill axes and the piscine its 6, with the untouched ones at zero.
  Tapping a point or a skill name opens a bubble with the skill's level and
  percentage; tapping elsewhere closes it, and a second tap flips the card
  back.
- Cursus selector when the user has more than one cursus. Everything follows
  the selected one: card, radar, level panel, details and projects. The
  piscine is shown in a neutral blue, without the coalition; so is a profile
  without a coalition.
- Coalitions: only the four of the main cursus count (Zefiria, Marventis,
  Ignisaria and Tiamant, in that order if a user has more than one). Others,
  like the piscine coalitions, are ignored; a profile with none of the four
  shows no coalition and uses the neutral blue.
- Level panel with the exact level, "level in the Cursus" or "level in the
  Piscine", and a bar with the progress towards the next level.
- Details: the pool month and, outside the piscine, the kickoff date and, for
  a Transcender, the alumni deadline (see "Alumni deadline").
- Projects of the selected cursus, including the failed ones. Each one shows
  its mark, its status in color (passed, failed, in progress, waiting for
  correction, searching group, creating group) and the date it was graded. Two
  menus filter by status, with how many projects have each one, and sort by
  date, grade or name.
- Adaptive layout: one scrolling column on phones in portrait; from 600 dp
  wide (tablets, phones in landscape, unfolded foldables) the card, level and
  details sit on the left and the projects on the right, each pane with its
  own scroll. The top bar scrolls away with the content and comes back on the
  first scroll up, which matters on short screens. Content stays clear of the
  system bars and display cutouts.
- The selected cursus, project filter and order, the side of the card and the
  text typed in the search are kept when the screen rotates, when the layout
  changes and when Android closes the app in the background.
- Refresh from the profile; back to the search with the top bar arrow or the
  system back gesture.
- One access token reused across requests and app restarts, renewed before it
  expires and again if the server rejects it.
- Token inspector in debug builds (lock icon at the top of both screens): token
  fingerprint, time left, how many API requests and token requests the app has
  made, the last renewal and its cause, and two buttons that expire or corrupt
  the stored token to exercise both renewal paths.

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

1. Type a login and press Search or the keyboard's search key. EN | ES at
   the top left switches the language; the Staff and Blackholed chips under
   the button also let those profiles through.
2. The profile opens if the login exists and belongs to an active student or
   an alumnus. The refresh icon reloads it.
3. Tap the card to see the skills radar on its back. Tap a point or a skill
   name to see its level and percentage; tap elsewhere to close it, and again
   to turn the card over. The chips above the card choose the cursus.
4. Scroll down to the projects (on wide screens they are on the right). The
   Status menu filters them and Sort by orders them.
5. Go back with the arrow in the top bar or the system back gesture.

### Profile kinds

The API has no single field that says whether someone is a student, so the
app decides from the user and their main cursus (`42cursus`), in this order:

| Kind | Rule | Shown |
|------|------|-------|
| Staff | `staff?` is true | With the Staff option, tagged "Staff" |
| Alumni | `alumni?` is true or the main cursus grade is "Alumni" | Yes, with an "Alumni" tag |
| Not started (pisciner) | no main cursus, or its kickoff (`begin_at`) is still to come | No |
| Blackholed | the main cursus has ended (`end_at`) or its black hole date (`blackholed_at`) has passed | With the Blackholed option, tagged "Blackholed" |
| Transcender | main cursus grade "Transcender" | Yes |
| Student | anyone else in the main cursus | Yes |

The two options are stored in the app's private preferences and read on every
search, so they also apply when a profile is refreshed.

### Alumni deadline

A Transcender (not an alumnus) has eight months from the last time they gained
experience to become alumni. The app takes the date the latest passed project
of the main cursus was graded, adds eight months and shows that date in the
details, as "Alumni". While a "Work Experience I" or "Work
Experience II" project is open (not passed or failed), the count is paused
and the row says "PAUSED".

Filter Logcat by the tag `SwiftyAuth` to see when the access token is reused
or renewed. The token itself is never logged.

### Token inspector (debug builds)

The lock icon at the top of both screens opens the token inspector:

- **Token**: the first and last four characters of the access token.
- **Expires in**: time left before the token expires.
- **API requests** and **Token requests**: counted since the app started. Many
  API requests share one token request; after a restart the stored token is
  reused, so token requests can stay at zero.
- **Last renewal**: when the token was last requested and why: before expiry,
  or after the server answered 401.
- **Expire now** marks the stored token as expired: the next search or refresh
  asks for a token before sending the request.
- **Corrupt token** replaces the stored token with an invalid one: the next
  request is rejected with a 401, the app asks for a token and replays the
  request, and the profile still loads.

While the old token is still valid on the server, the API answers a token
request with that same token and its remaining lifetime, so the fingerprint
does not change after these actions.

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
  translated message. Token request failures are raised as `IOException`s,
  the only kind OkHttp reports from inside an interceptor, so not even an
  unreadable token answer can crash the app.
- **Profile card**: drawn with Compose layouts and a `Canvas` for the level ring.
  Every size inside the card is a multiple of one unit (card width / 32), so the
  card scales as one piece; its text intentionally ignores the system font
  scale to keep the proportions, while the rest of the app follows it. Images
  are loaded with Coil, with an SVG decoder for coalition logos.
- **Flip**: the card rotates around its vertical axis in a graphics layer; past
  90 degrees the back is shown with a half turn of its own so it does not read
  mirrored. The visible side is saved, so it survives rotation and refresh.
- **Skills radar**: drawn by hand on a `Canvas` with a `TextMeasurer`. The
  radius is the largest one that keeps every label inside the card, and labels
  that would overlap their neighbors are moved apart. The same geometry
  answers taps: a tap on a name, or close to a point, selects that skill and
  the bubble is placed above the point (below if there is no room), always
  inside the chart. The piscine chart is a hexagon with Unix at the upper left
  and the other skills clockwise.
- **Wallet sign**: the ₳ is drawn as a vector, since many phone fonts have no
  glyph for it.
- **Project status**: an attempt that is waiting for correction, searching a
  group or creating a group keeps that status. Otherwise the latest grade
  decides: validated is passed; a failed grade, or finished without
  validation, is failed; without a grade it is in progress. Some attempts keep
  an open status after being graded (piscine exams stay "in_progress" with
  their mark), which is why the grade comes first for them.
- **Hidden profiles**: the repository classifies the profile right after the
  user request and stops there if it is hidden, so a hidden profile costs one
  request. Visible ones also fetch the coalitions and keep the main cursus one.
- **Language**: on Android 13 and newer the switch sets the app language
  through the system's `LocaleManager`; on older versions the choice is kept
  in preferences and applied when the activity starts. Every language ships
  in the base APK (language splits are off), since the app changes it at
  runtime.
- **Layout**: `ProfileScreen` measures the space it gets with
  `BoxWithConstraints`. Below 600 dp it shows one `LazyColumn`; from 600 dp
  it shows two panes. The left pane is as wide as it can be while the whole
  card still fits in its height, kept between 300 and 440 dp so the card's
  text stays readable on short screens. That height is measured as if the top
  bar were fully shown, so the card keeps its size while the bar scrolls away.
  Both layouts share the same composables.
- **Screen state**: the selected cursus, project filter and order, and card side
  live in the profile ViewModel and in its `SavedStateHandle`, which Android
  restores after closing the app in the background. Since they do not live in
  the composables, switching between the one and two pane layouts keeps them.

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
    ui/                                             navigation, theme, search screen, language switch
    ui/debug/                                       token inspector (debug builds only)
    ui/profile/                                     profile screen, cursus selector, level panel, details, projects
    ui/profile/card/                                profile card (front, back, flip), level ring, skills radar, wallet sign
  app/src/test/                                     unit tests and a local fake of the API
```

## Tests

`./gradlew testDebugUnitTest` runs the unit tests on the JVM. The network tests
use a local HTTP server that imitates the token and user endpoints, so they go
through the real OkHttp and Retrofit stack without touching the API. They
cover token reuse, reuse after a restart, renewal before expiry, renewal and
replay after a 401, rejected credentials, an unreadable token answer, both
inspector actions, error mapping (404, 429, 5xx, malformed JSON, no
connection), hidden profiles (not started, and staff and blackholed with every
combination of the search options) without extra requests, which coalition is
kept, the
alumni deadline (date, pause by an open work experience, who has one), JSON to model
mapping (including project status and order), the profile kind rules, cursus
selection, the axes of the main and piscine charts, the projects of each
cursus with their filter and sort orders, login validation, and restoring the
search text, the selected cursus, project filter and order, and card side
after Android kills the app process, and keeping the search options for the
next launch.
