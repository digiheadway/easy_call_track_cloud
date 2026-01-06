package com.example.callyzer2.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.callyzer2.R
import com.example.callyzer2.data.AppDatabase
import com.example.callyzer2.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        // Check permissions
        if (!hasAllPermissions()) {
            requestPermissions()
        } else {
            setupUI()
        }
    }

    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    private fun requestPermissions() {
        permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            setupUI()
        } else {
            showPermissionExplanationDialog()
        }
    }

    private fun showPermissionExplanationDialog() {
        val deniedPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        val permissionExplanations = deniedPermissions.map { permission ->
            when (permission) {
                Manifest.permission.READ_PHONE_STATE -> "📞 Phone State: Access call information and phone number"
                Manifest.permission.READ_CALL_LOG -> "📞 Call Logs: Read your call history to track business calls"
                Manifest.permission.READ_CONTACTS -> "👥 Contacts: Access contact information for customer management"
                else -> "Unknown permission"
            }
        }

        val explanationMessage = buildString {
            append("This app needs the following permissions to provide full business management functionality:\n\n")
            permissionExplanations.forEach { explanation ->
                append("• $explanation\n")
            }
            append("\nWithout these permissions, the app's core features will be limited.")
        }

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(explanationMessage)
            .setCancelable(false)
            .setPositiveButton("Grant Permissions") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Exit App") { _, _ ->
                finish()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
        // Finish the activity so user has to restart with proper permissions
        finish()
    }

    private fun setupUI() {
        setupNavigation()
        updateDashboardStats()
    }

    private fun updateDashboardStats() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val customerCount = database.customerDao().getAllCustomersSync().size
                val callCount = database.callDao().getAllCallsSync().size
                val followUpCountValue = database.callDao().getCallsRequiringFollowUpSync().size

                CoroutineScope(Dispatchers.Main).launch {
                    binding.apply {
                        customersCount.text = customerCount.toString()
                        callsCount.text = callCount.toString()
                        // Use the correct field name from the layout
                        val followUpTextView = findViewById<TextView>(R.id.follow_up_count)
                        followUpTextView.text = followUpCountValue.toString()
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    // Handle error - set default values
                    binding.apply {
                        customersCount.text = "0"
                        callsCount.text = "0"
                        val followUpTextView = findViewById<TextView>(R.id.follow_up_count)
                        followUpTextView.text = "0"
                    }
                }
            }
        }
    }

    private fun setupNavigation() {
        binding.apply {
            customersCard.setOnClickListener {
                startActivity(Intent(this@MainActivity, CustomerActivity::class.java))
            }

            callsCard.setOnClickListener {
                startActivity(Intent(this@MainActivity, CallLogActivity::class.java))
            }

            followUpCard.setOnClickListener {
                startActivity(Intent(this@MainActivity, FollowUpActivity::class.java))
            }

            reportsCard.setOnClickListener {
                startActivity(Intent(this@MainActivity, ReportsActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permissions again when resuming (user might have granted them in settings)
        if (hasAllPermissions()) {
            setupUI()
        } else {
            // Show permission dialog if permissions are still not granted
            showPermissionExplanationDialog()
        }
    }

    private fun openDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        documentPickerLauncher.launch(intent)
    }

    private fun createDocumentPicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "callyzer_backup.json")
        }
        documentCreateLauncher.launch(intent)
    }

    private val documentPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle imported file
                handleImportedFile(uri)
            }
        }
    }

    private val documentCreateLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle file creation for export
                exportDataToFile(uri)
            }
        }
    }

    private fun handleImportedFile(uri: Uri) {
        // Handle file import logic here
        // This could be JSON import, CSV import, etc.
        showToast("Import functionality coming soon!")
    }

    private fun exportDataToFile(uri: Uri) {
        // Handle file export logic here
        // This could export database data as JSON, CSV, etc.
        showToast("Export functionality coming soon!")
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
