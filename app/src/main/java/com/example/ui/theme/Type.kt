package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val TimesFontFamily = FontFamily(Font(R.font.tinos, FontWeight.Normal))

val AppTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.6.sp,
      ),
    displayMedium =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.4.sp,
      ),
    headlineLarge =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.4.sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.3.sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.25.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.25.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.15.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = TimesFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.15.sp,
      ),
  )
