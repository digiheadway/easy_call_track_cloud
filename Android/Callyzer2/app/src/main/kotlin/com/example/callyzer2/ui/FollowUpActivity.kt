package com.example.callyzer2.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.callyzer2.databinding.ActivityPlaceholderBinding

class FollowUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceholderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceholderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Follow-ups"

        binding.message.text = "Follow-up management feature coming soon!"
    }
}
