package io.aegis.apikey.config;

/**
 * Configuration for API Key authentication.
 *
 * @since 1.0.0
 */
public class ApiKeyConfiguration {

    private String headerName = "X-API-Key";
    private String queryParameterName = "api_key";
    private boolean allowHeader = true;
    private boolean allowQueryParameter = false;
    private boolean allowCookie = false;
    private String cookieName = "API-Key";

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getQueryParameterName() {
        return queryParameterName;
    }

    public void setQueryParameterName(String queryParameterName) {
        this.queryParameterName = queryParameterName;
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

    public boolean isAllowCookie() {
        return allowCookie;
    }

    public void setAllowCookie(boolean allowCookie) {
        this.allowCookie = allowCookie;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ApiKeyConfiguration config = new ApiKeyConfiguration();

        public Builder headerName(String headerName) {
            config.headerName = headerName;
            return this;
        }

        public Builder queryParameterName(String name) {
            config.queryParameterName = name;
            return this;
        }

        public Builder allowHeader(boolean allow) {
            config.allowHeader = allow;
            return this;
        }

        public Builder allowQueryParameter(boolean allow) {
            config.allowQueryParameter = allow;
            return this;
        }

        public Builder allowCookie(boolean allow) {
            config.allowCookie = allow;
            return this;
        }

        public Builder cookieName(String name) {
            config.cookieName = name;
            return this;
        }

        public ApiKeyConfiguration build() {
            return config;
        }
    }
}
