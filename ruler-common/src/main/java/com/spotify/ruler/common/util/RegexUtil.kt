package com.spotify.ruler.common.util

/**
 * Escapes special regex characters in the input string.
 *
 * This extension function escapes backslashes, caret, dollar sign, period, vertical bar, question mark,
 * asterisk, plus sign, parentheses, square brackets, and curly braces.
 *
 * @return The input string with special regex characters properly escaped.
 */
fun String.toEscapeCharRegex(): Regex {
    val specialChars = setOf("\\", "^", "$", ".", "|", "?", "*", "+", "(", ")", "[", "]", "{", "}")

    if (!any { it.toString() in specialChars }) {
        return this.toRegex()
    }

    val escapedRegex = StringBuilder()
    for (char in this) {
        val charStr = char.toString()
        if (charStr in specialChars) {
            escapedRegex.append("\\")
        }
        escapedRegex.append(char)
    }

    return escapedRegex.toString().toRegex()
}
