package com.tyassine.wearther

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class PreferencesActivity : AppCompatActivity() {
    class PreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            //Location preferences logic
            val autoLocationPreference = findPreference<SwitchPreferenceCompat>("auto_location")
            val setLocationPreference = findPreference<Preference>("set_location")
            setLocationPreference?.isVisible = autoLocationPreference?.isChecked == false

            autoLocationPreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
                val switchPreference = preference as SwitchPreferenceCompat
                val setLocationPreference = findPreference<Preference>("set_location")
                setLocationPreference?.isVisible = !(newValue as Boolean)
                true
            }

            //Dynamically change Unit and Gender summary
            val unit = findPreference<ListPreference>("unit")
            unit?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            val gender = findPreference<ListPreference>("gender")
            gender?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

            //About preference behavior
            val about = findPreference<Preference>("about")
            about?.onPreferenceClickListener = Preference.OnPreferenceClickListener {

                Toast.makeText(context, "Version 1.0", Toast.LENGTH_SHORT).show()
                true

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.preferences_container, PreferencesFragment())
            .commit()
    }
}



