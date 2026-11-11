package org.levimc.launcher.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class NativeLibraryDiagnosticTest {
    @Test
    public void preservesLinkerFailureDetails() {
        UnsatisfiedLinkError linkerFailure = new UnsatisfiedLinkError(
                "dlopen failed: library libc++_shared.so not found: needed by libgxcore.so");

        String description = NativeLibraryDiagnostic.describeFailure(linkerFailure);
        IOException preparationFailure = NativeLibraryDiagnostic.imagePreparationUnavailable(
                description, linkerFailure);

        assertTrue(description.contains("UnsatisfiedLinkError"));
        assertTrue(description.contains("libc++_shared.so"));
        assertTrue(preparationFailure.getMessage().contains("libgxcore.so"));
        assertTrue(preparationFailure.getMessage().contains("libc++_shared.so"));
        assertSame(linkerFailure, preparationFailure.getCause());
    }

    @Test
    public void reportsMissingDiagnosticWithoutClaimingPreparationSucceeded() {
        IOException preparationFailure = NativeLibraryDiagnostic.imagePreparationUnavailable(null, null);

        assertTrue(preparationFailure.getMessage().contains("inspect the preceding NativeBridgeHelper error"));
        assertFalse(preparationFailure.getMessage().contains("successful"));
        assertEquals(null, preparationFailure.getCause());
    }
}
