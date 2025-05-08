package com.lacklab.app.fullcamera.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.lacklab.app.fullcamera.R
import com.lacklab.app.fullcamera.util.Constant

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)

            val multiStreamingPreference = SwitchPreference(context)
            multiStreamingPreference.key = Constant.KEY_MULTI_STREAMING
            multiStreamingPreference.title = "Multi Streaming"
//            multiStreamingPreference.summaryProvider =
//                SummaryProvider<SwitchPreference> { preference ->
//                    preference.
//                }
            val previewCategory = PreferenceCategory(context)
            previewCategory.key = "PREVIEW"
            previewCategory.title = "Preview"
            screen.addPreference(previewCategory)
            previewCategory.addPreference(multiStreamingPreference)
            preferenceScreen = screen
        }
    }
}