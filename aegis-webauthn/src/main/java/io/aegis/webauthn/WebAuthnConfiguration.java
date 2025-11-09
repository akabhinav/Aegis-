package io.aegis.webauthn;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for WebAuthn authentication.
 *
 * @since 1.0.0
 */
public class WebAuthnConfiguration {

    private String relyingPartyId;
    private String relyingPartyName;
    private List<String> allowedOrigins = new ArrayList<>();
    private boolean requireResidentKey = false;
    private long timeout = 60000L; // 60 seconds

    public String getRelyingPartyId() {
        return relyingPartyId;
    }

    public void setRelyingPartyId(String relyingPartyId) {
        this.relyingPartyId = relyingPartyId;
    }

    public String getRelyingPartyName() {
        return relyingPartyName;
    }

    public void setRelyingPartyName(String relyingPartyName) {
        this.relyingPartyName = relyingPartyName;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public boolean isRequireResidentKey() {
        return requireResidentKey;
    }

    public void setRequireResidentKey(boolean requireResidentKey) {
        this.requireResidentKey = requireResidentKey;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final WebAuthnConfiguration config = new WebAuthnConfiguration();

        public Builder relyingPartyId(String rpId) {
            config.relyingPartyId = rpId;
            return this;
        }

        public Builder relyingPartyName(String rpName) {
            config.relyingPartyName = rpName;
            return this;
        }

        public Builder allowedOrigins(List<String> origins) {
            config.allowedOrigins = new ArrayList<>(origins);
            return this;
        }

        public Builder addOrigin(String origin) {
            config.allowedOrigins.add(origin);
            return this;
        }

        public Builder requireResidentKey(boolean require) {
            config.requireResidentKey = require;
            return this;
        }

        public Builder timeout(long timeout) {
            config.timeout = timeout;
            return this;
        }

        public WebAuthnConfiguration build() {
            if (config.relyingPartyId == null || config.relyingPartyName == null) {
                throw new IllegalStateException("Relying party ID and name are required");
            }
            return config;
        }
    }
}
