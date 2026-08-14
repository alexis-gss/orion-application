package com.orion.app.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ===================================================================================
// TV Time-style "streaming app" palette: near-black / heavily saturated anthracite
// background in dark mode (this app type's main mode), the brand's gold-yellow as a
// single, strong accent (never diluted across many colors), movie ratings colored
// separately (green / amber / red) to remain a standalone signal.
// ===================================================================================

// ---------------------------------------------------------------------------------
// Light palette — warm cream background, the logo's black as a strong accent, yellow
// as the highlight
// ---------------------------------------------------------------------------------
private val LightColors = lightColorScheme(
    primary = Color(0xFF17171A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFC93C),
    onPrimaryContainer = Color(0xFF231A00),
    secondary = Color(0xFF6B6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E7D3),
    onSecondaryContainer = Color(0xFF241D0D),
    tertiary = Color(0xFF8A5D00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA6),
    onTertiaryContainer = Color(0xFF2A1B00),
    error = Color(0xFFE0453A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF1A1918),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1918),
    surfaceVariant = Color(0xFFECE5D6),
    onSurfaceVariant = Color(0xFF4C463A),
    outline = Color(0xFF7D7669),
    outlineVariant = Color(0xFFE1DACB),
    inverseSurface = Color(0xFF201F1C),
    inverseOnSurface = Color(0xFFF7F5F0),
    inversePrimary = Color(0xFFFFC93C),
)

// ---------------------------------------------------------------------------------
// Dark palette — deep near-black in a streaming-app style, dominant gold-yellow,
// slightly bluish surfaces to avoid a "dirty gray" look.
// ---------------------------------------------------------------------------------
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFC93C),
    onPrimary = Color(0xFF241A00),
    primaryContainer = Color(0xFF4A3600),
    onPrimaryContainer = Color(0xFFFFE49B),
    secondary = Color(0xFFCBC3B0),
    onSecondary = Color(0xFF353024),
    secondaryContainer = Color(0xFF2B271E),
    onSecondaryContainer = Color(0xFFEDE4CE),
    tertiary = Color(0xFFF4A94A),
    onTertiary = Color(0xFF402C00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFDF99),
    error = Color(0xFFFF6B5E),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A0A0D),
    onBackground = Color(0xFFEFECE4),
    surface = Color(0xFF131317),
    onSurface = Color(0xFFEFECE4),
    surfaceVariant = Color(0xFF232228),
    onSurfaceVariant = Color(0xFFC9C4B8),
    outline = Color(0xFF7D7A72),
    outlineVariant = Color(0xFF2E2D33),
    inverseSurface = Color(0xFFEFECE4),
    inverseOnSurface = Color(0xFF1A1918),
    inversePrimary = Color(0xFF8A5D00),
)

/**
 * Custom colors not covered by the standard Material3 ColorScheme: poster gradients,
 * colored rating badges (the TV Time-style "signature look"), floating nav's frosted
 * glass background, home screens' hero gradient.
 * Accessible everywhere via `OrionColors.colors`.
 */
data class OrionExtendedColors(
    val posterOverlayTop: Color,
    val posterOverlayBottom: Color,
    val posterOverlayText: Color,
    val badgeBackground: Color,
    val badgeText: Color,
    val navBarContainer: Color,
    val navBarBorder: Color,
    val navBarSelectedContainer: Color,
    val navBarSelectedContainerAlt: Color,
    val navBarIcon: Color,
    val navBarSelectedIcon: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val ratingGood: Color,
    val ratingAverage: Color,
    val ratingLow: Color,
    val chipSurface: Color,
)

private val LightExtendedColors = OrionExtendedColors(
    posterOverlayTop = Color(0x00000000),
    posterOverlayBottom = Color(0xE6141312),
    posterOverlayText = Color(0xFFFFFFFF),
    badgeBackground = Color(0xFFFFC93C),
    badgeText = Color(0xFF231A00),
    navBarContainer = Color(0xFF181817),
    navBarBorder = Color(0x1FFFFFFF),
    navBarSelectedContainer = Color(0xFFFFC93C),
    navBarSelectedContainerAlt = Color(0xFFFFDB74),
    navBarIcon = Color(0xB3FFFFFF),
    navBarSelectedIcon = Color(0xFF231A00),
    cardSurface = Color(0xFFFFFFFF),
    cardBorder = Color(0x14000000),
    heroGradientStart = Color(0xFFFFC93C),
    heroGradientEnd = Color(0xFFFF9D3C),
    ratingGood = Color(0xFF1FB565),
    ratingAverage = Color(0xFFE8A400),
    ratingLow = Color(0xFFE0453A),
    chipSurface = Color(0xFFEFE8D9),
)

