package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.adapters.HealthDataAdapter
import com.samsung.android.health.sdk.sample.healthdiary.databinding.GenericHealthDataBinding
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants.currentDate
import com.samsung.android.health.sdk.sample.healthdiary.utils.AppConstants.minimumDate
import com.samsung.android.health.sdk.sample.healthdiary.utils.SwipeDetector
import com.samsung.android.health.sdk.sample.healthdiary.utils.SwipeDetector.OnSwipeEvent
import com.samsung.android.health.sdk.sample.healthdiary.utils.SwipeDetector.SwipeTypeEnum
import com.samsung.android.health.sdk.sample.healthdiary.utils.resolveException
import com.samsung.android.health.sdk.sample.healthdiary.utils.showDatePickerDialogueBox
import com.samsung.android.health.sdk.sample.healthdiary.utils.showErrorToast
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.BaseHealthDataViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory
import kotlinx.coroutines.launch

abstract class BaseHealthDataActivity<VM : BaseHealthDataViewModel<*>> : AppCompatActivity() {

    protected abstract val viewModelClass: Class<VM>
    protected abstract val titleRes: Int
    protected abstract val iconRes: Int

    protected lateinit var binding: GenericHealthDataBinding
    protected lateinit var adapter: HealthDataAdapter
    protected lateinit var viewModel: VM

    private var startDate = currentDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            HealthViewModelFactory(this)
        )[viewModelClass]

        binding = DataBindingUtil.setContentView(this, R.layout.generic_health_data)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.metricTitle.setText(titleRes)
        binding.metricIcon.setImageResource(iconRes)

        adapter = HealthDataAdapter()
        binding.metricList.layoutManager = LinearLayoutManager(this)
        binding.metricList.adapter = adapter

        initializeListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.readMetricData(startDate)
        if (startDate == currentDate) {
            binding.moveNextDate.setColorFilter(getColor(R.color.silver))
        } else {
            binding.moveNextDate.setColorFilter(getColor(R.color.black))
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.setDefaultValueToExceptionResponse()
    }

    private fun initializeListeners() {
        binding.movePreviousDate.setOnClickListener {
            movePreviousDate()
        }

        binding.moveNextDate.setOnClickListener {
            moveNextDate()
        }

        binding.datePicker.setOnClickListener {
            showDatePickerDialogueBox(this, startDate) { newStartDate ->
                startDate = newStartDate
                handleNavigationColors()
                viewModel.readMetricData(startDate)
            }
        }

        SwipeDetector(binding.metricList).setOnSwipeListener(object : OnSwipeEvent {
            override fun swipeEventDetected(swipeType: SwipeTypeEnum) {
                when (swipeType) {
                    SwipeTypeEnum.LEFT_TO_RIGHT -> movePreviousDate()
                    SwipeTypeEnum.RIGHT_TO_LEFT -> moveNextDate()
                    else -> Unit
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.records.observe(this) {
            adapter.submitList(it)
            updateEmptyStateVisibility(it.isEmpty(), null)
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exceptionResponse.collect { exception ->
                    if (exception.message != "Default") {
                        showErrorToast(this@BaseHealthDataActivity, exception)
                        resolveException(exception, this@BaseHealthDataActivity)
                    }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.statusMessage.collect { message ->
                    updateEmptyStateVisibility(adapter.itemCount == 0, message)
                }
            }
        }
    }

    private fun movePreviousDate() {
        if (startDate > minimumDate) {
            startDate = startDate.minusDays(1)
            handleNavigationColors()
            viewModel.readMetricData(startDate)
        }
    }

    private fun moveNextDate() {
        if (startDate < currentDate) {
            startDate = startDate.plusDays(1)
            handleNavigationColors()
            viewModel.readMetricData(startDate)
        }
    }

    private fun handleNavigationColors() {
        if (startDate == minimumDate) {
            binding.movePreviousDate.setColorFilter(getColor(R.color.silver))
        } else {
            binding.movePreviousDate.setColorFilter(getColor(R.color.black))
        }

        if (startDate == currentDate) {
            binding.moveNextDate.setColorFilter(getColor(R.color.silver))
        } else {
            binding.moveNextDate.setColorFilter(getColor(R.color.black))
        }
    }

    private fun updateEmptyStateVisibility(
        listEmpty: Boolean,
        message: String?
    ) {
        if (!message.isNullOrBlank()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.emptyState.text = message
            return
        }

        if (listEmpty) {
            binding.emptyState.visibility = View.VISIBLE
            binding.emptyState.text = getString(R.string.no_data)
        } else {
            binding.emptyState.visibility = View.GONE
        }
    }
}

