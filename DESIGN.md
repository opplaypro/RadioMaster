# RadioMaster — dokumentacja projektu (UI/UX, Android Auto, i18n)

Dokument opisuje **zaimplementowany** stan aplikacji. Wersje bibliotek i uzasadnienia decyzji są w `README.md`.

## 1. Cel i grupa docelowa

Aplikacja mobilna pozwalająca znaleźć dowolną stację radiową na świecie i słuchać jej na smartfonie oraz — przez Android Auto — z panelu samochodowego. Interfejs po angielsku i po polsku, z możliwością niezależnej zmiany języka w aplikacji. Odbiorca: słuchacz szukający stacji zagranicznych (emigracja, nauka języków, niszowe gatunki) oraz kierowca, który chce radia bez dotykania telefonu.

## 2. Założenia UX/UI

### 2.1 Zasady projektowe

- **Jedno tapnięcie = jedna akcja.** Klik wiersza stacji włącza ją, a jeśli właśnie gra — pauzuje; gwiazdka jest osobnym celem kliknięcia. Nie ma stanu „zaznaczone".
- **Stan zawsze widoczny.** Buforowanie = wskaźnik w wierszu i w pasku „teraz gram"; błąd = osobny komunikat z przyciskiem Ponów; brak prądu audio → pasek znika (`PlaybackUiState.isIdle`).
- **Long-press = szczegóły.** Długie przytrzymanie wiersza otwiera dialog stacji (lista → dialog, nie osobna trasa nawigacji).
- **Zero obrazów z sieci.** Brak biblioteki grafik; miejsce logo zajmuje kafelek z gradientem liczonym z hashu uuid (`tileColors`: barwa bazowa z `TilePalette` + ta sama barwa z `alpha = 0.55`), więc jest identyczny dla tej samej stacji w każdej sesji i w obu motywach.
- **Dostępność.** `contentDescription` na każdej ikonie, cele ≥ 48 dp, typografia z `MaterialTheme` (skaluje się z czcionką systemu), kontrastowe pary surface/onSurface.
- **i18n od pierwszego ekranu.** Onboarding i wszystkie ekrany przetłumaczone; język aplikacji niezależny od systemu.

### 2.2 Ekrany

