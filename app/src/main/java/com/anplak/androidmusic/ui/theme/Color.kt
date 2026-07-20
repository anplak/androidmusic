package com.anplak.androidmusic.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Dark (first-class) — graphite + phosphor
val Ink = Color(0xFF0E1114)
val Graphite = Color(0xFF161A1F)
val GraphiteElevated = Color(0xFF1E242B)
val GraphiteHigh = Color(0xFF252C35)
val Mist = Color(0xFFE6E9ED)
val MistDim = Color(0xFF9AA3AD)
val Phosphor = Color(0xFF3DDC97)
val PhosphorDim = Color(0xFF1F3D32)
val PhosphorOn = Color(0xFF06140F)
val OutlineMute = Color(0xFF2C333C)
val Danger = Color(0xFFFF6B6B)

// Light twin — same accent, paper neutrals
val Paper = Color(0xFFF4F6F8)
val PaperElevated = Color(0xFFE8ECF0)
val PaperInk = Color(0xFF12161A)
val PaperInkDim = Color(0xFF5C6570)
val OutlineLight = Color(0xFFC5CCD4)

val DarkMusicColorScheme = darkColorScheme(
    primary = Phosphor,
    onPrimary = PhosphorOn,
    primaryContainer = PhosphorDim,
    onPrimaryContainer = Mist,
    secondary = MistDim,
    onSecondary = Ink,
    secondaryContainer = GraphiteElevated,
    onSecondaryContainer = Mist,
    tertiary = Phosphor,
    onTertiary = PhosphorOn,
    tertiaryContainer = PhosphorDim,
    onTertiaryContainer = Mist,
    background = Ink,
    onBackground = Mist,
    surface = Graphite,
    onSurface = Mist,
    surfaceVariant = GraphiteElevated,
    onSurfaceVariant = MistDim,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Graphite,
    surfaceContainer = GraphiteElevated,
    surfaceContainerHigh = GraphiteHigh,
    surfaceContainerHighest = GraphiteHigh,
    error = Danger,
    onError = Ink,
    outline = OutlineMute,
    outlineVariant = OutlineMute
)

val LightMusicColorScheme = lightColorScheme(
    primary = Color(0xFF0F8F5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0F5E4),
    onPrimaryContainer = PaperInk,
    secondary = PaperInkDim,
    onSecondary = Paper,
    secondaryContainer = PaperElevated,
    onSecondaryContainer = PaperInk,
    tertiary = Color(0xFF0F8F5C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD0F5E4),
    onTertiaryContainer = PaperInk,
    background = Paper,
    onBackground = PaperInk,
    surface = Color(0xFFFFFFFF),
    onSurface = PaperInk,
    surfaceVariant = PaperElevated,
    onSurfaceVariant = PaperInkDim,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = PaperElevated,
    surfaceContainerHigh = PaperElevated,
    surfaceContainerHighest = Color(0xFFDDE2E8),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    outline = OutlineLight,
    outlineVariant = OutlineLight
)
