package org.levimc.launcher.core.ai;

import java.util.Collections;

/** Default provider when no backend has been configured. It never makes a network request. */
public final class UnavailableAIProvider implements AIProvider {
    private static final AIProviderConfig CONFIG = new AIProviderConfig(
            "unavailable", "", null, Collections.emptySet());

    @Override public AIAvailability getAvailability() { return AIAvailability.NOT_CONFIGURED; }
    @Override public AIProviderConfig getConfig() { return CONFIG; }

    @Override
    public void generate(AIRequest request, Callback callback) {
        if (callback != null) callback.onError(AIError.NOT_CONFIGURED);
    }

    @Override public void cancel() { }
}
