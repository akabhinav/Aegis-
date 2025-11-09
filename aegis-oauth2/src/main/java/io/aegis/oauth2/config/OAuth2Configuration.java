package io.aegis.oauth2.config;

import java.net.URI;

/**
 * Configuration for OAuth 2.0 / OpenID Connect authentication.
 *
 * @since 1.0.0
 */
public class OAuth2Configuration {

    private String clientId;
    private String clientSecret;
    private URI authorizationEndpoint;
    private URI tokenEndpoint;
    private URI userInfoEndpoint;
    private URI introspectionEndpoint;
    private URI jwksUri;
    private String scope = "openid profile email";
    private String redirectUri;

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

    public URI getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(URI authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public URI getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(URI tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public URI getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(URI userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    public URI getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(URI introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public URI getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(URI jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final OAuth2Configuration config = new OAuth2Configuration();

        public Builder clientId(String clientId) {
            config.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            config.clientSecret = clientSecret;
            return this;
        }

        public Builder authorizationEndpoint(URI uri) {
            config.authorizationEndpoint = uri;
            return this;
        }

        public Builder tokenEndpoint(URI uri) {
            config.tokenEndpoint = uri;
            return this;
        }

        public Builder userInfoEndpoint(URI uri) {
            config.userInfoEndpoint = uri;
            return this;
        }

        public Builder introspectionEndpoint(URI uri) {
            config.introspectionEndpoint = uri;
            return this;
        }

        public Builder jwksUri(URI uri) {
            config.jwksUri = uri;
            return this;
        }

        public Builder scope(String scope) {
            config.scope = scope;
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            config.redirectUri = redirectUri;
            return this;
        }

        public OAuth2Configuration build() {
            return config;
        }
    }
}
