package com.example.callyzer2.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.callyzer2.data.Customer
import com.example.callyzer2.databinding.ItemCustomerBinding

class CustomerAdapter(
    private val customers: List<Customer>,
    private val onCustomerClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customers[position])
    }

    override fun getItemCount() = customers.size

    inner class CustomerViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: Customer) {
            binding.apply {
                customerName.text = customer.name
                customerPhone.text = customer.phoneNumber
                customerEmail.text = customer.email ?: "No email"
                customerCompany.text = customer.company ?: "No company"

                if (customer.notes.isNullOrBlank()) {
                    notesLabel.visibility = View.GONE
                    customerNotes.visibility = View.GONE
                } else {
                    notesLabel.visibility = View.VISIBLE
                    customerNotes.visibility = View.VISIBLE
                    customerNotes.text = customer.notes
                }

                root.setOnClickListener { onCustomerClick(customer) }
            }
        }
    }
}
