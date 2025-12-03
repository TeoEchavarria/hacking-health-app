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
                type = data.type,
                timestamp = data.timestamp,
                values = data.values.joinToString(","),
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
                    type = data.type,
                    timestamp = data.timestamp,
                    values = data.values.joinToString(","),
                    synced = false
                )
            }
            sensorDataDao.insertAll(entities)
            scheduleUpload()
        }
    }

    private fun scheduleUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "UploadSensorData",
            ExistingWorkPolicy.KEEP, // Don't replace if already scheduled
            uploadWork
        )
    }
}