private val DarkExtendedColors = OrionExtendedColors(
    posterOverlayTop = Color(0x00000000),
    posterOverlayBottom = Color(0xF2000000),
    posterOverlayText = Color(0xFFFFFFFF),
    badgeBackground = Color(0xFFFFC93C),
    badgeText = Color(0xFF241A00),
    navBarContainer = Color(0xFF17171C),
    navBarBorder = Color(0x1FFFFFFF),
    navBarSelectedContainer = Color(0xFFFFC93C),
    navBarSelectedContainerAlt = Color(0xFFFFAE42),
    navBarIcon = Color(0x99EFECE4),
    navBarSelectedIcon = Color(0xFF241A00),
    cardSurface = Color(0xFF17171B),
    cardBorder = Color(0x14FFFFFF),
    heroGradientStart = Color(0xFF2A2410),
    heroGradientEnd = Color(0xFF0A0A0D),
    ratingGood = Color(0xFF3ADC85),
    ratingAverage = Color(0xFFFFC93C),
    ratingLow = Color(0xFFFF6B5E),
    chipSurface = Color(0xFF232228),
)

// ---------------------------------------------------------------------------------
// "Video games" palette — Twitch-style purple gradient, same structure as cinema so
// MaterialTheme.colorScheme stays usable everywhere without changing UI code.
// ---------------------------------------------------------------------------------
private val GamesLightColors = lightColorScheme(
    primary = Color(0xFF6441A5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4D6FF),
    onPrimaryContainer = Color(0xFF230046),
    secondary = Color(0xFF6B6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E7D3),
    onSecondaryContainer = Color(0xFF241D0D),
    tertiary = Color(0xFF9147FF),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADCFF),
    onTertiaryContainer = Color(0xFF2E0070),
    error = Color(0xFFE0453A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F5F9),
    onBackground = Color(0xFF1A1918),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1918),
    surfaceVariant = Color(0xFFE8E0F0),
    onSurfaceVariant = Color(0xFF49435A),
    outline = Color(0xFF79738A),
    outlineVariant = Color(0xFFDDD5EA),
    inverseSurface = Color(0xFF201F1C),
    inverseOnSurface = Color(0xFFF7F5F0),
    inversePrimary = Color(0xFFCBB2FF),
)

private val GamesDarkColors = darkColorScheme(
    primary = Color(0xFF9147FF),
    onPrimary = Color(0xFF2A0060),
    primaryContainer = Color(0xFF4B1F91),
    onPrimaryContainer = Color(0xFFE4D6FF),
    secondary = Color(0xFFCBC3B0),
    onSecondary = Color(0xFF353024),
    secondaryContainer = Color(0xFF2B271E),
    onSecondaryContainer = Color(0xFFEDE4CE),
    tertiary = Color(0xFFBE9DFF),
    onTertiary = Color(0xFF3B0090),
    tertiaryContainer = Color(0xFF5A2FBE),
    onTertiaryContainer = Color(0xFFEADCFF),
    error = Color(0xFFFF6B5E),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0C0A12),
    onBackground = Color(0xFFEFECE4),
    surface = Color(0xFF161221),
    onSurface = Color(0xFFEFECE4),
    surfaceVariant = Color(0xFF272235),
    onSurfaceVariant = Color(0xFFC9C4D8),
    outline = Color(0xFF847E96),
    outlineVariant = Color(0xFF322D40),
    inverseSurface = Color(0xFFEFECE4),
    inverseOnSurface = Color(0xFF1A1918),
    inversePrimary = Color(0xFF6441A5),
)

