package org.levimc.launcher.core.ai;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Public, non-secret provider metadata. Credentials must never be placed in this object. */
public final class AIProviderConfig {
    public static final int CONNECT_TIMEOUT_MS = 10_000;
    public static final int READ_TIMEOUT_MS = 20_000;
    public static final int MAX_RESPONSE_BYTES = 64 * 1024;
    private final String providerId;
    private final String displayName;
    private final String endpoint;
    private final Set<AICapability> capabilities;

    public AIProviderConfig(String providerId, String displayName, String endpoint,
                            Set<AICapability> capabilities) {
        this.providerId = providerId == null ? "" : providerId;
        this.displayName = displayName == null ? "" : displayName;
        this.endpoint = endpoint;
        this.capabilities = capabilities == null || capabilities.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }

    public String getProviderId() { return providerId; }
    public String getDisplayName() { return displayName; }
    public String getEndpoint() { return endpoint; }
    public Set<AICapability> getCapabilities() { return capabilities; }

    public boolean hasSecureEndpoint() {
        if (endpoint == null || endpoint.trim().isEmpty()) return false;
        try {
            URI uri = URI.create(endpoint);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
