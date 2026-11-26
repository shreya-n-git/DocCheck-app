package com.doccheck.app.util

import android.util.Patterns

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    fun doPasswordsMatch(password: String, confirm: String): Boolean {
        return password == confirm && confirm.isNotEmpty()
    }
}

