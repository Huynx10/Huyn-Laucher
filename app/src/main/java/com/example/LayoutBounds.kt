package com.example

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spring animations may briefly overshoot zero; Compose padding requires a finite non-negative value. */
internal fun nonNegativeLayoutDp(value: Dp): Dp =
    if (value.value.isFinite()) value.coerceAtLeast(0.dp) else 0.dp
