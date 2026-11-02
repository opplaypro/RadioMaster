package com.radiomaster.app.ui.navigation

object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Search = "search"
    const val StationDetail = "station/{stationId}"
    const val Favorites = "favorites"
    const val Recent = "recent"
    const val Settings = "settings"
    const val Language = "language"

    val bottomNavRoutes = listOf(Home, Search, Favorites, Settings)
}
