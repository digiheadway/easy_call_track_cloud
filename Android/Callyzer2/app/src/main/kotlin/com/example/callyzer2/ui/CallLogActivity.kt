package com.example.callyzer2.ui

import android.content.Intent
import android.os.Bundle
import android.provider.CallLog
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.callyzer2.data.*
import com.example.callyzer2.databinding.ActivityCallLogBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CallLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallLogBinding
    private lateinit var database: AppDatabase
    private lateinit var callAdapter: CallAdapter
    private var calls = mutableListOf<Call>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupToolbar()
        setupRecyclerView()
        setupAddButton()
        loadCalls()
        syncCallLogs()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Call Logs"
    }

    private fun setupRecyclerView() {
        callAdapter = CallAdapter(calls) { call ->
            // Handle call click - show call details and add note option
            showCallDetails(call)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CallLogActivity)
            adapter = callAdapter
        }
    }

    private fun setupAddButton() {
        binding.fabAddCall.setOnClickListener {
            // For now, we'll sync call logs. In a real app, you might want a manual add option
            syncCallLogs()
        }
    }

    private fun loadCalls() {
        database.callDao().getAllCalls().observe(this, Observer<List<Call>> { callList ->
            calls.clear()
            calls.addAll(callList)
            callAdapter.notifyDataSetChanged()
            updateEmptyState()
        })
    }

    private fun syncCallLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Query the system call log
                val cursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION
                    ),
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
                )

                cursor?.use {
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                    val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                    val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

                    while (it.moveToNext()) {
                        val number = it.getString(numberIndex) ?: continue
                        val type = when (it.getInt(typeIndex)) {
                            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                            else -> CallType.MISSED
                        }
                        val date = it.getLong(dateIndex)
                        val duration = it.getLong(durationIndex)

                        // Create or find customer for this number
                        val customerId = findOrCreateCustomer(number)

                        // Create call record
                        val call = Call(
                            customerId = customerId,
                            phoneNumber = number,
                            callType = type,
                            duration = duration,
                            timestamp = date
                        )

                        database.callDao().insertCall(call)
                    }
                }
            } catch (e: SecurityException) {
                CoroutineScope(Dispatchers.Main).launch {
                    binding.root.post {
                        android.widget.Toast.makeText(
                            this@CallLogActivity,
                            "Permission denied to access call logs",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private suspend fun findOrCreateCustomer(phoneNumber: String): Long {
        // Try to find existing customer
        val existingCustomer = database.customerDao().findCustomerByPhoneSync(phoneNumber)
        if (existingCustomer != null) {
            return existingCustomer.id
        }

        // Create new customer from phone number
        val customer = Customer(
            name = "Unknown Contact",
            phoneNumber = phoneNumber,
            notes = "Auto-created from call log"
        )
        return database.customerDao().insertCustomer(customer)
    }

    private fun showCallDetails(call: Call) {
        val intent = Intent(this, CallDetailActivity::class.java)
        intent.putExtra("call_id", call.id)
        startActivity(intent)
    }

    private fun updateEmptyState() {
        if (calls.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadCalls() // Refresh data when returning to this activity
    }
}
