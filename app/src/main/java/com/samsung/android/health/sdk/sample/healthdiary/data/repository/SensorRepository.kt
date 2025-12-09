package com.samsung.android.health.sdk.sample.healthdiary.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.SensorDataEntity
import com.samsung.android.health.sdk.sample.healthdiary.wearable.model.SensorData
import com.samsung.android.health.sdk.sample.healthdiary.worker.UploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SensorRepository(private val context: Context) {

    private val sensorDataDao = AppDatabase.getDatabase(context).sensorDataDao()
    private val workManager = WorkManager.getInstance(context)

    suspend fun saveSensorData(data: SensorData) {
        withContext(Dispatchers.IO) {
            val entity = SensorDataEntity(
                deviceId = data.deviceId,
                timestamp = data.timestamp,
                x = data.values.getOrElse(0) { 0f },
                y = data.values.getOrElse(1) { 0f },
                z = data.values.getOrElse(2) { 0f },
                synced = false
            )
            sensorDataDao.insert(entity)
            scheduleUpload()
        }
    }

    suspend fun saveBatch(batch: List<SensorData>) {
        withContext(Dispatchers.IO) {
            val entities = batch.map { data ->
                SensorDataEntity(
                    deviceId = data.deviceId,
                    timestamp = data.timestamp,
                    x = data.values.getOrElse(0) { 0f },
                    y = data.values.getOrElse(1) { 0f },
                    z = data.values.getOrElse(2) { 0f },
                    synced = false
                )
            }
            sensorDataDao.insertAll(entities)
            scheduleUpload()
        }
    }

    suspend fun getUnsyncedCount(): Int {
        return withContext(Dispatchers.IO) {
            sensorDataDao.getUnsyncedCount()
        }
    }

    fun scheduleUpload() {
        // Use centralized scheduler to enforce 10s/5min interval
        com.samsung.android.health.sdk.sample.healthdiary.worker.UploadScheduler.scheduleNext(context)
    }
}
