package org.levimc.launcher.util;

import java.io.IOException;

/** Small diagnostic helpers shared by native loading and library preparation. */
public final class NativeLibraryDiagnostic {
    private NativeLibraryDiagnostic() {
    }

    public static String describeFailure(Throwable error) {
        if (error == null) {
            return "unknown native loading failure";
        }
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        return error.getClass().getSimpleName() + ": " + message;
    }

    public static IOException imagePreparationUnavailable(String detail, Throwable cause) {
        String explanation = detail == null || detail.trim().isEmpty()
                ? "native bridge load failed; inspect the preceding NativeBridgeHelper error"
                : detail;
        return new IOException("Failed to load native image preparation (libgxcore.so): " + explanation, cause);
    }
}
