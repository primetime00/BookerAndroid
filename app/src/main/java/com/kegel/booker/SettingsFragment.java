package com.kegel.booker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        androidx.preference.EditTextPreference editTextPreference = getPreferenceManager().findPreference(getString(R.string.booker_preference_port));
        editTextPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        EditTextPreference portPreference = findPreference(getString(R.string.booker_preference_port));
        portPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            try {
                int val = Integer.parseInt((String) newValue);
                if (val < 0 || val > 65535) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
            prefs.edit().putString(getString(R.string.booker_preference_port), (String) newValue).apply();
            return true;
        });


        EditTextPreference urlPreference = findPreference(getString(R.string.booker_preference_url));
        urlPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String val = (String) newValue;
                try {
                    URI uri = new URI(val);
                    if (uri.toURL().getHost().isEmpty()) {
                        return false;
                    }
                } catch (URISyntaxException | MalformedURLException e) {
                    return false;
                }
                prefs.edit().putString(getString(R.string.booker_preference_url), val).apply();
                return true;
            }
        });

    }
}
