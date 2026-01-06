package com.example.deviceadmin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.deviceadmin.databinding.ActivityLockBinding

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private var breakTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.tvEmiAmount.text = "EMI Pending: ${Utils.getEmiAmount(this)}"
        binding.tvServerMessage.text = Utils.getServerMessage(this)

        binding.btnCheckStatus.setOnClickListener {
            checkStatusManually()
        }

        binding.btnEmergencyCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:112") // Default emergency or support number
            startActivity(intent)
        }

        binding.btnBreakRequest.setOnClickListener {
            startBreakTimer()
        }

        binding.tvMasterUnlock.setOnClickListener {
            showPinDialog()
        }
    }

    private fun checkStatusManually() {
        Toast.makeText(this, "Checking status...", Toast.LENGTH_SHORT).show()
        // Here you would trigger the worker immediately or make a direct network call
        // For simplicity, we'll just show a toast for now.
    }

    private fun startBreakTimer() {
        binding.btnBreakRequest.visibility = View.GONE
        binding.tvTimer.visibility = View.VISIBLE
        
        breakTimer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.tvTimer.text = "Break ends in: ${seconds}s"
            }

            override fun onFinish() {
                binding.btnBreakRequest.visibility = View.VISIBLE
                binding.tvTimer.visibility = View.GONE
                // After break, ensure activity is still on top
                reLockIfNecessary()
            }
        }.start()
        
        // Minimize the app or go home for 2 minutes
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    private fun reLockIfNecessary() {
        if (Utils.isFreezed(this)) {
            val intent = Intent(this, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
    }

    private fun showPinDialog() {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        
        AlertDialog.Builder(this)
            .setTitle("Admin Unlock")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                if (input.text.toString() == Utils.MASTER_PIN) {
                    unlock()
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun unlock() {
        Utils.setFreezed(this, false)
        finish()
    }

    override fun onBackPressed() {
        // Disable back button
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(intent)
        }
    }
}
