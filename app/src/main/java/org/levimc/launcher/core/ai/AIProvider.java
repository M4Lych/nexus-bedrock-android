package org.levimc.launcher.core.ai;

/**
 * Provider boundary. Implementations must use HTTPS, enforce the configured timeout and payload
 * limits, disable automatic retries, support cancellation, and never log request data or secrets.
 * The API key/credential must be owned by an authorized provider implementation and must not be
 * embedded in this Android client.
 */
public interface AIProvider {
    AIAvailability getAvailability();
    AIProviderConfig getConfig();
    void generate(AIRequest request, Callback callback);
    void cancel();

    interface Callback {
        void onSuccess(AIResponse response);
        void onError(AIError error);
    }
}
