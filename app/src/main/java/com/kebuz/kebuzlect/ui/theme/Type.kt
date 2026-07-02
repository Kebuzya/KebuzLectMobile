package com.kebuz.kebuzlect.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kebuz.kebuzlect.R

val IbmPlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_bold, FontWeight.Bold),
)

val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.W800),
)

private val baseline = Typography()

val Typography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = IbmPlexSans),
    displayMedium = baseline.displayMedium.copy(fontFamily = IbmPlexSans),
    displaySmall = baseline.displaySmall.copy(fontFamily = IbmPlexSans),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = IbmPlexSans),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = IbmPlexSans),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = IbmPlexSans),
    titleLarge = baseline.titleLarge.copy(fontFamily = IbmPlexSans, fontWeight = FontWeight.SemiBold),
    titleMedium = baseline.titleMedium.copy(fontFamily = IbmPlexSans, fontWeight = FontWeight.SemiBold),
    titleSmall = baseline.titleSmall.copy(fontFamily = IbmPlexSans, fontWeight = FontWeight.SemiBold),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = IbmPlexSans),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = IbmPlexSans),
    bodySmall = baseline.bodySmall.copy(fontFamily = IbmPlexSans),
    labelLarge = baseline.labelLarge.copy(fontFamily = IbmPlexSans, fontWeight = FontWeight.SemiBold),
    labelMedium = baseline.labelMedium.copy(fontFamily = IbmPlexSans, fontWeight = FontWeight.Medium),
    labelSmall = baseline.labelSmall.copy(fontFamily = IbmPlexSans, fontWeight = FontWeight.Medium),
)

val MonoCaps = TextStyle(
    fontFamily = IbmPlexMono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    letterSpacing = 0.18.em,
)

val MonoMeta = TextStyle(
    fontFamily = IbmPlexMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    letterSpacing = 0.04.em,
)
