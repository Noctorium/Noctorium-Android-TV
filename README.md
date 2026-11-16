# Noctorium for Android TV

Noctorium on a television: screens made for a remote and a room, for the
[Noctorium](https://github.com/Noctorium/Noctorium-Mobile) Android app.

> Noctorium is an independent third-party client. It is not affiliated with Google, YouTube, SoundCloud or Android TV.

## What the TV shows

Down the left, a list of pages, moved through with the remote's arrows:

- **Now playing** — the song large, with its cover, where it is, and play, pause, skip, shuffle and repeat. Picking a
  song anywhere brings you here.
- **Home** — the rows Noctorium's Home has: what you pinned, *Jump back in*, and the services' rows, songs and
  playlists alike. A playlist opens to its songs.
- **Search** — songs, typed with the TV's own keyboard.
- **Library** — *Recently played*, your Noctorium playlists, and your playlists on the services you are signed in to.
- **Queue** — the queue from the song playing on, then what autoplay has lined up. Picking one jumps there.
- **Radio** — the most listened-to stations in the country chosen in Noctorium.
- **Settings** — opens Noctorium's full screens for signing in to the services and its settings.

Back goes from an opened playlist to its page, and from any page to Home.

A phone or computer running Noctorium on the same network finds the TV under Connect, and can play on it and drive it.

## How it works

The TV runs the same Noctorium as a phone — the same services, sign-ins, queue and player — and only the screens differ.
So this is not an app of its own: it is an Android library that
[Noctorium-Mobile](https://github.com/Noctorium/Noctorium-Mobile) includes as `:tv`, and one APK installs on phones and
TVs alike. Android TV's home screen opens the phone app's `TvActivity`, which shows
[`NoctoriumTv`](tv/src/main/kotlin/app/noctorium/tv/NoctoriumTv.kt).

- [`TvMusic`](tv/src/main/kotlin/app/noctorium/tv/TvMusic.kt) is what the screens read and do, and `AppStateTvMusic`
  answers it from the app's own state: every play goes through the same calls the phone's screens make.
- [`TvRows`](tv/src/main/kotlin/app/noctorium/tv/TvRows.kt) turns Home, the library and the queue into rows of cards.
- The screens are Compose for TV: every card and button shows where the remote's focus is.
- The library's manifest says a TV may run the app: no touchscreen, camera or wifi required, and the leanback interface
  used where there is one.

## Building and testing

Noctorium-Base comes in as the `base/` submodule, or from a checkout beside this one as `../Noctorium-Base`.

```bash
git clone --recursive https://github.com/Noctorium/Noctorium-Android-TV.git
```

```bash
./gradlew :tv:testDebugUnitTest
```

The tests draw the screens with Robolectric at a TV's size and press them as the remote's OK does.

To try it on a TV, build Noctorium-Mobile with this beside it (`../Noctorium-Android-TV`) and install the APK on an
Android TV or Google TV device, or an Android TV emulator. Noctorium shows on the TV's home screen.

## Licence

See [LICENSE](LICENSE).
