package com.arman.rasald.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.arman.rasald.R
import com.arman.rasald.databinding.ActivitySettingsBinding
import com.arman.rasald.utils.PreferenceManager
import org.koin.android.ext.android.inject

/**
 * Settings Activity
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val preferenceManager: PreferenceManager by inject()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            setupDarkModePreference()
            setupLanguagePreference()
            setupDownloadPathPreference()
            setupQualityPreference()
            setupWifiOnlyPreference()
        }

        private fun setupDarkModePreference() {
            findPreference<ListPreference>(PreferenceManager.KEY_DARK_MODE)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val mode = newValue as String
                    updateTheme(mode)
                    true
                }
            }
        }

        private fun setupLanguagePreference() {
            findPreference<ListPreference>(PreferenceManager.KEY_LANGUAGE)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    // Language change will take effect on app restart
                    true
                }
            }
        }

        private fun setupDownloadPathPreference() {
            findPreference<androidx.preference.Preference>(PreferenceManager.KEY_DOWNLOAD_PATH)?.apply {
                setOnPreferenceClickListener {
                    // Show folder picker dialog
                    true
                }
            }
        }

        private fun setupQualityPreference() {
            findPreference<ListPreference>(PreferenceManager.KEY_DEFAULT_QUALITY)?.apply {
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            }
        }

        private fun setupWifiOnlyPreference() {
            findPreference<SwitchPreferenceCompat>(PreferenceManager.KEY_WIFI_ONLY)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val enabled = newValue as Boolean
                    preferenceManager.setWifiOnly(enabled)
                    true
                }
            }
        }

        private fun updateTheme(mode: String) {
            val nightMode = when (mode) {
                "light" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}
