package kr.moa.app

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalTextApi::class)
private val MoaFont = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        Font(R.font.pretendard_variable, weight = FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
    }
)

fun moaTypography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = MoaFont), displayMedium = base.displayMedium.copy(fontFamily = MoaFont), displaySmall = base.displaySmall.copy(fontFamily = MoaFont),
        headlineLarge = base.headlineLarge.copy(fontFamily = MoaFont), headlineMedium = base.headlineMedium.copy(fontFamily = MoaFont), headlineSmall = base.headlineSmall.copy(fontFamily = MoaFont),
        titleLarge = base.titleLarge.copy(fontFamily = MoaFont), titleMedium = base.titleMedium.copy(fontFamily = MoaFont), titleSmall = base.titleSmall.copy(fontFamily = MoaFont),
        bodyLarge = base.bodyLarge.copy(fontFamily = MoaFont), bodyMedium = base.bodyMedium.copy(fontFamily = MoaFont), bodySmall = base.bodySmall.copy(fontFamily = MoaFont),
        labelLarge = base.labelLarge.copy(fontFamily = MoaFont), labelMedium = base.labelMedium.copy(fontFamily = MoaFont), labelSmall = base.labelSmall.copy(fontFamily = MoaFont)
    )
}
