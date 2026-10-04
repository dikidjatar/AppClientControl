package com.xeg911.shared.util

fun String.equalsIgnoreCase(other: String?) =
    equals(other, ignoreCase = true)

fun String.containsIgnoreCase(other: String) =
    contains(other, ignoreCase = true)