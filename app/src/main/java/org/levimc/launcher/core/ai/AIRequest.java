package org.levimc.launcher.core.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/** Bounded request payload. Local files and account state have no implicit representation here. */
public final class AIRequest {
    public static final int MAX_TOTAL_CHARACTERS = 16_000;
    public static final int MAX_CONTEXT_FIELD_CHARACTERS = 1_000;
    public static final int MAX_REQUEST_UTF8_BYTES = 64 * 1024;

    private final AICapability capability;
    private final List<AIMessage> messages;
    private final Map<AIContextField, String> context;

    public AIRequest(AICapability capability, List<AIMessage> messages) {
        this(capability, messages, Collections.emptyMap());
    }

    public AIRequest(AICapability capability, List<AIMessage> messages,
                     Map<AIContextField, String> selectedContext) {
        if (capability == null) throw new IllegalArgumentException("Capability is required");
        if (messages == null || messages.isEmpty() || messages.size() > AIConversation.MAX_MESSAGES) {
            throw new IllegalArgumentException("Request message count is invalid");
        }
        int characters = 0;
        int utf8Bytes = 0;
        List<AIMessage> sanitized = new ArrayList<>(messages.size());
        for (AIMessage message : messages) {
            if (message == null) throw new IllegalArgumentException("Request contains an empty message");
            AIMessage copy = new AIMessage(message.getRole(), message.getText());
            characters += copy.getText().length();
            utf8Bytes += copy.getText().getBytes(StandardCharsets.UTF_8).length;
            if (characters > MAX_TOTAL_CHARACTERS) {
                throw new IllegalArgumentException("Request exceeds the configured size limit");
            }
            if (utf8Bytes > MAX_REQUEST_UTF8_BYTES) {
                throw new IllegalArgumentException("Request exceeds the configured size limit");
            }
            sanitized.add(copy);
        }
        EnumMap<AIContextField, String> contextCopy = new EnumMap<>(AIContextField.class);
        if (selectedContext != null) {
            if (selectedContext.size() > AIContextField.values().length) {
                throw new IllegalArgumentException("Request context contains unsupported fields");
            }
            for (Map.Entry<AIContextField, String> entry : selectedContext.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                String value = AIPrivacySanitizer.sanitize(entry.getValue());
                if (value.length() > MAX_CONTEXT_FIELD_CHARACTERS) {
                    throw new IllegalArgumentException("Request context exceeds the configured size limit");
                }
                utf8Bytes += value.getBytes(StandardCharsets.UTF_8).length;
                if (utf8Bytes > MAX_REQUEST_UTF8_BYTES) {
                    throw new IllegalArgumentException("Request exceeds the configured size limit");
                }
                contextCopy.put(entry.getKey(), value);
            }
        }
        this.capability = capability;
        this.messages = Collections.unmodifiableList(sanitized);
        this.context = Collections.unmodifiableMap(contextCopy);
    }

    public AICapability getCapability() { return capability; }
    public List<AIMessage> getMessages() { return messages; }
    public Map<AIContextField, String> getContext() { return context; }
}
