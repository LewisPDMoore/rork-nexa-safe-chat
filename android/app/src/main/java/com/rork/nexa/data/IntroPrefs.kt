package com.rork.nexa.data

import android.content.Context

object IntroPrefs {
    private const val FILE = "nexa_intro"
    private const val KEY_SEEN = "seen_intro"

    fun hasSeen(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEN, false)

    fun markSeen(context: Context) {
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEN, true)
            .apply()
    }
}
