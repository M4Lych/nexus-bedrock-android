package org.levimc.launcher.core.ai;

public final class AIResponse {
    public static final int MAX_CHARACTERS = 8_000;
    private final String text;

    public AIResponse(String text) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("Response text is required");
        String sanitized = AIPrivacySanitizer.sanitize(text);
        if (sanitized.length() > MAX_CHARACTERS) throw new IllegalArgumentException("Response exceeds the configured size limit");
        this.text = sanitized;
    }

    public String getText() { return text; }
}