private val GamesLightExtendedColors = OrionExtendedColors(
    posterOverlayTop = Color(0x00000000),
    posterOverlayBottom = Color(0xE6141312),
    posterOverlayText = Color(0xFFFFFFFF),
    badgeBackground = Color(0xFF9147FF),
    badgeText = Color(0xFFFFFFFF),
    navBarContainer = Color(0xFF18131F),
    navBarBorder = Color(0x1FFFFFFF),
    navBarSelectedContainer = Color(0xFF9147FF),
    navBarSelectedContainerAlt = Color(0xFFB58CFF),
    navBarIcon = Color(0xB3FFFFFF),
    navBarSelectedIcon = Color(0xFFFFFFFF),
    cardSurface = Color(0xFFFFFFFF),
    cardBorder = Color(0x14000000),
    heroGradientStart = Color(0xFF9147FF),
    heroGradientEnd = Color(0xFF4B1F91),
    ratingGood = Color(0xFF1FB565),
    ratingAverage = Color(0xFFE8A400),
    ratingLow = Color(0xFFE0453A),
    chipSurface = Color(0xFFEDE4F8),
)

private val GamesDarkExtendedColors = OrionExtendedColors(
    posterOverlayTop = Color(0x00000000),
    posterOverlayBottom = Color(0xF2000000),
    posterOverlayText = Color(0xFFFFFFFF),
    badgeBackground = Color(0xFF9147FF),
    badgeText = Color(0xFFFFFFFF),
    navBarContainer = Color(0xFF161120),
    navBarBorder = Color(0x1FFFFFFF),
    navBarSelectedContainer = Color(0xFF9147FF),
    navBarSelectedContainerAlt = Color(0xFFBE9DFF),
    navBarIcon = Color(0x99EFECE4),
    navBarSelectedIcon = Color(0xFFFFFFFF),
    cardSurface = Color(0xFF1C1729),
    cardBorder = Color(0x14FFFFFF),
    heroGradientStart = Color(0xFF3B1E70),
    heroGradientEnd = Color(0xFF0C0A12),
    ratingGood = Color(0xFF3ADC85),
    ratingAverage = Color(0xFFFFC93C),
    ratingLow = Color(0xFFFF6B5E),
    chipSurface = Color(0xFF272235),
)

// ---------------------------------------------------------------------------------
// "Books" palette — library/bookstore-style blue, same structure as cinema and video
// games so MaterialTheme.colorScheme stays usable everywhere.
// ---------------------------------------------------------------------------------
private val BooksLightColors = lightColorScheme(
    primary = Color(0xFF2258D3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF5B6478),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE2F3),
    onSecondaryContainer = Color(0xFF181E2E),
    tertiary = Color(0xFF0891B2),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC9F0FA),
    onTertiaryContainer = Color(0xFF002F38),
    error = Color(0xFFE0453A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF1A1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE1E4F0),
    onSurfaceVariant = Color(0xFF44475A),
    outline = Color(0xFF75778A),
    outlineVariant = Color(0xFFD5D7E5),
    inverseSurface = Color(0xFF1F2024),
    inverseOnSurface = Color(0xFFF5F6FB),
    inversePrimary = Color(0xFFB0C6FF),
)

private val BooksDarkColors = darkColorScheme(
    primary = Color(0xFF6E9BFF),
    onPrimary = Color(0xFF002C6D),
    primaryContainer = Color(0xFF0F3D93),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFC3C6D9),
    onSecondary = Color(0xFF2C3142),
    secondaryContainer = Color(0xFF232733),
    onSecondaryContainer = Color(0xFFDFE2F3),
    tertiary = Color(0xFF4DD4F0),
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = Color(0xFF004E5C),
    onTertiaryContainer = Color(0xFFC9F0FA),
    error = Color(0xFFFF6B5E),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A0C13),
    onBackground = Color(0xFFE7E9F2),
    surface = Color(0xFF13161F),
    onSurface = Color(0xFFE7E9F2),
    surfaceVariant = Color(0xFF232735),
    onSurfaceVariant = Color(0xFFC2C5D6),
    outline = Color(0xFF7D8094),
    outlineVariant = Color(0xFF2E3242),
    inverseSurface = Color(0xFFE7E9F2),
    inverseOnSurface = Color(0xFF1A1B1F),
    inversePrimary = Color(0xFF2258D3),
)

