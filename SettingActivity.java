package com.maxmade.bluetooth.le;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingActivity extends Activity{
    @Override    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SetupFregment())
                .commit();
    }

    public static class SetupFregment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.setting_preference);

        }
    }
}