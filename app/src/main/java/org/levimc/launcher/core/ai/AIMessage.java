package org.levimc.launcher.core.ai;

import java.util.Objects;

public final class AIMessage {
    public enum Role { USER, ASSISTANT }

    public static final int MAX_CHARACTERS = 4_000;

    private final Role role;
    private final String text;

    public AIMessage(Role role, String text) {
        this.role = Objects.requireNonNull(role, "role");
        String value = text == null ? "" : text;
        value = AIPrivacySanitizer.sanitize(value);
        if (value.length() > MAX_CHARACTERS) {
            throw new IllegalArgumentException("Message exceeds the configured size limit");
        }
        this.text = value;
    }

    public Role getRole() { return role; }
    public String getText() { return text; }
}
