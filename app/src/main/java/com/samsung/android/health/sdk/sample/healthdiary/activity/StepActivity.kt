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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.StepActivityBinding
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants.currentDate
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants.minimumDate
import com.samsung.android.health.sdk.sample.healthdiary.utils.resolveException
import com.samsung.android.health.sdk.sample.healthdiary.utils.showDatePickerDialogueBox
import com.samsung.android.health.sdk.sample.healthdiary.utils.showErrorToast
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.StepViewModel
import com.samsung.android.sdk.health.data.data.AggregatedData
import kotlinx.coroutines.launch

class StepActivity : AppCompatActivity() {

    private lateinit var binding: StepActivityBinding
    private lateinit var stepViewModel: StepViewModel
    private var startDate = currentDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stepViewModel = ViewModelProvider(
            this, HealthViewModelFactory(this)
        )[StepViewModel::class.java]

        binding = DataBindingUtil
            .setContentView<StepActivityBinding>(this, R.layout.step)
            .apply {
                viewModel = stepViewModel
            }

        initializeOnClickListeners()
        setupLineChart()
        setStepDataObservers()
    }

    private fun initializeOnClickListeners() {
        binding.movePreviousDate.setOnClickListener {
            movePreviousDate()
        }

        binding.moveNextDate.setOnClickListener {
            moveNextDate()
        }

        binding.datePicker.setOnClickListener {
            showDatePickerDialogueBox(this@StepActivity, startDate) { newStartDate ->
                startDate = newStartDate
                if (startDate == minimumDate) {
                    binding.movePreviousDate.setColorFilter(getColor(R.color.disabled))
                } else {
                    binding.movePreviousDate.setColorFilter(getColor(R.color.black))
                }
                if (newStartDate == currentDate) {
                    binding.moveNextDate.setColorFilter(getColor(R.color.disabled))
                } else {
                    binding.moveNextDate.setColorFilter(getColor(R.color.black))
                }
                stepViewModel.readStepData(startDate)
            }
        }
    }

    private fun setupLineChart() {
        binding.stepsChart.apply {
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
                textColor = getColor(R.color.steps_text_secondary)
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
                textColor = getColor(R.color.steps_text_secondary)
                textSize = 10f
                setDrawGridLines(true)
                gridColor = getColor(R.color.steps_chart_grid)
                gridLineWidth = 0.5f
                setDrawAxisLine(false)
                setDrawZeroLine(false)
                axisMinimum = 0f
            }
            
            // Configure Y axis (right) - hide it
            axisRight.isEnabled = false
            
            // Configure legend - hide it
            legend.isEnabled = false
            
            // Animate
            animateX(1000)
        }
    }

    private fun transformDataToEntries(stepDataList: List<AggregatedData<Long>>): List<Entry> {
        val entries = mutableListOf<Entry>()
        val hourStepsMap = mutableMapOf<Int, Long>()
        
        // Initialize all 24 hours with 0
        for (hour in 0..23) {
            hourStepsMap[hour] = 0L
        }
        
        // Fill in actual data
        stepDataList.forEach { stepData ->
            val hour = stepData.getStartLocalDateTime().hour
            val steps = stepData.value as Long
            hourStepsMap[hour] = (hourStepsMap[hour] ?: 0L) + steps
        }
        
        // Create entries with accumulated steps
        var accumulatedSteps: Long = 0
        for (hour in 0..23) {
            val stepsInHour = hourStepsMap[hour] ?: 0L
            accumulatedSteps += stepsInHour
            entries.add(Entry(hour.toFloat(), accumulatedSteps.toFloat()))
        }
        
        return entries
    }

    private fun updateChart(stepDataList: List<AggregatedData<Long>>) {
        val entries = transformDataToEntries(stepDataList)
        
        if (entries.isEmpty()) {
            binding.stepsChart.data = null
            binding.stepsChart.invalidate()
            return
        }
        
        val dataSet = LineDataSet(entries, "").apply {
            color = getColor(R.color.steps_chart_line)
            lineWidth = 2.5f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawFilled(true)
            fillColor = getColor(R.color.steps_chart_line)
            fillAlpha = 20
        }
        
        val lineData = LineData(dataSet)
        binding.stepsChart.data = lineData
        binding.stepsChart.invalidate()
    }

    private fun setStepDataObservers() {
        /**  Update chart with steps data */
        stepViewModel.totalStepCountData.observe(this) { stepDataList ->
            updateChart(stepDataList)
        }

        /** Show toast on exception occurrence **/
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stepViewModel.exceptionResponse.collect { exception ->
                    if(exception.message != "Default"){
                        showErrorToast(this@StepActivity, exception)
                        resolveException(exception, this@StepActivity)
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (startDate == currentDate) {
            binding.moveNextDate.setColorFilter(getColor(R.color.disabled))
        }
        stepViewModel.readStepData(startDate)
    }

    override fun onStop() {
        super.onStop()
        stepViewModel.setDefaultValueToExceptionResponse()
    }

    private fun movePreviousDate() {
        if (startDate > minimumDate) {
            startDate = startDate.minusDays(1)
            if (startDate == minimumDate) {
                binding.movePreviousDate.setColorFilter(getColor(R.color.disabled))
            }
            binding.moveNextDate.setColorFilter(getColor(R.color.black))
            stepViewModel.readStepData(startDate)
        }
    }

    private fun moveNextDate() {
        if (startDate < currentDate) {
            startDate = startDate.plusDays(1)
            if (startDate == currentDate) {
                binding.moveNextDate.setColorFilter(getColor(R.color.disabled))
            }
            binding.movePreviousDate.setColorFilter(getColor(R.color.black))
            stepViewModel.readStepData(startDate)
        }
    }
}
