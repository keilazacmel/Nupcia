package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = BordeauxPrimary,
    onPrimary = OnBordeaux,
    primaryContainer = BordeauxContainer,
    onPrimaryContainer = OnBordeauxContainer,
    secondary = SageSecondary,
    onSecondary = OnSage,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = ChampagneTertiary,
    onTertiary = OnChampagne,
    tertiaryContainer = ChampagneContainer,
    onTertiaryContainer = OnChampagneContainer,
    background = SilkIvoryBg,
    onBackground = EspressoText,
    surface = AlabasterSurface,
    onSurface = EspressoText,
    surfaceVariant = BlushMistSurface,
    onSurfaceVariant = MutedPlumText,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = ErrorCrimson,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DarkColors = darkColorScheme(
    primary = BlushPrimaryDark,
    onPrimary = OnBlushDark,
    primaryContainer = BordeauxContainerDark,
    onPrimaryContainer = OnBordeauxContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = OnSageDark,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = OnSageContainerDark,
    tertiary = GoldTertiaryDark,
    onTertiary = OnGoldDark,
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = OnGoldContainerDark,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant
)

val NupciaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NupciaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NupciaTypography,
        shapes = NupciaShapes,
        content = content
    )
}