private val BooksLightExtendedColors = OrionExtendedColors(
    posterOverlayTop = Color(0x00000000),
    posterOverlayBottom = Color(0xE6141312),
    posterOverlayText = Color(0xFFFFFFFF),
    badgeBackground = Color(0xFF2258D3),
    badgeText = Color(0xFFFFFFFF),
    navBarContainer = Color(0xFF13161F),
    navBarBorder = Color(0x1FFFFFFF),
    navBarSelectedContainer = Color(0xFF2258D3),
    navBarSelectedContainerAlt = Color(0xFF5B8DFF),
    navBarIcon = Color(0xB3FFFFFF),
    navBarSelectedIcon = Color(0xFFFFFFFF),
    cardSurface = Color(0xFFFFFFFF),
    cardBorder = Color(0x14000000),
    heroGradientStart = Color(0xFF2258D3),
    heroGradientEnd = Color(0xFF0F3D93),
    ratingGood = Color(0xFF1FB565),
    ratingAverage = Color(0xFFE8A400),
    ratingLow = Color(0xFFE0453A),
    chipSurface = Color(0xFFE7EBFA),
)

private val BooksDarkExtendedColors = OrionExtendedColors(
    posterOverlayTop = Color(0x00000000),
    posterOverlayBottom = Color(0xF2000000),
    posterOverlayText = Color(0xFFFFFFFF),
    badgeBackground = Color(0xFF2258D3),
    badgeText = Color(0xFFFFFFFF),
    navBarContainer = Color(0xFF10131C),
    navBarBorder = Color(0x1FFFFFFF),
    navBarSelectedContainer = Color(0xFF2258D3),
    navBarSelectedContainerAlt = Color(0xFF6E9BFF),
    navBarIcon = Color(0x99EFECE4),
    navBarSelectedIcon = Color(0xFFFFFFFF),
    cardSurface = Color(0xFF171A24),
    cardBorder = Color(0x14FFFFFF),
    heroGradientStart = Color(0xFF14285C),
    heroGradientEnd = Color(0xFF0A0C13),
    ratingGood = Color(0xFF3ADC85),
    ratingAverage = Color(0xFFFFC93C),
    ratingLow = Color(0xFFFF6B5E),
    chipSurface = Color(0xFF232735),
)

val LocalOrionColors = staticCompositionLocalOf { LightExtendedColors }

/** Theme universe: determines which palette (cinema yellow / games purple / books blue) to apply. */
enum class ThemeUniverse { CINEMA, GAMES, BOOKS }

/** Convenient access: `OrionColors.colors.badgeBackground` from any composable. */
object OrionColors {
    val colors: OrionExtendedColors
        @Composable get() = LocalOrionColors.current
}

/** "Hero" gradient (welcome banner, Planning/Account headers). */
val OrionExtendedColors.heroBrush: Brush
    @Composable get() = Brush.verticalGradient(listOf(heroGradientStart, heroGradientEnd))

/** TV Time-style color associated with a rating out of 10: green / amber / red. */
fun OrionExtendedColors.colorForRating(rating: Double): Color = when {
    rating >= 7.0 -> ratingGood
    rating >= 5.0 -> ratingAverage
    else -> ratingLow
}

@Composable
fun OrionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    universe: ThemeUniverse = ThemeUniverse.CINEMA,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        universe == ThemeUniverse.GAMES -> if (darkTheme) GamesDarkColors else GamesLightColors
        universe == ThemeUniverse.BOOKS -> if (darkTheme) BooksDarkColors else BooksLightColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    val extendedColors = when {
        universe == ThemeUniverse.GAMES -> if (darkTheme) GamesDarkExtendedColors else GamesLightExtendedColors
        universe == ThemeUniverse.BOOKS -> if (darkTheme) BooksDarkExtendedColors else BooksLightExtendedColors
        darkTheme -> DarkExtendedColors
        else -> LightExtendedColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        if (activity != null) {
            androidx.compose.runtime.SideEffect {
                WindowCompat.getInsetsController(activity.window, view)
                    .isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(activity.window, view)
                    .isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalOrionColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OrionTypography,
            content = content
        )
    }
}
