package io.github.pum3l0.scoreboard.data.match

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "matches", indices = [Index(value = ["pendingSlot"], unique = true)])
data class MatchEntity(
    @field:PrimaryKey val id: String,
    val firstName: String,
    val secondName: String,
    val gamesToWin: Int,
    val firstServer: Int,
    val points: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val pendingSlot: Int?,
    @field:ColumnInfo(defaultValue = "0") val sidesSwapped: Boolean = false,
    val timing: String? = null,
)

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE pendingSlot = 1")
    suspend fun pending(): MatchEntity?

    @Query("SELECT * FROM matches WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC, id DESC")
    suspend fun history(): List<MatchEntity>

    @Query("DELETE FROM matches WHERE finishedAt IS NOT NULL")
    suspend fun clearHistory()

    @Insert
    suspend fun create(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArchived(matches: List<MatchEntity>): List<Long>

    @Query(
        """
        UPDATE matches SET points = :points, finishedAt = :finishedAt, pendingSlot = :pendingSlot, sidesSwapped = :sidesSwapped, timing = :timing
        WHERE id = :id AND pendingSlot = 1 AND points = :previousPoints AND sidesSwapped = :previousSidesSwapped AND timing IS :previousTiming
        """,
    )
    suspend fun save(
        id: String,
        previousPoints: String,
        points: String,
        finishedAt: Long?,
        pendingSlot: Int?,
        previousSidesSwapped: Boolean,
        sidesSwapped: Boolean,
        previousTiming: String?,
        timing: String?,
    ): Int

    @Query("DELETE FROM matches WHERE id = :id AND pendingSlot = 1")
    suspend fun discardPending(id: String): Int

    @Transaction
    suspend fun replacePending(previousId: String, match: MatchEntity) {
        check(discardPending(previousId) == 1) { "The previous match has already changed" }
        create(match)
    }
}

@Database(entities = [MatchEntity::class], version = 3)
abstract class ScoreBoardDatabase : RoomDatabase() {
    abstract fun matches(): MatchDao
}


internal val addMatchSides = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE matches ADD COLUMN sidesSwapped INTEGER NOT NULL DEFAULT 0")
    }
}


internal val addMatchTiming = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE matches ADD COLUMN timing TEXT")
    }
}
