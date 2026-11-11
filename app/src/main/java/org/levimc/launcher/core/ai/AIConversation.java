package org.levimc.launcher.core.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Session-only history. This model has no serializer or storage integration. */
public final class AIConversation {
    public static final int MAX_MESSAGES = 32;
    private final List<AIMessage> messages = new ArrayList<>();

    public synchronized void add(AIMessage message) {
        if (message == null) throw new IllegalArgumentException("Message is required");
        if (messages.size() >= MAX_MESSAGES) throw new IllegalStateException("Conversation is full");
        messages.add(message);
    }

    public synchronized List<AIMessage> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public synchronized void clear() { messages.clear(); }
    public synchronized int size() { return messages.size(); }
}
