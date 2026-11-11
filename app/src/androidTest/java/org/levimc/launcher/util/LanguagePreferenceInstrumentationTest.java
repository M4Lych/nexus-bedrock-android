package org.levimc.launcher.util;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LanguagePreferenceInstrumentationTest {
    @Test
    public void freshPreferenceDefaultsToAndPersistsEnglishThenPreservesSelection() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean hadSavedLanguage = prefs.contains("language");
        String previousLanguage = prefs.getString("language", null);
        try {
            prefs.edit().remove("language").commit();
            assertEquals("en", LanguageManager.getSavedLanguage(context));
            assertEquals("en", prefs.getString("language", null));

            prefs.edit().putString("language", "id").commit();
            assertEquals("id", LanguageManager.getSavedLanguage(context));
            assertEquals("id", prefs.getString("language", null));
        } finally {
            SharedPreferences.Editor restore = prefs.edit();
            if (hadSavedLanguage) restore.putString("language", previousLanguage);
            else restore.remove("language");
            restore.commit();
        }
    }
}
