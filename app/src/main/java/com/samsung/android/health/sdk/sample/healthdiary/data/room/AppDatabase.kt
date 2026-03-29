package com.samsung.android.health.sdk.sample.healthdiary.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.dao.*
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.*
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.BlockEntity
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.RoutineDao
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.RoutineEntity
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.WorkoutSessionDao
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.WorkoutSessionEntity

/**
 * Main Room database for the Health Diary application
 * Version 13: Added LocationPointEntity for GPS tracking feature
 */
@Database(
    entities = [
        SensorDataEntity::class,
        SensorBatchEntity::class,
        UploadLogEntity::class,
        MedicalDocumentEntity::class,
        TxAgentQueryEntity::class,
        TxAgentResponseEntity::class,
        RoutineEntity::class,
        BlockEntity::class,
        WorkoutSessionEntity::class,
        HabitEntity::class,
        HabitReminderTimeEntity::class,
        WatchStepsEntity::class,
        WatchHeartRateEntity::class,
        WatchSleepEntity::class,
        WatchDailySummaryEntity::class,
        PairedDeviceEntity::class,
        LocationPointEntity::class
    ],
    version = 13,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // DAOs
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun sensorBatchDao(): SensorBatchDao
    abstract fun uploadLogDao(): UploadLogDao
    abstract fun medicalDocumentDao(): MedicalDocumentDao
    abstract fun txAgentQueryDao(): TxAgentQueryDao
    abstract fun txAgentResponseDao(): TxAgentResponseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun habitDao(): HabitDao
    abstract fun habitReminderTimeDao(): HabitReminderTimeDao
    abstract fun watchHealthDao(): WatchHealthDao
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to version 2
         * Adds new tables for comprehensive data management
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create sensor_batches table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sensor_batches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        sensorType TEXT NOT NULL,
                        dataJson TEXT NOT NULL,
                        sampleCount INTEGER NOT NULL,
                        uploaded INTEGER NOT NULL DEFAULT 0,
                        uploadedAt INTEGER,
                        receivedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create upload_logs table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS upload_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        endpoint TEXT NOT NULL,
                        responseCode INTEGER,
                        errorMessage TEXT,
                        dataSizeBytes INTEGER,
                        durationMs INTEGER
                    )
                """.trimIndent())
                
                // Create indices for upload_logs
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_upload_logs_entityType_entityId 
                    ON upload_logs(entityType, entityId)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_upload_logs_status 
                    ON upload_logs(status)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_upload_logs_timestamp 
                    ON upload_logs(timestamp)
                """.trimIndent())
                
                // Create medical_documents table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS medical_documents (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        filename TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        uploadTimestamp INTEGER NOT NULL,
                        fileSize INTEGER NOT NULL,
                        mimeType TEXT NOT NULL,
                        description TEXT,
                        processed INTEGER NOT NULL DEFAULT 0,
                        processedAt INTEGER,
                        fileHash TEXT,
                        pageCount INTEGER,
                        tags TEXT
                    )
                """.trimIndent())
                
                // Create indices for medical_documents
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_medical_documents_filename 
                    ON medical_documents(filename)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_medical_documents_uploadTimestamp 
                    ON medical_documents(uploadTimestamp)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_medical_documents_processed 
                    ON medical_documents(processed)
                """.trimIndent())
                
                // Create txagent_queries table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS txagent_queries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        queryText TEXT NOT NULL,
                        documentId INTEGER,
                        queryType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        sentAt INTEGER,
                        completedAt INTEGER,
                        errorMessage TEXT,
                        userId TEXT,
                        FOREIGN KEY(documentId) REFERENCES medical_documents(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for txagent_queries
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_queries_documentId 
                    ON txagent_queries(documentId)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_queries_status 
                    ON txagent_queries(status)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_queries_queryType 
                    ON txagent_queries(queryType)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_queries_timestamp 
                    ON txagent_queries(timestamp)
                """.trimIndent())
                
                // Create txagent_responses table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS txagent_responses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        queryId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        responseText TEXT NOT NULL,
                        metadata TEXT,
                        confidence REAL,
                        sources TEXT,
                        userRating INTEGER,
                        userFeedback TEXT,
                        processingTimeMs INTEGER,
                        modelVersion TEXT,
                        FOREIGN KEY(queryId) REFERENCES txagent_queries(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for txagent_responses
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_txagent_responses_queryId 
                    ON txagent_responses(queryId)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_txagent_responses_timestamp 
                    ON txagent_responses(timestamp)
                """.trimIndent())
            }
        }

        /**
         * Migration from version 2 to version 3
         * No schema changes - version bump only
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes required - this migration exists to complete the migration path
                // from version 2 to version 3
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS routines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        routineId TEXT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_routines_routineId ON routines(routineId)")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS blocks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        blockId TEXT NOT NULL,
                        routineId TEXT NOT NULL,
                        exerciseName TEXT NOT NULL,
                        sets INTEGER NOT NULL,
                        targetWeight REAL NOT NULL,
                        targetReps INTEGER,
                        restSec INTEGER NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        FOREIGN KEY(routineId) REFERENCES routines(routineId) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_blocks_routineId ON blocks(routineId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_blocks_blockId ON blocks(blockId)")
                // Seed one routine with 2 blocks for end-to-end validation
                val rid = "00000000-0000-0000-0000-000000000001"
                val bid1 = "00000000-0000-0000-0000-000000000002"
                val bid2 = "00000000-0000-0000-0000-000000000003"
                database.execSQL("INSERT OR IGNORE INTO routines (routineId, name) VALUES ('$rid', 'Sample Routine')")
                database.execSQL("INSERT OR IGNORE INTO blocks (blockId, routineId, exerciseName, sets, targetWeight, targetReps, restSec, orderIndex) VALUES ('$bid1', '$rid', 'Push-ups', 4, 0, 12, 60, 0)")
                database.execSQL("INSERT OR IGNORE INTO blocks (blockId, routineId, exerciseName, sets, targetWeight, targetReps, restSec, orderIndex) VALUES ('$bid2', '$rid', 'Squats', 4, 40, 10, 60, 1)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val rid = "00000000-0000-0000-0000-000000000001"
                val bid1 = "00000000-0000-0000-0000-000000000002"
                val bid2 = "00000000-0000-0000-0000-000000000003"
                database.execSQL("UPDATE routines SET name = 'Test Routine' WHERE routineId = '$rid'")
                database.execSQL("UPDATE blocks SET sets = 3, restSec = 30 WHERE routineId = '$rid'")
                database.execSQL("INSERT OR IGNORE INTO routines (routineId, name) VALUES ('$rid', 'Test Routine')")
                database.execSQL("INSERT OR IGNORE INTO blocks (blockId, routineId, exerciseName, sets, targetWeight, targetReps, restSec, orderIndex) VALUES ('$bid1', '$rid', 'Push-ups', 3, 0, 12, 30, 0)")
                database.execSQL("INSERT OR IGNORE INTO blocks (blockId, routineId, exerciseName, sets, targetWeight, targetReps, restSec, orderIndex) VALUES ('$bid2', '$rid', 'Squats', 3, 40, 10, 30, 1)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS workout_sessions (
                        sessionId TEXT NOT NULL,
                        routineId TEXT NOT NULL,
                        routineName TEXT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        blocksSnapshotJson TEXT NOT NULL,
                        completionStateJson TEXT NOT NULL,
                        activeBlockIndex INTEGER NOT NULL,
                        activeSetIndex INTEGER NOT NULL,
                        PRIMARY KEY(sessionId)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS habits (
                        habitId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        triggerTime TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habits_habitId ON habits(habitId)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_reminder_times (
                        reminderId TEXT NOT NULL PRIMARY KEY,
                        habitId TEXT NOT NULL,
                        triggerTime TEXT NOT NULL,
                        FOREIGN KEY(habitId) REFERENCES habits(habitId) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_habit_reminder_times_habitId ON habit_reminder_times(habitId)")

                database.execSQL("""
                    INSERT INTO habit_reminder_times (reminderId, habitId, triggerTime)
                    SELECT 'mig-' || habitId || '-' || rowid, habitId, triggerTime FROM habits
                    WHERE triggerTime IS NOT NULL AND trim(triggerTime) != ''
                """.trimIndent())

                database.execSQL("""
                    CREATE TABLE habits_new (
                        habitId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                database.execSQL("INSERT INTO habits_new (habitId, title, isEnabled) SELECT habitId, title, isEnabled FROM habits")
                database.execSQL("DROP TABLE habits")
                database.execSQL("ALTER TABLE habits_new RENAME TO habits")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habits_habitId ON habits(habitId)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add dayOfWeek column to habit_reminder_times table
                // NULL means "all days" (backward compatible with existing records)
                database.execSQL("ALTER TABLE habit_reminder_times ADD COLUMN dayOfWeek TEXT")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add routineId column to habits table
                database.execSQL("ALTER TABLE habits ADD COLUMN routineId TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create watch_steps table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS watch_steps (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        steps INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        syncedAt INTEGER
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_watch_steps_date ON watch_steps(date)")

                // Create watch_heart_rate table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS watch_heart_rate (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bpm INTEGER NOT NULL,
                        measurementTimestamp INTEGER NOT NULL,
                        accuracy TEXT NOT NULL,
                        receivedAt INTEGER NOT NULL,
                        syncedAt INTEGER
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_watch_heart_rate_measurementTimestamp ON watch_heart_rate(measurementTimestamp)")

                // Create watch_sleep table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS watch_sleep (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        sleepMinutes INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        syncedAt INTEGER
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_watch_sleep_date ON watch_sleep(date)")

                // Create watch_daily_summary table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS watch_daily_summary (
                        date TEXT NOT NULL PRIMARY KEY,
                        steps INTEGER NOT NULL,
                        sleepMinutes INTEGER,
                        avgHeartRate INTEGER,
                        minHeartRate INTEGER,
                        maxHeartRate INTEGER,
                        heartRateSampleCount INTEGER NOT NULL,
                        heartRateSamplesJson TEXT,
                        syncTimestamp INTEGER NOT NULL,
                        receivedAt INTEGER NOT NULL,
                        syncedToBackendAt INTEGER
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create paired_devices table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS paired_devices (
                        deviceId TEXT NOT NULL PRIMARY KEY,
                        deviceName TEXT NOT NULL,
                        alias TEXT,
                        boundNodeId TEXT,
                        connectionStatus TEXT NOT NULL,
                        lastSyncTimestamp INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_paired_devices_deviceId ON paired_devices(deviceId)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create location_points table for GPS tracking
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS location_points (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        address TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        activityType TEXT NOT NULL,
                        notes TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create index for timestamp queries (24h lookups)
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_location_points_timestamp 
                    ON location_points(timestamp)
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_diary_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .fallbackToDestructiveMigration() // Only for development
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Clear the database instance (useful for testing)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
