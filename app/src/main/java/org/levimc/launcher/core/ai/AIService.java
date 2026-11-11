package org.levimc.launcher.core.ai;

/** Small orchestration boundary. Opt-in is checked on every request; no automatic retries occur. */
public final class AIService {
    private final AIProvider provider;

    public AIService(AIProvider provider) {
        if (provider == null) throw new IllegalArgumentException("Provider is required");
        this.provider = provider;
    }

    public AIAvailability getAvailability() {
        try { return provider.getAvailability(); }
        catch (RuntimeException ignored) { return AIAvailability.DISABLED; }
    }

    public AIProviderConfig getConfig() {
        try { return provider.getConfig(); }
        catch (RuntimeException ignored) { return null; }
    }

    public void generate(AIRequest request, boolean userOptedIn, AIProvider.Callback callback) {
        if (callback == null) return;
        if (request == null) {
            callback.onError(AIError.INVALID_RESPONSE);
            return;
        }
        if (!userOptedIn) {
            callback.onError(AIError.OPT_IN_REQUIRED);
            return;
        }
        AIAvailability availability = getAvailability();
        if (availability != AIAvailability.AVAILABLE) {
            callback.onError(availability == AIAvailability.UNSUPPORTED_CAPABILITY
                    ? AIError.UNSUPPORTED_CAPABILITY : AIError.NOT_CONFIGURED);
            return;
        }
        AIProviderConfig config = getConfig();
        if (config == null || !config.hasSecureEndpoint()) {
            callback.onError(AIError.NOT_CONFIGURED);
            return;
        }
        if (!config.getCapabilities().contains(request.getCapability())) {
            callback.onError(AIError.UNSUPPORTED_CAPABILITY);
            return;
        }
        try { provider.generate(request, callback); }
        catch (RuntimeException ignored) { callback.onError(AIError.UNAVAILABLE); }
    }

    public void cancel() {
        try { provider.cancel(); }
        catch (RuntimeException ignored) { }
    }
}
