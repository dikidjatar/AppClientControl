package com.xeg911.shared.util

private val FORBIDDEN_KEY_CHARS = Regex("""[.$\[\]#/\u0000-\u001F\u007F]""")

fun String.toSafeFirebaseKey(replacementChar: String = "_"): String =
    replace(FORBIDDEN_KEY_CHARS, replacementChar)
