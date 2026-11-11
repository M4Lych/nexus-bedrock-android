package org.levimc.launcher.core.ai;

/** Safe, provider-independent errors. Raw provider messages must not be surfaced or logged. */
public enum AIError {
    OPT_IN_REQUIRED,
    NOT_CONFIGURED,
    UNAVAILABLE,
    UNSUPPORTED_CAPABILITY,
    REQUEST_TOO_LARGE,
    RESPONSE_TOO_LARGE,
    NETWORK,
    INVALID_RESPONSE,
    CANCELLED
}
