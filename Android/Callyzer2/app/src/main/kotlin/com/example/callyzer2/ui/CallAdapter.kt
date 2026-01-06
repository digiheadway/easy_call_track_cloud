package com.example.callyzer2.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.callyzer2.data.Call
import com.example.callyzer2.data.CallType
import com.example.callyzer2.databinding.ItemCallBinding
import java.text.SimpleDateFormat
import java.util.*

class CallAdapter(
    private val calls: List<Call>,
    private val onCallClick: (Call) -> Unit
) : RecyclerView.Adapter<CallAdapter.CallViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val binding = ItemCallBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(calls[position])
    }

    override fun getItemCount() = calls.size

    inner class CallViewHolder(private val binding: ItemCallBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(call: Call) {
            binding.apply {
                callPhoneNumber.text = call.phoneNumber

                // Set call type color for phone number
                when (call.callType) {
                    CallType.INCOMING -> {
                        callPhoneNumber.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                    }
                    CallType.OUTGOING -> {
                        callPhoneNumber.setTextColor(root.context.getColor(android.R.color.holo_blue_dark))
                    }
                    CallType.MISSED -> {
                        callPhoneNumber.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
                    }
                }

                callTimestamp.text = dateFormat.format(Date(call.timestamp))

                // Show duration for completed calls
                if (call.duration > 0) {
                    val minutes = call.duration / 60
                    val seconds = call.duration % 60
                    callDuration.text = String.format("%02d:%02d", minutes, seconds)
                    callDuration.visibility = View.VISIBLE
                } else {
                    callDuration.visibility = View.GONE
                }

                // Show follow-up indicator
                followUpIndicator.text = if (call.followUpRequired) "📋" else ""
                followUpIndicator.visibility = if (call.followUpRequired) View.VISIBLE else View.GONE

                // Show notes if available
                if (!call.notes.isNullOrBlank()) {
                    callNotes.text = call.notes
                    callNotes.visibility = View.VISIBLE
                    notesLabel.visibility = View.VISIBLE
                } else {
                    callNotes.visibility = View.GONE
                    notesLabel.visibility = View.GONE
                }

                root.setOnClickListener { onCallClick(call) }
            }
        }
    }
}
