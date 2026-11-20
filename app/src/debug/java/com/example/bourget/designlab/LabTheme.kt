package com.example.bourget.designlab

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

/** The three directions compared in the lab. */
enum class Concept(val label: String) {
    /** Stock Material 3 with dynamic color: looks like a system tool, no decoration. */
    SYSTEM("시스템"),

    /** Warm gray canvas, white grouped cards, one ink color (prototype direction B). */
    CARDS("카드"),

    /** No cards: large type and hairlines on paper; the confirm popup is a full-screen pause. */
    TYPE("글자"),
}

/** Colors the lab mocks draw with directly, on top of the Material color scheme. */
data class LabPalette(
    val canvas: Color,
    val card: Color,
    val ink: Color,
    val inkVariant: Color,
    val onInk: Color,
    val line: Color,
    val outline: Color,
    val tonal: Color,
)

data class LabStyle(val concept: Concept, val p: LabPalette, val lineSeed: Boolean)

val LocalLabStyle = staticCompositionLocalOf { LabStyle(Concept.CARDS, warmPalette(dark = false, paper = false), false) }

fun warmPalette(dark: Boolean, paper: Boolean): LabPalette = if (!dark) {
    LabPalette(
        canvas = if (paper) Color(0xFFFBFAF8) else Color(0xFFF1EFEB),
        card = Color(0xFFFFFFFF),
        ink = Color(0xFF22201D),
        inkVariant = Color(0xFF67625B),
        onInk = Color(0xFFFFFFFF),
        line = if (paper) Color(0xFFE4E0DA) else Color(0xFFEFECE7),
        outline = Color(0xFF8E8880),
        tonal = Color(0xFFE6E3DD),
    )
} else {
    LabPalette(
        canvas = if (paper) Color(0xFF141312) else Color(0xFF121110),
        card = Color(0xFF1F1E1C),
        ink = Color(0xFFEDEAE5),
        inkVariant = Color(0xFFA8A29A),
        onInk = Color(0xFF1F1D1A),
        line = Color(0xFF2E2C29),
        outline = Color(0xFF7C766F),
        tonal = Color(0xFF2B2926),
    )
}

/**
 * LINE Seed Sans KR, if its files are in res/font as line_seed_kr_regular and line_seed_kr_bold.
 * Looked up by name so the lab compiles before the font is added.
 */
@Composable
fun lineSeedOrNull(): FontFamily? {
    val context = LocalContext.current
    return remember {
        val res = context.resources
        @Suppress("DiscouragedApi")
        val regular = res.getIdentifier("line_seed_kr_regular", "font", context.packageName)
        @Suppress("DiscouragedApi")
        val bold = res.getIdentifier("line_seed_kr_bold", "font", context.packageName)
        if (regular != 0 && bold != 0) {
            FontFamily(Font(regular, FontWeight.Normal), Font(bold, FontWeight.Bold))
        } else {
            null
        }
    }
}

@Composable
private fun labColorScheme(concept: Concept, dark: Boolean): ColorScheme {
    if (concept == Concept.SYSTEM) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        return if (dark) darkColorScheme() else lightColorScheme()
    }
    val p = warmPalette(dark, paper = concept == Concept.TYPE)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.ink,
        onPrimary = p.onInk,
        primaryContainer = p.tonal,
        onPrimaryContainer = p.ink,
        secondary = p.ink,
        onSecondary = p.onInk,
        secondaryContainer = p.tonal,
        onSecondaryContainer = p.ink,
        background = p.canvas,
        onBackground = p.ink,
        surface = p.canvas,
        onSurface = p.ink,
        surfaceVariant = p.tonal,
        onSurfaceVariant = p.inkVariant,
        surfaceTint = Color.Transparent,
        outline = p.outline,
        outlineVariant = p.line,
    )
}

private fun paletteFrom(scheme: ColorScheme) = LabPalette(
    canvas = scheme.surface,
    card = scheme.surfaceVariant,
    ink = scheme.onSurface,
    inkVariant = scheme.onSurfaceVariant,
    onInk = scheme.onPrimary,
    line = scheme.outlineVariant,
    outline = scheme.outline,
    tonal = scheme.secondaryContainer,
)

private fun TextStyle.set(family: FontFamily, size: Int, line: Int, bold: Boolean) = copy(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    letterSpacing = 0.sp,
)

/** LINE Seed KR has only Regular and Bold, so hierarchy comes from size and those two weights. */
private fun labTypography(family: FontFamily, concept: Concept): Typography {
    val b = Typography()
    return when (concept) {
        Concept.SYSTEM -> b
        Concept.CARDS -> Typography(
            headlineSmall = b.headlineSmall.set(family, 24, 32, true),
            titleLarge = b.titleLarge.set(family, 20, 28, true),
            titleMedium = b.titleMedium.set(family, 15, 22, true),
            bodyLarge = b.bodyLarge.set(family, 16, 26, false),
            bodyMedium = b.bodyMedium.set(family, 14, 21, false),
            bodySmall = b.bodySmall.set(family, 12, 18, false),
            labelLarge = b.labelLarge.set(family, 14, 20, true),
            labelMedium = b.labelMedium.set(family, 12, 16, true),
        )
        Concept.TYPE -> Typography(
            displaySmall = b.displaySmall.set(family, 34, 42, true),
            headlineSmall = b.headlineSmall.set(family, 26, 34, true),
            titleLarge = b.titleLarge.set(family, 20, 28, true),
            titleMedium = b.titleMedium.set(family, 16, 24, true),
            bodyLarge = b.bodyLarge.set(family, 16, 26, false),
            bodyMedium = b.bodyMedium.set(family, 14, 21, false),
            bodySmall = b.bodySmall.set(family, 12, 18, false),
            labelLarge = b.labelLarge.set(family, 16, 22, true),
            labelMedium = b.labelMedium.set(family, 13, 18, true),
        )
    }
}

/** Applies one concept, light or dark, and an extra font scale on top of the system's. */
@Composable
fun LabTheme(concept: Concept, dark: Boolean, fontScale: Float, content: @Composable () -> Unit) {
    val lineSeed = lineSeedOrNull()
    val family = lineSeed ?: FontFamily.Default
    val scheme = labColorScheme(concept, dark)
    val palette = if (concept == Concept.SYSTEM) paletteFrom(scheme) else warmPalette(dark, concept == Concept.TYPE)
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * fontScale),
        LocalLabStyle provides LabStyle(concept, palette, lineSeed != null),
    ) {
        MaterialTheme(colorScheme = scheme, typography = labTypography(family, concept)) {
            Surface(modifier = Modifier.fillMaxSize(), color = palette.canvas, contentColor = palette.ink) {
                content()
            }
        }
    }
}
