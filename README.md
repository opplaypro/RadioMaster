# RadioMaster

Aplikacja mobilna na Androida (Jetpack Compose) do wyszukiwania i słuchania stacji radiowych z całego świata, z obsługą **Android Auto** i pełną **wielojęzycznością** (Polski / English, język wybieralny niezależnie od systemu).

> **Uwaga:** repozytorium zawiera kompletny kod źródłowy oraz dokumentację projektu. Skompilowanie wymaga zainstalowanego Android SDK (Android Studio / wiersz poleceń) — w środowisku nie było dostępnego JDK ani Android SDK, dlatego kompilacja nie została uruchomiona.

## Funkcje

- **Wyszukiwanie stacji z całego świata** — po nazwie, kraju, języku lub gatunku (dane z darmowego API [radio-browser.info](https://www.radio-browser.info/)).
- **Odtwarzanie strumieni radiowe** w tle (Media3 ExoPlayer), z powiadomieniem i sterowaniem z ekranu blokady.
- **Ulubione stacje** i lista **ostatnio odtwarzanych** (baza lokalna Room).
- **Ekran szczegółów stacji** — informacje o nadawcy (kraj, język, codec, bitrate, tagi), odtwarzanie/pauza, udostępnianie, otwarcie strony stacji.
- **Android Auto** — przeglądarka multimediów (MediaSessionService + MediaLibrarySession): popularne stacje, ulubione, wyszukiwarka i odtwarzanie z panelu samochodowego.
- **Wielojęzyczność (i18n)** — zasoby stringów w języku polskim (domyślny) i angielskim; zmienny język aplikacji bez restartu urządzenia (z recreate aktywności).
- **Motywy** — jasny, ciemny i systemowy (Material 3, kolory marki).
- **Onboarding** i ekran powitalny.

## Architektura

```
Clean Architecture + MVVM (jednoaktywnościowy, Navigation Compose)

data   -> remote (Retrofit + radio-browser.info), lokalne (Room: ulubione/ostatnie),
         DataStore (ustawienia: język, motyw)
domain -> modele domenowe, interfejsy repozytoriów, case'y użycia
presentation -> Jetpack Compose (Material 3), Hilt (DI), ViewModel, stany UI
media   -> ExoPlayer (RadioPlayerManager), serwis foreground (powiadomienie odtwarzania),
         MediaSessionService + MediaLibrarySession (Android Auto / Automotive)
```

### Biblioteki kluczowe
- Jetpack Compose + Material 3, Navigation Compose, Hilt (KSP)
- Media3 (ExoPlayer, Session)
- Room, DataStore Preferences
- Retrofit + Gson + OkHttp
- Coil (ładowanie obrazów/logo stacji)
- Timber (logi)

## Android Auto / Android Automotive

Aplikacja deklaruje usługę `RadioSessionService` (medialna przeglądarka, filtr `android.media.browse.MediaBrowserService`) oraz metadanę `com.google.android.gms.car.application` wskazującą `res/xml/automotive_app_desc.xml` (`<uses name="media" />`). W aucie dostępne są: stacje polecane, ulubione i wyszukiwarka, a wybór stacji uruchamia odtwarzanie we wspólnym odtwarzaczu aplikacji.

## Wielojęzyczność

- `res/values/strings.xml` — polski (domyślny)
- `res/values-pl/strings.xml` — polski
- `res/values-en/strings.xml` — angielski

Język aplikacji jest nadpisywany na poziomie kontekstu (`LocaleContextWrapper`) i zapisywany w DataStore; zmiana języka w Ustawieniach powoduje `recreate()` aktywności, więc nowe napisy pojawiają się natychmiast.

## Jak uruchomić

1. Zainstaluj Android Studio (lub Android SDK wiersza poleceń) oraz pakiet SDK 35 (`compileSdk = 35`, `minSdk = 24`).
2. Otwórz katalog `/home/user/RadioMaster` w Android Studio jako projekt Gradle.
3. Zsynchronizuj Gradle i uruchom moduł `app` na emulatorze lub urządzeniu z API 24+.

Brak kluczy API — aplikacja korzysta z publicznego API radio-browser.info (brak wymogu rejestracji, honoruj ich warunki użytkowania).

## Struktura katalogów (wybrane)

```
app/src/main/java/com/radiomaster/app/
  data/           # warstwa danych (remote, local, repository)
  domain/         # modele, interfejsy repozytoriów, case'y użycia
  di/             # moduły Hilt (DI)
  media/          # odtwarzanie (ExoPlayer), serwis foreground, Android Auto
  presentation/   # App (Hilt Application), MainActivity
  ui/
    component/    # StationCard, SearchBar, CategoryChip
    navigation/   # trasy, AppNavGraph, dolny pasek nawigacji
    screens/      # Splash, Onboarding, Home, Search, Szczegóły, Ulubione, Ostatnie, Ustawienia, Język
    theme/        # Motyw Material 3, kolory, typografia
```

## Licencje i źródła

- Dane o stacjach: [radio-browser.info](https://www.radio-browser.info/) (własność ich właścicieli).
- Biblioteki open source na licencjach Apache 2.0 / MIT / BSD (szczegóły po kompilacji w sekcji licencji Androida).
