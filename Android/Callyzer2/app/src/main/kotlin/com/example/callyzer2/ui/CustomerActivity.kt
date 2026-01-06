package com.example.callyzer2.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.callyzer2.data.AppDatabase
import com.example.callyzer2.data.Customer
import com.example.callyzer2.databinding.ActivityCustomerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerBinding
    private lateinit var database: AppDatabase
    private lateinit var customerAdapter: CustomerAdapter
    private var customers = mutableListOf<Customer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupToolbar()
        setupRecyclerView()
        setupAddButton()
        loadCustomers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Customers"
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerAdapter(customers) { customer ->
            // Handle customer click - open customer detail
            val intent = Intent(this, CustomerDetailActivity::class.java)
            intent.putExtra("customer_id", customer.id)
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CustomerActivity)
            adapter = customerAdapter
        }
    }

    private fun setupAddButton() {
        binding.fabAddCustomer.setOnClickListener {
            val intent = Intent(this, AddEditCustomerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadCustomers() {
        database.customerDao().getAllCustomers().observe(this, Observer<List<Customer>> { customerList ->
            customers.clear()
            customers.addAll(customerList)
            customerAdapter.notifyDataSetChanged()
            updateEmptyState()
        })
    }

    private fun updateEmptyState() {
        if (customers.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadCustomers() // Refresh data when returning to this activity
    }
}
