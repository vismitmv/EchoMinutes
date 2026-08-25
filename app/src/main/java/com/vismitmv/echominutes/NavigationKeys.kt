package com.vismitmv.echominutes

import kotlinx.serialization.Serializable

// Routes are plain data objects/classes — no interface needed for Navigation 3
data object RecordRoute
data object HistoryRoute
data object SettingsRoute
data class ResultRoute(val meetingId: Long)
