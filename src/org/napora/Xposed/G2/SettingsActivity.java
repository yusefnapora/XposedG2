package org.napora.Xposed.G2;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.widget.Toast;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: yusef
 * Date: 10/30/13
 * Time: 9:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsActivity extends Activity  {



    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private boolean mShowedRebootToast = false;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            PreferenceManager.getDefaultSharedPreferences(getActivity()).
                    registerOnSharedPreferenceChangeListener(this);

            // Hide the pink -> black nav bar theme option if the pink theme doesn't exist (non-Verizon device)
            File pinkTheme = new File("/system/app/SystemUI_Theme_Lovely_Pink.apk");
            if (!pinkTheme.exists()) {
                Preference pref = findPreference(getString(R.string.pref_key_black_nav_bar));
                PreferenceCategory category = (PreferenceCategory)findPreference(getString(R.string.pref_key_appearance_category));
                category.removePreference(pref);
            }
        }


        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (!mShowedRebootToast) {
                Toast toast = Toast.makeText(getActivity(), getString(R.string.toast_reboot_for_changes), Toast.LENGTH_LONG);
                toast.show();
                mShowedRebootToast = true;
            }

            if (key.equals(getString(R.string.pref_key_long_press_home_behavior)) ||
                key.equals(getString(R.string.pref_key_recent_apps_mod)) ||
                key.equals(getString(R.string.pref_key_nav_bar_size))) {
                ListPreference pref = (ListPreference)findPreference(key);
                pref.setSummary(pref.getEntry());
            }
        }
    }



    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // load default prefs
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }


}