| Ekran (plik) | Opis |
|---|---|
| `OnboardingScreen` | 3 strony (Glob / Odtwarzanie w tle i Auto / Ulubione i dwa języki) + kropki + CTA. Po CTA prosimy o uprawnienie powiadomień (API 33+). |
| `GlobeScreen` | Mapa 2D (rzut equirektangularny) rysowana na `Canvas` z poligonów `assets/world_land.json`; pinch + przesuwanie (zoom do 9×, translacja ograniczona do świata), jedno żądanie po zatrzymaniu gestu (380 ms) w promieniu okręgu opisanego na widoku (clamped 150–20 000 km) — bez uprawnień lokalizacji, stacje dobiera katalog (`nearby` → `centre`/`geo_distance`). Tapnięcie dobiera klaster (≤ 6 stacji, sortowanych po głosach) i pokazuje arkusz z listą. Przyciski: losowa stacja, reset widoku. Kropki: promienie 4.5 / 7 / 9 px (rysowana, nie zaznaczona, grająca — ta ostatnia z poświatą). |
| `HomeScreen` | Dwie szyny: Popularne (`clickcount`) i Najwyżej ocenione (`votes`); pod nimi pigułki krajów i gatunków → `StationListScreen`; stan błędu z `retryHome()`. |
| `SearchScreen` | Pole + przełącznik trybu `Nazwa / Tag / Kraj` (katalog nie ma jednego endpointa „wszystko"), debounce 450 ms + Enter, wiersze wyników. |
| `StationListScreen` | Jedna strona na `browseId`: kraj (`country/<nazwa>`), gatunek (`genre/<tag>`), ulubione, historia, popularne, top voted, random. Lista lokalna jest obserwowana (`collectAsStateWithLifecycle`), zdalna pobierana raz na identyfikator. |
| `LibraryScreen` | Dwie zakładki: Ulubione / Ostatnie; usuwanie gwiazdką, wyczyszczenie historii jednym przyciskiem, wiersz wyjaśniający, że dane są tylko lokalne. |
| `SettingsScreen` | Motyw (System/Jasny/Ciemny), język (System/English/Polski), instrukcja Android Auto, wersja + źródło danych. |
| `StationDetailDialog` | Nazwa, kafelek 52 dp, metadane (kraj, region, język, codec+bitrate, liczba głosów, homepage), tagi, przyciski: Odtwórz/Pauza, Ulubione, Strona www (brak strony = przycisk ukryty, nie wyłączony), Udostępnij. |
| `NowPlayingBar` | Pasek nad dolną nawigacją: kafelek, nazwa + status, play/pauza, stop, ponów. |

Brak osobnego splasha i osobnego ekranu języka: pierwszy rozstrzyga się jako `Spinner` w `AppRoot` dopóki `onboarded == null`, drugi mieszka w Ustawieniach.

### 2.3 Nawigacja

Jedna aktywność (`presentation.MainActivity`) + Navigation Compose. Pięć zakładek w `NavigationBar` (w tej kolejności): Glob, Start, Szukaj, Biblioteka, Ustawienia. Trasa `list?browseId={browseId}` (identyfikator kodowany `Uri.encode`, bo `country/Poland` zawiera `/`) obsługuje wszystkie strony kategorii. Widoki listy mają własny `ScreenHeader` ze strzałką wstecz — dolny pasek pozostaje, bo służy przełączaniu zakładek. Stan odtwarzania i ulubionych przeżywa zmianę zakładki, bo viewmodely są tworzone na właścicielu aktywności.

## 3. Odtwarzanie audio

- Współdzielony `ExoPlayer` w `media/RadioPlayback` (jedna instancja na proces, tworzona na wątku głównym w `RadioMasterApplication.onCreate`).
- `AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_MEDIA)` + `handleAudioFocus=true`, `setHandleAudioBecomingNoisy(true)`, `setWakeMode(C.WAKE_MODE_NETWORK)`.
- Stan odtwarzacza publikowany jako `StateFlow<PlaybackUiState>` (`isPlaying`, `isBuffering`, `hasError`) z `Player.Listener`; mutacje wyłącznie przez `update {}`/przypisanie na głównym.
- Powiadomienie i sterowanie z blokady zapewnia Media3 `MediaSessionService` (nie własny foreground service) — `foregroundServiceType="mediaPlayback"`.
- Każde odtworzenie: `recordPlay` do Room (historia, limit 200) oraz `GET /json/url/{uuid}` (licznik kliknięć katalogu). Oba w `runCatching`, żeby nie blokować pierwszej ramki audio.
- Błąd strumienia nie jest tłumaczony użytkownikowi tekstowo (`PlaybackException` jest deweloperski i nieprzetłumaczony) — UI pokazuje `player_stream_error` + `common_retry`.

## 4. Android Auto i Android Automotive

- `media/RadioMediaSessionService : MediaLibraryService` (deklaracja w manifeście: eksportowana, `foregroundServiceType="mediaPlayback"`, akcje `androidx.media3.session.MediaLibraryService` **oraz** `android.media.browse.MediaBrowserService`). Brak `MediaButtonReceiver` — w Media3 1.11.1 ta klasa nie istnieje, przyciski obsługuje sesja.
- `MediaLibrarySession.Builder(this, radioPlayback.player, callback)` — ten sam odtwarzacz co w telefonie; `setSessionActivity(PendingIntent → MainActivity)`.
- `onGetSession` **zawsze** zwraca sesję (odrzucony klient = brak źródła w aucie; legacy clients i media-button start też muszą dostać sesję).
- Drzewo: `collection:root` → ulubione, ostatnie, popularne, top voted, kraje, gatunki, losowe. `country/<nazwa>` i `genre/<tag>` rozwijają się do stacji. Metadane folderów: `MEDIA_TYPE_FOLDER_RADIO_STATIONS` + `setIsBrowsable(true)`, stacje: `MEDIA_TYPE_RADIO_STATION` + `setIsPlayable(true)`.
- Stronicowanie: `onGetChildren(pageIndex, pageSize)` przeliczane na `offset = pageIndex * pageSize`; brak pozycji w korzeniu to `RESULT_ERROR_BAD_VALUE`, `CancellationException` nie jest „błędem IO".
- Wyszukiwanie głosowe: `onSearch` + `notifySearchResultChanged` + `onGetSearchResult` (limit 40 pozycji na sesję).
- Identyfikatory mediów: `station:<uuid>` i `collection:<browseId>`; `onAddMediaItems` tłumaczy identyfikator na `MediaItem` z cache Room, więc „włącz ostatnio słuchaną" działa bez sieci.
- Zmiany ulubionej/historii z telefonu **nie** emitują `notifyChildrenChanged`, więc otwarta w aucie lista ulubionych odświeża się przy ponownym wejściu do węzła (listy są krótkie, a Auto i tak odpytuje rodzica po powrocie).
- Ikona źródła i atrybucji: `android:icon` (launcher) + `androidx.car.app.TintableAttributionIcon` → `@drawable/ic_radio`, wektor monochrome; `res/xml/automotive_app_desc.xml` z `<uses name="media"/>`. Żadne uprawnienie `CAR_DIALER`/`MEDIA_CONTENT_CONTROL` nie jest potrzebne.

## 5. Wielojęzyczność (i18n)

- `values/` = angielski (bazowy, 82 klucze), `values-pl/` = polski (te same 82 klucze, pilnowane przez `tools/check_resources.py`).
- `res/xml/locales_config.xml` (en, pl) + `android:localeConfig` w manifeście: to chroni split `values-pl` przed wycięciem przez Play i dodaje pozycję „Język aplikacji" w ustawieniach systemowych.
- API 33+: zapis przez `LocaleManager.setApplicationLocales(LocaleList)` (system sam odtwarza język i robi recreate). API 26–32: `AppLocale.wrap(base)` w `attachBaseContext` → `createConfigurationContext`.
- Lustro wyboru w `SharedPreferences` (`AppLocale.PREFS`), ponieważ `attachBaseContext` nie może czekać na DataStore; DataStore (`app_language`) pozostaje źródłem dla UI ustawień.
- Ustawienia: po zapisie `recreate()` jest wymagane tylko < API 33; na 33+ zajmuje się tym framework.

## 6. Architektura i warstwy

Clean Architecture + MVVM, jeden moduł `app`, zależności skierowane do `domain`.

- `data/remote` — `RadioBrowserApi` (Retrofit, Gson), `RadioBrowserClient` (mirrory + fallback przy `IOException`, TTL 3 h dla listy krajów/gatunków), `StationDto.toDomain()` filtruje rekordy bez `stationuuid`/`url`.
- `data/local` — Room: `stations` (cache metadanych, PK = uuid), `favorites`, `history` (+ licznik odtworzeń); DAO z `Flow` łączonymi z tabelą `stations`, bez `INSERT … ON CONFLICT` (SQLite 3.19 na API 26).
- `data/repository` — `StationRepositoryImpl` (sieć + zapis do cache w `runCatching`), `LocalRepositoryImpl`, `SettingsRepositoryImpl` (DataStore).
- `domain` — modele (`Station`, `Genre`, `Country`, `MapPosition`, `BrowseIds`, `BrowseCategory`), interfejsy repozytoriów, `StationBrowser` jako jeden resolver zapytań „co jest w tym browseId" dla telefonu i samochodu.
- `di/AppGraph` — pięć singletonów w `by lazy`; bez Hilta (patrz README → Decyzje).
- `presentation` — `MainActivity` (ComponentActivity, bez AppCompat), `AppRoot` (Scaffold + NavHost + pasek), viewmodely w `RadioViewModels.kt` z `RadioViewModelFactory`, komponenty w `components/`.
- `ui/globe/WorldBackdrop` — dekodowanie `assets/world_land.json` → `Path` (raz, cache na proces).

## 7. Zależności kluczowe

W `gradle/libs.versions.toml` (pełna tabela z uzasadnieniem w `README.md`): AGP 9.5.0, Kotlin 2.4.21 (plugin compose), KSP 2.3.14, Compose BOM 2026.11.01, Activity 1.13.0, Core 1.19.0, Lifecycle 2.11.1, Navigation 2.10.2, Room 2.8.5, DataStore 1.3.0, Media3 1.11.1 (`exoplayer`, `session`, `datasource`), Retrofit 3.0.0 + converter-gson, OkHttp 5.5.1, Gson 2.15.0, kotlinx-coroutines 1.11.0. compileSdk/targetSdk 37, minSdk 26, JDK 17. Brak Hilt, Coil, Timber i `media3-ui`.

## 8. Uruchomienie i kompilacja

```bash
python3 tools/check_resources.py   # spójność zasobów, bez Android SDK
./gradlew assembleDebug            # wymaga JDK 17 + platformy 37
./gradlew installDebug
```

### Co sprawdzono bez kompilatora

Sygnatury Media3 porównano z wyeksportowanym plikiem API wydania 1.11.1 (`// Signature format: 5.0`). Konsekwencje widoczne w kodzie:

- `MediaSession.ControllerInfo` nie ma `connectionId` (są `packageName`, `uid`, `controllerVersion`, `connectionHints`) — dlatego wyniki wyszukiwania są trzymane w mapie **po zapytaniu**, a nie per połączenie;
- `MediaSessionService.onUpdateNotification` jest `@Deprecated` — nie nadpisujemy go; klasy `MediaButtonReceiver` w 1.11.1 nie ma;
- `MediaItem.Builder` nie ma `setTitle`, a `MediaMetadata.Builder.setFolderType` jest zastąpione przez `setMediaType(...)`;
- `LibraryResult.ofItemList(List, LibraryParams?)` zwraca `LibraryResult<ImmutableList<MediaItem>>` — stąd `com.google.common.collect.ImmutableList` w sygnaturach `onGetChildren`/`onGetSearchResult` (Guava chodzi z `media3-session`, bez osobnego wpisu w gradle).

Wymagane jest nadal: `./gradlew assembleDebug` (kompilator + Lint), test jednostkowy na `BrowseIds`/`MapView.radiusKm` oraz uruchomienie na emulatorze Auto.

Release: `isMinifyEnabled=false`, więc `proguard-rules.pro` opisuje tylko reguły na wypadek włączenia R8 (Gson + `StationDto`) i nie wymaga dalszych zmian. Podpisanie należy skonfigurować lokalnie.

## 9. Kierunki dalszego rozwoju

- Testy: JUnit + Turbine dla `BrowseViewModel`/`RadioPlayback`, Robolectric dla `AppLocale.resolve`, Paparazzi dla `StationRow`.
- Cache favicon (dołączenie `Coil` albo ręczne `ImageDecoder` + Room BLOB) i zapis logo do cache.
- Budzik radiowy (AlarmManager + `playById`), Leanback/Android TV, Chromecast.
- Vote/unvote w UI (endpoint 10 min/IP wymaga throttlingu), filory językowe i regionalne (`iso_3166_2`).
- Eksport/import ulubionych (plik JSON) i ewentualna synchronizacja w chmurze.
