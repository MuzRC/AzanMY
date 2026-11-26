package com.musa.azanmy.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.musa.azanmy.data.ThemePreference

val MidnightEmerald = Color(0xFF051310)
val DeepPine = Color(0xFF0B221D)
val CardEmerald = Color(0xFF102E27)
val ElevatedEmerald = Color(0xFF173E35)

val WarmGold = Color(0xFFD4AF37)
val BrightGold = Color(0xFFE6C252)
val PaleGoldHighlight = Color(0xFFFFF0B8)
val DeepBronzeGold = Color(0xFF7A5610)
val SoftGoldContainer = Color(0x33D4AF37)

val OffWhiteText = Color(0xFFF6F8F7)
val MutedMintText = Color(0xFFA8C9C1)
val SubtleMintBorder = Color(0xFF28584C)

val WarmIvory = Color(0xFFFBF9F5)
val LightCardSurface = Color(0xFFFFFFFF)
val LightEmeraldPrimary = Color(0xFF0B4F3E)
val LightEmeraldDarkText = Color(0xFF0A2E25)
val LightMutedText = Color(0xFF4A6B62)
val LightBorder = Color(0xFFD2DFDB)

@Immutable
data class ExtendedAzanColors(
    val isDark: Boolean,
    val goldAccent: Color,
    val goldBright: Color,
    val goldHighlight: Color,
    val goldDeep: Color,
    val goldContainer: Color,
    val cardBackground: Color,
    val cardGradientTop: Color,
    val cardGradientBottom: Color,
    val cardBorder: Color,
    val cardRimTop: Color,
    val cardRimBottom: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val screenGlowTop: Color,
    val screenGlowBottom: Color,
    val subtleText: Color,
    val statusOk: Color,
    val statusWarn: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedAzanColors(
        isDark = true,
        goldAccent = WarmGold,
        goldBright = BrightGold,
        goldHighlight = PaleGoldHighlight,
        goldDeep = DeepBronzeGold,
        goldContainer = SoftGoldContainer,
        cardBackground = CardEmerald,
        cardGradientTop = Color(0xFF163C33),
        cardGradientBottom = Color(0xFF0B221D),
        cardBorder = SubtleMintBorder,
        cardRimTop = Color(0xFF74B8A7),
        cardRimBottom = Color(0xFF234E43),
        heroStart = Color(0xFF143E34),
        heroEnd = Color(0xFF071915),
        screenGlowTop = Color(0xFF13483C),
        screenGlowBottom = Color(0xFF04100D),
        subtleText = MutedMintText,
        statusOk = Color(0xFF4ADE80),
        statusWarn = Color(0xFFFBBF24)
    )
}

private val DarkAzanColorScheme = darkColorScheme(
    primary = BrightGold,
    onPrimary = Color(0xFF1D1600),
    primaryContainer = SoftGoldContainer,
    onPrimaryContainer = BrightGold,
    secondary = MutedMintText,
    onSecondary = MidnightEmerald,
    secondaryContainer = ElevatedEmerald,
    onSecondaryContainer = OffWhiteText,
    background = MidnightEmerald,
    onBackground = OffWhiteText,
    surface = DeepPine,
    onSurface = OffWhiteText,
    surfaceVariant = CardEmerald,
    onSurfaceVariant = MutedMintText,
    outline = SubtleMintBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF3B0909)
)

private val LightAzanColorScheme = lightColorScheme(
    primary = LightEmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EFE8),
    onPrimaryContainer = LightEmeraldDarkText,
    secondary = Color(0xFF8C6D1F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7EED3),
    onSecondaryContainer = Color(0xFF3A2B00),
    background = WarmIvory,
    onBackground = LightEmeraldDarkText,
    surface = LightCardSurface,
    onSurface = LightEmeraldDarkText,
    surfaceVariant = Color(0xFFF0F4F2),
    onSurfaceVariant = LightMutedText,
    outline = LightBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun AzanMYTheme(
    themePreference: ThemePreference,
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkAzanColorScheme else LightAzanColorScheme
    val extendedColors = if (isDark) {
        ExtendedAzanColors(
            isDark = true,
            goldAccent = WarmGold,
            goldBright = BrightGold,
            goldHighlight = PaleGoldHighlight,
            goldDeep = DeepBronzeGold,
            goldContainer = SoftGoldContainer,
            cardBackground = CardEmerald,
            cardGradientTop = Color(0xFF163D34),
            cardGradientBottom = Color(0xFF0A211C),
            cardBorder = SubtleMintBorder,
            cardRimTop = Color(0xFF79BDA9),
            cardRimBottom = Color(0xFF214A40),
            heroStart = Color(0xFF154137),
            heroEnd = Color(0xFF071A16),
            screenGlowTop = Color(0xFF144C40),
            screenGlowBottom = Color(0xFF04100D),
            subtleText = MutedMintText,
            statusOk = Color(0xFF4ADE80),
            statusWarn = Color(0xFFFBBF24)
        )
    } else {
        ExtendedAzanColors(
            isDark = false,
            goldAccent = Color(0xFFB3871B),
            goldBright = Color(0xFF9A7210),
            goldHighlight = Color(0xFFFFE082),
            goldDeep = Color(0xFF6E4E0A),
            goldContainer = Color(0x26D4AF37),
            cardBackground = LightCardSurface,
            cardGradientTop = Color(0xFFFFFFFF),
            cardGradientBottom = Color(0xFFF3F7F5),
            cardBorder = LightBorder,
            cardRimTop = Color(0xFFD4AF37),
            cardRimBottom = Color(0xFFC5D6D1),
            heroStart = Color(0xFF0E4F3F),
            heroEnd = Color(0xFF062920),
            screenGlowTop = Color(0xFFE7F2EE),
            screenGlowBottom = WarmIvory,
            subtleText = LightMutedText,
            statusOk = Color(0xFF15803D),
            statusWarn = Color(0xFFB45309)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
