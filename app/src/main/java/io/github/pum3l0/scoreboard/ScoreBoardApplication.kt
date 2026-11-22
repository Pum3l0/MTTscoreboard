package io.github.pum3l0.scoreboard

import android.app.Application
import androidx.room.Room
import io.github.pum3l0.scoreboard.data.match.RoomMatchStore
import io.github.pum3l0.scoreboard.data.match.ScoreBoardDatabase
import io.github.pum3l0.scoreboard.data.match.addMatchSides
import io.github.pum3l0.scoreboard.data.match.addMatchTiming
import io.github.pum3l0.scoreboard.data.settings.ThemePreferences
import io.github.pum3l0.scoreboard.data.settings.SETTINGS_FILE
import java.io.File

class ScoreBoardApplication : Application() {
    val matches by lazy {
        val database = Room.databaseBuilder(
            applicationContext, ScoreBoardDatabase::class.java, "scoreboard.db",
        ).addMigrations(addMatchSides, addMatchTiming).build()
        RoomMatchStore(database.matches())
    }

    val theme by lazy {
        ThemePreferences(getSharedPreferences(SETTINGS_FILE, MODE_PRIVATE), File(filesDir, "fonts"))
    }
}
