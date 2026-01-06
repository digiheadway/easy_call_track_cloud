package com.example.callyzer2.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.callyzer2.databinding.ActivityPlaceholderBinding

class CallDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceholderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceholderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Call Details"

        binding.message.text = "Call detail view coming soon!"
    }
}
