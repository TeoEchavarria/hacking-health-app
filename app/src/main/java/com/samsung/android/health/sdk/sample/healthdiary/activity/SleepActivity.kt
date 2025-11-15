/*
 * Copyright (C) 2024 Samsung Electronics Co., Ltd. All rights reserved
 */
package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.SleepActivityBinding
import com.samsung.android.health.sdk.sample.healthdiary.adapters.SleepAdapter
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants.currentDate
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants.minimumDate
import com.samsung.android.health.sdk.sample.healthdiary.utils.SwipeDetector
import com.samsung.android.health.sdk.sample.healthdiary.utils.SwipeDetector.OnSwipeEvent
import com.samsung.android.health.sdk.sample.healthdiary.utils.SwipeDetector.SwipeTypeEnum
import com.samsung.android.health.sdk.sample.healthdiary.utils.resolveException
import com.samsung.android.health.sdk.sample.healthdiary.utils.showDatePickerDialogueBox
import com.samsung.android.health.sdk.sample.healthdiary.utils.showErrorToast
import com.samsung.android.health.sdk.sample.healthdiary.utils.showToast
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.SleepViewModel
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.SleepSession
import com.samsung.android.sdk.health.data.request.DataType
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class SleepActivity : AppCompatActivity() {

    private lateinit var sleepViewModel: SleepViewModel
    private lateinit var binding: SleepActivityBinding
    private lateinit var sleepAdapter: SleepAdapter
    private var startDate = currentDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sleepViewModel = ViewModelProvider(
            this, HealthViewModelFactory(this)
        )[SleepViewModel::class.java]

        sleepAdapter = SleepAdapter()

        binding = DataBindingUtil
            .setContentView<SleepActivityBinding>(this, R.layout.sleep)
            .apply {
                viewModel = sleepViewModel
                sleepList.layoutManager = LinearLayoutManager(this@SleepActivity)
                sleepList.adapter = sleepAdapter
            }

        initializeOnClickListeners()
        setupLineChart()
        setSwipeDetector()
        setSleepDataObservers()
    }

    private fun initializeOnClickListeners() {
        binding.movePreviousDate.setOnClickListener {
            movePreviousDate()
        }

        binding.moveNextDate.setOnClickListener {
            moveNextDate()
        }

        binding.datePicker.setOnClickListener {
            showDatePickerDialogueBox(this@SleepActivity, startDate) { newStartDate ->
                startDate = newStartDate
                if (startDate == minimumDate) {
                    binding.movePreviousDate.setColorFilter(getColor(R.color.silver))
                } else {
                    binding.movePreviousDate.setColorFilter(getColor(R.color.black))
                }
                if (newStartDate == currentDate) {
                    binding.moveNextDate.setColorFilter(getColor(R.color.silver))
                } else {
                    binding.moveNextDate.setColorFilter(getColor(R.color.black))
                }
                sleepViewModel.readSleepData(startDate)
            }
        }
    }

    private fun setupLineChart() {
        binding.sleepChart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            setDragEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            
            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = getColor(R.color.sleep_chart_text_secondary)
                textSize = 10f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f
                labelCount = 5
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val hour = value.toInt()
                        return when (hour) {
                            0 -> "00:00"
                            6 -> "06:00"
                            12 -> "12:00"
                            18 -> "18:00"
                            23 -> "24:00"
                            else -> ""
                        }
                    }
                }
            }
            
            // Configure Y axis (left)
            axisLeft.apply {
                textColor = getColor(R.color.sleep_chart_text_secondary)
                textSize = 10f
                setDrawGridLines(true)
                gridColor = getColor(R.color.sleep_chart_grid)
                gridLineWidth = 0.5f
                setDrawAxisLine(false)
                setDrawZeroLine(false)
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.1fh", value)
                    }
                }
                
                // Add 8-hour sleep goal line
                val goalLine = LimitLine(8f, getString(R.string.sleep_goal_8h)).apply {
                    lineColor = getColor(R.color.sleep_goal_line)
                    lineWidth = 2f
                    enableDashedLine(10f, 10f, 0f)
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                    textSize = 10f
                    textColor = getColor(R.color.sleep_goal_line)
                }
                addLimitLine(goalLine)
            }
            
            // Configure Y axis (right) - hide it
            axisRight.isEnabled = false
            
            // Configure legend with custom entries (only 4 stages)
            legend.apply {
                isEnabled = true
                orientation = Legend.LegendOrientation.HORIZONTAL
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                setDrawInside(false)
                textColor = getColor(R.color.sleep_chart_text_secondary)
                textSize = 10f
                formSize = 8f
                formToTextSpace = 4f
                xEntrySpace = 8f
                yEntrySpace = 4f
                
                // Create custom legend entries for the 4 sleep stages
                setCustom(
                    listOf(
                        LegendEntry(
                            getString(R.string.stage_awake),
                            Legend.LegendForm.SQUARE,
                            8f,
                            8f,
                            null,
                            getColor(R.color.sleep_stage_awake)
                        ),
                        LegendEntry(
                            getString(R.string.stage_rem),
                            Legend.LegendForm.SQUARE,
                            8f,
                            8f,
                            null,
                            getColor(R.color.sleep_stage_rem)
                        ),
                        LegendEntry(
                            getString(R.string.stage_light),
                            Legend.LegendForm.SQUARE,
                            8f,
                            8f,
                            null,
                            getColor(R.color.sleep_stage_light)
                        ),
                        LegendEntry(
                            getString(R.string.stage_deep),
                            Legend.LegendForm.SQUARE,
                            8f,
                            8f,
                            null,
                            getColor(R.color.sleep_stage_deep)
                        )
                    )
                )
            }
            
            // Animate
            animateX(1000)
        }
    }

    private fun setSwipeDetector() {
        SwipeDetector(binding.sleepList).setOnSwipeListener(object : OnSwipeEvent {
            override fun swipeEventDetected(
                swipeType: SwipeTypeEnum
            ) {
                if (swipeType == SwipeTypeEnum.LEFT_TO_RIGHT) {
                    movePreviousDate()
                } else if (swipeType == SwipeTypeEnum.RIGHT_TO_LEFT) {
                    moveNextDate()
                }
            }
        })
    }

    private fun getStageColor(stageType: DataType.SleepType.StageType): Int {
        return when (stageType) {
            DataType.SleepType.StageType.AWAKE -> getColor(R.color.sleep_stage_awake)
            DataType.SleepType.StageType.REM -> getColor(R.color.sleep_stage_rem)
            DataType.SleepType.StageType.LIGHT -> getColor(R.color.sleep_stage_light)
            DataType.SleepType.StageType.DEEP -> getColor(R.color.sleep_stage_deep)
            else -> getColor(R.color.sleep_stage_light)
        }
    }

    private fun updateChart(sleepDataList: List<HealthDataPoint>) {
        if (sleepDataList.isEmpty()) {
            binding.sleepChart.data = null
            binding.sleepChart.invalidate()
            return
        }
        
        // Process all sleep sessions to calculate minutes per hour per stage
        val hourMinutesMap = mutableMapOf<Int, MutableMap<DataType.SleepType.StageType, Int>>()
        
        for (hour in 0..23) {
            hourMinutesMap[hour] = mutableMapOf(
                DataType.SleepType.StageType.AWAKE to 0,
                DataType.SleepType.StageType.REM to 0,
                DataType.SleepType.StageType.LIGHT to 0,
                DataType.SleepType.StageType.DEEP to 0
            )
        }
        
        sleepDataList.forEach { sleepData ->
            val zoneOffset = sleepData.zoneOffset
            val sleepSessionList = sleepData.getValue(DataType.SleepType.SESSIONS) as? List<SleepSession>
            
            sleepSessionList?.forEach { session ->
                session.stages?.forEach { stage ->
                    val startLocal = LocalDateTime.ofInstant(stage.startTime, zoneOffset)
                    val endLocal = LocalDateTime.ofInstant(stage.endTime, zoneOffset)
                    val durationSeconds = (stage.endTime.epochSecond - stage.startTime.epochSecond).toInt()
                    val durationMinutes = durationSeconds / 60
                    
                    val stageType = stage.stage
                    if (stageType == DataType.SleepType.StageType.UNDEFINED) {
                        return@forEach
                    }
                    
                    // Distribute minutes across hours
                    var currentTime = startLocal
                    while (currentTime.isBefore(endLocal)) {
                        val currentHour = currentTime.hour
                        val hourEnd = currentTime.toLocalDate().atStartOfDay().plusHours((currentHour + 1).toLong())
                        val segmentEnd = if (endLocal.isBefore(hourEnd)) endLocal else hourEnd
                        val segmentSeconds = java.time.Duration.between(currentTime, segmentEnd).seconds.toInt()
                        val segmentMinutes = segmentSeconds / 60
                        
                        val currentHourMinutes = hourMinutesMap[currentHour]?.get(stageType) ?: 0
                        hourMinutesMap[currentHour]?.set(stageType, currentHourMinutes + segmentMinutes)
                        
                        currentTime = segmentEnd
                    }
                }
            }
        }
        
        // Calculate accumulated values per hour and determine predominant stage
        // Convert to hours for better clarity
        var accumulatedHours = 0f
        val allEntries = mutableListOf<Entry>()
        val hourStageMap = mutableMapOf<Int, DataType.SleepType.StageType>()
        
        // Create entries for all 24 hours
        for (hour in 0..23) {
            val hourStages = hourMinutesMap[hour] ?: emptyMap()
            val totalHourMinutes = hourStages.values.sum()
            val totalHourHours = totalHourMinutes / 60f // Convert minutes to hours
            accumulatedHours += totalHourHours
            
            if (totalHourMinutes > 0) {
                // Find predominant stage type
                val predominantStage = hourStages.maxByOrNull { it.value }?.key 
                    ?: DataType.SleepType.StageType.LIGHT
                hourStageMap[hour] = predominantStage
            } else {
                // No sleep data for this hour, use previous stage or default
                if (hour > 0) {
                    hourStageMap[hour] = hourStageMap[hour - 1] ?: DataType.SleepType.StageType.LIGHT
                } else {
                    hourStageMap[hour] = DataType.SleepType.StageType.LIGHT
                }
            }
            
            allEntries.add(Entry(hour.toFloat(), accumulatedHours))
        }
        
        if (allEntries.isEmpty() || allEntries.all { it.y == 0f }) {
            binding.sleepChart.data = null
            binding.sleepChart.invalidate()
            return
        }
        
        // Create segments for continuous ranges of the same stage type
        // Strategy: Each segment ends at transition point, next segment starts at same point
        // This ensures continuity - both segments share the transition point
        val dataSets = mutableListOf<LineDataSet>()
        var currentSegmentStart = 0
        var currentStage = hourStageMap[0] ?: DataType.SleepType.StageType.LIGHT
        
        for (hour in 1..23) {
            val stage = hourStageMap[hour] ?: DataType.SleepType.StageType.LIGHT
            
            // If stage changed, finalize previous segment and start new one
            if (stage != currentStage) {
                // Get the entry at the transition point
                val transitionEntry = allEntries[hour]
                
                // Create segment from currentSegmentStart to transition hour (inclusive)
                if (currentSegmentStart <= hour) {
                    val segmentEntries = mutableListOf<Entry>()
                    
                    // Add all hours from start to transition hour (inclusive)
                    for (h in currentSegmentStart..hour) {
                        val entry = allEntries[h]
                        segmentEntries.add(Entry(entry.x, entry.y))
                    }
                    
                    if (segmentEntries.size >= 2) {
                        val dataSet = LineDataSet(segmentEntries, "").apply {
                            color = getStageColor(currentStage)
                            lineWidth = 2.5f
                            setDrawCircles(false)
                            setDrawValues(false)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                            cubicIntensity = 0.2f
                            setDrawFilled(true)
                            fillColor = getStageColor(currentStage)
                            fillAlpha = 20
                        }
                        
                        dataSets.add(dataSet)
                    }
                }
                
                // Start new segment from the transition hour (will include it as first point)
                currentSegmentStart = hour
                currentStage = stage
            }
        }
        
        // Create final segment from currentSegmentStart to end
        if (currentSegmentStart <= 23) {
            val segmentEntries = mutableListOf<Entry>()
            
            for (h in currentSegmentStart..23) {
                val entry = allEntries[h]
                segmentEntries.add(Entry(entry.x, entry.y))
            }
            
            if (segmentEntries.size >= 2) {
                val dataSet = LineDataSet(segmentEntries, "").apply {
                    color = getStageColor(currentStage)
                    lineWidth = 2.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    cubicIntensity = 0.2f
                    setDrawFilled(true)
                    fillColor = getStageColor(currentStage)
                    fillAlpha = 20
                }
                
                dataSets.add(dataSet)
            }
        }
        
        if (dataSets.isEmpty()) {
            binding.sleepChart.data = null
            binding.sleepChart.invalidate()
            return
        }
        
        val lineData = LineData(dataSets as List<ILineDataSet>)
        binding.sleepChart.data = lineData
        binding.sleepChart.invalidate()
    }

    private fun setSleepDataObservers() {
        /**  Update sleep UI */
        sleepViewModel.dailySleepData.observe(this) {
            sleepAdapter.updateList(it)
            updateChart(it)
        }

        /**  Update sleep Associate UI */
        sleepViewModel.associatedData.observe(this) {
            sleepAdapter.updateAssociatedList(it)
        }

        /** Show toast on exception occurrence **/
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sleepViewModel.exceptionResponse.collect { exception ->
                    if(exception.message != "Default"){
                        showErrorToast(this@SleepActivity, exception)
                        resolveException(exception, this@SleepActivity)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (startDate == currentDate) {
            binding.moveNextDate.setColorFilter(getColor(R.color.silver))
        }
        sleepViewModel.readSleepData(startDate)
    }

    override fun onStop() {
        super.onStop()
        sleepViewModel.setDefaultValueToExceptionResponse()
    }

    private fun movePreviousDate() {
        if (startDate > minimumDate) {
            startDate = startDate.minusDays(1)
            if (startDate == minimumDate) {
                binding.movePreviousDate.setColorFilter(getColor(R.color.silver))
            }
            binding.moveNextDate.setColorFilter(getColor(R.color.black))
            sleepViewModel.readSleepData(startDate)
        }
    }

    private fun moveNextDate() {
        if (startDate < currentDate) {
            startDate = startDate.plusDays(1)
            if (startDate == currentDate) {
                binding.moveNextDate.setColorFilter(getColor(R.color.silver))
            }
            binding.movePreviousDate.setColorFilter(getColor(R.color.black))
            sleepViewModel.readSleepData(startDate)
        }
    }
}
