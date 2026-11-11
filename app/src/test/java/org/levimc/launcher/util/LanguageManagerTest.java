package org.levimc.launcher.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LanguageManagerTest {
    @Test
    public void freshInstallDefaultsToEnglishRegardlessOfSystemLocale() {
        assertEquals("en", LanguageManager.resolveLanguageTag(null));
        assertEquals("en", LanguageManager.resolveLanguageTag(""));
    }

    @Test
    public void explicitlySavedLanguageIsPreserved() {
        assertEquals("id", LanguageManager.resolveLanguageTag("id"));
        assertEquals("zh-CN", LanguageManager.resolveLanguageTag("zh-CN"));
    }

    @Test
    public void legacyIndonesianTagsRemainCompatible() {
        assertEquals("id", LanguageManager.resolveLanguageTag("in"));
        assertEquals("id-ID", LanguageManager.resolveLanguageTag("idn-ID"));
    }
}
