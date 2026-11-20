package com.example.bourget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.bourget.R

val LineSeed = FontFamily(
    Font(R.font.line_seed_kr_regular, FontWeight.Normal),
    Font(R.font.line_seed_kr_bold, FontWeight.Bold),
)

private fun TextStyle.set(size: Int, line: Int, bold: Boolean) = copy(
    fontFamily = LineSeed,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    letterSpacing = 0.sp,
)

/** LINE Seed KR has only Regular and Bold, so hierarchy comes from size and those two weights. */
val Typography = Typography().run {
    copy(
        displayLarge = displayLarge.set(57, 64, true),
        displayMedium = displayMedium.set(45, 52, true),
        displaySmall = displaySmall.set(34, 42, true),
        headlineLarge = headlineLarge.set(32, 40, true),
        headlineMedium = headlineMedium.set(28, 36, true),
        headlineSmall = headlineSmall.set(26, 34, true),
        titleLarge = titleLarge.set(20, 28, true),
        titleMedium = titleMedium.set(16, 24, true),
        titleSmall = titleSmall.set(14, 20, true),
        bodyLarge = bodyLarge.set(16, 26, false),
        bodyMedium = bodyMedium.set(14, 21, false),
        bodySmall = bodySmall.set(12, 18, false),
        labelLarge = labelLarge.set(16, 22, true),
        labelMedium = labelMedium.set(13, 18, true),
        labelSmall = labelSmall.set(11, 16, true),
    )
}
