package com.samsung.android.health.sdk.sample.healthdiary.utils

/**
 * Constants for testing Wearable Data Layer communication
 */
object TestConstants {
    // Wearable Data Layer Paths
    const val PATH_PING = "/ping"
    const val PATH_PONG = "/pong"
    const val PATH_WORKOUT_START = "/workout/start"
    const val PATH_WORKOUT_ACK = "/workout/ack"
    const val PATH_HABIT_REMINDER_START = "/habit/reminder/start"
    const val PATH_HABIT_REMINDER_DONE = "/habit/reminder/done"
    const val PATH_HABIT_REMINDER_POSTPONE = "/habit/reminder/postpone"
    const val PATH_HEALTH_DAILY = "/health/daily"
    const val PATH_HEALTH_HR = "/health/hr"
    const val PATH_SENSOR_BATCH = "/sensor_batch"
    const val PATH_STATE_PHONE = "/state/phone"
    const val PATH_STREAM_SENSORS = "/stream/sensors/v1"
    
    // Timeout values (milliseconds)
    const val TIMEOUT_PING_PONG = 200L
    const val TIMEOUT_WORKOUT_ACK = 500L
    const val TIMEOUT_WORKOUT_START = 2000L
    const val TIMEOUT_DEFAULT = 5000L
    
    // Test data ranges
    const val MIN_HEART_RATE = 40
    const val MAX_HEART_RATE = 220
    const val MIN_STEPS = 0
    const val MAX_STEPS = 100000
    
    // Batch sizes
    const val MIN_HR_BATCH_SIZE = 5
    const val SENSOR_BATCH_SIZE = 100
    const val PERFORMANCE_TEST_ITERATIONS = 1000
    
    // Test device IDs
    const val TEST_DEVICE_ID = "test-device-001"
    const val TEST_NODE_ID = "test-node-001"
    
    // Capability names
    const val CAPABILITY_SENSOR_DATA_SENDER = "sensor_data_sender"
    const val CAPABILITY_WEAR_HEALTH_SYNC = "wear_health_sync"
    
    // Protocol version
    const val PROTOCOL_VERSION = 2
    
    // Test routine IDs
    const val TEST_ROUTINE_ID = "test-routine-uuid-12345"
    const val TEST_SESSION_ID = "test-session-uuid-67890"
    const val TEST_ATTEMPT_ID = "test-attempt-uuid-abcde"
}
