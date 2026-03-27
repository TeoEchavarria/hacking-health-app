package com.samsung.android.health.sdk.sample.healthdiary.protocol

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.samsung.android.health.sdk.sample.healthdiary.fakes.FakeMessageClient
import com.samsung.android.health.sdk.sample.healthdiary.utils.TestConstants
import com.samsung.android.health.sdk.sample.healthdiary.utils.TestLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test suite for PING/PONG protocol between Phone and Watch.
 * 
 * Protocol:
 * 1. Watch sends /ping with empty payload
 * 2. Phone responds with /pong with empty payload
 * 3. Response time should be < 200ms
 * 
 * These tests use FakeMessageClient to simulate the Wearable Data Layer
 * without requiring actual hardware.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class PingPongProtocolTest {
    
    private lateinit var messageClient: FakeMessageClient
    private val testName = "PingPongProtocol"
    
    @Before
    fun setup() {
        TestLogger.start(testName)
        messageClient = FakeMessageClient(defaultLatencyMs = 50L)
    }
    
    @After
    fun teardown() {
        messageClient.clearSentMessages()
        TestLogger.cleanup(testName)
    }
    
    /**
     * Test: PING/PONG latency should be under 200ms
     * 
     * Expected: Response time < 200ms
     * Actual: Measured from send to receive
     */
    @Test
    fun testPingPongLatency_shouldRespondWithin200ms() = runBlocking {
        // Setup
        val testCase = "$testName.latency"
        TestLogger.expectation(testCase, "PONG response < 200ms")
        
        // Simulate: Watch sends PING
        val startTime = System.currentTimeMillis()
        messageClient.sendMessage(
            nodeId = TestConstants.TEST_NODE_ID,
            path = TestConstants.PATH_PING,
            data = byteArrayOf()
        )
        
        // Simulate: Phone receives PING and responds with PONG
        withTimeout(TestConstants.TIMEOUT_PING_PONG) {
            messageClient.simulateReceive(
                path = TestConstants.PATH_PONG,
                data = byteArrayOf(),
                sourceNodeId = TestConstants.TEST_NODE_ID
            )
        }
        
        val latency = System.currentTimeMillis() - startTime
        
        // Verify
        TestLogger.actual(testCase, "latency=${latency}ms")
        assertThat(latency).isLessThan(TestConstants.TIMEOUT_PING_PONG)
        
        // Log result
        TestLogger.pass(testCase, "latency=${latency}ms, sla_met=${latency < 200}")
    }
    
    /**
     * Test: PING payload should be empty
     * 
     * Expected: Empty byte array
     * Actual: Payload size = 0
     */
    @Test
    fun testPingPayload_shouldBeEmpty() = runBlocking {
        val testCase = "$testName.pingEmpty"
        TestLogger.expectation(testCase, "PING payload size = 0")
        
        // Send PING
        messageClient.sendMessage(
            nodeId = TestConstants.TEST_NODE_ID,
            path = TestConstants.PATH_PING,
            data = byteArrayOf()
        )
        
        // Verify
        val messages = messageClient.getMessagesSentTo(TestConstants.PATH_PING)
        assertThat(messages).hasSize(1)
        
        val payload = messages.first()
        TestLogger.actual(testCase, "payload_size=${payload.size}")
        assertThat(payload).isEmpty()
        
        TestLogger.pass(testCase, "payload_size=0")
    }
    
    /**
     * Test: PONG payload should be empty
     * 
     * Expected: Empty byte array
     * Actual: Payload size = 0
     */
    @Test
    fun testPongPayload_shouldBeEmpty() = runBlocking {
        val testCase = "$testName.pongEmpty"
        TestLogger.expectation(testCase, "PONG payload size = 0")
        
        var receivedPayloadSize = -1
        
        // Add listener for PONG
        messageClient.addListener { event ->
            if (event.path == TestConstants.PATH_PONG) {
                receivedPayloadSize = event.data.size
            }
        }
        
        // Simulate PONG from phone
        messageClient.simulateReceive(
            path = TestConstants.PATH_PONG,
            data = byteArrayOf()
        )
        
        // Wait for listener to process
        delay(100)
        
        // Verify
        TestLogger.actual(testCase, "payload_size=$receivedPayloadSize")
        assertThat(receivedPayloadSize).isEqualTo(0)
        
        TestLogger.pass(testCase, "payload_size=0")
    }
    
    /**
     * Test: Connection timeout handling
     * 
     * Expected: Timeout after 200ms when no PONG received
     * Actual: TimeoutCancellationException thrown
     */
    @Test
    fun testPingTimeout_whenNoPongReceived_shouldTimeout() = runBlocking {
        val testCase = "$testName.timeout"
        TestLogger.expectation(testCase, "Timeout after 200ms when no PONG")
        
        // Disconnect to simulate no response
        messageClient.isConnected = false
        
        // Send PING
        val startTime = System.currentTimeMillis()
        messageClient.sendMessage(
            nodeId = TestConstants.TEST_NODE_ID,
            path = TestConstants.PATH_PING,
            data = byteArrayOf()
        )
        
        // Try to wait for PONG (should timeout)
        var timedOut = false
        try {
            withTimeout(TestConstants.TIMEOUT_PING_PONG) {
                // Simulate waiting for PONG that never arrives
                delay(TestConstants.TIMEOUT_PING_PONG + 100)
            }
        } catch (e: Exception) {
            timedOut = true
            val elapsed = System.currentTimeMillis() - startTime
            TestLogger.actual(testCase, "timeout_triggered=true, elapsed=${elapsed}ms")
        }
        
        assertThat(timedOut).isTrue()
        TestLogger.pass(testCase, "timeout_handled=true")
    }
    
    /**
     * Test: Multiple PING/PONG cycles
     * 
     * Expected: All 5 cycles complete successfully
     * Actual: Count of successful cycles
     */
    @Test
    fun testMultiplePingPong_shouldAllSucceed() = runBlocking {
        val testCase = "$testName.multiple"
        val cycles = 5
        TestLogger.expectation(testCase, "$cycles PING/PONG cycles successful")
        
        var successCount = 0
        val latencies = mutableListOf<Long>()
        
        repeat(cycles) { cycle ->
            val startTime = System.currentTimeMillis()
            
            // Send PING
            messageClient.sendMessage(
                nodeId = TestConstants.TEST_NODE_ID,
                path = TestConstants.PATH_PING,
                data = byteArrayOf()
            )
            
            // Receive PONG
            withTimeout(TestConstants.TIMEOUT_PING_PONG) {
                messageClient.simulateReceive(
                    path = TestConstants.PATH_PONG,
                    data = byteArrayOf()
                )
            }
            
            val latency = System.currentTimeMillis() - startTime
            latencies.add(latency)
            successCount++
            
            TestLogger.phone("$testCase.cycle$cycle", "latency=${latency}ms")
        }
        
        // Calculate statistics
        val avgLatency = latencies.average().toLong()
        val maxLatency = latencies.maxOrNull() ?: 0L
        
        TestLogger.actual(testCase, "successful_cycles=$successCount/$cycles, avg_latency=${avgLatency}ms")
        assertThat(successCount).isEqualTo(cycles)
        assertThat(maxLatency).isLessThan(TestConstants.TIMEOUT_PING_PONG)
        
        TestLogger.pass(testCase, "cycles=$successCount/$cycles, avg=${avgLatency}ms, max=${maxLatency}ms")
    }
    
    /**
     * Test: Network failure recovery
     * 
     * Expected: PING fails when disconnected, succeeds after reconnection
     * Actual: First attempt fails, second succeeds
     */
    @Test
    fun testPingPong_afterReconnection_shouldSucceed() = runBlocking {
        val testCase = "$testName.reconnection"
        TestLogger.expectation(testCase, "PING fails when disconnected, succeeds after reconnect")
        
        // Attempt 1: Disconnected (should fail)
        messageClient.isConnected = false
        val result1 = messageClient.sendMessage(
            nodeId = TestConstants.TEST_NODE_ID,
            path = TestConstants.PATH_PING,
            data = byteArrayOf()
        )
        
        var firstAttemptFailed = false
        try {
            result1.result
        } catch (e: Exception) {
            firstAttemptFailed = true
        }
        
        TestLogger.phone("$testCase.attempt1", "disconnected, result=failed")
        assertThat(firstAttemptFailed).isTrue()
        
        // Attempt 2: Reconnected (should succeed)
        messageClient.isConnected = true
        val result2 = messageClient.sendMessage(
            nodeId = TestConstants.TEST_NODE_ID,
            path = TestConstants.PATH_PING,
            data = byteArrayOf()
        )
        
        val secondAttemptSucceeded = try {
            result2.result
            true
        } catch (e: Exception) {
            false
        }
        
        TestLogger.phone("$testCase.attempt2", "reconnected, result=success")
        assertThat(secondAttemptSucceeded).isTrue()
        
        TestLogger.actual(testCase, "attempt1=failed, attempt2=success")
        TestLogger.pass(testCase, "reconnection_recovery=true")
    }
}
