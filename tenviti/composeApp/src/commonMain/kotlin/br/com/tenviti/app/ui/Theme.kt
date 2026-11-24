package br.com.tenviti.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tenviti.composeapp.generated.resources.Poppins_Bold
import tenviti.composeapp.generated.resources.Poppins_Medium
import tenviti.composeapp.generated.resources.Poppins_Regular
import tenviti.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val PoppinsFamily = FontFamily(
        Font(Res.font.Poppins_Regular, FontWeight.Normal),
        Font(Res.font.Poppins_Medium, FontWeight.Medium),
        Font(Res.font.Poppins_Bold, FontWeight.Bold)
    )

    val AppTypography = Typography(
        headlineLarge = TextStyle(
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    )

    MaterialTheme(
        typography = AppTypography,
        content = content
    )
}