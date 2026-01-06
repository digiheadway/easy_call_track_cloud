package com.example.deviceadmin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.example.deviceadmin.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        setupUI()
        startStatusWorker()
    }

    private fun setupUI() {
        binding.etDeviceId.setText(Utils.getDeviceId(this))
        binding.swStealthMode.isChecked = Utils.isStealthMode(this)

        binding.btnDeviceAdmin.setOnClickListener {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_description))
            startActivity(intent)
        }

        binding.btnAccessibility.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Setup Security Service")
                .setMessage("You will be taken to Settings.\n\n" +
                        "1. Scroll to the very bottom.\n" +
                        "2. Look for 'Downloaded apps' or 'Installed Services'.\n" +
                        "3. Tap on 'A1 Enterprise Protection' and turn it ON.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        binding.swStealthMode.setOnCheckedChangeListener { _, isChecked ->
            Utils.setStealthMode(this, isChecked)
            toggleStealthMode(isChecked)
        }

        binding.btnTestLock.setOnClickListener {
            LockActivity.start(this)
        }

        binding.btnRemoveProtection.setOnClickListener {
            showPinDialog()
        }

        binding.etDeviceId.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                Utils.setDeviceId(this, binding.etDeviceId.text.toString())
            }
        }
    }

    private fun toggleStealthMode(enabled: Boolean) {
        val packageManager = packageManager
        val mainActivity = ComponentName(this, MainActivity::class.java)
        val aliasActivity = ComponentName(this, MainActivityAlias::class.java)

        if (enabled) {
            packageManager.setComponentEnabledSetting(mainActivity, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
            packageManager.setComponentEnabledSetting(aliasActivity, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        } else {
            packageManager.setComponentEnabledSetting(mainActivity, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
            packageManager.setComponentEnabledSetting(aliasActivity, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        }
        Toast.makeText(this, "Stealth mode toggled. Restart launcher to see changes.", Toast.LENGTH_LONG).show()
    }

    private fun showPinDialog() {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        
        AlertDialog.Builder(this)
            .setTitle("Enter Master PIN")
            .setView(input)
            .setPositiveButton("Verify") { _, _ ->
                if (input.text.toString() == Utils.MASTER_PIN) {
                    removeProtection()
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeProtection() {
        Utils.setProtected(this, false)
        Utils.setFreezed(this, false)
        DevicePolicyHandler.applyEnterprisePolicies(this, dpm, adminComponent, false)
        dpm.removeActiveAdmin(adminComponent)
        Toast.makeText(this, "Protection removed. You can now uninstall the app.", Toast.LENGTH_LONG).show()
    }

    private fun startStatusWorker() {
        val workRequest = PeriodicWorkRequestBuilder<StatusWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "StatusSync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

// Dummy class for the alias target in manifest
class MainActivityAlias
