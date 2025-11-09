package io.aegis.spring.autoconfigure;

import io.aegis.apikey.ApiKeyAuthenticationProvider;
import io.aegis.apikey.config.ApiKeyConfiguration;
import io.aegis.apikey.store.ApiKeyStore;
import io.aegis.apikey.store.InMemoryApiKeyStore;
import io.aegis.basic.BasicAuthenticationProvider;
import io.aegis.basic.credentials.CredentialsStore;
import io.aegis.core.AuthenticationManager;
import io.aegis.core.AuthenticationProvider;
import io.aegis.jwt.JwtAuthenticationProvider;
import io.aegis.jwt.JwtGenerator;
import io.aegis.jwt.config.JwtConfiguration;
import io.aegis.oauth2.OAuth2AuthenticationProvider;
import io.aegis.oauth2.config.OAuth2Configuration;
import io.aegis.spring.security.AegisSecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration for Aegis authentication SDK.
 * <p>
 * Automatically configures authentication providers based on properties
 * and available dependencies on the classpath.
 *
 * @since 1.0.0
 */
@AutoConfiguration
@EnableWebSecurity
@EnableConfigurationProperties(AegisProperties.class)
public class AegisAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AegisAutoConfiguration.class);

    private final AegisProperties properties;

    public AegisAutoConfiguration(AegisProperties properties) {
        this.properties = properties;
        logger.info("Aegis Authentication SDK auto-configuration initialized");
    }

    /**
     * Creates the central authentication manager.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationManager authenticationManager(List<AuthenticationProvider<?>> providers) {
        logger.info("Creating AuthenticationManager with {} providers", providers.size());
        return new AuthenticationManager(providers);
    }

    /**
     * Configures JWT authentication if enabled.
     */
    @Bean
    @ConditionalOnProperty(prefix = "aegis.jwt", name = "enabled", havingValue = "true")
    @ConditionalOnClass(name = "io.aegis.jwt.JwtAuthenticationProvider")
    public JwtAuthenticationProvider jwtAuthenticationProvider() {
        logger.info("Configuring JWT authentication provider");
        JwtConfiguration config = createJwtConfiguration();
        return new JwtAuthenticationProvider(config);
    }

    /**
     * Configures JWT generator if enabled.
     */
    @Bean
    @ConditionalOnProperty(prefix = "aegis.jwt", name = "enabled", havingValue = "true")
    @ConditionalOnClass(name = "io.aegis.jwt.JwtGenerator")
    @ConditionalOnMissingBean
    public JwtGenerator jwtGenerator() {
        logger.info("Configuring JWT generator");
        JwtConfiguration config = createJwtConfiguration();
        return new JwtGenerator(config);
    }

    /**
     * Configures OAuth2 authentication if enabled.
     */
    @Bean
    @ConditionalOnProperty(prefix = "aegis.oauth2", name = "enabled", havingValue = "true")
    @ConditionalOnClass(name = "io.aegis.oauth2.OAuth2AuthenticationProvider")
    public OAuth2AuthenticationProvider oauth2AuthenticationProvider() {
        logger.info("Configuring OAuth2 authentication provider");
        OAuth2Configuration config = createOAuth2Configuration();
        return new OAuth2AuthenticationProvider(config);
    }

    /**
     * Configures API Key authentication if enabled.
     */
    @Bean
    @ConditionalOnProperty(prefix = "aegis.apikey", name = "enabled", havingValue = "true")
    @ConditionalOnClass(name = "io.aegis.apikey.ApiKeyAuthenticationProvider")
    public ApiKeyAuthenticationProvider apiKeyAuthenticationProvider(ApiKeyStore apiKeyStore) {
        logger.info("Configuring API Key authentication provider");
        ApiKeyConfiguration config = createApiKeyConfiguration();
        return new ApiKeyAuthenticationProvider(config, apiKeyStore);
    }

    /**
     * Provides default in-memory API key store.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "aegis.apikey", name = "enabled", havingValue = "true")
    public ApiKeyStore apiKeyStore() {
        logger.info("Creating in-memory API key store");
        return new InMemoryApiKeyStore();
    }

    /**
     * Configures Basic authentication if enabled.
     */
    @Bean
    @ConditionalOnProperty(prefix = "aegis.basic", name = "enabled", havingValue = "true")
    @ConditionalOnClass(name = "io.aegis.basic.BasicAuthenticationProvider")
    public BasicAuthenticationProvider basicAuthenticationProvider(CredentialsStore credentialsStore) {
        logger.info("Configuring Basic authentication provider");
        return new BasicAuthenticationProvider(credentialsStore);
    }

    /**
     * Configures Spring Security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManager authenticationManager) throws Exception {

        logger.info("Configuring Spring Security filter chain");

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // Configure public endpoints
                    String[] publicPaths = properties.getPublicPaths().toArray(new String[0]);
                    if (publicPaths.length > 0) {
                        auth.requestMatchers(publicPaths).permitAll();
                    }
                    // All other requests require authentication
                    auth.anyRequest().authenticated();
                });

        // Add Aegis authentication filter
        http.addFilterBefore(
                new AegisSecurityFilter(authenticationManager),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    private JwtConfiguration createJwtConfiguration() {
        AegisProperties.JwtProperties jwtProps = properties.getJwt();
        return JwtConfiguration.builder()
                .issuer(jwtProps.getIssuer())
                .audience(jwtProps.getAudience())
                .secretKey(jwtProps.getSecretKey())
                .build();
    }

    private OAuth2Configuration createOAuth2Configuration() {
        AegisProperties.OAuth2Properties oauth2Props = properties.getOauth2();
        return OAuth2Configuration.builder()
                .clientId(oauth2Props.getClientId())
                .clientSecret(oauth2Props.getClientSecret())
                .build();
    }

    private ApiKeyConfiguration createApiKeyConfiguration() {
        AegisProperties.ApiKeyProperties apiKeyProps = properties.getApikey();
        return ApiKeyConfiguration.builder()
                .headerName(apiKeyProps.getHeaderName())
                .allowHeader(apiKeyProps.isAllowHeader())
                .allowQueryParameter(apiKeyProps.isAllowQueryParameter())
                .build();
    }
}
