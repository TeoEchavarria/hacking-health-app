package com.samsung.android.health.sdk.sample.healthdiary.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.samsung.android.health.sdk.sample.healthdiary.databinding.ItemHealthMetricCardBinding
import com.samsung.android.health.sdk.sample.healthdiary.entries.HealthMetricCardUiState

class HealthMetricCardAdapter(
    private val listener: CardClickListener
) : RecyclerView.Adapter<HealthMetricCardAdapter.CardViewHolder>() {

    interface CardClickListener {
        fun onCardClicked(uiState: HealthMetricCardUiState)
    }

    private val items: MutableList<HealthMetricCardUiState> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemHealthMetricCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CardViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun submitList(data: List<HealthMetricCardUiState>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    inner class CardViewHolder(
        private val binding: ItemHealthMetricCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uiState: HealthMetricCardUiState) {
            binding.metricTitle.setText(uiState.definition.titleRes)
            binding.metricIcon.setImageResource(uiState.definition.iconRes)
            binding.metricValue.text = if (uiState.isLoading) {
                binding.root.context.getString(
                    com.samsung.android.health.sdk.sample.healthdiary.R.string.loading_value
                )
            } else {
                uiState.latestValue
            }
            binding.root.setOnClickListener {
                listener.onCardClicked(uiState)
            }
        }
    }
}

