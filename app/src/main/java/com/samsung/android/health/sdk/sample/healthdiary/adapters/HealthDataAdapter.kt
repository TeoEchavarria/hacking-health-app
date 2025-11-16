package com.samsung.android.health.sdk.sample.healthdiary.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.health.sdk.sample.healthdiary.databinding.HealthDataRecordBinding
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricRecord

class HealthDataAdapter :
    RecyclerView.Adapter<HealthDataAdapter.HealthDataViewHolder>() {

    private val records: MutableList<HealthMetricRecord> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthDataViewHolder {
        val binding = HealthDataRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HealthDataViewHolder(binding)
    }

    override fun getItemCount(): Int = records.size

    override fun onBindViewHolder(holder: HealthDataViewHolder, position: Int) {
        holder.bind(records[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(data: List<HealthMetricRecord>) {
        records.clear()
        records.addAll(data)
        notifyDataSetChanged()
    }

    inner class HealthDataViewHolder(
        private val binding: HealthDataRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: HealthMetricRecord) {
            binding.metricTime.text = record.timeRange
            if (record.dataSourceName.isNullOrBlank()) {
                binding.metricSource.visibility = View.GONE
            } else {
                binding.metricSource.visibility = View.VISIBLE
                binding.metricSource.text = binding.root.context.getString(
                    R.string.data_source_label,
                    record.dataSourceName
                )
            }
            val values = record.fieldValues.joinToString(separator = "\n") { field ->
                "${field.label}: ${field.value}"
            }
            binding.metricValues.text = values
        }
    }
}



