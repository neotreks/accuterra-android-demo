package com.neotreks.accuterra.mobile.demo.trip

import android.content.ContentValues
import android.content.Context
import com.neotreks.accuterra.mobile.demo.util.FileLogger
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.model.Result
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryModel
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryModelBuilder
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryRecordType
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryValuesBuilder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecording
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import com.neotreks.accuterra.mobile.sdk.trip.service.ITripRecordingService
import com.neotreks.accuterra.mobile.sdk.util.NeoStopWatch
import java.util.*
import kotlin.random.Random

/**
 * Tests related to recording telemetry with the trip recording
 */
class TestTelemetryRecording(private val _context: Context) {

    private lateinit var recorder: ITripRecorder
    private lateinit var service: ITripRecordingService

    private val driverId = "test user"
    private val vehicleId = "test vehicle"
    private val telemetryTypeName = "locations"

    suspend fun setup() {
        recorder = ServiceFactory.getTripRecorder(getContext())
        service = ServiceFactory.getTripRecordingService(getContext())
    }

    suspend fun testLogTelemetry(runFromScope: String): Result<String> {
        setup()
        var activeTripRecording: TripRecording?
        val logger = MessageLogger(getContext(), "log-test-telemetry-recording.txt")
        var testTripUuid: String? = null
        try {
            // Finish existing recording if exists
            var watch = NeoStopWatch.newMeasurement()
            val telemetryModel: TelemetryModel
            activeTripRecording = recorder.getActiveTripRecording()
            if (activeTripRecording != null) {
                recorder.finishTripRecording()
            }
            // Get or create active trip
            activeTripRecording = recorder.getActiveTripRecording()
            // Get the new recording
            if (activeTripRecording == null) {
                telemetryModel = buildTestTelemetryModel()
                val result = recorder.startTripRecording("Testing trip",
                    driverId = driverId ,
                    vehicleId = vehicleId,
                    telemetryModel = telemetryModel,
                )
                if (result.isFailure) {
                    throw IllegalStateException("Recording not started")
                }
                testTripUuid = result.value?.tripUuid
            } else {
                throw IllegalStateException("We need a new recording!")
            }
            watch.stop()

            logger.logMessage("----------------------------------")
            logger.logMessage("Testing Telemetry Recording: ${Date()}")
            logger.logMessage("Trip and Model initialization time: ${watch.getDurationString()}")
            logger.logMessage("")
            logger.logMessage("Run from coroutine scope: $runFromScope")

            // Record count
            val recCount = 50_000
            // Same random for all
            val random = Random(5_000)

            // Log location data
            val recordType = telemetryModel.recordTypes.find { it.name == telemetryTypeName }
                ?: throw IllegalStateException("Model does not contail recordType: $telemetryTypeName")
            // Do first insert to open the DB
            recorder.logTelemetry(recordType, buildTestTelemetryValues(recordType, random))
            // Start measurement after first insert
            watch = NeoStopWatch.newMeasurement()
            for (i in 1..recCount) {
                val values = buildTestTelemetryValues(recordType, random)
                recorder.logTelemetry(recordType, values)
            }
            watch.stop()
            logger.logMessage("Set of [$recCount] location data logged in: ${watch.getDurationString()}")

            // Pause
            recorder.pauseTripRecording()
            // Finish
            recorder.finishTripRecording()

            logger.logMessage("")
            logger.flushFileLogger()
            return Result.buildSuccess(logger.fileLogger.getLogAsList().joinToString(separator = "\n"))
        } catch (e: Exception) {
            logger.flushFileLogger()
            logger.logMessage(e.localizedMessage ?: "Unknown error")
            return Result.buildError(e)
        } finally {
            testTripUuid?.let { uuid ->
                service.deleteTripRecording(uuid)
            }
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun getContext(): Context {
        return _context
    }

    private fun buildTestTelemetryModel(): TelemetryModel {

        val builder  = TelemetryModelBuilder.create("test_telemetry", "neo", 0)
        builder.addRecordType(telemetryTypeName)
            .addDoubleField("lat", required = true)
            .addDoubleField("lon", required = true)
            .addDoubleField("alt", required = false)

        return builder.build()
    }

    private fun buildTestTelemetryValues(recordType: TelemetryRecordType, random: Random): ContentValues {
        val builder = TelemetryValuesBuilder.create(recordType, System.currentTimeMillis())
            .put("lat", random.nextDouble())
            .put("lon", random.nextDouble())
            .put("alt", random.nextDouble())
        return builder.build()
    }

    private class MessageLogger(context: Context, fileName: String) {

        val fileLogger = FileLogger(context, fileName)
        val fileBuffer = StringBuilder()
        val stringLogger = StringBuilder()

        fun logMessage(message: String) {
            fileBuffer.appendLine(message)
            stringLogger.appendLine(message)
        }

        fun flushFileLogger() {
            fileLogger.log(fileBuffer.toString())
            fileBuffer.clear()
        }

    }

}