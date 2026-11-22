# RadioMaster

Aplikacja na Androida (Jetpack Compose) do słuchania radia z całego świata: wyszukiwanie stacji w katalogu [radio-browser.info](https://www.radio-browser.info/), odtwarzanie w tle (Media3), ulubione i historia (Room), ekran globu z lokalizacjami nadajników, pełne **Android Auto** (drzewo przeglądania + wyszukiwanie głosowe) oraz **i18n PL/EN** z wyborem języka w aplikacji.

> **Stan weryfikacji.** Cały kod jest napisany pod aktualne wersje bibliotek (tabela poniżej), ale w tym środowisku **nie było JDK, Gradle ani Android SDK**, więc projekt **nie został skompilowany ani uruchomiony**. Spójność sprawdzano skryptem `tools/check_resources.py` (zasoby, importy równoległe, delegaty Compose) oraz przez weryfikację sygnatur Media3 na podstawie `api.txt` taga `1.11.1` i oficjalnej dokumentacji Androida. Pierwsze `./gradlew assembleDebug` nadal pozostaje do wykonania — dopiero ono zweryfikuje sygnatury kompilatorem.

## Funkcje

- **Glob (2D)** — canvas z equirektangularną mapą lądów; przesunięcie/zoom wyznacza okrąg (opisany na widoku), w którego promieniu katalog zwraca stacje, tapnięcie dobiera klaster kropek i otwiera ich listę. Dane mapy: `assets/world_land.json` (TopoJSON Natural Earth 110m, generowany przez `tools/generate_world_map.py`), bez uprawnień lokalizacji.
- **Start** — dwie poziome szyny (Popularne wg `clickcount`, Najwyżej ocenione wg `votes`) + pigułki krajów i gatunków prowadzące na listę stacji.
- **Wyszukiwanie** — trzy tryby pola (`nazwa` / `tag` / `kraj`), bo katalog nie ma jednego endpointa „wszystko”; debounce 450 ms.
- **Biblioteka** — ulubione i ostatnio słuchane, zapisywane w Room, obserwowane jako `Flow`.
- **Odtwarzanie w tle** — wspólny `ExoPlayer`, powiadomienie systemowe Media3, sterowanie z blokady, `WAKE_MODE_NETWORK`, audio focus, pauza przy `AUDIO_BECOMING_NOISY`.
- **Android Auto** — `MediaLibraryService` z drzewem `root → {ulubione, ostatnie, popularne, top voted, kraje, gatunki, losowe}` i `onSearch` dla wyszukiwania głosowego.
- **Ustawienia** — motyw (system/jasny/ciemny), język (system/EN/PL), instrukcja Auto, numery wersji.
- **Onboarding** — trzy ekrany + zgoda na powiadomienia po jego zakończeniu.

## Architektura

```
app/src/main/java/com/radiomaster/app/
  data/local/        Room: stations (cache), favorites, history
  data/remote/       Retrofit + OkHttp + Gson, klient radio-browser.info z fallbackiem na mirrory
  data/repository/   implementacje repozytoriów (sieć lustrzanie zapisywana do cache)
  di/AppGraph.kt     ręczny graf zależności (by lazy), bez frameworka DI
  domain/model/      Station, MapPosition, Genre, Country, BrowseIds
  domain/repository/ interfejsy: StationRepository, LocalRepository, SettingsRepository
  domain/usecase/    StationBrowser — jeden resolver list dla telefonu i dla Auto
  locale/AppLocale   per-app locales (API 33+) + createConfigurationContext (26–32)
  media/             RadioPlayback (ExoPlayer), MediaItems (id/metadane), RadioMediaSessionService
  presentation/      MainActivity, AppRoot (Scaffold + NavHost), screens/, components/, viewmodel/
  ui/globe/          dekodowanie world_land.json → Path
  ui/theme/          Material 3 (własne schematy, dark/light)
```

Warstwy są kierunkowe: `presentation → domain ← data`; `media` korzysta z `domain/usecase`, więc telefon i samochód czytają te same dane z tego samego kodu. Widoki nie znają Retrofitu ani Rooma.

### Kluczowe wersje (stan na 2026-11-21)

| Element | Wersja | Uwaga |
|---|---|---|
| Gradle | 9.9.2 | AGP 9.5 wymaga ≥ 9.6 |
| Android Gradle Plugin | 9.5.0 | latest stable |
| compileSdk / targetSdk | 37 | max API dla AGP 9.5 |
| minSdk | 26 | nasz wybór (media3 wymaga 23, automotive 28) |
| Kotlin | 2.4.21 | tylko jako `org.jetbrains.kotlin.plugin.compose`; `kotlin-android` nie jest stosowany (AGP 9 ma wbudowany Kotlin) |
| KSP | 2.3.14 | tylko Room |
| Compose BOM | 2026.11.01 | Compose 1.13.0, Material 3 1.5.0 |
| Activity Compose | 1.13.0 | |
| Lifecycle | 2.11.1 | `collectAsStateWithLifecycle` + `lifecycle-viewmodel-compose`; bez `repeatOnLifecycle` |
| Navigation Compose | 2.10.2 | |
| Room | 2.8.5 | |
| DataStore | 1.3.0 | preferencje ustawień |
| Media3 | 1.11.1 | `exoplayer`, `session`, `datasource` (bez `media3-ui`) |
| Retrofit / OkHttp / Gson | 3.0.0 / 5.5.1 / 2.15.0 | |
| kotlinx-coroutines | 1.11.0 | |

Brak Hilt, Coil i Timber — patrz „Decyzje” poniżej.

## Decyzje, które warto znać przy review

1. **Zero frameworków DI i obrazów.** `AppGraph` ma pięć obiektów; `AppGraph.radioPlayback` jest `by lazy`, więc serwis Auto i ekrany dostają **tę samą** instancję `ExoPlayera`. Aplikacja nic nie loguje (bez Timbera) — awarie są albo naprawiane (fallback na inny mirror), albo pokazywane w UI, a `runCatching` stoi tylko tam, gdzie porażka nie ma znaczenia dla słuchacza (raport kliknięcia, zapis historii). Loga stacji nie są wczytywane z sieci, bo nie ma biblioteki obrazowej; miejsce logo zajmuje kafelek z deterministycznym gradientem (`TilePalette`: sześć barw, indeks liczony z hashu uuid).