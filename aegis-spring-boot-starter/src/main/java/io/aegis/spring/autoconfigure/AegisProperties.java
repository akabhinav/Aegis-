package io.aegis.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Aegis authentication.
 * <p>
 * Bind these properties in application.yml or application.properties with the prefix "aegis".
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "aegis")
public class AegisProperties {

    /**
     * JWT authentication configuration
     */
    private JwtProperties jwt = new JwtProperties();

    /**
     * OAuth2 authentication configuration
     */
    private OAuth2Properties oauth2 = new OAuth2Properties();

    /**
     * API Key authentication configuration
     */
    private ApiKeyProperties apikey = new ApiKeyProperties();

    /**
     * Basic authentication configuration
     */
    private BasicProperties basic = new BasicProperties();

    /**
     * Public paths that don't require authentication
     */
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/public/**",
            "/health",
            "/actuator/health"
    ));

    public JwtProperties getJwt() {
        return jwt;
    }

    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

    public OAuth2Properties getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2Properties oauth2) {
        this.oauth2 = oauth2;
    }

    public ApiKeyProperties getApikey() {
        return apikey;
    }

    public void setApikey(ApiKeyProperties apikey) {
        this.apikey = apikey;
    }

    public BasicProperties getBasic() {
        return basic;
    }

    public void setBasic(BasicProperties basic) {
        this.basic = basic;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public static class JwtProperties {
        private boolean enabled = false;
        private String secretKey;
        private String issuer;
        private String audience;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }
    }

    public static class OAuth2Properties {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    public static class ApiKeyProperties {
        private boolean enabled = false;
        private String headerName = "X-API-Key";
        private boolean allowHeader = true;
        private boolean allowQueryParameter = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public boolean isAllowHeader() {
            return allowHeader;
        }

        public void setAllowHeader(boolean allowHeader) {
            this.allowHeader = allowHeader;
        }

        public boolean isAllowQueryParameter() {
            return allowQueryParameter;
        }

        public void setAllowQueryParameter(boolean allowQueryParameter) {
            this.allowQueryParameter = allowQueryParameter;
        }
    }

    public static class BasicProperties {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
