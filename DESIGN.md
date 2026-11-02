# RadioMaster — dokumentacja projektu (UI/UX, Android Auto, i18N)

## 1. Cel i grupa docelowa

Aplikacja mobilna pozwalająca znaleźć dowolną stację radiową na świecie i słuchać jej na smartfonie oraz — przez Android Auto — z panelu samochodowego. Interfejs po polsku i po angielsku, z możliwością niezależnej zmiany języka aplikacji.

## 2. Założenia UX/UI

### 2.1 Zasady projektowe
- **Jedno palce, jedna akcja** — najważniejsze akcje (odtwarzanie, ulubione) accessible jednym kliknięciem.
- **Stan zawsze widoczny** — ekran szczegółów stacji pokazuje stan odtwarzacza (Łączenie… / Buforowanie… / Gotowe / Błąd); powiadomienie w tle pozwala sterować odtwarzaniem z ekranu blokady i zasłon powiadomień.
- **Materiały i kontrast** — Material 3, paleta marki (navy primary #0F3D7A, akcent amber #C98A1C), obsługa trybu ciemnego i systemowego, dynamiczne kolory (Material You) jako opcja.
- **Dostępność** — contentDescription na ikonach, duże cele klikowe (≥48 dp), kontrastowe teksty, obsługa zmiany rozmiaru czcionki systemowej.
- **Wielojęzyczność od pierwszego ekranu** — onboarding, wszystkie ekrany i ustawienia przetłumaczone; wybór języka nie zależy od ustawień systemu.

### 2.2 Ekrany
| Ekran | Opis |
|-------|------|
| Powitalny (Splash) | Logo, nazwa, wstępne sprawdzenie czy to pierwsze uruchomienie. |
| Onboarding | 4 slajdy: wyszukiwanie na świecie, odtwarzanie w tle, ulubione, Android Auto. |
| Strona główna | Stacje polecane (poziomo), kategorie (przewijany w poziom wiersz chipów), ostatnio odtwarzane. |
| Wyszukiwanie | Pole wyszukiwania z debounce (300 ms), wyniki listą, stan pusty i brak wyników. |
| Szczegóły stacji | Duże logo, nazwa, kraj, język, codec, bitrate, tagi, duży przycisk odtwarzania/pauzy, gwiazda ulubionych, udostępnianie, strona stacji w przeglądarce. |
| Ulubione | Lista stacji z usunięciem (gwiazda), odtwarzanie z poziomu listy. |
| Ostatnio odtwarzane | Lista ostatnio słuchanych stacji (max 50, Room). |
| Ustawienia | Język aplikacji (Polski/Angielski/System), motyw (Jasny/Ciemny/Systemowy), o aplikacji, źródło danych, Android Auto. |
| Wybór języka | Lista języków z zaznaczeniem bieżącego; zmiana natychmiastowa (recreate aktywności). |

### 2.3 Nawigacja
- Jedna aktywność (`MainActivity`) + Navigation Compose.
- Dolny pasek nawigacji: Strona główna / Szukaj / Ulubione / Ostatnie / Ustawienia.
- Szczegóły stacji otwierane jako pełnoekranowy tras (dolny pasek znika).
- Głęboki link trasy: `station/{stationId}`.

## 3. Odtwarzanie audio

- **Media3 ExoPlayer** jako singleton `RadioPlayerManager` (Hilt, `@Singleton`).
- Strumień radiowy uruchamiany w serwisie foreground (`PlayerForegroundService`, typ `mediaPlayback`) z powiadomieniem `MediaStyle` — odtwarzanie w tle i sterowanie z ekranu blokady.
- Stany odtwarzacza udostępniane jako `StateFlow<PlaybackState>` (IDLE/CONNECTING/BUFFERING/READY/ERROR) — ViewModele obserwują je i renderują odpowiednie komunikaty UI.
- Każde odtworzenie stacji zapisywane jako „ostatnio odtwarzane” (Room, lista ograniczona do 50 pozycji).

## 4. Android Auto i Android Automotive

- `RadioSessionService` (`@AndroidEntryPoint`, rozszerza `MediaSessionService`) z `MediaLibrarySession` i własnym `Callback`.
- Drzewo biblioteki dla samochodu: korzeń → [Szukaj, Polecane, Ulubione]; „Polecane” i „Ulubione” rozwijają się do listy stacji; wyszukiwarka obsługuje `onGetSearchResult`.
- Wybór stacji z panelu samochodowego wywołuje `RadioPlayerManager.play(station)` — ten sam odtwarzacz co w aplikacji mobilnej.
- Deklaracja w `AndroidManifest.xml` usługi medialnej z filtrem `android.media.browse.MediaBrowserService` oraz metadane `com.google.android.gms.car.application` → `res/xml/automotive_app_desc.xml` (`<uses name="media" />`).
- `minSdk 24`, usługa foreground z typem `mediaPlayback` (wymagane przy `targetSdk 35`).

## 5. Wielojęzyczność (i18n)

- Zasoby stringów: `values/` (polski, domyślny), `values-pl/`, `values-en/`.
- `LocaleContextWrapper` nadpisuje `Configuration.locale` na poziomie kontekstu aktywności (`AppLocaleActivity`), więc język aplikacji jest niezależny od systemu.
- Preferowany język przechowywany w DataStore (`AppSettingsRepository`), odczytywany już w `Application` (`App.currentLocale`) i stosowany przy tworzeniu kontekstu aktywności.
- Zmiana języka w Ustawieniach → `recreate()` aktywności → natychmiastowe przeładowanie napisów.

## 6. Architektura i warstwy

**Clean Architecture + MVVM:**

- `data.remote` — Retrofit (`RadioBrowserApi`) do `https://all.api.radio-browser.info/`, DTO (`StationDto`), mapery na modele domenowe.
- `data.local` — Room (`AppDatabase`, `FavoriteDao`, `RecentDao`), encje JSON (`station_json`), `LocalDataSource` (ulubione, ostatnie, limit 50).
- `data.repository` — implementacje repozytoriów łączące źródło zdalne i lokalne.
- `domain` — `Station`, `Category`, `AppTheme`; interfejsy `StationRepository`, `AppSettingsRepository`; case'y użycia (`ToggleFavoriteUseCase`, `SearchStationsUseCase`, `ChangeLanguageUseCase`, `AddRecentUseCase`, `LocaleHelper`).
- `di` — moduły Hilt: `NetworkModule` (OkHttp + Retrofit + Gson), `DatabaseModule` (Room), `DataStoreModule`, `AppModule` (repozytoria).
- `presentation` — Hilt ViewModel (`@HiltViewModel`), ekrany Compose, `RadioMasterTheme` (Material 3), `AppNavGraph` (Navigation Compose).
- `media` — `RadioPlayerManager`, `PlayerForegroundService`, `RadioSessionService`.

## 7. Zależności kluczowe

Compose BOM 2024.10.00, Material 3, Navigation Compose, Hilt (KSP), Media3 1.3.1, Room 2.6.1, DataStore 1.1.1, Retrofit 2.11.0, OkHttp, Coil 2.7.0, Timber, Kotlin 2.0.20, AGP 8.5.0, compileSdk 35, minSdk 24.

## 8. Uruchomienie i kompilacja

Wymagane: Android SDK 35, JDK 17 (Kotlin 2.0 / AGP 8.5), Android Studio lub Gradle wiersz poleceń:

```
./gradlew assembleDebug
```

Aplikacja nie wymaga klucza API (dane z publicznego radio-browser.info). W trybie release należy uzupełnić `proguard-rules.pro` i podpisanie aplikacji.

## 9. Kierunki dalszego rozwoju

- Buforowanie logo stacji i obrazków w Room/DataStore dla trybu offline (lista ulubionych).
- Rejestracja użytkownika i synchronizacja ulubionych w chmurze (Firebase).
- Historie audycji (EPG) i alarmy (budzik radiowy).
- Chromecast i Android TV (Leanback).
- Testy jednostkowe i instrumentowane (JUnit, Turbine dla Flow, Robolectric).
