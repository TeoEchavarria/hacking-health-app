package com.samsung.android.health.sdk.sample.healthdiary.data.ingest

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReceivedBatchEntity::class], version = 1, exportSchema = false)
abstract class SensorIngestDatabase : RoomDatabase() {
    abstract fun ingestDao(): IngestDao

    companion object {
        @Volatile
        private var INSTANCE: SensorIngestDatabase? = null

        fun getDatabase(context: Context): SensorIngestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SensorIngestDatabase::class.java,
                    "sensor_ingest_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
