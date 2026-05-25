package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "note_reminders")
data class NoteReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val date: String, // format YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

@Dao
interface NoteReminderDao {
    @Query("SELECT * FROM note_reminders ORDER BY timestamp DESC")
    fun getAllReminders(): Flow<List<NoteReminder>>

    @Query("SELECT * FROM note_reminders WHERE date = :date ORDER BY timestamp DESC")
    fun getRemindersForDate(date: String): Flow<List<NoteReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: NoteReminder)

    @Query("DELETE FROM note_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)

    @Query("UPDATE note_reminders SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: Int, isCompleted: Boolean)
}

// Room Entity for Custom Free Fire player telemetry
@Entity(tableName = "ff_players")
data class FFPlayerDataEntity(
    @PrimaryKey val uid: String,
    val username: String,
    val level: Int,
    val rank: String,
    val winRate: String,
    val headshotRatio: String,
    val playStyle: String,
    val favouriteWeapon: String,
    val guildName: String,
    val guildLeader: String,
    val regionalTitle: String,
    val battleStars: Int
)

@Dao
interface FFPlayerDao {
    @Query("SELECT * FROM ff_players WHERE uid = :uid LIMIT 1")
    suspend fun getPlayerByUid(uid: String): FFPlayerDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: FFPlayerDataEntity)

    @Query("DELETE FROM ff_players WHERE uid = :uid")
    suspend fun deletePlayer(uid: String)
}

@Database(entities = [NoteReminder::class, FFPlayerDataEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteReminderDao(): NoteReminderDao
    abstract fun ffPlayerDao(): FFPlayerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gp_panel_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
