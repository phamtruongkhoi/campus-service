package com.campus.service.util

import android.util.Patterns

fun isValidEmail(value: String): Boolean =
    Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()

fun isValidOtp(value: String): Boolean =
    value.length in 6..10 && value.all { it.isDigit() }