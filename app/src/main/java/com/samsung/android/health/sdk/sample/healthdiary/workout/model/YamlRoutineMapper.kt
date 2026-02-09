package com.samsung.android.health.sdk.sample.healthdiary.workout.model

import com.samsung.android.health.sdk.sample.healthdiary.config.YamlExercise
import com.samsung.android.health.sdk.sample.healthdiary.config.YamlRoutine
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Maps rich YAML routines (exercise-level detail, wrist safety metadata)
 * into the simpler timer-based Routine model used by the workout player.
 *
 * Mapping rules:
 * - Each exercise becomes one WORK segment with its duration_sec.
 * - If rest_after_sec > 0, a REST segment is inserted immediately after it.
 * - Segment labels are taken from the exercise name (for WORK)
 *   and "Rest" (for REST) so the watch UI stays concise.
 */
object YamlRoutineMapper {

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /**
     * Build a Routine for immediate start (startAt = now).
     */
    fun mapToRoutineNow(yaml: YamlRoutine): Routine {
        val now = ZonedDateTime.now()
        val startAt = isoFormatter.format(now)
        val routineId = yaml.routine_id.ifBlank { UUID.randomUUID().toString() }
        return mapToRoutine(yaml, routineId = routineId, startAt = startAt)
    }

    /**
     * Build a Routine for a specific start time (ISO 8601).
     */
    fun mapToRoutine(
        yaml: YamlRoutine,
        routineId: String = yaml.routine_id.ifBlank { UUID.randomUUID().toString() },
        startAt: String
    ): Routine {
        val segments = yaml.exercises.flatMap { ex: YamlExercise ->
            val workSegment = Segment(
                type = SegmentType.WORK,
                label = ex.name.ifBlank { "Exercise" },
                durationSec = ex.duration_sec.coerceAtLeast(1)
            )

            if (ex.rest_after_sec > 0) {
                listOf(
                    workSegment,
                    Segment(
                        type = SegmentType.REST,
                        label = "Rest",
                        durationSec = ex.rest_after_sec.coerceAtLeast(1)
                    )
                )
            } else {
                listOf(workSegment)
            }
        }

        return Routine(
            routineId = routineId,
            startAt = startAt,
            segments = segments
        )
    }
}

