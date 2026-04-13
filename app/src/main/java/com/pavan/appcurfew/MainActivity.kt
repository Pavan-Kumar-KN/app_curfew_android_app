package com.pavan.appcurfew

import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: BedtimePrefs
    private lateinit var enableSwitch: MaterialSwitch
    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var selectAppsButton: Button
    private lateinit var accessibilityButton: Button
    private lateinit var whitelistSummary: TextView
    private var accessibilityPromptShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        prefs = BedtimePrefs(this)
        enableSwitch = findViewById(R.id.switchEnableBlocking)
        startTimeButton = findViewById(R.id.buttonStartTime)
        endTimeButton = findViewById(R.id.buttonEndTime)
        selectAppsButton = findViewById(R.id.buttonSelectApps)
        accessibilityButton = findViewById(R.id.buttonAccessibilitySettings)
        whitelistSummary = findViewById(R.id.textWhitelistSummary)

        enableSwitch.isChecked = prefs.isBlockingEnabled()
        startTimeButton.text = formatMinutes(prefs.getStartMinutes())
        endTimeButton.text = formatMinutes(prefs.getEndMinutes())
        updateSummary()

        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBlockingEnabled(isChecked)
            updateSummary()
        }

        startTimeButton.setOnClickListener {
            pickTime(prefs.getStartMinutes()) {
                prefs.setStartMinutes(it)
                startTimeButton.text = formatMinutes(it)
                updateSummary()
            }
        }

        endTimeButton.setOnClickListener {
            pickTime(prefs.getEndMinutes()) {
                prefs.setEndMinutes(it)
                endTimeButton.text = formatMinutes(it)
                updateSummary()
            }
        }

        selectAppsButton.setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, R.string.accessibility_help, Toast.LENGTH_LONG).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        enableSwitch.isChecked = prefs.isBlockingEnabled()
        startTimeButton.text = formatMinutes(prefs.getStartMinutes())
        endTimeButton.text = formatMinutes(prefs.getEndMinutes())
        updateSummary()
        showAccessibilityPromptIfNeeded()
    }

    private fun updateSummary() {
        val blockedCount = prefs.getBlockedPackages().size
        whitelistSummary.text = getString(
            R.string.whitelist_summary,
            blockedCount,
            if (prefs.isBlockingEnabled()) getString(R.string.enabled) else getString(R.string.disabled)
        )
    }

    private fun pickTime(initialMinutes: Int, onSelected: (Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute -> onSelected(selectedHour * 60 + selectedMinute) },
            initialMinutes / 60,
            initialMinutes % 60,
            true
        ).show()
    }

    private fun formatMinutes(minutesOfDay: Int): String {
        return String.format("%02d:%02d", minutesOfDay / 60, minutesOfDay % 60)
    }

    private fun showAccessibilityPromptIfNeeded() {
        if (accessibilityPromptShown || isAccessibilityServiceEnabled()) {
            return
        }

        accessibilityPromptShown = true
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.enable_accessibility_title)
            .setMessage(R.string.enable_accessibility_message)
            .setCancelable(false)
            .setPositiveButton(R.string.open_accessibility_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton(R.string.not_now) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, AppBlockAccessibilityService::class.java)
            .flattenToString()
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()

        if (enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }) {
            return true
        }

        val enabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        return enabled == 1 && enabledServices.contains(expected)
    }
}