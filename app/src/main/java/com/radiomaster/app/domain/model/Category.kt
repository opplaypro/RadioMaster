package com.radiomaster.app.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconRes: Int = com.radiomaster.app.R.drawable.ic_globe,
    val stationCount: Int = 0
)
